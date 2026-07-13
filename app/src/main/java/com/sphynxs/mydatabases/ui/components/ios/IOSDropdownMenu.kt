package com.sphynxs.mydatabases.ui.components.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Dropdown menu estilo iOS con bordes redondeados y sombra suave.
 *
 * Wrapper sobre Material3 DropdownMenu que aplica el estilo visual
 * consistente con el resto de la app.
 *
 * @param expanded Si el menú está visible
 * @param onDismissRequest Callback cuando se cierra el menú
 * @param modifier Modificador opcional
 * @param offset Offset del menú respecto al anchor
 * @param content Contenido del menú (DropdownMenuItem)
 *
 * @author israel-icm
 * @date 2026-06-30
 */
@Composable
fun IOSDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = LocalDesignTokens.current.cardShadowColor
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        content = content
    )
}
