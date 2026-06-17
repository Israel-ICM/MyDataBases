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
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.ui.components.ios.IOSCard
import com.sphynxs.mydatabases.ui.theme.DesignTokens
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme

/**
 * Card de base de datos con diseño iOS unificado.
 *
 * @param database La base de datos a mostrar
 * @param onCardClick Callback cuando se toca la tarjeta (navegar a tablas)
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-17
 */
@Composable
fun DatabaseCard(
    database: Database,
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
            // Ícono de database con gradiente turquesa
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF8EE3D3).copy(alpha = 0.20f),  // Turquesa branded
                                androidx.compose.ui.graphics.Color(0xFF006B63).copy(alpha = 0.12f)   // Turquesa oscuro branded
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorAppIcons.Db.mysql,  // Usa Database genérico
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
                // Nombre de la base de datos
                Text(
                    text = database.name,
                    fontSize = DesignTokens.CardTitleSize,
                    fontWeight = DesignTokens.CardTitleWeight,
                    color = DesignTokens.CardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Charset
                Text(
                    text = database.charset,
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

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun DatabaseCardPreview() {
    MyDataBasesTheme {
        DatabaseCard(
            database = Database(
                name = "my_database",
                charset = "utf8mb4",
                collation = "utf8mb4_unicode_ci"
            ),
            onCardClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
