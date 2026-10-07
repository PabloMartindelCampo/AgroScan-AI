package com.example.agroscanai.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.agroscanai.R
import com.example.agroscanai.data.model.EstadoCultivo
import kotlin.random.Random

object NotificacionHelper {

    private const val CANAL_ESCANEOS = "agroscan_escaneos"
    private const val CANAL_ALERTAS  = "agroscan_alertas"

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CANAL_ESCANEOS,
                    "Escaneos completados",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones cuando un escaneo de parcela finaliza"
                }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CANAL_ALERTAS,
                    "Alertas de cultivos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas: enfermedad detectada en una foto analizada"
                    enableVibration(true)
                }
            )
        }
    }

    fun notificarEscaneoCompleto(
        context: Context,
        nombreCultivo: String,
        estado: EstadoCultivo,
        diagnostico: String
    ) {
        val (canalId, titulo, cuerpo) = when (estado) {
            EstadoCultivo.ENFERMEDAD -> Triple(
                CANAL_ALERTAS,
                "⚠️ Enfermedad detectada en $nombreCultivo",
                "El modelo detectó ${diagnostico.ifBlank { "una enfermedad" }} en la foto. Revisa la recomendación y consulta a un técnico."
            )
            EstadoCultivo.SANA -> Triple(
                CANAL_ESCANEOS,
                "✅ Escaneo completado: $nombreCultivo",
                "El modelo clasificó la hoja como sana."
            )
            else -> Triple(
                CANAL_ESCANEOS,
                "📷 Foto guardada: $nombreCultivo",
                "La foto quedó registrada y está pendiente de análisis."
            )
        }

        val notificacion = NotificationCompat.Builder(context, canalId)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setPriority(
                if (canalId == CANAL_ALERTAS) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(Random.nextInt(10_000), notificacion)
        } catch (_: SecurityException) {
            // Permiso POST_NOTIFICATIONS no otorgado — sin acción
        }
    }
}
