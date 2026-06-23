# Tasks: SQL Editor with Syntax Highlighting

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1260-1410 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Foundation) → PR 2 (Integration) |
| Delivery strategy | ask-always |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation: Tokenizer + ViewModel + ResultGrid extraction | PR 1 (~650-750 lines) | Testable core logic; TableViewer refactor proves ResultGrid works |
| 2 | Integration: Screen + WorkspaceCard + strings | PR 2 (~610-660 lines) | Depends on PR 1; connects UI + navigation |

**Recommended chain strategy**:
- **stacked-to-main**: both PRs can merge independently if sliced correctly (ResultGrid refactor is safe, ViewModel has full test coverage)
- **feature-branch-chain**: if team prefers coordinated final merge (PR 2 base = PR 1 branch)

User must choose before proceeding with `sdd-apply`.

---

## Phase 1: Foundation — Result Grid Extraction

- [x] 1.1 Create `ui/components/ResultGrid.kt` with public `@Composable ResultGrid(columns, rows, modifier)` signature
- [x] 1.2 Implement header row rendering with `LazyColumn` + `Row` + fixed 150.dp width per column
- [x] 1.3 Implement data rows with NULL display (`onSurfaceVariant` 60% alpha, "NULL" text)
- [x] 1.4 Add horizontal scroll modifier to parent container (both header + rows scroll together)
- [x] 1.5 Refactor `TableViewerScreen.kt` RowsTab → consume shared `ResultGrid` component
- [x] 1.6 Test: `TableViewerScreen` still renders rows identically after extraction
- [ ] 1.7 Write `ResultGridTest.kt` Compose UI test (header visible, rows align, NULL rendering, empty result, horizontal scroll)

## Phase 2: Foundation — SQL Tokenizer

- [x] 2.1 Create `ui/screens/queryeditor/components/SqlTokenizer.kt` with `tokenize(sql: String): List<SqlToken>` signature
- [x] 2.2 Define `SqlToken(range: IntRange, kind: TokenKind)` data class
- [x] 2.3 Define `TokenKind` enum: KEYWORD, STRING, COMMENT, NUMBER, IDENTIFIER, OPERATOR, WHITESPACE, PUNCTUATION
- [x] 2.4 Implement regex-based tokenizer: match strings (`'...'`, `"..."`), comments (`--`, `/* */`), numbers, keywords (case-insensitive), identifiers, operators, punctuation, whitespace
- [x] 2.5 Write `SqlTokenizerTest.kt` JVM unit tests: keywords (SELECT, FROM, WHERE, mixed case), strings (single/double quotes, escaped quotes), comments (line, block, nested), numbers (int, float), identifiers (plain, backtick), operators, punctuation, whitespace, edge cases (empty string, only whitespace, unclosed strings)
- [x] 2.6 Test: 100% coverage on tokenizer (pure function, highly testable)

## Phase 3: Foundation — Syntax Highlighting Transformation

- [x] 3.1 Create `ui/screens/queryeditor/components/SqlHighlightTransformation.kt` implementing `VisualTransformation`
- [x] 3.2 Implement `filter(text: AnnotatedString): TransformedText` that maps tokens → SpanStyle (KEYWORD → primary bold, STRING → tertiary, COMMENT → onSurfaceVariant italic 60% alpha, NUMBER → secondary, OPERATOR → onSurface)
- [x] 3.3 Use `MaterialTheme.colorScheme` for colors (no hard-coded colors)
- [x] 3.4 Return `TransformedText(annotated, OffsetMapping.Identity)`

## Phase 4: Foundation — Query Editor ViewModel

- [x] 4.1 Create `domain/models/StatementResult.kt` data class: `sql: String, affectedRows: Int?, executionTimeMs: Long, isQuery: Boolean`
- [x] 4.2 Create `ui/screens/queryeditor/QueryEditorUiState.kt` sealed class: `Idle | Running | SelectResult(QueryResult, executionTimeMs) | UpdateSummary(List<StatementResult>) | Error(message, failedStatement?)`
- [x] 4.3 Create `ui/screens/queryeditor/QueryEditorViewModel.kt` annotated `@HiltViewModel` with `ExecuteQueryUseCase`, `ExecuteUpdateUseCase` injected
- [x] 4.4 Add `StateFlow<QueryEditorUiState>` starting at `Idle`
- [x] 4.5 Implement `executeStatements(sql: String)`: split by `;`, trim, skip empty, detect SELECT vs non-SELECT (first keyword: SELECT/SHOW/DESCRIBE/EXPLAIN/WITH → query, else → update)
- [x] 4.6 Execute statements sequentially in `viewModelScope.launch`, track results in `List<StatementResult>`
- [x] 4.7 Aggregate results: if all queries → `SelectResult(last result)`, else → `UpdateSummary(all results)`, on error → `Error(message, statement)`
- [x] 4.8 Implement `cancel()`: cancel `executionJob`, set state to `Idle`
- [x] 4.9 Write `QueryEditorViewModelTest.kt` with mockk + Turbine: test single SELECT, single UPDATE, multi-SELECT (last shown), mixed (UPDATE + SELECT), error on first/middle/last statement, cancel during execution, state transitions `Idle → Running → Result/Error`

## Phase 5: Integration — SQL Code Editor Component

- [ ] 5.1 Create `ui/screens/queryeditor/components/SqlCodeEditor.kt` composable
- [ ] 5.2 Render `BasicTextField` with `FontFamily.Monospace`, `visualTransformation = SqlHighlightTransformation(tokens, colorScheme)`
- [ ] 5.3 Add 300ms debounced re-tokenization on text change (launch on `Default` dispatcher, update tokens state)
- [ ] 5.4 Handle Tab key: `onKeyEvent` modifier intercepts Tab, inserts four spaces at caret, returns `true`
- [ ] 5.5 Configure `KeyboardOptions(imeAction = ImeAction.Default)` to preserve Tab on soft keyboards
- [ ] 5.6 Test: editor renders, highlights keywords/strings/comments, Tab inserts spaces

## Phase 6: Integration — Query Editor Screen

- [ ] 6.1 Create `ui/screens/queryeditor/QueryEditorScreen.kt` composable accepting `connectionId: String, initialSql: String?`
- [ ] 6.2 Hoist `QueryEditorViewModel` via `hiltViewModel()`
- [ ] 6.3 Render `Column` layout: TopAppBar (connection name), `SqlCodeEditor`, Toolbar (`Run` button enabled when text non-empty and state is Idle, `Cancel` button visible when state is Running), Result pane
- [ ] 6.4 Wire Run button → `viewModel.executeStatements(sql)`
- [ ] 6.5 Wire Cancel button → `viewModel.cancel()`
- [ ] 6.6 Collect `viewModel.uiState` and render result pane: `Idle` → empty, `Running` → CircularProgressIndicator, `SelectResult` → `ResultGrid(result.columns, result.rows)`, `UpdateSummary` → summary table (columns: SQL, Rows Affected, Time), `Error` → error card with message + failed statement
- [ ] 6.7 Test: `QueryEditorScreenTest.kt` Compose UI test (render editor, type SQL, click Run, verify result grid appears for SELECT, verify summary table for UPDATE, verify error card for invalid SQL)

## Phase 7: Integration — Workspace Card Integration

- [ ] 7.1 Modify `ui/workspace/WorkspaceCard.kt`: add `Query(id: String, connectionId: String, initialSql: String?)` sealed variant
- [ ] 7.2 Modify `ui/workspace/WorkspaceManager.kt`: handle `openQueryCard(connectionId, initialSql?)` → append `WorkspaceCard.Query` with unique id, focus it
- [ ] 7.3 Modify `ui/workspace/WorkspaceManager.kt`: handle close/focus for Query cards (same logic as Table cards)
- [ ] 7.4 Modify workspace renderer to pattern-match `WorkspaceCard.Query` → render `QueryEditorScreen(connectionId, initialSql)`
- [ ] 7.5 Modify `ui/screens/databases/NewQueryScreen.kt`: replace placeholder body with `WorkspaceManager.openQueryCard(connectionId, initialSql = "")`
- [ ] 7.6 Test: open New Query from bottom-nav → WorkspaceCard.Query opens with empty editor, type SQL → state isolated, open second query card → both coexist with independent state

## Phase 8: Integration — Strings

- [ ] 8.1 Add to `res/values/strings.xml`: `query_run`, `query_running`, `query_cancel`, `query_rows_label`, `query_time_label`, `query_affected_rows`, `query_error_title`, `query_multi_statement_warning`
- [ ] 8.2 Add Spanish translations to `res/values-es/strings.xml`: `Ejecutar`, `Ejecutando`, `Cancelar`, `Filas`, `Tiempo`, `Filas afectadas`, `Error de consulta`, `Advertencia: punto y coma dentro de cadenas puede dividir incorrectamente`
- [ ] 8.3 Wire strings to UI: Run/Cancel buttons, result grid labels, error card title

## Phase 9: Testing & Verification

- [ ] 9.1 Run all unit tests: `SqlTokenizerTest`, `QueryEditorViewModelTest`
- [ ] 9.2 Run all Compose UI tests: `ResultGridTest`, `QueryEditorScreenTest`
- [ ] 9.3 Manual smoke test: connect to test DB, run single SELECT → verify result grid, run single UPDATE → verify summary table, run multi-statement `UPDATE ...; SELECT ...;` → verify SELECT result shown, run invalid SQL → verify error card, click Cancel during long query → verify UI resets to Idle
- [ ] 9.4 Verify `TableViewerScreen` still renders rows correctly after ResultGrid extraction (regression check)
- [ ] 9.5 Verify two query cards can coexist with independent state (workspace isolation check)
- [ ] 9.6 Verify theme colors apply to syntax highlighting in both light and dark themes
