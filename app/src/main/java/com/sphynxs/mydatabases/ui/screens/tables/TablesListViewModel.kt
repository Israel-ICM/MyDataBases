package com.sphynxs.mydatabases.ui.screens.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.usecases.GetTablesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de lista de tablas.
 *
 * Maneja el estado de la lista de tablas disponibles
 * en una base de datos seleccionada.
 *
 * @property getTablesUseCase Use case para obtener todas las tablas de una DB
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@HiltViewModel
class TablesListViewModel @Inject constructor(
    private val getTablesUseCase: GetTablesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TablesUiState>(TablesUiState.Loading)
    
    /**
     * Estado de la UI reactivo.
     *
     * Emite Loading inicialmente, luego Success/Empty o Error según el resultado
     * de GetTablesUseCase.
     */
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    /**
     * Carga la lista de tablas de una base de datos.
     *
     * Invoca GetTablesUseCase y actualiza uiState con Success, Empty o Error.
     *
     * @param databaseName Nombre de la base de datos
     */
    fun loadTables(databaseName: String) {
        viewModelScope.launch {
            _uiState.value = TablesUiState.Loading
            
            getTablesUseCase(databaseName).fold(
                onSuccess = { tables ->
                    _uiState.value = if (tables.isEmpty()) {
                        TablesUiState.Empty
                    } else {
                        TablesUiState.Success(tables)
                    }
                },
                onFailure = { error ->
                    _uiState.value = TablesUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
}
