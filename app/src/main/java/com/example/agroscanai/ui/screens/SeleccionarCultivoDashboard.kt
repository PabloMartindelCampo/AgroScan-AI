package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.components.colorEstado
import com.example.agroscanai.ui.viewmodel.CultivosViewModel

@Composable
fun SeleccionarCultivoDashboard(
    onCultivoSelected: (cultivoId: String) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val cultivos by cultivosViewModel.cultivos.collectAsState()
    val isLoading by cultivosViewModel.isLoading.collectAsState()

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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .shadow(2.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = VerdeBosque)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dashboard de Salud",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque
                )
                Text(
                    text = "Selecciona una parcela",
                    fontSize = 12.sp,
                    color = GrisMedio
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = VerdeLima),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = VerdeBosque,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Elige una parcela para ver su dashboard de salud completo basado en el último escaneo.",
                    fontSize = 13.sp,
                    color = VerdeBosqueOscuro
                )
            }
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdeBosque)
                }
            }
            cultivos.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Agriculture,
                            contentDescription = null,
                            tint = GrisMedio,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No tienes parcelas registradas",
                            fontSize = 16.sp,
                            color = GrisMedio,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Agrega un cultivo desde \"Mis Cultivos\"",
                            fontSize = 13.sp,
                            color = GrisMedio,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cultivos, key = { it.id }) { cultivo ->
                        CultivoSeleccionCard(
                            cultivo = cultivo,
                            onClick = { onCultivoSelected(cultivo.id) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
    }
}

@Composable
private fun CultivoSeleccionCard(
    cultivo: Cultivo,
    onClick: () -> Unit
) {
    val estadoColor = colorEstado(cultivo.estadoEnum())
    val estadoLabel = cultivo.estadoTexto()
    val estadoBg = estadoColor.copy(alpha = 0.10f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(estadoBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Grass,
                    contentDescription = null,
                    tint = estadoColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cultivo.nombre,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "${cultivo.tipoCultivo} · ${cultivo.hectareas} ha",
                    fontSize = 12.sp,
                    color = GrisMedio,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (cultivo.tieneEscaneo()) {
                    Text(
                        text = "Último escaneo: ${cultivo.ultimoEscaneo}",
                        fontSize = 11.sp,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(estadoBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = estadoLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = estadoColor
                    )
                }
                Spacer(Modifier.height(6.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = GrisMedio,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
