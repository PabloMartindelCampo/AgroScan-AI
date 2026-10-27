package com.example.agroscanai.data.ml

import android.content.Context
import java.io.File

/** Archivos del modelo dentro de app/src/main/assets (ver assets/LEEME_MODELO.txt). */
const val MODELO_ASSET    = "modelo_frijol.tflite"
const val ETIQUETAS_ASSET = "etiquetas_frijol.txt"

/** Resultado que entrega el modelo para una foto. */
data class ResultadoAnalisis(
    val etiqueta: String,        // una de las etiquetas de CatalogoFrijol
    val probabilidad: Float,     // salida del modelo para esa etiqueta (0–1)
    val modeloVersion: String
)

/**
 * Punto de integración del modelo de hojas de frijol (TensorFlow Lite, ver AnalizadorTFLite).
 * Devuelve null cuando no hay modelo: el escaneo queda "Pendiente de análisis"
 * y nunca se muestra un diagnóstico inventado.
 */
interface AnalizadorHojas {
    suspend fun analizar(foto: File): ResultadoAnalisis?
}

/** Se usa cuando el modelo no está en assets: no hay análisis. */
class AnalizadorNoDisponible : AnalizadorHojas {
    override suspend fun analizar(foto: File): ResultadoAnalisis? = null
}

fun modeloEnAssets(context: Context): Boolean =
    runCatching { context.assets.open(MODELO_ASSET).close(); true }.getOrDefault(false)

/** Usa el modelo TFLite si está en assets; si no, los escaneos quedan pendientes. */
fun crearAnalizador(context: Context): AnalizadorHojas =
    if (modeloEnAssets(context)) AnalizadorTFLite(context) else AnalizadorNoDisponible()
