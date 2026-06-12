package com.sphynxs.mydatabases.ui.screens.databases

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.models.Database
import com.sphynxs.mydatabases.ui.components.DatabaseCard
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.LoadingIndicator
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme

/**
 * Pantalla de lista de bases de datos.
 *
 * Muestra todas las bases de datos disponibles en el servidor conectado.
 * Al seleccionar una base de datos, navega a la lista de tablas.
 *
 * @param onNavigateToTables Callback para navegar a la lista de tablas
 * @param viewModel El ViewModel con la lógica de estado
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabasesListScreen(
    onNavigateToTables: (databaseName: String) -> Unit,
    viewModel: DatabasesListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Cargar databases al montar la pantalla
    LaunchedEffect(Unit) {
        viewModel.loadDatabases()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.databases_title)) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (uiState) {
            is DatabasesUiState.Loading -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }

            is DatabasesUiState.Success -> {
                val databases = (uiState as DatabasesUiState.Success).databases
                
                if (databases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.databases_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        items(databases, key = { it.name }) { database ->
                            DatabaseCard(
                                database = database,
                                onCardClick = { onNavigateToTables(database.name) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
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
