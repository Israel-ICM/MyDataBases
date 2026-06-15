package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens de elevation (elevación/sombra) para componentes.
 *
 * Define niveles sutiles de elevación para crear jerarquía visual:
 * - none: 0dp (plano)
 * - cardResting: 1dp (cards en reposo, elevación mínima)
 * - cardHover: 3dp (cards en hover, feedback de interacción)
 * - cardPressed: 6dp (cards presionadas, feedback táctil)
 * - modal: 8dp (dialogs, bottom sheets, elementos flotantes)
 *
 * @author israel-icm (TDD GREEN)
 * @date 2026-06-15
 */
@Immutable
data class AppElevation(
    val none: Dp = 0.dp,
    val cardResting: Dp = 1.dp,
    val cardHover: Dp = 3.dp,
    val cardPressed: Dp = 6.dp,
    val modal: Dp = 8.dp
)

/**
 * CompositionLocal para exponer AppElevation a toda la jerarquía de Composables.
 *
 * Uso:
 * ```kotlin
 * val elevation = LocalAppElevation.current
 * Card(modifier = Modifier.shadow(elevation.cardResting)) { ... }
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
val LocalAppElevation = staticCompositionLocalOf { AppElevation() }
