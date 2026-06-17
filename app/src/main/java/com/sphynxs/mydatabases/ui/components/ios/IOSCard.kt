package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Card moderno con sombras suaves y sin bordes.
 *
 * @param onClick Callback cuando se toca el card
 * @param modifier Modificador opcional
 * @param content Contenido del card
 *
 * @author israel-icm
 * @date 2026-06-17
 */
@Composable
fun IOSCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF7C80E8).copy(alpha = 0.15f),  // Violeta branded
                spotColor = Color(0xFF7C80E8).copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    color = Color(0xFF7C80E8).copy(alpha = 0.08f)  // Violeta branded
                ),
                onClick = onClick
            )
    ) {
        content()
    }
}
