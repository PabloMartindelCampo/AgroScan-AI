package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.data.model.CatalogoFrijol
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.data.model.EstadoEscaneo
import com.example.agroscanai.data.model.OrigenFoto
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.FotoEscaneo
import com.example.agroscanai.ui.components.RecomendacionCultivo
import com.example.agroscanai.ui.components.colorEstado
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import kotlin.math.roundToInt

/** Resultado de una foto: imagen, fecha, ubicación y resultado del modelo. */
@Composable
fun DetalleEscaneoScreen(
    cultivoId: String,
    escaneoId: String,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel
) {
    val cultivos by cultivosViewModel.cultivos.collectAsState()
    val escaneo  by cultivosViewModel.escaneoDetalle.collectAsState()
    val cultivo  = cultivos.find { it.id == cultivoId }

    LaunchedEffect(cultivoId, escaneoId) {
        cultivosViewModel.cargarEscaneo(cultivoId, escaneoId)
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
            .background(Color(0xFFF5F7F5))
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
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
            Column {
                Text("Detalle del escaneo", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold, color = VerdeBosque)
                if (cultivo != null)
                    Text(cultivo.nombre, fontSize = 12.sp, color = GrisMedio)
            }
        }

        val esc = escaneo
        if (esc == null || esc.id != escaneoId) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdeBosque)
            }
            return@Column
        }

        val clase     = CatalogoFrijol.porEtiqueta(esc.claseDetectada)
        val analizado = esc.estadoGeneral == EstadoEscaneo.COMPLETADO && clase != null

        // ── Foto ─────────────────────────────────────────────────────────────
        FotoEscaneo(
            ruta = esc.fotoRuta,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        // ── Resultado ────────────────────────────────────────────────────────
        TarjetaDetalle(titulo = "Resultado", icono = Icons.Filled.Analytics) {
            if (!analizado) {
                val color = colorEstado(EstadoCultivo.PENDIENTE_ANALISIS)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HourglassEmpty, null, tint = color, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Pendiente de análisis", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "La foto está guardada. El diagnóstico aparecerá cuando el modelo de IA esté disponible.",
                    fontSize = 12.sp, color = GrisMedio, lineHeight = 17.sp
                )
            } else {
                val sana  = clase!!.etiquetaModelo == CatalogoFrijol.ETIQUETA_SANA
                val color = colorEstado(if (sana) EstadoCultivo.SANA else EstadoCultivo.ENFERMEDAD)
                Text(
                    if (sana) "Sana" else "Enfermedad detectada: ${clase.nombre}",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color
                )
                if (esc.confianza != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Probabilidad del modelo: ${(esc.confianza * 100).roundToInt()}%",
                        fontSize = 12.sp, color = GrisHumo)
                }
                if (esc.modeloVersion.isNotBlank()) {
                    Text("Versión del modelo: ${esc.modeloVersion}", fontSize = 11.sp, color = GrisMedio)
                }
                Spacer(Modifier.height(12.dp))
                // Recomendación de la tabla fija para la clase de esta foto
                RecomendacionCultivo(
                    cultivo = Cultivo(
                        estado      = if (sana) EstadoCultivo.SANA.name else EstadoCultivo.ENFERMEDAD.name,
                        diagnostico = if (sana) "" else clase.nombre
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Datos de la foto ─────────────────────────────────────────────────
        TarjetaDetalle(titulo = "Datos de la foto", icono = Icons.Filled.PhotoCamera) {
            FilaDato(Icons.Filled.Schedule, "Fecha de registro", esc.fecha.ifBlank { "—" })
            FilaDato(
                Icons.Filled.LocationOn,
                if (esc.origen == OrigenFoto.CAMARA) "Ubicación al tomar la foto" else "Ubicación guardada en la foto",
                if (esc.gpsLat != null && esc.gpsLon != null)
                    "%.6f, %.6f".format(esc.gpsLat, esc.gpsLon)
                else "No disponible"
            )
            FilaDato(
                Icons.Filled.PhotoLibrary, "Origen",
                if (esc.origen == OrigenFoto.CAMARA) "Cámara" else "Galería"
            )
        }

        Text(
            text = "AgroScan AI es una marca registrada. Todos los derechos reservados",
            fontSize = 11.sp,
            color = GrisMedio,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        )
    }
    }
}

@Composable
private fun TarjetaDetalle(
    titulo: String,
    icono: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
private fun FilaDato(icono: ImageVector, etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = GrisHumo, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(etiqueta, fontSize = 11.sp, color = GrisMedio)
            Text(valor, fontSize = 13.sp, color = GrisHumo, fontWeight = FontWeight.Medium)
        }
    }
    Divider(color = Color(0xFFF0F0F0))
}
