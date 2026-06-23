package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.ui.components.ios.IOSCard
import com.sphynxs.mydatabases.ui.theme.DbAccents
import com.sphynxs.mydatabases.ui.theme.DesignTokens
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme

/**
 * Card de conexión con diseño iOS unificado.
 *
 * @param connection La configuración de conexión a mostrar
 * @param onEditClick Callback cuando se toca el botón editar
 * @param onDeleteClick Callback cuando se toca el botón eliminar
 * @param onCardClick Callback cuando se toca la tarjeta completa (conectar)
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-17
 */
@Composable
fun ConnectionCard(
    connection: ConnectionConfig,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = DbAccents.accentFor(connection.type)

    IOSCard(
        onClick = {},
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.InnerSpacing)
        ) {
            // Ícono con gradiente vibrante
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.25f),
                                accentColor.copy(alpha = 0.12f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        when (connection.type) {
                            DatabaseType.MYSQL -> AppIcons.Db.MySql
                            DatabaseType.POSTGRESQL -> AppIcons.Db.Postgres
                            DatabaseType.MARIADB -> AppIcons.Db.MariaDb
                            DatabaseType.SQLITE -> AppIcons.Db.Sqlite
                        }
                    ),
                    contentDescription = connection.type.displayName,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Contenido principal (clickable para conectar)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCardClick),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Nombre de la conexión
                Text(
                    text = connection.name,
                    fontSize = DesignTokens.CardTitleSize,
                    fontWeight = DesignTokens.CardTitleWeight,
                    color = DesignTokens.CardTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Host:Puerto
                Text(
                    text = "${connection.host}:${connection.port}",
                    fontSize = DesignTokens.CardSubtitleSize,
                    fontWeight = DesignTokens.CardSubtitleWeight,
                    color = DesignTokens.CardSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Botones de acción
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = PhosphorAppIcons.Action.edit,
                    contentDescription = stringResource(R.string.connection_action_edit),
                    tint = DesignTokens.IconNormal,
                    modifier = Modifier.size(DesignTokens.IconSmall)
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = PhosphorAppIcons.Action.delete,
                    contentDescription = stringResource(R.string.connection_action_delete),
                    tint = DesignTokens.IconNormal,
                    modifier = Modifier.size(DesignTokens.IconSmall)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun ConnectionCardPreview() {
    MyDataBasesTheme {
        ConnectionCard(
            connection = ConnectionConfig(
                id = "1",
                name = "Producción",
                type = DatabaseType.MYSQL,
                host = "db.example.com",
                port = 3306,
                database = "mydb",
                username = "admin",
                password = "secret"
            ),
            onEditClick = {},
            onDeleteClick = {},
            onCardClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
