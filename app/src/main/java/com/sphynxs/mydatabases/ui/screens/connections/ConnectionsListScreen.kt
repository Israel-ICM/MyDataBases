package com.sphynxs.mydatabases.ui.screens.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.ui.components.AppIcons
import com.sphynxs.mydatabases.ui.components.EmptyState
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSListItem
import com.sphynxs.mydatabases.ui.components.skeleton.ConnectionListSkeleton
import com.sphynxs.mydatabases.ui.theme.DbAccents
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
    var connectionToDelete by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.ConnectionConfig?>(null) }
    
    // Estado para el bottom sheet del formulario
    var showFormSheet by remember { mutableStateOf(false) }
    var editingConnectionId by remember { mutableStateOf<String?>(null) }
    val formSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.background(Color(0xFFF2F2F7)),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    editingConnectionId = null
                    showFormSheet = true
                },
                containerColor = Color(0xFF007AFF)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.connections_add_new),
                    tint = Color.White
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (uiState) {
            is ConnectionsUiState.Loading -> {
                ConnectionListSkeleton(modifier = Modifier.padding(paddingValues))
            }

            is ConnectionsUiState.Success -> {
                val connections = (uiState as ConnectionsUiState.Success).connections

                if (connections.isEmpty()) {
                    // Empty state
                    EmptyState(
                        icon = painterResource(AppIcons.State.EmptyConnections),
                        title = stringResource(R.string.empty_connections_title),
                        description = stringResource(R.string.empty_connections_description),
                        action = {
                            Button(onClick = { onNavigateToForm(null) }) {
                                Text(stringResource(R.string.connections_add_new))
                            }
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    // Lista de conexiones estilo iOS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF2F2F7))
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "CONEXIONES",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93),
                                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                            )
                        }
                        
                        item {
                            IOSGroupedCard {
                                connections.forEachIndexed { index, connection ->
                                    IOSListItem(
                                        title = connection.name,
                                        subtitle = "${connection.host}:${connection.port}",
                                        onClick = { onConnect(connection.id) },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(AppIcons.Db.icon(connection.type)),
                                                contentDescription = null,
                                                tint = DbAccents.accentFor(connection.type),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        },
                                        showDivider = index < connections.size - 1
                                    )
                                }
                            }
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
                    onRetry = null,  // Flow se recarga automáticamente
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
    
    // Bottom Sheet del formulario
    if (showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFormSheet = false },
            sheetState = formSheetState,
            containerColor = Color(0xFFF2F2F7),
            sheetMaxWidth = 10000.dp // Sin límite de ancho
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp) // Margen superior
            ) {
                ConnectionFormScreen(
                    connectionId = editingConnectionId,
                    onNavigateBack = { showFormSheet = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}




