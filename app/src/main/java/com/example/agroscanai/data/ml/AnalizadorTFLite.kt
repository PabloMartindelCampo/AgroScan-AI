package com.example.agroscanai.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.agroscanai.utils.FotosEscaneo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Analiza la foto con el modelo TensorFlow Lite de assets
 * (entrenado con entrenamiento/entrenar_modelo_frijol.ipynb).
 *
 * Entrada: foto recortada al centro en cuadrado y reducida al tamaño del modelo,
 * píxeles RGB 0–255 en float32 (la normalización va dentro del modelo).
 * Salida: una probabilidad por etiqueta, en el orden de etiquetas_frijol.txt.
 * Si el modelo no se puede cargar devuelve null y el escaneo queda "Pendiente de análisis".
 */
class AnalizadorTFLite(private val context: Context) : AnalizadorHojas {

    private class Modelo(
        val interprete: Interpreter,
        val etiquetas: List<String>,
        val version: String,
        val alto: Int,
        val ancho: Int
    )

    // Se carga la primera vez que se analiza una foto, ya fuera del hilo principal
    private val modelo: Modelo? by lazy {
        runCatching { cargar() }
            .onFailure { Log.e(TAG, "No se pudo cargar el modelo", it) }
            .getOrNull()
    }

    // El intérprete no admite dos análisis al mismo tiempo
    private val candado = Mutex()

    override suspend fun analizar(foto: File): ResultadoAnalisis? {
        val m = modelo ?: return null
        val bitmap = FotosEscaneo.cargarBitmap(foto.absolutePath, ladoMaximo = 2 * m.alto)
            ?: return null
        val entrada = aEntrada(bitmap, m.ancho, m.alto)
        val salida = Array(1) { FloatArray(m.etiquetas.size) }
        candado.withLock { m.interprete.run(entrada, salida) }

        val probabilidades = salida[0]
        val mejor = probabilidades.indices.maxBy { probabilidades[it] }
        return ResultadoAnalisis(m.etiquetas[mejor], probabilidades[mejor], m.version)
    }

    private fun cargar(): Modelo {
        val bytes = context.assets.open(MODELO_ASSET).use { it.readBytes() }
        val etiquetas = context.assets.open(ETIQUETAS_ASSET).bufferedReader()
            .use { it.readLines() }.map { it.trim() }.filter { it.isNotEmpty() }

        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes).rewind()
        val interprete = Interpreter(buffer)
        try {
            val entrada = interprete.getInputTensor(0)
            val forma = entrada.shape()          // [1, alto, ancho, 3]
            check(entrada.dataType() == DataType.FLOAT32 && forma.size == 4 && forma[3] == 3) {
                "Entrada del modelo inesperada: ${forma.contentToString()} ${entrada.dataType()}"
            }
            check(interprete.getOutputTensor(0).shape().last() == etiquetas.size) {
                "El número de etiquetas no coincide con la salida del modelo"
            }
            // La versión es la huella del archivo: identifica exactamente qué modelo dio el resultado
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            return Modelo(interprete, etiquetas, "frijol-${sha.take(12)}", forma[1], forma[2])
        } catch (e: Exception) {
            interprete.close()
            throw e
        }
    }

    private fun aEntrada(bitmap: Bitmap, ancho: Int, alto: Int): ByteBuffer {
        // Recorte cuadrado al centro: las fotos del entrenamiento eran cuadradas
        val lado = minOf(bitmap.width, bitmap.height)
        val pixeles = IntArray(lado * lado)
        bitmap.getPixels(pixeles, 0, lado,
            (bitmap.width - lado) / 2, (bitmap.height - lado) / 2, lado, lado)

        val rgb = Array(3) { canal ->
            val desplazamiento = 16 - 8 * canal     // R, G, B
            FloatArray(pixeles.size) { ((pixeles[it] shr desplazamiento) and 0xFF).toFloat() }
        }
        val pesosX = pesosTriangulares(lado, ancho)
        val pesosY = pesosTriangulares(lado, alto)
        val reducida = rgb.map { canal ->
            val horizontal = remuestrear(canal, lado, lado, pesosX, horizontal = true)
            remuestrear(horizontal, ancho, lado, pesosY, horizontal = false)
        }

        val buffer = ByteBuffer.allocateDirect(4 * 3 * ancho * alto).order(ByteOrder.nativeOrder())
        for (i in 0 until ancho * alto) {
            for (canal in reducida) buffer.putFloat(canal[i])
        }
        buffer.rewind()
        return buffer
    }

    private class Pesos(val inicio: IntArray, val valores: Array<FloatArray>)

    /**
     * Pesos para reducir la imagen igual que PIL (Image.BILINEAR), que es como se
     * redimensionaron las fotos del entrenamiento: cada píxel nuevo promedia, con peso
     * triangular, todos los píxeles que cubre. El escalado bilineal de Android solo mira
     * 2x2 píxeles y al reducir borra las manchas pequeñas (con él, en las 128 fotos de
     * prueba de iBean la exactitud bajaba de 89.8 % a 81.3 %).
     */
    private fun pesosTriangulares(entrada: Int, salida: Int): Pesos {
        val escala = entrada.toDouble() / salida
        val soporte = maxOf(escala, 1.0)
        val inicio = IntArray(salida)
        val valores = Array(salida) { i ->
            val centro = (i + 0.5) * escala
            val desde = maxOf((centro - soporte + 0.5).toInt(), 0)
            val hasta = minOf((centro + soporte + 0.5).toInt(), entrada)
            val pesos = DoubleArray(hasta - desde) { j ->
                maxOf(0.0, 1.0 - abs((j + desde - centro + 0.5) / soporte))
            }
            val suma = pesos.sum()
            inicio[i] = desde
            FloatArray(pesos.size) { (pesos[it] / suma).toFloat() }
        }
        return Pesos(inicio, valores)
    }

    /** Una pasada (horizontal o vertical) del remuestreo; como PIL, redondea a 0–255. */
    private fun remuestrear(
        canal: FloatArray, ancho: Int, alto: Int, pesos: Pesos, horizontal: Boolean
    ): FloatArray {
        val salida = pesos.inicio.size
        val anchoSalida = if (horizontal) salida else ancho
        val altoSalida = if (horizontal) alto else salida
        return FloatArray(anchoSalida * altoSalida) { k ->
            val x = k % anchoSalida
            val y = k / anchoSalida
            val i = if (horizontal) x else y
            var suma = 0f
            pesos.valores[i].forEachIndexed { j, w ->
                val origen = pesos.inicio[i] + j
                suma += w * canal[if (horizontal) y * ancho + origen else origen * ancho + x]
            }
            suma.roundToInt().coerceIn(0, 255).toFloat()
        }
    }

    private companion object {
        const val TAG = "AnalizadorTFLite"
    }
}
