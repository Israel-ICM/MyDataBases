package com.sphynxs.mydatabases.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.sphynxs.mydatabases.ui.workspace.WorkspaceManager
import javax.inject.Inject

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
fun MyDataBasesNavHost(
    workspaceManager: WorkspaceManager
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    
    // Derivar NavigationContext desde el route activo (pure derivation, zero state paralelo)
    val navigationContext = remember(currentBackStackEntry) {
        NavigationContext.from(currentBackStackEntry?.destination?.route)
    }
    
    // Estado para controlar el sheet de agregar database
    var showAddDatabaseSheet by remember { mutableStateOf(false) }
    
    // Obtener WindowSizeClass desde CompositionLocal provisto por MainActivity
    val windowSizeClass = LocalWindowSizeClass.current
        ?: throw IllegalStateException("WindowSizeClass no disponible — MainActivity debe proveerlo vía LocalWindowSizeClass")
    
    // WorkspaceOverlay envuelve TODO - Scaffold adaptativo + NavHost
    com.sphynxs.mydatabases.ui.workspace.WorkspaceOverlay(
        workspaceManager = workspaceManager,
        modifier = Modifier.fillMaxSize()
    ) {
        AdaptiveNavigationScaffold(
            windowSizeClass = windowSizeClass,
            navigationContext = navigationContext,
            currentRoute = currentBackStackEntry?.destination?.route,
            onNavigate = { route ->
                navController.navigate(route) {
                    // Evitar múltiples copias de la misma pantalla en el back stack
                    launchSingleTop = true
                }
            },
            onModalAction = { destinationId ->
                when (destinationId) {
                    "add_database" -> showAddDatabaseSheet = true
                    "new_query" -> {
                        // Extraer connectionId del contexto actual
                        val connectionId = when (navigationContext) {
                            is NavigationContext.InsideConnection -> navigationContext.connectionId
                            else -> ""
                        }
                        // WorkspaceManager maneja su propio sheet/overlay
                        workspaceManager.openQueryCard(
                            connectionId = connectionId,
                            initialSql = null
                        )
                    }
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
                        // Navegar a la nueva ruta contextual connection/{id}/databases
                        navController.navigate(Routes.Databases.createRoute(connectionId))
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
            
            composable(
                route = Routes.Databases.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                DatabasesListScreen(
                    connectionId = connectionId,
                    onNavigateToTables = { databaseName ->
                        navController.navigate(Routes.TableList.createRoute(databaseName))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    showAddDatabaseSheet = showAddDatabaseSheet,
                    onDismissAddDatabaseSheet = {
                        showAddDatabaseSheet = false
                    }
                )
            }
            
            composable(
                route = Routes.NewQuery.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                com.sphynxs.mydatabases.ui.screens.databases.NewQueryScreen(
                    connectionId = connectionId,
                    workspaceManager = workspaceManager
                )
            }
            
            composable(
                route = Routes.Monitor.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                com.sphynxs.mydatabases.ui.screens.databases.MonitorScreen(
                    connectionId = connectionId,
                    onNavigateBack = { navController.popBackStack() }
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
                        navController.navigate(Routes.TableViewer.createRoute(databaseName, tableName))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    workspaceManager = workspaceManager
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
                    tableName = tableName,
                    onNavigateBack = { navController.popBackStack() }
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
                SettingsScreen(
                    onNavigateBack = if (navController.previousBackStackEntry != null) {
                        { navController.popBackStack() }
                    } else {
                        null
                    }
                )
            }
        }
        } // Cierre AdaptiveNavigationScaffold
    } // Cierre WorkspaceOverlay
}

/**
 * Pantalla placeholder para rutas en desarrollo.
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
