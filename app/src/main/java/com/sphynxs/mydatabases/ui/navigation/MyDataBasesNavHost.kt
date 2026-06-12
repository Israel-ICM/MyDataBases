package com.sphynxs.mydatabases.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sphynxs.mydatabases.ui.screens.connections.ConnectionFormScreen
import com.sphynxs.mydatabases.ui.screens.connections.ConnectionsListScreen
import com.sphynxs.mydatabases.ui.screens.databases.DatabasesListScreen
import com.sphynxs.mydatabases.ui.screens.tables.TablesListScreen
import com.sphynxs.mydatabases.ui.screens.tableviewer.TableViewerScreen

/**
 * NavHost principal de la aplicación.
 *
 * Define todas las rutas y sus pantallas correspondientes.
 * Usa pantallas placeholder temporales para cada ruta.
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@Composable
fun MyDataBasesNavHost() {
    val navController = rememberNavController()
    
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
                    // TODO: navigate to DatabaseList after successful connection
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
        
        composable(Routes.QueryEditor.route) {
            PlaceholderScreen("Query Editor")
        }
        
        composable(Routes.Settings.route) {
            PlaceholderScreen("Settings")
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
