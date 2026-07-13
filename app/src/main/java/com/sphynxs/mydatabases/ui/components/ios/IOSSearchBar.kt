package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Search bar estilo iOS 26 — ultra minimalista, sin fondo de color.
 *
 * Exactamente como en iCloud Notes: solo ícono + texto + clear, fondo blanco sutil.
 *
 * @param query El valor actual del campo de búsqueda
 * @param onQueryChange Callback cuando cambia el texto
 * @param placeholder Texto del placeholder
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-17
 */
@Composable
fun IOSSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    // textPrimary = scheme.onBackground — usado tanto para el texto/ícono como base de
    // la sombra ambient/spot (antes Color.Black fijo, invisible sobre fondo ya oscuro en
    // dark mode; mismo criterio que WorkspaceCarousel — ver design.md R6).
    val tokens = LocalDesignTokens.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(26.dp),  // CASI REDONDO (la mitad de la altura aprox)
                ambientColor = tokens.textPrimary.copy(alpha = 0.04f),
                spotColor = tokens.textPrimary.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(tokens.surfacePrimary)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Lupa GRANDE, theme-aware
                Icon(
                    imageVector = PhosphorAppIcons.Action.search,
                    contentDescription = null,
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(24.dp)  // MÁS GRANDE
                )

                // Campo de texto
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 12.dp)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(
                            color = tokens.textPrimary,
                            fontSize = 17.sp,  // PROLIJO con el ícono
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(tokens.accentPrimary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = tokens.textPrimary.copy(alpha = 0.4f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // X cuando hay texto
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorAppIcons.Action.close,
                            contentDescription = "Clear",
                            tint = tokens.textPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
