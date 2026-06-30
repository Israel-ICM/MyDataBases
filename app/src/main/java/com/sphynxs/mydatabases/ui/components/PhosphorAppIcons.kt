package com.sphynxs.mydatabases.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * Mapeo de íconos de la aplicación usando Tabler Icons.
 *
 * Todos los íconos son clean, minimal y consistentes.
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
        val connections: ImageVector get() = TablerIcons.Server
        val settings: ImageVector get() = TablerIcons.Settings
        val addDatabase: ImageVector get() = TablerIcons.Plus
        val newQuery: ImageVector get() = TablerIcons.FileText
        val monitor: ImageVector get() = TablerIcons.Activity
        val tables: ImageVector get() = TablerIcons.Table
        val views: ImageVector get() = TablerIcons.Eye
        val editor: ImageVector get() = TablerIcons.Code
        val functions: ImageVector get() = TablerIcons.Math
        val backup: ImageVector get() = TablerIcons.DeviceFloppy
    }
    
    /**
     * Íconos de tipos de base de datos.
     */
    object Db {
        val mysql: ImageVector get() = TablerIcons.Database
        val postgres: ImageVector get() = TablerIcons.Database
        val sqlite: ImageVector get() = TablerIcons.Database
        val mariadb: ImageVector get() = TablerIcons.Database
        val sqlServer: ImageVector get() = TablerIcons.Database
        
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
        val emptyConnections: ImageVector get() = TablerIcons.Database
        val emptyTables: ImageVector get() = TablerIcons.Folder
        val error: ImageVector get() = TablerIcons.AlertTriangle
    }
    
    /**
     * Íconos de acciones comunes.
     */
    object Action {
        val add: ImageVector get() = TablerIcons.Plus
        val edit: ImageVector get() = TablerIcons.Edit
        val delete: ImageVector get() = TablerIcons.Trash
        val search: ImageVector get() = TablerIcons.Search
        val close: ImageVector get() = TablerIcons.X
        val check: ImageVector get() = TablerIcons.Check
        val info: ImageVector get() = TablerIcons.InfoCircle
        val back: ImageVector get() = TablerIcons.ArrowLeft
        val more: ImageVector get() = TablerIcons.DotsVertical
        val power: ImageVector get() = TablerIcons.Power
    }
}
