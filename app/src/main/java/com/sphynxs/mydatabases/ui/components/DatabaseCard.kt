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
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import com.sphynxs.mydatabases.ui.theme.AppTheme

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
                .padding(LocalDesignTokens.current.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalDesignTokens.current.innerSpacing)
        ) {
            // Ícono de database con gradiente turquesa
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        // accentSecondary = brand_tertiary (0xFF8EE3D3); accentSuccess = el
                        // mismo turquesa oscuro (0xFF006B63) — ambos exact-match a los
                        // literales previos, ahora vía token theme-invariant (DesignTokens.kt).
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                LocalDesignTokens.current.accentSecondary.copy(alpha = 0.20f),
                                LocalDesignTokens.current.accentSuccess.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorAppIcons.Db.mysql,  // Usa Database genérico
                    contentDescription = null,
                    tint = LocalDesignTokens.current.accentSuccess,
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
                    fontSize = LocalDesignTokens.current.cardTitleSize,
                    fontWeight = LocalDesignTokens.current.cardTitleWeight,
                    color = LocalDesignTokens.current.cardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Charset
                Text(
                    text = database.charset,
                    fontSize = LocalDesignTokens.current.cardSubtitleSize,
                    fontWeight = LocalDesignTokens.current.cardSubtitleWeight,
                    color = LocalDesignTokens.current.cardSubtitleColor,
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
    AppTheme {
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
