package com.sphynxs.mydatabases.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.sphynxs.mydatabases.R

/**
 * Destino de navegación en el scaffold adaptativo.
 *
 * Representa un ítem en la barra de navegación (BottomBar, Rail, o Drawer).
 *
 * @property id Identificador único del destino (usado para selección)
 * @property labelRes Resource ID del string traducible para el label
 * @property iconRes Resource ID del vector drawable del ícono
 * @property route Ruta de navegación completa (puede incluir connectionId interpolado)
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Immutable
data class NavigationDestination(
    val id: String,
    @StringRes val labelRes: Int,
    val iconRes: Int,
    val route: String,
)

/**
 * Devuelve los destinos de navegación según el contexto actual.
 *
 * **Lógica contextual**:
 * - **OutsideConnection** → 2 destinos: Conexiones, Configuración
 * - **InsideConnection** → 5 destinos: Tablas, Vistas, Editor, Funciones, Backup
 *
 * Los destinos `InsideConnection` interpolan el `connectionId` en la ruta.
 *
 * @param context Contexto de navegación activo
 * @return Lista de destinos visibles para el contexto dado
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val destinations = destinationsForContext(NavigationContext.InsideConnection("abc-123"))
 * // destinations[0].route == "connection/abc-123/tables"
 * // destinations[1].route == "connection/abc-123/views"
 * // ...
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
fun destinationsForContext(context: NavigationContext): List<NavigationDestination> {
    return when (context) {
        is NavigationContext.OutsideConnection -> listOf(
            NavigationDestination(
                id = "connections",
                labelRes = R.string.nav_connections,
                iconRes = R.drawable.ic_nav_connections,
                route = Routes.Connections.route,
            ),
            NavigationDestination(
                id = "settings",
                labelRes = R.string.nav_settings,
                iconRes = R.drawable.ic_nav_settings,
                route = Routes.Settings.route,
            ),
        )
        
        is NavigationContext.InsideConnection -> listOf(
            NavigationDestination(
                id = "tables",
                labelRes = R.string.nav_tables,
                iconRes = R.drawable.ic_nav_tables,
                route = Routes.Tables.createRoute(context.connectionId),
            ),
            NavigationDestination(
                id = "views",
                labelRes = R.string.nav_views,
                iconRes = R.drawable.ic_nav_views,
                route = Routes.Views.createRoute(context.connectionId),
            ),
            NavigationDestination(
                id = "editor",
                labelRes = R.string.nav_editor,
                iconRes = R.drawable.ic_nav_editor,
                route = Routes.QueryEditor.createRoute(context.connectionId),
            ),
            NavigationDestination(
                id = "functions",
                labelRes = R.string.nav_functions,
                iconRes = R.drawable.ic_nav_functions,
                route = Routes.Functions.createRoute(context.connectionId),
            ),
            NavigationDestination(
                id = "backup",
                labelRes = R.string.nav_backup,
                iconRes = R.drawable.ic_nav_backup,
                route = Routes.Backup.createRoute(context.connectionId),
            ),
        )
    }
}
