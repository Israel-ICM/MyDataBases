package com.sphynxs.mydatabases.ui.screens.connections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.ConnectionConfig
import com.sphynxs.mydatabases.domain.usecases.connections.GetConnectionByIdUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.SaveConnectionUseCase
import com.sphynxs.mydatabases.domain.usecases.connections.TestConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el formulario de conexión (crear/editar).
 *
 * Maneja el estado del formulario, guardado, y test de conexión.
 *
 * @property saveConnectionUseCase Use case para guardar la conexión
 * @property getConnectionByIdUseCase Use case para cargar una conexión existente
 * @property testConnectionUseCase Use case para probar conexión sin guardar
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@HiltViewModel
class ConnectionFormViewModel @Inject constructor(
    private val saveConnectionUseCase: SaveConnectionUseCase,
    private val getConnectionByIdUseCase: GetConnectionByIdUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow<ConnectionFormUiState>(ConnectionFormUiState.Idle)
    /**
     * Estado del formulario reactivo (Idle, Saving, Saved, Error).
     */
    val formState: StateFlow<ConnectionFormUiState> = _formState.asStateFlow()

    private val _testState = MutableStateFlow<ConnectionTestUiState>(ConnectionTestUiState.Idle)
    /**
     * Estado del test de conexión reactivo (Idle, Testing, Success, Error).
     */
    val testState: StateFlow<ConnectionTestUiState> = _testState.asStateFlow()

    /**
     * Guarda una conexión (nueva o actualización).
     *
     * Emite Saving → Saved en caso de éxito, o Error si falla.
     *
     * @param config La configuración de conexión a guardar
     */
    fun saveConnection(config: ConnectionConfig) {
        viewModelScope.launch {
            _formState.value = ConnectionFormUiState.Saving
            try {
                saveConnectionUseCase(config)
                _formState.value = ConnectionFormUiState.Saved
            } catch (e: Exception) {
                _formState.value = ConnectionFormUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Prueba una conexión sin guardarla.
     *
     * Emite Testing → Success si conecta, o Testing → Error si falla.
     *
     * @param config La configuración de conexión a probar
     */
    fun testConnection(config: ConnectionConfig) {
        viewModelScope.launch {
            _testState.value = ConnectionTestUiState.Testing
            val result = testConnectionUseCase(config)
            if (result.isSuccess) {
                _testState.value = ConnectionTestUiState.Success
            } else {
                _testState.value = ConnectionTestUiState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error"
                )
            }
        }
    }

    /**
     * Carga una conexión existente por su ID (para edición).
     *
     * @param connectionId El ID de la conexión a cargar
     * @return La configuración cargada, o null si no existe
     */
    suspend fun loadConnection(connectionId: String): ConnectionConfig? {
        return getConnectionByIdUseCase(connectionId)
    }
}
