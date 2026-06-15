package com.sphynxs.mydatabases.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppElevation
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Tarjeta reutilizable para mostrar una conexión en la lista.
 *
 * Muestra: nombre, host:puerto, tipo de DB, y acciones (editar/eliminar).
 *
 * @param connection La configuración de conexión a mostrar
 * @param onEditClick Callback cuando se toca el botón editar
 * @param onDeleteClick Callback cuando se toca el botón eliminar
 * @param onCardClick Callback cuando se toca la tarjeta completa (conectar)
 * @param modifier Modificador opcional para la tarjeta
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Composable
fun ConnectionCard(
    connection: ConnectionConfig,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalAppSpacing.current
    val shapes = LocalAppShapes.current
    val elevation = LocalAppElevation.current

    Card(
        onClick = onCardClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = elevation.cardResting, shape = shapes.medium),
        shape = shapes.medium,
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenido principal
            Column(modifier = Modifier.weight(1f)) {
                // Nombre de la conexión
                Text(
                    text = connection.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(spacing.xxs))

                // Host:puerto
                Text(
                    text = "${connection.host}:${connection.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                // Chip con el tipo de DB
                SuggestionChip(
                    onClick = { /* No interactivo */ },
                    label = {
                        Text(
                            text = when (connection.type) {
                                DatabaseType.MYSQL -> "MySQL"
                                DatabaseType.MARIADB -> "MariaDB"
                                DatabaseType.POSTGRESQL -> "PostgreSQL"
                                DatabaseType.SQLITE -> "SQLite"
                            }
                        )
                    }
                )
            }

            // Acciones
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.connection_action_edit)
                )
            }

            Spacer(modifier = Modifier.width(spacing.xxs))

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.connection_action_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Preview para ConnectionCard.
 */
@Preview(showBackground = true)
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
            onCardClick = {}
        )
    }
}
