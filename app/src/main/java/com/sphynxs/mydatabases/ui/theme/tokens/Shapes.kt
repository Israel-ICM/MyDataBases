package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Design tokens de shapes (formas) con corner radius consistente.
 *
 * Define los radios de esquinas para componentes de la aplicación:
 * - none: 0dp (rectangular puro)
 * - small: 8dp (chips, botones pequeños)
 * - medium: 12dp (cards, inputs estándar)
 * - large: 20dp (cards destacadas, dialogs)
 * - extraLarge: 28dp (bottom sheets, grandes containers)
 *
 * @author israel-icm (TDD GREEN)
 * @date 2026-06-15
 */
@Immutable
data class AppShapes(
    val none: CornerBasedShape = RoundedCornerShape(0.dp),
    val small: CornerBasedShape = RoundedCornerShape(8.dp),
    val medium: CornerBasedShape = RoundedCornerShape(12.dp),
    val large: CornerBasedShape = RoundedCornerShape(20.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(28.dp)
)

/**
 * CompositionLocal para exponer AppShapes a toda la jerarquía de Composables.
 *
 * Uso:
 * ```kotlin
 * val shapes = LocalAppShapes.current
 * Card(shape = shapes.medium) { ... }
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
val LocalAppShapes = staticCompositionLocalOf { AppShapes() }
