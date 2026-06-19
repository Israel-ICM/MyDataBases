package com.sphynxs.mydatabases.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sphynxs.mydatabases.R

/**
 * Destino de navegación en el scaffold adaptativo.
 *
 * Representa un ítem en la barra de navegación (BottomBar, Rail, o Drawer).
 *
 * @property id Identificador único del destino (usado para selección)
 * @property labelRes Resource ID del string traducible para el label
 * @property icon ImageVector del ícono (Material Icons Rounded)
 * @property route Ruta de navegación completa (puede incluir connectionId interpolado)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Immutable
data class NavigationDestination(
    val id: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: String,
)

/**
 * Devuelve los destinos de navegación según el contexto actual y el route suffix.
 *
 * **Lógica contextual**:
 * - **OutsideConnection** → 2 destinos: Conexiones, Configuración
 * - **InsideConnection** → bifurca según currentRoute:
 *   - Si `currentRoute?.endsWith("/databases")` → 4 destinos: Add Database, New Query, Monitor, Settings
 *   - Si otro suffix o null → 5 destinos: Tablas, Vistas, Editor, Funciones, Backup
 *
 * Los destinos `InsideConnection` interpolan el `connectionId` en la ruta.
 *
 * @param context Contexto de navegación activo
 * @param currentRoute Ruta activa (opcional) para bifurcar destinos según suffix
 * @return Lista de destinos visibles para el contexto dado
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val destinations = destinationsForContext(
 *     NavigationContext.InsideConnection("abc-123"),
 *     "connection/abc-123/databases"
 * )
 * // destinations.size == 4 (server menu)
 * ```
 *
 * @author israel-icm
 * @date 2026-06-19 (updated para route suffix branching)
 */
fun destinationsForContext(
    context: NavigationContext,
    currentRoute: String? = null
): List<NavigationDestination> {
    return when (context) {
        is NavigationContext.OutsideConnection -> listOf(
            NavigationDestination(
                id = "connections",
                labelRes = R.string.nav_connections,
                icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.connections,
                route = Routes.Connections.route,
            ),
            NavigationDestination(
                id = "settings",
                labelRes = R.string.nav_settings,
                icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.settings,
                route = Routes.Settings.route,
            ),
        )
        
        is NavigationContext.InsideConnection -> {
            if (currentRoute?.endsWith("/databases") == true) {
                // Menú servidor (4 items) cuando estamos en /databases
                destinationsForDatabaseList(context.connectionId)
            } else {
                // Menú DB (5 items) para todos los demás destinos InsideConnection
                destinationsForDatabaseContext(context.connectionId)
            }
        }
    }
}

/**
 * Destinos para el menú servidor (cuando estamos en /databases).
 *
 * @param connectionId ID de la conexión activa
 * @return Lista de 4 destinos: Add Database, New Query, Monitor, Settings
 */
private fun destinationsForDatabaseList(connectionId: String): List<NavigationDestination> {
    return listOf(
        NavigationDestination(
            id = "add_database",
            labelRes = R.string.nav_add_database,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.addDatabase,
            route = Routes.AddDatabase.createRoute(connectionId),
        ),
        NavigationDestination(
            id = "new_query",
            labelRes = R.string.nav_new_query,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.newQuery,
            route = Routes.NewQuery.createRoute(connectionId),
        ),
        NavigationDestination(
            id = "monitor",
            labelRes = R.string.nav_monitor,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.monitor,
            route = Routes.Monitor.createRoute(connectionId),
        ),
        NavigationDestination(
            id = "settings",
            labelRes = R.string.nav_settings,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.settings,
            route = Routes.Settings.route,
        ),
    )
}

/**
 * Destinos para el menú DB (cuando estamos en /tables, /views, etc.).
 *
 * @param connectionId ID de la conexión activa
 * @return Lista de 5 destinos: Tablas, Vistas, Editor, Funciones, Backup
 */
private fun destinationsForDatabaseContext(connectionId: String): List<NavigationDestination> {
    return listOf(
        NavigationDestination(
            id = "tables",
            labelRes = R.string.nav_tables,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.tables,
            route = Routes.Tables.createRoute(connectionId),
        ),
        NavigationDestination(
            id = "views",
            labelRes = R.string.nav_views,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.views,
            route = Routes.Views.createRoute(connectionId),
        ),
        NavigationDestination(
            id = "editor",
            labelRes = R.string.nav_editor,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.editor,
            route = Routes.QueryEditor.createRoute(connectionId),
        ),
        NavigationDestination(
            id = "functions",
            labelRes = R.string.nav_functions,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.functions,
            route = Routes.Functions.createRoute(connectionId),
        ),
        NavigationDestination(
            id = "backup",
            labelRes = R.string.nav_backup,
            icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.backup,
            route = Routes.Backup.createRoute(connectionId),
        ),
    )
}
