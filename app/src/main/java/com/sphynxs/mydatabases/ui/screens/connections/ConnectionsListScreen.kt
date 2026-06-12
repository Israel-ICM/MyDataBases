package com.sphynxs.mydatabases.ui.screens.connections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.ui.components.ConnectionCard
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.LoadingIndicator
import com.sphynxs.mydatabases.ui.theme.MyDataBasesTheme
import kotlinx.coroutines.launch

/**
 * Pantalla de lista de conexiones.
 *
 * Muestra todas las conexiones guardadas, permite agregar/editar/eliminar,
 * y conectarse a una base de datos.
 *
 * @param onNavigateToForm Callback para navegar al formulario (nuevo o editar)
 * @param onConnect Callback cuando se selecciona una conexión para conectar
 * @param viewModel El ViewModel con la lógica de estado
 * @param modifier Modificador opcional
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsListScreen(
    onNavigateToForm: (connectionId: String?) -> Unit,
    onConnect: (connectionId: String) -> Unit,
    viewModel: ConnectionsListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estado para el diálogo de confirmación de eliminación
    var connectionToDelete by remember { mutableStateOf<ConnectionConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.connections_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToForm(null) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.connections_add_new)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        when (uiState) {
            is ConnectionsUiState.Loading -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }

            is ConnectionsUiState.Success -> {
                val connections = (uiState as ConnectionsUiState.Success).connections

                if (connections.isEmpty()) {
                    // Empty state
                    EmptyConnectionsState(
                        onAddClick = { onNavigateToForm(null) },
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    // Lista de conexiones
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp)
                    ) {
                        items(
                            items = connections,
                            key = { it.id }
                        ) { connection ->
                            Spacer(modifier = Modifier.height(8.dp))
                            ConnectionCard(
                                connection = connection,
                                onEditClick = { onNavigateToForm(connection.id) },
                                onDeleteClick = { connectionToDelete = connection },
                                onCardClick = { onConnect(connection.id) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // Para que no tape el FAB
                        }
                    }
                }
            }

            is ConnectionsUiState.Error -> {
                val message = (uiState as ConnectionsUiState.Error).message
                ErrorCard(
                    message = message,
                    onRetry = { /* El Flow se recarga automáticamente */ },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Diálogo de confirmación de eliminación
    connectionToDelete?.let { connection ->
        AlertDialog(
            onDismissRequest = { connectionToDelete = null },
            title = { Text(stringResource(R.string.connection_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.connection_delete_confirm_message, connection.name)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConnection(connection.id)
                        connectionToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Conexión eliminada" // TODO: string resource
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { connectionToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/**
 * Estado vacío cuando no hay conexiones.
 */
@Composable
private fun EmptyConnectionsState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.connections_empty_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.connections_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.connections_add_new)
                )
            }
        }
    }
}

/**
 * Preview para ConnectionsListScreen con lista.
 */
@Preview(showBackground = true)
@Composable
private fun ConnectionsListScreenPreview() {
    MyDataBasesTheme {
        // Preview con datos estáticos
        ConnectionsListScreen(
            onNavigateToForm = {},
            onConnect = {}
        )
    }
}

/**
 * Preview para empty state.
 */
@Preview(showBackground = true)
@Composable
private fun EmptyConnectionsStatePreview() {
    MyDataBasesTheme {
        EmptyConnectionsState(onAddClick = {})
    }
}
