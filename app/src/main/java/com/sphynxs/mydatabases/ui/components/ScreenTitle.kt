package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Título de pantalla estilo iOS con botón de navegación opcional.
 *
 * Muestra un título grande (34sp) con un botón de retroceso a la izquierda
 * si se proporciona un callback de navegación.
 *
 * @param title Texto del título principal
 * @param subtitle Texto opcional del subtítulo (más pequeño y gris)
 * @param onBackClick Callback opcional para el botón de retroceso
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-30
 */
@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = LocalDesignTokens.current.screenPaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(
                    imageVector = PhosphorAppIcons.Action.back,
                    contentDescription = "Back",
                    tint = LocalDesignTokens.current.largeTitleColor
                )
            }
        }
        
        Column {
            Text(
                text = title,
                fontSize = LocalDesignTokens.current.largeTitleSize,
                fontWeight = LocalDesignTokens.current.largeTitleWeight,
                color = LocalDesignTokens.current.largeTitleColor
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = LocalDesignTokens.current.cardSubtitleSize,
                    fontWeight = LocalDesignTokens.current.cardSubtitleWeight,
                    color = LocalDesignTokens.current.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
