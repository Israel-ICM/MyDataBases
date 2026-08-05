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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    
    // Estado para controlar el sheet de crear tabla (change `create-table`)
    var showAddTableSheet by remember { mutableStateOf(false) }

    // Estado para el selector "¿Qué quieres hacer?" de New Query (change `large-sql-script-execution`)
    var showNewQueryOptionsSheet by remember { mutableStateOf(false) }
    var pendingScriptUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val contentResolver = LocalContext.current.contentResolver

    // Deriva connectionId del contexto actual — reusado por ambos pickers de abajo y por
    // el callback onNewQuery del sheet.
    val activeConnectionId = when (navigationContext) {
        is NavigationContext.InsideConnection -> navigationContext.connectionId
        else -> ""
    }

    // Fase 17: picker para "Open Query File" — decide editor vs. redirect a Run Script
    // según LineThresholdGuard (NUNCA readText() para el chequeo en sí).
    val openQueryFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val exceedsThreshold = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                        com.sphynxs.mydatabases.domain.sql.LineThresholdGuard.exceedsThreshold(reader)
                    } ?: false
                }
                if (exceedsThreshold) {
                    pendingScriptUri = uri
                    navController.navigate(Routes.RunScript.createRoute(activeConnectionId))
                } else {
                    val content = withContext(Dispatchers.IO) {
                        contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    workspaceManager.openQueryCard(connectionId = activeConnectionId, initialSql = content)
                }
            }
        }
    }

    // Picker para "Run Script (No Edit)" — siempre redirige a Run Script, sin chequear tamaño.
    val runScriptLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingScriptUri = uri
            navController.navigate(Routes.RunScript.createRoute(activeConnectionId))
        }
    }
    
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
                    "new_table" -> showAddTableSheet = true
                    "new_query" -> {
                        // Cambio `large-sql-script-execution`: en vez de abrir directo una
                        // query en blanco, mostrar el selector "¿Qué quieres hacer?" con 3
                        // opciones. La acción previa (openQueryCard directo) ahora vive en
                        // el callback onNewQuery del sheet.
                        showNewQueryOptionsSheet = true
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
                    onNavigateToDatabaseMenu = { databaseName ->
                        navController.navigate(Routes.DatabaseActionMenu.createRoute(connectionId, databaseName))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    showAddDatabaseSheet = showAddDatabaseSheet,
                    onDismissAddDatabaseSheet = {
                        showAddDatabaseSheet = false
                    }
                )
            }

            composable(
                route = Routes.DatabaseActionMenu.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("databaseName") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                com.sphynxs.mydatabases.ui.screens.databases.DatabaseActionMenuScreen(
                    databaseName = databaseName,
                    onNavigateToTables = {
                        navController.navigate(Routes.TableList.createRoute(connectionId, databaseName))
                    },
                    onNavigateToViews = {
                        navController.navigate(Routes.DatabaseViews.createRoute(connectionId, databaseName))
                    },
                    onNavigateToQueries = {
                        // Corrección post-QA (change `large-sql-script-execution`): este era el
                        // segundo punto de entrada a "New Query", dejado fuera de alcance por
                        // error en el diseño original — el usuario esperaba (correctamente) que
                        // ambos puntos de entrada mostraran el mismo selector de 3 opciones.
                        showNewQueryOptionsSheet = true
                    },
                    onNavigateToFunctions = {
                        navController.navigate(Routes.DatabaseFunctions.createRoute(connectionId, databaseName))
                    },
                    onNavigateToAutomations = {
                        navController.navigate(Routes.DatabaseAutomations.createRoute(connectionId, databaseName))
                    },
                    onNavigateToBackups = {
                        navController.navigate(Routes.DatabaseBackups.createRoute(connectionId, databaseName))
                    },
                    onNavigateBack = { navController.popBackStack() }
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
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("databaseName") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                TablesListScreen(
                    databaseName = databaseName,
                    connectionId = connectionId,
                    onNavigateToTableViewer = { tableName ->
                        navController.navigate(Routes.TableViewer.createRoute(databaseName, tableName))
                    },
                    onNavigateBack = { navController.popBackStack() },
                    workspaceManager = workspaceManager,
                    showAddTableSheet = showAddTableSheet,
                    onDismissAddTableSheet = {
                        showAddTableSheet = false
                    }
                )
            }

            // --- Placeholders del menú de acciones de base de datos (Vistas, Funciones,
            // Automatizaciones, Backups) — cada uno scoped por connectionId + databaseName.
            // TODO: reemplazar por pantallas reales cuando estén implementadas.

            composable(
                route = Routes.DatabaseViews.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("databaseName") { type = NavType.StringType }
                )
            ) {
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                PlaceholderScreen("Views — $databaseName")
            }

            composable(
                route = Routes.DatabaseFunctions.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("databaseName") { type = NavType.StringType }
                )
            ) {
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                PlaceholderScreen("Functions — $databaseName")
            }

            composable(
                route = Routes.DatabaseAutomations.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("databaseName") { type = NavType.StringType }
                )
            ) {
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                PlaceholderScreen("Automations — $databaseName")
            }

            composable(
                route = Routes.DatabaseBackups.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType },
                    navArgument("databaseName") { type = NavType.StringType }
                )
            ) {
                val databaseName = it.arguments?.getString("databaseName") ?: ""
                PlaceholderScreen("Backups — $databaseName")
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

            // Cambio `large-sql-script-execution` (amendment): destino de "Run Script".
            // El Uri viaja por estado hoisted (pendingScriptUri), no como argumento de ruta.
            composable(
                route = Routes.RunScript.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) {
                val connectionId = it.arguments?.getString("connectionId") ?: ""
                val uri = pendingScriptUri
                if (uri != null) {
                    com.sphynxs.mydatabases.ui.screens.runscript.RunScriptScreen(
                        uri = uri,
                        connectionId = connectionId,
                        onFinished = { navController.popBackStack() }
                    )
                }
            }
        }
        } // Cierre AdaptiveNavigationScaffold

        // Cambio `large-sql-script-execution` (amendment): selector "¿Qué quieres hacer?" para
        // New Query. Renderizado como sibling de AdaptiveNavigationScaffold (no threadeado en
        // DatabasesListScreen/TablesListScreen) porque "new_query" es una acción modal declarada
        // en AMBOS menús (destinationsForDatabaseList y destinationsForTablesList) — anidarlo en
        // uno solo rompería el otro. Esto preserva la garantía de `2026-06-30-new-query-modal-fix`
        // (sin cambio de ruta, sin doble sheet) porque sigue siendo un overlay, no una navegación.
        if (showNewQueryOptionsSheet) {
            com.sphynxs.mydatabases.ui.screens.databases.NewQueryOptionsSheet(
                onNewQuery = {
                    workspaceManager.openQueryCard(connectionId = activeConnectionId, initialSql = null)
                },
                onOpenQueryFile = { openQueryFileLauncher.launch("*/*") },
                onRunScript = { runScriptLauncher.launch("*/*") },
                onDismiss = { showNewQueryOptionsSheet = false }
            )
        }
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
