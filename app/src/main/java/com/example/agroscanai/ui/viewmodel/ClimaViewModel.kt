package com.example.agroscanai.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.model.Clima
import com.example.agroscanai.data.model.PronosticoClima
import com.example.agroscanai.data.remote.OpenMeteoApi
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class ClimaUiState {
    object Cargando : ClimaUiState()
    data class Exito(val pronostico: PronosticoClima, val region: String) : ClimaUiState()
    data class Error(val mensaje: String) : ClimaUiState()
}

class ClimaViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<ClimaUiState>(ClimaUiState.Cargando)
    val uiState: StateFlow<ClimaUiState> = _uiState

    private val fusedLocation = LocationServices.getFusedLocationProviderClient(app)
    private val api = OpenMeteoApi.instance

    fun tienePermisoUbicacion(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun cargarClima(tienePermiso: Boolean) {
        viewModelScope.launch {
            _uiState.value = ClimaUiState.Cargando
            // Sin ubicación real no se muestra el clima de otro lugar
            if (!tienePermiso) {
                _uiState.value = ClimaUiState.Error(
                    "Permite el acceso a tu ubicación para ver el clima de tu zona.")
                return@launch
            }
            val (lat, lon, region) = try { obtenerUbicacion() }
            catch (_: Exception) {
                _uiState.value = ClimaUiState.Error(
                    "No se pudo obtener tu ubicación. Activa el GPS e intenta de nuevo.")
                return@launch
            }
            fetchClima(lat, lon, region)
        }
    }

    private suspend fun fetchClima(lat: Double, lon: Double, region: String) {
        try {
            val response = api.getPronostico(lat, lon)
            val regionFinal = response.timezone
                .replace("_", " ")
                .replace("/", ", ")
                .ifBlank { region }
            _uiState.value = ClimaUiState.Exito(mapearPronostico(response, regionFinal), regionFinal)
        } catch (e: Exception) {
            _uiState.value = ClimaUiState.Error("Sin conexión. Verifica tu red e intenta de nuevo.")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun obtenerUbicacion(): Triple<Double, Double, String> =
        suspendCancellableCoroutine { cont ->
            fusedLocation.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(Triple(loc.latitude, loc.longitude, "Tu ubicación"))
                    } else {
                        val cts = CancellationTokenSource()
                        cont.invokeOnCancellation { cts.cancel() }
                        fusedLocation.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token
                        ).addOnSuccessListener { currentLoc ->
                            if (currentLoc != null)
                                cont.resume(Triple(currentLoc.latitude, currentLoc.longitude, "Tu ubicación"))
                            else
                                cont.resumeWithException(Exception("Ubicación no disponible"))
                        }.addOnFailureListener { cont.resumeWithException(it) }
                    }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    private fun mapearPronostico(
        response: com.example.agroscanai.data.remote.OpenMeteoResponse,
        region: String
    ): PronosticoClima {
        val current = response.current
        val daily   = response.daily
        val nombres = nombresDias()

        val dias = daily.time.take(7).mapIndexed { i, _ ->
            Clima(
                fecha           = nombres.getOrElse(i) { "Día ${i+1}" },
                temperatura     = if (i == 0) current.temperatura
                                  else (daily.tempMax.getOrElse(i){0f} + daily.tempMin.getOrElse(i){0f}) / 2f,
                temperaturaMin  = daily.tempMin.getOrElse(i) { 0f },
                temperaturaMax  = daily.tempMax.getOrElse(i) { 0f },
                humedad         = if (i == 0) current.humedad else 0,
                descripcion     = wmoADescripcion(
                                    if (i == 0) current.weatherCode
                                    else daily.weatherCodes.getOrElse(i) { 0 }
                                  ),
                velocidadViento = if (i == 0) current.velocidadViento else 0f,
                probabilidadLluvia = daily.precipProb.getOrElse(i) { 0 },
                uvIndex         = daily.uvIndex.getOrElse(i) { 3f }.toInt(),
                region          = region
            )
        }

        return PronosticoClima(dias, recomendaciones(dias))
    }

    private fun wmoADescripcion(code: Int): String = when (code) {
        0        -> "Despejado"
        1        -> "Principalmente despejado"
        2        -> "Parcialmente nublado"
        3        -> "Nublado"
        45, 48   -> "Niebla"
        51,53,55 -> "Llovizna"
        61,63,65 -> "Lluvioso"
        71,73,75 -> "Nevado"
        80,81,82 -> "Lluvioso"
        85, 86   -> "Nevado"
        95       -> "Tormenta"
        96, 99   -> "Tormenta con granizo"
        else     -> "Variable"
    }

    private fun nombresDias(): List<String> {
        val fmt = SimpleDateFormat("EEEE", Locale("es", "ES"))
        return (0..6).map { offset ->
            when (offset) {
                0 -> "Hoy"
                1 -> "Mañana"
                else -> {
                    val c = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, offset) }
                    fmt.format(c.time).replaceFirstChar { it.uppercase() }
                }
            }
        }
    }

    private fun recomendaciones(dias: List<Clima>): List<String> {
        val lista = mutableListOf<String>()

        val conLluvia = dias.filter { it.probabilidadLluvia >= 60 }
        if (conLluvia.isNotEmpty())
            lista += "${conLluvia.joinToString(", ") { it.fecha }}: alta probabilidad de lluvia — evita fertilizar o fumigar esos días."

        val soleados = dias.filter { it.probabilidadLluvia < 20 && it.uvIndex >= 5 }
        if (soleados.isNotEmpty())
            lista += "${soleados.first().fecha}: condiciones ideales para actividades de campo al aire libre."

        if (dias.any { it.temperaturaMin < 5f })
            lista += "Temperaturas bajo 5 °C esperadas — protege los cultivos jóvenes de posibles heladas."

        if (dias.count { it.humedad > 75 } >= 3)
            lista += "Humedad alta persistente — riesgo de hongos. Considera aplicar fungicidas preventivos."

        if (dias.any { it.uvIndex >= 8 })
            lista += "Índice UV muy alto. Protege los cultivos sensibles y planifica trabajos de campo temprano por la mañana."

        if (lista.isEmpty())
            lista += "Condiciones climáticas favorables para todas las actividades agrícolas esta semana."

        return lista
    }
}
