package com.sphynxs.mydatabases.ui.screens.tableviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.usecases.ExecuteQueryUseCase
import com.sphynxs.mydatabases.domain.usecases.GetColumnsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de visor de tabla.
 *
 * Maneja el estado de la visualización de rows y schema
 * de una tabla seleccionada.
 *
 * @property executeQueryUseCase Use case para obtener rows (SELECT * LIMIT 1000)
 * @property getColumnsUseCase Use case para obtener metadata de columnas
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@HiltViewModel
class TableViewerViewModel @Inject constructor(
    private val executeQueryUseCase: ExecuteQueryUseCase,
    private val getColumnsUseCase: GetColumnsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TableViewerUiState>(TableViewerUiState.Loading)
    
    /**
     * Estado de la UI reactivo.
     *
     * Emite Loading inicialmente, luego Success, Empty o Error según el resultado
     * de ExecuteQueryUseCase + GetColumnsUseCase.
     */
    val uiState: StateFlow<TableViewerUiState> = _uiState.asStateFlow()

    /**
     * Carga los datos de una tabla (rows + schema).
     *
     * Ejecuta en paralelo:
     * - ExecuteQueryUseCase con "SELECT * FROM {db}.{table} LIMIT 1000"
     * - GetColumnsUseCase para metadata de columnas
     *
     * @param databaseName Nombre de la base de datos
     * @param tableName Nombre de la tabla
     */
    fun loadTable(databaseName: String, tableName: String) {
        viewModelScope.launch {
            _uiState.value = TableViewerUiState.Loading
            
            // Ejecutar rows y columns en paralelo
            val columnsDeferred = async { getColumnsUseCase("$databaseName.$tableName") }
            val rowsDeferred = async { executeQueryUseCase("SELECT * FROM $databaseName.$tableName LIMIT 1000", emptyList()) }
            
            val columnsResult = columnsDeferred.await()
            val rowsResult = rowsDeferred.await()
            
            // Evaluar resultados
            if (rowsResult.isFailure) {
                _uiState.value = TableViewerUiState.Error(rowsResult.exceptionOrNull()?.message ?: "Unknown error")
                return@launch
            }
            
            if (columnsResult.isFailure) {
                _uiState.value = TableViewerUiState.Error(columnsResult.exceptionOrNull()?.message ?: "Unknown error")
                return@launch
            }
            
            val rows = rowsResult.getOrNull()!!
            val columns = columnsResult.getOrNull()!!
            
            _uiState.value = if (rows.rowCount == 0) {
                TableViewerUiState.Empty(columns)
            } else {
                TableViewerUiState.Success(rows, columns)
            }
        }
    }
}
