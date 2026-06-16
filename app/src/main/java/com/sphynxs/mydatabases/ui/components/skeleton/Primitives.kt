package com.sphynxs.mydatabases.ui.components.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes

/**
 * Primitivas reusables para construir skeletons de carga.
 *
 * Estas primitivas componen los skeletons específicos de cada pantalla.
 * Son bloques estáticos grises sin animación shimmer (shimmer se agrega en cambio futuro de motion).
 *
 * Color del placeholder: surfaceVariant con alpha 0.6 para efecto sutil.
 *
 * @author israel-icm
 * @date 2026-06-15
 */

/**
 * Caja skeleton rectangular con forma configurable.
 *
 * Usá esto para representar bloques de contenido (cards, botones, etc.).
 *
 * @param width Ancho del skeleton
 * @param height Alto del skeleton
 * @param shape Forma del skeleton (default: AppShapes.medium)
 * @param modifier Modificador opcional
 */
@Composable
fun SkeletonBox(
    width: Dp,
    height: Dp,
    shape: Shape = LocalAppShapes.current.medium,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    )
}

/**
 * Skeleton de texto simulando líneas de texto.
 *
 * Cada línea es un SkeletonBox de altura fija (12.dp).
 * Las líneas se separan con 4.dp de spacing.
 *
 * @param width Ancho de cada línea
 * @param lines Cantidad de líneas a simular
 * @param modifier Modificador opcional
 */
@Composable
fun SkeletonText(
    width: Dp = 120.dp,
    lines: Int = 1,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        repeat(lines) { index ->
            SkeletonBox(
                width = if (index == lines - 1) width * 0.7f else width,  // Última línea más corta
                height = 12.dp,
                shape = LocalAppShapes.current.small
            )
            if (index < lines - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Skeleton circular para íconos o avatares.
 *
 * @param size Diámetro del círculo
 * @param modifier Modificador opcional
 */
@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    )
}
