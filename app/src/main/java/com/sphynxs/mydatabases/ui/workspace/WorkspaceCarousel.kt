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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
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
 * - **Sombra dibujada a mano** (`BlurMaskFilter` vía `drawBehind` en [WorkspaceCarouselItem]):
 *   `Surface.shadowElevation` (elevación nativa de Android) llegó a un tope que no escalaba
 *   más sin importar cuánto margen se le diera — se reemplazó por una sombra propia con
 *   control total sobre blur/alpha/offset, más marcada en la card del centro para que se lea
 *   como "flotando" sobre el mazo.
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
        // Capturado ANTES del Canvas: drawRect corre en un DrawScope no-composable,
        // no puede leer LocalDesignTokens.current directamente (ver design.md Gotcha).
        val backdropScrimColor = LocalDesignTokens.current.backdropScrim

        Box(modifier = Modifier.fillMaxSize()) {
            // Backdrop scrim — tap descarta SIN mutar activeIndex (DECISION D3).
            // Mismo patrón que TopSheet.kt: Canvas + detectTapGestures. Antes hardcoded
            // Color.White.copy(alpha=0.4f) — en dark mode eso pintaba un velo BLANCO
            // brillante sobre un fondo oscuro (mismo bug que backdropScrim ya arregló
            // en PR-2 para AddDatabaseScreen/ConnectionsListScreen). Ahora usa el token
            // real (scheme.background.copy(alpha=0.4f)), coherente en ambos temas.
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { onDismiss() }
                    }
            ) {
                drawRect(backdropScrimColor)
            }

            val itemWidth = 220.dp // ancho de la card VISIBLE (lo que ve el usuario)
            val itemHeight = 240.dp // alto de la card VISIBLE
            // IMPORTANTE: el margen para la sombra tiene que ir en el TAMAÑO que LazyRow le
            // asigna a CADA ITEM (ancho y alto), no solo en el contenedor. LazyRow recorta
            // cada item a su propio rectángulo de posición en AMBOS ejes (necesario para el
            // recycling, y en el eje de scroll — horizontal acá — específicamente para
            // evitar overscroll bleed entre celdas). Agrandar solo el contenedor y centrar
            // el item adentro NO alcanza (ya lo comprobamos con el alto); mismo criterio para
            // el ancho — por eso el mismo `shadowClearance` se usa en ambos ejes.
            val shadowClearance = 80.dp
            // `overlap` es el solape VISUAL deseado entre cards vecinas (lo que se ve, no el
            // ancho de celda). Como cada celda ahora es más ancha (itemWidth + shadowClearance)
            // para dejarle lugar a la sombra, el espaciado negativo de Arrangement tiene que
            // compensar ese ancho extra además del solape visual — si no, las cards visibles
            // quedarían más separadas de lo esperado.
            val overlap = 56.dp // cuánto "entra" una card debajo/encima de su vecina (visual)
            val arrangementSpacing = -(overlap + shadowClearance)

            // initialFirstVisibleItemScrollOffset = 0 centra activeIndex de entrada, porque
            // contentPadding (más abajo) ya deja la primera card VISIBLE centrada.
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = activeIndex)
            val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

            val localDensity = LocalDensity.current
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
            // Centra la card VISIBLE (itemWidth), no la celda completa (itemWidth +
            // shadowClearance) — de ahí el "- shadowClearance / 2": el padding interno del
            // item (ver WorkspaceCarouselItem) ya corre la card visible shadowClearance/2
            // hacia adentro desde el borde de la celda.
            val sidePadding = maxOf(
                (screenWidthDp - itemWidth) / 2 - shadowClearance / 2,
                0.dp
            )

            // Unidad para normalizar "distancia al centro" en fracciones de "una card". Se
            // basa en la distancia VISUAL entre cards (itemWidth - overlap), no en el ancho
            // de celda con clearance — mantiene la misma sensación de profundidad que antes.
            val depthRangePx = with(localDensity) { (itemWidth - overlap).toPx() }.coerceAtLeast(1f)

            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                horizontalArrangement = Arrangement.spacedBy(arrangementSpacing),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = sidePadding),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(itemHeight + shadowClearance)
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
                        shadowClearance = shadowClearance,
                        onSelect = { onSelectCard(i) },
                        onClose = { onCloseCard(i) },
                        modifier = Modifier
                            // El tamaño del ITEM (lo que LazyRow mide y recorta en AMBOS
                            // ejes) es más grande que la card visible en ancho Y alto — el
                            // margen extra lo consume el padding interno de
                            // WorkspaceCarouselItem, dejando lugar a la sombra en los 4 lados.
                            .width(itemWidth + shadowClearance)
                            .height(itemHeight + shadowClearance)
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
 * `depthFraction` (-1..1, 0 = centro) controla el blur/alpha/offset de la sombra dibujada a
 * mano (ver más abajo): la card más cercana al centro tira una sombra más marcada y difusa
 * (parece "flotar" sobre las de atrás), las de los costados casi no proyectan sombra —
 * refuerza la lectura de mazo apilado junto con el solapado/zIndex que aplica el caller
 * ([WorkspaceCarousel]).
 *
 * `shadowClearance`: el `modifier` que llega de [WorkspaceCarousel] ya viene con este alto
 * extra incluido (LazyRow recorta cada item a su propio rectángulo de posición, así que el
 * margen para la sombra DEBE estar en el alto del item mismo, no solo en el contenedor —
 * ver comentario en [WorkspaceCarousel]).
 *
 * La sombra NO usa `Surface.shadowElevation` (elevación nativa de Android) — subir ese valor
 * de 40dp a 80dp de `shadowClearance` no cambió NADA visualmente, lo que indica que el
 * renderizado de sombra por elevación tiene un tope propio (no escala más allá de cierto
 * punto, sin importar cuánto espacio libre le demos). En cambio, se dibuja una sombra propia
 * con `BlurMaskFilter` (API de Android, disponible desde siempre — no requiere minSdk 31+
 * como `RenderEffect`) vía `drawBehind`, dando control total y predecible sobre qué tan
 * grande/suave es, sin depender de un tope interno de la plataforma.
 */
/**
 * Deriva el ARGB de la sombra dibujada a mano del carousel (`BlurMaskFilter`) a partir
 * de [onSurfaceColor] del `ColorScheme` activo. Reemplaza `android.graphics.Color.BLACK`
 * — negro puro es invisible sobre fondos oscuros — por la técnica de "lighter-overlay"
 * de Material para elevación en dark mode (misma decisión que `WorkspaceCarousel`'s
 * backdrop scrim y design.md Architecture Decisions: "Carousel shadow tint").
 *
 * Función pura extraída para test unitario directo, sin dependencias de Compose
 * runtime más allá de `Color` (ver `WorkspaceCarouselShadowTest`).
 *
 * @author gentle-ai (TDD GREEN, PR-3)
 */
internal fun carouselShadowColorArgb(onSurfaceColor: Color): Int = onSurfaceColor.toArgb()

@Composable
private fun WorkspaceCarouselItem(
    card: WorkspaceCard,
    isActive: Boolean,
    depthFraction: Float,
    shadowClearance: Dp,
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
    val density = LocalDensity.current
    // Capturado ANTES de drawBehind (DrawScope no-composable) — ver Gotcha en design.md.
    val shadowColorArgb = carouselShadowColorArgb(MaterialTheme.colorScheme.onSurface)

    // Parámetros de la sombra dibujada a mano (BlurMaskFilter), todos en px ya convertidos.
    // shadowClearance/2 (20dp con clearance=80dp) da de sobra para blur (hasta 26dp) + offset
    // (hasta 8dp) = 34dp máximo por lado, dentro del margen ya reservado en AMBOS ejes.
    // Más blur + menos alpha que la versión anterior = sombra más suave/difusa, menos opaca.
    val cornerRadiusPx = with(density) { 20.dp.toPx() }
    val blurRadiusPx = with(density) { (12.dp + 14.dp * proximityToCenter).toPx() }
    val shadowOffsetYPx = with(density) { (2.dp + 6.dp * proximityToCenter).toPx() }
    val shadowAlpha = 0.10f + 0.22f * proximityToCenter
    val visibleCardLeftPx = with(density) { (shadowClearance / 2).toPx() }
    val visibleCardTopPx = with(density) { (shadowClearance / 2).toPx() }
    val visibleCardWidthPx = with(density) { 220.dp.toPx() } // = itemWidth en WorkspaceCarousel
    val visibleCardHeightPx = with(density) { 240.dp.toPx() } // = itemHeight en WorkspaceCarousel

    Box(
        modifier = modifier.drawBehind {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = shadowColorArgb
                alpha = (shadowAlpha * 255).toInt().coerceIn(0, 255)
                maskFilter = android.graphics.BlurMaskFilter(
                    blurRadiusPx,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRoundRect(
                    visibleCardLeftPx,
                    visibleCardTopPx + shadowOffsetYPx,
                    visibleCardLeftPx + visibleCardWidthPx,
                    visibleCardTopPx + visibleCardHeightPx + shadowOffsetYPx,
                    cornerRadiusPx,
                    cornerRadiusPx,
                    paint
                )
            }
        }
    ) {
        Surface(
            // OJO: NO se aplica `.clip(shape)` acá manualmente — Surface ya clipea su propio
            // contenido a `shape` internamente DESPUÉS de dibujar border. Un `.clip()`
            // externo aplicado ANTES recortaría el border también. Se usa el parámetro
            // nativo `border` de Surface en vez de `Modifier.border` encadenado, para que se
            // dibuje en el orden correcto.
            //
            // padding(shadowClearance / 2) en AMBOS ejes ANTES de fillMaxSize(): consume el
            // tamaño extra que ya viene en `modifier` (ver KDoc arriba) dejando la Surface
            // centrada al tamaño visual real de la card — la sombra ahora es la de
            // drawBehind de arriba, no `shadowElevation` (ver KDoc de la clase).
            modifier = Modifier
                .padding(shadowClearance / 2)
                .fillMaxSize()
                .selectable(selected = isActive, onClick = onSelect, role = Role.Tab),
            shape = shape,
            tonalElevation = if (isActive) 8.dp else 1.dp,
            shadowElevation = 0.dp, // reemplazada por la sombra dibujada a mano (drawBehind)
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
    } // cierra el Box exterior (drawBehind con la sombra manual)
}
