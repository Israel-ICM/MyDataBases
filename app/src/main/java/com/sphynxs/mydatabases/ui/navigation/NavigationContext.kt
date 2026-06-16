package com.sphynxs.mydatabases.ui.navigation

/**
 * Contexto de navegación derivado puramente desde el route activo.
 *
 * **Sin estado paralelo** — se deriva directamente desde `currentBackStackEntry.destination.route`
 * en tiempo de composición, garantizando sincronización perfecta con NavController.
 *
 * ## Tipos de Contexto
 *
 * - **OutsideConnection**: Usuario fuera de una conexión activa (pantallas: Conexiones, Configuración)
 * - **InsideConnection**: Usuario dentro de una conexión activa (pantallas: Tablas, Vistas, Editor, Funciones, Backup)
 *
 * ## Derivación
 *
 * La función `from(route)` parsea el route actual usando regex para extraer el `connectionId` si existe.
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val context = remember(currentBackStackEntry) {
 *     NavigationContext.from(currentBackStackEntry?.destination?.route)
 * }
 * // route "connection/abc-123/tables" → InsideConnection("abc-123")
 * // route "connections" → OutsideConnection
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
sealed class NavigationContext {
    /**
     * Usuario fuera de una conexión activa.
     *
     * Destinos visibles: Conexiones, Configuración.
     */
    data object OutsideConnection : NavigationContext()
    
    /**
     * Usuario dentro de una conexión activa.
     *
     * Destinos visibles: Tablas, Vistas, Editor, Funciones, Backup.
     *
     * @property connectionId ID de la conexión activa (extraído del route)
     */
    data class InsideConnection(val connectionId: String) : NavigationContext()
    
    companion object {
        /**
         * Regex para extraer connectionId de rutas tipo "connection/{id}/..."
         *
         * Captura el primer segmento después de "connection/".
         */
        private val connectionRouteRegex = Regex("^connection/([^/]+)/.+")
        
        /**
         * Deriva el contexto de navegación desde el route activo.
         *
         * **Derivación pura** — sin efectos colaterales, sin estado mutable.
         *
         * @param route Route del NavBackStackEntry actual (puede ser null si no hay stack)
         * @return Contexto derivado
         *
         * ## Lógica de Derivación
         *
         * 1. Si `route == null` → **OutsideConnection** (default seguro)
         * 2. Si `route` matchea `"connection/{id}/.*"` → **InsideConnection(id)**
         * 3. Si `route` es `"connections"`, `"settings"`, o cualquier otra → **OutsideConnection**
         *
         * ## Ejemplos
         *
         * ```kotlin
         * from("connection/abc-123/tables") == InsideConnection("abc-123")
         * from("connection/xyz-789/views") == InsideConnection("xyz-789")
         * from("connections") == OutsideConnection
         * from("settings") == OutsideConnection
         * from(null) == OutsideConnection
         * ```
         */
        fun from(route: String?): NavigationContext {
            if (route == null) return OutsideConnection
            
            // Intentar matchear ruta de conexión activa
            val match = connectionRouteRegex.find(route)
            
            return if (match != null) {
                // Extraer connectionId desde grupo de captura (groupValues[0] es el match completo, [1] es el grupo)
                InsideConnection(connectionId = match.groupValues[1])
            } else {
                // Ruta sin connectionId → OutsideConnection
                OutsideConnection
            }
        }
    }
}
