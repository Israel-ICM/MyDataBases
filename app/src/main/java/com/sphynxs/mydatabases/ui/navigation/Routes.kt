package com.sphynxs.mydatabases.ui.navigation

/**
 * Rutas de navegación de la aplicación.
 *
 * Define todas las pantallas disponibles y sus argumentos de navegación.
 *
 * @property route El template del path de navegación (ej: "connections", "table_list/{databaseName}")
 *
 * @author israel-icm
 * @date 2026-06-12
 */
sealed class Routes(val route: String) {
    
    /**
     * Pantalla de lista de conexiones guardadas.
     */
    data object Connections : Routes("connections")
    
    /**
     * Pantalla de formulario de conexión (crear/editar).
     */
    data object ConnectionForm : Routes("connection_form")
    
    /**
     * Pantalla de lista de bases de datos disponibles en la conexión activa.
     * 
     * Usa el patrón contextual connection/{connectionId}/databases para ser
     * consistente con el resto de destinos InsideConnection.
     *
     * @property route Template con argumento `connectionId`
     */
    data object Databases : Routes("connection/{connectionId}/databases") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/databases")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/databases"
        }
    }
    
    /**
     * Pantalla de formulario para agregar una nueva base de datos/schema.
     * 
     * @property route Template con argumento `connectionId`
     */
    data object AddDatabase : Routes("connection/{connectionId}/add_database") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/add_database")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/add_database"
        }
    }
    
    /**
     * Acción modal para crear una nueva tabla dentro de una base de datos (change `create-table`).
     *
     * No se registra como destino navegable real en el `NavHost` — al igual que [AddDatabase],
     * solo se usa como identificador de ruta para [NavigationDestination]; la acción modal
     * (`new_table`) abre un `ModalBottomSheet` en vez de navegar.
     *
     * @property route Template con argumento `connectionId`
     */
    data object NewTable : Routes("connection/{connectionId}/new_table") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/new_table")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/new_table"
        }
    }
    
    /**
     * Pantalla del editor de consultas SQL (placeholder para nueva query).
     * 
     * @property route Template con argumento `connectionId`
     */
    data object NewQuery : Routes("connection/{connectionId}/new_query") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/new_query")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/new_query"
        }
    }
    
    /**
     * Pantalla de monitoreo del servidor (métricas, queries, salud).
     * 
     * @property route Template con argumento `connectionId`
     */
    data object Monitor : Routes("connection/{connectionId}/monitor") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/monitor")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/monitor"
        }
    }
    
    /**
     * Pantalla de lista de tablas de una base de datos específica.
     *
     * NOTA: Esta ruta usa el modelo contextual connection/{id}/tables/{db}
     * para mantener el connectionId y ser consistente con otros destinos.
     *
     * @property route Template con argumentos `connectionId` y `databaseName`
     */
    data object TableList : Routes("connection/{connectionId}/tables/{databaseName}") {
        /**
         * Crea la ruta completa reemplazando los argumentos.
         *
         * @param connectionId ID de la conexión activa
         * @param databaseName Nombre de la base de datos
         * @return Ruta navegable (ej: "connection/abc-123/tables/my_db")
         */
        fun createRoute(connectionId: String, databaseName: String): String {
            return "connection/$connectionId/tables/$databaseName"
        }
    }
    
    /**
     * Pantalla de visualización de datos de una tabla específica.
     *
     * @property route Template con argumentos `databaseName` y `tableName`
     */
    data object TableViewer : Routes("table_viewer/{databaseName}/{tableName}") {
        /**
         * Crea la ruta completa reemplazando los argumentos.
         *
         * @param databaseName Nombre de la base de datos
         * @param tableName Nombre de la tabla
         * @return Ruta navegable (ej: "table_viewer/my_db/users")
         */
        fun createRoute(databaseName: String, tableName: String): String {
            return "table_viewer/$databaseName/$tableName"
        }
    }
    
    /**
     * Pantalla del editor de consultas SQL dentro de una conexión.
     *
     * @property route Template con argumento `connectionId`
     */
    data object QueryEditor : Routes("connection/{connectionId}/editor") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/editor")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/editor"
        }
    }
    
    /**
     * Pantalla de lista de vistas (views) dentro de una conexión.
     *
     * @property route Template con argumento `connectionId`
     */
    data object Views : Routes("connection/{connectionId}/views") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/views")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/views"
        }
    }
    
    /**
     * Pantalla de lista de funciones/stored procedures dentro de una conexión.
     *
     * @property route Template con argumento `connectionId`
     */
    data object Functions : Routes("connection/{connectionId}/functions") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/functions")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/functions"
        }
    }
    
    /**
     * Pantalla de backup/export de base de datos dentro de una conexión.
     *
     * @property route Template con argumento `connectionId`
     */
    data object Backup : Routes("connection/{connectionId}/backup") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/backup")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/backup"
        }
    }
    
    /**
     * Pantalla de lista de tablas dentro de una conexión.
     *
     * NOTA: Esta ruta usa el modelo contextual connection/{id}/tables
     * para ser consistente con el resto de destinos InsideConnection.
     *
     * @property route Template con argumento `connectionId`
     */
    data object Tables : Routes("connection/{connectionId}/tables") {
        /**
         * Crea la ruta completa reemplazando el argumento connectionId.
         *
         * @param connectionId ID de la conexión activa
         * @return Ruta navegable (ej: "connection/abc-123/tables")
         */
        fun createRoute(connectionId: String): String {
            return "connection/$connectionId/tables"
        }
    }
    
    /**
     * Pantalla de ajustes de la aplicación (tema, idioma, etc.).
     */
    data object Settings : Routes("settings")
}
