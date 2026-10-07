package com.example.agroscanai.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.R
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.components.colorEstado
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

// ─────────────────────────────────────────────────────────────────────────────
// Fuente de tiles de imágenes satelitales ESRI (gratuita, sin API key)
// ─────────────────────────────────────────────────────────────────────────────
private val ESRI_SATELITE = object : OnlineTileSourceBase(
    "ESRI_World_Imagery", 1, 19, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl +
            MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex)     + "/" +
            MapTileIndex.getX(pMapTileIndex)
}

private enum class PasoMapa { SELECCIONAR, VER_MAPA }

@Composable
fun MapaLotesScreen(
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos  by cultivosViewModel.cultivos.collectAsState()
    val isLoading by cultivosViewModel.isLoading.collectAsState()

    var paso                by remember { mutableStateOf(PasoMapa.SELECCIONAR) }
    var cultivoSeleccionado by remember { mutableStateOf<Cultivo?>(null) }

    Scaffold(
        containerColor = Color(0xFFF5F7F5),
        bottomBar = {
            AgroBottomBar(
                onHomeClick           = onHomeClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick         = onPerfilClick,
                paginaActual          = PaginaActual.NINGUNA
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // ── Encabezado ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .shadow(2.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ArrowBack, "Regresar",
                    tint = VerdeBosque,
                    modifier = Modifier.size(26.dp).clickable {
                        if (paso == PasoMapa.VER_MAPA) {
                            paso = PasoMapa.SELECCIONAR
                            cultivoSeleccionado = null
                        } else onHomeClick()
                    }
                )
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.logo_agroscan), "Logo",
                        modifier = Modifier.size(32.dp))
                    Text("AgroScan AI", fontSize = 8.sp,
                        fontWeight = FontWeight.Bold, color = VerdeBosque)
                }
            }

            HorizontalDivider(color = Color(0xFFE8ECE8))

            when (paso) {
                PasoMapa.SELECCIONAR -> SeleccionarLoteStep(
                    cultivos = cultivos, isLoading = isLoading,
                    onSeleccionar = { cultivoSeleccionado = it; paso = PasoMapa.VER_MAPA }
                )
                PasoMapa.VER_MAPA -> {
                    val actual = cultivos.find { it.id == cultivoSeleccionado?.id } ?: cultivoSeleccionado!!
                    MapaLoteStep(
                        cultivo = actual,
                        onUbicacionObtenida = { lat, lon ->
                            cultivosViewModel.actualizarUbicacion(actual.id, lat, lon)
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 1 – Lista de lotes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SeleccionarLoteStep(
    cultivos: List<Cultivo>,
    isLoading: Boolean,
    onSeleccionar: (Cultivo) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdeEsmeralda)
            }
            cultivos.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Map, null,
                    tint = VerdeEsmeralda.copy(alpha = 0.4f), modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(16.dp))
                Text("Sin parcelas registradas",
                    fontSize = 16.sp, color = GrisMedio, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Agrega un cultivo en \"Mis Cultivos\" para ver su mapa.",
                    fontSize = 13.sp, color = GrisMedio.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cultivos, key = { it.id }) { c ->
                    LoteCard(cultivo = c, onClick = { onSeleccionar(c) })
                }
                item {
                    Text("AgroScan AI es una marca registrada. Todos los derechos reservados.",
                        fontSize = 10.sp, color = GrisMedio.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun LoteCard(cultivo: Cultivo, onClick: () -> Unit) {
    val estadoColor = colorEstado(cultivo.estadoEnum())
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(VerdeEsmeralda),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_cultivos), null,
                    modifier = Modifier.size(32.dp), contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(cultivo.nombre)
                        if (cultivo.tipoCultivo.isNotBlank()) append(" - ${cultivo.tipoCultivo}")
                    },
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(estadoColor))
                    Spacer(Modifier.width(5.dp))
                    Text(if (cultivo.ubicacion.isNotBlank()) "Ver mapa" else "Sin ubicación registrada",
                        fontSize = 12.sp, color = GrisMedio)
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(width = 72.dp, height = 56.dp)
                    .clip(RoundedCornerShape(10.dp)).background(VerdeEsmeralda.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_cultivos), null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop, alpha = 0.85f)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PASO 2 – Mapa del lote con OSMDroid
// ─────────────────────────────────────────────────────────────────────────────

/** Lee la ubicación guardada como "lat,lon". Devuelve null si no hay una válida. */
private fun parsearUbicacion(ubicacion: String): Pair<Double, Double>? {
    val parts = ubicacion.split(",").mapNotNull { it.trim().toDoubleOrNull() }
    if (parts.size != 2) return null
    val (lat, lon) = parts
    if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return lat to lon
}

@Composable
private fun MapaLoteStep(
    cultivo: Cultivo,
    onUbicacionObtenida: (Double, Double) -> Unit
) {
    var panelActivo by remember { mutableStateOf<String?>(null) }
    val coordenadas = remember(cultivo.ubicacion) { parsearUbicacion(cultivo.ubicacion) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Tarjeta info del cultivo ────────────────────────────────────────
        val estadoColor = colorEstado(cultivo.estadoEnum())
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Map, null, tint = VerdeEsmeralda, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        buildString {
                            append(cultivo.nombre)
                            if (cultivo.tipoCultivo.isNotBlank()) append(" · ${cultivo.tipoCultivo}")
                        },
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VerdeBosque,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
                            append("${cultivo.hectareas} ha")
                            if (cultivo.tieneEscaneo())
                                append(" · Escaneo: ${cultivo.ultimoEscaneo}")
                        },
                        fontSize = 11.sp, color = GrisMedio
                    )
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(estadoColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(cultivo.estadoTexto(), fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = estadoColor)
                }
            }
        }

        // ── Mapa OSMDroid (solo si hay ubicación real) ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (coordenadas == null) {
                SinUbicacion(onUbicacionObtenida = onUbicacionObtenida)
            } else {
                val (lat, lon) = coordenadas
                OsmMapaLote(
                    lat = lat, lon = lon,
                    cultivo = cultivo,
                    modifier = Modifier.fillMaxSize()
                )

                // Coordenadas GPS
                if (panelActivo == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp, bottom = 10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("GPS: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}",
                            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Panel overlay
            if (panelActivo != null) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { panelActivo = null },
                    contentAlignment = Alignment.Center
                ) {
                    MapaInfoPanel(cultivo = cultivo, onClose = { panelActivo = null })
                }
            }
        }

        // ── Botones ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MapaBotonCircular("Información", painterResource(R.drawable.ic_satelite),
                isActive = panelActivo == "info", hasAlert = false,
                onClick = { panelActivo = if (panelActivo == "info") null else "info" })
        }

        Text("AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 9.sp, color = GrisMedio.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Parcela sin ubicación: ofrecer fijarla con el GPS del teléfono
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("MissingPermission")
@Composable
private fun SinUbicacion(onUbicacionObtenida: (Double, Double) -> Unit) {
    val context = LocalContext.current
    var buscando by remember { mutableStateOf(false) }
    var mensaje  by remember { mutableStateOf<String?>(null) }

    fun obtenerUbicacion() {
        buscando = true
        mensaje = null
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                buscando = false
                if (loc != null) onUbicacionObtenida(loc.latitude, loc.longitude)
                else mensaje = "No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo."
            }
            .addOnFailureListener {
                buscando = false
                mensaje = "No se pudo obtener la ubicación. Activa el GPS e intenta de nuevo."
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) obtenerUbicacion()
        else mensaje = "Se necesita permiso de ubicación para fijar la parcela en el mapa."
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.LocationOff, null,
            tint = VerdeEsmeralda.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Esta parcela no tiene ubicación registrada",
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = GrisHumo,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Párate dentro de la parcela y usa tu GPS para fijar su ubicación.",
            fontSize = 13.sp, color = GrisMedio, textAlign = TextAlign.Center, lineHeight = 19.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val tienePermiso = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (tienePermiso) obtenerUbicacion()
                else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            enabled = !buscando,
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBosque)
        ) {
            if (buscando) {
                CircularProgressIndicator(color = Color.White,
                    modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Obteniendo ubicación...")
            } else {
                Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Fijar ubicación con mi GPS")
            }
        }
        mensaje?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, fontSize = 12.sp, color = RojoAlerta, textAlign = TextAlign.Center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composable: MapView de OSMDroid con el marcador del lote
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OsmMapaLote(
    lat: Double,
    lon: Double,
    cultivo: Cultivo,
    modifier: Modifier = Modifier
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val centro        = remember(lat, lon) { GeoPoint(lat, lon) }

    val mapView = remember(context) {
        // Configurar OSMDroid antes de crear el mapa
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidTileCache = java.io.File(context.cacheDir, "osmdroid_tiles")
        }

        MapView(context).apply {
            setTileSource(ESRI_SATELITE)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true

            controller.apply {
                setZoom(17.0)
                setCenter(centro)
            }

            // Marcador del lote
            val marker = Marker(this).apply {
                position = centro
                title    = cultivo.nombre
                snippet  = "${cultivo.estadoTexto()} · ${cultivo.hectareas} ha"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            overlays.add(marker)
        }
    }

    // Ciclo de vida del MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE  -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

// ─────────────────────────────────────────────────────────────────────────────
// Botones circulares
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaBotonCircular(
    label: String,
    painter: androidx.compose.ui.graphics.painter.Painter,
    isActive: Boolean,
    hasAlert: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier.size(110.dp).clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painter, contentDescription = label,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            if (hasAlert) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                        .background(Color(0xFFE53935)).align(Alignment.TopEnd)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = VerdeBosque, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panel de información
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapaInfoPanel(cultivo: Cultivo, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(0.88f).clickable(enabled = false) {},
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Información de la parcela",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                Icon(Icons.Filled.Close, "Cerrar", tint = GrisMedio,
                    modifier = Modifier.size(20.dp).clickable { onClose() })
            }
            Spacer(Modifier.height(14.dp))
            InfoRow("Parcela",        cultivo.nombre)
            InfoRow("Tipo",           cultivo.tipoCultivo.ifBlank { "—" })
            InfoRow("Área",           "${cultivo.hectareas} hectáreas")
            InfoRow("Último escaneo", if (cultivo.tieneEscaneo()) cultivo.ultimoEscaneo else "Sin escaneos aún")
            InfoRow("Estado",         cultivo.estadoTexto())
            InfoRow("Coordenadas",    cultivo.ubicacion.ifBlank { "Sin ubicación" })
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = GrisMedio)
        Text(value, fontSize = 12.sp, color = GrisHumo, fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(max = 180.dp), textAlign = TextAlign.End)
    }
}
