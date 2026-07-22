package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Card moderno con sombras suaves y sin bordes.
 *
 * @param onClick Callback cuando se toca el card
 * @param modifier Modificador opcional
 * @param onLongClick Callback opcional cuando se mantiene presionado el card (long-press).
 *   `null` (default) preserva el comportamiento previo: solo tap, sin gesto de long-press
 *   ni feedback háptico asociado — no rompe a los callers existentes (ConnectionCard,
 *   FolderCard, DatabaseActionMenuTile, etc.) que no lo pasan.
 * @param content Contenido del card
 *
 * @author israel-icm
 * @date 2026-06-17 (updated 2026-07-21: soporte de long-press para menú contextual
 *   de tablas, change `table-row-actions-menu`)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IOSCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                // cardShadowColor ya es brand_primary.copy(alpha=0.15f) — mismo valor que
                // el literal reemplazado, ahora vía token (theme-invariant por diseño, ver
                // DesignTokens.kt buildDesignTokens).
                ambientColor = LocalDesignTokens.current.cardShadowColor,
                spotColor = LocalDesignTokens.current.accentPrimary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(LocalDesignTokens.current.surfacePrimary)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(
                    color = LocalDesignTokens.current.accentPrimary.copy(alpha = 0.08f)
                ),
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        content()
    }
}
