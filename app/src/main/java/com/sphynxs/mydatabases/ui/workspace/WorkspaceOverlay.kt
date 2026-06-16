package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Overlay del workspace estilo PlayStation.
 * 
 * Muestra las cards draggables apiladas en la parte superior
 * sobre el contenido principal (lista de tablas).
 * 
 * Fase 1: Card única expandible/minimizable con drag vertical
 * 
 * @param modifier Modificador opcional
 * @param workspaceManager Manager del workspace
 * @param backgroundContent Contenido de fondo (lista de tablas)
 * 
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun WorkspaceOverlay(
    workspaceManager: WorkspaceManager,
    modifier: Modifier = Modifier,
    backgroundContent: @Composable () -> Unit
) {
    val workspaceState by workspaceManager.state.collectAsState()
    val activeCards by workspaceManager.cards.collectAsState()
    val selectedCardIndex by workspaceManager.activeIndex.collectAsState()
    
    // Card empieza EXPANDIDA cuando hay cards activas
    var isCardExpanded by remember { mutableStateOf(false) }
    
    // Estado compartido para sincronizar el frame decorativo
    var expansionProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Auto-expandir cuando aparece una nueva card
    LaunchedEffect(activeCards.size) {
        if (activeCards.isNotEmpty()) {
            isCardExpanded = true
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Contenido de fondo (lista de tablas)
        backgroundContent()
        
        // TopSheet si hay cards activas (dual-layer: base + frame decorativo)
        if (workspaceState != WorkspaceState.Collapsed && activeCards.isNotEmpty()) {
            val selectedCard = activeCards.getOrNull(selectedCardIndex)
            
            selectedCard?.let { card ->
                // Capa 1: TopSheet base VACÍO (rectangular, fondo/sombra)
                TopSheet(
                    isExpanded = isCardExpanded,
                    onExpandedChange = { expanded ->
                        isCardExpanded = expanded
                        if (expanded) {
                            workspaceManager.expand()
                        } else {
                            workspaceManager.peek()
                        }
                    },
                    onProgressChange = { progress, dragging ->
                        expansionProgress = progress
                        isDragging = dragging
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Vacío - solo backdrop/fondo
                }
                
                // Capa 2: TopSheetFrame CON CONTENIDO (escalón, baja más rápido)
                TopSheetFrame(
                    expansionProgress = expansionProgress,
                    isDragging = isDragging,
                    modifier = Modifier.fillMaxSize(),
                    card = card,
                    isExpanded = isCardExpanded,
                    onClose = { workspaceManager.closeCard(selectedCardIndex) }
                )
            }
        }
    }
}

/**
 * Contenido de una workspace card.
 * 
 * Muestra solo título cuando está minimizada, contenido completo cuando expandida.
 */
@Composable
internal fun WorkspaceCardContent(
    card: WorkspaceCard,
    isExpanded: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isExpanded) {
        // Estado minimizado: solo handle (ya está en DraggableCard)
        // No mostramos nada más para que quede delgada como en el GIF
        Box(modifier = modifier.fillMaxSize())
    } else {
        // Estado expandido: mostrar contenido completo
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header de la card con botón cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Botón cerrar
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Badge del tipo de card
            Text(
                text = "Tipo: ${card::class.simpleName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Contenido según tipo de card
            when (card) {
                is WorkspaceCard.Table -> {
                    TableCardContent(
                        databaseName = card.databaseName,
                        tableName = card.tableName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
