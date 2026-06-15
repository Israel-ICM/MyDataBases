package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Card draggable estilo PlayStation con animaciones suaves.
 * 
 * COMPORTAMIENTO CORRECTO:
 * - Card vive ARRIBA en estado minimizado (peek como pestaña)
 * - Drag DOWN → card BAJA y se EXPANDE
 * - Drag UP → card SUBE y se MINIMIZA
 * 
 * Features:
 * - Drag vertical para expandir/minimizar
 * - Animaciones spring suaves
 * - Shadow pronunciada estilo PlayStation
 * - Estados: Minimizada (arriba), Expandida (abajo)
 * 
 * @param isExpanded Si la card está expandida (true) o minimizada (false)
 * @param onDragStateChange Callback cuando el estado de drag cambia
 * @param modifier Modificador opcional
 * @param content Contenido de la card
 * 
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun DraggableCard(
    isExpanded: Boolean,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // Estado de drag temporal
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Offset: cuando minimizada el panel está ARRIBA oculto (negativo)
    // cuando expandida baja y se hace visible (0 o positivo pequeño)
    val peekHeight = 60.dp // Altura del peek visible
    val panelHeight = 600.dp // Altura total del panel
    
    val targetOffsetY = if (isExpanded) {
        // Expandida: panel baja y queda visible
        0.dp
    } else {
        // Minimizada: panel oculto arriba, solo peek visible
        -(panelHeight - peekHeight)
    }
    
    // Animación suave con spring
    val animatedOffsetY by animateDpAsState(
        targetValue = if (isDragging) dragOffsetY.dp else targetOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardOffsetY"
    )
    
    // Shape: esquinas redondeadas ABAJO cuando está arriba (minimizada)
    val cardShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, animatedOffsetY.roundToPx()) }
            .shadow(
                elevation = 24.dp,
                shape = cardShape,
                ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        // Threshold: si arrastró más de 100dp cambiar estado
                        val threshold = 100.dp.toPx()
                        val currentOffset = if (isDragging) dragOffsetY else targetOffsetY.toPx()
                        
                        // Minimizada (oculta arriba, offset negativo) + drag DOWN (positivo) → EXPANDIR
                        if (!isExpanded && dragOffsetY > threshold) {
                            onDragStateChange(true)
                        } 
                        // Expandida (visible, offset ~0) + drag UP (negativo) → MINIMIZAR
                        else if (isExpanded && dragOffsetY < -threshold) {
                            onDragStateChange(false)
                        }
                        
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffsetY = (dragOffsetY + dragAmount).coerceIn(
                            -(panelHeight - peekHeight).toPx(), // Límite superior: oculta excepto peek
                            100.dp.toPx() // Límite inferior: puede bajar un poco
                        )
                    }
                )
            },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // Shadow manejada manualmente arriba
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Handle visual para indicar que es draggable (línea gris horizontal)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .align(androidx.compose.ui.Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Contenido de la card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                content()
            }
        }
    }
}
