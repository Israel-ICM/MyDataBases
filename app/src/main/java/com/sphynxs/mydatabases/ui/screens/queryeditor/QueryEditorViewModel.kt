package com.sphynxs.mydatabases.ui.screens.queryeditor

import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sphynxs.mydatabases.domain.completion.CompletionSuggestion
import com.sphynxs.mydatabases.domain.completion.SchemaSnapshot
import com.sphynxs.mydatabases.domain.completion.SqlCompletionProvider
import com.sphynxs.mydatabases.domain.editor.BracketMatcher
import com.sphynxs.mydatabases.domain.editor.EditorHistory
import com.sphynxs.mydatabases.domain.editor.EditorSnapshot
import com.sphynxs.mydatabases.domain.editor.MultiCursorEngine
import com.sphynxs.mydatabases.domain.models.StatementResult
import com.sphynxs.mydatabases.domain.usecases.ExecuteBatchStatementsUseCase
import com.sphynxs.mydatabases.domain.usecases.LoadSchemaSnapshotUseCase
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlToken
import com.sphynxs.mydatabases.ui.screens.queryeditor.components.SqlTokenizer
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
    private val executeBatchStatementsUseCase: ExecuteBatchStatementsUseCase,
    private val loadSchemaSnapshotUseCase: LoadSchemaSnapshotUseCase
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
    
    // SQL text state (para toolbar flotante)
    private val _sqlText = MutableStateFlow("")
    val sqlText: StateFlow<String> = _sqlText.asStateFlow()
    
    // Cursor positions (para multi-cursor - toolbar flotante)
    private val _cursorPositions = MutableStateFlow<List<Int>>(emptyList())
    val cursorPositions: StateFlow<List<Int>> = _cursorPositions.asStateFlow()
    
    // Schema snapshot for completion
    private val _schemaSnapshot = MutableStateFlow<SchemaSnapshot?>(null)
    val schemaSnapshot: StateFlow<SchemaSnapshot?> = _schemaSnapshot.asStateFlow()

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

    // Format toolbar button trigger (tick counter, StateFlow always replays last value)
    private val _formatRequest = MutableStateFlow(0)
    val formatRequest: StateFlow<Int> = _formatRequest.asStateFlow()

    /**
     * Request a Format apply from outside QueryEditorScreen's local Compose state
     * (e.g. the QueryEditorToolbarRow Format button, a separate composable that
     * shares this ViewModel instance but cannot mutate the screen's local
     * `sqlText`/`cursorPositions` state directly).
     *
     * QueryEditorScreen observes [formatRequest] and applies the format using the
     * exact same pathway as the Ctrl+Shift+F shortcut (spec scenario 10: same
     * behavior regardless of entry point).
     */
    fun requestFormat() {
        _formatRequest.value += 1
    }
    
    /**
     * Load schema snapshot for the current database.
     *
     * Triggers LoadSchemaSnapshotUseCase to fetch tables (eager) and columns (lazy per-table).
     * Updates _schemaSnapshot StateFlow for completion provider.
     *
     * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 19-20)
     * Design: ADR 5 — Lazy-load schema (tables eager, columns on-demand)
     *
     * @param databaseName Target database (nullable - if null, completion shows keywords only)
     */
    fun loadSchema(databaseName: String?) {
        viewModelScope.launch {
            if (databaseName == null) {
                _schemaSnapshot.value = null
                return@launch
            }
            
            val result = loadSchemaSnapshotUseCase(databaseName)
            result.fold(
                onSuccess = { snapshot ->
                    _schemaSnapshot.value = snapshot
                },
                onFailure = {
                    // Graceful degradation: keep previous snapshot or null
                    // Completion will show keywords only
                }
            )
        }
    }
    
    /**
     * Get completion suggestions for the current editor state.
     *
     * Delegates to SqlCompletionProvider.getSuggestions() with prefix, context, and schema.
     * Pure function call - no side effects.
     *
     * Spec: openspec/changes/editor-completion-and-format/spec.md (scenarios 13-28)
     * Design: ADR 3 — Completion provider is pure function
     *
     * @param prefix Text before cursor (e.g., "SEL", "users")
     * @param context Full text before cursor for context detection (e.g., "SELECT * FROM ")
     * @return List of suggestions sorted by relevance (top 20, ranked by context)
     */
    fun getSuggestions(prefix: String, context: String): List<CompletionSuggestion> {
        return SqlCompletionProvider.getSuggestions(prefix, context, _schemaSnapshot.value)
    }
    
    companion object {
        /**
         * Compute bracket pair at cursor position.
         *
         * Task 4.2.2 — TDD GREEN: Pure function to compute bracket pair from text and cursor offset.
         *
         * @param text SQL text
         * @param cursorOffset Cursor position
         * @return Pair of (openBracketOffset, closeBracketOffset) or null if not adjacent to bracket
         */
        fun computeBracketPairAtCursor(text: String, cursorOffset: Int): Pair<Int, Int>? {
            val tokens = SqlTokenizer.tokenize(text)
            val matchingOffset = BracketMatcher.findMatchingBracket(text, tokens, cursorOffset)
                ?: return null
            
            // Determine which offset is the bracket we're adjacent to
            // BracketMatcher checks both charBeforeCursor and charAtCursor
            val charBeforeCursor = if (cursorOffset - 1 in text.indices) text[cursorOffset - 1] else null
            val charAtCursor = if (cursorOffset in text.indices) text[cursorOffset] else null
            
            val openBrackets = setOf('(', '[', '{')
            val closeBrackets = setOf(')', ']', '}')
            val quoteBrackets = setOf('\'', '"', '`')
            
            // Find which bracket we're adjacent to and its offset
            val (bracketOffset, isOpen) = when {
                charBeforeCursor in openBrackets -> (cursorOffset - 1) to true
                charBeforeCursor in closeBrackets -> (cursorOffset - 1) to false
                charBeforeCursor in quoteBrackets -> (cursorOffset - 1) to true // quotes are both open and close
                charAtCursor in openBrackets -> cursorOffset to true
                charAtCursor in closeBrackets -> cursorOffset to false
                charAtCursor in quoteBrackets -> cursorOffset to true
                else -> return null
            }
            
            // Return pair with open bracket first, close bracket second
            return if (isOpen) {
                Pair(bracketOffset, matchingOffset)
            } else {
                Pair(matchingOffset, bracketOffset)
            }
        }
    }

    // ========================================
    // Find & Replace State (Phase 5.2 — PR #5)
    // ========================================

    private val _findReplaceOpen = MutableStateFlow(false)
    val findReplaceOpen: StateFlow<Boolean> = _findReplaceOpen.asStateFlow()

    private val _findReplaceMode = MutableStateFlow(FindReplaceMode.FIND)
    val findReplaceMode: StateFlow<FindReplaceMode> = _findReplaceMode.asStateFlow()

    private val _findQuery = MutableStateFlow("")
    val findQuery: StateFlow<String> = _findQuery.asStateFlow()

    private val _replaceText = MutableStateFlow("")
    val replaceText: StateFlow<String> = _replaceText.asStateFlow()

    private val _findMatches = MutableStateFlow<List<TextRange>>(emptyList())
    val findMatches: StateFlow<List<TextRange>> = _findMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(-1)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    private val _matchCase = MutableStateFlow(false)
    val matchCase: StateFlow<Boolean> = _matchCase.asStateFlow()

    private val _wholeWord = MutableStateFlow(false)
    val wholeWord: StateFlow<Boolean> = _wholeWord.asStateFlow()

    private val _useRegex = MutableStateFlow(false)
    val useRegex: StateFlow<Boolean> = _useRegex.asStateFlow()

    fun openFind() {
        _findReplaceOpen.value = true
        _findReplaceMode.value = FindReplaceMode.FIND
    }

    fun openReplace() {
        _findReplaceOpen.value = true
        _findReplaceMode.value = FindReplaceMode.REPLACE
    }

    fun closeFind() {
        _findReplaceOpen.value = false
        _findMatches.value = emptyList()
        _currentMatchIndex.value = -1
    }

    fun updateFindQuery(query: String, text: String) {
        _findQuery.value = query
        
        // Update matches
        if (query.isEmpty()) {
            _findMatches.value = emptyList()
            _currentMatchIndex.value = -1
        } else {
            val matches = com.sphynxs.mydatabases.domain.editor.FindReplaceEngine.findAllMatches(
                text = text,
                query = query,
                matchCase = _matchCase.value,
                wholeWord = _wholeWord.value,
                useRegex = _useRegex.value
            )
            _findMatches.value = matches
            _currentMatchIndex.value = if (matches.isNotEmpty()) 0 else -1
        }
    }

    fun updateReplaceText(text: String) {
        _replaceText.value = text
    }

    fun toggleMatchCase(text: String) {
        _matchCase.value = !_matchCase.value
        updateFindQuery(_findQuery.value, text) // Refresh matches
    }

    fun toggleWholeWord(text: String) {
        _wholeWord.value = !_wholeWord.value
        updateFindQuery(_findQuery.value, text)
    }

    fun toggleUseRegex(text: String) {
        _useRegex.value = !_useRegex.value
        updateFindQuery(_findQuery.value, text)
    }

    fun navigateToNextMatch() {
        val matches = _findMatches.value
        if (matches.isEmpty()) return
        
        _currentMatchIndex.value = (_currentMatchIndex.value + 1) % matches.size
    }

    fun navigateToPreviousMatch() {
        val matches = _findMatches.value
        if (matches.isEmpty()) return
        
        val newIndex = _currentMatchIndex.value - 1
        _currentMatchIndex.value = if (newIndex < 0) matches.size - 1 else newIndex
    }

    /**
     * Replace current match (FR-10).
     * Returns new text and updated cursor position.
     */
    fun replaceCurrentMatch(text: String): Pair<String, Int>? {
        val matches = _findMatches.value
        val currentIndex = _currentMatchIndex.value
        
        if (matches.isEmpty() || currentIndex < 0 || currentIndex >= matches.size) {
            return null
        }
        
        val currentMatch = matches[currentIndex]
        val newText = com.sphynxs.mydatabases.domain.editor.FindReplaceEngine.replaceOne(
            text = text,
            matchRange = currentMatch,
            replaceText = _replaceText.value
        )
        
        // Update cursor position to after replacement
        val newCursorPos = currentMatch.start + _replaceText.value.length
        
        // Refresh matches with new text
        updateFindQuery(_findQuery.value, newText)
        
        return Pair(newText, newCursorPos)
    }

    /**
     * Replace all matches (FR-11: atomic operation for single undo).
     * Returns new text.
     */
    fun replaceAllMatches(text: String): String {
        if (_findMatches.value.isEmpty()) return text
        
        val newText = com.sphynxs.mydatabases.domain.editor.FindReplaceEngine.replaceAll(
            text = text,
            query = _findQuery.value,
            replaceText = _replaceText.value,
            matchCase = _matchCase.value,
            wholeWord = _wholeWord.value,
            useRegex = _useRegex.value
        )
        
        // Refresh matches (will be empty after replace all)
        updateFindQuery(_findQuery.value, newText)
        
        return newText
    }
    
    // Phase 6.3: Multi-cursor ViewModel Integration
    
    // Internal state for multi-cursor (exposed via cursorSelections parameter in UI)
    private var _targetColumn: Int = 0 // Cached column for Ctrl+Alt+Down/Up (Task 6.3.9)
    
    /**
     * Handle Ctrl+Alt+Down: Add cursor below, preserving column (MC-1).
     * 
     * @param layout Text layout result for line calculations
     * @param primarySelection Current primary cursor
     * @param currentSelections Current cursor selections list
     * @return Updated selections list, or null if no-op
     */
    fun <T : Any> handleAddCursorBelow(
        layout: T,
        primarySelection: TextRange,
        currentSelections: List<TextRange>
    ): List<TextRange>? {
        val newCursor = MultiCursorEngine.addCursorBelow(layout, primarySelection, _targetColumn)
        return if (newCursor != null) {
            currentSelections + newCursor
        } else {
            null // No-op at last line
        }
    }
    
    /**
     * Handle Ctrl+Alt+Up: Add cursor above, preserving column (MC-2).
     */
    fun <T : Any> handleAddCursorAbove(
        layout: T,
        primarySelection: TextRange,
        currentSelections: List<TextRange>
    ): List<TextRange>? {
        val newCursor = MultiCursorEngine.addCursorAbove(layout, primarySelection, _targetColumn)
        return if (newCursor != null) {
            currentSelections + newCursor
        } else {
            null // No-op at first line
        }
    }
    
    /**
     * Handle Ctrl+D: Select next occurrence (MC-3, MC-4, MC-5).
     * 
     * First press: Select word at cursor (MC-4.1)
     * Second press: Add next occurrence (MC-4.2)
     * No more occurrences: Show snackbar (MC-5.1)
     * 
     * @param text Current editor text
     * @param selection Current primary selection
     * @param tokens Tokens for word boundary detection
     * @param currentSelections Current cursor selections list
     * @param onShowSnackbar Callback to show "No more occurrences" snackbar
     * @return Updated selection and selections list
     */
    fun handleSelectNextOccurrence(
        text: String,
        selection: TextRange,
        tokens: List<SqlToken>,
        currentSelections: List<TextRange>,
        onShowSnackbar: (String) -> Unit
    ): Pair<TextRange, List<TextRange>> {
        // MC-4.1: First press — select word if collapsed
        if (selection.collapsed) {
            val wordRange = MultiCursorEngine.selectWordAtOffset(text, selection.start, tokens)
            return Pair(wordRange, currentSelections)
        }
        
        // MC-4.2: Second press — find next occurrence
        val selectedText = text.substring(selection.start, selection.end)
        val nextOccurrence = MultiCursorEngine.findNextOccurrence(
            text = text,
            selectedText = selectedText,
            fromOffset = selection.end
        )
        
        return if (nextOccurrence != null) {
            // Append next occurrence to selections
            Pair(selection, currentSelections + nextOccurrence)
        } else {
            // MC-5.1: No more occurrences — show snackbar
            onShowSnackbar("No more occurrences") // TODO: i18n in Phase 6.6
            Pair(selection, currentSelections)
        }
    }
    
    /**
     * Update cached target column for Ctrl+Alt+Down/Up (Task 6.3.9).
     */
    fun updateTargetColumn(column: Int) {
        _targetColumn = column
    }
    
    /**
     * Update SQL text from UI (para toolbar flotante).
     */
    fun updateSqlText(text: String) {
        _sqlText.value = text
    }
    
    /**
     * Update cursor positions from UI (para toolbar flotante).
     */
    fun updateCursorPositions(positions: List<Int>) {
        _cursorPositions.value = positions
    }
    
    /**
     * Clear cursor positions (para toolbar flotante Clear button).
     */
    fun clearCursorPositions() {
        _cursorPositions.value = emptyList()
    }
}

enum class FindReplaceMode {
    FIND,
    REPLACE
}
