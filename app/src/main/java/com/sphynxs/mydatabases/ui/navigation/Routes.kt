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
     */
    data object DatabaseList : Routes("database_list")
    
    /**
     * Pantalla de lista de tablas de una base de datos específica.
     *
     * @property route Template con argumento `databaseName`
     */
    data object TableList : Routes("table_list/{databaseName}") {
        /**
         * Crea la ruta completa reemplazando el argumento databaseName.
         *
         * @param databaseName Nombre de la base de datos
         * @return Ruta navegable (ej: "table_list/my_db")
         */
        fun createRoute(databaseName: String): String {
            return "table_list/$databaseName"
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
     * Pantalla del editor de consultas SQL.
     */
    data object QueryEditor : Routes("query_editor")
    
    /**
     * Pantalla de ajustes de la aplicación (tema, idioma, etc.).
     */
    data object Settings : Routes("settings")
}
