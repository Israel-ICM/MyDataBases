package com.sphynxs.mydatabases.ui.screens.tables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.AppIcons
import com.sphynxs.mydatabases.ui.components.EmptyState
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.LoadingIndicator
import com.sphynxs.mydatabases.ui.components.TableCard
import com.sphynxs.mydatabases.ui.components.skeleton.TableListSkeleton
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import com.sphynxs.mydatabases.ui.workspace.WorkspaceCard
import com.sphynxs.mydatabases.ui.workspace.WorkspaceManager
import com.sphynxs.mydatabases.ui.workspace.WorkspaceOverlay
import javax.inject.Inject

/**
 * Pantalla de lista de tablas.
 *
 * Muestra todas las tablas disponibles en una base de datos seleccionada.
 * Al seleccionar una tabla, navega al visor de tabla.
 *
 * @param databaseName Nombre de la base de datos
 * @param onNavigateToTableViewer Callback para navegar al visor de tabla
 * @param viewModel El ViewModel con la lógica de estado
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesListScreen(
    databaseName: String,
    onNavigateToTableViewer: (tableName: String) -> Unit,
    workspaceManager: WorkspaceManager,
    viewModel: TablesListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Cargar tables al montar la pantalla
    LaunchedEffect(databaseName) {
        viewModel.loadTables(databaseName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tables_title, databaseName)) }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is TablesUiState.Loading -> {
                TableListSkeleton(modifier = Modifier.padding(paddingValues))
            }

            is TablesUiState.Success -> {
                val tables = (uiState as TablesUiState.Success).tables
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(tables, key = { it.name }) { table ->
                        TableCard(
                            table = table,
                            onCardClick = {
                                // Abrir tabla en el workspace en lugar de navegar
                                workspaceManager.openCard(
                                    WorkspaceCard.Table(
                                        id = "table:${databaseName}:${table.name}",
                                        title = table.name,
                                        connectionId = "current", // TODO: obtener connectionId real
                                        databaseName = databaseName,
                                        tableName = table.name
                                    )
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            is TablesUiState.Empty -> {
                EmptyState(
                    icon = painterResource(AppIcons.State.EmptyTables),
                    title = stringResource(R.string.empty_tables_title),
                    description = stringResource(R.string.empty_tables_description),
                    action = null,  // No action para empty tables (se crean desde Editor)
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is TablesUiState.Error -> {
                val errorMessage = (uiState as TablesUiState.Error).message
                ErrorCard(
                    message = errorMessage,
                    onRetry = { viewModel.loadTables(databaseName) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    } // Cierre del Scaffold
}

/**
 * Preview para TablesListScreen.
 */
@Preview(showBackground = true)
@Composable
private fun TablesListScreenPreview() {
    MyDataBasesTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview: Lista de tablas")
        }
    }
}
