package com.example.agroscanai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.agroscanai.ui.screens.*
import com.example.agroscanai.ui.viewmodel.AuthViewModel
import com.example.agroscanai.ui.viewmodel.CalendarioViewModel
import com.example.agroscanai.ui.viewmodel.CultivosViewModel
import com.example.agroscanai.ui.viewmodel.PerfilViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

object Routes {
    const val SPLASH               = "splash"
    const val ONBOARDING           = "onboarding"
    const val WELCOME              = "welcome"
    const val LOGIN                = "login"
    const val RECUPERAR_CONTRASENA = "recuperar_contrasena"
    const val REGISTER             = "register"
    const val VERIFICACION_EMAIL   = "verificacion_email/{email}"
    const val BIENVENIDA           = "bienvenida/{nombre}"
    const val HOME                 = "home"
    const val MIS_CULTIVOS         = "mis_cultivos"
    const val MAPA_LOTES           = "mapa_lotes"
    const val DASHBOARD_SALUD         = "dashboard_salud"
    const val DASHBOARD_SALUD_DETALLE = "dashboard_salud_detalle/{cultivoId}"
    const val REPORTES_IA          = "reportes_ia"
    const val CLIMA                = "clima"
    const val CALENDARIO           = "calendario"
    const val PRECIOS              = "precios"
    const val ESCANEO              = "escaneo"
    const val NOTIFICACIONES       = "notificaciones"
    const val DETALLE_CULTIVO      = "detalle_cultivo/{cultivoId}"
    const val DETALLE_ESCANEO      = "detalle_escaneo/{cultivoId}/{escaneoId}"

    const val PERFIL               = "perfil"
    const val PERFIL_EDITAR        = "perfil_editar"
    const val PERFIL_CONFIGURACION = "perfil_configuracion"
    const val PERFIL_INFO          = "perfil_info/{tipo}"

    fun verificacionEmail(email: String)    = "verificacion_email/$email"
    fun bienvenida(nombre: String)          = "bienvenida/${nombre.ifBlank { "Usuario" }}"
    fun detallesCultivo(cultivoId: String)  = "detalle_cultivo/$cultivoId"
    fun detallesEscaneo(cultivoId: String, escaneoId: String) = "detalle_escaneo/$cultivoId/$escaneoId"
    fun dashboardSaludDetalle(id: String)   = "dashboard_salud_detalle/$id"
    fun perfilInfo(tipo: String)            = "perfil_info/$tipo"
}

@Composable
fun NavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel           = viewModel()
    val cultivosViewModel: CultivosViewModel   = viewModel()
    val perfilViewModel: PerfilViewModel       = viewModel()
    val calendarioViewModel: CalendarioViewModel = viewModel()

    fun goNotificaciones() = navController.navigate(Routes.NOTIFICACIONES)

    fun signOut() {
        navController.navigate(Routes.WELCOME) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    val user = Firebase.auth.currentUser
                    val destino = if (user != null && user.isEmailVerified) Routes.HOME else Routes.ONBOARDING
                    navController.navigate(destino) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onLoginClick    = { navController.navigate(Routes.LOGIN) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onBackClick    = { navController.popBackStack() },
                onLoginSuccess = { nombre ->
                    navController.navigate(Routes.bienvenida(nombre)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onForgotPassword = { navController.navigate(Routes.RECUPERAR_CONTRASENA) },
                onRegisterClick  = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.RECUPERAR_CONTRASENA) {
            RecuperarContrasenaScreen(
                onBackClick   = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackClick        = { navController.popBackStack() },
                onVerificationSent = { email -> navController.navigate(Routes.verificacionEmail(email)) },
                onRegisterSuccess  = { nombre ->
                    navController.navigate(Routes.bienvenida(nombre)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.VERIFICACION_EMAIL) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerificacionEmailScreen(
                email       = email,
                onBackClick = {
                    navController.navigate(Routes.REGISTER) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onVerified    = { nombre ->
                    navController.navigate(Routes.bienvenida(nombre)) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(Routes.BIENVENIDA) { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre") ?: ""
            BienvenidaScreen(
                nombreUsuario = nombre,
                onFinished    = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.BIENVENIDA) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onMenuItemClick       = { route -> navController.navigate(route) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) }
            )
        }

        composable(Routes.MAPA_LOTES) {
            MapaLotesScreen(
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.MIS_CULTIVOS) {
            MisCultivosScreen(
                onHomeClick              = { navController.navigate(Routes.HOME) },
                onNotificacionesClick    = { goNotificaciones() },
                onPerfilClick            = { navController.navigate(Routes.PERFIL) },
                onDetalleCultivoClick    = { id -> navController.navigate(Routes.detallesCultivo(id)) },
                cultivosViewModel        = cultivosViewModel
            )
        }

        composable(Routes.ESCANEO) {
            EscanearParcelaScreen(
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                onVerEscaneo          = { cultivoId, escaneoId ->
                    navController.navigate(Routes.detallesEscaneo(cultivoId, escaneoId))
                },
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.DASHBOARD_SALUD) {
            SeleccionarCultivoDashboard(
                onCultivoSelected     = { cultivoId -> navController.navigate(Routes.dashboardSaludDetalle(cultivoId)) },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onBackClick           = { navController.popBackStack() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.DASHBOARD_SALUD_DETALLE) { backStackEntry ->
            val cultivoId = backStackEntry.arguments?.getString("cultivoId") ?: ""
            DashboardSaludScreen(
                cultivoId             = cultivoId,
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onBackClick           = { navController.popBackStack() },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.DETALLE_CULTIVO) { backStackEntry ->
            val cultivoId = backStackEntry.arguments?.getString("cultivoId") ?: ""
            DetalleCultivoScreen(
                cultivoId             = cultivoId,
                onBackClick           = { navController.popBackStack() },
                onNotificacionesClick = { goNotificaciones() },
                onCultivoEliminado    = { navController.popBackStack() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                onEscaneoClick        = { escaneoId ->
                    navController.navigate(Routes.detallesEscaneo(cultivoId, escaneoId))
                },
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.DETALLE_ESCANEO) { backStackEntry ->
            val cultivoId = backStackEntry.arguments?.getString("cultivoId") ?: ""
            val escaneoId = backStackEntry.arguments?.getString("escaneoId") ?: ""
            DetalleEscaneoScreen(
                cultivoId             = cultivoId,
                escaneoId             = escaneoId,
                onBackClick           = { navController.popBackStack() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.NOTIFICACIONES) {
            NotificacionesScreen(
                onBackClick       = { navController.popBackStack() },
                onHomeClick       = { navController.navigate(Routes.HOME) },
                onPerfilClick     = { navController.navigate(Routes.PERFIL) },
                cultivosViewModel = cultivosViewModel
            )
        }

        composable(Routes.REPORTES_IA) {
            ReportesIAScreen(
                onBackClick           = { navController.popBackStack() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.CALENDARIO) {
            CalendarioScreen(
                onBackClick           = { navController.popBackStack() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                cultivosViewModel     = cultivosViewModel,
                calendarioViewModel   = calendarioViewModel
            )
        }

        composable(Routes.CLIMA) {
            ClimaScreen(
                onBackClick           = { navController.popBackStack() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) }
            )
        }

        composable(Routes.PRECIOS) {
            PreciosScreen(
                onBackClick           = { navController.popBackStack() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) }
            )
        }

        composable(Routes.PERFIL) {
            PerfilScreen(
                onBackClick           = { navController.popBackStack() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onNotificacionesClick = { goNotificaciones() },
                onEditarPerfil        = { navController.navigate(Routes.PERFIL_EDITAR) },
                onConfiguracion       = { navController.navigate(Routes.PERFIL_CONFIGURACION) },
                onAcerca              = { navController.navigate(Routes.perfilInfo("acerca")) },
                onTerminos            = { navController.navigate(Routes.perfilInfo("terminos")) },
                onPrivacidad          = { navController.navigate(Routes.perfilInfo("privacidad")) },
                onCookies             = { navController.navigate(Routes.perfilInfo("cookies")) },
                onAyuda               = { navController.navigate(Routes.perfilInfo("ayuda")) },
                onSignOut             = { signOut() },
                perfilViewModel       = perfilViewModel,
                cultivosViewModel     = cultivosViewModel
            )
        }

        composable(Routes.PERFIL_EDITAR) {
            EditarPerfilScreen(
                onBackClick           = { navController.popBackStack() },
                onNotificacionesClick = { goNotificaciones() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                perfilViewModel       = perfilViewModel
            )
        }

        composable(Routes.PERFIL_CONFIGURACION) {
            ConfiguracionScreen(
                onBackClick           = { navController.popBackStack() },
                onNotificacionesClick = { goNotificaciones() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) },
                perfilViewModel       = perfilViewModel
            )
        }

        composable(Routes.PERFIL_INFO) { backStackEntry ->
            val tipo = backStackEntry.arguments?.getString("tipo") ?: "acerca"
            val tipoEnum = when (tipo) {
                "terminos"   -> TipoInfoLegal.TERMINOS
                "privacidad" -> TipoInfoLegal.PRIVACIDAD
                "cookies"    -> TipoInfoLegal.COOKIES
                "ayuda"      -> TipoInfoLegal.AYUDA
                else         -> TipoInfoLegal.ACERCA
            }
            InfoLegalScreen(
                tipo                  = tipoEnum,
                onBackClick           = { navController.popBackStack() },
                onNotificacionesClick = { goNotificaciones() },
                onHomeClick           = { navController.navigate(Routes.HOME) },
                onPerfilClick         = { navController.navigate(Routes.PERFIL) }
            )
        }
    }
}
