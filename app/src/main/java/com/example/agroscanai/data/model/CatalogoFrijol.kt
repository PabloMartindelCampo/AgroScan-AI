package com.example.agroscanai.data.model

/**
 * Tabla fija de clases que reconoce el modelo de hojas de frijol y su recomendación.
 *
 * Las etiquetas deben coincidir con las del archivo de etiquetas del modelo .tflite.
 * Una recomendación solo se muestra cuando tiene texto Y fuente citada; mientras
 * falte alguno de los dos, la app indica que está pendiente de fuente verificada.
 *
 * TODO (responsable del proyecto): rellenar descripcion, tratamiento y fuente con
 * información verificada (SENASICA, INIFAP u otras).
 */
object CatalogoFrijol {

    const val ETIQUETA_SANA = "sana"

    const val AVISO = "Recomendación referencial; consulte a un técnico."

    val clases: List<Plaga> = listOf(
        Plaga(
            id = 0,
            etiquetaModelo = ETIQUETA_SANA,
            nombre = "Hoja sana",
            descripcion = "",
            tratamiento = "",
            fuente = ""
        ),
        Plaga(
            id = 1,
            etiquetaModelo = "mancha_angular",
            nombre = "Mancha angular",
            descripcion = "",
            tratamiento = "",
            fuente = ""
        ),
        Plaga(
            id = 2,
            etiquetaModelo = "roya",
            nombre = "Roya",
            descripcion = "",
            tratamiento = "",
            fuente = ""
        )
    )

    fun porEtiqueta(etiqueta: String): Plaga? = clases.find { it.etiquetaModelo == etiqueta }

    fun porNombre(nombre: String): Plaga? = clases.find { it.nombre == nombre }

    fun tieneRecomendacionVerificada(clase: Plaga): Boolean =
        clase.tratamiento.isNotBlank() && clase.fuente.isNotBlank()
}
