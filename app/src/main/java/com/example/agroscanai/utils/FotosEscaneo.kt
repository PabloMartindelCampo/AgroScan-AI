package com.example.agroscanai.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Manejo de las fotos de hojas. Se guardan solo en el almacenamiento interno
 * de la app (filesDir/escaneos), excluido del respaldo en la nube.
 */
object FotosEscaneo {

    private const val CARPETA = "escaneos"

    private fun carpeta(context: Context): File =
        File(context.filesDir, CARPETA).apply { mkdirs() }

    fun nuevoArchivo(context: Context): File =
        File(carpeta(context), "hoja_${System.currentTimeMillis()}.jpg")

    /** Uri que recibe la cámara del sistema para escribir la foto en nuestro archivo. */
    fun uriParaCamara(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

    /** Copia la imagen elegida en la galería al almacenamiento interno. */
    suspend fun copiarDesdeGaleria(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        val destino = nuevoArchivo(context)
        try {
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            } ?: return@withContext null
            destino
        } catch (_: Exception) {
            destino.delete()
            null
        }
    }

    /** Coordenadas guardadas en el EXIF de la foto, si la foto las trae. */
    fun gpsDeExif(archivo: File): Pair<Double, Double>? = try {
        val latLong = FloatArray(2)
        if (ExifInterface(archivo.path).getLatLong(latLong))
            latLong[0].toDouble() to latLong[1].toDouble()
        else null
    } catch (_: Exception) { null }

    /** Ubicación actual del teléfono; null si no hay permiso o no se pudo obtener. */
    @SuppressLint("MissingPermission")
    suspend fun ubicacionActual(context: Context): Pair<Double, Double>? {
        val permiso = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!permiso) return null
        return suspendCancellableCoroutine { cont ->
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    cont.resume(loc?.let { it.latitude to it.longitude })
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    /** Carga la foto reducida y con la orientación correcta (según su EXIF). */
    suspend fun cargarBitmap(ruta: String, ladoMaximo: Int = 1280): Bitmap? = withContext(Dispatchers.IO) {
        val archivo = File(ruta)
        if (!archivo.exists()) return@withContext null
        try {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(ruta, limites)
            var muestra = 1
            while (limites.outWidth / (muestra * 2) >= ladoMaximo ||
                   limites.outHeight / (muestra * 2) >= ladoMaximo) muestra *= 2

            val bitmap = BitmapFactory.decodeFile(ruta,
                BitmapFactory.Options().apply { inSampleSize = muestra }) ?: return@withContext null

            val grados = when (ExifInterface(ruta).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (grados == 0f) bitmap
            else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
                Matrix().apply { postRotate(grados) }, true)
        } catch (_: Exception) { null }
    }
}
