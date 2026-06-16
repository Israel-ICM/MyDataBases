package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Top Sheet - Modal sheet que baja desde arriba (bottom sheet invertido).
 * 
 * Comportamiento idéntico a ModalBottomSheet pero invertido:
 * - Peek minimizado arriba mostrando handle
 * - Drag down para expandir
 * - Drag up para minimizar
 * - Backdrop con tap para cerrar
 * 
 * @param isExpanded Si el sheet está expandido
 * @param onExpandedChange Callback cuando cambia el estado
 * @param modifier Modificador
 * @param peekHeight Altura del peek visible cuando está minimizado
 * @param sheetContent Contenido del sheet
 */
@Composable
fun TopSheet(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    peekHeight: Dp = 60.dp,
    sheetContent: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val peekHeightPx = with(density) { peekHeight.toPx() }
    
    // Altura del panel: 92% de la pantalla
    val screenHeightDp = configuration.screenHeightDp.dp
    val sheetHeight = screenHeightDp * 0.92f
    val sheetHeightPx = with(density) { sheetHeight.toPx() }
    
    // Target offset según estado
    val targetOffset = if (isExpanded) 0f else -sheetHeightPx + peekHeightPx
    
    // Offset actual que sigue el dedo o anima
    var rawOffset by remember { mutableFloatStateOf(targetOffset) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Sincronizar rawOffset cuando isExpanded cambia externamente (ej: click en backdrop)
    LaunchedEffect(isExpanded) {
        if (!isDragging) {
            rawOffset = targetOffset
        }
    }
    
    // Solo animar cuando NO está arrastrando
    val animatedOffset by animateFloatAsState(
        targetValue = rawOffset,
        animationSpec = if (isDragging) {
            TweenSpec(durationMillis = 0) // Sin animación durante drag
        } else {
            TweenSpec(durationMillis = 300) // Animación suave al soltar
        },
        label = "topSheetOffset"
    )
    
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    
    val displayOffset = animatedOffset
    
    // Calcular el progreso de expansión (0 = minimizado, 1 = expandido)
    // Minimizado: offset = -sheetHeightPx + peekHeightPx (negativo grande)
    // Expandido: offset = 0
    val expansionProgress = ((displayOffset - (-sheetHeightPx + peekHeightPx)) / (sheetHeightPx - peekHeightPx)).coerceIn(0f, 1f)
    
    // Alpha del backdrop proporcional al progreso (0 cuando minimizado, 0.5 cuando expandido)
    val backdropAlpha = expansionProgress * 0.5f
    
    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop - siempre visible pero con alpha variable
        if (backdropAlpha > 0.01f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onExpandedChange(false)
                        }
                    }
            ) {
                drawRect(Color.Black.copy(alpha = backdropAlpha))
            }
        }
        
        // Sheet
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .offset { IntOffset(0, displayOffset.roundToInt()) }
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .draggable(
                        state = rememberDraggableState { delta ->
                            accumulatedDrag += delta
                            // Actualizar rawOffset directamente - sigue el dedo sin animación
                            rawOffset = (rawOffset + delta).coerceIn(-sheetHeightPx + peekHeightPx, 100f)
                        },
                        orientation = Orientation.Vertical,
                        onDragStarted = {
                            isDragging = true
                            accumulatedDrag = 0f
                        },
                        onDragStopped = { velocity ->
                            // Decidir nuevo estado según threshold
                            val threshold = 100f
                            val shouldExpand = !isExpanded && accumulatedDrag > threshold
                            val shouldCollapse = isExpanded && accumulatedDrag < -threshold
                            
                            // Marcar que ya NO está arrastrando ANTES de cambiar target
                            // Esto activa la animación de 300ms
                            isDragging = false
                            
                            if (shouldExpand) {
                                onExpandedChange(true)
                                rawOffset = 0f // Animar a expandido
                            } else if (shouldCollapse) {
                                onExpandedChange(false)
                                rawOffset = -sheetHeightPx + peekHeightPx // Animar a colapsado
                            } else {
                                // No cambió estado - volver a la posición original
                                rawOffset = targetOffset
                            }
                            
                            accumulatedDrag = 0f
                        }
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Contenido
                    Box(modifier = Modifier.weight(1f)) {
                        Column {
                            sheetContent()
                        }
                    }
                    
                    // Handle - línea horizontal en la parte INFERIOR para arrastrar
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.5.dp)
                            )
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
