package com.sphynxs.mydatabases.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.graphics.Brush
import com.skydoves.cloudy.cloudy
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
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

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
 * @param onModalAction Callback para acciones modales (ej: abrir bottom sheet) - recibe el id del destino
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
 *     onNavigate = { route -> navController.navigate(route) },
 *     onModalAction = { id -> /* handle modal */ }
 * ) {
 *     NavHost(...) { ... }
 * }
 * ```
 *
 * @author israel-icm
 * @date 2026-06-15 (updated 2026-06-19 para modal actions)
 */
@Composable
fun AdaptiveNavigationScaffold(
    windowSizeClass: WindowSizeClass,
    navigationContext: NavigationContext,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onModalAction: (String) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val destinations = destinationsForContext(navigationContext, currentRoute)
    
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
                        onModalAction = onModalAction,
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
                                    imageVector = destination.icon,
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
                                        imageVector = destination.icon,
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
 * @param onNavigate Callback al seleccionar un destino (navegación normal)
 * @param onModalAction Callback para acciones modales (ej: abrir sheet)
 */
@Composable
private fun LiquidGlassBottomBar(
    destinations: List<NavigationDestination>,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    onModalAction: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = LocalDesignTokens.current.screenPaddingHorizontal, end = LocalDesignTokens.current.screenPaddingHorizontal, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Bottom bar estilo iOS unificado (GitHub mobile style - pill completo + backdrop
        // translúcido). Antes usaba Color.White/Color.Black fijos — en dark mode eso
        // pintaba una pill BLANCA brillante sobre fondo oscuro y una sombra negra
        // invisible; ahora deriva de MaterialTheme.colorScheme (mismo criterio "R6" que
        // WorkspaceCarousel/TopSheet — ver design.md).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(40.dp),
                    ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(40.dp))
                .background(LocalDesignTokens.current.surfacePrimary.copy(alpha = 0.75f))  // Superficie semi-transparente (el contenido de atrás ya tiene blur)
        ) {
            // Separator superior sutil (divide visualmente del contenido de arriba)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))  // línea ultra sutil, adapta con el tema
                    .align(Alignment.TopCenter)
            )

            // Contenido: botones de navegación estilo GitHub mobile
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    val isSelected = selectedRoute == destination.route

                    // Item seleccionado: ícono + texto en pill
                    // Item NO seleccionado: solo ícono
                    if (isSelected) {
                        // Selected: pill con ícono + texto
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .clickable {
                                    if (destination.isModal) {
                                        onModalAction(destination.id)
                                    } else {
                                        onNavigate(destination.route)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(destination.labelRes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Not selected: solo ícono
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (destination.isModal) {
                                        onModalAction(destination.id)
                                    } else {
                                        onNavigate(destination.route)
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes),
                                modifier = Modifier.size(24.dp),
                                tint = LocalDesignTokens.current.iconNormal
                            )
                        }
                    }
                }
            }
        }
    }
}
