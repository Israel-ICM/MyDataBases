package com.sphynxs.mydatabases.ui.screens.connections

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.usecases.ConnectToDatabaseUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.DeleteConnectionUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.GetConnectionsUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.GetConnectionUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de lista de conexiones.
 *
 * Maneja el estado de la lista de conexiones y las acciones del usuario
 * (eliminar, probar conexión).
 *
 * @property getConnectionsUseCase Use case para obtener todas las conexiones
 * @property deleteConnectionUseCase Use case para eliminar una conexión
 * @property testConnectionUseCase Use case para probar una conexión sin guardarla
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@HiltViewModel
class ConnectionsListViewModel @Inject constructor(
    getConnectionsUseCase: GetConnectionsUseCase,
    private val getConnectionUseCase: GetConnectionUseCase,
    private val deleteConnectionUseCase: DeleteConnectionUseCase,
    private val testConnectionUseCase: TestConnectionUseCase,
    private val connectToDatabaseUseCase: ConnectToDatabaseUseCase,
    private val repository: com.sphynxs.mydatabases.core.database.repository.DatabaseRepository,
    private val connectionRepository: com.sphynxs.mydatabases.domain.repositories.ConnectionRepository,
    private val getGroupedConnectionsUseCase: com.sphynxs.mydatabases.domain.usecases.folders.GetGroupedConnectionsUseCase,
    private val createFolderUseCase: com.sphynxs.mydatabases.domain.usecases.folders.CreateFolderUseCase,
    private val deleteFolderUseCase: com.sphynxs.mydatabases.domain.usecases.folders.DeleteFolderUseCase,
    private val moveConnectionToFolderUseCase: com.sphynxs.mydatabases.domain.usecases.folders.MoveConnectionToFolderUseCase,
    private val folderRepository: com.sphynxs.mydatabases.domain.repositories.FolderRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DBConnectionsList"
    }

    /**
     * Estado de la UI reactivo.
     *
     * Escucha cambios en el Flow de conexiones y mapea
     * a Success con la lista actual.
     */
    val uiState: StateFlow<ConnectionsUiState> = getConnectionsUseCase()
        .map { connections ->
            ConnectionsUiState.Success(connections)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionsUiState.Loading
        )

    /**
     * Elimina una conexión por su ID.
     *
     * Llama al use case de eliminación en una coroutine.
     * El Flow de conexiones se actualiza automáticamente gracias
     * a Room, emitiendo nuevo Success con la lista actualizada.
     *
     * @param connectionId El ID de la conexión a eliminar
     */
    fun deleteConnection(connectionId: String) {
        viewModelScope.launch {
            deleteConnectionUseCase(connectionId)
        }
    }

    private val _connectingState = MutableStateFlow<String?>(null)
    val connectingState: StateFlow<String?> = _connectingState.asStateFlow()
    
    /**
     * Flow del ID de la conexión activa actualmente.
     */
    val activeConnectionId: StateFlow<String?> = repository.activeConnectionId
    
    /**
     * Estado de la lista agrupada de conexiones y folders.
     *
     * Escucha cambios en folders y conexiones, combinándolos en una
     * estructura jerárquica lista para mostrar en UI.
     */
    val groupedConnections: StateFlow<List<com.sphynxs.mydatabases.domain.models.ConnectionListItem>> =
        getGroupedConnectionsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Prueba una conexión sin guardarla.
     *
     * @param config La configuración de conexión a probar
     * @return Result.success si conectó bien, Result.failure con error si falló
     */
    suspend fun testConnection(config: ConnectionConfig): Result<Unit> {
        return testConnectionUseCase(config)
    }

    /**
     * Conecta a una base de datos usando una conexión guardada.
     * 
     * @param connectionId El ID de la conexión a usar
     * @return Result con Unit si exitoso, error si falla
     */
    suspend fun connect(connectionId: String): Result<Unit> {
        return try {
            _connectingState.value = connectionId
            val config = getConnectionUseCase(connectionId)
            if (config == null) {
                _connectingState.value = null
                Log.e(TAG, "Connect failed: connection not found, id=$connectionId")
                return Result.failure(Exception("Conexión no encontrada"))
            }
            
            val result = connectToDatabaseUseCase(config)
            _connectingState.value = null

            result.exceptionOrNull()?.let { error ->
                Log.e(TAG, "Connect failed: ${config.toSafeLogString()}", error)
            }
            
            result.map { Unit }
        } catch (e: Exception) {
            _connectingState.value = null
            Log.e(TAG, "Connect crashed: id=$connectionId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Desconecta de la base de datos activa.
     * 
     * @return Result con Unit si exitoso, error si falla
     */
    suspend fun disconnect(): Result<Unit> {
        return try {
            val result = repository.disconnect()
            
            result.exceptionOrNull()?.let { error ->
                Log.e(TAG, "Disconnect failed", error)
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect crashed", e)
            Result.failure(e)
        }
    }
    
    // ========== Folder Operations ==========
    
    /**
     * Alterna el estado expandido/colapsado de un folder.
     *
     * @param folderId El ID del folder a alternar
     */
    fun toggleFolderExpand(folderId: String) {
        viewModelScope.launch {
            try {
                val folder = folderRepository.getById(folderId)
                if (folder != null) {
                    folderRepository.toggleExpand(folderId, !folder.isExpanded)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Toggle folder expand failed: folderId=$folderId", e)
            }
        }
    }
    
    /**
     * Crea un nuevo folder.
     *
     * @param name El nombre del folder
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            try {
                createFolderUseCase(name)
            } catch (e: Exception) {
                Log.e(TAG, "Create folder failed: name=$name", e)
            }
        }
    }
    
    /**
     * Actualiza el nombre de un folder existente.
     *
     * @param folderId El ID del folder a actualizar
     * @param name El nuevo nombre
     */
    fun updateFolder(folderId: String, name: String) {
        viewModelScope.launch {
            try {
                folderRepository.updateName(folderId, name)
            } catch (e: Exception) {
                Log.e(TAG, "Update folder failed: folderId=$folderId, name=$name", e)
            }
        }
    }
    
    /**
     * Elimina un folder.
     *
     * @param folderId El ID del folder a eliminar
     * @param moveToRoot Si true, mueve las conexiones a root; si false, las elimina
     */
    fun deleteFolder(folderId: String, moveToRoot: Boolean = true) {
        viewModelScope.launch {
            try {
                deleteFolderUseCase(folderId, moveToRoot)
            } catch (e: Exception) {
                Log.e(TAG, "Delete folder failed: folderId=$folderId, moveToRoot=$moveToRoot", e)
            }
        }
    }
    
    /**
     * Mueve una conexión a un folder.
     *
     * @param connectionId El ID de la conexión a mover
     * @param folderId El ID del folder destino (null = mover a root)
     */
    fun moveConnectionToFolder(connectionId: String, folderId: String?) {
        viewModelScope.launch {
            try {
                moveConnectionToFolderUseCase(connectionId, folderId)
            } catch (e: Exception) {
                Log.e(TAG, "Move connection to folder failed: connectionId=$connectionId, folderId=$folderId", e)
            }
        }
    }
    
    /**
     * Reordena items en la lista (folders y conexiones).
     *
     * @param fromIndex Índice original del item
     * @param toIndex Índice destino del item
     * @param itemType Tipo de item ("folder" o "connection")
     * @param itemId ID del item a reordenar
     */
    fun reorderItem(fromIndex: Int, toIndex: Int, itemType: String, itemId: String) {
        viewModelScope.launch {
            try {
                when (itemType) {
                    "folder" -> {
                        folderRepository.updateOrder(itemId, toIndex)
                    }
                    "connection" -> {
                        connectionRepository.updateOrder(itemId, toIndex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reorder item failed: type=$itemType, id=$itemId, from=$fromIndex, to=$toIndex", e)
            }
        }
    }
}

private fun ConnectionConfig.toSafeLogString(): String {
    return "id=$id, name=$name, type=$type, host=$host, port=$port, database=$database, username=$username, useSSL=$useSSL"
}
