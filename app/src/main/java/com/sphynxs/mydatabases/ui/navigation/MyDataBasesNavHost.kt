package com.sphynxs.mydatabases.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sphynxs.mydatabases.LocalWindowSizeClass
import com.sphynxs.mydatabases.ui.adaptive.AdaptiveNavigationScaffold
import com.sphynxs.mydatabases.ui.screens.connections.ConnectionFormScreen
import com.sphynxs.mydatabases.ui.screens.connections.ConnectionsListScreen
import com.sphynxs.mydatabases.ui.screens.databases.DatabasesListScreen
import com.sphynxs.mydatabases.ui.screens.settings.SettingsScreen
import com.sphynxs.mydatabases.ui.screens.tables.TablesListScreen
import com.sphynxs.mydatabases.ui.screens.tableviewer.TableViewerScreen

/**
 * NavHost principal de la aplicación envuelto con AdaptiveNavigationScaffold.
 *
 * ## Integración Adaptativa (PR 4b)
 *
 * - Lee `WindowSizeClass` desde `LocalWindowSizeClass` (provisto por MainActivity)
 * - Deriva `NavigationContext` desde el route activo (pure derivation sin estado paralelo)
 * - Envuelve el NavHost con `AdaptiveNavigationScaffold` que switchea BottomBar/Rail/Drawer
 * - Destinos contextuales: 2 fuera de conexión (Conexiones, Configuración) / 5 dentro (Tablas, Vistas, Editor, Funciones, Backup)
 *
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-15 para PR 4b)
 */
@Composable
fun MyDataBasesNavHost() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    // Derivar NavigationContext desde el route activo (pure derivation, zero state paralelo)
    val navigationContext = remember(currentBackStackEntry) {
        NavigationContext.from(currentBackStackEntry?.destination?.route)
    }
    
    // Obtener WindowSizeClass desde CompositionLocal provisto por MainActivity
    val windowSizeClass = LocalWindowSizeClass.current
        ?: throw IllegalStateException("WindowSizeClass no disponible — MainActivity debe proveerlo vía LocalWindowSizeClass")
    
    AdaptiveNavigationScaffold(
        windowSizeClass = windowSizeClass,
        navigationContext = navigationContext,
        currentRoute = currentBackStackEntry?.destination?.route,
        onNavigate = { route ->
            navController.navigate(route) {
                // Evitar múltiples copias de la misma pantalla en el back stack
                launchSingleTop = true
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.Connections.route
        ) {
            composable(Routes.Connections.route) {
                ConnectionsListScreen(
                    onNavigateToForm = { connectionId ->
                        if (connectionId == null) {
                            navController.navigate(Routes.ConnectionForm.route)
                        } else {
                            navController.navigate("${Routes.ConnectionForm.route}?connectionId=$connectionId")
                        }
                    },
                    onConnect = { connectionId ->
                        // TODO: navegar a la nueva ruta contextual connection/{id}/tables
                        // Por ahora usa la ruta legacy database_list
                        navController.navigate(Routes.DatabaseList.route)
                    }
                )
            }
            
            composable(
                route = "${Routes.ConnectionForm.route}?connectionId={connectionId}",
                arguments = listOf(
                    navArgument("connectionId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val connectionId = backStackEntry.arguments?.getString("connectionId")
                ConnectionFormScreen(
                    connectionId = connectionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable(Routes.DatabaseList.route) {
                DatabasesListScreen(
                    onNavigateToTables = { databaseName ->
                        navController.navigate("tables/$databaseName")
                    }
                )
            }
            
            composable(
                route = Routes.TableList.route,
                arguments = listOf(
                    navArgument("databaseName") { type = NavType.StringType }
                )
            ) {
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                TablesListScreen(
                    databaseName = databaseName,
                    onNavigateToTableViewer = { tableName ->
                        navController.navigate("table_viewer/$databaseName/$tableName")
                    }
                )
            }
            
            composable(
                route = Routes.TableViewer.route,
                arguments = listOf(
                    navArgument("databaseName") { type = NavType.StringType },
                    navArgument("tableName") { type = NavType.StringType }
                )
            ) {
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                val tableName = it.arguments?.getString("tableName") ?: ""
                TableViewerScreen(
                    databaseName = databaseName,
                    tableName = tableName
                )
            }
            
            // --- NUEVAS RUTAS CONTEXTUALES (PR 4b) ---
            
            composable(
                route = Routes.Tables.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                // TODO: implementar TablesScreen contextual (por ahora placeholder)
                PlaceholderScreen("Tables — Connection: $connectionId")
            }
            
            composable(
                route = Routes.Views.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                PlaceholderScreen("Views — Connection: $connectionId")
            }
            
            composable(
                route = Routes.QueryEditor.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                PlaceholderScreen("Editor — Connection: $connectionId")
            }
            
            composable(
                route = Routes.Functions.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                PlaceholderScreen("Functions — Connection: $connectionId")
            }
            
            composable(
                route = Routes.Backup.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                PlaceholderScreen("Backup — Connection: $connectionId")
            }
            
            composable(Routes.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

/**
 * Pantalla placeholder temporal para desarrollo.
 *
 * Muestra el título de la pantalla centrado.
 *
 * @param title Título de la pantalla
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
