package com.example.agroscanai.data.model

const val SIN_ESCANEOS = "Sin escaneos aún"

data class Cultivo(
    val id: String = "",
    val nombre: String = "",
    val tipoCultivo: String = "",
    val variedadSemilla: String = "",
    val hectareas: Double = 0.0,
    val fechaSiembra: String = "",
    val ubicacion: String = "",
    val estado: String = EstadoCultivo.SIN_ESCANEO.name,
    // Nombre de la enfermedad cuando estado = ENFERMEDAD (viene del modelo, nunca se inventa)
    val diagnostico: String = "",
    val ultimoEscaneo: String = "",
    val imagenUrl: String = ""
) {
    fun estadoEnum(): EstadoCultivo = try {
        EstadoCultivo.valueOf(estado)
    } catch (e: Exception) {
        EstadoCultivo.SIN_ESCANEO
    }

    fun tieneEscaneo(): Boolean = estadoEnum() != EstadoCultivo.SIN_ESCANEO

    /** Parcela con al menos una foto ya analizada por el modelo. */
    fun tieneFotoAnalizada(): Boolean =
        estadoEnum() == EstadoCultivo.SANA || estadoEnum() == EstadoCultivo.ENFERMEDAD

    fun estadoTexto(): String = when (estadoEnum()) {
        EstadoCultivo.SIN_ESCANEO        -> "Sin escaneo"
        EstadoCultivo.PENDIENTE_ANALISIS -> "Pendiente de análisis"
        EstadoCultivo.SANA               -> "Sana"
        EstadoCultivo.ENFERMEDAD         ->
            if (diagnostico.isNotBlank()) "Enfermedad detectada: $diagnostico"
            else "Enfermedad detectada"
    }

    fun descripcionSalud(): String = when (estadoEnum()) {
        EstadoCultivo.SIN_ESCANEO ->
            "Esta parcela aún no ha sido escaneada. Toma una foto de una hoja para registrar un escaneo."
        EstadoCultivo.PENDIENTE_ANALISIS ->
            "La última foto está guardada y pendiente de análisis. El diagnóstico aparecerá cuando el modelo de IA esté disponible."
        EstadoCultivo.SANA ->
            "El modelo clasificó la hoja de la última foto como sana."
        EstadoCultivo.ENFERMEDAD ->
            "El modelo detectó ${diagnostico.ifBlank { "una enfermedad" }} en la hoja de la última foto analizada."
    }
}

enum class EstadoCultivo {
    SIN_ESCANEO,
    PENDIENTE_ANALISIS,
    SANA,
    ENFERMEDAD
}
