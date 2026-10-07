package com.example.agroscanai.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.data.model.CatalogoFrijol
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.components.RecomendacionCultivo
import com.example.agroscanai.ui.components.colorEstado
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel

// ── Texto del reporte (solo datos registrados por los escaneos por foto) ───────

/** Texto de recomendación de la tabla fija, solo si tiene fuente citada. */
private fun textoRecomendacion(cultivo: Cultivo): String {
    val clase = when (cultivo.estadoEnum()) {
        EstadoCultivo.SANA       -> CatalogoFrijol.porEtiqueta(CatalogoFrijol.ETIQUETA_SANA)
        EstadoCultivo.ENFERMEDAD -> CatalogoFrijol.porNombre(cultivo.diagnostico)
        else                     -> null
    }
    return when {
        !cultivo.tieneFotoAnalizada() -> "Sin recomendación: no hay foto analizada."
        clase == null || !CatalogoFrijol.tieneRecomendacionVerificada(clase) ->
            "Recomendación pendiente de fuente verificada."
        else -> "${clase.tratamiento} (Fuente: ${clase.fuente})"
    }
}

private fun textoCultivo(c: Cultivo): String = buildString {
    appendLine("--- ${c.nombre} (${c.tipoCultivo}) ---")
    appendLine("Estado: ${c.estadoTexto()}")
    appendLine("Último escaneo: ${if (c.tieneEscaneo()) c.ultimoEscaneo else "Sin escaneos"}")
    appendLine("Recomendación: ${textoRecomendacion(c)}")
    appendLine(CatalogoFrijol.AVISO)
}

private fun construirTextoReporte(cultivos: List<Cultivo>): String {
    val sb = StringBuilder()
    sb.appendLine("=== REPORTE DE ESCANEOS — AGROSCAN AI ===")
    sb.appendLine("Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
    sb.appendLine()
    sb.appendLine("Resumen general: ${cultivos.size} cultivos")
    sb.appendLine("  Sanos:                 ${cultivos.count { it.estadoEnum() == EstadoCultivo.SANA }}")
    sb.appendLine("  Con enfermedad:        ${cultivos.count { it.estadoEnum() == EstadoCultivo.ENFERMEDAD }}")
    sb.appendLine("  Pendientes de análisis: ${cultivos.count { it.estadoEnum() == EstadoCultivo.PENDIENTE_ANALISIS }}")
    sb.appendLine("  Sin escaneo:           ${cultivos.count { it.estadoEnum() == EstadoCultivo.SIN_ESCANEO }}")
    sb.appendLine()
    cultivos.forEach { c ->
        sb.append(textoCultivo(c))
        sb.appendLine()
    }
    return sb.toString()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ReportesIAScreen(
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel
) {
    val cultivos by cultivosViewModel.cultivos.collectAsState()
    val context  = LocalContext.current

    val sanas      = cultivos.count { it.estadoEnum() == EstadoCultivo.SANA }
    val enfermas   = cultivos.count { it.estadoEnum() == EstadoCultivo.ENFERMEDAD }
    val pendientes = cultivos.count { it.estadoEnum() == EstadoCultivo.PENDIENTE_ANALISIS }
    val sinEscaneo = cultivos.count { it.estadoEnum() == EstadoCultivo.SIN_ESCANEO }

    Scaffold(
        containerColor = Color(0xFFF0F4F0),
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(VerdeBosqueOscuro, VerdeBosque)))
                    .padding(bottom = 20.dp)
            ) {
                IconButton(
                    onClick  = onBackClick,
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Reportes de IA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Resultados de los escaneos por foto",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (cultivos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Psychology, contentDescription = null,
                            tint = GrisMedio, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Sin cultivos registrados", fontSize = 16.sp, color = GrisMedio, fontWeight = FontWeight.SemiBold)
                        Text("Agrega cultivos y escanea sus hojas para ver resultados.",
                            fontSize = 13.sp, color = GrisMedio)
                    }
                }
                return@Scaffold
            }

            // ── Resumen general ───────────────────────────────────────────────
            Text(
                "RESUMEN GENERAL",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = GrisMedio,
                modifier   = Modifier.padding(start = 24.dp, bottom = 6.dp)
            )
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ResumenChip(sanas.toString(),      "Sanas",       colorEstado(EstadoCultivo.SANA))
                        ResumenChip(enfermas.toString(),   "Enfermas",    colorEstado(EstadoCultivo.ENFERMEDAD))
                        ResumenChip(pendientes.toString(), "Pendientes",  colorEstado(EstadoCultivo.PENDIENTE_ANALISIS))
                        ResumenChip(sinEscaneo.toString(), "Sin escaneo", GrisMedio)
                    }
                }
            }

            // ── Botón compartir reporte global ────────────────────────────────
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val texto = construirTextoReporte(cultivos)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, texto)
                        putExtra(Intent.EXTRA_SUBJECT, "Reporte AgroScan AI")
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeBosque),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, VerdeBosque)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Compartir reporte completo", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            // ── Reportes por cultivo ──────────────────────────────────────────
            Text(
                "ANÁLISIS POR CULTIVO",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                color      = GrisMedio,
                modifier   = Modifier.padding(start = 24.dp, bottom = 6.dp)
            )

            cultivos.forEach { cultivo ->
                ReporteCard(cultivo = cultivo, context = context)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Composables privados ──────────────────────────────────────────────────────

@Composable
private fun ResumenChip(valor: String, etiqueta: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor,    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        Text(etiqueta, fontSize = 11.sp, color = GrisMedio)
    }
}

@Composable
private fun ReporteCard(cultivo: Cultivo, context: android.content.Context) {
    var expandido by remember { mutableStateOf(false) }

    val estadoColor = colorEstado(cultivo.estadoEnum())

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Cabecera clickeable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(estadoColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Grass, contentDescription = null,
                        tint = estadoColor, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(cultivo.nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                    Text(cultivo.tipoCultivo, fontSize = 12.sp, color = GrisMedio)
                }
                Box(
                    modifier = Modifier
                        .background(estadoColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(cultivo.estadoTexto(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = estadoColor)
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint     = GrisMedio,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Contenido expandible
            AnimatedVisibility(
                visible = expandido,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

                    Text(cultivo.descripcionSalud(), fontSize = 12.sp, color = Color(0xFF424242), lineHeight = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Último escaneo: ${if (cultivo.tieneEscaneo()) cultivo.ultimoEscaneo else "Sin escaneos"}",
                        fontSize = 11.sp, color = GrisMedio
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Recomendación", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = VerdeBosque)
                    Spacer(Modifier.height(6.dp))
                    RecomendacionCultivo(cultivo = cultivo)

                    Spacer(Modifier.height(10.dp))

                    // Botón compartir reporte individual
                    OutlinedButton(
                        onClick = {
                            val texto = textoCultivo(cultivo)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, texto)
                                putExtra(Intent.EXTRA_SUBJECT, "Reporte de escaneo — ${cultivo.nombre}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape  = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeBosque),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VerdeBosque)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartir este reporte", fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
