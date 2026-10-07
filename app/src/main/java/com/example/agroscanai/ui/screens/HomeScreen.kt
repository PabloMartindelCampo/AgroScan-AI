package com.example.agroscanai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.R
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*

data class HomeMenuItem(
    val label: String,
    val iconRes: Int,
    val route: String
)

@Composable
fun HomeScreen(
    nombreUsuario: String = "",
    onMenuItemClick: (route: String) -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {}
) {
    val menuItems = listOf(
        HomeMenuItem("Escanear Parcela", R.drawable.ic_escanear, "escaneo"),
        HomeMenuItem("Mis Cultivos", R.drawable.ic_cultivos, "mis_cultivos"),
        HomeMenuItem("Mapa de Lotes", R.drawable.ic_mapa, "mapa_lotes"),
        HomeMenuItem("Dashboard de Salud", R.drawable.ic_dashboard, "dashboard_salud"),
        HomeMenuItem("Reportes de IA", R.drawable.ic_reportes, "reportes_ia"),
        HomeMenuItem("Clima Inteligente", R.drawable.ic_clima, "clima"),
        HomeMenuItem("Calendario de Tareas", R.drawable.ic_calendario, "calendario"),
        HomeMenuItem("Precios de Mercado", R.drawable.ic_precios, "precios")
    )

    Scaffold(
        containerColor = Color(0xFFF5F7F5),
        bottomBar = {
            AgroBottomBar(
                onHomeClick           = {},
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick         = onPerfilClick,
                paginaActual          = PaginaActual.HOME
            )//bara inferior
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .shadow(2.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo_agroscan),
                    contentDescription = "Logo",
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "AgroScan AI",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = VerdeBosque,
                    lineHeight = 10.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(VerdeBosque)
                    .clickable { onPerfilClick() },
                contentAlignment = Alignment.Center
            ) {
                if (nombreUsuario.isNotBlank()) {
                    Text(
                        text = nombreUsuario.first().uppercaseChar().toString(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Perfil",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFFE8ECE8), thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_agroscan),
                contentDescription = "AgroScan AI",
                modifier = Modifier.size(90.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AgroScan AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = VerdeBosque
            )
        }

        HorizontalDivider(color = Color(0xFFE8ECE8), thickness = 1.dp)

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            menuItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    rowItems.forEach { item ->
                        HomeMenuCard(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onMenuItemClick(item.route) }
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "AgroScan AI es una marca registrada. Todos los derechos reservados.",
            fontSize = 11.sp,
            color = GrisMedio,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp)
        )
    }
    }
}

@Composable
private fun HomeMenuCard(
    item: HomeMenuItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.label,
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = VerdeBosque,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
