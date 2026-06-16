package com.sphynxs.mydatabases.ui.screens.tableviewer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.usecases.ExecuteQueryUseCase
import com.sphynxs.mydatabases.domain.usecases.ExecuteUpdateUseCase
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
 * @property executeUpdateUseCase Use case para comandos DDL (USE database)
 * @property getColumnsUseCase Use case para obtener metadata de columnas
 *
 * @author israel-icm
 * @date 2026-06-12
 */
@HiltViewModel
class TableViewerViewModel @Inject constructor(
    private val executeQueryUseCase: ExecuteQueryUseCase,
    private val executeUpdateUseCase: ExecuteUpdateUseCase,
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
            
            // Cambiar a la database correcta antes de consultar metadata
            // Esto es necesario porque getColumns usa DATABASE() en la query
            // USE es un comando DDL, no retorna ResultSet → usar executeUpdate
            val useDatabaseResult = executeUpdateUseCase("USE `$databaseName`", emptyList())
            if (useDatabaseResult.isFailure) {
                val error = "Failed to switch database: ${useDatabaseResult.exceptionOrNull()?.message}"
                Log.e("TableViewerViewModel", error, useDatabaseResult.exceptionOrNull())
                _uiState.value = TableViewerUiState.Error(error)
                return@launch
            }
            
            // Ejecutar rows y columns en paralelo
            // getColumns espera SOLO el nombre de la tabla (sin DB prefix)
            // porque la query usa TABLE_SCHEMA = DATABASE() internamente
            val columnsDeferred = async { getColumnsUseCase(tableName) }
            
            // executeQuery sí necesita DB.table con backticks para evitar errores de sintaxis
            val rowsDeferred = async { executeQueryUseCase("SELECT * FROM `$databaseName`.`$tableName` LIMIT 1000", emptyList()) }
            
            val columnsResult = columnsDeferred.await()
            val rowsResult = rowsDeferred.await()
            
            // Evaluar resultados
            if (rowsResult.isFailure) {
                val error = "Query execution failed: ${rowsResult.exceptionOrNull()?.message}"
                Log.e("TableViewerViewModel", error, rowsResult.exceptionOrNull())
                _uiState.value = TableViewerUiState.Error(error)
                return@launch
            }
            
            if (columnsResult.isFailure) {
                val error = "Failed to load columns: ${columnsResult.exceptionOrNull()?.message}"
                Log.e("TableViewerViewModel", error, columnsResult.exceptionOrNull())
                _uiState.value = TableViewerUiState.Error(error)
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
