# Tasks: Large SQL Script Execution

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~2,850-3,100 lines (14 new files, 10 modified files) — base ~2,600-2,800 (13 new / 5 modified) + Entry-Point Selector amendment ~250-300 (1 new / 5 modified) |
| Review budget applied | 800 changed lines (project-specified for this change) |
| 800-line budget risk | High |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | 6 PRs (feature-branch-chain recommended; see below) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Est. lines | Notes |
|------|------|-----------|-----------|-------|
| 1 | Domain foundations: `ScriptModels`, `LineThresholdGuard`, `StatementRiskClassifier` (+ tests) | PR 1 | ~325 | No dependency on splitter; independently mergeable/testable. Base: feature/tracker branch. |
| 2 | `SqlStatementStreamSplitter` (+ exhaustive tests) — the correctness-critical component | PR 2 | ~770 | Depends on PR 1 models only. Isolated on purpose per R4 (mis-split risk); base = PR 1 branch. |
| 3 | Engine `executeScript` primitive (MySQL impl + MariaDB delegation + Repository) + `PreScanScriptUseCase` + `ExecuteScriptUseCase` (+ tests) | PR 3 | ~700 | Depends on PR 1 + PR 2. Base = PR 2 branch. |
| 4 | `RunScriptViewModel` state machine (+ tests) | PR 4 | ~430 | Depends on PR 3 use cases. Base = PR 3 branch. |
| 5 | `RunScriptScreen` UI + editor guard integration + localization (+ tests) | PR 5 | ~480 | Depends on PR 4. Base = PR 4 branch. Manual verification (Phase 13) runs here, once the full chain is integrated. |
| 6 | Entry-Point Selector (amendment): `NewQueryOptionsSheet`, 2 new `PhosphorAppIcons.Nav` properties, `Routes.RunScript`, NavHost state/trigger/guard-branch wiring, localization (Phases 14-20) | PR 6 | ~250-300 | Depends on PR 1 (`LineThresholdGuard`) + PR 5 (`RunScriptScreen` is the navigation target). Base = PR 5 branch. Final slice before the tracker merges to `master`; effectively supersedes the Phase 11 guard placement — see Phase 14 note. |

**Recommended chain strategy**: **feature-branch-chain** — now 6 sequential layers (domain → splitter → engine/use-cases → VM → UI → entry-point selector), each strictly depending on the previous or on an already-integrated screen. A tracker branch accumulates the full feature; PR 1 targets the tracker, PR 2 targets PR 1's branch, ..., PR 6 targets PR 5's branch. Only the tracker merges to `master`. This protects rollback (e.g. revert only the UI layer per the proposal's rollback plan) and matches the layered dependency graph better than independent stacked-to-main slices.

**Does the previously-recommended 5-PR split still hold?** No — the Entry-Point Selector amendment needs its own 6th slice. It cannot be folded into PR 5 (already ~480 lines, already over the 400-line default budget) and it cannot ship before PR 5 exists (it navigates into `RunScriptScreen`) or before PR 1 exists (it reuses `LineThresholdGuard`). At ~250-300 lines it fits the budget on its own and isolates the `2026-06-30-new-query-modal-fix` regression risk to one small, independently reviewable diff.

User must choose (or confirm) the chain strategy before `sdd-apply` proceeds (delivery strategy `ask-on-risk`).

---

## Phase 1: Foundation — Domain Models

- [x] 1.1 Create `domain/sql/ScriptModels.kt`: `ScriptStatement(sql, lineNumber, hasTopLevelWhere)`, `RiskCategory` enum (`DDL`, `DELETE`, `UPDATE_NO_WHERE`), `RiskReport(totalStatements, counts, lineNumbers)` with `isRisky` computed property, `ScriptExecutionProgress(statementIndex, lineNumber, totalStatements)`, `ScriptExecutionSummary(statementsExecuted, stoppedAtStatement, selectRowsDiscarded)`, sealed `ScriptError(message): Throwable` with `MalformedDelimiterDirective(lineNumber)` and `UnterminatedToken(lineNumber, kind)`

## Phase 2: Foundation — Line Threshold Guard (TDD)

- [x] 2.1 RED: write `LineThresholdGuardTest.kt` — exceeds threshold at 50,001+ lines, does NOT exceed at exactly 50,000 lines, does NOT exceed for an 8,000-line file, early-exit does not read past line 50,001
- [x] 2.2 GREEN: create `domain/sql/LineThresholdGuard.kt` with early-exit newline counter (`exceedsThreshold(reader): Boolean`, threshold = 50,000 as a hardcoded `private const val`, strictly-greater-than comparison). No Settings UI entry, no configurable parameter — v1 fixed constant per locked decision.

## Phase 3: Foundation — Statement Risk Classifier (TDD)

- [x] 3.1 RED: write `StatementRiskClassifierTest.kt` — parameterized cases for the full rule table: DDL (`CREATE`/`ALTER`/`DROP`/`TRUNCATE`/`RENAME`) → CONFIRM; `DELETE` with/without WHERE → CONFIRM; `UPDATE` without top-level WHERE → CONFIRM; `UPDATE` with top-level WHERE → CLEAN; `INSERT`/`SELECT` → CLEAN; `CREATE PROCEDURE` (DELIMITER body) classifies by leading DDL keyword → CONFIRM
- [x] 3.2 GREEN: create `domain/sql/StatementRiskClassifier.kt` as a pure `object` with `classify(statement: ScriptStatement): RiskCategory?` (null = clean), case-insensitive on leading keyword

## Phase 4: Splitter — RED (write failing tests first)

Highest-risk component (R4): a mis-split silently corrupts execution. Tests MUST precede implementation.

- [x] 4.1 Write `SqlStatementStreamSplitterTest.kt` — Streaming Contract: whole file never materialized as `String` (assert on a large `Reader`), token spanning a read-buffer boundary is preserved intact
- [x] 4.2 Add tests — Statement Termination: top-level `;` splits statements with correct line numbers, consecutive/whitespace-surrounded `;` produce no empty statements
- [x] 4.3 Add tests — String/Identifier Awareness: `;` inside `'...'` is not a boundary, backslash-escaped quote (`\'`) does not close the string, doubled quote (`''`) is a literal, `;` inside backtick identifier is not a boundary, `--` inside a string literal is inert
- [x] 4.4 Add tests — Comment Awareness: `--` line comment skipped, `#` line comment skipped, `/* ... */` block comment spanning lines skipped with correct post-comment line number
- [x] 4.5 Add tests — DELIMITER Directive: `DELIMITER $$ ... $$ DELIMITER ;` emits exactly one statement (stored procedure body) with inner `;` preserved, default terminator restored after `DELIMITER ;`, two consecutive DELIMITER blocks handled sequentially, unparseable DELIMITER (no token after keyword) raises `ScriptError.MalformedDelimiterDirective` and emits nothing further
- [x] 4.6 Add tests — Line Number Reporting: correct line number after a multi-line block comment, correct line number after a statement containing a multi-line string literal
- [x] 4.7 Add tests — Top-Level WHERE Detection: `UPDATE ... WHERE` → `true`, `UPDATE` with no WHERE → `false`, WHERE only inside a subquery → outer statement reports `false` for the outer-only case / `true` when an outer WHERE wraps the subquery, WHERE inside a string literal → `false`
- [x] 4.8 Add tests — Edge Cases: empty file → zero statements no error, comments-only file → zero statements no error, final statement without trailing terminator is still emitted, single giant statement with no terminator anywhere is emitted once at EOF (also added: unterminated block comment at EOF fails loud, not in original scope but required by 5.4)

## Phase 5: Splitter — GREEN (implementation)

- [x] 5.1 Create `domain/sql/SqlStatementStreamSplitter.kt`: `split(reader: Reader): Flow<ScriptStatement>` char-by-char state machine over `BufferedReader`, one `StringBuilder` per in-progress statement (never the whole file), `flow { }` builder emitting lazily
- [x] 5.2 Implement `NORMAL` state: `parenDepth` tracking, active-terminator match (length-aware, not hardcoded `;`) triggers emission + buffer/`parenDepth`/`hasTopLevelWhere` reset, keeps line counter and active terminator across statements
- [x] 5.3 Implement `SINGLE_QUOTE`/`DOUBLE_QUOTE`/`BACKTICK` states: verbatim char append, backslash-escape and doubled-quote handling, correct across internal buffer refills
- [x] 5.4 Implement `LINE_COMMENT` (`--`, `#`) and `BLOCK_COMMENT` (`/* */`) states: chars ignored (not appended), line counter still advances, unterminated block comment at EOF throws `ScriptError.UnterminatedToken`
- [x] 5.5 Implement `DELIMITER` directive parsing: recognized via `BufferedReader.mark`/`reset` line-sniff at the start of any physical line with no accumulated statement content, updates active terminator string, directive line itself never emitted as a statement, malformed directive throws `ScriptError.MalformedDelimiterDirective` and halts further emission
- [x] 5.6 Implement top-level `WHERE` keyword detection: case-insensitive match only at `parenDepth == 0`, sets `hasTopLevelWhere = true` on the in-progress `ScriptStatement`
- [x] 5.7 Implement 1-based line number tracking: constant-per-character newline counting, correct across comments, multi-line strings, and DELIMITER blocks
- [x] 5.8 Run Phase 4 test suite, confirm all green; fix implementation gaps until every scenario in `sql-statement-stream-splitting/spec.md` passes — 29/29 passing on first real implementation pass

## Phase 6: Splitter — REFACTOR

- [x] 6.1 States are a named `private enum class LexState` from the initial implementation (not a separate refactor step — no behavior change needed)
- [x] 6.2 Reviewed: single streaming pass, one `StringBuilder` per in-progress statement cleared on each emission, constant-per-character line tracking; no unbounded allocation. The `DELIMITER` line-sniff mark/reset holds at most one physical line at a time, consistent with the "bounded by the largest single statement" NFR

## Phase 7: Engine — `executeScript` Primitive

- [ ] 7.1 Add `suspend fun executeScript(statements: Flow<ScriptStatement>, onProgress: suspend (ScriptExecutionProgress) -> Unit): Result<ScriptExecutionSummary>` to `core/database/engine/DatabaseEngine.kt` interface
- [ ] 7.2 Implement in `core/database/engine/mysql/MySQLEngine.kt`: acquire ONE connection for the whole run (`connection.use { }`/`finally` close on completion, error, or cancellation), never buffer all statements or all results
- [ ] 7.3 Implement per-statement execution loop: `AtomicReference<Statement?>` updated before each `execute*` call, `coroutineContext[Job]?.invokeOnCompletion(onCancelling = true) { cause -> if (cause is CancellationException) currentStatement.get()?.cancel() }` registered before the loop and disposed after
- [ ] 7.4 Implement SELECT handling inside the loop: `setFetchSize(Integer.MIN_VALUE)`, iterate `resultSet.next()` counting rows only, discard row data, never call `getObject`
- [ ] 7.5 Implement stop-on-first-failure: catch the first `SQLException`, stop before the next statement, return `Result.failure` carrying `stoppedAtStatement = N` and the native error, no rollback attempted
- [ ] 7.6 Implement progress emission: call `onProgress(ScriptExecutionProgress(...))` after each statement completes
- [ ] 7.7 Modify `core/database/engine/mariadb/MariaDBEngine.kt`: delegate `executeScript` to `MySQLEngine` (single implementation point)
- [ ] 7.8 Modify `core/database/repository/DatabaseRepository.kt` (+ `DatabaseRepositoryImpl.kt`): add `executeScript(...)` to the interface, delegate to the connected engine, return `DatabaseError.ConnectionFailed` when not connected (mirrors existing methods)

## Phase 8: Domain — Use Cases

- [ ] 8.1 Create `domain/usecases/PreScanScriptUseCase.kt`: define sealed `PreScanEvent` (`Progress(statementsScanned, lineNumber)`, `Completed(RiskReport)`, `Error(ScriptError)`); `channelFlow { }` opens Reader#1 over the `Uri`, calls `SqlStatementStreamSplitter.split()`, classifies each statement via `StatementRiskClassifier`, aggregates counts + line numbers per `RiskCategory` and `totalStatements`, cancels cleanly on collector cancellation (read-only, no server-side hook needed)
- [ ] 8.2 Write `PreScanScriptUseCaseTest.kt`: aggregates correct counts + exact line numbers for a mixed script, zero DB execution occurs during pre-scan, mid-pass cancellation stops promptly with no `RiskReport` finalized, an unparseable-`DELIMITER` `ScriptError` surfaces as `PreScanEvent.Error` and never reaches confirmation
- [ ] 8.3 Create `domain/usecases/ExecuteScriptUseCase.kt`: define sealed `ExecutionEvent` (`Progress(ScriptExecutionProgress)`, `Completed(ScriptExecutionSummary)`, `Error(...)`); `channelFlow { }` opens a FRESH Reader#2 over the same `Uri`, re-splits, calls `repository.executeScript(statements, onProgress)`, bridges the engine's suspend `onProgress` callback into `send(...)`
- [ ] 8.4 Write `ExecuteScriptUseCaseTest.kt`: progress events emitted in statement order, `ExecutionEvent.Completed(summary)` on success, `ScriptError` from the re-split propagates as `Error`, cancellation reported without a false `Completed`

## Phase 9: ViewModel — `RunScriptViewModel` State Machine

- [ ] 9.1 Add sealed `RunScriptState` in `ui/screens/runscript/RunScriptViewModel.kt`: `Idle`, `PreScanning(statementsScanned, lineNumber)`, `AwaitingConfirmation(report: RiskReport)`, `Executing(progress: ScriptExecutionProgress)`, `Success(summary: ScriptExecutionSummary)`, `Error(message: String)`, `Cancelled`
- [ ] 9.2 Create `@HiltViewModel RunScriptViewModel` injecting `PreScanScriptUseCase`, `ExecuteScriptUseCase`; expose `StateFlow<RunScriptState>` starting at `Idle`; remain `Context`-free
- [ ] 9.3 Implement `runScript(uri: Uri)`: launches in `viewModelScope`, collects `PreScanEvent.Progress` → `PreScanning`; on `Completed(report)`: `report.isRisky` → `AwaitingConfirmation(report)`, else auto-continue directly to Phase B
- [ ] 9.4 Implement `confirm()` and `decline()`: `confirm()` starts Phase B execution from `AwaitingConfirmation`; `decline()` returns state to `Idle` without executing anything
- [ ] 9.5 Implement Phase B collection: `ExecutionEvent.Progress` → `Executing`, `Completed(summary)` → `Success(summary)`
- [ ] 9.6 Implement `cancel()`: stores the running `Job`, `cancel()` calls `job.cancel()` (mirrors `QueryEditorViewModel` pattern); resulting cancellation maps to `Cancelled` carrying the same partial-update warning as a failure
- [ ] 9.7 Implement error mapping: `DatabaseError.ConnectionFailed`/`QueryExecutionFailed` (including stopped-at-N + native message) → localized strings via a resolver; `ScriptError.MalformedDelimiterDirective`/`UnterminatedToken` → localized strings; never a Context reference
- [ ] 9.8 Write `RunScriptViewModelTest.kt`: full state sequence `Idle → PreScanning → AwaitingConfirmation → Executing → Success` for a risky script, clean script auto-skips `AwaitingConfirmation`, `decline()` returns to `Idle` with zero execution, `cancel()` during execution yields `Cancelled` with partial-update warning, `ConnectionFailed`/`QueryExecutionFailed`/`ScriptError` each map to a localized `Error`, no active connection → `Error` derived from `ConnectionFailed`

## Phase 10: UI — `RunScriptScreen`

- [ ] 10.1 Create `ui/screens/runscript/RunScriptScreen.kt` composable accepting `uri: Uri, connectionId: String`; hoist `RunScriptViewModel` via `hiltViewModel()`
- [ ] 10.2 Render `PreScanning`: progress indicator + statements-scanned/line-number label, Cancel action (pure `Flow` cancellation, no server-side hook)
- [ ] 10.3 Render `AwaitingConfirmation`: exactly ONE `AlertDialog` showing per-`RiskCategory` counts and affected line numbers, Confirm → `viewModel.confirm()`, Decline/dismiss → `viewModel.decline()`; dialog never re-appears per statement
- [ ] 10.4 Render `Executing`: progress indicator + current statement index/line/total, Cancel action wired to true mid-execution `viewModel.cancel()`
- [ ] 10.5 Render `Success`: outcome content (statements executed, elapsed time)
- [ ] 10.6 Render `Error`: outcome content with stopped-at-statement-N (line L), native error message, and the partial-update warning
- [ ] 10.7 Render `Cancelled`: outcome content with the same partial-update warning as `Error`
- [ ] 10.8 Verify layout adapts across Compact / Medium / Expanded `WindowSizeClass`
- [ ] 10.9 Write `RunScriptScreenTest.kt` Compose UI test: pre-scan → confirmation dialog → confirm → execute → success happy path renders correctly

## Phase 11: Integration — Editor Guard

- [ ] 11.1 Modify `ui/screens/queryeditor/QueryEditorScreen.kt` `openFileLauncher`: call `LineThresholdGuard.exceedsThreshold(...)` on a throwaway `Reader` BEFORE the existing `readText()` call
- [ ] 11.2 On threshold exceeded (> 50,000 lines): skip `readText()`/`TextFieldValue` entirely, navigate to `RunScriptScreen(uri, connectionId)` instead
- [ ] 11.3 Confirm the existing ≤ 50,000-line path (including exactly 50,000) is completely unmodified — `readText()` still runs, editor opens normally

## Phase 12: Localization

- [ ] 12.1 Add to `res/values/strings.xml`: pre-scan progress label, risk-report category labels + counts, confirmation dialog title/body/confirm/decline, execution progress label, success outcome, error outcome (stopped-at-N-line-L + native message), cancellation outcome, partial-update warning, oversized-file editor-guard message
- [ ] 12.2 Add the same keys with Spanish translations to `res/values-es/strings.xml`
- [ ] 12.3 Wire every string in `RunScriptScreen.kt` and the editor-guard message via `stringResource(...)` — zero hardcoded `Text()` calls (run the strings audit before commit)

## Phase 13: Manual Verification

- [ ] 13.1 Run `./gradlew test` and verify every new unit test (splitter, classifier, guard, use cases, ViewModel) passes
- [ ] 13.2 Run `./gradlew assembleDebug` and verify the build succeeds
- [ ] 13.3 Open a > 50,000-line `.sql` file: editor refuses to load it, offers only "Run script", `readText()` is never invoked
- [ ] 13.4 Open an exactly-50,000-line file and an 8,000-line file: both open normally in the editor
- [ ] 13.5 Run a risky script (DDL + DELETE + UPDATE-no-WHERE) against a real MySQL/MariaDB server: one aggregated dialog shows correct counts/line numbers, confirming executes all statements
- [ ] 13.6 Run a clean script (INSERT/SELECT/UPDATE-with-WHERE only): no dialog appears, execution starts directly
- [ ] 13.7 Run a script containing mid-script `USE`: context switches correctly between databases for subsequent statements
- [ ] 13.8 Run a statement requiring a database with no default DB and no prior `USE`: native error 1046 surfaces unmodified
- [ ] 13.9 Force a mid-execution failure: execution stops, outcome reports "stopped at statement N (line L)" with the native error and the partial-update warning, no rollback of prior statements
- [ ] 13.10 Cancel a long-running statement mid-execution: verify via the DB process list that `Statement.cancel()` actually halted it server-side; outcome reports `Cancelled`
- [ ] 13.11 Run a script containing a large `SELECT` (millions of rows): no `OutOfMemoryError`, only a row count is reported
- [ ] 13.12 Run a `DELIMITER $$ ... $$ DELIMITER ;` stored-procedure dump: it executes as exactly one statement
- [ ] 13.13 Verify all new strings render correctly in both English and Spanish device locales

## Phase 14: Entry-Point Selector — Icons & Route Foundation (Amendment)

> **Amendment scope**: Adds the `NewQueryOptionsSheet` UI entry point per `proposal.md` § "Amendment: Entry-Point Selector" and `design.md` § "Amendment: Entry-Point Selector Design". Consumes `LineThresholdGuard` (Phase 2) and `RunScriptScreen` (Phase 10) unchanged — no engine/domain/execution decision is altered.
>
> **Supersession notice**: Phase 11 (`QueryEditorScreen.openFileLauncher` guard, tasks 11.1-11.3) is SUPERSEDED by this amendment's "Guard ownership" decision. Do NOT implement Phase 11 as originally written — the guard now lives in the selector's "Open Query File" handler (Phase 17). `QueryEditorScreen`'s own in-session "open file" toolbar action (reopening a file inside an already-open editor) stays untouched and ungated; it is out of scope for this amendment.

- [ ] 14.1 Add two new documented properties to `PhosphorAppIcons.Nav` in `ui/components/PhosphorAppIcons.kt`: `openQueryFile: ImageVector get() = TablerIcons.FileImport` and `runScript: ImageVector get() = TablerIcons.PlayerPlay`, each with a rationale comment matching the file's existing convention (see `automations`, `duplicate`, `truncate`) — no new icon library, `tabler-icons-android` already provides both glyphs
- [ ] 14.2 Add `Routes.RunScript` data object to `ui/navigation/Routes.kt`: `Routes("connection/{connectionId}/run_script")` with `createRoute(connectionId: String): String`, matching the KDoc + `createRoute` pattern of sibling single-arg routes (e.g. `QueryEditor`, `Views`)

## Phase 15: Entry-Point Selector — `NewQueryOptionsSheet` Composable

- [ ] 15.1 Create `ui/screens/databases/NewQueryOptionsSheet.kt`: stateless `@Composable fun NewQueryOptionsSheet(onNewQuery: () -> Unit, onOpenQueryFile: () -> Unit, onRunScript: () -> Unit, onDismiss: () -> Unit)` using `ModalBottomSheet` + `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, mirroring `MoveToFolderSheet`'s `FolderOption` row shape (static `Column` of 3 clickable icon+`Text` rows, no `LazyColumn`, no `ViewModel`)
- [ ] 15.2 Implement each row in fixed order — New Query (`PhosphorAppIcons.Nav.newQuery`), Open Query File (`PhosphorAppIcons.Nav.openQueryFile`), Run Script (No Edit) (`PhosphorAppIcons.Nav.runScript`) — `onClick` invokes the row's callback then `onDismiss()` (mirrors `FolderOption.onClick = { onSelectFolder(id); onDismiss() }`)
- [ ] 15.3 No unit/Compose UI test required for this file — verified precedent: `MoveToFolderSheet.kt` (identical stateless pattern, no ViewModel, static option rows) has zero test coverage anywhere in the codebase; document this as an intentional convention match, not a strict-TDD gap

## Phase 16: Entry-Point Selector — NavHost State & Trigger Wiring

- [ ] 16.1 In `ui/navigation/MyDataBasesNavHost.kt`, add `var showNewQueryOptionsSheet by remember { mutableStateOf(false) }` and `var pendingScriptUri by remember { mutableStateOf<Uri?>(null) }`, placed next to `showAddDatabaseSheet`/`showAddTableSheet`
- [ ] 16.2 Change the `"new_query"` branch of `onModalAction` from `workspaceManager.openQueryCard(...)` to `showNewQueryOptionsSheet = true`
- [ ] 16.3 Render `NewQueryOptionsSheet` as a direct sibling inside the `WorkspaceOverlay { ... }` trailing lambda (NOT threaded into `DatabasesListScreen`/`TablesListScreen` params as a screen parameter), guarded by `if (showNewQueryOptionsSheet)` — required because `new_query` is declared as a modal action in both `destinationsForDatabaseList` and `destinationsForTablesList`
- [ ] 16.4 Wire `onNewQuery = { workspaceManager.openQueryCard(connectionId, initialSql = null) }` — behaviorally identical to the prior direct action
- [ ] 16.5 Wire `onOpenQueryFile` to launch an `ActivityResultContracts.GetContent()` picker filtered to `.sql`; on result, hand off to the Phase 17 guard branch
- [ ] 16.6 Wire `onRunScript` to launch the same picker; on result, unconditionally set `pendingScriptUri = uri` and `navController.navigate(Routes.RunScript.createRoute(connectionId))` — no guard call, regardless of file size

## Phase 17: Entry-Point Selector — "Open Query File" Streaming Guard Branch

- [ ] 17.1 On the Phase 16.5 picker result: open `openInputStream(uri)` as a throwaway stream #1, call `LineThresholdGuard.exceedsThreshold(reader)` (reused from Phase 2, no reimplementation) — NEVER call `readText()` for this check
- [ ] 17.2 If `exceedsThreshold == true` (> 50,000 lines, strictly greater): set `pendingScriptUri = uri` and `navController.navigate(Routes.RunScript.createRoute(connectionId))` — same navigation target as `onRunScript`
- [ ] 17.3 If `exceedsThreshold == false` (≤ 50,000 lines): open a FRESH `openInputStream(uri)` stream #2, call `readText()` exactly once, then `workspaceManager.openQueryCard(connectionId, initialSql = fileContent)` — reuses the existing small-file `TextFieldValue(initialSql)` path in `QueryEditorScreen` unmodified

## Phase 18: Entry-Point Selector — `RunScript` Route Registration

- [ ] 18.1 Register `composable(Routes.RunScript.route, arguments = listOf(navArgument("connectionId") { type = NavType.StringType }))` in `MyDataBasesNavHost.kt`: read `connectionId` from `it.arguments`, read `uri` from the hoisted `pendingScriptUri` state with an early `?: return@composable` guard, render `RunScriptScreen(uri = uri, connectionId = connectionId)`

## Phase 19: Entry-Point Selector — Localization

- [ ] 19.1 Add to `res/values/strings.xml`: selector sheet title + `new_query_option_new`, `new_query_option_open_file`, `new_query_option_run_script` (label + description each, matching the labels locked in `proposal.md`: "New Query" / "Open Query File" / "Run Script (No Edit)")
- [ ] 19.2 Add the same keys with Spanish translations to `res/values-es/strings.xml`
- [ ] 19.3 Wire every string in `NewQueryOptionsSheet.kt` via `stringResource(...)` — zero hardcoded `Text()` calls (run the strings audit before commit, per `android-dev` skill)

## Phase 20: Entry-Point Selector — Regression Check & Scope Boundary

- [ ] 20.1 Regression-verify the `2026-06-30-new-query-modal-fix` guarantees explicitly hold: tapping "New Query" triggers NO `navController.navigate` call, NO workspace context switch, and NO double sheet — confirmed by `NewQueryOptionsSheet` rendering at `WorkspaceOverlay` level (an overlay, not a route)
- [ ] 20.2 Confirm `NewQueryOptionsSheet` opens correctly from BOTH trigger contexts (`destinationsForDatabaseList` and `destinationsForTablesList`) with no state loss switching between them
- [ ] 20.3 Confirm and document (code comment or PR description) that `DatabaseActionMenuScreen.onNavigateToQueries` — the second, independent `NewQueryScreen` entry point, a real `navController.navigate(Routes.NewQuery...)` call — is OUT OF SCOPE and NOT touched by this change; `NewQueryScreen.kt` and its `composable(Routes.NewQuery.route)` registration remain untouched and fully live
- [ ] 20.4 Manual verification: from both entry contexts, exercise all 3 selector options end-to-end — New Query → blank editor; Open Query File (≤50,000 lines) → editor; Open Query File (>50,000 lines) → `RunScriptScreen`; Run Script (No Edit), any size → `RunScriptScreen`; verify en+es locale rendering of all selector strings
