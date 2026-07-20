package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.BreathingBackground
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons
import com.sphynxs.mydatabases.ui.components.ScreenTitle
import com.sphynxs.mydatabases.ui.components.ios.IOSCard
import com.sphynxs.mydatabases.ui.theme.AppTheme
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens

/**
 * Pantalla de menú de acciones tras seleccionar una base de datos ("¿Qué quieres hacer?").
 *
 * Punto de entrada intermedio entre [com.sphynxs.mydatabases.ui.screens.databases.DatabasesListScreen]
 * y las distintas secciones de una base de datos concreta. Muestra una grilla de 2 columnas
 * con 6 tiles ([IOSCard]): Tablas, Vistas, Query's, Funciones, Automatizaciones y Backups.
 *
 * @param databaseName Nombre de la base de datos seleccionada (mostrado como subtítulo)
 * @param onNavigateToTables Callback al seleccionar la opción "Tablas"
 * @param onNavigateToViews Callback al seleccionar la opción "Vistas"
 * @param onNavigateToQueries Callback al seleccionar la opción "Query's"
 * @param onNavigateToFunctions Callback al seleccionar la opción "Funciones"
 * @param onNavigateToAutomations Callback al seleccionar la opción "Automatizaciones"
 * @param onNavigateToBackups Callback al seleccionar la opción "Backups"
 * @param onNavigateBack Callback para el botón de retroceso
 * @param modifier Modificador opcional
 *
 * @author gentle-ai
 * @date 2026-07-20
 */
@Composable
fun DatabaseActionMenuScreen(
    databaseName: String,
    onNavigateToTables: () -> Unit,
    onNavigateToViews: () -> Unit,
    onNavigateToQueries: () -> Unit,
    onNavigateToFunctions: () -> Unit,
    onNavigateToAutomations: () -> Unit,
    onNavigateToBackups: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { paddingValues ->
        BreathingBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(16.dp))

                ScreenTitle(
                    title = stringResource(R.string.database_action_menu_title),
                    subtitle = databaseName,
                    onBackClick = onNavigateBack
                )

                Spacer(modifier = Modifier.height(LocalDesignTokens.current.sectionSpacing))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = LocalDesignTokens.current.screenPaddingHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(LocalDesignTokens.current.cardSpacing),
                    verticalArrangement = Arrangement.spacedBy(LocalDesignTokens.current.cardSpacing)
                ) {
                    items(
                        items = databaseActionMenuItems(
                            onNavigateToTables = onNavigateToTables,
                            onNavigateToViews = onNavigateToViews,
                            onNavigateToQueries = onNavigateToQueries,
                            onNavigateToFunctions = onNavigateToFunctions,
                            onNavigateToAutomations = onNavigateToAutomations,
                            onNavigateToBackups = onNavigateToBackups
                        ),
                        key = { it.labelRes }
                    ) { item ->
                        DatabaseActionMenuTile(
                            icon = item.icon,
                            label = stringResource(item.labelRes),
                            onClick = item.onClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Entrada de la grilla del menú de acciones de base de datos.
 *
 * @property icon Ícono de la opción
 * @property labelRes Resource ID del label traducible
 * @property onClick Callback al tocar el tile
 */
private data class DatabaseActionMenuItem(
    val icon: ImageVector,
    val labelRes: Int,
    val onClick: () -> Unit
)

/**
 * Construye la lista de 6 opciones del menú, en el orden fijo requerido:
 * Tablas, Vistas, Query's, Funciones, Automatizaciones, Backups.
 */
private fun databaseActionMenuItems(
    onNavigateToTables: () -> Unit,
    onNavigateToViews: () -> Unit,
    onNavigateToQueries: () -> Unit,
    onNavigateToFunctions: () -> Unit,
    onNavigateToAutomations: () -> Unit,
    onNavigateToBackups: () -> Unit
): List<DatabaseActionMenuItem> = listOf(
    DatabaseActionMenuItem(
        icon = PhosphorAppIcons.Nav.tables,
        labelRes = R.string.nav_tables,
        onClick = onNavigateToTables
    ),
    DatabaseActionMenuItem(
        icon = PhosphorAppIcons.Nav.views,
        labelRes = R.string.nav_views,
        onClick = onNavigateToViews
    ),
    DatabaseActionMenuItem(
        icon = PhosphorAppIcons.Nav.newQuery,
        labelRes = R.string.database_action_queries,
        onClick = onNavigateToQueries
    ),
    DatabaseActionMenuItem(
        icon = PhosphorAppIcons.Nav.functions,
        labelRes = R.string.nav_functions,
        onClick = onNavigateToFunctions
    ),
    DatabaseActionMenuItem(
        icon = PhosphorAppIcons.Nav.automations,
        labelRes = R.string.database_action_automations,
        onClick = onNavigateToAutomations
    ),
    DatabaseActionMenuItem(
        icon = PhosphorAppIcons.Nav.backup,
        labelRes = R.string.nav_backup,
        onClick = onNavigateToBackups
    ),
)

/**
 * Tile individual del grid del menú de acciones: ícono centrado + label debajo,
 * usando [IOSCard] como building block reusable (mismo lenguaje visual de
 * sombra/borde-redondeado que el resto de la app).
 *
 * @param icon Ícono a mostrar
 * @param label Texto del label (ya traducido)
 * @param onClick Callback al tocar el tile
 * @param modifier Modificador opcional
 */
@Composable
private fun DatabaseActionMenuTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IOSCard(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalDesignTokens.current.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LocalDesignTokens.current.accentPrimary,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(LocalDesignTokens.current.innerSpacing))

            Text(
                text = label,
                fontSize = LocalDesignTokens.current.cardTitleSize,
                fontWeight = LocalDesignTokens.current.cardTitleWeight,
                color = LocalDesignTokens.current.cardTitleColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Preview de [DatabaseActionMenuScreen].
 */
@Preview(showBackground = true)
@Composable
private fun DatabaseActionMenuScreenPreview() {
    AppTheme {
        DatabaseActionMenuScreen(
            databaseName = "my_database",
            onNavigateToTables = {},
            onNavigateToViews = {},
            onNavigateToQueries = {},
            onNavigateToFunctions = {},
            onNavigateToAutomations = {},
            onNavigateToBackups = {},
            onNavigateBack = {}
        )
    }
}
