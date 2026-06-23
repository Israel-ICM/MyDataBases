package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.models.StatementResult
import com.sphynxs.mydatabases.domain.usecases.ExecuteQueryUseCase
import com.sphynxs.mydatabases.domain.usecases.ExecuteUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el editor de queries SQL.
 *
 * Responsabilidades:
 * - Ejecutar multi-statement SQL (split por `;`, secuencial)
 * - Detectar SELECT vs non-SELECT (primer keyword)
 * - Agregar resultados (SelectResult si todos queries, UpdateSummary si algún update)
 * - Manejar errores (stop on first failure)
 * - Cancelación UI-only (cancela coroutine, no termina JDBC query)
 *
 * State machine:
 * Idle → Running → SelectResult/UpdateSummary/Error
 *      → cancel() → Idle
 *
 * @param executeQueryUseCase Use case para SELECT-like statements
 * @param executeUpdateUseCase Use case para INSERT/UPDATE/DELETE/DDL
 *
 * @author israel-icm
 * @date 2026-06-23
 */
@HiltViewModel
class QueryEditorViewModel @Inject constructor(
    private val executeQueryUseCase: ExecuteQueryUseCase,
    private val executeUpdateUseCase: ExecuteUpdateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<QueryEditorUiState>(QueryEditorUiState.Idle)
    val uiState: StateFlow<QueryEditorUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null

    // Exception handler para catchear TODAS las excepciones no manejadas
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = QueryEditorUiState.Error(
            message = throwable.message ?: "Unknown error",
            failedStatement = ""
        )
    }

    /**
     * Ejecuta uno o más statements SQL.
     *
     * Split naive por `;` (sin parsing — `;` dentro de strings puede romper).
     * Ejecuta secuencialmente, para en el primer error.
     * Agrega resultados:
     * - Si todos son queries → SelectResult (muestra última)
     * - Si alguno es update → UpdateSummary (tabla de resultados)
     *
     * @param sql SQL input (puede contener múltiples statements separados por `;`)
     */
    fun executeStatements(sql: String) {
        executionJob?.cancel()
        executionJob = viewModelScope.launch(exceptionHandler) {
            _uiState.value = QueryEditorUiState.Running

            // Split por ; y limpiar
            val statements = sql.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (statements.isEmpty()) {
                _uiState.value = QueryEditorUiState.Idle
                return@launch
            }

            val results = mutableListOf<StatementResult>()
            var lastQueryResult: com.sphynxs.mydatabases.core.database.models.QueryResult? = null

            for (statement in statements) {
                val startTime = System.currentTimeMillis()
                val isQuery = detectQueryType(statement)

                try {
                    if (isQuery) {
                        val result = executeQueryUseCase(statement, emptyList()).getOrThrow()
                        lastQueryResult = result
                        results.add(
                            StatementResult(
                                sql = statement,
                                affectedRows = null,
                                executionTimeMs = System.currentTimeMillis() - startTime,
                                isQuery = true
                            )
                        )
                    } else {
                        val affectedRows = executeUpdateUseCase(statement, emptyList()).getOrThrow()
                        results.add(
                            StatementResult(
                                sql = statement,
                                affectedRows = affectedRows,
                                executionTimeMs = System.currentTimeMillis() - startTime,
                                isQuery = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = QueryEditorUiState.Error(
                        message = e.message ?: "Unknown error",
                        failedStatement = statement
                    )
                    return@launch
                }
            }

            // Agregar resultados:
            // Si todos son queries Y hay lastQueryResult → SelectResult
            // Caso contrario → UpdateSummary
            _uiState.value = if (results.all { it.isQuery } && lastQueryResult != null) {
                QueryEditorUiState.SelectResult(
                    result = lastQueryResult,
                    executionTimeMs = results.sumOf { it.executionTimeMs }
                )
            } else {
                QueryEditorUiState.UpdateSummary(results)
            }
        }
    }

    /**
     * Cancela ejecución en curso (UI-only — no termina JDBC query).
     */
    fun cancel() {
        executionJob?.cancel()
        _uiState.value = QueryEditorUiState.Idle
    }

    /**
     * Detecta si un statement es SELECT-like o INSERT/UPDATE/DELETE/DDL.
     *
     * @param sql Statement SQL trimmed
     * @return true si SELECT/SHOW/DESCRIBE/EXPLAIN/WITH, false caso contrario
     */
    private fun detectQueryType(sql: String): Boolean {
        val trimmed = sql.trim().uppercase()
        return trimmed.startsWith("SELECT") ||
                trimmed.startsWith("SHOW") ||
                trimmed.startsWith("DESCRIBE") ||
                trimmed.startsWith("EXPLAIN") ||
                trimmed.startsWith("WITH")
    }
}
