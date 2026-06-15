package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens de spacing basados en una escala de 4dp.
 *
 * Proporciona valores consistentes de espaciado para toda la aplicación,
 * desde micros ajustes (xxs=2dp) hasta grandes separaciones (xxxl=48dp).
 *
 * Sistema de naming:
 * - none: 0dp (sin espacio)
 * - xxs: 2dp (micro ajuste)
 * - xs: 4dp (mínimo táctil)
 * - sm: 8dp (pequeño)
 * - md: 12dp (mediano)
 * - lg: 16dp (grande, estándar Material)
 * - xl: 24dp (extra grande)
 * - xxl: 32dp (separación de secciones)
 * - xxxl: 48dp (espaciado hero)
 *
 * @author israel-icm (TDD GREEN)
 * @date 2026-06-15
 */
@Immutable
data class AppSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp
)

/**
 * CompositionLocal para exponer AppSpacing a toda la jerarquía de Composables.
 *
 * Uso:
 * ```kotlin
 * val spacing = LocalAppSpacing.current
 * Modifier.padding(spacing.lg) // 16.dp
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }
