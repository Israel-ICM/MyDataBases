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
    
    /** Fondo principal - gradiente sutil violeta/turquesa */
    val BackgroundPrimary = Color(0xFFF5F6FA)  // Mismo que brand_light_bg
    val BackgroundGradientStart = Color(0xFFF8F8FF)  // Blanco con toque violeta visible
    val BackgroundGradientEnd = Color(0xFFF5FFFE)  // Blanco con toque turquesa visible
    
    /** Fondo de cards - blanco puro */
    val SurfacePrimary = Color.White
    
    /** Texto principal - negro profundo del branded */
    val TextPrimary = Color(0xFF1A1F2E)  // brand_light_on_bg
    
    /** Texto secundario - gris violáceo del branded */
    val TextSecondary = Color(0xFF5B5F7D)  // brand_outline
    
    /** Texto terciario - violeta muy claro */
    val TextTertiary = Color(0xFF9DA1C0)
    
    /** Acento primario - violeta brillante branded */
    val AccentPrimary = Color(0xFF7C80E8)  // brand_primary
    val AccentPrimaryLight = Color(0xFFE6E7FF)  // Violeta muy claro
    val AccentPrimaryDark = Color(0xFF5B5EC8)  // Violeta oscuro
    
    /** Acento secundario - turquesa menta branded */
    val AccentSecondary = Color(0xFF8EE3D3)  // brand_tertiary
    val AccentSecondaryLight = Color(0xFFB3F5EA)  // Turquesa claro
    
    /** Acento de éxito - turquesa oscuro branded */
    val AccentSuccess = Color(0xFF006B63)  // Turquesa oscuro
    val AccentSuccessLight = Color(0xFFA3F2E6)  // Turquesa muy claro
    
    /** Separadores - violeta muy claro */
    val Separator = Color(0xFFE3E5F0)
    
    /** Fondo de íconos - violeta muy claro */
    val IconBackground = Color(0xFFF0F1FF)
    
    /** Color de íconos normales - gris violáceo */
    val IconNormal = Color(0xFF75788C)
    
    /** Backdrop/scrim para modales y overlays - blanco transparente claro */
    val BackdropScrim = Color.White.copy(alpha = 0.4f)
    
    
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
    
    /** Radio de bordes de cards - mucho más redondeados */
    val CardCornerRadius = 24.dp
    
    /** Elevación de sombra de cards - pronunciada */
    val CardElevation = 12.dp
    
    /** Sombra con color de acento - violeta branded */
    val CardShadowColor = Color(0xFF7C80E8).copy(alpha = 0.15f)
    
    /** Radio de bordes de íconos */
    val IconCornerRadius = 16.dp
}
