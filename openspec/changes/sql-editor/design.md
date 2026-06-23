# Design: SQL Editor with Syntax Highlighting

## Technical Approach

Build a professional SQL editor using **custom tokenizer + `VisualTransformation`** over `BasicTextField`. This approach integrates natively with the existing `BrandedColors` / `MaterialTheme` system, requires zero third-party dependencies, and delivers testable syntax highlighting without WebView overhead.

The editor reuses the existing domain layer (`ExecuteQueryUseCase` + `ExecuteUpdateUseCase`) and follows the established MVVM + Clean Architecture pattern from `TableViewerViewModel`. Multi-statement execution splits SQL by `;` (naive approach — document limitations), dispatches SELECT vs non-SELECT via keyword inspection, and aggregates results into a unified UI state.

This design references and extends the prior `ui-implementation/specs/query-runner/spec.md` by adding syntax highlighting, monospace font, and tab key preservation.

## Architecture Decisions

### Decision: Custom SQL Tokenizer over Third-Party Libraries

**Choice**: Implement a lightweight regex-based tokenizer (`SqlTokenizer.tokenize(sql: String): List<SqlToken>`)

**Alternatives considered**:
- `Qawaz/compose-code-editor` — rejected: GitHub Packages distribution friction, no SQL lexer included, heavy dependency footprint
- `hossain-khan/android-compose-highlight` (Highlight.js in WebView) — rejected: designed for display, not editing; latency from WebView bridge unacceptable
- Plain `TextField` with no highlighting — rejected: user explicitly requested professional syntax highlighting

**Rationale**: 
- SQL keyword set is small (~250 MySQL/MariaDB reserved words) → tokenizer implementation is ~200 LOC
- Pure function → JVM unit testable without Compose harness (aligns with `strict_tdd: true`)
- Zero supply-chain risk, zero APK weight increase
- Full control over color tokens — integrates cleanly with `BrandedColors` and `MaterialTheme.colorScheme`
- Future-proof for autocomplete (reuses same token stream)

### Decision: VisualTransformation for Syntax Highlighting

**Choice**: Apply token-to-`SpanStyle` mapping via `VisualTransformation` on `BasicTextField`

**Alternatives considered**:
- `AnnotatedString` directly on `TextFieldValue.text` — rejected: requires copying/merging user input with annotations on every keystroke, violates single-source-of-truth for TextField state
- Custom `DrawScope` / Canvas rendering — rejected: complex, doesn't integrate with TextField's selection/cursor system

**Rationale**:
- `VisualTransformation` is the canonical Compose API for altering text display without mutating state
- Integrates seamlessly with `BasicTextField` (already used elsewhere in the app)
- `MaterialTheme.colorScheme` and `BrandedColors` are directly accessible for theme-aware highlighting
- Performance manageable with debounce (see below)

### Decision: Debounced Re-Tokenization (300ms)

**Choice**: Re-tokenize SQL input with 300ms debounce on text changes

**Alternatives considered**:
- Re-tokenize on every keystroke — rejected: O(n) cost on >5KB queries causes jank on low-end devices
- Line-level incremental tokenization — rejected: complexity not justified for v1 (SQL queries typically <2KB)
- No debounce, background dispatcher only — rejected: still computes on every keystroke, wastes CPU

**Rationale**:
- 300ms debounce balances responsiveness with performance (user pauses → highlights update)
- Aligns with industry standards (VS Code, JetBrains IDEs use similar delays)
- Background `Default` dispatcher offloads work from UI thread
- Can optimize to line-level tokenization in future if profiling shows need

### Decision: Multi-Statement Execution via Naive `;` Split

**Choice**: Split SQL by `;`, trim, skip empty statements, execute sequentially

**Alternatives considered**:
- Reject multi-statement queries with error — considered but deferred: users will paste multi-statement scripts (common workflow)
- Parse `;` inside strings/comments correctly — rejected for v1: adds significant tokenizer complexity, rare edge case
- Use `Statement.execute()` with `ALLOW_MULTI_QUERIES` — rejected: security risk (SQL injection surface), not supported consistently across MySQL/MariaDB drivers

**Rationale**:
- Naive split covers 95% of user workflows (`;` rarely appears inside strings in SQL scripts)
- Document limitation clearly: "Semicolons inside strings/comments may split incorrectly — use single statements for complex queries"
- Low implementation cost, enables common paste-and-run workflow
- Can upgrade to full parser in v2 if user feedback demands it

### Decision: UI-Only Cancel Semantics (v1)

**Choice**: "Cancel" button sets ViewModel state to Idle, does NOT cancel in-flight JDBC query

**Alternatives considered**:
- Call `Statement.cancel()` from another thread — rejected for v1: requires refactoring `DatabaseEngine` interface to expose `Statement`, adds threading complexity

**Rationale**:
- JDBC `Statement.cancel()` requires thread-safe access to the active statement — current `MySQLEngine` does not expose this
- UI-only cancel provides immediate user feedback (screen resets to editor)
- In-flight query continues on DB server but result is discarded
- Document limitation: "Cancel resets the UI but does not terminate the database query"
- Can add proper cancellation in follow-up after refactoring `DatabaseEngine`

## Data Flow

```
User Input (SQL text)
    │
    ├──> [Debounce 300ms] ──> SqlTokenizer.tokenize()
    │                              │
    │                              ├──> List<SqlToken>
    │                              │         │
    │                              │         └──> SqlHighlightTransformation
    │                              │                   │
    │                              │                   └──> AnnotatedString (with SpanStyle)
    │                              │                             │
    │                              │                             └──> BasicTextField (visual only)
    │
    └──> [User clicks Run] ──> QueryEditorViewModel.executeStatements()
                                    │
                                    ├──> Split by `;` → List<Statement>
                                    │
                                    ├──> For each statement:
                                    │       │
                                    │       ├──> Trim, skip if empty
                                    │       │
                                    │       ├──> Detect type (SELECT/SHOW/etc → query, else → update)
                                    │       │
                                    │       ├──> ExecuteQueryUseCase OR ExecuteUpdateUseCase
                                    │       │
                                    │       └──> Collect Result<QueryResult> or Result<Int>
                                    │
                                    └──> Aggregate results:
                                            │
                                            ├──> All queries → SelectResult(last QueryResult)
                                            │
                                            ├──> Any updates → UpdateSummary(List<UpdateResult>)
                                            │
                                            └──> Error → Error(message)
                                                    │
                                                    └──> StateFlow<QueryEditorUiState>
                                                            │
                                                            └──> QueryEditorScreen renders:
                                                                    ├── ResultGrid (for SELECT)
                                                                    ├── UpdateSummaryTable (for INSERT/UPDATE/DELETE)
                                                                    └── ErrorCard (for failures)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/screens/queryeditor/QueryEditorScreen.kt` | Create | Main composable: TopAppBar + SqlCodeEditor + Toolbar (Run/Cancel) + ResultPane |
| `ui/screens/queryeditor/QueryEditorViewModel.kt` | Create | `@HiltViewModel` — manages `TextFieldValue`, executes multi-statement SQL, emits `StateFlow<QueryEditorUiState>` |
| `ui/screens/queryeditor/QueryEditorUiState.kt` | Create | Sealed class: `Idle | Running | SelectResult(QueryResult) | UpdateSummary(List<StatementResult>) | Error(String)` |
| `ui/screens/queryeditor/components/SqlCodeEditor.kt` | Create | `BasicTextField` with `FontFamily.Monospace` + `SqlHighlightTransformation` |
| `ui/screens/queryeditor/components/SqlTokenizer.kt` | Create | Pure function: `tokenize(sql: String): List<SqlToken>` — regex-based, returns `SqlToken(range: IntRange, kind: TokenKind)` |
| `ui/screens/queryeditor/components/SqlHighlightTransformation.kt` | Create | `VisualTransformation` impl — maps `SqlToken` → `SpanStyle` via `MaterialTheme.colorScheme` + `BrandedColors` |
| `ui/screens/queryeditor/components/ResultGrid.kt` | Create | Extracted from `TableViewerScreen.RowsTab` — reusable LazyColumn with headers + horizontal scroll |
| `ui/screens/databases/NewQueryScreen.kt` | Modify | **Replace** placeholder body with `QueryEditorScreen(connectionId)` OR rename file to `QueryEditorScreen.kt` if navigation is refactored |
| `ui/navigation/MyDataBasesNavHost.kt` | Modify | Keep `Routes.NewQuery` binding, remove duplicate `Routes.QueryEditor` placeholder (if consolidating routes) |
| `ui/navigation/Routes.kt` | Modify | Delete `Routes.QueryEditor` OR keep both if separate navigation paths required (decision pending) |
| `res/values/strings.xml` | Modify | Add: `query_run`, `query_running`, `query_cancel`, `query_rows_label`, `query_time_label`, `query_affected_rows`, `query_error_title`, `query_multi_statement_warning` |
| `res/values-es/strings.xml` | Modify | Add Spanish translations for new query editor strings |
| `domain/models/StatementResult.kt` | Create | Data class: `sql: String, affectedRows: Int?, executionTimeMs: Long, isQuery: Boolean` (for multi-statement summary) |
| `test/.../queryeditor/SqlTokenizerTest.kt` | Create | JVM unit tests for tokenizer (keywords, strings, comments, operators, edge cases) |
| `test/.../queryeditor/QueryEditorViewModelTest.kt` | Create | ViewModel tests with mockk + Turbine (state transitions, multi-statement execution, error handling) |
| `androidTest/.../queryeditor/QueryEditorScreenTest.kt` | Create | Compose UI test (render editor, type SQL, click Run, verify result grid) |

## Interfaces / Contracts

### SqlToken

```kotlin
/**
 * Token produced by SQL tokenizer.
 *
 * @property range Character range in the input string (IntRange)
 * @property kind Token classification (keyword, string, comment, etc.)
 */
data class SqlToken(
    val range: IntRange,
    val kind: TokenKind
)

enum class TokenKind {
    KEYWORD,        // SELECT, FROM, WHERE, etc.
    STRING,         // 'foo', "bar"
    COMMENT,        // -- comment, /* block */
    NUMBER,         // 123, 45.67
    IDENTIFIER,     // table_name, column_name, `backtick_id`
    OPERATOR,       // =, +, -, *, /, <, >, <=, >=, !=, AND, OR
    WHITESPACE,     // spaces, tabs, newlines
    PUNCTUATION     // ( ) , ; .
}
```

### SqlTokenizer

```kotlin
object SqlTokenizer {
    /**
     * Tokenizes SQL input into a list of classified tokens.
     *
     * Pure function — no side effects, fully testable in JVM unit tests.
     *
     * @param sql Raw SQL string
     * @return List of tokens with ranges and kinds
     */
    fun tokenize(sql: String): List<SqlToken> {
        // Regex-based implementation:
        // 1. Match string literals: '...' or "..." (escaped quotes: '', \")
        // 2. Match comments: -- to EOL, /* ... */
        // 3. Match numbers: \d+(\.\d+)?
        // 4. Match keywords: SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|... (case-insensitive)
        // 5. Match identifiers: [a-zA-Z_][a-zA-Z0-9_]* or `...`
        // 6. Match operators: =, !=, <>, <=, >=, +, -, *, /, AND, OR, NOT
        // 7. Match punctuation: ( ) , ; .
        // 8. Match whitespace: \s+
        //
        // Order matters: strings/comments first (greedy), then keywords, then identifiers
    }
}
```

### SqlHighlightTransformation

```kotlin
class SqlHighlightTransformation(
    private val tokens: List<SqlToken>,
    private val colorScheme: ColorScheme,
    private val brandedColors: BrandedColors? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        
        tokens.forEach { token ->
            val style = when (token.kind) {
                TokenKind.KEYWORD -> SpanStyle(
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                TokenKind.STRING -> SpanStyle(
                    color = colorScheme.tertiary
                )
                TokenKind.COMMENT -> SpanStyle(
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontStyle = FontStyle.Italic
                )
                TokenKind.NUMBER -> SpanStyle(
                    color = brandedColors?.numberColor ?: colorScheme.secondary
                )
                TokenKind.OPERATOR -> SpanStyle(
                    color = colorScheme.onSurface
                )
                // ... other token kinds
            }
            
            builder.addStyle(style, token.range.first, token.range.last + 1)
        }
        
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
```

### QueryEditorUiState

```kotlin
sealed class QueryEditorUiState {
    data object Idle : QueryEditorUiState()
    data object Running : QueryEditorUiState()
    
    data class SelectResult(
        val result: QueryResult,
        val executionTimeMs: Long
    ) : QueryEditorUiState()
    
    data class UpdateSummary(
        val results: List<StatementResult>
    ) : QueryEditorUiState()
    
    data class Error(
        val message: String,
        val failedStatement: String? = null
    ) : QueryEditorUiState()
}
```

### QueryEditorViewModel

```kotlin
@HiltViewModel
class QueryEditorViewModel @Inject constructor(
    private val executeQueryUseCase: ExecuteQueryUseCase,
    private val executeUpdateUseCase: ExecuteUpdateUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<QueryEditorUiState>(QueryEditorUiState.Idle)
    val uiState: StateFlow<QueryEditorUiState> = _uiState.asStateFlow()
    
    private var executionJob: Job? = null
    
    /**
     * Executes one or more SQL statements.
     *
     * Splits by `;`, detects SELECT vs non-SELECT, executes sequentially,
     * aggregates results into SelectResult or UpdateSummary.
     *
     * @param sql Raw SQL input (may contain multiple statements)
     */
    fun executeStatements(sql: String) {
        executionJob?.cancel()
        executionJob = viewModelScope.launch {
            _uiState.value = QueryEditorUiState.Running
            
            val statements = sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            val results = mutableListOf<StatementResult>()
            var lastQueryResult: QueryResult? = null
            
            for (statement in statements) {
                val startTime = System.currentTimeMillis()
                val isQuery = detectQueryType(statement)
                
                try {
                    if (isQuery) {
                        val result = executeQueryUseCase(statement, emptyList()).getOrThrow()
                        lastQueryResult = result
                        results.add(StatementResult(
                            sql = statement,
                            affectedRows = null,
                            executionTimeMs = System.currentTimeMillis() - startTime,
                            isQuery = true
                        ))
                    } else {
                        val affectedRows = executeUpdateUseCase(statement, emptyList()).getOrThrow()
                        results.add(StatementResult(
                            sql = statement,
                            affectedRows = affectedRows,
                            executionTimeMs = System.currentTimeMillis() - startTime,
                            isQuery = false
                        ))
                    }
                } catch (e: Exception) {
                    _uiState.value = QueryEditorUiState.Error(
                        message = e.message ?: "Unknown error",
                        failedStatement = statement
                    )
                    return@launch
                }
            }
            
            // Aggregate results
            _uiState.value = if (lastQueryResult != null && results.all { it.isQuery }) {
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
     * Cancels in-flight execution (UI-only — does NOT cancel JDBC query).
     */
    fun cancel() {
        executionJob?.cancel()
        _uiState.value = QueryEditorUiState.Idle
    }
    
    private fun detectQueryType(sql: String): Boolean {
        val trimmed = sql.trim().uppercase()
        return trimmed.startsWith("SELECT") ||
               trimmed.startsWith("SHOW") ||
               trimmed.startsWith("DESCRIBE") ||
               trimmed.startsWith("EXPLAIN") ||
               trimmed.startsWith("WITH")
    }
}
```

### StatementResult

```kotlin
/**
 * Result of executing a single SQL statement in multi-statement mode.
 *
 * @property sql The executed SQL statement (for display in summary table)
 * @property affectedRows Rows affected (for INSERT/UPDATE/DELETE), null for queries
 * @property executionTimeMs Execution time in milliseconds
 * @property isQuery True if SELECT/SHOW/etc, false if INSERT/UPDATE/DELETE/DDL
 */
data class StatementResult(
    val sql: String,
    val affectedRows: Int?,
    val executionTimeMs: Long,
    val isQuery: Boolean
)
```

### ResultGrid (Extracted Component)

```kotlin
/**
 * Reusable data grid for query results.
 *
 * Extracted from TableViewerScreen.RowsTab — displays columns + rows
 * with horizontal scroll and fixed column width.
 *
 * @param columns List of column names
 * @param rows List of rows (each row is a Map<String, Any?>)
 * @param modifier Modifier for the grid container
 */
@Composable
fun ResultGrid(
    columns: List<String>,
    rows: List<Map<String, Any?>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.horizontalScroll(rememberScrollState())) {
        // Header row
        item {
            Row {
                columns.forEach { column ->
                    Text(
                        text = column,
                        modifier = Modifier.width(150.dp).padding(8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            HorizontalDivider()
        }
        
        // Data rows
        items(rows) { row ->
            Row {
                columns.forEach { column ->
                    Text(
                        text = row[column]?.toString() ?: "NULL",
                        modifier = Modifier.width(150.dp).padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (row[column] == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| **Unit** | `SqlTokenizer.tokenize()` | JVM tests: keywords (case-insensitive), string literals (`'foo'`, `"bar"`, escaped quotes), comments (`--`, `/* */`), numbers (int, float), identifiers (plain, backtick), operators, punctuation, whitespace. Edge cases: empty string, only whitespace, nested comments, unclosed strings. |
| **Unit** | `QueryEditorViewModel` state transitions | Mockk + Turbine: mock `ExecuteQueryUseCase` / `ExecuteUpdateUseCase`, assert state flows `Idle → Running → SelectResult/UpdateSummary/Error`. Test single statement, multi-statement (all queries, all updates, mixed), error on first/middle/last statement. |
| **Unit** | Multi-statement split logic | Assert splitting by `;` (valid cases), edge cases (trailing `;`, multiple `;` in a row, no `;`). Document limitation: does NOT handle `;` inside strings. |
| **Integration** | `SqlHighlightTransformation` with tokenizer | Compose UI test (or screenshot test if available): render `BasicTextField` with sample SQL, assert colors via semantics or snapshot. |
| **Integration** | `ResultGrid` extraction | Compose UI test: render with sample columns/rows, assert column headers visible, assert NULL rendering, assert horizontal scroll enabled. |
| **E2E** | Manual smoke test | Connect to test DB, type `SELECT * FROM users;`, click Run, verify result grid renders. Type `INSERT INTO ...`, verify affected rows summary. Type invalid SQL, verify error message. Click Cancel during long query, verify UI resets. |

**Coverage expectations**:
- `SqlTokenizer`: 100% (pure function, highly testable)
- `QueryEditorViewModel`: >85% (state machine, critical business logic)
- UI components: >70% (Compose UI tests, exclude manual-only cases like Cancel semantics)

## Migration / Rollout

No data migration required. This is a new feature with no existing persisted state.

**Rollout steps**:

1. **Phase 1: Extract `ResultGrid`** (prep work, low risk)
   - Refactor `TableViewerScreen.RowsTab` → shared `ResultGrid` component
   - Update `TableViewerScreen` to consume `ResultGrid`
   - Test: existing TableViewer screen still works identically
   - **Benefit**: Reduces risk of final integration, improves codebase before adding editor

2. **Phase 2: Build Tokenizer + ViewModel** (core logic, highly testable)
   - Implement `SqlTokenizer` with full JVM test coverage
   - Implement `QueryEditorViewModel` with mockk/Turbine tests
   - **No UI yet** → pure logic can be validated in isolation

3. **Phase 3: Build UI Components**
   - Implement `SqlCodeEditor` with `SqlHighlightTransformation`
   - Implement `QueryEditorScreen` composable
   - Wire to `NewQueryScreen.kt` OR rename file to `QueryEditorScreen.kt`
   - Test: Compose UI tests for editor rendering, syntax highlighting

4. **Phase 4: Navigation Integration**
   - Decide route consolidation: keep `Routes.NewQuery` only, or keep both
   - Update `MyDataBasesNavHost.kt`
   - Test: navigate from bottom-nav, verify editor screen appears

5. **Phase 5: Manual QA + Documentation**
   - Smoke test with real DB connections (MySQL, MariaDB)
   - Test multi-statement edge cases (paste from external SQL files)
   - Document limitations in user-facing strings: `;` inside strings may split incorrectly, Cancel does not terminate DB query
   - Update any in-app help/tutorial screens if they exist

**Feature flag**: Not required (this is a standalone new screen, does not affect existing features).

**Backward compatibility**: Full backward compatibility — existing screens unchanged, new screen is additive.

## Open Questions

- [x] **Route consolidation**: Keep both `Routes.NewQuery` and `Routes.QueryEditor`, or merge into one? **Recommendation**: Merge into `Routes.NewQuery` (already in bottom-nav), delete the unused `QueryEditor` placeholder.
  
- [x] **Multi-statement result display**: If user runs `SELECT ...; UPDATE ...;`, show last SELECT result only, or show UpdateSummary? **Recommendation**: Show UpdateSummary if ANY non-SELECT exists (user likely cares about affected rows).

- [x] **Tab key behavior**: Android IME's "next" action may swallow tab character. Preserve tab in SQL editor? **Recommendation**: Yes — configure `BasicTextField` with `KeyboardOptions(imeAction = ImeAction.Default)` and handle tab key via `onKeyEvent` modifier.

- [x] **NULL display in ResultGrid**: Show as grayed-out "NULL" or empty cell? **Recommendation**: Show "NULL" in `onSurfaceVariant` color with 60% alpha (matches industry standard, avoids confusion with empty strings).

- [ ] **Workspace integration**: The exploration mentions `WorkspaceCard.Query` — is this a separate feature (saved queries) or part of this change? **Needs clarification from user/orchestrator** before implementation.

- [ ] **ResultGrid extraction timing**: Do this refactor inside `sql-editor` change, or as a separate prep PR? **Recommendation**: Inside this change (documented as Phase 1 in rollout), avoids coordination overhead.
