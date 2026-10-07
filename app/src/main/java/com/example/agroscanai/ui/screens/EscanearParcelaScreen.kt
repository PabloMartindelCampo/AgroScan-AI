package com.example.agroscanai.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.R
import com.example.agroscanai.data.model.CatalogoFrijol
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.Escaneo
import com.example.agroscanai.data.model.EstadoEscaneo
import com.example.agroscanai.data.model.OrigenFoto
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.FotoEscaneo
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.components.colorEstado
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import com.example.agroscanai.utils.FotosEscaneo
import kotlinx.coroutines.launch
import java.io.File


private enum class PasoEscaneo { SELECCIONAR_PARCELA, ESCANEAR }

@Composable
fun EscanearParcelaScreen(
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    onVerEscaneo: (cultivoId: String, escaneoId: String) -> Unit = { _, _ -> },
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos     by cultivosViewModel.cultivos.collectAsState()
    val isLoading    by cultivosViewModel.isLoading.collectAsState()

    // rememberSaveable: Android puede cerrar la app mientras la cámara está abierta
    var paso                  by rememberSaveable { mutableStateOf(PasoEscaneo.SELECCIONAR_PARCELA) }
    var cultivoSeleccionadoId by rememberSaveable { mutableStateOf<String?>(null) }
    val cultivoSeleccionado   = cultivos.find { it.id == cultivoSeleccionadoId }

    Scaffold(
        bottomBar = {
            AgroBottomBar(
                onHomeClick = onHomeClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick = onPerfilClick
            )
        }
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(innerPadding)) {

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).shadow(2.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ArrowBack, "Regresar", tint = VerdeBosque,
                modifier = Modifier.size(26.dp).clickable {
                    if (paso == PasoEscaneo.ESCANEAR) {
                        paso = PasoEscaneo.SELECCIONAR_PARCELA
                        cultivoSeleccionadoId = null
                    } else {
                        onHomeClick()
                    }
                }
            )
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.logo_agroscan), "Logo",
                    modifier = Modifier.size(32.dp))
                Text("AgroScan AI", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                    color = VerdeBosque, lineHeight = 10.sp)
            }
        }

        HorizontalDivider(color = Color(0xFFE8ECE8), thickness = 1.dp)

        when {
            paso == PasoEscaneo.ESCANEAR && cultivoSeleccionado != null -> {
                EscaneoStep(
                    cultivo = cultivoSeleccionado,
                    cultivosViewModel = cultivosViewModel,
                    onVerEscaneo = onVerEscaneo,
                    onCambiarParcela = {
                        cultivoSeleccionadoId = null
                        paso = PasoEscaneo.SELECCIONAR_PARCELA
                    }
                )
            }
            else -> {
                SeleccionarParcelaStep(
                    cultivos = cultivos,
                    isLoading = isLoading,
                    onSeleccionar = { cultivo ->
                        cultivoSeleccionadoId = cultivo.id
                        paso = PasoEscaneo.ESCANEAR
                    }
                )
            }
        }
    }
    }
}

@Composable
private fun SeleccionarParcelaStep(
    cultivos: List<Cultivo>,
    isLoading: Boolean,
    onSeleccionar: (Cultivo) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text("Escanear Parcela", fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = VerdeBosque)
            Text("Selecciona la parcela que deseas escanear",
                fontSize = 13.sp, color = GrisMedio)
        }

        HorizontalDivider(color = Color(0xFFE8ECE8), thickness = 1.dp)

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdeEsmeralda)
                }
            }
            cultivos.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Grass, null, tint = GrisMedio,
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Sin parcelas registradas", fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, color = GrisHumo)
                    Spacer(Modifier.height(8.dp))
                    Text("Primero agrega un cultivo en\n\"Mis Cultivos\" para poder escanearlo.",
                        fontSize = 13.sp, color = GrisMedio, textAlign = TextAlign.Center,
                        lineHeight = 20.sp)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cultivos) { cultivo ->
                        ParcelaSeleccionCard(cultivo = cultivo, onClick = { onSeleccionar(cultivo) })
                    }
                    item {
                        Text(
                            "AgroScan AI es una marca registrada. Todos los derechos reservados.",
                            fontSize = 11.sp, color = GrisMedio, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelaSeleccionCard(cultivo: Cultivo, onClick: () -> Unit) {
    val estadoColor = colorEstado(cultivo.estadoEnum())

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(VerdeEsmeralda.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Grass, null, tint = VerdeEsmeralda,
                    modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(cultivo.nombre, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold, color = VerdeBosque)
                Text(
                    buildString {
                        append(cultivo.tipoCultivo)
                        if (cultivo.variedadSemilla.isNotBlank()) append(" · ${cultivo.variedadSemilla}")
                    },
                    fontSize = 12.sp, color = GrisMedio
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Straighten, null, tint = GrisMedio,
                            modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${cultivo.hectareas} ha", fontSize = 11.sp, color = GrisMedio)
                    }
                    if (cultivo.ubicacion.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = GrisMedio,
                                modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(cultivo.ubicacion, fontSize = 11.sp, color = GrisMedio,
                                maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(estadoColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(cultivo.estadoTexto(), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = estadoColor)
                }
                Spacer(Modifier.height(8.dp))
                Icon(Icons.Filled.ChevronRight, null, tint = GrisMedio,
                    modifier = Modifier.size(22.dp))
            }
        }
    }
}


@Composable
private fun EscaneoStep(
    cultivo: Cultivo,
    cultivosViewModel: CultivosViewModel,
    onVerEscaneo: (cultivoId: String, escaneoId: String) -> Unit,
    onCambiarParcela: () -> Unit
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val registrando by cultivosViewModel.registrandoEscaneo.collectAsState()

    var rutaCamara  by rememberSaveable { mutableStateOf<String?>(null) }
    var preparando  by remember { mutableStateOf(false) }
    var registrado  by remember { mutableStateOf<Escaneo?>(null) }
    var error       by remember { mutableStateOf<String?>(null) }
    val ocupado     = registrando || preparando

    fun registrar(foto: File, origen: OrigenFoto, gps: Pair<Double, Double>?) {
        cultivosViewModel.registrarEscaneoFoto(cultivo.id, foto, origen, gps) { registrado = it }
    }

    val camaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val archivo = rutaCamara?.let { File(it) }
        rutaCamara = null
        if (ok && archivo != null && archivo.length() > 0) {
            preparando = true
            scope.launch {
                // GPS del teléfono en el momento de la foto (null si no hay permiso o señal)
                val gps = FotosEscaneo.ubicacionActual(context)
                preparando = false
                registrar(archivo, OrigenFoto.CAMARA, gps)
            }
        } else {
            archivo?.delete()
        }
    }

    val galeriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        preparando = true
        scope.launch {
            val archivo = FotosEscaneo.copiarDesdeGaleria(context, uri)
            preparando = false
            if (archivo == null) error = "No se pudo copiar la imagen elegida."
            // Ubicación solo si la foto la trae en su EXIF; nunca la del teléfono ahora
            else registrar(archivo, OrigenFoto.GALERIA, FotosEscaneo.gpsDeExif(archivo))
        }
    }

    fun abrirCamara() {
        error = null
        val archivo = FotosEscaneo.nuevoArchivo(context)
        rutaCamara = archivo.absolutePath
        try {
            camaraLauncher.launch(FotosEscaneo.uriParaCamara(context, archivo))
        } catch (_: ActivityNotFoundException) {
            rutaCamara = null
            error = "No se encontró una app de cámara en el teléfono."
        }
    }

    // Se pide la ubicación para registrar dónde se tomó la foto; si se niega, la foto se guarda sin GPS
    val permisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { abrirCamara() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Grass, null, tint = VerdeEsmeralda,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cultivo.nombre, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, color = VerdeBosque)
                    Text(
                        buildString {
                            append(cultivo.tipoCultivo)
                            if (cultivo.ubicacion.isNotBlank()) append(" · ${cultivo.ubicacion}")
                            append(" · ${cultivo.hectareas} ha")
                        },
                        fontSize = 12.sp, color = GrisMedio
                    )
                }
                if (!ocupado) {
                    TextButton(
                        onClick = onCambiarParcela,
                        colors = ButtonDefaults.textButtonColors(contentColor = VerdeEsmeralda),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Foto ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .height(260.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2D5A27))
        ) {
            val foto = registrado
            if (foto != null) {
                FotoEscaneo(ruta = foto.fotoRuta, modifier = Modifier.fillMaxSize())
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_cultivos),
                    contentDescription = "Campo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.4f
                )
                Column(modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PhotoCamera, null, tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Toma una foto de una sola hoja, de cerca y con buena luz",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp,
                        textAlign = TextAlign.Center)
                }
            }
            if (ocupado) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Guardando foto...", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Acciones ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoCircleButton(
                label = if (registrado != null) "Tomar otra foto" else "Tomar foto",
                painter = painterResource(R.drawable.ic_escanear),
                enabled = !ocupado,
                isActive = false,
                hasAlert = false,
                isLoading = ocupado,
                onClick = {
                    val tienePermiso = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (tienePermiso) abrirCamara()
                    else permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    error = null
                    galeriaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeBosque),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VerdeBosque)
            ) {
                Icon(Icons.Filled.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Elegir de la galería", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        error?.let { err ->
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3)),
                shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, null, tint = RojoAlerta,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(err, fontSize = 12.sp, color = RojoAlerta, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.Close, "Cerrar", tint = GrisMedio,
                        modifier = Modifier.size(18.dp).clickable { error = null })
                }
            }
        }

        // ── Resultado del registro ───────────────────────────────────────────
        registrado?.let { esc ->
            Spacer(Modifier.height(16.dp))
            ResultadoRegistro(
                escaneo = esc,
                nombreCultivo = cultivo.nombre,
                onVerDetalle = { onVerEscaneo(cultivo.id, esc.id) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 11.sp, color = GrisMedio, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ResultadoRegistro(
    escaneo: Escaneo,
    nombreCultivo: String,
    onVerDetalle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clase = CatalogoFrijol.porEtiqueta(escaneo.claseDetectada)
    val analizado = escaneo.estadoGeneral == EstadoEscaneo.COMPLETADO && clase != null
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = VerdeEsmeralda,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Foto guardada en \"$nombreCultivo\"",
                    fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            ResultadoRow(Icons.Filled.Schedule, "Fecha", escaneo.fecha, GrisHumo)
            ResultadoRow(
                Icons.Filled.LocationOn, "Ubicación",
                if (escaneo.gpsLat != null && escaneo.gpsLon != null)
                    "%.5f, %.5f".format(escaneo.gpsLat, escaneo.gpsLon)
                else "No disponible",
                GrisHumo
            )
            ResultadoRow(
                Icons.Filled.Analytics, "Resultado",
                if (analizado) clase!!.nombre else "Pendiente de análisis",
                if (analizado) VerdeBosque else Color(0xFF1565C0)
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onVerDetalle,
                colors = ButtonDefaults.textButtonColors(contentColor = VerdeEsmeralda),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Text("Ver detalle del escaneo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ResultadoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String, color: Color
) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 12.sp, color = GrisHumo, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InfoCircleButton(
    label: String,
    painter: androidx.compose.ui.graphics.painter.Painter,
    enabled: Boolean,
    isActive: Boolean,
    hasAlert: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier.size(110.dp)
                    .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = VerdeEsmeralda,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Image(
                        painter = painter,
                        contentDescription = label,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            if (hasAlert && enabled) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(RojoAlerta)
                        .align(Alignment.TopEnd).border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = if (enabled) VerdeBosque else GrisMedio)
    }
}
