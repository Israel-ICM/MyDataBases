package com.sphynxs.mydatabases.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens de diseño unificados para toda la app.
 *
 * Define colores, tamaños, espaciados y estilos consistentes.
 *
 * @author israel-icm
 * @date 2026-06-17
 */
object DesignTokens {
    
    // ============ COLORES ============
    
    /** Fondo principal - gradiente sutil azul/morado */
    val BackgroundPrimary = Color(0xFFF8FAFC)  // Azul muy claro
    val BackgroundGradientStart = Color(0xFFF1F5F9)
    val BackgroundGradientEnd = Color(0xFFFAF5FF)  // Toque morado
    
    /** Fondo de cards - blanco con borde de color */
    val SurfacePrimary = Color.White
    
    /** Texto principal - negro profundo */
    val TextPrimary = Color(0xFF0F172A)
    
    /** Texto secundario - slate con contraste */
    val TextSecondary = Color(0xFF475569)
    
    /** Texto terciario */
    val TextTertiary = Color(0xFF94A3B8)
    
    /** Acento primario - índigo vibrante */
    val AccentPrimary = Color(0xFF6366F1)
    val AccentPrimaryLight = Color(0xFFEEF2FF)
    val AccentPrimaryDark = Color(0xFF4F46E5)
    
    /** Acento secundario - cyan eléctrico */
    val AccentSecondary = Color(0xFF06B6D4)
    val AccentSecondaryLight = Color(0xFFCFFAFE)
    
    /** Acento de éxito - verde esmeralda */
    val AccentSuccess = Color(0xFF10B981)
    val AccentSuccessLight = Color(0xFFD1FAE5)
    
    /** Separadores */
    val Separator = Color(0xFFE2E8F0)
    
    /** Fondo de íconos - gradiente */
    val IconBackground = Color(0xFFF1F5F9)
    
    /** Color de íconos normales */
    val IconNormal = Color(0xFF64748B)
    
    
    // ============ TIPOGRAFÍA ============
    
    /** Título GRANDE estilo iOS 26 (ej: "Todo lo de iCloud") */
    val LargeTitleSize = 34.sp
    val LargeTitleWeight = FontWeight.Bold
    val LargeTitleColor = TextPrimary
    
    /** Título de sección desplegable (ej: "Destacadas") */
    val SectionTitleSize = 22.sp
    val SectionTitleWeight = FontWeight.Bold
    val SectionTitleColor = TextPrimary
    
    /** Título principal de card (ej: nombre de conexión) */
    val CardTitleSize = 17.sp
    val CardTitleWeight = FontWeight.SemiBold
    val CardTitleColor = TextPrimary
    
    /** Subtítulo de card (ej: host:puerto) */
    val CardSubtitleSize = 15.sp
    val CardSubtitleWeight = FontWeight.Normal
    val CardSubtitleColor = TextSecondary
    
    /** Label pequeño (ej: badges, metadata) */
    val LabelSize = 13.sp
    val LabelWeight = FontWeight.Medium
    val LabelColor = TextSecondary
    
    /** Caption (ej: contadores, información extra) */
    val CaptionSize = 12.sp
    val CaptionWeight = FontWeight.Normal
    val CaptionColor = TextTertiary
    
    
    // ============ ESPACIADO ============
    
    /** Padding interno de cards */
    val CardPadding = 16.dp
    
    /** Espaciado entre cards en listas */
    val CardSpacing = 12.dp
    
    /** Padding horizontal de pantallas */
    val ScreenPaddingHorizontal = 16.dp
    
    /** Espaciado entre secciones */
    val SectionSpacing = 24.dp
    
    /** Espaciado entre elementos dentro de un card */
    val InnerSpacing = 12.dp
    
    
    // ============ TAMAÑOS DE ÍCONOS ============
    
    /** Ícono grande en cards principales */
    val IconLarge = 48.dp
    
    /** Ícono mediano en cards secundarios */
    val IconMedium = 40.dp
    
    /** Ícono pequeño en botones y acciones */
    val IconSmall = 24.dp
    
    
    // ============ BORDES Y SOMBRAS ============
    
    /** Radio de bordes de cards - más redondeados */
    val CardCornerRadius = 16.dp
    
    /** Elevación de sombra de cards - más pronunciada */
    val CardElevation = 4.dp
    
    /** Sombra con color de acento */
    val CardShadowColor = Color(0xFF2563EB).copy(alpha = 0.08f)
    
    /** Radio de bordes de íconos */
    val IconCornerRadius = 12.dp
}
