package com.sphynxs.mydatabases.ui.adaptive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.ui.navigation.NavigationContext
import com.sphynxs.mydatabases.ui.navigation.NavigationDestination
import com.sphynxs.mydatabases.ui.navigation.destinationsForContext

/**
 * Scaffold adaptativo que switchea entre BottomBar, Rail, y Drawer según WindowSizeClass.
 *
 * ## Comportamiento Adaptativo
 *
 * - **Compact** (< 600dp): `NavigationBar` en la parte inferior (2 o 5 destinos)
 * - **Medium** (600-840dp): `NavigationRail` en la parte izquierda (2 o 5 destinos)
 * - **Expanded** (> 840dp): `PermanentNavigationDrawer` a la izquierda (2 o 5 destinos)
 *
 * ## Destinos Contextuales
 *
 * Los destinos visibles se derivan del `navigationContext`:
 * - **OutsideConnection**: 2 destinos (Conexiones, Configuración)
 * - **InsideConnection**: 5 destinos (Tablas, Vistas, Editor, Funciones, Backup)
 *
 * @param windowSizeClass WindowSizeClass actual del dispositivo
 * @param navigationContext Contexto derivado desde NavBackStackEntry
 * @param currentRoute Ruta activa para destacar el destino seleccionado
 * @param onNavigate Callback para navegar al seleccionar un destino (recibe route completo)
 * @param content Contenido principal (NavHost con las pantallas)
 *
 * ## Ejemplo
 *
 * ```kotlin
 * val windowSizeClass = calculateWindowSizeClass(activity)
 * val currentBackStackEntry by navController.currentBackStackEntryAsState()
 * val navigationContext = remember(currentBackStackEntry) {
 *     NavigationContext.from(currentBackStackEntry?.destination?.route)
 * }
 *
 * AdaptiveNavigationScaffold(
 *     windowSizeClass = windowSizeClass,
 *     navigationContext = navigationContext,
 *     currentRoute = currentBackStackEntry?.destination?.route,
 *     onNavigate = { route -> navController.navigate(route) }
 * ) {
 *     NavHost(...) { ... }
 * }
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15
 */
@Composable
fun AdaptiveNavigationScaffold(
    windowSizeClass: WindowSizeClass,
    navigationContext: NavigationContext,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val destinations = destinationsForContext(navigationContext)
    
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Compact: BottomNavigationBar
            Column(modifier = Modifier.fillMaxSize()) {
                // Contenido principal ocupa todo el espacio disponible
                Column(modifier = Modifier.weight(1f)) {
                    content()
                }
                
                // BottomNavigationBar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { onNavigate(destination.route) },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.iconRes),
                                    contentDescription = stringResource(destination.labelRes)
                                )
                            },
                            label = {
                                Text(text = stringResource(destination.labelRes))
                            }
                        )
                    }
                }
            }
        }
        
        WindowWidthSizeClass.Medium -> {
            // Medium: NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                // NavigationRail a la izquierda
                NavigationRail {
                    destinations.forEach { destination ->
                        NavigationRailItem(
                            selected = currentRoute == destination.route,
                            onClick = { onNavigate(destination.route) },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.iconRes),
                                    contentDescription = stringResource(destination.labelRes)
                                )
                            },
                            label = {
                                Text(text = stringResource(destination.labelRes))
                            }
                        )
                    }
                }
                
                // Contenido principal
                content()
            }
        }
        
        WindowWidthSizeClass.Expanded -> {
            // Expanded: PermanentNavigationDrawer
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet {
                        destinations.forEach { destination ->
                            NavigationDrawerItem(
                                label = {
                                    Text(text = stringResource(destination.labelRes))
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(destination.iconRes),
                                        contentDescription = stringResource(destination.labelRes)
                                    )
                                },
                                selected = currentRoute == destination.route,
                                onClick = { onNavigate(destination.route) }
                            )
                        }
                    }
                }
            ) {
                // Contenido principal
                content()
            }
        }
        
        else -> {
            // Fallback seguro: sin navegación (solo contenido)
            content()
        }
    }
}
