package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.editor.EditorHistory
import com.sphynxs.mydatabases.domain.editor.EditorSnapshot
import com.sphynxs.mydatabases.domain.models.StatementResult
import com.sphynxs.mydatabases.domain.usecases.ExecuteBatchStatementsUseCase
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
 * - Ejecutar multi-statement SQL (split por `;`, secuencial en la MISMA conexión)
 * - Detectar SELECT vs non-SELECT (primer keyword)
 * - Agregar resultados (SelectResult si todos queries, UpdateSummary si algún update)
 * - Manejar errores (stop on first failure)
 * - Cancelación UI-only (cancela coroutine, no termina JDBC query)
 *
 * State machine:
 * Idle → Running → SelectResult/UpdateSummary/Error
 *      → cancel() → Idle
 *
 * @param executeBatchStatementsUseCase Use case para ejecutar múltiples statements en la misma conexión
 *
 * @author israel-icm
 * @date 2026-06-23
 */
@HiltViewModel
class QueryEditorViewModel @Inject constructor(
    private val executeBatchStatementsUseCase: ExecuteBatchStatementsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<QueryEditorUiState>(QueryEditorUiState.Idle)
    val uiState: StateFlow<QueryEditorUiState> = _uiState.asStateFlow()

    private var executionJob: Job? = null
    
    // Editor history for undo/redo
    private val editorHistory = EditorHistory(maxSnapshots = 100, coalescingWindowMs = 500)
    
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Exception handler para catchear TODAS las excepciones no manejadas
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = QueryEditorUiState.Error(
            message = throwable.message ?: "Unknown error",
            failedStatement = ""
        )
    }

    /**
     * Ejecuta uno o más SQL statements en la MISMA conexión.
     *
     * Split naive por `;` (sin parsing — `;` dentro de strings puede romper).
     * Ejecuta secuencialmente en la misma conexión (permite USE DATABASE + SELECT).
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

            try {
                // Ejecutar TODOS los statements en la MISMA conexión
                val batchResults = executeBatchStatementsUseCase(statements).getOrThrow()
                
                // Convertir BatchStatementResult a StatementResult
                val results = batchResults.map { batchResult ->
                    StatementResult(
                        sql = batchResult.sql,
                        affectedRows = batchResult.affectedRows,
                        executionTimeMs = batchResult.executionTimeMs,
                        isQuery = batchResult.isQuery
                    )
                }
                
                // Obtener último QueryResult (si existe)
                val lastQueryResult = batchResults
                    .lastOrNull { it.isQuery }
                    ?.queryResult

                // Agregar resultados:
                // 1. Si todos son queries Y hay lastQueryResult → SelectResult
                // 2. Si ninguno es query Y todos tienen affectedRows = 0 → Success (ej: USE DATABASE)
                // 3. Caso contrario → UpdateSummary
                _uiState.value = when {
                    results.all { it.isQuery } && lastQueryResult != null -> {
                        QueryEditorUiState.SelectResult(
                            result = lastQueryResult,
                            executionTimeMs = results.sumOf { it.executionTimeMs }
                        )
                    }
                    results.none { it.isQuery } && results.all { (it.affectedRows ?: 0) == 0 } -> {
                        QueryEditorUiState.Success(
                            message = "Query executed successfully (${results.size} statement${if (results.size > 1) "s" else ""})",
                            executionTimeMs = results.sumOf { it.executionTimeMs }
                        )
                    }
                    else -> {
                        QueryEditorUiState.UpdateSummary(results)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = QueryEditorUiState.Error(
                    message = e.message ?: "Unknown error",
                    failedStatement = statements.firstOrNull() ?: ""
                )
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
     * Push a snapshot to history when text changes.
     * 
     * Coalescing is handled by EditorHistory automatically.
     *
     * @param text Current text
     * @param selection Current selection
     * @param cursorPositions Multi-cursor positions
     */
    fun pushHistory(text: String, selection: TextRange, cursorPositions: List<Int>) {
        val snapshot = EditorSnapshot(
            text = text,
            selection = selection,
            cursorPositions = cursorPositions
        )
        editorHistory.push(snapshot)
        updateHistoryState()
    }
    
    /**
     * Undo the last change.
     * 
     * @return Snapshot to restore, or null if nothing to undo
     */
    fun undo(): EditorSnapshot? {
        val snapshot = editorHistory.undo()
        updateHistoryState()
        return snapshot
    }
    
    /**
     * Redo the last undone change.
     * 
     * @return Snapshot to restore, or null if nothing to redo
     */
    fun redo(): EditorSnapshot? {
        val snapshot = editorHistory.redo()
        updateHistoryState()
        return snapshot
    }
    
    private fun updateHistoryState() {
        _canUndo.value = editorHistory.canUndo()
        _canRedo.value = editorHistory.canRedo()
    }
    
    /**
     * Format SQL text to normalized form (UPPERCASE keywords, newlines before major clauses).
     *
     * Runs on Dispatchers.Default to avoid blocking the main thread for large SQL texts.
     * Pure transformation via SqlFormatter.format() — no side effects.
     *
     * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 9-12)
     * Design: ADR 2 — Formatter is pure function, runs off main thread
     *
     * @param currentText Input SQL text (any case, any whitespace)
     * @return Formatted SQL (UPPERCASE keywords, major clause newlines, idempotent)
     */
    suspend fun formatSql(currentText: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
        com.sphynxs.mydatabases.domain.editor.SqlFormatter.format(currentText)
    }
}
