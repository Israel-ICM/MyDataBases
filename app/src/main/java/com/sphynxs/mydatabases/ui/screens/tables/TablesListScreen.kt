package com.sphynxs.mydatabases.ui.screens.tables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.AppIcons
import com.sphynxs.mydatabases.ui.components.BreathingBackground
import com.sphynxs.mydatabases.ui.components.EmptyState
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.LoadingIndicator
import com.sphynxs.mydatabases.ui.components.ScreenTitle
import com.sphynxs.mydatabases.ui.components.TableCard
import com.sphynxs.mydatabases.ui.components.ios.IOSSearchBar
import com.sphynxs.mydatabases.ui.components.skeleton.TableListSkeleton
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import com.sphynxs.mydatabases.ui.theme.AppTheme
import com.sphynxs.mydatabases.ui.workspace.WorkspaceCard
import com.sphynxs.mydatabases.ui.workspace.WorkspaceManager
import kotlinx.coroutines.launch

/**
 * Pantalla de lista de tablas.
 *
 * Muestra todas las tablas disponibles en una base de datos seleccionada.
 * Al seleccionar una tabla, navega al visor de tabla.
 *
 * @param databaseName Nombre de la base de datos
 * @param connectionId ID de la conexión activa (change `create-table`: reemplaza el
 *   `connectionId = "current"` hardcodeado previo, ver design.md)
 * @param onNavigateToTableViewer Callback para navegar al visor de tabla
 * @param showAddTableSheet Si se debe mostrar el sheet de crear tabla (controlado externamente)
 * @param onDismissAddTableSheet Callback cuando se cierra el sheet
 * @param viewModel El ViewModel con la lógica de estado
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-07-15 para sheet de crear tabla, change `create-table`)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesListScreen(
    databaseName: String,
    connectionId: String,
    onNavigateToTableViewer: (tableName: String) -> Unit,
    onNavigateBack: () -> Unit,
    workspaceManager: WorkspaceManager,
    showAddTableSheet: Boolean = false,
    onDismissAddTableSheet: () -> Unit = {},
    viewModel: TablesListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estado del bottom sheet de crear tabla
    val addTableSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Altura real del status bar (incluye notch/cutout) — mirrors DatabasesListScreen
    val statusBarHeightDp = with(LocalDensity.current) {
        LocalContext.current.resources
            .getIdentifier("status_bar_height", "dimen", "android")
            .takeIf { it > 0 }
            ?.let { resourceId ->
                LocalContext.current.resources.getDimensionPixelSize(resourceId).toDp()
            } ?: 24.dp
    }

    // Cargar tables al montar la pantalla
    LaunchedEffect(databaseName) {
        viewModel.loadTables(databaseName)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        BreathingBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Título grande estilo iOS con botón de retroceso
                ScreenTitle(
                    title = "Tables",
                    subtitle = databaseName,
                    onBackClick = onNavigateBack
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (uiState) {
                    is TablesUiState.Loading -> {
                        TableListSkeleton(modifier = Modifier.weight(1f))
                    }

                    is TablesUiState.Success -> {
                        val tables = (uiState as TablesUiState.Success).tables

                        IOSSearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            placeholder = stringResource(R.string.tables_search_hint),
                            modifier = Modifier.padding(horizontal = LocalDesignTokens.current.screenPaddingHorizontal)
                        )

                        Spacer(modifier = Modifier.height(LocalDesignTokens.current.cardSpacing))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(tables, key = { it.name }) { table ->
                                TableCard(
                                    table = table,
                                    onCardClick = {
                                        workspaceManager.openCard(
                                            WorkspaceCard.Table(
                                                id = "table:${databaseName}:${table.name}",
                                                title = table.name,
                                                connectionId = connectionId,
                                                databaseName = databaseName,
                                                tableName = table.name
                                            )
                                        )
                                    },
                                    modifier = Modifier.padding(
                                        horizontal = LocalDesignTokens.current.screenPaddingHorizontal,
                                        vertical = LocalDesignTokens.current.cardSpacing / 2
                                    )
                                )
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }

                    is TablesUiState.Empty -> {
                        EmptyState(
                            icon = painterResource(AppIcons.State.EmptyTables),
                            title = stringResource(R.string.empty_tables_title),
                            description = stringResource(R.string.empty_tables_description),
                            action = null,  // No action para empty tables (se crean desde Editor)
                            modifier = Modifier.weight(1f)
                        )
                    }

                    is TablesUiState.Error -> {
                        val errorMessage = (uiState as TablesUiState.Error).message
                        ErrorCard(
                            message = errorMessage,
                            onRetry = { viewModel.loadTables(databaseName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    } // Cierre del Scaffold

    // Bottom Sheet para crear tabla (change `create-table`)
    if (showAddTableSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    addTableSheetState.hide()
                    onDismissAddTableSheet()
                }
            },
            sheetState = addTableSheetState,
            containerColor = LocalDesignTokens.current.backgroundPrimary,
            sheetMaxWidth = 10000.dp,
            scrimColor = LocalDesignTokens.current.backdropScrim,
            tonalElevation = 16.dp
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(start = 16.dp, end = 16.dp, top = statusBarHeightDp)
                ) {
                    CreateTableFormContent(
                        connectionId = connectionId,
                        onDismiss = {
                            scope.launch {
                                addTableSheetState.hide()
                                onDismissAddTableSheet()
                            }
                        },
                        onTableCreated = {
                            // Refrescar la lista de tablas tras crear una nueva
                            viewModel.loadTables(databaseName)
                        },
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Preview para TablesListScreen.
 */
@Preview(showBackground = true)
@Composable
private fun TablesListScreenPreview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview: Lista de tablas")
        }
    }
}
