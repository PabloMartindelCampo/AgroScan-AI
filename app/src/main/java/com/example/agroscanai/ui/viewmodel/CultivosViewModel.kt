package com.example.agroscanai.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.agroscanai.utils.NotificacionHelper
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UltimoEscaneoData(
    val fecha: String = "",
    val confianzaIA: Int = 0,
    val gpsLat: Double = 0.0,
    val gpsLon: Double = 0.0,
    val humedadSuelo: Float = 0f,
    val estresSuelo: String = "",
    val descripcionSuelo: String = "",
    val plagasDetectadas: Boolean = false,
    val descripcionPlagas: String = "",
    val nivelNitrogenio: Float = 0f,
    val nivelFosforo: Float = 0f,
    val nivelPotasio: Float = 0f,
    val descripcionNutrientes: String = "",
    val recomendacionNutrientes: String = ""
)

class CultivosViewModel(application: Application) : AndroidViewModel(application) {

    private val db   = Firebase.firestore
    private val auth = Firebase.auth
    private val gson = Gson()
    private val prefs = application.getSharedPreferences("agroscan_v2", Context.MODE_PRIVATE)

    private val _cultivos   = MutableStateFlow<List<Cultivo>>(emptyList())
    val cultivos: StateFlow<List<Cultivo>> = _cultivos.asStateFlow()

    private val _isLoading  = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error      = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _ultimoEscaneo = MutableStateFlow<UltimoEscaneoData?>(null)
    val ultimoEscaneo: StateFlow<UltimoEscaneoData?> = _ultimoEscaneo.asStateFlow()

    private val _cargandoEscaneo = MutableStateFlow(false)
    val cargandoEscaneo: StateFlow<Boolean> = _cargandoEscaneo.asStateFlow()

    private var snapshotListener: ListenerRegistration? = null

    // ── SharedPreferences cache ──────────────────────────────────────────────

    private fun clave() = "cultivos_${userId()}"

    /** Escribe la lista al disco. Siempre sincrónico (commit). */
    private fun escribirLocal(lista: List<Cultivo>) {
        val uid = userId()
        if (uid.isEmpty() || lista.isEmpty()) return
        prefs.edit().putString(clave(), gson.toJson(lista)).commit()
    }

    /** Lee la lista del disco. Devuelve lista vacía si no hay nada. */
    private fun leerLocal(): List<Cultivo> {
        val uid = userId()
        if (uid.isEmpty()) return emptyList()
        val json = prefs.getString(clave(), null) ?: return emptyList()
        return try {
            val tipo = object : TypeToken<List<Cultivo>>() {}.type
            gson.fromJson(json, tipo) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    /** Decide qué versión de un cultivo conservar: la que tiene el escaneo más reciente. */
    private fun tienEscaneo(c: Cultivo) =
        c.ultimoEscaneo.isNotBlank() && c.ultimoEscaneo != "Sin escaneos aún"

    private fun copiarEscaneoLocal(fc: Cultivo, lc: Cultivo) = fc.copy(
        estado           = lc.estado,
        ultimoEscaneo    = lc.ultimoEscaneo,
        humedadPromedio  = lc.humedadPromedio,
        nitrogenio       = lc.nitrogenio,
        fosforo          = lc.fosforo,
        potasio          = lc.potasio,
        plagasDetectadas = lc.plagasDetectadas,
        indiceSalud      = lc.indiceSalud
    )

    private fun mergeListas(
        firestore: List<Cultivo>,
        local: List<Cultivo>
    ): List<Cultivo> {
        // Si Firestore está vacío, devolver local directamente (sin borrar datos)
        if (firestore.isEmpty()) return local

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val firestoreIds = firestore.map { it.id }.toSet()

        val merged = firestore.map { fc ->
            val lc = local.find { it.id == fc.id } ?: return@map fc
            when {
                // local tiene escaneo, firestore no → conservar local
                tienEscaneo(lc) && !tienEscaneo(fc) -> copiarEscaneoLocal(fc, lc)
                // ambos tienen escaneo → el más reciente gana
                tienEscaneo(lc) && tienEscaneo(fc) -> {
                    val fechaL = runCatching { sdf.parse(lc.ultimoEscaneo) }.getOrNull()
                    val fechaF = runCatching { sdf.parse(fc.ultimoEscaneo) }.getOrNull()
                    if (fechaL != null && fechaF != null && fechaL > fechaF)
                        copiarEscaneoLocal(fc, lc)
                    else fc
                }
                else -> fc
            }
        }.toMutableList()

        // Incluir cultivos que sólo están en local (no sincronizados a Firestore todavía)
        local.filter { it.id !in firestoreIds }.forEach { merged.add(it) }

        return merged
    }

    // ────────────────────────────────────────────────────────────────────────

    private fun userId() = auth.currentUser?.uid ?: ""

    private fun cultivosRef() =
        db.collection("users").document(userId()).collection("cultivos")

    private val authStateListener = FirebaseAuth.AuthStateListener { fa ->
        if (fa.currentUser != null) {
            // Carga local primero para que la UI no quede en blanco
            val local = leerLocal()
            if (local.isNotEmpty()) _cultivos.value = local
            if (snapshotListener == null) cargarCultivos()
        } else {
            snapshotListener?.remove()
            snapshotListener = null
            _cultivos.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        if (auth.currentUser != null) {
            val local = leerLocal()
            if (local.isNotEmpty()) _cultivos.value = local
            cargarCultivos()
        }
    }

    fun cargarCultivos() {
        val uid = userId()
        if (uid.isEmpty()) return
        snapshotListener?.remove()
        _isLoading.value = true
        snapshotListener = cultivosRef().addSnapshotListener { snapshot, e ->
            _isLoading.value = false
            if (e != null) return@addSnapshotListener

            val firestoreLista = snapshot?.documents?.mapNotNull { doc ->
                Cultivo(
                    id               = doc.id,
                    nombre           = doc.getString("nombre")           ?: "",
                    tipoCultivo      = doc.getString("tipoCultivo")      ?: "",
                    variedadSemilla  = doc.getString("variedadSemilla")  ?: "",
                    hectareas        = doc.getDouble("hectareas")        ?: 0.0,
                    fechaSiembra     = doc.getString("fechaSiembra")     ?: "",
                    ubicacion        = doc.getString("ubicacion")        ?: "",
                    estado           = doc.getString("estado")           ?: EstadoCultivo.SIN_ESCANEO.name,
                    humedadPromedio  = (doc.getDouble("humedadPromedio") ?: 0.0).toFloat(),
                    nitrogenio       = (doc.getDouble("nitrogenio")      ?: 0.0).toFloat(),
                    fosforo          = (doc.getDouble("fosforo")         ?: 0.0).toFloat(),
                    potasio          = (doc.getDouble("potasio")         ?: 0.0).toFloat(),
                    indiceSalud      = (doc.getDouble("indiceSalud")     ?: 0.0).toFloat(),
                    plagasDetectadas = doc.getBoolean("plagasDetectadas") ?: false,
                    ultimoEscaneo    = doc.getString("ultimoEscaneo")    ?: ""
                )
            } ?: emptyList()

            // Merge: los datos locales de escaneo nunca son sobreescritos por Firestore
            val local = leerLocal()
            val merged = mergeListas(firestoreLista, local)

            _cultivos.value = if (merged.isNotEmpty()) merged else _cultivos.value
            if (merged.isNotEmpty()) escribirLocal(merged)
        }
    }

    fun agregarCultivo(
        nombre: String,
        tipoCultivo: String,
        variedadSemilla: String,
        fechaSiembra: String,
        hectareas: Double,
        ubicacion: String
    ) {
        val uid = userId()
        if (uid.isEmpty()) return
        viewModelScope.launch {
            try {
                cultivosRef().add(
                    hashMapOf(
                        "nombre"          to nombre,
                        "tipoCultivo"     to tipoCultivo,
                        "variedadSemilla" to variedadSemilla,
                        "fechaSiembra"    to fechaSiembra,
                        "hectareas"       to hectareas,
                        "ubicacion"       to ubicacion,
                        "estado"          to EstadoCultivo.SIN_ESCANEO.name,
                        "ultimoEscaneo"   to "Sin escaneos aún"
                    )
                ).await()
            } catch (e: Exception) {
                _error.value = "No se pudo guardar el cultivo. Intenta de nuevo."
            }
        }
    }

    fun eliminarCultivo(cultivoId: String) {
        viewModelScope.launch {
            try {
                cultivosRef().document(cultivoId).delete().await()
                // Eliminar también del caché local
                val actualizada = _cultivos.value.filter { it.id != cultivoId }
                _cultivos.value = actualizada
                escribirLocal(actualizada)
            } catch (e: Exception) {
                _error.value = "No se pudo eliminar el cultivo."
            }
        }
    }

    /** Guarda la ubicación "lat,lon" de la parcela (tomada del GPS del teléfono). */
    fun actualizarUbicacion(cultivoId: String, lat: Double, lon: Double) {
        if (userId().isEmpty() || cultivoId.isEmpty()) return
        val ubicacion = "%.6f,%.6f".format(Locale.US, lat, lon)
        val actualizada = _cultivos.value.map { c ->
            if (c.id == cultivoId) c.copy(ubicacion = ubicacion) else c
        }
        _cultivos.value = actualizada
        escribirLocal(actualizada)
        viewModelScope.launch {
            try {
                cultivosRef().document(cultivoId).update("ubicacion", ubicacion).await()
            } catch (_: Exception) {
                _error.value = "No se pudo guardar la ubicación. Intenta de nuevo."
            }
        }
    }

    fun cargarUltimoEscaneo(cultivoId: String) {
        if (userId().isEmpty() || cultivoId.isEmpty()) return
        viewModelScope.launch {
            _cargandoEscaneo.value = true
            _ultimoEscaneo.value = null
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val snapshot = cultivosRef().document(cultivoId)
                    .collection("escaneos").get().await()
                val doc = snapshot.documents
                    .sortedByDescending { runCatching { sdf.parse(it.getString("fecha") ?: "") }.getOrNull() }
                    .firstOrNull()
                if (doc != null) {
                    _ultimoEscaneo.value = UltimoEscaneoData(
                        fecha                   = doc.getString("fecha")                    ?: "",
                        confianzaIA             = (doc.getLong("confianzaIA")               ?: 0L).toInt(),
                        gpsLat                  = doc.getDouble("gpsLat")                   ?: 0.0,
                        gpsLon                  = doc.getDouble("gpsLon")                   ?: 0.0,
                        humedadSuelo            = (doc.getDouble("humedadSuelo")            ?: 0.0).toFloat(),
                        estresSuelo             = doc.getString("estresSuelo")              ?: "",
                        descripcionSuelo        = doc.getString("descripcionSuelo")         ?: "",
                        plagasDetectadas        = doc.getBoolean("plagasDetectadas")        ?: false,
                        descripcionPlagas       = doc.getString("descripcionPlagas")        ?: "",
                        nivelNitrogenio         = (doc.getDouble("nivelNitrogenio")         ?: 0.0).toFloat(),
                        nivelFosforo            = (doc.getDouble("nivelFosforo")            ?: 0.0).toFloat(),
                        nivelPotasio            = (doc.getDouble("nivelPotasio")            ?: 0.0).toFloat(),
                        descripcionNutrientes   = doc.getString("descripcionNutrientes")    ?: "",
                        recomendacionNutrientes = doc.getString("recomendacionNutrientes")  ?: ""
                    )
                }
            } catch (_: Exception) {
            } finally {
                _cargandoEscaneo.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
        auth.removeAuthStateListener(authStateListener)
    }
}
