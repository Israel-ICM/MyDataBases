package com.sphynxs.mydatabases.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.sphynxs.mydatabases.ui.navigation.NavigationContext
import com.sphynxs.mydatabases.ui.navigation.NavigationDestination
import com.sphynxs.mydatabases.ui.navigation.Routes
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
            // Compact: contenido full screen + card flotando al fondo
            Box(modifier = Modifier.fillMaxSize()) {
                // Contenido principal ocupa toda la pantalla
                content()

                // Mostrar menú solo en pantallas "dentro" de la app
                val showMenu = currentRoute != null &&
                    currentRoute != Routes.Connections.route &&
                    currentRoute != Routes.Settings.route &&
                    !currentRoute.startsWith("connection_form")

                AnimatedVisibility(
                    visible = showMenu,
                    enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
                    exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    LiquidGlassBottomBar(
                        destinations = destinations,
                        selectedRoute = currentRoute,
                        onNavigate = onNavigate,
                    )
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

/**
 * Bottom card con estilo Liquid Glass (Apple).
 *
 * Card flotante con efecto frosted glass, bordes redondeados y sombra.
 * Simula el glassmorphism de iOS sin costo de blur en tiempo real.
 *
 * @param destinations Destinos de navegación a mostrar
 * @param selectedRoute Ruta actualmente seleccionada
 * @param onNavigate Callback al seleccionar un destino
 */
@Composable
private fun LiquidGlassBottomBar(
    destinations: List<NavigationDestination>,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Card con glass effect: Box + shadow directo (sin Surface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF5F5F7).copy(alpha = 0.80f))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.05f),
                        )
                    )
                )
        ) {
            // Borde highlight superior (glass edge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.5f))
                    .align(Alignment.TopCenter)
            )

            // Contenido: botones de navegación
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    val isSelected = selectedRoute == destination.route

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    )
                                } else Modifier
                            )
                            .clickable { onNavigate(destination.route) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = stringResource(destination.labelRes),
                            modifier = Modifier.size(26.dp),
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color(0xFF8E8E93)
                        )
                        Text(
                            text = stringResource(destination.labelRes),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }
    }
}
