package com.sphynxs.mydatabases.ui.theme.tokens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Design tokens de motion (duraciones y easings).
 *
 * Define duraciones estándar para animaciones:
 * - instant: 0ms (sin animación)
 * - fast: 150ms (transiciones rápidas, micro-interacciones)
 * - medium: 300ms (transiciones estándar)
 * - slow: 500ms (transiciones complejas, entrada/salida de pantallas)
 *
 * Define easings de Material Design:
 * - standard: FastOutSlowInEasing (default de Material)
 * - decelerate: LinearOutSlowInEasing (objetos que entran en pantalla)
 * - accelerate: FastOutLinearInEasing (objetos que salen de pantalla)
 * - emphasized: CubicBezierEasing(0.2, 0.0, 0.0, 1.0) (transiciones dramáticas)
 *
 * @author israel-icm (TDD GREEN)
 * @date 2026-06-15
 */
@Immutable
data class AppMotion(
    val instant: Int = 0,
    val fast: Int = 150,
    val medium: Int = 300,
    val slow: Int = 500,
    val standard: Easing = FastOutSlowInEasing,
    val decelerate: Easing = LinearOutSlowInEasing,
    val accelerate: Easing = FastOutLinearInEasing,
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
) {
    /**
     * Devuelve la duración apropiada según si reduced motion está activo.
     *
     * @param base Duración normal en millisegundos
     * @param reduced Si el usuario tiene reduced motion activado
     * @return 0 si reduced=true, base si reduced=false
     */
    fun durationOrInstant(base: Int, reduced: Boolean): Int =
        if (reduced) instant else base
}

/**
 * CompositionLocal para exponer AppMotion a toda la jerarquía de Composables.
 *
 * Uso:
 * ```kotlin
 * val motion = LocalAppMotion.current
 * val reduced = LocalReducedMotion.current
 * val duration = motion.durationOrInstant(motion.medium, reduced)
 * animateFloatAsState(targetValue, animationSpec = tween(duration, easing = motion.standard))
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
val LocalAppMotion = staticCompositionLocalOf { AppMotion() }
