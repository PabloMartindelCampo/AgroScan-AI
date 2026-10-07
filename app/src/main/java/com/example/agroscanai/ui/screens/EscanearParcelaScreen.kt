package com.example.agroscanai.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.R
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel


private enum class PasoEscaneo { SELECCIONAR_PARCELA, ESCANEAR }

@Composable
fun EscanearParcelaScreen(
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos     by cultivosViewModel.cultivos.collectAsState()
    val isLoading    by cultivosViewModel.isLoading.collectAsState()

    var paso                  by remember { mutableStateOf(PasoEscaneo.SELECCIONAR_PARCELA) }
    var cultivoSeleccionado   by remember { mutableStateOf<Cultivo?>(null) }

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
                        cultivoSeleccionado = null
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

        when (paso) {
            PasoEscaneo.SELECCIONAR_PARCELA -> {
                SeleccionarParcelaStep(
                    cultivos = cultivos,
                    isLoading = isLoading,
                    onSeleccionar = { cultivo ->
                        cultivoSeleccionado = cultivo
                        paso = PasoEscaneo.ESCANEAR
                    }
                )
            }
            PasoEscaneo.ESCANEAR -> {
                EscaneoStep(
                    cultivo = cultivoSeleccionado!!,
                    onCambiarParcela = {
                        cultivoSeleccionado = null
                        paso = PasoEscaneo.SELECCIONAR_PARCELA
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
    val estadoColor = when (cultivo.estadoEnum()) {
        EstadoCultivo.SALUDABLE  -> VerdeEsmeralda
        EstadoCultivo.ALERTA     -> Color(0xFFFFA000)
        EstadoCultivo.CRITICO    -> RojoAlerta
        EstadoCultivo.SIN_ESCANEO -> GrisMedio
    }

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
    onCambiarParcela: () -> Unit
) {
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
                TextButton(
                    onClick = onCambiarParcela,
                    colors = ButtonDefaults.textButtonColors(contentColor = VerdeEsmeralda),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .height(260.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2D5A27))
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cultivos),
                contentDescription = "Campo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
            Column(modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.PhotoCamera, null, tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("El escaneo por foto estará disponible próximamente",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                    textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 11.sp, color = GrisMedio, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp))
        Spacer(Modifier.height(16.dp))
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
