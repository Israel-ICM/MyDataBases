package com.sphynxs.mydatabases.ui.screens.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.sphynxs.mydatabases.R
import com.sphynxs.mydatabases.core.database.engine.DatabaseType
import com.sphynxs.mydatabases.ui.components.AppIcons
import com.sphynxs.mydatabases.ui.components.BreathingBackground
import com.sphynxs.mydatabases.ui.components.ConnectionCard
import com.sphynxs.mydatabases.ui.components.DatabaseTypeCard
import com.sphynxs.mydatabases.ui.components.EmptyState
import com.sphynxs.mydatabases.ui.components.ErrorCard
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSListItem
import com.sphynxs.mydatabases.ui.theme.DbAccents
import com.sphynxs.mydatabases.ui.theme.DesignTokens
import com.sphynxs.mydatabases.ui.components.skeleton.ConnectionListSkeleton
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
    val connectingState by viewModel.connectingState.collectAsState()
    val activeConnectionId by viewModel.activeConnectionId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estado para el diálogo de confirmación de eliminación
    var connectionToDelete by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.ConnectionConfig?>(null) }
    
    // Estado para el bottom sheet del selector de tipo
    var showTypeSelectorSheet by remember { mutableStateOf(false) }
    val typeSelectorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Estado para el bottom sheet del formulario
    var showFormSheet by remember { mutableStateOf(false) }
    var editingConnectionId by remember { mutableStateOf<String?>(null) }
    var preselectedType by remember { mutableStateOf<DatabaseType?>(null) }
    
    // Resetear el sheet state cada vez que se abre/cierra
    val formSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )
    
    // Reset sheet state when closing
    LaunchedEffect(showFormSheet) {
        if (!showFormSheet) {
            formSheetState.hide()
        }
    }

    Scaffold(
        modifier = modifier.background(DesignTokens.BackgroundPrimary),
        containerColor = DesignTokens.BackgroundPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is ConnectionsUiState.Success) {
                FloatingActionButton(
                    onClick = {
                        showTypeSelectorSheet = true
                        scope.launch { typeSelectorSheetState.show() }
                    }
                ) {
                    Icon(PhosphorAppIcons.Action.add, contentDescription = "Add connection")
                }
            }
        }
    ) { paddingValues ->
        when (uiState) {
            is ConnectionsUiState.Loading -> {
                ConnectionListSkeleton(modifier = Modifier.padding(paddingValues))
            }

            is ConnectionsUiState.Success -> {
                val connections = (uiState as ConnectionsUiState.Success).connections

                if (connections.isEmpty()) {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_state_empty_connections),
                        title = "No connections",
                        description = "Tap + to add your first database connection",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    BreathingBackground(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Título grande estilo iOS 26
                        Text(
                            text = stringResource(R.string.connections_title),
                            fontSize = DesignTokens.LargeTitleSize,
                            fontWeight = DesignTokens.LargeTitleWeight,
                            color = DesignTokens.LargeTitleColor,
                            modifier = Modifier.padding(horizontal = DesignTokens.ScreenPaddingHorizontal)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(connections) { connection ->
                                val disconnectSuccessMsg = stringResource(R.string.connection_disconnect_success)
                                val disconnectErrorMsg = stringResource(R.string.connection_disconnect_error)
                                
                                ConnectionCard(
                                    connection = connection,
                                    onEditClick = {
                                        editingConnectionId = connection.id
                                        preselectedType = null
                                        showFormSheet = true
                                        scope.launch { 
                                            formSheetState.expand() 
                                        }
                                    },
                                    onDeleteClick = {
                                        connectionToDelete = connection
                                    },
                                    onDisconnectClick = {
                                        scope.launch {
                                            val result = viewModel.disconnect()
                                            result.fold(
                                                onSuccess = {
                                                    snackbarHostState.showSnackbar(
                                                        message = disconnectSuccessMsg,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                },
                                                onFailure = { error ->
                                                    snackbarHostState.showSnackbar(
                                                        message = disconnectErrorMsg.format(error.message ?: "Unknown"),
                                                        duration = SnackbarDuration.Long
                                                    )
                                                }
                                            )
                                        }
                                    },
                                    onCardClick = {
                                        // Si ya está conectada, solo navegar
                                        if (connection.id == activeConnectionId) {
                                            onConnect(connection.id)
                                        } else {
                                            // Si no está conectada, conectar primero
                                            scope.launch {
                                                val result = viewModel.connect(connection.id)
                                                result.fold(
                                                    onSuccess = { onConnect(connection.id) },
                                                    onFailure = { error ->
                                                        snackbarHostState.showSnackbar(
                                                            message = "Error: ${error.message}",
                                                            duration = SnackbarDuration.Long
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    isConnected = connection.id == activeConnectionId,
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
                                message = "Conexión eliminada", // TODO: string resource
                                duration = SnackbarDuration.Long
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
    
    // Loading indicator mientras conecta
    if (connectingState != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesignTokens.BackdropScrim),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFF007AFF)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Conectando...",
                    color = Color.Black,
                    fontSize = 17.sp
                )
            }
        }
    }
    
    // Bottom Sheet selector de tipo de DB
    if (showTypeSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    typeSelectorSheetState.hide()
                    showTypeSelectorSheet = false
                }
            },
            sheetState = typeSelectorSheetState,
            containerColor = Color(0xFFF2F2F7),
            scrimColor = DesignTokens.BackdropScrim,
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_database_type),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                DatabaseType.entries.forEach { type ->
                    DatabaseTypeSelectorCard(
                        type = type,
                        onClick = {
                            scope.launch {
                                typeSelectorSheetState.hide()
                                showTypeSelectorSheet = false
                                preselectedType = type
                                editingConnectionId = null
                                showFormSheet = true
                                formSheetState.expand()
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Bottom Sheet del formulario
    if (showFormSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                scope.launch {
                    formSheetState.hide()
                }.invokeOnCompletion {
                    showFormSheet = false
                    editingConnectionId = null
                    preselectedType = null
                }
            },
            sheetState = formSheetState,
            containerColor = Color(0xFFF2F2F7),
            sheetMaxWidth = 10000.dp,
            scrimColor = DesignTokens.BackdropScrim,
            tonalElevation = 16.dp
        ) {
            ConnectionFormScreen(
                connectionId = editingConnectionId,
                preselectedType = preselectedType,
                onNavigateBack = { 
                    scope.launch {
                        formSheetState.hide()
                    }.invokeOnCompletion {
                        showFormSheet = false
                        editingConnectionId = null
                        preselectedType = null
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Card para seleccionar tipo de base de datos.
 */
@Composable
private fun DatabaseTypeSelectorCard(
    type: DatabaseType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(AppIcons.Db.icon(type)),
                contentDescription = null,
                tint = DbAccents.accentFor(type),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.size(16.dp))
            
            Column {
                Text(
                    text = type.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = getDescriptionForType(type),
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

/**
 * Descripción para cada tipo de DB.
 */
private fun getDescriptionForType(type: DatabaseType): String {
    return when (type) {
        DatabaseType.MYSQL -> "Popular open-source relational database"
        DatabaseType.POSTGRESQL -> "Advanced open-source relational database"
        DatabaseType.SQLITE -> "Lightweight embedded database"
        DatabaseType.MARIADB -> "MySQL-compatible database server"
    }
}

