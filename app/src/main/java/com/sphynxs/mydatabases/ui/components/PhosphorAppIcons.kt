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
        val newTable: ImageVector get() = TablerIcons.Plus
        val newQuery: ImageVector get() = TablerIcons.FileText

        // Chosen for the "Open Query File" entry-point selector option (change
        // `large-sql-script-execution`): TablerIcons exposes `FileImport` as the closest
        // purpose-built match for "bring a file in" — no dedicated `Open`/`FileOpen` variant
        // exists in this icon set, and `FileImport` reads clearly as importing a file for use.
        val openQueryFile: ImageVector get() = TablerIcons.FileImport

        // Chosen for the "Run Script (No Edit)" entry-point selector option (change
        // `large-sql-script-execution`): `PlayerPlay` (a media-style play glyph) is the clearest
        // "execute/run" signifier available in TablerIcons — no dedicated `Run`/`Execute` glyph
        // exists, and this is already the conventional icon for "run" in most dev tools.
        val runScript: ImageVector get() = TablerIcons.PlayerPlay
        val console: ImageVector get() = TablerIcons.Activity
        val monitor: ImageVector get() = TablerIcons.Activity
        val tables: ImageVector get() = TablerIcons.Table
        val views: ImageVector get() = TablerIcons.Eye
        val editor: ImageVector get() = TablerIcons.Code
        val functions: ImageVector get() = TablerIcons.Math
        val backup: ImageVector get() = TablerIcons.DeviceFloppy

        // Chosen for the "Automatizaciones" database action menu tile: TablerIcons
        // exposes `SettingsAutomation` (a gear + flow-arrow glyph) as the closest
        // purpose-built match for automation in this icon set — no dedicated
        // `Bolt`/`Robot`/`Cpu` "automation" variant exists, and `SettingsAutomation`
        // reads clearly as "automated settings/workflow" at tile size.
        val automations: ImageVector get() = TablerIcons.SettingsAutomation
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
        val dragHandle: ImageVector get() = TablerIcons.GripVertical

        // Popup menu de acciones de tabla (change `table-row-actions-menu`):
        val duplicate: ImageVector get() = TablerIcons.Files

        // Eraser en vez de Trash (ya usado por `delete`): truncar borra todas las filas
        // pero conserva la estructura de la tabla, a diferencia de eliminar la tabla entera.
        val truncate: ImageVector get() = TablerIcons.Eraser

        // Download representa sacar/guardar los datos fuera de la app.
        val export: ImageVector get() = TablerIcons.Download
        val copy: ImageVector get() = TablerIcons.Copy
        val share: ImageVector get() = TablerIcons.Share

        // Tag representa la acción de re-etiquetar/renombrar.
        val rename: ImageVector get() = TablerIcons.Tag

        // Pin representa "fijar" un acceso directo/atajo rápido.
        val addShortcut: ImageVector get() = TablerIcons.Pin
    }
}
