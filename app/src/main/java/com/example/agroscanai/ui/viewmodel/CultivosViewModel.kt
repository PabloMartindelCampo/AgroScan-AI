package com.example.agroscanai.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agroscanai.data.ml.AnalizadorHojas
import com.example.agroscanai.data.ml.crearAnalizador
import com.example.agroscanai.data.model.CatalogoFrijol
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.Escaneo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.data.model.EstadoEscaneo
import com.example.agroscanai.data.model.OrigenFoto
import com.example.agroscanai.data.model.SIN_ESCANEOS
import com.example.agroscanai.utils.NotificacionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val _ultimoEscaneo = MutableStateFlow<Escaneo?>(null)
    val ultimoEscaneo: StateFlow<Escaneo?> = _ultimoEscaneo.asStateFlow()

    private val _cargandoEscaneo = MutableStateFlow(false)
    val cargandoEscaneo: StateFlow<Boolean> = _cargandoEscaneo.asStateFlow()

    private var snapshotListener: ListenerRegistration? = null

    // ── Datos heredados del dron simulado ────────────────────────────────────

    /**
     * Los cultivos con estados del esquema anterior (SALUDABLE, ALERTA, CRITICO)
     * vienen de escaneos simulados: se muestran como "Sin escaneo".
     */
    private fun normalizar(c: Cultivo): Cultivo {
        val valido = EstadoCultivo.entries.any { it.name == c.estado }
        return if (valido) c
        else c.copy(estado = EstadoCultivo.SIN_ESCANEO.name, diagnostico = "", ultimoEscaneo = SIN_ESCANEOS)
    }

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
            (gson.fromJson<List<Cultivo>>(json, tipo) ?: emptyList()).map { normalizar(it) }
        } catch (_: Exception) { emptyList() }
    }

    /** Decide qué versión de un cultivo conservar: la que tiene el escaneo más reciente. */
    private fun tienEscaneo(c: Cultivo) =
        c.ultimoEscaneo.isNotBlank() && c.ultimoEscaneo != SIN_ESCANEOS

    private fun copiarEscaneoLocal(fc: Cultivo, lc: Cultivo) = fc.copy(
        estado        = lc.estado,
        ultimoEscaneo = lc.ultimoEscaneo,
        diagnostico   = lc.diagnostico
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
                normalizar(Cultivo(
                    id               = doc.id,
                    nombre           = doc.getString("nombre")           ?: "",
                    tipoCultivo      = doc.getString("tipoCultivo")      ?: "",
                    variedadSemilla  = doc.getString("variedadSemilla")  ?: "",
                    hectareas        = doc.getDouble("hectareas")        ?: 0.0,
                    fechaSiembra     = doc.getString("fechaSiembra")     ?: "",
                    ubicacion        = doc.getString("ubicacion")        ?: "",
                    estado           = doc.getString("estado")           ?: EstadoCultivo.SIN_ESCANEO.name,
                    diagnostico      = doc.getString("diagnostico")      ?: "",
                    ultimoEscaneo    = doc.getString("ultimoEscaneo")    ?: ""
                ))
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
                        "ultimoEscaneo"   to SIN_ESCANEOS
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

    // ── Escaneos por foto ────────────────────────────────────────────────────

    private val analizador: AnalizadorHojas = crearAnalizador(application)

    private val _escaneos = MutableStateFlow<List<Escaneo>>(emptyList())
    val escaneos: StateFlow<List<Escaneo>> = _escaneos.asStateFlow()

    private val _escaneoDetalle = MutableStateFlow<Escaneo?>(null)
    val escaneoDetalle: StateFlow<Escaneo?> = _escaneoDetalle.asStateFlow()

    private val _registrandoEscaneo = MutableStateFlow(false)
    val registrandoEscaneo: StateFlow<Boolean> = _registrandoEscaneo.asStateFlow()

    /**
     * Registra la foto de una hoja: la analiza si hay modelo y guarda en Firestore
     * solo los metadatos (ruta local, fecha, GPS y resultado).
     */
    fun registrarEscaneoFoto(
        cultivoId: String,
        foto: File,
        origen: OrigenFoto,
        gps: Pair<Double, Double>?,
        onRegistrado: (Escaneo) -> Unit
    ) {
        if (userId().isEmpty() || cultivoId.isEmpty()) return
        viewModelScope.launch {
            _registrandoEscaneo.value = true
            val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val resultado = withContext(Dispatchers.Default) {
                runCatching { analizador.analizar(foto) }.getOrNull()
            }
            val clase = resultado?.let { CatalogoFrijol.porEtiqueta(it.etiqueta) }
            // Sin resultado del modelo (o con una etiqueta desconocida) queda pendiente
            val valido = resultado?.takeIf { clase != null }

            val escaneo = Escaneo(
                cultivoId      = cultivoId,
                fecha          = fecha,
                fotoRuta       = foto.absolutePath,
                origen         = origen,
                gpsLat         = gps?.first,
                gpsLon         = gps?.second,
                estadoGeneral  = if (valido != null) EstadoEscaneo.COMPLETADO else EstadoEscaneo.PENDIENTE,
                claseDetectada = clase?.etiquetaModelo ?: "",
                confianza      = valido?.probabilidad,
                modeloVersion  = valido?.modeloVersion ?: ""
            )
            val (nuevoEstado, diagnostico) = when {
                clase == null -> EstadoCultivo.PENDIENTE_ANALISIS to ""
                clase.etiquetaModelo == CatalogoFrijol.ETIQUETA_SANA -> EstadoCultivo.SANA to ""
                else -> EstadoCultivo.ENFERMEDAD to clase.nombre
            }

            // El id se genera localmente para no depender de la red
            val ref = cultivosRef().document(cultivoId).collection("escaneos").document()
            val guardado = escaneo.copy(id = ref.id)

            val actualizada = _cultivos.value.map { c ->
                if (c.id == cultivoId) c.copy(
                    estado        = nuevoEstado.name,
                    diagnostico   = diagnostico,
                    ultimoEscaneo = fecha
                ) else c
            }
            _cultivos.value = actualizada
            escribirLocal(actualizada)
            _ultimoEscaneo.value = guardado
            _escaneoDetalle.value = guardado
            _escaneos.value = listOf(guardado) + _escaneos.value.filter { it.cultivoId == cultivoId }

            NotificacionHelper.notificarEscaneoCompleto(
                context       = getApplication(),
                nombreCultivo = actualizada.find { it.id == cultivoId }?.nombre ?: "Cultivo",
                estado        = nuevoEstado,
                diagnostico   = diagnostico
            )

            _registrandoEscaneo.value = false
            onRegistrado(guardado)

            // Firestore sincroniza cuando haya red; la escritura queda en su caché local
            ref.set(hashMapOf(
                "fecha"          to guardado.fecha,
                "fotoRuta"       to guardado.fotoRuta,
                "origen"         to guardado.origen.name,
                "gpsLat"         to guardado.gpsLat,
                "gpsLon"         to guardado.gpsLon,
                "estadoGeneral"  to guardado.estadoGeneral.name,
                "claseDetectada" to guardado.claseDetectada,
                "confianza"      to guardado.confianza,
                "modeloVersion"  to guardado.modeloVersion
            )).addOnFailureListener { _error.value = "No se pudo sincronizar el escaneo." }
            cultivosRef().document(cultivoId).update(mapOf(
                "estado"        to nuevoEstado.name,
                "diagnostico"   to diagnostico,
                "ultimoEscaneo" to fecha
            ))
        }
    }

    /** Historial de escaneos por foto de una parcela, del más reciente al más antiguo. */
    fun cargarEscaneos(cultivoId: String) {
        if (userId().isEmpty() || cultivoId.isEmpty()) return
        viewModelScope.launch {
            _escaneos.value = emptyList()
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val snapshot = cultivosRef().document(cultivoId)
                    .collection("escaneos").get().await()
                _escaneos.value = snapshot.documents
                    .mapNotNull { docAEscaneo(cultivoId, it) }
                    .sortedByDescending { runCatching { sdf.parse(it.fecha) }.getOrNull() }
            } catch (_: Exception) { }
        }
    }

    fun cargarEscaneo(cultivoId: String, escaneoId: String) {
        if (userId().isEmpty() || cultivoId.isEmpty() || escaneoId.isEmpty()) return
        if (_escaneoDetalle.value?.id == escaneoId) return
        viewModelScope.launch {
            _escaneoDetalle.value = null
            try {
                val doc = cultivosRef().document(cultivoId)
                    .collection("escaneos").document(escaneoId).get().await()
                _escaneoDetalle.value = docAEscaneo(cultivoId, doc)
            } catch (_: Exception) { }
        }
    }

    /** Convierte un documento de `escaneos`. Los escaneos sin foto (simulados) se ignoran. */
    private fun docAEscaneo(cultivoId: String, doc: DocumentSnapshot): Escaneo? {
        val fotoRuta = doc.getString("fotoRuta") ?: return null
        if (fotoRuta.isBlank()) return null
        return Escaneo(
            id             = doc.id,
            cultivoId      = cultivoId,
            fecha          = doc.getString("fecha") ?: "",
            fotoRuta       = fotoRuta,
            origen         = runCatching {
                OrigenFoto.valueOf(doc.getString("origen") ?: "")
            }.getOrDefault(OrigenFoto.CAMARA),
            gpsLat         = doc.getDouble("gpsLat"),
            gpsLon         = doc.getDouble("gpsLon"),
            estadoGeneral  = runCatching {
                EstadoEscaneo.valueOf(doc.getString("estadoGeneral") ?: "")
            }.getOrDefault(EstadoEscaneo.PENDIENTE),
            claseDetectada = doc.getString("claseDetectada") ?: "",
            confianza      = doc.getDouble("confianza")?.toFloat(),
            modeloVersion  = doc.getString("modeloVersion") ?: ""
        )
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
                _ultimoEscaneo.value = snapshot.documents
                    .mapNotNull { docAEscaneo(cultivoId, it) }
                    .sortedByDescending { runCatching { sdf.parse(it.fecha) }.getOrNull() }
                    .firstOrNull()
            } catch (_: Exception) {
            } finally {
                _cargandoEscaneo.value = false
            }
        }
    }

    // ── Limpieza de datos simulados (NO se llama todavía) ────────────────────

    /**
     * Borra los datos de prueba del dron simulado del usuario actual:
     *  - en cada cultivo, los documentos de `escaneos` que no tienen `fotoRuta`;
     *  - los campos simulados del cultivo (humedad, N-P-K, índice de salud, etc.),
     *    dejando el estado en SIN_ESCANEO si venía del esquema anterior;
     *  - la copia local en SharedPreferences.
     * Pendiente de confirmación del responsable del proyecto antes de conectarla.
     */
    @Suppress("unused")
    private suspend fun limpiarDatosSimulados() {
        if (userId().isEmpty()) return
        val camposSimulados = listOf(
            "humedadPromedio", "temperatura", "nitrogenio", "fosforo",
            "potasio", "indiceSalud", "plagasDetectadas"
        )
        val cultivosDocs = cultivosRef().get().await().documents
        for (doc in cultivosDocs) {
            val escaneos = doc.reference.collection("escaneos").get().await().documents
            escaneos.filter { (it.getString("fotoRuta") ?: "").isBlank() }
                .forEach { it.reference.delete().await() }

            val cambios = mutableMapOf<String, Any>()
            camposSimulados.forEach { campo -> if (doc.contains(campo)) cambios[campo] = FieldValue.delete() }
            val estado = doc.getString("estado") ?: ""
            if (EstadoCultivo.entries.none { it.name == estado }) {
                cambios["estado"] = EstadoCultivo.SIN_ESCANEO.name
                cambios["ultimoEscaneo"] = SIN_ESCANEOS
            }
            if (cambios.isNotEmpty()) doc.reference.update(cambios).await()
        }
        prefs.edit().remove(clave()).commit()
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove()
        auth.removeAuthStateListener(authStateListener)
    }
}
