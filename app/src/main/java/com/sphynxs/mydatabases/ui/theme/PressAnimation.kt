package com.sphynxs.mydatabases.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppMotion
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalReducedMotion

/**
 * Modifier helper para animación de presión tipo PlayStation.
 *
 * Cuando el usuario presiona un componente interactivo:
 * - Scale: reduce a 97% (0.97f) creando sensación de "push"
 * - Shadow: eleva a 8dp simulando el elemento siendo presionado hacia adelante
 *
 * La animación usa el easing `emphasized` de Material Design para
 * transiciones dramáticas y satisfactorias.
 *
 * Respeta `LocalReducedMotion` — si el usuario tiene animaciones desactivadas,
 * la transición es instantánea (0ms).
 *
 * ## Uso
 *
 * ```kotlin
 * val interactionSource = remember { MutableInteractionSource() }
 * Card(
 *     onClick = { ... },
 *     interactionSource = interactionSource,
 *     modifier = Modifier.pressAnimation(interactionSource)
 * )
 * ```
 *
 * @param interactionSource Fuente de eventos de interacción del componente
 * @param scale Factor de escala al presionar (default: 0.97f = 97%)
 * @param elevation Elevación de sombra al presionar (default: 8.dp)
 * @return Modifier con animación de presión aplicada
 *
 * @author israel-icm
 * @date 2026-06-15
 */
fun Modifier.pressAnimation(
    interactionSource: InteractionSource,
    scale: Float = 0.97f,
    elevation: Dp = 8.dp
): Modifier = composed {
    val motion = LocalAppMotion.current
    val shapes = LocalAppShapes.current
    val reduced = LocalReducedMotion.current

    val isPressed by interactionSource.collectIsPressedAsState()

    val duration = motion.durationOrInstant(motion.fast, reduced)

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = tween(
            durationMillis = duration,
            easing = motion.emphasized
        ),
        label = "pressAnimationScale"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .shadow(
            elevation = if (isPressed) elevation else 0.dp,
            shape = shapes.large
        )
}
