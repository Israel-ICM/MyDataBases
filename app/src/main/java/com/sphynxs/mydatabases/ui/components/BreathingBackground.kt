package com.sphynxs.mydatabases.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Fondo con gradiente animado que "respira" suavemente.
 *
 * Anima el gradiente entre violeta y turquesa de forma continua,
 * creando un efecto visual orgánico y moderno.
 *
 * @param modifier Modificador opcional
 * @param content Contenido que se renderiza sobre el fondo
 *
 * @author israel-icm
 * @date 2026-06-17
 */
@Composable
fun BreathingBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Animación infinita que oscila entre 0f y 1f con easing suave
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,  // 8 segundos por ciclo completo
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse  // Ida y vuelta suave
        ),
        label = "breathing_progress"
    )
    
    // Interpolamos entre dos gradientes diferentes
    val startColor1 = LocalDesignTokens.current.backgroundGradientStart
    val endColor1 = LocalDesignTokens.current.backgroundGradientEnd
    
    val startColor2 = LocalDesignTokens.current.backgroundGradientEnd
    val endColor2 = LocalDesignTokens.current.backgroundGradientStart
    
    // Mezclamos los colores según el progreso de la animación
    val currentStartColor = lerpColor(startColor1, startColor2, animatedProgress)
    val currentEndColor = lerpColor(endColor1, endColor2, animatedProgress)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(currentStartColor, currentEndColor)
                )
            )
    ) {
        content()
    }
}

/**
 * Interpola linealmente entre dos colores.
 *
 * @param start Color inicial
 * @param end Color final
 * @param fraction Fracción entre 0f y 1f
 * @return Color interpolado
 */
private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    return Color(
        red = androidx.compose.ui.util.lerp(start.red, end.red, fraction),
        green = androidx.compose.ui.util.lerp(start.green, end.green, fraction),
        blue = androidx.compose.ui.util.lerp(start.blue, end.blue, fraction),
        alpha = androidx.compose.ui.util.lerp(start.alpha, end.alpha, fraction)
    )
}
