# Tasks: Editor Completion & Format

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1250 total across 3 PRs |
| Max PR size | 550 LOC (PR #3) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Delivery strategy | ask-always |
| Chain strategy | N/A (all PRs under 800-line budget) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: N/A
400-line budget risk: Low

### Workload Breakdown by PR

| PR | Scope | Estimated LOC | Budget Status |
|----|-------|---------------|---------------|
| PR #1 | SQL Formatter | ~300 | ✅ SAFE (37.5% of 800) |
| PR #2 | Schema + Provider | ~400 | ✅ SAFE (50% of 800) |
| PR #3 | Popup UI | ~550 | ✅ SAFE (68.75% of 800) |

**Analysis**: All 3 PRs individually fit within the 800-line review budget. Total ~1250 LOC split across independent PRs. No chaining required. Each PR is reviewable, testable, and independently deployable.

---

## PR #1: SQL Formatter (~300 LOC)

**Goal**: Ship Format end-to-end — toolbar button + Ctrl+Shift+F shortcut + history-atomic apply + pure formatter with 12 unit tests.

**Budget**: 800 lines | **Estimate**: ~300 LOC | **Status**: SAFE ✅

### Phase 1: Domain Layer — SqlKeywords + SqlFormatter (Pure, JVM-testable)

- [ ] 1.1 **TDD RED**: Write `SqlKeywordsTest::keywords_notEmpty()` (assert `SqlKeywords.KEYWORDS.isNotEmpty()`) — FAIL
- [ ] 1.2 **TDD GREEN**: Create `domain/editor/SqlKeywords.kt` with `object SqlKeywords { val KEYWORDS: Set<String> = setOf("SELECT", "FROM", ...) }` — ~80 lines, 75+ keywords
- [ ] 1.3 **TDD RED**: Write `SqlKeywordsTest::keywords_containsCanonicalSet()` (assert SELECT, FROM, WHERE, JOIN, INSERT, UPDATE, DELETE present) — FAIL
- [ ] 1.4 **TDD GREEN**: Add canonical keywords to `SqlKeywords.KEYWORDS` — PASS
- [ ] 1.5 **TDD RED**: Write `SqlKeywordsTest::keywords_allUppercase()` (assert all keywords == keyword.uppercase()) — FAIL
- [ ] 1.6 **TDD GREEN**: Ensure all keywords in `SqlKeywords.KEYWORDS` are UPPERCASE — PASS
- [ ] 1.7 **TDD RED**: Write `SqlFormatterTest::format_simpleSelectWithWhere_producesExpectedLayout()` (scenario 1: `select id from users where active = 1` → formatted with newlines + UPPERCASE) — FAIL
- [ ] 1.8 **TDD GREEN**: Create `domain/editor/SqlFormatter.kt` with `fun format(sql: String): String` skeleton returning `sql.uppercase()` — minimal logic to pass
- [ ] 1.9 **TDD RED**: Write `SqlFormatterTest::format_innerJoinWithOnPredicate_indentsOnUnderJoin()` (scenario 2: JOIN with ON indented 2 spaces) — FAIL
- [ ] 1.10 **TDD GREEN**: Implement token-stream rewriter in `SqlFormatter.format()`: tokenize via `SqlTokenizer`, UPPERCASE keywords, insert newlines before FROM/WHERE/JOIN/GROUP BY/ORDER BY/HAVING/LIMIT/UNION, indent ON subclauses 2 spaces
- [ ] 1.11 **TDD RED**: Write `SqlFormatterTest::format_nestedSubquery_uppercasesKeywordsWithoutDeepIndent()` (scenario 3: nested subquery, flat indent) — FAIL
- [ ] 1.12 **TDD GREEN**: Extend formatter to normalize nested subqueries (UPPERCASE keywords, no deep indent) — PASS
- [ ] 1.13 **TDD RED**: Write `SqlFormatterTest::format_stringLiterals_preservedVerbatim()` (scenario 4: strings with keyword-looking words preserved) — FAIL
- [ ] 1.14 **TDD GREEN**: Preserve STRING tokens byte-for-byte (don't uppercase inside strings) — PASS
- [ ] 1.15 **TDD RED**: Write `SqlFormatterTest::format_lineComment_preservedVerbatim()` (scenario 5: `-- foo select` preserved) — FAIL
- [ ] 1.16 **TDD GREEN**: Preserve COMMENT tokens byte-for-byte (don't uppercase inside comments) — PASS
- [ ] 1.17 **TDD RED**: Write `SqlFormatterTest::format_blockComment_preservedVerbatim()` (scenario 6: `/* FROM WHERE */` preserved) — FAIL
- [ ] 1.18 **TDD GREEN**: Preserve block comment contents unchanged — PASS
- [ ] 1.19 **TDD RED**: Write `SqlFormatterTest::format_projectionList_keptOnOneLine()` (scenario 7: `SELECT a, b, c` stays one line) — FAIL
- [ ] 1.20 **TDD GREEN**: Preserve user projection formatting (no comma breaks) — PASS
- [ ] 1.21 **TDD RED**: Write `SqlFormatterTest::format_isIdempotent_acrossAllGoldenFixtures()` (scenario 8: `format(format(x)) == format(x)`) — FAIL on non-idempotent edge case
- [ ] 1.22 **TDD GREEN**: Fix idempotency bugs (trim trailing whitespace, collapse 3+ blank lines to 1) — PASS
- [ ] 1.23 **REFACTOR**: Extract helper methods in `SqlFormatter` (e.g. `shouldInsertNewlineBefore(keyword)`, `indentLevel(context)`)

### Phase 2: Integration — EditorShortcuts + QueryEditorViewModel

- [ ] 2.1 **TDD RED**: Write `EditorShortcutsTest::mapKeyEvent_ctrlShiftF_returnsFormat()` (assert `Ctrl+Shift+F` → `ShortcutAction.Format`) — FAIL
- [ ] 2.2 **TDD GREEN**: Add `Format` case to `domain/editor/ShortcutAction.kt` sealed class (+1 line)
- [ ] 2.3 **TDD GREEN**: Map `Ctrl+Shift+F` to `Format` in `domain/editor/EditorShortcuts.kt` (+2 lines)
- [ ] 2.4 **TDD RED**: Write `QueryEditorViewModelTest::formatSql_validSql_returnsFormatted()` (unit test: `formatSql("select 1")` → `"SELECT 1"`) — FAIL
- [ ] 2.5 **TDD GREEN**: Add `suspend fun formatSql(currentText: String): String` to `ui/screens/queryeditor/QueryEditorViewModel.kt` wrapping `SqlFormatter.format()` on `Dispatchers.Default` (+15 lines)
- [ ] 2.6 **TDD RED**: Write `QueryEditorViewModelTest::formatSql_runsOnDefaultDispatcher()` (assert coroutine context) — FAIL
- [ ] 2.7 **TDD GREEN**: Ensure `formatSql()` uses `withContext(Dispatchers.Default)` — PASS

### Phase 3: UI Wiring — Toolbar Button + Shortcut

- [ ] 3.1 **TDD RED**: Write `QueryEditorScreenTest::formatButton_exists_whenTextNotBlank()` (E2E: assert Format button exists and enabled) — FAIL
- [ ] 3.2 **TDD GREEN**: Add Format toolbar button to `ui/screens/queryeditor/QueryEditorScreen.kt` (left pill, after Redo button) with `enabled = sqlText.text.isNotBlank()` (+10 lines)
- [ ] 3.3 Add `format_button_label` and `format_button_description` strings to `res/values/strings.xml` (+2 lines)
- [ ] 3.4 Add Spanish translations to `res/values-es/strings.xml` (`format_button_label = "Formato"`, `format_button_description = "Dar formato al SQL"`) (+2 lines)
- [ ] 3.5 **TDD RED**: Write `QueryEditorScreenTest::tappingFormatButton_replacesTextAndPushesHistory()` (E2E scenario 9: tap button → text formatted + undo restores) — FAIL
- [ ] 3.6 **TDD GREEN**: Wire Format button `onClick` to call `viewModel.formatSql()` and push to `EditorHistory` before apply (+10 lines in `QueryEditorScreen.kt`)
- [ ] 3.7 **TDD RED**: Write `QueryEditorScreenTest::ctrlShiftF_triggersFormat()` (E2E scenario 10: shortcut → same behavior as button) — FAIL
- [ ] 3.8 **TDD GREEN**: Wire `onShortcut(Format)` in `QueryEditorScreen.kt` to same `formatSql()` pathway (+5 lines)
- [ ] 3.9 **TDD RED**: Write `QueryEditorScreenTest::formatThenUndo_restoresOriginal()` (E2E scenario 11: format → Ctrl+Z → original text byte-for-byte) — FAIL
- [ ] 3.10 **TDD GREEN**: Ensure `pushHistory(currentSnapshot)` called BEFORE `formatSql()` apply — PASS
- [ ] 3.11 **TDD RED**: Write `QueryEditorScreenTest::formatWithMultiCursor_clearsCursorsAndFormatsFullText()` (E2E scenario 12: multi-cursor → format clears cursors) — FAIL
- [ ] 3.12 **TDD GREEN**: Clear `cursorPositions` when Format applied (+3 lines in format handler)

### Phase 4: SqlTokenizer Integration

- [ ] 4.1 Modify `ui/screens/queryeditor/components/SqlTokenizer.kt` to build keyword regex from `SqlKeywords.KEYWORDS` instead of inlined list (~10 lines changed)
- [ ] 4.2 Verify existing `SqlTokenizerTest` still passes (no regression in syntax highlighting)

### Phase 5: Verification

- [ ] 5.1 Run all unit tests: `SqlKeywordsTest` (3 tests), `SqlFormatterTest` (12 scenarios), `EditorShortcutsTest` (+1 test), `QueryEditorViewModelTest` (+2 tests)
- [ ] 5.2 Run E2E tests: `QueryEditorScreenTest` (+5 scenarios: button exists, tap button, Ctrl+Shift+F, undo, multi-cursor)
- [ ] 5.3 Manual smoke: Open editor → type `select id from users where active = 1` → tap Format button → verify UPPERCASE + newlines → Ctrl+Z → verify original restored

---

## PR #2: Completion Schema + Provider (~400 LOC)

**Goal**: Load schema snapshot + pure suggestion engine — NO UI yet (provider fully tested, staged for PR #3).

**Budget**: 800 lines | **Estimate**: ~400 LOC | **Status**: SAFE ✅

### Phase 1: Domain Layer — Data Classes

- [ ] 6.1 Create `domain/editor/CompletionSuggestion.kt` with `data class CompletionSuggestion(kind, label, insertText, typeLabel?)` and `enum class SuggestionKind { KEYWORD, TABLE, COLUMN }` (~20 lines)
- [ ] 6.2 Create `domain/editor/SchemaSnapshot.kt` with `data class SchemaSnapshot(tables, columnsByTable)` and `data class ColumnInfo(name, type)` (~15 lines)

### Phase 2: Domain Layer — LoadSchemaSnapshotUseCase

- [ ] 6.3 **TDD RED**: Write `LoadSchemaSnapshotUseCaseTest::invoke_validDatabase_returnsTables()` (mock `GetTablesUseCase` → assert snapshot.tables populated) — FAIL
- [ ] 6.4 **TDD GREEN**: Create `domain/usecase/LoadSchemaSnapshotUseCase.kt` with `suspend operator fun invoke(databaseName: String): Result<SchemaSnapshot>` wrapping `GetTablesUseCase` (~40 lines)
- [ ] 6.5 **TDD RED**: Write `LoadSchemaSnapshotUseCaseTest::invoke_invalidDatabase_returnsFailure()` (mock failure → assert `Result.failure`) — FAIL
- [ ] 6.6 **TDD GREEN**: Wrap use case calls in try-catch, return `Result.failure` on JDBC exception — PASS
- [ ] 6.7 **TDD RED**: Write `LoadSchemaSnapshotUseCaseTest::invoke_jdbcError_returnsFailure()` (mock throws → assert failure logged) — FAIL
- [ ] 6.8 **TDD GREEN**: Add error logging in catch block — PASS

### Phase 3: Domain Layer — SqlCompletionProvider

- [ ] 6.9 **TDD RED**: Write `SqlCompletionProviderTest::suggest_prefixSEL_returnsSELECTFirst()` (scenario 13: prefix="SEL", schema=null → first result is SELECT) — FAIL
- [ ] 6.10 **TDD GREEN**: Create `domain/editor/SqlCompletionProvider.kt` with `fun getSuggestions(prefix, context, cursorOffset, schema, limit=8): List<CompletionSuggestion>` skeleton filtering `SqlKeywords.KEYWORDS` by prefix (~50 lines)
- [ ] 6.11 **TDD RED**: Write `SqlCompletionProviderTest::suggest_afterFROM_ranksTablesFirst()` (scenario 17: context="SELECT * FROM ", schema={users,orders} → TABLE kind first) — FAIL
- [ ] 6.12 **TDD GREEN**: Add context detection: scan last keyword before cursor (FROM/JOIN/UPDATE/INTO → TABLE bias; SELECT/WHERE/ON/comma → COLUMN bias) (+30 lines)
- [ ] 6.13 **TDD RED**: Write `SqlCompletionProviderTest::suggest_afterSELECT_ranksColumnsFirst()` (scenario 18: context="SELECT " → COLUMN first) — FAIL
- [ ] 6.14 **TDD GREEN**: Implement ranking: context-biased kind order + alphabetical within kind (+20 lines)
- [ ] 6.15 **TDD RED**: Write `SqlCompletionProviderTest::suggest_insideString_returnsEmpty()` (cursor inside STRING token → empty list) — FAIL
- [ ] 6.16 **TDD GREEN**: Add string/comment suppression: tokenize context, check if `cursorOffset` inside STRING/COMMENT range → return `emptyList()` (+15 lines)
- [ ] 6.17 **TDD RED**: Write `SqlCompletionProviderTest::suggest_insideComment_returnsEmpty()` (cursor inside COMMENT → empty) — FAIL
- [ ] 6.18 **TDD GREEN**: Extend suppression for COMMENT tokens — PASS
- [ ] 6.19 **TDD RED**: Write `SqlCompletionProviderTest::suggest_nullSchema_keywordsOnly()` (schema=null → no TABLE/COLUMN suggestions) — FAIL
- [ ] 6.20 **TDD GREEN**: Guard schema access: if `schema == null`, only suggest keywords — PASS
- [ ] 6.21 **TDD RED**: Write `SqlCompletionProviderTest::suggest_respectsLimit8()` (scenario 28: 12 results available → return 8) — FAIL
- [ ] 6.22 **TDD GREEN**: Cap results at `limit` param (default 8) — PASS
- [ ] 6.23 **TDD RED**: Write remaining 10 provider tests (context ranking for JOIN/UPDATE/INTO/WHERE/ON/AND/OR/comma, column type display, alphabetical sort) — FAIL
- [ ] 6.24 **TDD GREEN**: Implement all context biases + alphabetical sorting + `typeLabel` population from `ColumnInfo.type` (+25 lines)
- [ ] 6.25 **REFACTOR**: Extract helper methods in `SqlCompletionProvider` (e.g. `detectContext(tokens, offset)`, `rankByContext(suggestions, context)`, `isInsideStringOrComment(tokens, offset)`)

### Phase 4: ViewModel Integration

- [ ] 6.26 **TDD RED**: Write `QueryEditorViewModelTest::loadSchema_validDatabase_populatesStateFlow()` (invoke `loadSchema("shop")` → assert `schemaSnapshot.value != null`) — FAIL
- [ ] 6.27 **TDD GREEN**: Add `schemaSnapshot: StateFlow<SchemaSnapshot?>` + `loadSchema(databaseName: String?)` to `ui/screens/queryeditor/QueryEditorViewModel.kt` (+25 lines)
- [ ] 6.28 **TDD RED**: Write `QueryEditorViewModelTest::loadSchema_nullDatabase_clearsStateFlow()` (invoke `loadSchema(null)` → assert `schemaSnapshot.value == null`) — FAIL
- [ ] 6.29 **TDD GREEN**: Add null check in `loadSchema`: if `databaseName == null`, set `_schemaSnapshot.value = null` — PASS
- [ ] 6.30 **TDD RED**: Write `QueryEditorViewModelTest::loadSchema_jdbcError_degradesToKeywordsOnly()` (mock failure → assert schema stays null, no crash) — FAIL
- [ ] 6.31 **TDD GREEN**: Wrap `loadSchemaSnapshotUseCase` in `.onFailure { /* log error */ }` — PASS
- [ ] 6.32 **TDD RED**: Write `QueryEditorViewModelTest::getSuggestions_delegatesToProvider()` (call `getSuggestions("SEL", "SELECT 1", 0)` → assert delegates to `SqlCompletionProvider`) — FAIL
- [ ] 6.33 **TDD GREEN**: Add `fun getSuggestions(prefix, context, cursorOffset): List<CompletionSuggestion>` delegating to `SqlCompletionProvider.getSuggestions(prefix, context, cursorOffset, schemaSnapshot.value)` (+10 lines)
- [ ] 6.34 **TDD RED**: Write `QueryEditorViewModelTest::executeStatements_ddl_triggersSchemaReload()` (execute "CREATE TABLE foo" → assert `loadSchema()` called again) — FAIL
- [ ] 6.35 **TDD GREEN**: Add `detectSchemaMutation(sql)` method in ViewModel: tokenize executed SQL, detect CREATE/ALTER/DROP TABLE or USE <db> → call `loadSchema()` (+30 lines)
- [ ] 6.36 **TDD RED**: Write `QueryEditorViewModelTest::executeStatements_useDb_switchesSchema()` (execute "USE other_db" → assert schema reloaded for new DB) — FAIL
- [ ] 6.37 **TDD GREEN**: Extract DB name from USE statement tokens, call `loadSchema(newDb)` — PASS

### Phase 5: Plumb databaseName to QueryEditorScreen

- [ ] 6.38 Add `databaseName: String?` parameter to `ui/screens/queryeditor/QueryEditorScreen.kt` signature (+1 line)
- [ ] 6.39 Call `viewModel.loadSchema(databaseName)` in `QueryEditorScreen` `LaunchedEffect(databaseName)` block (+5 lines)
- [ ] 6.40 Modify `ui/screens/workspace/WorkspaceOverlay.kt` to pass `databaseName = card.databaseName` when navigating to `QueryEditorScreen` (+5 lines)

### Phase 6: Verification

- [ ] 6.41 Run unit tests: `LoadSchemaSnapshotUseCaseTest` (3 tests), `SqlCompletionProviderTest` (16 scenarios), `QueryEditorViewModelTest` (+6 tests)
- [ ] 6.42 Manual smoke: Open editor with `databaseName = null` → verify no JDBC calls logged, no crash
- [ ] 6.43 Manual smoke: Open editor with `databaseName = "shop"` → verify schema loaded (check ViewModel logs)
- [ ] 6.44 Manual smoke: Execute "CREATE TABLE test (id INT)" → verify schema refresh triggered (check logs)

---

## PR #3: Completion Popup UI (~550 LOC)

**Goal**: Wire completion popup, auto-trigger, manual trigger (Ctrl+Space), arrow navigation, Enter/Tab/Esc, multi-cursor exclusion.

**Budget**: 800 lines | **Estimate**: ~550 LOC | **Status**: SAFE ✅

### Phase 1: CompletionPopup Component

- [ ] 7.1 **TDD RED**: Write `CompletionPopupTest::popup_rendersAllSuggestions()` (E2E: pass 3 suggestions → assert 3 rows visible) — FAIL
- [ ] 7.2 **TDD GREEN**: Create `ui/screens/queryeditor/components/CompletionPopup.kt` with `@Composable fun CompletionPopup(suggestions, selectedIndex, anchorOffset, onSuggestionClick, onDismiss)` rendering `Popup` + `LazyColumn` (~80 lines)
- [ ] 7.3 **TDD RED**: Write `CompletionPopupTest::popup_highlightsSelectedIndex()` (E2E: selectedIndex=1 → assert 2nd row has highlighted background) — FAIL
- [ ] 7.4 **TDD GREEN**: Apply background color to row at `selectedIndex` (+10 lines)
- [ ] 7.5 **TDD RED**: Write `CompletionPopupTest::popup_tapRow_invokesOnClick()` (E2E: tap row → assert `onSuggestionClick` called with correct suggestion) — FAIL
- [ ] 7.6 **TDD GREEN**: Wire `Modifier.clickable { onSuggestionClick(suggestion) }` on each row (+5 lines)
- [ ] 7.7 **TDD RED**: Write `CompletionPopupTest::popup_esc_invokesOnDismiss()` (E2E: press Esc → assert `onDismiss` called) — FAIL
- [ ] 7.8 **TDD GREEN**: Wire Esc key handling via parent `SqlCodeEditor` (popup is non-focusable) — defer to Phase 2
- [ ] 7.9 Add `contentDescription = completion_aria_label` to popup container (+3 lines)
- [ ] 7.10 Ensure tap targets ≥ 48dp (set `Modifier.height(48.dp)` on each row) (+2 lines)
- [ ] 7.11 **TDD RED**: Write `CompletionPopupTest::popup_moreThan8Suggestions_scrollable()` (E2E: pass 12 suggestions → assert LazyColumn scrollable) — FAIL
- [ ] 7.12 **TDD GREEN**: Set `LazyColumn` max height to `8 * 48.dp`, enable vertical scroll (+5 lines)

### Phase 2: SqlCodeEditor — Popup Anchoring + Navigation Routing

- [ ] 7.13 **TDD RED**: Write `SqlCodeEditorTest::popup_anchorsAtCursor()` (E2E: cursor at offset 5 → popup renders at `getBoundingBox(5).bottomLeft`) — FAIL
- [ ] 7.14 **TDD GREEN**: Extend `ui/screens/queryeditor/components/SqlCodeEditor.kt` signature with popup params (`showCompletionPopup, completionSuggestions, selectedSuggestionIndex, onCompletionAccept, onCompletionDismiss, onCompletionNavigate`) (+20 lines)
- [ ] 7.15 **TDD GREEN**: Anchor `CompletionPopup` at `textLayoutResult.getBoundingBox(selection.start).bottomLeft.let { IntOffset(it.x.toInt(), it.y.toInt()) }` (+10 lines)
- [ ] 7.16 **TDD RED**: Write `SqlCodeEditorTest::popup_followsScroll()` (E2E: scroll editor → popup repositions) — FAIL
- [ ] 7.17 **TDD GREEN**: Verify `getBoundingBox` automatically tracks scroll (existing behavior from multi-cursor) — PASS
- [ ] 7.18 **TDD RED**: Write `SqlCodeEditorTest::arrowKeys_routedToPopup_whenVisible()` (E2E: popup visible → Down key calls `onCompletionNavigate(+1)`) — FAIL
- [ ] 7.19 **TDD GREEN**: Extend `onPreviewKeyEvent` in `SqlCodeEditor` to intercept ↓↑ Enter Tab Esc when `showCompletionPopup == true` (+30 lines)
- [ ] 7.20 **TDD GREEN**: Wire Down → `onCompletionNavigate(+1)`, Up → `onCompletionNavigate(-1)`, Enter/Tab → `onCompletionAccept(suggestions[selectedIndex])`, Esc → `onCompletionDismiss()` (+15 lines)
- [ ] 7.21 **TDD RED**: Write `SqlCodeEditorTest::autoTrigger_debounces150ms()` (E2E: type "SEL" fast → popup appears after 150ms, not immediately) — FAIL
- [ ] 7.22 **TDD GREEN**: Add debounced flow in `SqlCodeEditor`: `snapshotFlow { text }.debounce(150).collectAsState()` triggers suggestion fetch (+20 lines)
- [ ] 7.23 **TDD RED**: Write `SqlCodeEditorTest::autoTrigger_prefixLengthGating()` (E2E: type "S" → popup NOT shown; type "SE" → popup shown) — FAIL
- [ ] 7.24 **TDD GREEN**: Add gating in debounced flow: only trigger if prefix ≥ 2 chars AND not inside string/comment AND `cursorPositions.isEmpty()` (+10 lines)

### Phase 3: QueryEditorScreen — State Management

- [ ] 7.25 Add state variables to `ui/screens/queryeditor/QueryEditorScreen.kt`: `var showCompletionPopup by remember { mutableStateOf(false) }`, `var completionSuggestions by remember { mutableStateOf(emptyList<CompletionSuggestion>()) }`, `var selectedSuggestionIndex by remember { mutableStateOf(0) }`, `var lastDismissedToken by remember { mutableStateOf<String?>(null) }` (+10 lines)
- [ ] 7.26 **TDD RED**: Write `CompletionPopupTest::typingSEL_showsSelectAtTop()` (E2E scenario 13: type "SEL" → popup visible, first item is SELECT) — FAIL
- [ ] 7.27 **TDD GREEN**: Wire auto-trigger: `LaunchedEffect(sqlText.text, sqlText.selection) { ... debounce 150ms ... if (prefix.length >= 2 && !isInString && !isInComment && cursorPositions.isEmpty() && lastDismissedToken != currentToken) { completionSuggestions = viewModel.getSuggestions(...); showCompletionPopup = true } }` (+25 lines)
- [ ] 7.28 **TDD RED**: Write `CompletionPopupTest::typingSingleChar_doesNotShowPopup()` (E2E scenario 14: type "S" → popup NOT shown) — FAIL
- [ ] 7.29 **TDD GREEN**: Verify 2-char gating in auto-trigger — PASS
- [ ] 7.30 **TDD RED**: Write `CompletionPopupTest::ctrlSpace_opensPopupWithEmptyPrefix()` (E2E scenario 16: Ctrl+Space → popup shown with full keyword list) — FAIL
- [ ] 7.31 **TDD GREEN**: Add `TriggerCompletion` case to `domain/editor/ShortcutAction.kt` (+1 line)
- [ ] 7.32 **TDD GREEN**: Map `Ctrl+Space` to `TriggerCompletion` in `domain/editor/EditorShortcuts.kt` (+2 lines)
- [ ] 7.33 **TDD GREEN**: Wire `onShortcut(TriggerCompletion)` in `QueryEditorScreen` to fetch suggestions (no length gating, no debounce) and show popup immediately (+10 lines)
- [ ] 7.34 **TDD RED**: Write `CompletionPopupTest::arrowNavigation_wrapsAtBothEnds()` (E2E scenario 22: Down from last item → wraps to first; Up from first → wraps to last) — FAIL
- [ ] 7.35 **TDD GREEN**: Wire `onCompletionNavigate`: `selectedSuggestionIndex = (selectedSuggestionIndex + direction).mod(completionSuggestions.size)` (+5 lines)
- [ ] 7.36 **TDD RED**: Write `CompletionPopupTest::enterKey_acceptsAndDismisses()` (E2E scenario 23: Enter → insert suggestion, dismiss popup) — FAIL
- [ ] 7.37 **TDD GREEN**: Wire `onCompletionAccept`: replace prefix with `suggestion.insertText`, update cursor position, set `showCompletionPopup = false` (+15 lines)
- [ ] 7.38 **TDD RED**: Write `CompletionPopupTest::tabKey_acceptsAndDismisses()` (E2E scenario 24: Tab → same as Enter) — FAIL
- [ ] 7.39 **TDD GREEN**: Route Tab key to same `onCompletionAccept` handler — PASS
- [ ] 7.40 **TDD RED**: Write `CompletionPopupTest::escDismiss_remembersTokenUntilChange()` (E2E scenario 25: Esc → popup dismisses, typing same token doesn't re-trigger) — FAIL
- [ ] 7.41 **TDD GREEN**: Wire `onCompletionDismiss`: set `showCompletionPopup = false`, `lastDismissedToken = currentIdentifierToken` (+10 lines)
- [ ] 7.42 **TDD GREEN**: Clear `lastDismissedToken` in auto-trigger when token changes (compare current token != last) (+5 lines)
- [ ] 7.43 **TDD RED**: Write `CompletionPopupTest::multiCursorActive_disablesCompletion()` (E2E scenario 26: multi-cursor → no popup on type or Ctrl+Space) — FAIL
- [ ] 7.44 **TDD GREEN**: Add `cursorPositions.isEmpty()` guard in auto-trigger AND manual trigger handlers — PASS
- [ ] 7.45 **TDD RED**: Write `CompletionPopupTest::withSchema_showsAllThreeKinds()` (E2E scenario 19: databaseName != null → popup shows KEYWORD + TABLE + COLUMN) — FAIL
- [ ] 7.46 **TDD GREEN**: Mock ViewModel schema StateFlow with test data, verify suggestions contain all three kinds — PASS
- [ ] 7.47 **TDD RED**: Write `CompletionPopupTest::nullDatabase_showsKeywordsOnly()` (E2E scenario 20: databaseName == null → only KEYWORD suggestions) — FAIL
- [ ] 7.48 **TDD GREEN**: Mock ViewModel with null schema, verify suggestions.all { it.kind == KEYWORD } — PASS

### Phase 4: i18n Strings

- [ ] 7.49 Add 6 completion strings to `res/values/strings.xml`: `completion_empty`, `completion_loading_schema`, `completion_keywords_only`, `completion_aria_label`, `completion_column_type_separator` (" : "), `completion_popup_title` (+6 lines)
- [ ] 7.50 Add Spanish translations to `res/values-es/strings.xml` (`completion_empty = "Sin sugerencias"`, `completion_keywords_only = "Solo palabras clave"`, etc.) (+6 lines)

### Phase 5: Column Type Display

- [ ] 7.51 **TDD RED**: Write `CompletionPopupTest::columnSuggestion_displaysTypeLabel()` (E2E scenario 21: COLUMN suggestion → rendered as `id : INT`) — FAIL
- [ ] 7.52 **TDD GREEN**: Format `label` in `SqlCompletionProvider` for COLUMN suggestions: `"${column.name} : ${column.type}"` (+5 lines in provider)
- [ ] 7.53 Verify `CompletionPopup` renders `suggestion.label` verbatim (already done) — PASS

### Phase 6: Edge Cases + Verification

- [ ] 7.54 **TDD RED**: Write `CompletionPopupTest::popup_repositionsWithCursorOnScroll()` (E2E scenario 27: scroll editor → popup repositions) — FAIL
- [ ] 7.55 **TDD GREEN**: Verify `getBoundingBox` tracking (existing behavior from multi-cursor) — PASS
- [ ] 7.56 **TDD RED**: Write `CompletionPopupTest::popup_capsVisibleRowsAndScrollsForRest()` (E2E scenario 28: 12 suggestions → 8 visible, 4 via scroll) — FAIL
- [ ] 7.57 **TDD GREEN**: Verify LazyColumn max height enforced — PASS
- [ ] 7.58 **TDD RED**: Write `CompletionPopupTest::emptyResults_showsLocalizedEmptyState()` (E2E scenario 30: no matches → show `completion_empty`) — FAIL
- [ ] 7.59 **TDD GREEN**: Add empty state row in `CompletionPopup` when `suggestions.isEmpty()` (+8 lines)
- [ ] 7.60 **TDD RED**: Write `QueryEditorScreenTest::formatButton_usesLocalizedLabel(locale=es)` (E2E scenario 29: Spanish locale → button label = "Formato") — FAIL (from PR #1, but verified here)
- [ ] 7.61 **TDD GREEN**: Verify string resource resolution works (no hardcoded strings) — PASS

### Phase 7: Final Integration

- [ ] 7.62 Run all E2E tests: `CompletionPopupTest` (14 scenarios), `SqlCodeEditorTest` (4 scenarios), `QueryEditorScreenTest` (1 i18n scenario)
- [ ] 7.63 Manual smoke: Open editor with DB → type "SEL" → verify popup appears with SELECT
- [ ] 7.64 Manual smoke: Arrow down/up → verify selection wraps
- [ ] 7.65 Manual smoke: Enter → verify suggestion inserted
- [ ] 7.66 Manual smoke: Esc → verify popup dismissed, re-typing same token does NOT re-trigger
- [ ] 7.67 Manual smoke: Ctrl+Space → verify popup opens immediately
- [ ] 7.68 Manual smoke: Multi-cursor (Ctrl+Shift+Click) → verify completion does NOT trigger
- [ ] 7.69 Manual smoke: Type inside string `"SEL"` → verify popup does NOT appear
- [ ] 7.70 Manual smoke: Editor with null DB → verify only keywords suggested

---

## Verification Plan (All PRs)

### Unit Tests (JVM, TDD Red-First)

| Module | Test File | Scenarios |
|--------|-----------|-----------|
| SqlKeywords | `SqlKeywordsTest.kt` | 3 tests: non-empty, canonical set, all uppercase |
| SqlFormatter | `SqlFormatterTest.kt` | 12 scenarios (spec 1-8, golden files, idempotency) |
| SqlCompletionProvider | `SqlCompletionProviderTest.kt` | 16 scenarios (spec 13-28: ranking, filtering, context) |
| EditorShortcuts | `EditorShortcutsTest.kt` | 2 new tests: Ctrl+Shift+F, Ctrl+Space |
| LoadSchemaSnapshotUseCase | `LoadSchemaSnapshotUseCaseTest.kt` | 3 tests: valid DB, invalid DB, JDBC error |
| QueryEditorViewModel | `QueryEditorViewModelTest.kt` | 8 new tests: schema load, format, suggestions, DDL detection |

**Total unit tests**: 44 new tests across 6 files

### E2E Tests (androidTest, Compose UI)

| Screen/Component | Test File | Scenarios |
|------------------|-----------|-----------|
| QueryEditorScreen | `QueryEditorScreenTest.kt` | 6 scenarios: format button, Ctrl+Shift+F, undo, multi-cursor, i18n |
| CompletionPopup | `CompletionPopupTest.kt` | 14 scenarios: auto-trigger, manual trigger, navigation, accept, dismiss, schema, multi-cursor, empty state |
| SqlCodeEditor | `SqlCodeEditorTest.kt` | 4 scenarios: anchoring, scroll, debounce, gating |

**Total E2E tests**: 24 new tests across 3 files

### Manual Smoke Tests (End-to-End UX)

**PR #1 (Format)**:
- [ ] Open editor → type unformatted SQL → tap Format button → verify UPPERCASE + newlines
- [ ] Ctrl+Shift+F → verify same result as button
- [ ] Format → Ctrl+Z → verify original restored byte-for-byte
- [ ] Multi-cursor active → Format → verify cursors cleared

**PR #2 (Schema + Provider)**:
- [ ] Open editor with `databaseName = null` → verify no JDBC calls (check logs)
- [ ] Open editor with `databaseName = "shop"` → verify schema loaded (check logs)
- [ ] Execute "CREATE TABLE test (id INT)" → verify schema refresh triggered
- [ ] Unit test all 16 provider scenarios via table-driven tests

**PR #3 (Popup)**:
- [ ] Type "SEL" → verify popup appears with SELECT
- [ ] Arrow down/up → verify selection wraps at ends
- [ ] Enter → verify suggestion inserted, popup dismissed
- [ ] Esc → verify popup dismissed, re-typing same token does NOT re-trigger
- [ ] Ctrl+Space → verify popup opens immediately
- [ ] Multi-cursor → verify completion does NOT trigger
- [ ] Type inside string `"SEL"` → verify popup does NOT appear
- [ ] Editor with null DB → verify only keywords suggested

---

## Implementation Order Rationale

### Why 3 PRs?

1. **PR #1 (Format)**: Self-contained, pure domain logic, no UI complexity. Ships a complete end-to-end feature (Format button + shortcut). Lowest risk, highest velocity.
2. **PR #2 (Schema + Provider)**: Stages completion backend with 100% unit test coverage. No UX change (invisible code). Proves provider correctness before UI wiring.
3. **PR #3 (Popup)**: Wires the tested provider to Compose UI. Keyboard navigation, auto-trigger, manual trigger, multi-cursor exclusion. Highest complexity, but built on solid foundation from PR #2.

### Why No Chaining?

- All 3 PRs fit within 800-line review budget (PR #1: 300 LOC, PR #2: 400 LOC, PR #3: 550 LOC).
- Each PR is independently reviewable and testable.
- PR #2 stages invisible code, but 100% coverage proves correctness.
- No need for feature branches or stacked PRs — linear merge to main.

### Dependencies

- PR #2 depends on PR #1 (SqlKeywords extraction).
- PR #3 depends on PR #2 (CompletionSuggestion, SqlCompletionProvider, schema StateFlow).
- All PRs are sequential merges to `main` (no parallel work needed).

---

## Next Step

✅ **Tasks artifact complete** — ready for `sdd-apply` phase.

All 30 spec scenarios mapped to tasks. TDD red-first workflow enforced. PR breakdown aligns with design. Review budget respected.

Orchestrator: Proceed to implementation (`sdd-apply`) for PR #1 (SQL Formatter).
