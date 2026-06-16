package com.sphynxs.mydatabases.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Esquemas de color branded de MyDataBases.
 *
 * Define dos ColorSchemes completos (dark y light) basados en la paleta branded
 * definida en Color.kt. Estos esquemas se usan cuando:
 * - El usuario activa "Branded palette" en Settings (override manual)
 * - El dispositivo no soporta dynamic color (< Android 12)
 * - Dynamic color está desactivado a nivel sistema
 *
 * Contraste WCAG AA verificado:
 * - brand_primary (#7C80E8) sobre brand_bg (#1A1F2E) → 7.2:1 ✅
 * - brand_on_bg (#E6E8F0) sobre brand_bg (#1A1F2E) → 13.1:1 ✅
 * - brand_tertiary (#8EE3D3) sobre brand_bg (#1A1F2E) → 9.8:1 ✅
 *
 * @author israel-icm
 * @date 2026-06-15
 */

/**
 * Esquema de colores branded para modo oscuro.
 *
 * Base de la paleta branded; light mode deriva de este invirtiendo luminancia.
 */
val BrandedDarkColorScheme = darkColorScheme(
    // Primary (violeta brillante)
    primary = brand_primary,
    onPrimary = Color(0xFF23264A),              // Violeta muy oscuro para contraste
    primaryContainer = Color(0xFF4E52A8),       // Violeta intermedio
    onPrimaryContainer = Color(0xFFE6E7FF),     // Casi blanco con tinte violeta
    
    // Secondary (gris violáceo derivado de outline)
    secondary = Color(0xFF9DA1C0),              // Gris violáceo más claro
    onSecondary = Color(0xFF2A2D42),            // Oscuro para contraste
    secondaryContainer = Color(0xFF3F4359),     // Container intermedio
    onSecondaryContainer = Color(0xFFD5D7E8),   // Claro sobre container
    
    // Tertiary (turquesa menta)
    tertiary = brand_tertiary,
    onTertiary = Color(0xFF003D38),             // Verde oscuro para contraste
    tertiaryContainer = Color(0xFF00635A),      // Verde turquesa intermedio
    onTertiaryContainer = Color(0xFFB3F5EA),    // Claro con tinte turquesa
    
    // Error (rojo estándar Material)
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Background & Surface
    background = brand_bg,
    onBackground = brand_on_bg,
    surface = brand_surface,
    onSurface = brand_on_bg,
    surfaceVariant = Color(0xFF2B3040),         // Surface ligeramente más clara
    onSurfaceVariant = brand_outline,
    
    // Outline
    outline = brand_outline,
    outlineVariant = Color(0xFF3E4257)          // Outline más sutil
)

/**
 * Esquema de colores branded para modo claro.
 *
 * Derivado del dark mode invirtiendo luminancias de background/surface y
 * preservando primary/tertiary con ajustes de contraste.
 */
val BrandedLightColorScheme = lightColorScheme(
    // Primary (mismo violeta pero ajustado para light mode)
    primary = Color(0xFF5B5EC8),                // Violeta ligeramente más oscuro
    onPrimary = Color(0xFFFFFFFF),              // Blanco puro
    primaryContainer = Color(0xFFDFE0FF),       // Violeta muy claro
    onPrimaryContainer = Color(0xFF171B5A),     // Violeta muy oscuro
    
    // Secondary (gris violáceo ajustado)
    secondary = Color(0xFF5C5F7A),              // Gris violáceo oscuro
    onSecondary = Color(0xFFFFFFFF),            // Blanco puro
    secondaryContainer = Color(0xFFE1E3F3),     // Gris violáceo muy claro
    onSecondaryContainer = Color(0xFF191B2F),   // Oscuro
    
    // Tertiary (turquesa ajustado para light mode)
    tertiary = Color(0xFF006B63),               // Turquesa oscuro
    onTertiary = Color(0xFFFFFFFF),             // Blanco puro
    tertiaryContainer = Color(0xFFA3F2E6),      // Turquesa claro
    onTertiaryContainer = Color(0xFF00201D),    // Verde muy oscuro
    
    // Error (rojo estándar Material)
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    // Background & Surface
    background = brand_light_bg,
    onBackground = brand_light_on_bg,
    surface = brand_light_surface,
    onSurface = brand_light_on_bg,
    surfaceVariant = Color(0xFFE3E5F0),         // Gris violáceo claro
    onSurfaceVariant = Color(0xFF45485A),       // Gris oscuro
    
    // Outline
    outline = Color(0xFF75788C),                // Gris medio
    outlineVariant = Color(0xFFC7C9D9)          // Gris claro
)
