package com.sphynxs.mydatabases.ui.screens.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.usecases.connections.DeleteConnectionUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.GetConnectionsUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val deleteConnectionUseCase: DeleteConnectionUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) : ViewModel() {

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

    /**
     * Prueba una conexión sin guardarla.
     *
     * @param config La configuración de conexión a probar
     * @return Result.success si conectó bien, Result.failure con error si falló
     */
    suspend fun testConnection(config: ConnectionConfig): Result<Unit> {
        return testConnectionUseCase(config)
    }
}
