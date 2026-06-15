package com.sphynxs.mydatabases.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.sphynxs.mydatabases.R

/**
 * Wrapper centralizado para acceso a íconos de la aplicación.
 *
 * Todos los íconos custom (vectores XML) se exponen como propiedades lazy
 * organizadas por dominio (Nav, Db, State).
 *
 * ## Uso
 *
 * ```kotlin
 * Icon(
 *     painter = AppIcons.Nav.Connections,
 *     contentDescription = "Connections",
 *     tint = MaterialTheme.colorScheme.primary
 * )
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
object AppIcons {
    
    /**
     * Íconos de navegación (BottomBar, Rail, Drawer).
     */
    object Nav {
        /**
         * Ícono de "Conexiones" — lista de conexiones guardadas.
         */
        @DrawableRes val Connections: Int = R.drawable.ic_nav_connections
        
        /**
         * Ícono de "Configuración" — ajustes de la app.
         */
        @DrawableRes val Settings: Int = R.drawable.ic_nav_settings
        
        /**
         * Ícono de "Tablas" — lista de tablas en una base de datos.
         */
        @DrawableRes val Tables: Int = R.drawable.ic_nav_tables
        
        /**
         * Ícono de "Vistas" — lista de vistas (views) en una base de datos.
         */
        @DrawableRes val Views: Int = R.drawable.ic_nav_views
        
        /**
         * Ícono de "Editor" — editor de consultas SQL.
         */
        @DrawableRes val Editor: Int = R.drawable.ic_nav_editor
        
        /**
         * Ícono de "Funciones" — lista de funciones/stored procedures.
         */
        @DrawableRes val Functions: Int = R.drawable.ic_nav_functions
        
        /**
         * Ícono de "Backup" — exportar/backup de base de datos.
         */
        @DrawableRes val Backup: Int = R.drawable.ic_nav_backup
    }
    
    /**
     * Íconos de tipos de base de datos (providers).
     */
    object Db {
        /**
         * Logo MySQL (estilizado).
         */
        @DrawableRes val MySql: Int = R.drawable.ic_db_mysql
        
        /**
         * Logo PostgreSQL (estilizado).
         */
        @DrawableRes val Postgres: Int = R.drawable.ic_db_postgres
        
        /**
         * Logo SQLite (estilizado).
         */
        @DrawableRes val Sqlite: Int = R.drawable.ic_db_sqlite
        
        /**
         * Logo MariaDB (estilizado).
         */
        @DrawableRes val MariaDb: Int = R.drawable.ic_db_mariadb
        
        /**
         * Logo SQL Server (estilizado).
         */
        @DrawableRes val SqlServer: Int = R.drawable.ic_db_sqlserver
        
        /**
         * Obtiene el ícono correcto según el tipo de base de datos.
         */
        fun icon(type: com.sphynxs.mydatabases.core.database.engine.DatabaseType): Int = when (type) {
            com.sphynxs.mydatabases.core.database.engine.DatabaseType.MYSQL -> MySql
            com.sphynxs.mydatabases.core.database.engine.DatabaseType.POSTGRESQL -> Postgres
            com.sphynxs.mydatabases.core.database.engine.DatabaseType.SQLITE -> Sqlite
            com.sphynxs.mydatabases.core.database.engine.DatabaseType.MARIADB -> MariaDb
        }
    }
    
    /**
     * Íconos de estados (empty states, error states).
     */
    object State {
        /**
         * Ilustración de "sin conexiones" (servidor con X).
         */
        @DrawableRes val EmptyConnections: Int = R.drawable.ic_state_empty_connections
        
        /**
         * Ilustración de "sin tablas" (carpeta vacía).
         */
        @DrawableRes val EmptyTables: Int = R.drawable.ic_state_empty_tables
        
        /**
         * Ilustración de error genérico (círculo con exclamación).
         */
        @DrawableRes val Error: Int = R.drawable.ic_state_error
    }
}
