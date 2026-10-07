package com.example.agroscanai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.data.model.CatalogoFrijol
import com.example.agroscanai.data.model.Cultivo
import com.example.agroscanai.data.model.EstadoCultivo
import com.example.agroscanai.ui.theme.*

/** Colores e íconos comunes para los estados del cultivo derivados del modelo. */
fun colorEstado(estado: EstadoCultivo): Color = when (estado) {
    EstadoCultivo.SIN_ESCANEO        -> GrisMedio
    EstadoCultivo.PENDIENTE_ANALISIS -> Color(0xFF1565C0)
    EstadoCultivo.SANA               -> Color(0xFF4CAF50)
    EstadoCultivo.ENFERMEDAD         -> Color(0xFFD32F2F)
}

fun iconoEstado(estado: EstadoCultivo): ImageVector = when (estado) {
    EstadoCultivo.SIN_ESCANEO        -> Icons.Filled.SearchOff
    EstadoCultivo.PENDIENTE_ANALISIS -> Icons.Filled.HourglassEmpty
    EstadoCultivo.SANA               -> Icons.Filled.CheckCircle
    EstadoCultivo.ENFERMEDAD         -> Icons.Filled.BugReport
}

/**
 * Recomendación de la tabla fija CatalogoFrijol para el diagnóstico del cultivo.
 * Solo muestra texto cuando tiene fuente citada, y siempre muestra el aviso.
 */
@Composable
fun RecomendacionCultivo(cultivo: Cultivo, modifier: Modifier = Modifier) {
    val clase = when (cultivo.estadoEnum()) {
        EstadoCultivo.SANA       -> CatalogoFrijol.porEtiqueta(CatalogoFrijol.ETIQUETA_SANA)
        EstadoCultivo.ENFERMEDAD -> CatalogoFrijol.porNombre(cultivo.diagnostico)
        else                     -> null
    }
    Column(modifier = modifier.fillMaxWidth()) {
        when {
            !cultivo.tieneFotoAnalizada() -> Text(
                "Sin recomendación: todavía no hay una foto analizada por el modelo.",
                fontSize = 12.sp, color = GrisMedio, lineHeight = 17.sp
            )
            clase == null || !CatalogoFrijol.tieneRecomendacionVerificada(clase) -> Text(
                "Recomendación pendiente: aún no se ha cargado una fuente verificada para esta clase.",
                fontSize = 12.sp, color = GrisMedio, lineHeight = 17.sp
            )
            else -> {
                if (clase.descripcion.isNotBlank()) {
                    Text(clase.descripcion, fontSize = 12.sp, color = GrisHumo, lineHeight = 17.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null,
                        tint = VerdeEsmeralda, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(clase.tratamiento, fontSize = 12.sp, color = VerdeBosque,
                        fontWeight = FontWeight.Medium, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("Fuente: ${clase.fuente}", fontSize = 10.sp, color = GrisMedio)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null,
                tint = Color(0xFFF57F17), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(CatalogoFrijol.AVISO, fontSize = 11.sp, color = Color(0xFF5D4037),
                fontWeight = FontWeight.Medium)
        }
    }
}
