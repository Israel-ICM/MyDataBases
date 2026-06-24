package com.sphynxs.mydatabases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.ui.theme.DbAccents
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppShapes
import com.sphynxs.mydatabases.ui.theme.tokens.LocalAppSpacing

/**
 * Hero connection card — diseño destacado inspirado en PlayStation App.
 *
 * Características:
 * - Hero icon 88dp con radial gradient usando el accent del tipo de DB
 * - Card sin borders, elevation 8dp, shape 24dp (xlShape)
 * - Width 92% del ancho de pantalla
 * - Typography: titleLarge bold para nombre, bodySmall mono para host:port
 * - Status pill con dot + "Inactiva"
 *
 * @param connection Configuración de conexión a mostrar
 * @param onClick Callback cuando se toca la card completa
 * @param onMenuClick Callback cuando se toca el botón de menú (tres puntos)
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun HeroConnectionCard(
    connection: ConnectionConfig,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalAppSpacing.current
    val shapes = LocalAppShapes.current
    val accentColor = DbAccents.accentFor(connection.type)
    
    // Width: 92% del ancho de pantalla
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardWidth = screenWidth * 0.92f

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(0.92f),
        shape = shapes.xlShape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hero Icon 88dp con radial gradient
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.25f),
                                accentColor.copy(alpha = 0.08f)
                            )
                        ),
                        shape = CircleShape
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
                    modifier = Modifier.size(48.dp),
                    tint = accentColor
                )
            }

            // Content column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                // Nombre de la conexión (titleLarge bold)
                Text(
                    text = connection.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Host:port en mono font (bodySmall)
                Text(
                    text = "${connection.host}:${connection.port}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Status pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status dot 8dp
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary,
                                CircleShape
                            )
                    )
                    Text(
                        text = stringResource(R.string.inactive_status),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Menu button (tres puntos verticales)
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menú",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Preview con MySQL.
 */
@Preview(name = "MySQL", showBackground = true)
@Composable
private fun HeroConnectionCardPreview_MySQL() {
    MyDataBasesTheme {
        HeroConnectionCard(
            connection = ConnectionConfig(
                id = "1",
                name = "Producción MySQL",
                type = DatabaseType.MYSQL,
                host = "db.example.com",
                port = 3306,
                database = "mydb",
                username = "admin",
                password = "secret"
            ),
            onClick = {},
            onMenuClick = {}
        )
    }
}

/**
 * Preview con PostgreSQL.
 */
@Preview(name = "PostgreSQL", showBackground = true)
@Composable
private fun HeroConnectionCardPreview_Postgres() {
    MyDataBasesTheme {
        HeroConnectionCard(
            connection = ConnectionConfig(
                id = "2",
                name = "Dev Postgres",
                type = DatabaseType.POSTGRESQL,
                host = "localhost",
                port = 5432,
                database = "devdb",
                username = "dev",
                password = "dev123"
            ),
            onClick = {},
            onMenuClick = {}
        )
    }
}

/**
 * Preview con MariaDB.
 */
@Preview(name = "MariaDB", showBackground = true)
@Composable
private fun HeroConnectionCardPreview_MariaDB() {
    MyDataBasesTheme {
        HeroConnectionCard(
            connection = ConnectionConfig(
                id = "3",
                name = "Legacy MariaDB",
                type = DatabaseType.MARIADB,
                host = "maria.internal",
                port = 3306,
                database = "legacy",
                username = "root",
                password = "root"
            ),
            onClick = {},
            onMenuClick = {}
        )
    }
}

/**
 * Preview con SQLite.
 */
@Preview(name = "SQLite", showBackground = true)
@Composable
private fun HeroConnectionCardPreview_SQLite() {
    MyDataBasesTheme {
        HeroConnectionCard(
            connection = ConnectionConfig(
                id = "4",
                name = "Local SQLite",
                type = DatabaseType.SQLITE,
                host = "file",
                port = 0,
                database = "app.db",
                username = "",
                password = ""
            ),
            onClick = {},
            onMenuClick = {}
        )
    }
}
