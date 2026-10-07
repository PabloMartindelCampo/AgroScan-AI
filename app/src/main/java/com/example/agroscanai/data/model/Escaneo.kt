package com.example.agroscanai.data.model

/**
 * Escaneo por foto de una hoja. La imagen se guarda solo en el almacenamiento
 * interno del teléfono; en Firestore se guardan únicamente estos metadatos.
 */
data class Escaneo(
    val id: String = "",
    val cultivoId: String = "",
    val fecha: String = "",                 // dd/MM/yyyy HH:mm
    val fotoRuta: String = "",              // ruta local dentro de filesDir
    val origen: OrigenFoto = OrigenFoto.CAMARA,
    // Cámara: GPS del teléfono al tomar la foto. Galería: GPS del EXIF de la foto.
    // null si no se pudo obtener; nunca se usa una ubicación por defecto.
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val estadoGeneral: EstadoEscaneo = EstadoEscaneo.PENDIENTE,
    val claseDetectada: String = "",        // etiqueta del modelo (ver CatalogoFrijol)
    val confianza: Float? = null,           // probabilidad que da el modelo (0–1); null sin análisis
    val modeloVersion: String = ""
)

enum class OrigenFoto { CAMARA, GALERIA }

enum class EstadoEscaneo {
    PENDIENTE,      // foto guardada, pendiente de análisis
    EN_PROGRESO,
    COMPLETADO,     // analizada por el modelo
    ERROR
}

/** Entrada de la tabla fija de clases del modelo y su recomendación (ver CatalogoFrijol). */
data class Plaga(
    val id: Int = 0,
    val etiquetaModelo: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    // Sin uso: un clasificador de una sola foto no mide severidad ni área afectada
    val severidad: SeveridadPlaga = SeveridadPlaga.LEVE,
    val tratamiento: String = "",
    val productoRecomendado: String = "",
    // Sin uso: ver comentario de severidad
    val areaAfectada: Float = 0f,
    val fuente: String = ""
)

enum class SeveridadPlaga {
    LEVE,
    MODERADA,
    GRAVE
}
