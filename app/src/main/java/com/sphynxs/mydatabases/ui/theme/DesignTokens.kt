package com.sphynxs.mydatabases.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens de diseño unificados para toda la app — theme-aware (light/dark).
 *
 * Define colores, tamaños, espaciados y estilos consistentes. Los campos de color
 * varían por tema (ver [buildDesignTokens]); typography/spacing/icon-size son
 * theme-INVARIANTES pero viven aquí también, para un único patrón de acceso.
 *
 * Se expone vía [LocalDesignTokens], provisto por `AppTheme` con [LightDesignTokens]
 * o [DarkDesignTokens] según el `darkTheme` resuelto — ver `openspec/changes/dark-mode`.
 *
 * @author israel-icm (theme-aware conversion: gentle-ai, PR-2)
 * @date 2026-06-17
 */
@Immutable
data class DesignTokens(
    // ============ COLORES ============
    val backgroundPrimary: Color,
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val surfacePrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentPrimary: Color,
    val accentPrimaryLight: Color,
    val accentPrimaryDark: Color,
    val accentSecondary: Color,
    val accentSecondaryLight: Color,
    val accentSuccess: Color,
    val accentSuccessLight: Color,
    val separator: Color,
    val iconBackground: Color,
    val iconNormal: Color,
    val destructiveAction: Color,
    val backdropScrim: Color,

    // ============ TIPOGRAFÍA ============
    val largeTitleSize: TextUnit,
    val largeTitleWeight: FontWeight,
    val largeTitleColor: Color,
    val sectionTitleSize: TextUnit,
    val sectionTitleWeight: FontWeight,
    val sectionTitleColor: Color,
    val cardTitleSize: TextUnit,
    val cardTitleWeight: FontWeight,
    val cardTitleColor: Color,
    val cardSubtitleSize: TextUnit,
    val cardSubtitleWeight: FontWeight,
    val cardSubtitleColor: Color,
    val labelSize: TextUnit,
    val labelWeight: FontWeight,
    val labelColor: Color,
    val captionSize: TextUnit,
    val captionWeight: FontWeight,
    val captionColor: Color,

    // ============ ESPACIADO ============
    val cardPadding: Dp,
    val cardSpacing: Dp,
    val screenPaddingHorizontal: Dp,
    val sectionSpacing: Dp,
    val innerSpacing: Dp,

    // ============ TAMAÑOS DE ÍCONOS ============
    val iconLarge: Dp,
    val iconMedium: Dp,
    val iconSmall: Dp,

    // ============ BORDES Y SOMBRAS ============
    val cardCornerRadius: Dp,
    val cardElevation: Dp,
    val cardShadowColor: Color,
    val iconCornerRadius: Dp
)

/**
 * CompositionLocal para exponer [DesignTokens] a toda la jerarquía de Composables,
 * mirroring el patrón de `LocalAppSpacing`/`LocalAppShapes`.
 *
 * `AppTheme` SIEMPRE lo provee explícitamente ([LightDesignTokens] o [DarkDesignTokens]
 * según `darkTheme` resuelto); el default acá es solo un fallback de seguridad para
 * contextos que leen `LocalDesignTokens.current` sin pasar por `AppTheme` (no debería
 * ocurrir en producción).
 *
 * Uso:
 * ```kotlin
 * val tokens = LocalDesignTokens.current
 * Text(color = tokens.textPrimary)
 * ```
 *
 * @author gentle-ai (PR-2)
 * @date 2026-07-13
 */
val LocalDesignTokens = staticCompositionLocalOf { LightDesignTokens }

/** Instancia de [DesignTokens] para tema claro, derivada de [BrandedLightColorScheme]. */
val LightDesignTokens: DesignTokens = buildDesignTokens(BrandedLightColorScheme, darkTheme = false)

/** Instancia de [DesignTokens] para tema oscuro, derivada de [BrandedDarkColorScheme]. */
val DarkDesignTokens: DesignTokens = buildDesignTokens(BrandedDarkColorScheme, darkTheme = true)

/**
 * Deriva un [DesignTokens] completo a partir de un [ColorScheme] branded resuelto
 * (light o dark). Función pura — sin dependencias de Compose runtime más allá de
 * `ColorScheme`/`Color` — extraída para test unitario directo (ver `DesignTokensTest`).
 *
 * `DesignTokens` permanece anclado a la paleta BRANDED (no a dynamic color) — preserva
 * la identidad visual de marca de los componentes iOS-style (cards, botones, sombras)
 * independientemente de si `brandedPaletteEnabled` está OFF y `MaterialTheme.colorScheme`
 * usa dynamic color; ver design.md Architecture Decisions ("Preserves branded WCAG AA
 * identity").
 *
 * Decisiones de contraste WCAG AA tomadas en esta derivación (ver apply-progress para el
 * detalle de cálculo):
 * - `textSecondary` deriva de `scheme.secondary`, NO de `scheme.outline` — `outline` en
 *   `BrandedDarkColorScheme` da solo 2.37:1 contra `surface` (falla AA); `secondary` da
 *   5.81:1 (pasa AA). En light ambos son casi idénticos visualmente (6.22 vs 6.23:1).
 * - `textPrimary`/`backgroundPrimary`/`surfacePrimary` derivan 1:1 de `onBackground`/
 *   `background`/`surface` — coinciden exactamente con los valores light ya shippeados.
 * - `iconNormal` deriva de `scheme.outline` (coincide exacto con el valor light actual,
 *   4.36:1 — sobrado para contraste no-textual 3:1 de WCAG 1.4.11).
 * - `separator` deriva de `scheme.surfaceVariant` (coincide exacto con el valor light
 *   actual).
 * - `accentPrimary`/`accentSecondary`/`destructiveAction`/`cardShadowColor` permanecen
 *   theme-INVARIANTES (colores de identidad de marca, ya verificados en ambos fondos vía
 *   `BrandedDarkColorScheme`/`BrandedLightColorScheme` — ver `BrandedColors.kt`).
 * - `backdropScrim` deriva de `scheme.background` (antes hardcoded a `Color.White`, lo
 *   que en dark mode habría producido un velo blanco brillante sobre fondo oscuro).
 *
 * Fix-round (verify-report.md WARNING #3 / #4 — ver `contrastRatio()` en
 * `ContrastUtils.kt` y la regresión ejecutable en `DesignTokensTest`):
 * - `accentSuccess` ahora es theme-aware (antes era un literal único
 *   `Color(0xFF006B63)` reusado sin adaptar en dark, dando solo 2.30:1 contra
 *   `brand_surface` — bajo el mínimo no-textual 3:1 de WCAG 1.4.11). Light mantiene el
 *   literal original (`brand_success_light`, 6.40:1, ya cumplía); dark usa
 *   `brand_success_dark` (4.32:1).
 * - `textTertiary` en dark ya NO deriva de `scheme.outline` (`brand_outline`, 2.37:1 —
 *   bajo el mínimo 4.5:1 de WCAG AA para texto). Usa `brand_text_tertiary_dark`
 *   (4.61:1). Light sigue derivando de `scheme.outline` sin cambios (no tocado en este
 *   fix-round — ver Nota abajo). `brand_outline` en sí NO se modificó: sigue
 *   alimentando `iconNormal`/`onSurfaceVariant`/bordes M3 nativos sin cambios, para no
 *   ampliar el radio de impacto de este fix más allá de lo pedido.
 *
 * **Nota (descubrimiento, no corregido acá)**: recalculando con `contrastRatio()`,
 * `textTertiary` en LIGHT (`scheme.outline` = `0xFF75788C`) da 4.36:1 contra
 * `brand_light_surface` — también por debajo de 4.5:1, aunque de forma mucho más leve
 * que el gap de dark (2.37:1). Este fix-round solo tenía scope para el valor dark
 * (ver prompt del fix-round); el gap leve en light queda flagged como seguimiento, no
 * corregido silenciosamente.
 *
 * @param scheme El `ColorScheme` branded resuelto (light o dark) del cual derivar roles
 * @param darkTheme true si `scheme` es el branded DARK scheme — necesario porque
 *   `accentSuccess`/`textTertiary` (dark) ya no se derivan puramente de `scheme`, sino
 *   de literales dedicados definidos para cumplir WCAG (ver arriba). Deviation de
 *   design.md's snippet abreviado de `buildDesignTokens(scheme)` de un solo parámetro
 *   — necesaria para la corrección real de contraste, documentada acá y en
 *   apply-progress.md.
 * @return Un [DesignTokens] completo, coherente con el tema activo
 *
 * @author gentle-ai (TDD GREEN, PR-2; contrast fix-round)
 * @date 2026-07-13
 */
internal fun buildDesignTokens(scheme: ColorScheme, darkTheme: Boolean): DesignTokens {
    val textPrimary = scheme.onBackground
    val textSecondary = scheme.secondary
    val textTertiary = if (darkTheme) brand_text_tertiary_dark else scheme.outline

    return DesignTokens(
        // ============ COLORES ============
        backgroundPrimary = scheme.background,
        backgroundGradientStart = lerp(scheme.background, scheme.primaryContainer, 0.06f),
        backgroundGradientEnd = lerp(scheme.background, scheme.tertiaryContainer, 0.06f),
        surfacePrimary = scheme.surface,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textTertiary = textTertiary,
        accentPrimary = brand_primary,
        accentPrimaryLight = Color(0xFFE6E7FF),
        accentPrimaryDark = Color(0xFF5B5EC8),
        accentSecondary = brand_tertiary,
        accentSecondaryLight = Color(0xFFB3F5EA),
        accentSuccess = if (darkTheme) brand_success_dark else brand_success_light,
        accentSuccessLight = Color(0xFFA3F2E6),
        separator = scheme.surfaceVariant,
        iconBackground = Color(0xFFF0F1FF),
        iconNormal = scheme.outline,
        destructiveAction = Color(0xFFFF3B30),
        backdropScrim = scheme.background.copy(alpha = 0.4f),

        // ============ TIPOGRAFÍA ============
        largeTitleSize = 34.sp,
        largeTitleWeight = FontWeight.Bold,
        largeTitleColor = textPrimary,
        sectionTitleSize = 22.sp,
        sectionTitleWeight = FontWeight.Bold,
        sectionTitleColor = textPrimary,
        cardTitleSize = 17.sp,
        cardTitleWeight = FontWeight.SemiBold,
        cardTitleColor = textPrimary,
        cardSubtitleSize = 15.sp,
        cardSubtitleWeight = FontWeight.Normal,
        cardSubtitleColor = textSecondary,
        labelSize = 13.sp,
        labelWeight = FontWeight.Medium,
        labelColor = textSecondary,
        captionSize = 12.sp,
        captionWeight = FontWeight.Normal,
        captionColor = textTertiary,

        // ============ ESPACIADO ============
        cardPadding = 16.dp,
        cardSpacing = 12.dp,
        screenPaddingHorizontal = 16.dp,
        sectionSpacing = 24.dp,
        innerSpacing = 12.dp,

        // ============ TAMAÑOS DE ÍCONOS ============
        iconLarge = 48.dp,
        iconMedium = 40.dp,
        iconSmall = 24.dp,

        // ============ BORDES Y SOMBRAS ============
        cardCornerRadius = 24.dp,
        cardElevation = 12.dp,
        cardShadowColor = brand_primary.copy(alpha = 0.15f),
        iconCornerRadius = 16.dp
    )
}
