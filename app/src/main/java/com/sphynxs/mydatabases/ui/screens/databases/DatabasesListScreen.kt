package com.sphynxs.mydatabases.ui.screens.databases

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.ui.components.AppIcons
import com.sphynxs.mydatabases.ui.components.BreathingBackground
import com.sphynxs.mydatabases.ui.components.DatabaseCard
import com.sphynxs.mydatabases.ui.components.EmptyState
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.LoadingIndicator
import com.sphynxs.mydatabases.ui.components.ScreenTitle
import com.sphynxs.mydatabases.ui.components.ios.IOSSearchBar
import com.sphynxs.mydatabases.ui.components.skeleton.DatabaseListSkeleton
import com.sphynxs.mydatabases.ui.theme.DesignTokens
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import kotlinx.coroutines.launch

/**
 * Pantalla de lista de bases de datos.
 *
 * Muestra todas las bases de datos disponibles en el servidor conectado.
 * Al seleccionar una base de datos, navega a la lista de tablas.
 *
 * @param connectionId ID de la conexión activa
 * @param onNavigateToTables Callback para navegar a la lista de tablas
 * @param showAddDatabaseSheet Si se debe mostrar el sheet de agregar database (controlado externamente)
 * @param onDismissAddDatabaseSheet Callback cuando se cierra el sheet
 * @param viewModel El ViewModel con la lógica de estado
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-12 (updated 2026-06-19 para bottom sheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabasesListScreen(
    connectionId: String,
    onNavigateToTables: (databaseName: String) -> Unit,
    onNavigateBack: () -> Unit,
    showAddDatabaseSheet: Boolean = false,
    onDismissAddDatabaseSheet: () -> Unit = {},
    viewModel: DatabasesListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estado para el bottom sheet de agregar database
    val addDatabaseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Altura real del status bar (incluye notch/cutout)
    val statusBarHeightDp = with(LocalDensity.current) {
        LocalContext.current.resources
            .getIdentifier("status_bar_height", "dimen", "android")
            .takeIf { it > 0 }
            ?.let { resourceId ->
                LocalContext.current.resources.getDimensionPixelSize(resourceId).toDp()
            } ?: 24.dp
    }

    // Cargar databases al montar la pantalla
    LaunchedEffect(Unit) {
        viewModel.loadDatabases()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (uiState) {
            is DatabasesUiState.Loading -> {
                DatabaseListSkeleton(modifier = Modifier.padding(paddingValues))
            }

            is DatabasesUiState.Success -> {
                val databases = (uiState as DatabasesUiState.Success).databases
                
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
                        title = stringResource(R.string.databases_title),
                        onBackClick = onNavigateBack
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IOSSearchBar(
                        query = searchQuery,
                        onQueryChange = viewModel::setSearchQuery,
                        placeholder = stringResource(R.string.databases_search_hint),
                        modifier = Modifier.padding(horizontal = DesignTokens.ScreenPaddingHorizontal)
                    )

                    if (databases.isEmpty()) {
                        EmptyState(
                            icon = painterResource(AppIcons.State.EmptyTables),
                            title = stringResource(R.string.empty_databases_title),
                            description = stringResource(R.string.empty_databases_description),
                            action = null,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(DesignTokens.CardSpacing))
                        
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(databases, key = { it.name }) { database ->
                                DatabaseCard(
                                    database = database,
                                    onCardClick = { onNavigateToTables(database.name) },
                                    modifier = Modifier.padding(
                                        horizontal = DesignTokens.ScreenPaddingHorizontal,
                                        vertical = DesignTokens.CardSpacing / 2
                                    )
                                )
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                    }
                }
            }

            is DatabasesUiState.Empty -> {
                EmptyState(
                    icon = painterResource(AppIcons.State.EmptyTables),
                    title = stringResource(R.string.empty_databases_title),
                    description = stringResource(R.string.empty_databases_description),
                    action = null,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is DatabasesUiState.Error -> {
                val errorMessage = (uiState as DatabasesUiState.Error).message
                ErrorCard(
                    message = errorMessage,
                    onRetry = { viewModel.loadDatabases() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }

    // Bottom Sheet para agregar database
    if (showAddDatabaseSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    addDatabaseSheetState.hide()
                    onDismissAddDatabaseSheet()
                }
            },
            sheetState = addDatabaseSheetState,
            containerColor = com.sphynxs.mydatabases.ui.theme.DesignTokens.BackgroundPrimary,
            sheetMaxWidth = 10000.dp,
            scrimColor = DesignTokens.BackdropScrim,
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
                    AddDatabaseFormContent(
                        connectionId = connectionId,
                        onDismiss = {
                            scope.launch {
                                addDatabaseSheetState.hide()
                                onDismissAddDatabaseSheet()
                            }
                        },
                        onDatabaseCreated = { databaseName ->
                            // Refresh the database list
                            viewModel.loadDatabases()
                            // Navigate directly to the newly created database's tables
                            onNavigateToTables(databaseName)
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
 * Preview para DatabasesListScreen con datos.
 */
@Preview(showBackground = true)
@Composable
private fun DatabasesListScreenPreview() {
    MyDataBasesTheme {
        // Preview estático con estado Success simulado
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview: Lista de bases de datos")
        }
    }
}
