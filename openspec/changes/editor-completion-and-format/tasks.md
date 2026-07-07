# Tasks: Editor Completion & Format

## Review Workload Forecast

**[REVISED 2026-07-07]** PR #1 recomputed per design.md ADR 7 (statement-split + depth-tracked list/clause breaking, full statement pretty-printing). PR #2/#3 unaffected — their only PR #1 dependency (`SqlKeywords`) is untouched by ADR 7.

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1600–1670 total across 3 PRs (was ~1250) |
| Max PR size | ~650–720 LOC (PR #1, revised — was 550 LOC / PR #3) |
| 400-line budget risk | Medium (PR #1 now ~81–90% of the 800-line budget; PR #2/#3 unchanged, still Low) |
| Chained PRs recommended | No |
| Delivery strategy | ask-on-risk |
| Chain strategy | N/A (all 3 PRs still individually under the 800-line budget) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: N/A
400-line budget risk: Medium

**Note**: design.md's Risks table flags PR #1's ~650–720 LOC as "consumes nearly all margin" and offers an OPTIONAL split into PR #1a (clause-newline + FROM/WHERE indent + SELECT-projection break, ~450 LOC) and PR #1b (statement split + INSERT/VALUES list-breaking, ~250 LOC) if the maintainer prefers a smaller diff. This is NOT required — PR #1 as a single unit stays under the 800-line ceiling — but it's surfaced here per `ask-on-risk` since the margin is tight. If the maintainer wants the split, re-run sdd-tasks to break Phase 1B below into two PR-scoped sub-phases before `sdd-apply`.

### Workload Breakdown by PR

| PR | Scope | Estimated LOC | Budget Status |
|----|-------|---------------|---------------|
| PR #1 | SQL Formatter (revised, ADR 7) | ~650–720 | ✅ SAFE, tight margin (~81–90% of 800) |
| PR #2 | Schema + Provider | ~400 | ✅ SAFE (50% of 800) — unchanged |
| PR #3 | Popup UI | ~550 | ✅ SAFE (68.75% of 800) — unchanged |

**Analysis**: All 3 PRs individually still fit within the 800-line review budget after the PR #1 revision. Total ~1600–1670 LOC split across independent PRs (was ~1250). No chaining required — PR #1's margin is tight but the maintainer has not requested a smaller diff. Each PR remains reviewable, testable, and independently deployable.

---

## PR #1: SQL Formatter (~650–720 LOC) — **[REVISED 2026-07-07]**

**Goal**: Ship Format end-to-end — toolbar button + Ctrl+Shift+F shortcut + history-atomic apply + pure formatter with full statement pretty-printing (multi-statement split, INSERT/VALUES paren-list breaking, SELECT projection breaking, FROM/WHERE indent) per design.md ADR 7.

**Budget**: 800 lines | **Estimate**: ~650–720 LOC (was ~300 LOC) | **Status**: SAFE ✅ (tight margin)

**Progress note**: Per Engram `sdd/editor-completion-and-format/apply-progress` (obs #1977, #2135): Phase 1 (`SqlKeywords`), Phase 2 (Integration), Phase 3 (UI wiring), Phase 4 (tokenizer) were already executed against the ORIGINAL flat-formatter design. Phases 1 (SqlKeywords only), 2, 3, 4 are correct and DONE below — carried forward, not regenerated. Phase 1B below SUPERSEDES the old flat-formatter portion of Phase 1 (old tasks 1.7–1.23) with the new ADR 7 depth-tracked state machine.

### Phase 1: Domain Layer — SqlKeywords (DONE — unaffected by the revision)

- [x] 1.1 **TDD RED**: `SqlKeywordsTest::keywords_notEmpty()` — DONE
- [x] 1.2 **TDD GREEN**: `domain/editor/SqlKeywords.kt` with `KEYWORDS: Set<String>` (~80 lines, 75+ keywords) — DONE
- [x] 1.3 **TDD RED**: `SqlKeywordsTest::keywords_containsCanonicalSet()` — DONE
- [x] 1.4 **TDD GREEN**: canonical keywords added — DONE
- [x] 1.5 **TDD RED**: `SqlKeywordsTest::keywords_allUppercase()` — DONE
- [x] 1.6 **TDD GREEN**: uppercase enforced — DONE

### Phase 1B: Domain Layer — SqlFormatter Rewrite per ADR 7 — **SUPERSEDES old tasks 1.7–1.23**

> The previously-implemented flat token-stream rewriter (old tasks 1.7–1.23) UPPERCASEd keywords and inserted flat clause newlines only — it does NOT satisfy the revised spec (multi-statement split, list-breaking, unconditional SELECT/FROM/WHERE indent). Treat `SqlFormatter.kt`'s current body as a WIP starting point, not a finished deliverable. Algorithm reference: design.md ADR 7 pseudocode.

- [x] 1.7 **TDD RED**: Write `SqlFormatterTest::format_maintainerExample_producesExactLayout()` (spec Scenario 8a — two-statement golden fixture: `INSERT INTO ... VALUES (...);SELECT ... FROM ... WHERE ...;`) — FAIL
- [x] 1.8 **TDD GREEN**: Implement `splitTopLevelStatements(tokens)` pre-pass — track `parenDepth`, split segments at top-level `;` (depth 0), drop the `;`, detect trailing semicolon (ADR 7 Pass 0)
- [x] 1.9 **TDD GREEN**: Wire `format(sql)`: tokenize once → split into segments → `map { formatStatement(it) }` → join with `;\n` → restore trailing `;` if present — PASS 1.7
- [x] 1.10 **TDD RED**: Write `SqlFormatterTest::format_fromTable_indentedUnderFrom()` (Scenario 8d) — FAIL
- [x] 1.11 **TDD GREEN**: Add `indentLevel`/`atLineStart` state to `formatStatement`; on `FROM`/`WHERE` at `parenDepth == 0` → `breakLine()`, `indentLevel = 1` (shared code path, no per-keyword duplication) — PASS
- [x] 1.12 **TDD RED**: Write `SqlFormatterTest::format_whereCondition_indentedUnderWhere()` (Scenario 8e) — FAIL
- [x] 1.13 **TDD GREEN**: Confirm WHERE body indent reuses the 1.11 code path — PASS
- [x] 1.14 **TDD RED**: Write `SqlFormatterTest::format_projectionList_breaksColumnPerLine()` (Scenario 7a — **supersedes + renames** old `format_projectionList_keptOnOneLine`; delete the old test) — FAIL
- [x] 1.15 **TDD GREEN**: On `SELECT` at `parenDepth == 0` → `listMode = PROJECTION; indentLevel = 1; breakLine()`; on `,` when `listMode == PROJECTION && parenDepth == 0` → append `,` + `breakLine()` — PASS
- [x] 1.16 **TDD RED**: Write `SqlFormatterTest::format_insertColumnList_breaksPerLine()` (Scenario 8b) and `format_valuesTuple_breaksPerLine()` (Scenario 8c) — FAIL
- [x] 1.17 **TDD GREEN**: Add `pendingTableCapture`/`pendingListTrigger` flags + `activeListDepth: Int?` + `listMode = PAREN_LIST`, triggered on `(` following `INSERT INTO <table>` or `VALUES`; close list on matching `)` — ONE shared list-breaking path for both triggers (ADR 7 — no per-keyword duplication) — PASS
- [x] 1.18 **TDD RED**: Write `SqlFormatterTest::format_singleItemList_stillBreaks()` (Scenario 7b: `INSERT INTO t (id) VALUES (1);`) — FAIL
- [x] 1.19 **TDD GREEN**: Confirm list-breaking has no count==1 special case — PASS (comma-driven logic naturally covers it)
- [x] 1.20 **TDD RED**: Write `SqlFormatterTest::format_deepNesting_leftFlatDepth1Only()` (Scenario 8f: subquery nested inside `VALUES(...)`) — FAIL
- [x] 1.21 **TDD GREEN**: Ensure `activeListDepth` tracks only ONE active depth; `(`/`)` beyond it append flat (no breaking, keyword-case normalization only) — PASS
- [x] 1.22 **Rewrite expected strings** (regression, no new scenario) for the 7 existing tests impacted by unconditional SELECT/FROM/WHERE breaking: `format_simpleSelectWithWhere_producesExpectedLayout`, `format_innerJoinWithOnPredicate_indentsOnUnderJoin`, `format_nestedSubquery_uppercasesKeywordsWithoutDeepIndent` (name unchanged per spec Scenario 3 — do NOT rename despite design.md's optional suggestion), `format_stringLiterals_preservedVerbatim`, `format_lineComment_preservedVerbatim`, `format_blockComment_preservedVerbatim`, `format_trailingSemicolon_preserved` — see design.md ADR 7 Backward Compatibility table for the exact reasoning per scenario — PASS
- [x] 1.23 **Verify unaffected** (no expected-string change needed): `format_isIdempotent_acrossAllGoldenFixtures`, `format_emptyString_returnsEmpty`, `format_onlyWhitespace_returnsEmpty`, `format_mixedCaseKeywords_allUppercase` — confirm still PASS
- [x] 1.24 **TDD RED**: Write `SqlFormatterTest::format_isIdempotent_onMultiStatementAndBrokenLists()` (Scenario 8 companion) — FAIL
- [x] 1.25 **TDD GREEN**: Fix any idempotency gaps in statement re-splitting/list re-breaking (decisions must derive only from KEYWORD/PUNCTUATION identity, never WHITESPACE content, per ADR 7 idempotency rationale) — PASS
- [x] 1.26 **REFACTOR**: Extract `shouldInsertNewlineBefore(kw)`, `breakLine()`, `isListBreakComma()`, `isListOpenParen()` helpers per ADR 7 pseudocode

> **Implementation note (sdd-apply, 2026-07-07)**: "PASS"/"FAIL" markers above describe the TDD
> red-first INTENT of each task per the strict-TDD convention already established for this
> change (see prior apply-progress obs #1977/#2135) — actual test execution is explicitly
> DEFERRED to the maintainer (no `./gradlew` invocation performed this session, per standing
> instruction). Additionally, 3 ordering bugs were found in design.md ADR 7's pseudocode by
> manually tracing it against the golden example BEFORE writing code; design.md was corrected
> in place (see ADR 7 "[CORRECTED during sdd-apply]" note) and the implementation follows the
> corrected version. All 20 tests in `SqlFormatterTest.kt` are written; none have been compiled
> or run.

**Net test tally for `SqlFormatterTest.kt`**: 20 scenarios total = 7 updated-expected-string + 1 superseded/renamed + 4 unaffected (all from the original 12) + 8 net-new (7b, 8-companion, 8a, 8b, 8c, 8d, 8e, 8f).

### Phase 2: Integration — EditorShortcuts + QueryEditorViewModel (DONE — unaffected, public contract unchanged)

- [x] 2.1 **TDD RED**: `EditorShortcutsTest::mapKeyEvent_ctrlShiftF_returnsFormat()` — DONE
- [x] 2.2 **TDD GREEN**: `Format` case added to `ShortcutAction.kt` — DONE
- [x] 2.3 **TDD GREEN**: `Ctrl+Shift+F` mapped in `EditorShortcuts.kt` — DONE
- [x] 2.4 **TDD RED**: `QueryEditorViewModelTest::formatSql_validSql_returnsFormatted()` — DONE
- [x] 2.5 **TDD GREEN**: `formatSql()` added to `QueryEditorViewModel.kt`, wraps `SqlFormatter.format()` — DONE (interface unchanged by ADR 7 — no rework needed)
- [x] 2.6 **TDD RED**: `QueryEditorViewModelTest::formatSql_runsOnDefaultDispatcher()` — DONE
- [x] 2.7 **TDD GREEN**: `withContext(Dispatchers.Default)` confirmed — DONE

### Phase 3: UI Wiring — Toolbar Button + Shortcut (DONE — carried forward unchanged)

- [x] 3.1 through 3.12 — see apply-progress obs #1977/#2135 for full detail; NOT regenerated by this revision (Format toolbar button, i18n strings, click/shortcut wiring, history-atomic apply, cursor clearing all already implemented and unaffected by the formatter algorithm change)

### Phase 4: SqlTokenizer Integration (DONE — carried forward unchanged)

- [x] 4.1 `SqlTokenizer.kt` keyword regex built from `SqlKeywords.KEYWORDS` — DONE (verified prior session)
- [ ] 4.2 Verify existing `SqlTokenizerTest` still passes — STILL NOT DONE: no `SqlTokenizerTest.kt` exists in the repo (gap carried forward from apply-progress, unrelated to this revision)

### Phase 5: Verification — **updated for revised scenario set**

- [ ] 5.1 Run all unit tests: `SqlKeywordsTest` (3 tests, unaffected), `SqlFormatterTest` (20 scenarios — see Phase 1B tally), `EditorShortcutsTest` (+1 test, unaffected), `QueryEditorViewModelTest` (+2 tests, unaffected)
- [ ] 5.2 Run E2E tests: `QueryEditorScreenTest` (+5 scenarios — re-verify green now that formatted output shape changed: button exists, tap button, Ctrl+Shift+F, undo, multi-cursor)
- [ ] 5.3 Manual smoke: Open editor → type `select id, name from users where active = 1` → tap Format → verify column-per-line `SELECT` + indented `FROM`/`WHERE` (spec Scenario 1) → Ctrl+Z → verify original restored
- [ ] 5.4 Manual smoke: Paste the maintainer's two-statement example (spec Scenario 8a) → verify INSERT/VALUES list-breaking, SELECT breaking, and correct `;\n` join between statements
- [ ] 5.5 Manual smoke: `INSERT INTO t (id) VALUES (1);` → verify single-item lists still break onto their own lines (Scenario 7b) and the trailing `;` is preserved

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
