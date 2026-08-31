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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.sphynxs.mydatabases.ui.components.FolderCard
import com.sphynxs.mydatabases.ui.components.PhosphorAppIcons
import com.sphynxs.mydatabases.ui.components.folders.FolderFormSheet
import com.sphynxs.mydatabases.ui.components.folders.MoveToFolderSheet
import com.sphynxs.mydatabases.domain.models.ConnectionListItem
import com.sphynxs.mydatabases.ui.components.ios.IOSGroupedCard
import com.sphynxs.mydatabases.ui.components.ios.IOSListItem
import com.sphynxs.mydatabases.ui.theme.DbAccents
import com.sphynxs.mydatabases.ui.theme.LocalDesignTokens
import com.sphynxs.mydatabases.ui.components.skeleton.ConnectionListSkeleton
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState

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
    
    // Estado para modo de reordenamiento
    var isReorderMode by remember { mutableStateOf(false) }
    
    // Estados para folders
    var showFolderFormSheet by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.ConnectionFolder?>(null) }
    var showMoveToFolderSheet by remember { mutableStateOf(false) }
    var movingConnection by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.ConnectionConfig?>(null) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var deletingFolder by remember { mutableStateOf<com.sphynxs.mydatabases.core.database.models.ConnectionFolder?>(null) }
    val allFolders by viewModel.groupedConnections.collectAsState()
    
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
        modifier = modifier.background(LocalDesignTokens.current.backgroundPrimary),
        containerColor = LocalDesignTokens.current.backgroundPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is ConnectionsUiState.Success) {
                FloatingActionButton(
                    onClick = {
                        // Fix: NO llamar sheetState.show()/expand() manualmente junto al
                        // booleano - ModalBottomSheet ya se muestra solo al entrar en
                        // composicion. Llamarlo a mano ademas del booleano deja el SheetState
                        // en un estado inconsistente la segunda vez que se reusa la misma
                        // instancia (funciona la primera vez, se cuelga despues) - mismo
                        // patron que ya funciona bien en showFolderFormSheet/showMoveToFolderSheet
                        // en este mismo archivo, que nunca llaman show()/expand() a mano.
                        showTypeSelectorSheet = true
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
                        
                        // Header con título y botón de reordenar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LocalDesignTokens.current.screenPaddingHorizontal),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Título grande estilo iOS 26
                            Text(
                                text = stringResource(R.string.connections_title),
                                fontSize = LocalDesignTokens.current.largeTitleSize,
                                fontWeight = LocalDesignTokens.current.largeTitleWeight,
                                color = LocalDesignTokens.current.largeTitleColor
                            )
                            
                            // Botón de reordenar
                            IconButton(
                                onClick = { isReorderMode = !isReorderMode },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReorderMode) PhosphorAppIcons.Action.check else PhosphorAppIcons.Action.edit,
                                    contentDescription = if (isReorderMode) "Done reordering" else "Reorder connections",
                                    tint = if (isReorderMode) LocalDesignTokens.current.accentPrimary else LocalDesignTokens.current.iconNormal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val lazyListState = rememberLazyListState()
                        
                        var draggingItemIndex by remember { mutableStateOf(-1) }
                        var draggingItemOffset by remember { mutableStateOf(0f) }
                        var draggingItemInitialOffset by remember { mutableStateOf(0) }
                        
                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(
                                items = allFolders,
                                key = { _, item ->
                                    when (item) {
                                        is ConnectionListItem.FolderItem -> "folder-${item.folder.id}"
                                        is ConnectionListItem.ConnectionItem -> "connection-${item.connection.id}"
                                    }
                                }
                            ) { index, item ->
                                val isDragging = draggingItemIndex == index
                                when (item) {
                                    is ConnectionListItem.FolderItem -> {
                                        // Folder card
                                        FolderCard(
                                            folder = item.folder,
                                            connectionCount = item.connectionCount,
                                            isExpanded = item.folder.isExpanded,
                                            onToggleExpand = { 
                                                viewModel.toggleFolderExpand(item.folder.id)
                                            },
                                            onEditClick = {
                                                editingFolder = item.folder
                                                showFolderFormSheet = true
                                            },
                                            onDeleteClick = {
                                                deletingFolder = item.folder
                                                showDeleteFolderDialog = true
                                            },
                                            isReorderMode = isReorderMode,
                                            onDragHandleTouch = {},
                                            modifier = Modifier
                                                .zIndex(if (isDragging) 1f else 0f)
                                                .graphicsLayer {
                                                    translationY = if (isDragging) draggingItemOffset else 0f
                                                }
                                                .padding(
                                                    horizontal = LocalDesignTokens.current.screenPaddingHorizontal,
                                                    vertical = LocalDesignTokens.current.cardSpacing / 2
                                                )
                                                .then(
                                                    if (isReorderMode) {
                                                        Modifier.pointerInput(allFolders.size) {
                                                            detectDragGesturesAfterLongPress(
                                                                onDragStart = {
                                                                    draggingItemIndex = index
                                                                    draggingItemOffset = 0f
                                                                    draggingItemInitialOffset = listState.layoutInfo.visibleItemsInfo
                                                                        .firstOrNull { it.index == index }?.offset ?: 0
                                                                },
                                                                onDrag = { change, dragAmount ->
                                                                    change.consume()
                                                                    draggingItemOffset += dragAmount.y
                                                                    
                                                                    // Calcular punto medio
                                                                    val draggingItemSize = listState.layoutInfo.visibleItemsInfo
                                                                        .firstOrNull { it.index == draggingItemIndex }?.size ?: 0
                                                                    val startOffset = draggingItemInitialOffset + draggingItemOffset
                                                                    val middleOffset = startOffset + draggingItemSize / 2f
                                                                    
                                                                    // Buscar target
                                                                    listState.layoutInfo.visibleItemsInfo.firstOrNull { targetItem ->
                                                                        middleOffset.toInt() in targetItem.offset..(targetItem.offset + targetItem.size) &&
                                                                        targetItem.index != draggingItemIndex
                                                                    }?.let { targetItem ->
                                                                        // Swap
                                                                        val newOrder = allFolders.take(targetItem.index + 1)
                                                                            .count { it is ConnectionListItem.FolderItem } - 1
                                                                        viewModel.reorderItem(draggingItemIndex, newOrder.coerceAtLeast(0), "folder", item.folder.id)
                                                                        
                                                                        // Ajustar offset
                                                                        draggingItemOffset += (draggingItemInitialOffset - targetItem.offset).toFloat()
                                                                        draggingItemIndex = targetItem.index
                                                                        draggingItemInitialOffset = targetItem.offset
                                                                    }
                                                                },
                                                                onDragEnd = {
                                                                    draggingItemIndex = -1
                                                                    draggingItemOffset = 0f
                                                                    draggingItemInitialOffset = 0
                                                                },
                                                                onDragCancel = {
                                                                    draggingItemIndex = -1
                                                                    draggingItemOffset = 0f
                                                                    draggingItemInitialOffset = 0
                                                                }
                                                            )
                                                        }
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                        )
                                        
                                        // Connections inside folder (indented)
                                        if (item.folder.isExpanded) {
                                            item.connections.forEach { connection ->
                                                val disconnectSuccessMsg = stringResource(R.string.connection_disconnect_success)
                                                val disconnectErrorMsg = stringResource(R.string.connection_disconnect_error)
                                                
                                                ConnectionCard(
                                                    connection = connection,
                                                    isReorderMode = isReorderMode,
                                                    onEditClick = {
                                                        // Fix: sin llamar expand() a mano (ver comentario en el FAB de +).
                                                        editingConnectionId = connection.id
                                                        preselectedType = null
                                                        showFormSheet = true
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
                                                    onMoveToFolderClick = {
                                                        movingConnection = connection
                                                        showMoveToFolderSheet = true
                                                    },
                                                    onCardClick = {
                                                        if (connection.id == activeConnectionId) {
                                                            onConnect(connection.id)
                                                        } else {
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
                                                        start = LocalDesignTokens.current.screenPaddingHorizontal + 32.dp, // Indent 32.dp
                                                        end = LocalDesignTokens.current.screenPaddingHorizontal,
                                                        top = LocalDesignTokens.current.cardSpacing / 2,
                                                        bottom = LocalDesignTokens.current.cardSpacing / 2
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    
                                    is ConnectionListItem.ConnectionItem -> {
                                        // Root connection (no folder)
                                        val connection = item.connection
                                        val disconnectSuccessMsg = stringResource(R.string.connection_disconnect_success)
                                        val disconnectErrorMsg = stringResource(R.string.connection_disconnect_error)
                                        
                                        ConnectionCard(
                                            connection = connection,
                                            isReorderMode = isReorderMode,
                                            onEditClick = {
                                                // Fix: sin llamar expand() a mano (ver comentario en el FAB de +).
                                                editingConnectionId = connection.id
                                                preselectedType = null
                                                showFormSheet = true
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
                                            onMoveToFolderClick = {
                                                movingConnection = connection
                                                showMoveToFolderSheet = true
                                            },
                                            onCardClick = {
                                                if (connection.id == activeConnectionId) {
                                                    onConnect(connection.id)
                                                } else {
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
                                            onDragHandleTouch = {},
                                            modifier = Modifier
                                                .zIndex(if (isDragging) 1f else 0f)
                                                .graphicsLayer {
                                                    translationY = if (isDragging) draggingItemOffset else 0f
                                                }
                                                .padding(
                                                    horizontal = LocalDesignTokens.current.screenPaddingHorizontal,
                                                    vertical = LocalDesignTokens.current.cardSpacing / 2
                                                )
                                                .then(
                                                    if (isReorderMode) {
                                                        Modifier.pointerInput(allFolders.size) {
                                                            detectDragGesturesAfterLongPress(
                                                                onDragStart = {
                                                                    draggingItemIndex = index
                                                                    draggingItemOffset = 0f
                                                                    draggingItemInitialOffset = listState.layoutInfo.visibleItemsInfo
                                                                        .firstOrNull { it.index == index }?.offset ?: 0
                                                                },
                                                                onDrag = { change, dragAmount ->
                                                                    change.consume()
                                                                    draggingItemOffset += dragAmount.y
                                                                    
                                                                    val draggingItemSize = listState.layoutInfo.visibleItemsInfo
                                                                        .firstOrNull { it.index == draggingItemIndex }?.size ?: 0
                                                                    val startOffset = draggingItemInitialOffset + draggingItemOffset
                                                                    val middleOffset = startOffset + draggingItemSize / 2f
                                                                    
                                                                    listState.layoutInfo.visibleItemsInfo.firstOrNull { targetItem ->
                                                                        middleOffset.toInt() in targetItem.offset..(targetItem.offset + targetItem.size) &&
                                                                        targetItem.index != draggingItemIndex
                                                                    }?.let { targetItem ->
                                                                        val newOrder = allFolders.take(targetItem.index + 1)
                                                                            .count { it is ConnectionListItem.ConnectionItem } - 1
                                                                        viewModel.reorderItem(draggingItemIndex, newOrder.coerceAtLeast(0), "connection", connection.id)
                                                                        
                                                                        draggingItemOffset += (draggingItemInitialOffset - targetItem.offset).toFloat()
                                                                        draggingItemIndex = targetItem.index
                                                                        draggingItemInitialOffset = targetItem.offset
                                                                    }
                                                                },
                                                                onDragEnd = {
                                                                    draggingItemIndex = -1
                                                                    draggingItemOffset = 0f
                                                                    draggingItemInitialOffset = 0
                                                                },
                                                                onDragCancel = {
                                                                    draggingItemIndex = -1
                                                                    draggingItemOffset = 0f
                                                                    draggingItemInitialOffset = 0
                                                                }
                                                            )
                                                        }
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                            
                            // Add Folder button (only in reorder mode)
                            if (isReorderMode) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showFolderFormSheet = true
                                                editingFolder = null
                                            }
                                            .padding(
                                                horizontal = LocalDesignTokens.current.screenPaddingHorizontal,
                                                vertical = 12.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Small circular + button (iOS style)
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(LocalDesignTokens.current.accentPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = PhosphorAppIcons.Action.add,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        
                                        // "Add Folder" text
                                        Text(
                                            text = stringResource(R.string.folder_create),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = LocalDesignTokens.current.accentPrimary
                                        )
                                    }
                                }
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
                .background(LocalDesignTokens.current.backdropScrim),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = LocalDesignTokens.current.accentPrimary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Conectando...",
                    color = LocalDesignTokens.current.textPrimary,
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
            containerColor = LocalDesignTokens.current.backgroundPrimary,
            scrimColor = LocalDesignTokens.current.backdropScrim,
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
                            // Fix: esperar a que termine la animacion de cierre del selector
                            // (hide() es suspend) antes de tocar el estado del form sheet, y
                            // sin llamar expand() a mano (ver comentario en el FAB de +).
                            scope.launch {
                                typeSelectorSheetState.hide()
                            }.invokeOnCompletion {
                                showTypeSelectorSheet = false
                                preselectedType = type
                                editingConnectionId = null
                                showFormSheet = true
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
            containerColor = LocalDesignTokens.current.backgroundPrimary,
            sheetMaxWidth = 10000.dp,
            scrimColor = LocalDesignTokens.current.backdropScrim,
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
    
    // Delete Folder Dialog
    deletingFolder?.let { folder ->
        val connectionCount = allFolders.filterIsInstance<ConnectionListItem.FolderItem>()
            .firstOrNull { it.folder.id == folder.id }?.connectionCount ?: 0
        
        AlertDialog(
            onDismissRequest = { 
                deletingFolder = null
                showDeleteFolderDialog = false
            },
            title = { Text(stringResource(R.string.folder_delete_confirm_title)) },
            text = { Text(stringResource(R.string.folder_delete_confirm_message, connectionCount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
                        deletingFolder = null
                        showDeleteFolderDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Folder eliminado",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        deletingFolder = null
                        showDeleteFolderDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
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
                    color = LocalDesignTokens.current.textSecondary
                )
            }
        }
    }
}

/**
 * Card para seleccionar tipo de base de datos.
 */
private fun getDescriptionForType(type: DatabaseType): String {
    return when (type) {
        DatabaseType.MYSQL -> "Popular open-source relational database"
        DatabaseType.POSTGRESQL -> "Advanced open-source relational database"
        DatabaseType.SQLITE -> "Lightweight embedded database"
        DatabaseType.MARIADB -> "MySQL-compatible database server"
    }
}

