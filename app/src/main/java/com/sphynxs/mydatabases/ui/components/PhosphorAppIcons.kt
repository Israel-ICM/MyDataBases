package com.sphynxs.mydatabases.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.adamglin.phosphoricons.PhosphorIcons
import com.adamglin.phosphoricons.regular.*
import com.sphynxs.mydatabases.core.database.engine.DatabaseType

/**
 * Mapeo de íconos de la aplicación usando Phosphor Icons.
 *
 * Todos los íconos son minimalistas, modernos y con esquinas redondeadas.
 * Usa el weight "Regular" por defecto para consistencia.
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
        val connections: ImageVector
            @Composable get() = PhosphorIcons.Regular.Database
        
        val settings: ImageVector
            @Composable get() = PhosphorIcons.Regular.Gear
        
        val tables: ImageVector
            @Composable get() = PhosphorIcons.Regular.Table
        
        val views: ImageVector
            @Composable get() = PhosphorIcons.Regular.Eye
        
        val editor: ImageVector
            @Composable get() = PhosphorIcons.Regular.Code
        
        val functions: ImageVector
            @Composable get() = PhosphorIcons.Regular.Function
        
        val backup: ImageVector
            @Composable get() = PhosphorIcons.Regular.FloppyDisk
    }
    
    /**
     * Íconos de tipos de base de datos.
     * 
     * NOTA: Por ahora usa íconos genéricos de Phosphor.
     * Los logos específicos (MySQL, PostgreSQL) se mantienen en AppIcons.Db
     */
    object Db {
        val mysql: ImageVector
            @Composable get() = PhosphorIcons.Regular.Database
        
        val postgres: ImageVector
            @Composable get() = PhosphorIcons.Regular.Database
        
        val sqlite: ImageVector
            @Composable get() = PhosphorIcons.Regular.Database
        
        val mariadb: ImageVector
            @Composable get() = PhosphorIcons.Regular.Database
        
        val sqlServer: ImageVector
            @Composable get() = PhosphorIcons.Regular.Database
        
        /**
         * Obtiene el ícono según el tipo de base de datos.
         */
        @Composable
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
        val emptyConnections: ImageVector
            @Composable get() = PhosphorIcons.Regular.Database
        
        val emptyTables: ImageVector
            @Composable get() = PhosphorIcons.Regular.FolderOpen
        
        val error: ImageVector
            @Composable get() = PhosphorIcons.Regular.Warning
    }
    
    /**
     * Íconos de acciones comunes.
     */
    object Action {
        val add: ImageVector
            @Composable get() = PhosphorIcons.Regular.Plus
        
        val edit: ImageVector
            @Composable get() = PhosphorIcons.Regular.Notepencil
        
        val delete: ImageVector
            @Composable get() = PhosphorIcons.Regular.Trash
        
        val search: ImageVector
            @Composable get() = PhosphorIcons.Regular.Magnifyingglass
        
        val close: ImageVector
            @Composable get() = PhosphorIcons.Regular.X
        
        val check: ImageVector
            @Composable get() = PhosphorIcons.Regular.Check
        
        val info: ImageVector
            @Composable get() = PhosphorIcons.Regular.Info
    }
}
