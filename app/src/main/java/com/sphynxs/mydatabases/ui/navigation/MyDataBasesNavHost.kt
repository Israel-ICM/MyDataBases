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
            PlaceholderScreen("Connections")
        }
        
        composable(Routes.DatabaseList.route) {
            PlaceholderScreen("Database List")
        }
        
        composable(
            route = Routes.TableList.route,
            arguments = listOf(
                navArgument("databaseName") { type = NavType.StringType }
            )
        ) {
            val databaseName = it.arguments?.getString("databaseName") ?: ""
            PlaceholderScreen("Table List: $databaseName")
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
            PlaceholderScreen("Table Viewer: $databaseName.$tableName")
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
