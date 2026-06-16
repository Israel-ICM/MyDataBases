package com.sphynxs.mydatabases.ui.screens.databases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.usecases.GetDatabasesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de lista de bases de datos.
 *
 * Maneja el estado de la lista de bases de datos disponibles
 * en el servidor conectado.
 *
 * @property getDatabasesUseCase Use case para obtener todas las bases de datos
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@HiltViewModel
class DatabasesListViewModel @Inject constructor(
    private val getDatabasesUseCase: GetDatabasesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DatabasesUiState>(DatabasesUiState.Loading)
    
    /**
     * Estado de la UI reactivo.
     *
     * Emite Loading inicialmente, luego Success o Error según el resultado
     * de GetDatabasesUseCase.
     */
    val uiState: StateFlow<DatabasesUiState> = _uiState.asStateFlow()

    /**
     * Carga la lista de bases de datos disponibles.
     *
     * Invoca GetDatabasesUseCase y actualiza uiState con Success o Error.
     */
    fun loadDatabases() {
        viewModelScope.launch {
            _uiState.value = DatabasesUiState.Loading
            
            getDatabasesUseCase().fold(
                onSuccess = { databases ->
                    _uiState.value = DatabasesUiState.Success(databases)
                },
                onFailure = { error ->
                    _uiState.value = DatabasesUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
}
