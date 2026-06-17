package com.sphynxs.mydatabases.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * Mapeo de íconos de la aplicación usando Material Icons Rounded.
 *
 * Todos los íconos son minimalistas, modernos y con esquinas redondeadas.
 *
 * ## Uso
 *
 * ```kotlin
 * Icon(
 *     imageVector = PhosphorAppIcons.Nav.connections,
 *     contentDescription = "Connections",
 *     tint = MaterialTheme.colorScheme.primary
 * )
 * ```
 *
 * @author israel-icm
 * @date 2026-06-17
 */
object PhosphorAppIcons {
    
    /**
     * Íconos de navegación (BottomBar, Rail, Drawer).
     */
    object Nav {
        val connections: ImageVector get() = Icons.Rounded.Dns  // Servidores/infraestructura
        val settings: ImageVector get() = Icons.Rounded.Settings
        val tables: ImageVector get() = Icons.Rounded.TableChart
        val views: ImageVector get() = Icons.Rounded.Visibility
        val editor: ImageVector get() = Icons.Rounded.Code
        val functions: ImageVector get() = Icons.Rounded.Functions
        val backup: ImageVector get() = Icons.Rounded.Save
    }
    
    /**
     * Íconos de tipos de base de datos.
     */
    object Db {
        val mysql: ImageVector get() = Icons.Rounded.Storage
        val postgres: ImageVector get() = Icons.Rounded.Storage
        val sqlite: ImageVector get() = Icons.Rounded.Storage
        val mariadb: ImageVector get() = Icons.Rounded.Storage
        val sqlServer: ImageVector get() = Icons.Rounded.Storage
        
        fun icon(type: DatabaseType): ImageVector = when (type) {
            DatabaseType.MYSQL -> mysql
            DatabaseType.POSTGRESQL -> postgres
            DatabaseType.SQLITE -> sqlite
            DatabaseType.MARIADB -> mariadb
        }
    }
    
    /**
     * Íconos de estados (empty states, error states).
     */
    object State {
        val emptyConnections: ImageVector get() = Icons.Rounded.Storage
        val emptyTables: ImageVector get() = Icons.Rounded.FolderOpen
        val error: ImageVector get() = Icons.Rounded.Warning
    }
    
    /**
     * Íconos de acciones comunes.
     */
    object Action {
        val add: ImageVector get() = Icons.Rounded.Add
        val edit: ImageVector get() = Icons.Rounded.Edit
        val delete: ImageVector get() = Icons.Rounded.Delete
        val search: ImageVector get() = Icons.Rounded.Search
        val close: ImageVector get() = Icons.Rounded.Close
        val check: ImageVector get() = Icons.Rounded.Check
        val info: ImageVector get() = Icons.Rounded.Info
    }
}
