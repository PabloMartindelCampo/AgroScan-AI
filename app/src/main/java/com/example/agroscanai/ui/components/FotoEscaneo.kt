package com.example.agroscanai.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.ui.theme.GrisMedio
import com.example.agroscanai.ui.theme.VerdeEsmeralda
import com.example.agroscanai.utils.FotosEscaneo

/** Muestra la foto local de un escaneo. Si ya no está en el teléfono, lo indica. */
@Composable
fun FotoEscaneo(
    ruta: String,
    modifier: Modifier = Modifier,
    ladoMaximo: Int = 1280,
    contentScale: ContentScale = ContentScale.Crop
) {
    val estado by produceState<Pair<Boolean, Bitmap?>>(false to null, ruta) {
        value = true to FotosEscaneo.cargarBitmap(ruta, ladoMaximo)
    }
    val (cargado, bitmap) = estado

    Box(modifier = modifier.background(Color(0xFFF1F3F1)), contentAlignment = Alignment.Center) {
        when {
            !cargado -> CircularProgressIndicator(color = VerdeEsmeralda,
                modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto de la hoja",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)) {
                Icon(Icons.Filled.BrokenImage, null, tint = GrisMedio, modifier = Modifier.size(32.dp))
                if (ladoMaximo > 300) {
                    Spacer(Modifier.height(4.dp))
                    Text("La foto no está en este teléfono", fontSize = 12.sp,
                        color = GrisMedio, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
