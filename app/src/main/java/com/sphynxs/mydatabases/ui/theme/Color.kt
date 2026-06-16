package com.sphynxs.mydatabases.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Sistema de colores Material 3 para MyDataBases.
 *
 * Define paletas completas para modo claro y oscuro. En dispositivos Android 12+,
 * los colores dinámicos (dynamic color) se generan automáticamente desde el wallpaper,
 * pero estos valores sirven como fallback para dispositivos más antiguos.
 *
 * @author israel-icm
 * @date 2026-06-12
 */

// Colores principales — Tema Claro
val md_theme_light_primary = Color(0xFF006C4C)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF89F8C7)
val md_theme_light_onPrimaryContainer = Color(0xFF002114)

// Colores secundarios — Tema Claro
val md_theme_light_secondary = Color(0xFF4A6359)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCDE8DB)
val md_theme_light_onSecondaryContainer = Color(0xFF072018)

// Colores terciarios — Tema Claro
val md_theme_light_tertiary = Color(0xFF3E6374)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFC1E8FC)
val md_theme_light_onTertiaryContainer = Color(0xFF001F2A)

// Colores de error — Tema Claro
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF410002)

// Colores de superficie — Tema Claro
val md_theme_light_background = Color(0xFFFBFDF9)
val md_theme_light_onBackground = Color(0xFF191C1A)
val md_theme_light_surface = Color(0xFFFBFDF9)
val md_theme_light_onSurface = Color(0xFF191C1A)
val md_theme_light_surfaceVariant = Color(0xFFDBE5DF)
val md_theme_light_onSurfaceVariant = Color(0xFF404943)
val md_theme_light_outline = Color(0xFF707973)
val md_theme_light_outlineVariant = Color(0xFFBFC9C3)

// Colores principales — Tema Oscuro
val md_theme_dark_primary = Color(0xFF6CDBAC)
val md_theme_dark_onPrimary = Color(0xFF003826)
val md_theme_dark_primaryContainer = Color(0xFF005138)
val md_theme_dark_onPrimaryContainer = Color(0xFF89F8C7)

// Colores secundarios — Tema Oscuro
val md_theme_dark_secondary = Color(0xFFB1CCBF)
val md_theme_dark_onSecondary = Color(0xFF1C352C)
val md_theme_dark_secondaryContainer = Color(0xFF334B42)
val md_theme_dark_onSecondaryContainer = Color(0xFFCDE8DB)

// Colores terciarios — Tema Oscuro
val md_theme_dark_tertiary = Color(0xFFA5CCDF)
val md_theme_dark_onTertiary = Color(0xFF073544)
val md_theme_dark_tertiaryContainer = Color(0xFF254B5B)
val md_theme_dark_onTertiaryContainer = Color(0xFFC1E8FC)

// Colores de error — Tema Oscuro
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

// Colores de superficie — Tema Oscuro
val md_theme_dark_background = Color(0xFF191C1A)
val md_theme_dark_onBackground = Color(0xFFE1E3DF)
val md_theme_dark_surface = Color(0xFF191C1A)
val md_theme_dark_onSurface = Color(0xFFE1E3DF)
val md_theme_dark_surfaceVariant = Color(0xFF404943)
val md_theme_dark_onSurfaceVariant = Color(0xFFBFC9C3)
val md_theme_dark_outline = Color(0xFF89938D)
val md_theme_dark_outlineVariant = Color(0xFF404943)

// ══════════════════════════════════════════════════════════════════════════════
// Branded Palette — Sistema de colores personalizado de MyDataBases
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Paleta branded de MyDataBases — dark-first.
 *
 * Estos colores definen la identidad visual única de la app cuando el usuario
 * opta por no usar dynamic color (Android 12+) o en dispositivos anteriores.
 *
 * Dark mode (base):
 * - #1A1F2E → background (azul oscuro profundo)
 * - #222837 → surface (azul grisáceo oscuro)
 * - #7C80E8 → primary (violeta brillante)
 * - #8EE3D3 → tertiary (turquesa menta)
 * - #5B5F7D → outline / onSurfaceVariant (gris violáceo)
 * - #E6E8F0 → onBackground / onSurface (gris muy claro)
 *
 * @author israel-icm
 * @date 2026-06-15
 */

// Tokens de color branded — Dark mode
val brand_bg = Color(0xFF1A1F2E)           // Background principal
val brand_surface = Color(0xFF222837)      // Surface elevada
val brand_primary = Color(0xFF7C80E8)      // Primary (violeta)
val brand_tertiary = Color(0xFF8EE3D3)     // Tertiary (turquesa)
val brand_outline = Color(0xFF5B5F7D)      // Outline y onSurfaceVariant
val brand_on_bg = Color(0xFFE6E8F0)        // onBackground y onSurface

// Tokens de color branded — Light mode (derivados por inversión de luminancia)
val brand_light_bg = Color(0xFFF5F6FA)         // Background claro (inverso)
val brand_light_surface = Color(0xFFFFFFFF)    // Surface clara (blanco puro)
val brand_light_on_bg = Color(0xFF1A1F2E)      // onBackground oscuro (inverso)
