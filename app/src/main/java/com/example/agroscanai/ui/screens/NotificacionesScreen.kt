package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel

private data class Notificacion(
    val id: String,
    val tipo: TipoNotif,
    val titulo: String,
    val descripcion: String
)

private enum class TipoNotif { CRITICA, ALERTA, RECORDATORIO, INFO }

@Composable
fun NotificacionesScreen(
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos by cultivosViewModel.cultivos.collectAsState()

    val notificaciones: List<Notificacion> = remember(cultivos) {
        val lista = mutableListOf<Notificacion>()

        val enfermas   = cultivos.count { it.estadoEnum() == EstadoCultivo.ENFERMEDAD }
        val sanas      = cultivos.count { it.estadoEnum() == EstadoCultivo.SANA }
        val pendientes = cultivos.count { it.estadoEnum() == EstadoCultivo.PENDIENTE_ANALISIS }
        val sinEscaneo = cultivos.count { it.estadoEnum() == EstadoCultivo.SIN_ESCANEO }

        if (cultivos.isNotEmpty()) {
            lista.add(Notificacion(
                id = "resumen",
                tipo = TipoNotif.INFO,
                titulo = "Resumen de tus parcelas",
                descripcion = "${cultivos.size} parcelas · $sanas sana(s) · $enfermas con enfermedad · $pendientes pendiente(s) de análisis · $sinEscaneo sin escaneo"
            ))
        }

        cultivos.filter { it.estadoEnum() == EstadoCultivo.ENFERMEDAD }.forEach { c ->
            lista.add(Notificacion(
                id = "enfermedad_${c.id}",
                tipo = TipoNotif.ALERTA,
                titulo = "${c.estadoTexto()} en ${c.nombre}",
                descripcion = "El modelo detectó ${c.diagnostico.ifBlank { "una enfermedad" }} en la última foto de '${c.nombre}' (${c.ultimoEscaneo}). Revisa la recomendación y consulta a un técnico."
            ))
        }

        cultivos.filter { it.estadoEnum() == EstadoCultivo.PENDIENTE_ANALISIS }.forEach { c ->
            lista.add(Notificacion(
                id = "pendiente_${c.id}",
                tipo = TipoNotif.INFO,
                titulo = "Foto pendiente de análisis: ${c.nombre}",
                descripcion = "La foto del ${c.ultimoEscaneo} está guardada. El diagnóstico aparecerá cuando el modelo de IA esté disponible."
            ))
        }

        cultivos.filter { it.estadoEnum() == EstadoCultivo.SIN_ESCANEO }.forEach { c ->
            lista.add(Notificacion(
                id = "sinEscaneo_${c.id}",
                tipo = TipoNotif.RECORDATORIO,
                titulo = "Escaneo pendiente: ${c.nombre}",
                descripcion = "La parcela '${c.nombre}' aún no ha sido escaneada. Toma una foto de una hoja desde \"Escanear Parcela\"."
            ))
        }

        lista
    }

    val badgeCount = notificaciones.count { it.tipo == TipoNotif.CRITICA || it.tipo == TipoNotif.ALERTA }

    Scaffold(
        bottomBar = {
            AgroBottomBar(
                onHomeClick = onHomeClick,
                onNotificacionesClick = {},
                onPerfilClick = onPerfilClick,
                paginaActual = PaginaActual.NOTIFICACIONES
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(innerPadding)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(VerdeBosqueOscuro, VerdeBosque)))
                .padding(bottom = 20.dp)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(Icons.Filled.Notifications, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    if (badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(17.dp)
                                .background(RojoAlerta, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$badgeCount", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Notificaciones", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    if (notificaciones.isEmpty()) "Sin notificaciones" else "${notificaciones.size} notificaciones",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        if (notificaciones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, null, tint = VerdeEsmeralda.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("¡Todo en orden!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No tienes alertas ni notificaciones pendientes.\nAgrega cultivos y realiza escaneos para monitorear su salud.",
                        fontSize = 14.sp, color = GrisMedio,
                        textAlign = TextAlign.Center, lineHeight = 20.sp
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notificaciones, key = { it.id }) { notif ->
                    NotificacionCard(notif)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
    }
}

@Composable
private fun NotificacionCard(notif: Notificacion) {
    val bgColor: Color = when (notif.tipo) {
        TipoNotif.CRITICA      -> Color(0xFFFFEBEE)
        TipoNotif.ALERTA       -> Color(0xFFFFF8E1)
        TipoNotif.RECORDATORIO -> Color(0xFFE3F2FD)
        TipoNotif.INFO         -> Color(0xFFE8F5E9)
    }
    val iconColor: Color = when (notif.tipo) {
        TipoNotif.CRITICA      -> RojoAlerta
        TipoNotif.ALERTA       -> Color(0xFFFF8F00)
        TipoNotif.RECORDATORIO -> Color(0xFF1565C0)
        TipoNotif.INFO         -> VerdeBosque
    }
    val icon: ImageVector = when (notif.tipo) {
        TipoNotif.CRITICA      -> Icons.Filled.Warning
        TipoNotif.ALERTA       -> Icons.Filled.NotificationImportant
        TipoNotif.RECORDATORIO -> Icons.Filled.Alarm
        TipoNotif.INFO         -> Icons.Filled.Info
    }
    val etiqueta: String = when (notif.tipo) {
        TipoNotif.CRITICA      -> "CRÍTICO"
        TipoNotif.ALERTA       -> "ALERTA"
        TipoNotif.RECORDATORIO -> "RECORDATORIO"
        TipoNotif.INFO         -> "INFO"
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(bgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(etiqueta, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = iconColor)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(notif.titulo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(notif.descripcion, fontSize = 12.sp, color = GrisMedio, lineHeight = 18.sp)
            }
        }
    }
}
