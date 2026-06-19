package com.sphynxs.mydatabases.ui.screens.databases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.core.database.models.Database
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
 * @property savedStateHandle Saved state handle para acceder a navArgs
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@HiltViewModel
class DatabasesListViewModel @Inject constructor(
    private val getDatabasesUseCase: GetDatabasesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val connectionId: String = savedStateHandle["connectionId"]
        ?: throw IllegalStateException("connectionId navArg missing")

    private val _uiState = MutableStateFlow<DatabasesUiState>(DatabasesUiState.Loading)
    
    /**
     * Estado de la UI reactivo.
     *
     * Emite Loading inicialmente, luego Success o Error según el resultado
     * de GetDatabasesUseCase.
     */
    val uiState: StateFlow<DatabasesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allDatabases: List<Database> = emptyList()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val query = _searchQuery.value
        val filtered = if (query.isBlank()) {
            allDatabases
        } else {
            allDatabases.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.value = when {
            allDatabases.isEmpty() -> DatabasesUiState.Empty
            else -> DatabasesUiState.Success(filtered)
        }
    }

    /**
     * Carga la lista de bases de datos disponibles.
     *
     * Invoca GetDatabasesUseCase y actualiza uiState con Success o Error.
     */
    fun loadDatabases() {
        viewModelScope.launch {
            _uiState.value = DatabasesUiState.Loading
            _searchQuery.value = ""

            getDatabasesUseCase().fold(
                onSuccess = { databases ->
                    allDatabases = databases
                    applyFilter()
                },
                onFailure = { error ->
                    _uiState.value = DatabasesUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
}
