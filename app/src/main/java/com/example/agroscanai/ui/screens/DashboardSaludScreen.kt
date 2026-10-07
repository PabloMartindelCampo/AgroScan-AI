package com.example.agroscanai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.Escaneo
import com.example.agroscanai.data.model.EstadoEscaneo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.RecomendacionCultivo
import com.example.agroscanai.ui.components.colorEstado
import com.example.agroscanai.ui.components.iconoEstado
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────────────
//  Main Screen
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardSaludScreen(
    cultivoId: String,
    onHomeClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos        by cultivosViewModel.cultivos.collectAsState()
    val ultimoEscaneo   by cultivosViewModel.ultimoEscaneo.collectAsState()
    val cargandoEscaneo by cultivosViewModel.cargandoEscaneo.collectAsState()

    val cultivo = cultivos.find { it.id == cultivoId }

    LaunchedEffect(cultivoId) {
        cultivosViewModel.cargarUltimoEscaneo(cultivoId)
    }

    var mostrarDetallado by remember { mutableStateOf(false) }

    if (cultivo == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = VerdeBosque)
        }
        return
    }

    val estado         = cultivo.estadoEnum()
    val colorPrincipal = colorEstado(estado)

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
            .background(Color(0xFFF5F7F5))
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .shadow(2.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = VerdeBosque)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Dashboard de Salud",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = VerdeBosque
            )
        }

        // ── Lote seleccionado ────────────────────────────────────────────────
        Text(
            text = "Lote seleccionado: ${cultivo.nombre} · ${cultivo.tipoCultivo}",
            fontSize = 13.sp,
            color = GrisHumo,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // ── Estado del cultivo ───────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Estado del Cultivo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(colorPrincipal.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconoEstado(estado), contentDescription = null,
                        tint = colorPrincipal, modifier = Modifier.size(52.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = cultivo.estadoTexto(),
                    fontSize = 15.sp,
                    color = colorPrincipal,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    cultivo.descripcionSalud(),
                    fontSize = 13.sp,
                    color = GrisMedio,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                if (cultivo.tieneEscaneo()) {
                    Text(
                        text = "Último escaneo: ${cultivo.ultimoEscaneo}",
                        fontSize = 11.sp,
                        color = GrisMedio,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // ── Recomendación ────────────────────────────────────────────────────
        Spacer(Modifier.height(6.dp))
        Box(Modifier.padding(horizontal = 16.dp)) {
            DetalleCard(titulo = "Recomendación", icono = Icons.Filled.Lightbulb) {
                RecomendacionCultivo(cultivo = cultivo)
            }
        }

        // ── Botón Dashboard Detallado ────────────────────────────────────────
        Button(
            onClick = { mostrarDetallado = !mostrarDetallado },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBosque),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (mostrarDetallado) "Ocultar Dashboard detallado" else "Ver Dashboard detallado",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (mostrarDetallado) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Sección Detallada (expandible) ───────────────────────────────────
        AnimatedVisibility(
            visible = mostrarDetallado,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(tween(300)),
            exit  = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(200))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (cargandoEscaneo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = VerdeBosque)
                            Spacer(Modifier.height(10.dp))
                            Text("Cargando datos del escaneo…", fontSize = 13.sp, color = GrisMedio)
                        }
                    }
                } else {
                    DashboardDetallado(cultivo = cultivo, ultimoEscaneo = ultimoEscaneo)
                }
            }
        }

        // ── Footer ───────────────────────────────────────────────────────────
        Text(
            text = "AgroScan AI es una marca registrada. Todos los derechos reservados",
            fontSize = 11.sp,
            color = GrisMedio,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )
    } // end Column
    } // end Scaffold content
}

@Composable
private fun DashboardDetallado(
    cultivo: Cultivo,
    ultimoEscaneo: Escaneo?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFDDDDDD))
            Text(
                "  Análisis Detallado  ",
                fontSize = 11.sp,
                color = GrisMedio,
                fontWeight = FontWeight.Medium
            )
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFDDDDDD))
        }

        Spacer(Modifier.height(10.dp))

        DetalleCard(
            titulo = "Datos del Último Escaneo",
            icono  = Icons.Filled.PhotoCamera
        ) {
            if (ultimoEscaneo == null) {
                Text(
                    "Esta parcela aún no tiene escaneos por foto registrados.",
                    fontSize = 13.sp,
                    color = GrisMedio
                )
            } else {
                val gpsText = if (ultimoEscaneo.gpsLat != null && ultimoEscaneo.gpsLon != null)
                    "%.6f, %.6f".format(ultimoEscaneo.gpsLat, ultimoEscaneo.gpsLon)
                else "No disponible"
                val analizado = ultimoEscaneo.estadoGeneral == EstadoEscaneo.COMPLETADO

                DatoTecnicoItem(Icons.Filled.CalendarMonth, "Fecha de escaneo", ultimoEscaneo.fecha.ifBlank { "—" }, GrisHumo)
                DatoTecnicoItem(Icons.Filled.LocationOn,    "Coordenadas GPS",  gpsText,                            Color(0xFF6A1B9A))
                DatoTecnicoItem(
                    Icons.Filled.Analytics, "Análisis",
                    if (analizado) "Analizada por el modelo" else "Pendiente de análisis",
                    Color(0xFF1565C0)
                )
                // La confianza solo se muestra si la dio el modelo
                if (analizado && ultimoEscaneo.confianza != null) {
                    DatoTecnicoItem(
                        Icons.Filled.SmartToy, "Probabilidad del modelo",
                        "${(ultimoEscaneo.confianza * 100).roundToInt()}%", Color(0xFF1565C0)
                    )
                }
                if (analizado && ultimoEscaneo.modeloVersion.isNotBlank()) {
                    DatoTecnicoItem(Icons.Filled.Memory, "Versión del modelo", ultimoEscaneo.modeloVersion, GrisHumo)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        DetalleCard(
            titulo = "Datos de la Parcela",
            icono  = Icons.Filled.Agriculture
        ) {
            DatoTecnicoItem(Icons.Filled.Landscape,   "Superficie", "${cultivo.hectareas} hectáreas", VerdeBosque)
            DatoTecnicoItem(Icons.Filled.Agriculture, "Variedad",   cultivo.variedadSemilla.ifBlank { cultivo.tipoCultivo }, VerdeEsmeraldaOscuro)
            if (cultivo.fechaSiembra.isNotBlank()) {
                DatoTecnicoItem(Icons.Filled.Event, "Fecha de siembra", cultivo.fechaSiembra, GrisHumo)
            }
            DatoTecnicoItem(Icons.Filled.Place, "Ubicación", cultivo.ubicacion.ifBlank { "Sin ubicación" }, GrisHumo)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetalleCard(
    titulo: String,
    icono: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(VerdeLima, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = null, tint = VerdeBosque, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun DatoTecnicoItem(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    iconColor: Color = GrisHumo
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(etiqueta, fontSize = 11.sp, color = GrisMedio)
            Text(valor, fontSize = 13.sp, color = GrisHumo, fontWeight = FontWeight.Medium)
        }
    }
    Divider(color = Color(0xFFF0F0F0))
}
