package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.Escaneo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.data.model.EstadoEscaneo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.components.RecomendacionCultivo
import com.example.agroscanai.ui.components.colorEstado
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel

@Composable
fun DetalleCultivoScreen(
    cultivoId: String,
    onBackClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onCultivoEliminado: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos        by cultivosViewModel.cultivos.collectAsState()
    val ultimoEscaneo   by cultivosViewModel.ultimoEscaneo.collectAsState()
    val cargandoEscaneo by cultivosViewModel.cargandoEscaneo.collectAsState()

    val cultivo = cultivos.find { it.id == cultivoId }

    var showDeleteDialog  by remember { mutableStateOf(false) }
    var expandedEscaneo   by remember { mutableStateOf(false) }

    LaunchedEffect(cultivoId) {
        cultivosViewModel.cargarUltimoEscaneo(cultivoId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Filled.Delete, null, tint = RojoAlerta, modifier = Modifier.size(30.dp)) },
            title = { Text("Eliminar cultivo", fontWeight = FontWeight.Bold) },
            text  = { Text("¿Estás seguro de que deseas eliminar '${cultivo?.nombre}'? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        cultivosViewModel.eliminarCultivo(cultivoId)
                        onCultivoEliminado()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta),
                    shape  = RoundedCornerShape(10.dp)
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = GrisHumo)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (cultivo == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VerdeBosque)
        }
        return
    }

    val estadoColor = colorEstado(cultivo.estadoEnum())
    val headerTop = when (cultivo.estadoEnum()) {
        EstadoCultivo.ENFERMEDAD         -> Color(0xFF8B0000)
        EstadoCultivo.PENDIENTE_ANALISIS -> Color(0xFF0D47A1)
        EstadoCultivo.SANA               -> VerdeBosqueOscuro
        EstadoCultivo.SIN_ESCANEO        -> Color(0xFF455A64)
    }
    val headerBot = when (cultivo.estadoEnum()) {
        EstadoCultivo.ENFERMEDAD         -> Color(0xFFD32F2F)
        EstadoCultivo.PENDIENTE_ANALISIS -> Color(0xFF1565C0)
        EstadoCultivo.SANA               -> VerdeBosque
        EstadoCultivo.SIN_ESCANEO        -> Color(0xFF607D8B)
    }

    Scaffold(
        bottomBar = {
            AgroBottomBar(
                onHomeClick = onHomeClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick = onPerfilClick
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(headerTop, headerBot)))
                .padding(bottom = 28.dp)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val estadoLabel = cultivo.estadoTexto()
                val badgeBg = when (cultivo.estadoEnum()) {
                    EstadoCultivo.SANA               -> Color(0xFF1B5E20)
                    EstadoCultivo.PENDIENTE_ANALISIS -> Color(0xFF0D47A1)
                    EstadoCultivo.ENFERMEDAD         -> Color(0xFFB71C1C)
                    EstadoCultivo.SIN_ESCANEO        -> Color(0xFF37474F)
                }
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(estadoLabel, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(cultivo.nombre, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (cultivo.tipoCultivo.isNotBlank()) {
                    Text(
                        "${cultivo.tipoCultivo}${if (cultivo.variedadSemilla.isNotBlank()) " · ${cultivo.variedadSemilla}" else ""}",
                        fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height((-16).dp))

        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape     = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("Área",           "${cultivo.hectareas} ha", Color(0xFF6A1B9A))
                    MiniStat("Último escaneo", if (cultivo.tieneEscaneo()) cultivo.ultimoEscaneo else "—", GrisHumo)
                }
                Spacer(Modifier.height(12.dp))
                Text(cultivo.descripcionSalud(), fontSize = 12.sp, color = estadoColor,
                    textAlign = TextAlign.Center, lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Recomendación", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                Spacer(Modifier.height(12.dp))
                RecomendacionCultivo(cultivo = cultivo)
            }
        }

        Spacer(Modifier.height(12.dp))

         Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Información del cultivo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                Spacer(Modifier.height(12.dp))
                InfoFilaCultivo(Icons.Filled.Grass,         "Tipo de cultivo", cultivo.tipoCultivo.ifBlank { "—" },             VerdeBosque)
                if (cultivo.variedadSemilla.isNotBlank())
                    InfoFilaCultivo(Icons.Filled.LocalFlorist,  "Variedad",        cultivo.variedadSemilla,                         VerdeEsmeralda)
                InfoFilaCultivo(Icons.Filled.Straighten,    "Extensión",       "${cultivo.hectareas} hectáreas",                Color(0xFF6A1B9A))
                if (cultivo.fechaSiembra.isNotBlank())
                    InfoFilaCultivo(Icons.Filled.CalendarToday, "Fecha de siembra", cultivo.fechaSiembra,                         Color(0xFF1565C0))
                if (cultivo.ubicacion.isNotBlank())
                    InfoFilaCultivo(Icons.Filled.LocationOn,    "Ubicación",       cultivo.ubicacion,                             Color(0xFFE65100))
                InfoFilaCultivo(Icons.Filled.Schedule,      "Último escaneo",  if (cultivo.tieneEscaneo()) cultivo.ultimoEscaneo else "Sin escaneos aún", GrisMedio)
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expandedEscaneo = !expandedEscaneo },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Analytics, null, tint = VerdeBosque, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Detalle del último escaneo",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (expandedEscaneo) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        null, tint = GrisMedio
                    )
                }

                if (expandedEscaneo) {
                    Spacer(Modifier.height(14.dp))
                    when {
                        cargandoEscaneo -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = VerdeBosque, modifier = Modifier.size(26.dp))
                        }
                        ultimoEscaneo != null -> EscaneoDetalleContent(ultimoEscaneo!!)
                        else -> Text(
                            "Esta parcela aún no tiene escaneos por foto registrados.",
                            fontSize = 13.sp, color = GrisMedio,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

         OutlinedButton(
            onClick  = { showDeleteDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RojoAlerta),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, RojoAlerta)
        ) {
            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Eliminar cultivo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(32.dp))
    }
    }
}


@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = GrisMedio)
    }
}

@Composable
private fun InfoFilaCultivo(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = GrisMedio)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun EscaneoDetalleContent(esc: Escaneo) {
    val analizado = esc.estadoGeneral == EstadoEscaneo.COMPLETADO
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (esc.fecha.isNotBlank())
            EscaneoFila("Fecha del escaneo", esc.fecha, Icons.Filled.Schedule, GrisMedio)
        EscaneoFila(
            "Análisis",
            if (analizado) "Analizada por el modelo" else "Pendiente de análisis",
            Icons.Filled.Analytics, Color(0xFF1565C0)
        )
        EscaneoFila(
            "Coordenadas GPS",
            if (esc.gpsLat != null && esc.gpsLon != null)
                "Lat: %.5f  Lon: %.5f".format(esc.gpsLat, esc.gpsLon)
            else "No disponible",
            Icons.Filled.LocationOn, Color(0xFF6A1B9A)
        )
    }
}

@Composable
private fun EscaneoFila(label: String, value: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(17.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = GrisMedio, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
        }
    }
}
