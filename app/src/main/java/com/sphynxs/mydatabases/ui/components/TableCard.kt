package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.core.database.models.Table as TableModel
import com.sphynxs.mydatabases.ui.components.ios.IOSCard
import com.sphynxs.mydatabases.ui.theme.DesignTokens
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme

/**
 * Card de tabla con diseño iOS unificado.
 *
 * @param table La tabla a mostrar
 * @param onCardClick Callback cuando se toca la tarjeta (navegar a visor de tabla)
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-17
 */
@Composable
fun TableCard(
    table: TableModel,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IOSCard(
        onClick = onCardClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.InnerSpacing)
        ) {
            // Ícono de tabla con gradiente turquesa
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFFA3F2E6).copy(alpha = 0.20f),  // Turquesa claro branded
                                androidx.compose.ui.graphics.Color(0xFF006B63).copy(alpha = 0.12f)   // Turquesa oscuro branded
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorAppIcons.Nav.tables,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFF006B63),  // Turquesa oscuro branded
                    modifier = Modifier.size(32.dp)
                )
            }

            // Contenido principal
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nombre de la tabla
                Text(
                    text = table.name,
                    fontSize = DesignTokens.CardTitleSize,
                    fontWeight = DesignTokens.CardTitleWeight,
                    color = DesignTokens.CardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Engine y row count
                val subtitle = buildString {
                    table.engine?.let { append(it) }
                    if (table.rowCount != null) {
                        if (isNotEmpty()) append(" • ")
                        append("${table.rowCount} rows")
                    }
                }
                
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = DesignTokens.CardSubtitleSize,
                        fontWeight = DesignTokens.CardSubtitleWeight,
                        color = DesignTokens.CardSubtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun TableCardPreview() {
    MyDataBasesTheme {
        TableCard(
            table = TableModel(
                name = "users",
                database = "mydb",
                type = com.sphynxs.mydatabases.core.database.models.TableType.TABLE,
                engine = "InnoDB",
                rowCount = 1234
            ),
            onCardClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
