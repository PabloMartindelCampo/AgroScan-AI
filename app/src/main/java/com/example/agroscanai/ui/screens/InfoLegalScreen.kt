package com.example.agroscanai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agroscanai.ui.components.AgroBottomBar
import com.example.agroscanai.ui.components.PaginaActual
import com.example.agroscanai.ui.theme.*

enum class TipoInfoLegal {
    ACERCA, TERMINOS, PRIVACIDAD, COOKIES, AYUDA
}

@Composable
fun InfoLegalScreen(
    tipo: TipoInfoLegal,
    onBackClick: () -> Unit = {},
    onNotificacionesClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onPerfilClick: () -> Unit = {}
) {
    val config = infoConfig(tipo)

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
            .background(Color(0xFFF0F4F0))
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(VerdeBosqueOscuro, VerdeBosque)))
                .padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(config.icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text(config.titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                if (config.subtitulo.isNotBlank()) {
                    Text(config.subtitulo, fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Contenido de la sección
        config.secciones.forEach { seccion ->
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (seccion.titulo.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (seccion.icono != null) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(VerdeLima, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(seccion.icono, contentDescription = null, tint = VerdeBosque, modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(seccion.titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = VerdeBosque)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(
                        text       = seccion.cuerpo,
                        fontSize   = 13.sp,
                        color      = GrisHumo,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Fecha de última actualización
        Text(
            text      = config.ultimaActualizacion,
            fontSize  = 11.sp,
            color     = GrisMedio,
            modifier  = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(24.dp))
    } // end Column
    } // end Scaffold content
}

// Datos de configuración

private data class SeccionInfo(
    val titulo: String = "",
    val cuerpo: String,
    val icono: ImageVector? = null
)

private data class InfoConfig(
    val titulo: String,
    val subtitulo: String = "",
    val icono: ImageVector,
    val secciones: List<SeccionInfo>,
    val ultimaActualizacion: String = ""
)

private fun infoConfig(tipo: TipoInfoLegal): InfoConfig = when (tipo) {

    TipoInfoLegal.ACERCA -> InfoConfig(
        titulo    = "Acerca de AgroScan AI",
        subtitulo = "Versión 1.0.0",
        icono     = Icons.Filled.Info,
        secciones = listOf(
            // REVISAR (sin dron): "combina drones con" -> "combina fotos de hojas tomadas con el celular e"
            SeccionInfo(
                titulo = "¿Qué es AgroScan AI?",
                icono  = Icons.Filled.Agriculture,
                cuerpo = "AgroScan AI es una aplicación móvil de tecnología agrícola de precisión que combina fotos de hojas tomadas con el celular e inteligencia artificial para ayudar a los agricultores a monitorear, analizar y gestionar la salud de sus cultivos de manera eficiente y en tiempo real."
            ),
            SeccionInfo(
                titulo = "Nuestra misión",
                icono  = Icons.Filled.EmojiObjects,
                cuerpo = "Democratizar el acceso a la tecnología agrícola de precisión, permitiendo que productores de cualquier escala puedan tomar decisiones informadas basadas en datos reales de sus parcelas, reduciendo costos, minimizando el uso de agroquímicos y maximizando el rendimiento de sus cultivos."
            ),
            // REVISAR (sin dron): se quita MAVLink/drones/análisis espectral/imágenes aéreas -> "fotos de hojas tomadas con la cámara del celular"
            SeccionInfo(
                titulo = "Tecnología",
                icono  = Icons.Filled.Sensors,
                cuerpo = "La aplicación utiliza fotos de hojas tomadas con la cámara del celular, procesadas por algoritmos de inteligencia artificial para detectar niveles de humedad, nutrientes (N, P, K), presencia de plagas y estrés hídrico."
            ),
            SeccionInfo(
                titulo = "Infraestructura",
                icono  = Icons.Filled.Cloud,
                cuerpo = "AgroScan AI está construida con Jetpack Compose para Android, Firebase como plataforma de autenticación y base de datos en la nube, y Google Cloud como infraestructura de backend. Todos los datos se almacenan de forma segura y cifrada."
            ),
            // REVISAR (sin dron): se quita la línea "Compatibilidad: Drones con protocolo MAVLink v1/v2"
            SeccionInfo(
                titulo = "Versión y compatibilidad",
                cuerpo = "Versión: 1.0.0\nAndroid: 8.0 (API 26) o superior\nÚltima actualización: Abril 2026"
            )
        ),
        ultimaActualizacion = "Última actualización: Abril 2026"
    )

    TipoInfoLegal.TERMINOS -> InfoConfig(
        titulo    = "Términos y Condiciones",
        subtitulo = "Acuerdo de uso de AgroScan AI",
        icono     = Icons.Filled.Description,
        secciones = listOf(
            SeccionInfo(
                titulo = "1. Aceptación de términos",
                icono  = Icons.Filled.Gavel,
                cuerpo = "Al descargar, instalar o usar AgroScan AI, usted acepta quedar vinculado por estos Términos y Condiciones. Si no está de acuerdo con alguna parte de estos términos, no podrá acceder al servicio. Estos términos se aplican a todos los visitantes, usuarios y otras personas que accedan o usen el servicio."
            ),
            // REVISAR (sin dron): "integración con drones" -> "fotos de hojas"
            SeccionInfo(
                titulo = "2. Descripción del servicio",
                icono  = Icons.Filled.AppRegistration,
                cuerpo = "AgroScan AI proporciona herramientas de monitoreo agrícola mediante fotos de hojas y análisis de inteligencia artificial. El servicio incluye gestión de cultivos, análisis de salud de parcelas, detección de plagas y nutrientes, visualización de mapas y generación de reportes."
            ),
            SeccionInfo(
                titulo = "3. Uso del servicio",
                icono  = Icons.Filled.Rule,
                cuerpo = "Usted acepta usar AgroScan AI únicamente para fines legales y de conformidad con estos Términos. No está permitido: (a) usar el servicio de manera que viole cualquier ley o regulación aplicable; (b) transmitir material dañino, ofensivo o ilegal; (c) intentar obtener acceso no autorizado a cualquier parte del servicio; (d) usar el servicio para usos distintos al monitoreo agrícola legítimo."
            ),
            // REVISAR (sin dron): se eliminó la cláusula "4. Responsabilidad sobre drones" (la numeración no se modificó)
            SeccionInfo(
                titulo = "5. Precisión de los datos",
                icono  = Icons.Filled.Analytics,
                cuerpo = "Los datos generados por AgroScan AI son de carácter orientativo y no deben ser utilizados como única fuente para decisiones críticas de cultivo. La aplicación no garantiza la exactitud absoluta de los análisis de IA. Se recomienda complementar la información con el criterio de agrónomos certificados."
            ),
            SeccionInfo(
                titulo = "6. Cuentas de usuario",
                cuerpo = "Usted es responsable de mantener la confidencialidad de su cuenta y contraseña, así como de todas las actividades que ocurran bajo su cuenta. Notifíquenos de inmediato sobre cualquier uso no autorizado de su cuenta."
            ),
            SeccionInfo(
                titulo = "7. Modificaciones",
                cuerpo = "Nos reservamos el derecho de modificar estos términos en cualquier momento. Los cambios entrarán en vigencia inmediatamente después de su publicación en la aplicación. El uso continuado del servicio después de dichos cambios constituye su aceptación de los nuevos términos."
            )
        ),
        ultimaActualizacion = "Última actualización: Abril 2026"
    )

    TipoInfoLegal.PRIVACIDAD -> InfoConfig(
        titulo    = "Aviso de Privacidad",
        subtitulo = "Cómo protegemos tus datos",
        icono     = Icons.Filled.Lock,
        secciones = listOf(
            SeccionInfo(
                titulo = "1. Datos que recopilamos",
                icono  = Icons.Filled.DataUsage,
                cuerpo = "Recopilamos la siguiente información: Datos de registro (nombre, apellido, correo electrónico, teléfono y región), Datos agrícolas (nombre de parcelas, tipo de cultivo, ubicaciones GPS, resultados de escaneos), Datos de uso (interacciones con la app, frecuencia de escaneos), Información del dispositivo (modelo, versión de Android, identificadores únicos para diagnóstico)."
            ),
            SeccionInfo(
                titulo = "2. Uso de la información",
                icono  = Icons.Filled.ManageSearch,
                cuerpo = "Utilizamos sus datos para: Proveer y mantener el servicio de AgroScan AI, mejorar y personalizar la experiencia del usuario, enviar notificaciones relacionadas con sus cultivos, generar análisis estadísticos anónimos para mejorar los algoritmos de IA, cumplir con obligaciones legales, y comunicarnos con usted sobre actualizaciones del servicio."
            ),
            SeccionInfo(
                titulo = "3. Almacenamiento y seguridad",
                icono  = Icons.Filled.Security,
                cuerpo = "Sus datos se almacenan en Firebase Firestore (Google Cloud) con cifrado en tránsito (SSL/TLS) y en reposo. Implementamos medidas de seguridad técnicas y organizativas para proteger su información contra acceso no autorizado, pérdida o alteración. Sin embargo, ningún método de transmisión por Internet o almacenamiento electrónico es 100% seguro."
            ),
            SeccionInfo(
                titulo = "4. Compartición de datos",
                icono  = Icons.Filled.Share,
                cuerpo = "No vendemos, comercializamos ni transferimos a terceros su información personal identificable, excepto: Proveedores de servicios que nos asisten en la operación del servicio (Google Firebase, Google Cloud) bajo acuerdos de confidencialidad, cuando sea requerido por ley o autoridad competente, o con su consentimiento explícito."
            ),
            SeccionInfo(
                titulo = "5. Sus derechos",
                icono  = Icons.Filled.AdminPanelSettings,
                cuerpo = "Usted tiene derecho a: Acceder a sus datos personales, corregir información inexacta, solicitar la eliminación de sus datos, oponerse al procesamiento de sus datos, portabilidad de datos. Para ejercer estos derechos, contáctenos a través de la sección de Soporte y Ayuda."
            ),
            SeccionInfo(
                titulo = "6. Retención de datos",
                cuerpo = "Conservamos sus datos mientras su cuenta esté activa o según sea necesario para proveer el servicio. Si solicita eliminar su cuenta, eliminaremos sus datos dentro de 30 días hábiles, excepto cuando sea requerido conservarlos por obligaciones legales."
            ),
            SeccionInfo(
                titulo = "7. Contacto",
                cuerpo = "Si tiene preguntas sobre este aviso de privacidad o el tratamiento de sus datos personales, puede contactarnos a través de la sección Soporte y Ayuda dentro de la aplicación."
            )
        ),
        ultimaActualizacion = "Última actualización: Abril 2026"
    )

    TipoInfoLegal.COOKIES -> InfoConfig(
        titulo    = "Política de Cookies",
        subtitulo = "Uso de cookies y tecnologías similares",
        icono     = Icons.Filled.Cookie,
        secciones = listOf(
            SeccionInfo(
                titulo = "¿Qué son las cookies?",
                icono  = Icons.Filled.Info,
                cuerpo = "Las cookies son pequeños archivos de texto que se almacenan en su dispositivo cuando utiliza nuestra aplicación. Junto con tecnologías similares como tokens de sesión y almacenamiento local, nos permiten recordar sus preferencias y mejorar su experiencia."
            ),
            SeccionInfo(
                titulo = "Cookies esenciales",
                icono  = Icons.Filled.Key,
                cuerpo = "Tokens de sesión de Firebase Authentication: Necesarios para mantener su sesión iniciada de forma segura. Sin estas cookies, no podría permanecer conectado a su cuenta entre sesiones. Estos tokens se almacenan de forma cifrada y tienen un período de expiración automático por seguridad."
            ),
            SeccionInfo(
                titulo = "Almacenamiento local",
                icono  = Icons.Filled.Storage,
                cuerpo = "Utilizamos almacenamiento local (DataStore de Android) para guardar sus preferencias de configuración como unidades de medida, preferencias de notificación y ajustes de la interfaz. Estos datos permanecen en su dispositivo y no se transmiten a servidores externos."
            ),
            SeccionInfo(
                titulo = "Cookies de terceros",
                icono  = Icons.Filled.Cloud,
                cuerpo = "Google Firebase y Google Analytics pueden usar sus propias tecnologías de seguimiento para análisis de rendimiento y diagnóstico de errores. Estas tecnologías están sujetas a las políticas de privacidad de Google. Para más información, consulte la Política de Privacidad de Google."
            ),
            SeccionInfo(
                titulo = "Control de cookies",
                cuerpo = "Puede controlar las cookies y el almacenamiento local a través de la configuración de su dispositivo Android. Tenga en cuenta que deshabilitar ciertas cookies puede afectar la funcionalidad de la aplicación, como la capacidad de mantener su sesión iniciada."
            )
        ),
        ultimaActualizacion = "Última actualización: Abril 2026"
    )

    TipoInfoLegal.AYUDA -> InfoConfig(
        titulo    = "Soporte y Ayuda",
        subtitulo = "Preguntas frecuentes y contacto",
        icono     = Icons.Filled.HelpOutline,
        secciones = listOf(
            // REVISAR (sin dron): pasos del dron reemplazados por los pasos del escaneo por foto; ícono FlightTakeoff -> PhotoCamera
            SeccionInfo(
                titulo = "¿Cómo escaneo una parcela?",
                icono  = Icons.Filled.PhotoCamera,
                cuerpo = "1. Ve a 'Escanear Parcela' desde el menú principal.\n2. Selecciona la parcela que deseas escanear.\n3. Presiona 'Tomar foto' y fotografía una sola hoja, de cerca y con buena luz, o elige una foto de la galería.\n4. La foto se guarda solo en tu teléfono; en Firebase se guardan la fecha, la ubicación y el resultado.\n5. Mientras el modelo de IA no esté disponible, el resultado aparece como 'Pendiente de análisis'."
            ),
            // REVISAR (sin dron): se eliminó la pregunta "¿Qué drones son compatibles?"
            SeccionInfo(
                titulo = "¿Por qué no aparecen mis cultivos?",
                icono  = Icons.Filled.Agriculture,
                cuerpo = "Verifica los siguientes puntos: Asegúrate de tener conexión a internet activa, confirma que iniciaste sesión con la cuenta correcta, ve a 'Mis Cultivos' y espera que carguen los datos, si el problema persiste cierra y vuelve a abrir la aplicación. Si aún no aparecen, puede ser un problema temporal de conectividad con Firebase."
            ),
            SeccionInfo(
                titulo = "¿Cómo interpreto el índice de salud?",
                icono  = Icons.Filled.BarChart,
                cuerpo = "El índice de salud va de 0% a 100% y considera: Niveles de humedad del suelo (óptimo: 25-55%), Nitrógeno (N), Fósforo (P) y Potasio (K) en ppm, Detección de plagas (reduce significativamente el índice). Verde (70-100%): Cultivo saludable. Naranja (40-69%): Requiere atención. Rojo (0-39%): Estado crítico, intervención inmediata."
            ),
            SeccionInfo(
                titulo = "¿Cómo recupero mi contraseña?",
                icono  = Icons.Filled.LockReset,
                cuerpo = "En la pantalla de inicio de sesión, presiona '¿Olvidaste tu contraseña?'. Ingresa tu correo electrónico registrado y presiona 'Enviar'. Recibirás un correo de Firebase con un enlace seguro para restablecer tu contraseña. Revisa también tu carpeta de spam si no lo ves en unos minutos."
            ),
            SeccionInfo(
                titulo = "Contacto y soporte",
                icono  = Icons.Filled.Email,
                cuerpo = "Para soporte técnico, reportar bugs o sugerencias de mejora:\n\nCorreo: soporte@agroscanai.com\nHorario de atención: Lunes a Viernes, 8:00 - 18:00 (hora local)\n\nPara reportar problemas urgentes o de seguridad, incluye en tu mensaje: la versión de la app, modelo de dispositivo, descripción detallada del problema y capturas de pantalla si es posible."
            )
        ),
        ultimaActualizacion = "Última actualización: Abril 2026"
    )
}
