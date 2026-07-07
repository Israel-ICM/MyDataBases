package com.sphynxs.mydatabases.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sphynxs.mydatabases.R
import kotlin.math.abs

/**
 * Carrusel de todas las cards abiertas en el workspace (capability `workspace-carousel`).
 *
 * Overlay a pantalla completa: scrim de backdrop + [LazyRow] con snap propio (NO usa el
 * `HorizontalMultiBrowseCarousel` experimental de Material3 — su masking/clip interno
 * suprimía tanto la sombra como la rotación 3D del efecto coverflow de abajo; con LazyRow
 * hay control total sobre posición, z-order y transformaciones, sin cajas negras de por
 * medio). Renderiza TODAS las cards de [cards] (incluida la activa, DECISION D5 de spec.md),
 * permite activar una card (tap) o cerrarla (per-item close), y se puede descartar sin mutar
 * el índice activo mediante backdrop-tap, back-gesture o botón BACK del sistema
 * (DECISION D3 — el `BackHandler` vive en [WorkspaceOverlay], no acá).
 *
 * Efecto "coverflow" (3D) — mazo apilado con profundidad real:
 * - **Superposición real de layout** vía `Arrangement.spacedBy(-overlapDp)` (espaciado
 *   NEGATIVO) — las cards se solapan de verdad en el layout, no es una ilusión de pintura.
 * - **`fraction`** (-1.6..1.6, 0 = centro): se lee directamente de
 *   `LazyListState.layoutInfo.visibleItemsInfo` (offset + tamaño real de cada item en
 *   pantalla, medido por el propio LazyRow) comparado contra el centro del viewport —
 *   mucho más preciso y confiable que medir coordenadas de ventana a mano.
 * - **`zIndex`** (Modifier): la card más cercana al centro se dibuja ARRIBA de sus vecinas
 *   (si no, el orden natural del Row haría que la de la derecha siempre tape a la de la
 *   izquierda, no la del centro a ambos lados).
 * - **`rotationY`/`scaleX,Y`/`alpha`** (dentro de [graphicsLayer]): inclinación tipo
 *   coverflow + reducción de tamaño/opacidad en las cards alejadas del centro.
 * - **`shadowElevation`** de [WorkspaceCarouselItem] (real, vía `Surface`, no solo
 *   `tonalElevation`): bien marcada en la card del centro para que se lea como "flotando"
 *   sobre el mazo.
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
@OptIn(ExperimentalFoundationApi::class) // rememberSnapFlingBehavior puede requerirlo según versión del BOM
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

            val itemWidth = 220.dp
            val itemHeight = 180.dp
            val overlap = 56.dp // cuánto "entra" una card debajo/encima de su vecina

            // initialFirstVisibleItemScrollOffset = 0 centra activeIndex de entrada, porque
            // contentPadding (más abajo) ya deja exactamente (viewportWidth - itemWidth) / 2
            // de margen a cada lado: el primer item visible con offset 0 cae ya centrado.
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeIndex)
            val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

            val localDensity = LocalDensity.current
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
            val sidePadding = maxOf((screenWidthDp - itemWidth) / 2, 0.dp)

            // Unidad para normalizar "distancia al centro" en fracciones de "una card". Con
            // pocas cards abiertas normalizar por el viewport completo daba fracciones chicas
            // y el efecto no se notaba (bug ya encontrado una vez) — acá se evita del todo
            // porque `fraction` sale directo de layoutInfo, no de coordenadas de ventana.
            val depthRangePx = with(localDensity) { (itemWidth - overlap).toPx() }.coerceAtLeast(1f)

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                horizontalArrangement = Arrangement.spacedBy(-overlap),
                contentPadding = PaddingValues(horizontal = sidePadding),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight)
            ) {
                itemsIndexed(cards, key = { _, card -> card.id }) { i, itemCard ->
                    // fraction: -1 (una card a la izq) .. 0 (centro) .. +1 (der); puede pasar
                    // de ±1 con 3+ cards de distancia (queda aún más chico/tenue). Se lee de
                    // layoutInfo en el CUERPO del composable (no solo dentro de graphicsLayer)
                    // porque zIndex necesita el Float en el momento de aplicar el modifier.
                    val layoutInfo = listState.layoutInfo
                    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == i }
                    val fraction = if (itemInfo == null) {
                        0f
                    } else {
                        val viewportCenter =
                            (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                        val itemCenter = itemInfo.offset + itemInfo.size / 2f
                        ((itemCenter - viewportCenter) / depthRangePx).coerceIn(-1.6f, 1.6f)
                    }

                    WorkspaceCarouselItem(
                        card = itemCard,
                        isActive = i == activeIndex,
                        depthFraction = fraction,
                        onSelect = { onSelectCard(i) },
                        onClose = { onCloseCard(i) },
                        modifier = Modifier
                            .width(itemWidth)
                            .fillParentMaxHeight()
                            // La card más cerca del centro se dibuja ARRIBA de sus vecinas —
                            // si no, el orden natural del Row haría que la de la derecha
                            // siempre tape a la de la izquierda, no la del centro a ambas.
                            .zIndex(1f - abs(fraction))
                            .graphicsLayer {
                                // Rotación tipo "coverflow": los items a los costados se
                                // inclinan hacia el centro. cameraDistance alto evita
                                // distorsión exagerada (default de graphicsLayer es
                                // 8*density, acá lo multiplicamos x4).
                                rotationY = fraction * -26f
                                cameraDistance = 32f * density

                                // Profundidad: items alejados del centro se ven más chicos
                                // y tenues. Piso (coerceAtLeast) para que no desaparezcan
                                // del todo con 3+ cards de distancia (fraction hasta 1.6).
                                val depthFactor =
                                    (1f - 0.22f * abs(fraction)).coerceAtLeast(0.55f)
                                scaleX = depthFactor
                                scaleY = depthFactor
                                alpha = (1f - 0.4f * abs(fraction)).coerceAtLeast(0.4f)

                                // Pivote en el borde más cercano al centro para que la
                                // rotación "gire hacia adentro" en vez de rotar sobre su
                                // propio eje central.
                                transformOrigin = TransformOrigin(
                                    pivotFractionX = if (fraction > 0f) 0f else 1f,
                                    pivotFractionY = 0.5f
                                )
                            }
                    )
                }
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
 *
 * `depthFraction` (-1..1, 0 = centro) controla `shadowElevation`: la card más cercana al
 * centro tira una sombra real más marcada (parece "flotar" sobre las de atrás), las de los
 * costados casi no proyectan sombra — refuerza la lectura de mazo apilado junto con el
 * solapado/zIndex que aplica el caller ([WorkspaceCarousel]).
 */
@Composable
private fun WorkspaceCarouselItem(
    card: WorkspaceCard,
    isActive: Boolean,
    depthFraction: Float,
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
    val proximityToCenter = 1f - abs(depthFraction).coerceIn(0f, 1f)

    Surface(
        // OJO: NO se aplica `.clip(shape)` acá manualmente — Surface ya clipea su propio
        // contenido a `shape` internamente DESPUÉS de dibujar shadowElevation/border. Un
        // `.clip()` externo aplicado ANTES (como estaba antes) recorta esa sombra apenas
        // sale de los límites de la forma, dejándola invisible. Mismo criterio para el
        // borde: se usa el parámetro nativo `border` de Surface en vez de `Modifier.border`
        // encadenado, para que se dibuje en el orden correcto respecto a la sombra.
        modifier = modifier
            .fillMaxSize()
            .selectable(selected = isActive, onClick = onSelect, role = Role.Tab),
        shape = shape,
        tonalElevation = if (isActive) 8.dp else 1.dp,
        // Sombra bien marcada: piso alto (12dp, no casi-cero) para que TODAS las cards
        // proyecten sombra visible, y hasta 40dp extra en la del centro para que se lea
        // claramente "flotando" arriba del mazo. Los valores previos (4dp base / +24dp)
        // quedaban casi imperceptibles contra el scrim del backdrop.
        shadowElevation = 12.dp + 40.dp * proximityToCenter,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
