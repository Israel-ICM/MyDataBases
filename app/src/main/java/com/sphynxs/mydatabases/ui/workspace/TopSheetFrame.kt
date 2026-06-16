package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import kotlin.math.roundToInt

/**
 * TopSheet Frame - Capa CON CONTENIDO que tiene forma de escalón y baja más rápido que el TopSheet base.
 * 
 * Se posiciona ENCIMA del TopSheet base vacío y contiene el contenido real (tabla, query, etc.),
 * creando un efecto de reveal progresivo donde ambas capas terminan alineadas al expandirse.
 * 
 * @param expansionProgress Progreso de expansión del TopSheet base (0.0 = minimizado, 1.0 = expandido)
 * @param isDragging Si el usuario está arrastrando activamente
 * @param card WorkspaceCard con los datos a mostrar
 * @param isExpanded Si el frame está expandido (muestra contenido completo)
 * @param onClose Callback cuando se cierra la card
 * @param speedMultiplier Multiplicador de velocidad respecto al base (ej: 1.5 = baja 50% más rápido)
 * @param peekHeight Altura peek del TopSheet base (para sincronizar posiciones)
 */
@Composable
fun TopSheetFrame(
    expansionProgress: Float,
    isDragging: Boolean,
    card: WorkspaceCard,
    isExpanded: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1.8f,
    peekHeight: Dp = 60.dp
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val peekHeightPx = with(density) { peekHeight.toPx() }
    
    // Altura de la barra de estado
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.getTop(density).toFloat() }
    
    // Altura del panel (92% igual que el base)
    val screenHeightDp = configuration.screenHeightDp.dp
    val sheetHeight = screenHeightDp * 0.92f
    val sheetHeightPx = with(density) { sheetHeight.toPx() }
    
    // Offset colapsado del base (referencia)
    val baseCollapsedOffsetPx = -sheetHeightPx + peekHeightPx + statusBarHeightPx
    
    // Offset expandido (mismo que el base: 0)
    val frameExpandedOffsetPx = 0f
    
    // Distancia total que el BASE recorre
    val baseTravelDistance = frameExpandedOffsetPx - baseCollapsedOffsetPx // positivo
    
    // El frame recorre MÁS distancia (speedMultiplier veces) en el MISMO tiempo
    val frameTravelDistance = baseTravelDistance * speedMultiplier
    
    // Offset colapsado del frame: empieza MÁS arriba (más oculto)
    val frameCollapsedOffsetPx = frameExpandedOffsetPx - frameTravelDistance
    
    // El frame usa el MISMO expansionProgress que el base (0.0 -> 1.0)
    // pero recorre más distancia, por eso se ve más rápido
    val targetOffset = frameCollapsedOffsetPx + frameTravelDistance * expansionProgress
    
    // Raw offset que se actualiza directamente durante drag (sin animación)
    var rawOffset by remember { mutableFloatStateOf(targetOffset) }
    
    // Sincronizar rawOffset cuando expansionProgress cambia
    LaunchedEffect(expansionProgress, isDragging) {
        if (isDragging) {
            // Durante drag: actualizar inmediatamente (sigue al base sin delay)
            rawOffset = targetOffset
        } else {
            // Al soltar: animar suavemente
            rawOffset = targetOffset
        }
    }
    
    // Animación (0ms durante drag, 300ms al soltar)
    val animatedOffset by animateFloatAsState(
        targetValue = rawOffset,
        animationSpec = if (isDragging) {
            TweenSpec(durationMillis = 0)
        } else {
            TweenSpec(durationMillis = 300)
        },
        label = "topSheetFrameOffset"
    )
    
    // Border radius progresivo: más redondeado cuando minimizado
    val cornerRadius by animateFloatAsState(
        targetValue = if (expansionProgress < 0.5f) 32f else 24f,
        animationSpec = TweenSpec(durationMillis = 200),
        label = "frameCornerRadius"
    )
    
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
            shape = StepNotchShape(
                stepWidthFraction = 0.4f,
                stepHeightDp = 60f,
                cornerRadiusDp = cornerRadius
            ),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            // Contenido real de la workspace card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                WorkspaceCardContent(
                    card = card,
                    isExpanded = isExpanded,
                    onClose = onClose
                )
            }
        }
    }
}
