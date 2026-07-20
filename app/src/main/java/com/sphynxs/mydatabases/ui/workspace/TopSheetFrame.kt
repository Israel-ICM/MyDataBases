package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.alpha
import com.sphynxs.mydatabases.R
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
 * @param totalCardCount Cantidad total de cards abiertas en el workspace; controla la visibilidad
 *                        del botón de carrusel (visible solo si >= 2, ver workspace-card-carousel)
 * @param onShowCarousel Callback cuando se presiona el botón de carrusel (delega el toggle a WorkspaceOverlay)
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
    totalCardCount: Int,
    onShowCarousel: () -> Unit,
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1.8f,
    peekHeight: Dp = WorkspaceConstants.PEEK_HEIGHT
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val peekHeightPx = with(density) { peekHeight.toPx() }
    
    // Altura de la barra de estado
    val statusBarHeightPx = with(density) { WindowInsets.statusBars.getTop(density).toFloat() }

    // Buffer extra entre la barra de estado y el peek — debe coincidir con el
    // usado en TopSheet.kt (misma constante) para que ambas capas queden
    // sincronizadas en la posición colapsada.
    val topGestureBufferPx = with(density) { WorkspaceConstants.TOP_GESTURE_BUFFER.toPx() }
    
    // Altura del panel (92% menos espacio para toolbar, igual que el base)
    val screenHeightDp = configuration.screenHeightDp.dp
    val sheetHeight = (screenHeightDp * 0.92f) - WorkspaceConstants.TOOLBAR_SPACING
    val sheetHeightPx = with(density) { sheetHeight.toPx() }
    
    // Offset colapsado del base (referencia)
    val baseCollapsedOffsetPx = -sheetHeightPx + peekHeightPx + statusBarHeightPx + topGestureBufferPx
    
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
    
    // Shadow elevation progresiva: mayor cuando minimizado (arriba), menor cuando expandido (abajo)
    val shadowElevation = (24f - (22f * expansionProgress)).dp // 24dp -> 2dp
    val tonalElevation = (12f - (11f * expansionProgress)).dp  // 12dp -> 1dp
    
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
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 60.dp) // Espacio para el escalón
            ) {
                // Contenido real de la workspace card con fade in
                Box(modifier = Modifier.alpha(expansionProgress)) {
                    WorkspaceCardContent(
                        card = card,
                        isExpanded = isExpanded,
                        onClose = onClose
                    )
                }
            }
        }
        
        // Ícono en el escalón con fade in (posicionado sobre el Surface sin padding)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                .align(Alignment.TopCenter)
        ) {
            StepIcon(
                card = card,
                alpha = expansionProgress,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = configuration.screenWidthDp.dp * 0.18f, bottom = 16.dp)
            )

            // Botón de carrusel, espejo horizontal de StepIcon; visible solo con 2+ cards abiertas.
            // IconButton reserva un touch target de 48dp alrededor del glifo de 28dp (10dp de inset
            // por lado), a diferencia de StepIcon que es un Icon plano sin caja extra. Se resta ese
            // inset del padding para que el GLIFO quede a la misma altura/distancia que StepIcon,
            // no la caja invisible del botón.
            if (totalCardCount >= 2) {
                val iconButtonInset = 10.dp // (48.dp touch target - 28.dp icon) / 2
                CarouselTriggerIcon(
                    onShowCarousel = onShowCarousel,
                    alpha = expansionProgress,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = configuration.screenWidthDp.dp * 0.18f - iconButtonInset,
                            bottom = 16.dp - iconButtonInset
                        )
                )
            }
        }
    }
}

/**
 * Ícono en el escalón del TopSheet que indica el tipo de workspace card.
 */
@Composable
private fun StepIcon(
    card: WorkspaceCard,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val icon = when (card) {
        is WorkspaceCard.Table -> Icons.Default.TableChart
        is WorkspaceCard.Query -> Icons.Default.Description
        // Future: Editor -> Icons.Default.Edit
    }
    
    Icon(
        imageVector = icon,
        contentDescription = card.title,
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * alpha),
        modifier = modifier.size(28.dp)
    )
}

/**
 * Botón trigger del carrusel de cards, en el escalón del TopSheet.
 *
 * Espejo horizontal de [StepIcon]: mismo tratamiento de alpha (`0.6f * expansionProgress`)
 * y mismo inset inferior (16.dp), alineado a `BottomEnd` en lugar de `BottomStart`.
 * A diferencia de [StepIcon] (un [Icon] plano, no interactivo), este se envuelve en
 * [IconButton] para garantizar un touch target mínimo de 48dp pese a que el glifo
 * visible sigue siendo de 28dp (Requirement: Localized content-description strings /
 * Non-Functional Requirements — Accessibility, spec.md).
 */
@Composable
private fun CarouselTriggerIcon(
    onShowCarousel: () -> Unit,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onShowCarousel,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.ViewCarousel,
            contentDescription = stringResource(R.string.workspace_carousel_button),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * alpha),
            modifier = Modifier.size(28.dp)
        )
    }
}
