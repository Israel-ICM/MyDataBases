package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R

/**
 * Carrusel de todas las cards abiertas en el workspace (capability `workspace-carousel`).
 *
 * Overlay a pantalla completa: scrim de backdrop + [HorizontalMultiBrowseCarousel] (M3,
 * `@ExperimentalMaterial3Api`, verificado disponible en `compose-bom:2025.05.01` — ver design.md)
 * centrado sobre el scrim. Renderiza TODAS las cards de [cards] (incluida la activa, DECISION D5
 * de spec.md), permite activar una card (tap) o cerrarla (per-item close), y se puede descartar
 * sin mutar el índice activo mediante backdrop-tap, back-gesture o botón BACK del sistema
 * (DECISION D3 — el `BackHandler` vive en [WorkspaceOverlay], no acá).
 *
 * @param cards Lista COMPLETA de cards abiertas (no solo las backgrounded)
 * @param activeIndex Índice de la card actualmente activa; su item se distingue visualmente
 *                     (borde 2.dp `primary` + tonalElevation elevada, DECISION D1) y vía semántica
 *                     `selected` (testeable con `assertIsSelected`)
 * @param onSelectCard Invocado con el índice tocado; [WorkspaceOverlay] decide si llama a
 *                      `setActiveIndex` (DECISION D4: tocar la card ya activa es dismiss puro)
 * @param onCloseCard Invocado con el índice a cerrar; delega 100% en `WorkspaceManager.closeCard`
 *                     (DECISION D2 — sin lógica de fallback nueva acá)
 * @param onDismiss Invocado en backdrop-tap; el caller decide `isCarouselOpen = false`
 * @param modifier Modificador opcional (normalmente `Modifier.fillMaxSize()`)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceCarousel(
    cards: List<WorkspaceCard>,
    activeIndex: Int,
    onSelectCard: (Int) -> Unit,
    onCloseCard: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Backdrop scrim — tap descarta SIN mutar activeIndex (DECISION D3).
            // Mismo patrón que TopSheet.kt: Canvas + detectTapGestures, alpha fijo 0.4f.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            ) {
                drawRect(Color.White.copy(alpha = 0.4f))
            }

            val carouselState = rememberCarouselState(initialItem = activeIndex) { cards.size }

            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = 220.dp,
                itemSpacing = 8.dp,
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(180.dp)
            ) { i ->
                val itemCard = cards[i]
                WorkspaceCarouselItem(
                    card = itemCard,
                    isActive = i == activeIndex,
                    onSelect = { onSelectCard(i) },
                    onClose = { onCloseCard(i) },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Item individual del carrusel: título + ícono de tipo + affordance de cierre (DECISION D5).
 *
 * La card activa se distingue visualmente (borde 2.dp `primary` + tonalElevation elevada,
 * DECISION D1) Y semánticamente vía `Modifier.selectable(selected = isActive, ...)` — esta
 * última es una adición sobre lo que design.md especifica textualmente: expone el estado
 * "activa" como una semantics property real (`Selected`), testeable con `assertIsSelected()` /
 * `assertIsNotSelected()` sin acoplarse a detalles visuales (border color, elevation), y mejora
 * accesibilidad (lectores de pantalla anuncian el estado de selección).
 */
@Composable
private fun WorkspaceCarouselItem(
    card: WorkspaceCard,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val icon = when (card) {
        is WorkspaceCard.Table -> Icons.Default.TableChart
        is WorkspaceCard.Query -> Icons.Default.Description
        // Future: Editor -> Icons.Default.Edit
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .then(
                if (isActive) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .selectable(selected = isActive, onClick = onSelect, role = Role.Tab),
        shape = shape,
        tonalElevation = if (isActive) 8.dp else 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(
                            R.string.workspace_carousel_close_card,
                            card.title
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
