package com.example.agroscanai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.Clima
import com.example.agroscanai.data.model.PronosticoClima
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.ClimaUiState
import com.example.agroscanai.ui.viewmodel.ClimaViewModel

@Composable
fun ClimaScreen(
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {},
    viewModel: ClimaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.cargarClima(granted) }

    LaunchedEffect(Unit) {
        if (uiState is ClimaUiState.Cargando) {
            val tienePermiso = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (tienePermiso) viewModel.cargarClima(true)
            else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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
        ) {
            ClimaHeader(
                onBackClick           = onBackClick,
                region                = (uiState as? ClimaUiState.Exito)?.region ?: "Cargando..."
            )

            when (val state = uiState) {
                is ClimaUiState.Cargando -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = VerdeBosque)
                            Spacer(Modifier.height(12.dp))
                            Text("Obteniendo datos del clima...", color = GrisMedio, fontSize = 14.sp)
                        }
                    }
                }
                is ClimaUiState.Error -> {
                    Box(
                        Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CloudOff, null,
                                tint = GrisMedio, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(state.mensaje, color = GrisMedio, fontSize = 14.sp,
                                textAlign = TextAlign.Center, lineHeight = 20.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val tienePermiso = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (tienePermiso) viewModel.cargarClima(true)
                                    else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VerdeBosque)
                            ) { Text("Reintentar") }
                        }
                    }
                }
                is ClimaUiState.Exito -> {
                    ClimaContenido(state.pronostico)
                }
            }
        }
    }
}

@Composable
private fun ClimaHeader(
    onBackClick: () -> Unit,
    region: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(VerdeBosqueOscuro, VerdeBosque)))
            .padding(bottom = 20.dp)
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
            Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.WbSunny, null, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text("Clima Agrícola", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(region, fontSize = 13.sp, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun ClimaContenido(pronostico: PronosticoClima) {
    val hoy = pronostico.diasPronostico.firstOrNull() ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("HOY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrisMedio)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${hoy.temperatura.toInt()}°C",
                    fontSize = 56.sp, fontWeight = FontWeight.Light, color = VerdeBosque
                )
                Text(hoy.descripcion, fontSize = 15.sp, color = GrisMedio, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mín ${hoy.temperaturaMin.toInt()}°C  ·  Máx ${hoy.temperaturaMax.toInt()}°C",
                    fontSize = 13.sp, color = GrisMedio
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricaClima(Icons.Filled.WaterDrop, "${hoy.humedad}%",                    "Humedad",    Color(0xFF1565C0))
                    MetricaClima(Icons.Filled.Air,        "${hoy.velocidadViento.toInt()} km/h","Viento",     Color(0xFF455A64))
                    MetricaClima(Icons.Filled.Umbrella,   "${hoy.probabilidadLluvia}%",         "Lluvia",     Color(0xFF5C6BC0))
                    MetricaClima(Icons.Filled.WbSunny,    "UV ${hoy.uvIndex}",                 "Índice UV",  Color(0xFFF57C00))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel("PRONÓSTICO 7 DÍAS")
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                pronostico.diasPronostico.forEachIndexed { idx, dia ->
                    FilaDia(dia, esHoy = idx == 0)
                    if (idx < pronostico.diasPronostico.lastIndex)
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color    = Color(0xFFF0F0F0)
                        )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (pronostico.recomendaciones.isNotEmpty()) {
            SectionLabel("RECOMENDACIONES AGRÍCOLAS")
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    pronostico.recomendaciones.forEach { rec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.Eco, null, tint = VerdeBosque,
                                modifier = Modifier.size(16.dp).padding(top = 1.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(rec, fontSize = 13.sp, color = Color(0xFF2E7D32), lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Datos: Open-Meteo (api.open-meteo.com) · Actualización automática",
            fontSize = 11.sp, color = GrisMedio, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(texto: String) {
    Text(
        texto, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GrisMedio,
        modifier = Modifier.padding(start = 24.dp, bottom = 6.dp)
    )
}

@Composable
private fun MetricaClima(icon: ImageVector, valor: String, etiqueta: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(valor,    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Text(etiqueta, fontSize = 10.sp, color = GrisMedio)
    }
}

@Composable
private fun FilaDia(dia: Clima, esHoy: Boolean) {
    val (iconoDia, colorIcono) = iconoYColor(dia.descripcion)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (esHoy) Color(0xFFF0F7F0) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dia.fecha,
            fontSize    = 14.sp,
            fontWeight  = if (esHoy) FontWeight.Bold else FontWeight.Normal,
            color       = if (esHoy) VerdeBosque else Color(0xFF212121),
            modifier    = Modifier.width(90.dp)
        )
        Icon(iconoDia, null, tint = colorIcono, modifier = Modifier.size(20.dp))
        Spacer(Modifier.weight(1f))
        Text(
            "${dia.probabilidadLluvia}%",
            fontSize = 12.sp,
            color    = if (dia.probabilidadLluvia >= 60) Color(0xFF1565C0) else GrisMedio,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
        Spacer(Modifier.width(8.dp))
        Text("${dia.temperaturaMin.toInt()}°", fontSize = 13.sp, color = GrisMedio, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
        Text(" / ${dia.temperaturaMax.toInt()}°C", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
    }
}

private fun iconoYColor(descripcion: String): Pair<ImageVector, Color> = when {
    descripcion.contains("Tormenta", true) -> Pair(Icons.Filled.Thunderstorm, Color(0xFF5C6BC0))
    descripcion.contains("Lluvi",    true) -> Pair(Icons.Filled.Umbrella,     Color(0xFF1565C0))
    descripcion.contains("Nublado",  true) -> Pair(Icons.Filled.Cloud,        Color(0xFF78909C))
    descripcion.contains("Niebla",   true) -> Pair(Icons.Filled.Cloud,        Color(0xFF90A4AE))
    descripcion.contains("Nevado",   true) -> Pair(Icons.Filled.AcUnit,       Color(0xFF42A5F5))
    else                                   -> Pair(Icons.Filled.WbSunny,      Color(0xFFF57C00))
}
