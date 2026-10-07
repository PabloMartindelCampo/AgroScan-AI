package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agroscanai.data.model.TipoCuenta
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import com.example.agroscanai.ui.viewmodel.PerfilViewModel

@Composable
fun PerfilScreen(
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onEditarPerfil: () -> Unit = {},
    onConfiguracion: () -> Unit = {},
    onAcerca: () -> Unit = {},
    onTerminos: () -> Unit = {},
    onPrivacidad: () -> Unit = {},
    onCookies: () -> Unit = {},
    onAyuda: () -> Unit = {},
    onSignOut: () -> Unit = {},
    perfilViewModel: PerfilViewModel = viewModel(),
    cultivosViewModel: CultivosViewModel = viewModel()
) {
    val usuario        by perfilViewModel.usuario.collectAsState()
    val cultivos       by cultivosViewModel.cultivos.collectAsState()
    val snackbarHost   = remember { SnackbarHostState() }
    val error          by perfilViewModel.error.collectAsState()
    var showLogoutDialog  by remember { mutableStateOf(false) }
    var showRatingDialog  by remember { mutableStateOf(false) }
    var ratingEstrellas   by remember { mutableStateOf(0) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHost.showSnackbar(it)
            perfilViewModel.clearError()
        }
    }

    // Stats derived from cultivos
    val totalCultivos  = cultivos.size
    val totalHectareas = cultivos.sumOf { it.hectareas }
    // Solo parcelas con al menos una foto analizada por el modelo
    val conEscaneo     = cultivos.count { it.tieneFotoAnalizada() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(snackbarData = data, containerColor = RojoAlerta, contentColor = Color.White)
            }
        },
        containerColor = Color(0xFFF0F4F0),
        bottomBar = {
            AgroBottomBar(
                onHomeClick = onHomeClick,
                onNotificacionesClick = onNotificacionesClick,
                onPerfilClick = {},
                paginaActual = PaginaActual.PERFIL
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header con gradiente ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(VerdeBosqueOscuro, VerdeBosque)
                        )
                    )
                    .padding(bottom = 24.dp)
            ) {
                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    when (usuario?.tipoCuentaEnum()) {
                                        TipoCuenta.PREMIUM      -> Color(0xFF1565C0)
                                        TipoCuenta.EMPRESARIAL  -> Color(0xFF6A1B9A)
                                        else                    -> VerdeBosque
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = usuario?.iniciales() ?: "?",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = usuario?.nombreCompleto() ?: "Cargando…",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = usuario?.email ?: perfilViewModel.emailActual,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(10.dp))

                    // Tipo cuenta badge
                    val (cuentaLabel, cuentaBg) = when (usuario?.tipoCuentaEnum()) {
                        TipoCuenta.PREMIUM     -> Pair("⭐ Premium", Color(0xFF1565C0))
                        TipoCuenta.EMPRESARIAL -> Pair("🏢 Empresarial", Color(0xFF6A1B9A))
                        else                   -> Pair("Básico", Color(0xFF37474F))
                    }
                    Box(
                        modifier = Modifier
                            .background(cuentaBg, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Text(cuentaLabel, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(14.dp))

                    // Editar perfil button
                    OutlinedButton(
                        onClick = onEditarPerfil,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Editar perfil", fontSize = 13.sp)
                    }
                }
            }

            // ── Stats ────────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-16).dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "$totalCultivos", label = "Parcelas")
                    Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color(0xFFEEEEEE))
                    StatItem(value = "%.1f".format(totalHectareas), label = "Hectáreas")
                    Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color(0xFFEEEEEE))
                    StatItem(value = "$conEscaneo", label = "Escaneadas")
                }
            }

            // ── Menú secciones ───────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))

            // SECCIÓN: Cuenta
            SeccionLabel("CUENTA")
            MenuCard {
                MenuItem(
                    icon     = Icons.Filled.Person,
                    iconBg   = Color(0xFFE8F5E9),
                    iconColor = VerdeBosque,
                    titulo   = "Mi información",
                    subtitulo = "Nombre, teléfono y región",
                    onClick  = onEditarPerfil
                )
                MenuDivider()
                MenuItem(
                    icon      = Icons.Filled.Settings,
                    iconBg    = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1565C0),
                    titulo    = "Configuración",
                    subtitulo = "Notificaciones, unidades y más",
                    onClick   = onConfiguracion
                )
                if (usuario?.fechaRegistro?.isNotBlank() == true) {
                    MenuDivider()
                    MenuItem(
                        icon      = Icons.Filled.CalendarMonth,
                        iconBg    = Color(0xFFF3E5F5),
                        iconColor = Color(0xFF6A1B9A),
                        titulo    = "Miembro desde",
                        subtitulo = usuario?.fechaRegistro ?: "",
                        showArrow = false,
                        onClick   = {}
                    )
                }
            }

            // SECCIÓN: Información
            SeccionLabel("INFORMACIÓN")
            MenuCard {
                MenuItem(
                    icon      = Icons.Filled.Info,
                    iconBg    = Color(0xFFE8F5E9),
                    iconColor = VerdeBosque,
                    titulo    = "Acerca de AgroScan AI",
                    subtitulo = "Versión, equipo y misión",
                    onClick   = onAcerca
                )
                MenuDivider()
                MenuItem(
                    icon      = Icons.Filled.Description,
                    iconBg    = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1565C0),
                    titulo    = "Términos y condiciones",
                    subtitulo = "Acuerdo de uso de la aplicación",
                    onClick   = onTerminos
                )
                MenuDivider()
                MenuItem(
                    icon      = Icons.Filled.Lock,
                    iconBg    = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFE65100),
                    titulo    = "Aviso de privacidad",
                    subtitulo = "Uso y protección de tus datos",
                    onClick   = onPrivacidad
                )
                MenuDivider()
                MenuItem(
                    icon      = Icons.Filled.Cookie,
                    iconBg    = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF6A1B9A),
                    titulo    = "Política de cookies",
                    subtitulo = "Cómo usamos las cookies",
                    onClick   = onCookies
                )
                MenuDivider()
                MenuItem(
                    icon      = Icons.Filled.HelpOutline,
                    iconBg    = Color(0xFFE0F7FA),
                    iconColor = Color(0xFF00695C),
                    titulo    = "Soporte y ayuda",
                    subtitulo = "Preguntas frecuentes y contacto",
                    onClick   = onAyuda
                )
            }

            // SECCIÓN: Más
            SeccionLabel("MÁS")
            MenuCard {
                MenuItem(
                    icon      = Icons.Filled.Star,
                    iconBg    = Color(0xFFFFF8E1),
                    iconColor = Color(0xFFF57F17),
                    titulo    = "Calificar la aplicación",
                    subtitulo = "Ayúdanos con tu opinión",
                    onClick   = { showRatingDialog = true }
                )
                MenuDivider()
                MenuItem(
                    icon      = Icons.Filled.Share,
                    iconBg    = Color(0xFFE8F5E9),
                    iconColor = VerdeEsmeralda,
                    titulo    = "Compartir AgroScan AI",
                    subtitulo = "Recomiéndale la app a un colega",
                    onClick   = { /* TODO: share intent */ }
                )
            }

            // SECCIÓN: Sesión
            SeccionLabel("SESIÓN")
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = RojoAlerta, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cerrar sesión", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = RojoAlerta)
                        Text("Salir de tu cuenta actual", fontSize = 12.sp, color = GrisMedio)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = RojoAlerta.copy(alpha = 0.5f))
                }
            }

            // Versión
            Spacer(Modifier.height(20.dp))
            Text(
                text      = "AgroScan AI · Versión 1.0.0\n© 2026 AgroScan AI. Todos los derechos reservados.",
                fontSize  = 11.sp,
                color     = GrisMedio,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Diálogo de calificación ──────────────────────────────────────────────
    if (showRatingDialog) {
        AlertDialog(
            onDismissRequest = { showRatingDialog = false; ratingEstrellas = 0 },
            icon = {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Calificar AgroScan AI", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "¿Qué tan buena te parece la aplicación?",
                        fontSize = 14.sp,
                        color = GrisHumo
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= ratingEstrellas) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$i estrellas",
                                tint = if (i <= ratingEstrellas) Color(0xFFF57F17) else Color(0xFFCCCCCC),
                                modifier = Modifier
                                    .size(42.dp)
                                    .clickable { ratingEstrellas = i }
                                    .padding(2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val texto = when (ratingEstrellas) {
                        1 -> "Muy mala"
                        2 -> "Regular"
                        3 -> "Buena"
                        4 -> "Muy buena"
                        5 -> "Excelente!"
                        else -> "Toca las estrellas para calificar"
                    }
                    Text(texto, fontSize = 14.sp, color = if (ratingEstrellas > 0) Color(0xFFF57F17) else GrisMedio, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRatingDialog = false; ratingEstrellas = 0 },
                    enabled = ratingEstrellas > 0,
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                    shape   = RoundedCornerShape(10.dp)
                ) {
                    Text("Enviar calificación", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRatingDialog = false; ratingEstrellas = 0 }) {
                    Text("Cancelar", color = GrisHumo)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Diálogo de cierre de sesión ──────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = RojoAlerta, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Cerrar sesión", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas cerrar sesión? Tendrás que volver a iniciar sesión para usar la app.",
                    fontSize = 14.sp,
                    color = GrisHumo
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        perfilViewModel.signOut()
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RojoAlerta),
                    shape  = RoundedCornerShape(10.dp)
                ) {
                    Text("Cerrar sesión", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = GrisHumo)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
        Text(label, fontSize = 11.sp, color = GrisMedio)
    }
}

@Composable
private fun SeccionLabel(texto: String) {
    Text(
        text     = texto,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color    = GrisMedio,
        modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
private fun MenuCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    titulo: String,
    subtitulo: String = "",
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
            if (subtitulo.isNotBlank()) {
                Text(subtitulo, fontSize = 12.sp, color = GrisMedio, modifier = Modifier.padding(top = 1.dp))
            }
        }
        if (showArrow) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrisMedio, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MenuDivider() {
    Divider(
        modifier  = Modifier.padding(start = 70.dp, end = 16.dp),
        color     = Color(0xFFF0F0F0),
        thickness = 0.8.dp
    )
}
