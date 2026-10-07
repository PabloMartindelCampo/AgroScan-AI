package com.example.agroscanai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Insights
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
import com.example.agroscanai.ui.theme.*

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Agriculture,
        title = "¿Qué es AgroScan AI?",
        // REVISAR (sin dron): "tecnología de drones con" -> "fotos de hojas tomadas con tu celular e"
        description = "AgroScan AI es una plataforma inteligente diseñada para agricultores modernos. Combina fotos de hojas tomadas con tu celular e inteligencia artificial para monitorear, analizar y optimizar el estado de tus parcelas en tiempo real."
    ),
    OnboardingPage(
        icon = Icons.Filled.BugReport,
        title = "Escaneo Inteligente",
        // REVISAR (sin dron): "Mediante el uso de drones ... sobrevuela tus parcelas y captura imágenes detalladas ... cada metro cuadrado" -> foto de hoja con el celular / "cada foto"
        description = "Con la cámara de tu celular, AgroScan AI captura fotos de las hojas de tus cultivos. Nuestra IA analiza cada foto para identificar plagas, exceso de humedad, zonas secas y cualquier anomalía que afecte tus cultivos."
    ),
    OnboardingPage(
        icon = Icons.Filled.Insights,
        title = "Decisiones con Base en Datos",
        description = "Recibe reportes detallados, alertas tempranas y recomendaciones personalizadas. Actúa a tiempo, reduce pérdidas y mejora la productividad de tus cultivos con información precisa generada por inteligencia artificial."
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    val page = pages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VerdeBosque, VerdeBosqueOscuro)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (index == currentPage) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index == currentPage) VerdeEsmeralda else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = VerdeEsmeralda,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    Text(
                        text = page.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = page.description,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdeEsmeralda
                    )
                ) {
                    Text(
                        text = if (currentPage < pages.size - 1) "Siguiente" else "Comenzar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (currentPage < pages.size - 1) {
                    TextButton(onClick = onFinished) {
                        Text(
                            text = "Omitir",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "AgroScan AI es una marca registrada. Todos los derechos reservados.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
