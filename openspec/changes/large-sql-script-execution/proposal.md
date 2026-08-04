# Proposal: Large SQL Script Execution

## Intent

The visual query editor loads any opened `.sql` file with `readText()` into a `TextFieldValue`, so Compose text layout hangs on large scripts (documented failure at ~600,000 lines). Users cannot run big migration/seed/dump files. This change adds a **"Run script"** execution path fully decoupled from the editor: it streams the file from disk statement-by-statement, never materializes the whole file as a `String`, pre-scans for risky statements, asks for one aggregated confirmation, then executes sequentially against the active connection. This is a locked user decision, grounded in `exploration.md` (Engram obs #2448).

## Scope

### In Scope
- New "Run script" action (SAF `Uri` → use case, **never** to the editor / `TextFieldValue` / `readText()`).
- `SqlStatementStreamSplitter` (new, `domain/`): comment/string/backtick-aware char-stream lexer over a `Reader`; emits statements + line numbers at top-level `;`; also answers "top-level WHERE present?" for the UPDATE rule. Greenfield — `SqlTokenizer` is whole-`String` regex, reference-only.
- `StatementRiskClassifier` (new, `domain/`): fixed rule table (pure function).
- `PreScanScriptUseCase` (new): Phase A — one cancelable streaming pass, drives a progress `Flow`, returns an aggregated `RiskReport`.
- `ExecuteScriptUseCase` (new): Phase B — streams + executes on one held-open connection.
- Engine extension: a **generic streaming primitive** on `DatabaseEngine`/`DatabaseRepository` (`executeScript(reader/statements, onProgress)`) keeping one connection open, not buffering all results, and setting `setFetchSize(Integer.MIN_VALUE)` for SELECTs. `MariaDBEngine` delegates to `MySQLEngine` (single impl point).
- New screen + ViewModel (sibling to `queryeditor`) with a sealed state machine: `Idle → PreScanning(progress) → AwaitingConfirmation(report) → Executing(progress) → Success | Error | Cancelled`.
- **One** aggregated confirmation dialog, shown once before any execution, only if risky statements were found.
- Localized strings (en + es) for progress, risk report, confirmation, outcomes, errors.
- Unit tests: splitter (escapes, comments, backticks, `;`-in-string, top-level WHERE), classifier (rule table), both use cases (state transitions, cancellation, error mapping).

### Out of Scope
- Editing the file (no editor involvement at all).
- Engines other than MySQL/MariaDB.
- Runtime-configurable classification rules / any settings toggle.
- Custom "which DB" dialog — DB context inherits the active connection; `USE` overrides sequentially; no context → native engine error surfaces as-is (e.g. MySQL 1046).
- True whole-script rollback for DDL-heavy scripts (impossible; see Q2).
- Connection pooling / driver upgrade (pre-existing tech debt, tracked separately).
- Building a second streaming importer for `backup-restore` (see R7 — coordinate, don't fork).

## Capabilities

### New Capabilities
- `large-sql-script-execution`: streams a `.sql` file from disk, pre-scans and classifies every statement, shows one aggregated risk confirmation, then executes sequentially on the active connection with progress and cancellation — decoupled from the visual editor.
- `sql-statement-stream-splitting`: a comment/string/backtick/`DELIMITER`-aware streaming statement splitter emitting statements + line numbers from a `Reader`, reusable by future importers (e.g. restore).

### Modified Capabilities
- None at the editor level. The editor's open-`.sql` behavior is untouched except that above the size threshold (Q1) the oversized file is routed to "Run script" instead of opening. Per the **Entry-Point Selector amendment**, that guard now lives in the new "What do you want to do?" selector flow (option "Open Query File"), not as a standalone editor delta.
- The existing single "New Query" modal action is **extended** (not a requirement change to editor behavior): the `new_query` entry now opens a three-option selector instead of launching a blank query card directly. Blank-editor launch is preserved verbatim as one option.

## Approach

Follow the `create-database-execution` precedent: one use case per action → repository → engine; pure lexical/classification logic in `domain/` (no `Context`); error mapping + sealed state machine in the ViewModel.

1. **Engine** — Add a generic `executeScript` streaming primitive (feeds one statement at a time, holds one connection open for the whole run, does NOT buffer results, `setFetchSize(Integer.MIN_VALUE)` for SELECTs, emits per-statement progress + a cancel hook via `Statement.cancel()`). Reuses the same-connection sequential `USE`-context semantics already validated by `executeBatch`, but is a NEW method — `executeBatch` buffers the full statement list + all results and closes the connection via `.use{}`, so it is not reusable as-is. Kept generic (not a business method) to preserve the clean-primitives boundary (R1).
2. **Repository** — Add `executeScript(...)` to the interface; delegate to `MySQLEngine`, returning `ConnectionFailed` when not connected (mirrors existing methods).
3. **Domain** — `SqlStatementStreamSplitter` + `StatementRiskClassifier` (pure, testable). `PreScanScriptUseCase` (Phase A) and `ExecuteScriptUseCase` (Phase B) drive `Flow` progress and cancellation.
4. **ViewModel** — Thin sealed state machine; maps `DatabaseError` → localized strings via a resolver (VM stays `Context`-free, per precedent).
5. **UI** — New screen: progress during pre-scan/execution, one aggregated confirmation dialog, final outcome (including "stopped at statement N, DB may be partially updated").

### Classification rule table (fixed, locked)
| Statement | Rule |
|-----------|------|
| DDL: CREATE / ALTER / DROP / TRUNCATE / RENAME | Always confirm |
| DELETE (any, with or without WHERE) | Always confirm |
| UPDATE without top-level WHERE | Confirm |
| UPDATE with top-level WHERE | No confirm |
| INSERT / SELECT | Never confirm |

## Open Question Resolutions

> Positions are proposed with rationale. Items flagged **[NEEDS USER CONFIRMATION]** must be confirmed/overridden before design/spec proceeds.

**Q1 — Editor vs. execution-only threshold.** Propose a **line-count guard of 50,000 lines** (cheap to check while acquiring the stream; the hang is a Compose text-layout limit, not a byte count). Below → editor opens normally (with an optional "Run script" affordance). At/above → editor refuses to open the file and offers "Run script" only. 50k is a conservative safety margin well under the 600k target and far below where Compose layout degrades. **[NEEDS USER CONFIRMATION — exact threshold value.]**

**Q2 — Transaction strategy.** Propose **best-effort statement-by-statement, no whole-script transaction.** Rationale: DDL (CREATE/ALTER/DROP) causes **implicit commits** in MySQL/MariaDB, so "rollback everything" is a lie for DDL-heavy scripts; and a single 600k-statement transaction holds locks + undo log for the entire run (R6). On failure or cancel we STOP and report a clear outcome: **"Stopped at statement N. The database may be partially updated."** No silent partial success. Per-DML-block transactions between DDL boundaries were considered and **rejected for v1** (adds boundary-tracking complexity for a guarantee that DDL already breaks). **[NEEDS USER CONFIRMATION — accept best-effort + partial-update warning over any rollback attempt.]**

**Q3 — Risk report detail.** Propose **counts per category by default, plus exact line numbers** — because the splitter already tracks line numbers to power the top-level-WHERE UPDATE rule and error reporting ("stopped at statement N"), so line numbers are effectively free within the pre-scan pass. If profiling on 600k lines shows the per-statement line-number list is a memory/perf problem, fall back to counts-only + first-N sample. **[NEEDS USER CONFIRMATION — counts+line-numbers vs. counts-only.]**

**Q4 — `DELIMITER` handling (R5).** Propose **v1 supports `DELIMITER`** (the splitter treats it as a client directive and switches the terminator), because mysqldump / stored-procedure dumps routinely emit `DELIMITER $$ … $$ DELIMITER ;` and rejecting them would fail a large class of real dump files — the exact files this feature exists to run. If timeline pressure forces a cut, the fallback is **reject-with-clear-error** ("Scripts using DELIMITER are not supported yet"), never silent mis-splitting. **[NEEDS USER CONFIRMATION — support vs. reject-with-message for v1.]**

**Q5 — SELECT output in execution-only mode (R2).** Propose **discard SELECT result sets, report row counts only** (with `setFetchSize(Integer.MIN_VALUE)` so a `SELECT * FROM huge_table;` streams and does not OOM). This path is about *running* a script, not browsing results; full result rendering belongs to the editor. **[NEEDS USER CONFIRMATION — counts-only vs. a capped preview.]**

**Q6 — Execution cancellation guarantee (R3).** Propose **true mid-execution cancel via `Statement.cancel()`** from a separate thread, not UI-only. The whole feature's promise is safe control over huge scripts; a Cancel button that keeps running server-side would be misleading. Pre-scan cancels cleanly (pure streaming). **[NEEDS USER CONFIRMATION — true `Statement.cancel()` vs. UI-only to match the current editor.]**

## Decisions Needing User Confirmation (blocking design/spec)

1. **Q1** — Threshold value (proposed: 50,000 lines).
2. **Q2** — Best-effort + "stopped at N, DB may be partially updated" (no rollback attempt).
3. **Q3** — Risk report granularity (proposed: counts + line numbers).
4. **Q4** — `DELIMITER` support in v1 (proposed: support).
5. **Q5** — SELECT output handling (proposed: discard rows, report counts).
6. **Q6** — Cancellation guarantee (proposed: true `Statement.cancel()`).
7. **New decision flagged by this proposal** — Whether the ~50k editor guard is specced inside this change or as a tiny separate editor delta (design-phase, but confirm scope ownership).

## Amendment: Entry-Point Selector

> **Scope addition (does not alter any locked decision above).** Adds the UI entry point that surfaces the "Run script" path (and the oversized-file guard) to users. All engine/domain/execution decisions (streaming splitter, two-phase pre-scan/confirm, fixed rule table, 50,000-line `>` threshold, best-effort no-rollback) are unchanged and simply *consumed* by this entry point.

### Intent
Today "New Query" is a single direct modal action: `onModalAction("new_query")` in `MyDataBasesNavHost.kt` calls `workspaceManager.openQueryCard(...)` immediately (`NewQueryScreen` is a UI-less `LaunchedEffect` launcher). There is no way to open an existing `.sql` file from this entry, and no way to reach the new "Run script" path. This amendment replaces that single action with a **"What do you want to do?" selector** offering three choices.

### Scope (added to In Scope)
- Replace the direct `new_query` modal action with a selector presenting three options:
  1. **New Query** — unchanged: `workspaceManager.openQueryCard(connectionId, initialSql = null)`.
  2. **Open Query File** — SAF picker (`.sql`) → **if file `> 50,000` lines (locked threshold, strictly greater), route directly to option 3's flow instead of the editor** (satisfies the Oversized-File Editor Guard from Q1/Q7, now triggered here); at or below → load into editor.
  3. **Run Script (No Edit)** — SAF picker (`.sql`) → straight into Phase A pre-scan → Phase B confirm/execute, **regardless of size**, never touching the editor.
- Localized strings (en + es) for the selector title and three option labels/descriptions.

### Integration Approach (grounded in current code)
- **Presentation** — reuse the established `ModalBottomSheet` + clickable-row pattern already used by `MoveToFolderSheet.kt` (options list, `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, icon + `Text` per row). No new sheet primitive needed. New component: `NewQueryOptionsSheet` (three static option rows).
- **Trigger wiring** — keep `isModal = true` on the `new_query` destination in `NavigationDestinations.kt` (both `destinationsForDatabaseList` and `destinationsForTablesList`). In `MyDataBasesNavHost.kt`, change the `"new_query"` branch of `onModalAction` from calling `openQueryCard(...)` directly to flipping a new `showNewQueryOptionsSheet` state (mirrors the existing `showAddDatabaseSheet` / `showAddTableSheet` pattern already in the NavHost). The sheet renders inside the `WorkspaceOverlay` scope so it overlays without a route change — preserving the exact fix locked by `2026-06-30-new-query-modal-fix.md` (no `navController.navigate`, no context switch, no double-sheet).
- **File picking** — reuse the existing `ActivityResultContracts.GetContent()` / `openInputStream(uri)` mechanism already present in `QueryEditorScreen.kt` (and the `FilePicker` helper). The `.sql` line-count check happens while acquiring the stream (as already specified in Q1) — the picker returns a `Uri`, the guard counts lines via the streaming reader (never `readText()`), then branches to editor vs. Run-script flow. Option 3 skips the count entirely and always enters pre-scan.
- **`NewQueryScreen`** — the UI-less launcher is superseded by the sheet-driven flow for this entry; its blank-editor behavior survives verbatim as option 1's callback.

### New Decisions Needing User Confirmation
1. **Presentation form** — `ModalBottomSheet` (proposed, matches `MoveToFolderSheet`/`AddDatabase`/`NewTable` siblings) vs. `AlertDialog`. Labels themselves are locked by the user ("New Query" / "Open Query File" / "Run Script (No Edit)"); only the container is open. **[NEEDS USER CONFIRMATION — sheet vs. dialog.]**
2. **Option order** — proposed top-to-bottom: New Query → Open Query File → Run Script (No Edit) (simplest → most powerful). **[NEEDS USER CONFIRMATION — ordering.]**
3. **Icons** — proposed `PhosphorAppIcons` set (e.g. `Nav.newQuery` for option 1; a file/folder-open glyph for option 2; a play/run glyph for option 3), sourced from the existing icon pack — no new assets. **[NEEDS USER CONFIRMATION — exact glyphs.]**
4. **Guard ownership (resolves prior item 7)** — the oversized-file redirect now lives at this entry point rather than as a separate editor delta. **[NEEDS USER CONFIRMATION — accept that the guard belongs to this selector flow.]**

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/sql/SqlStatementStreamSplitter.kt` | New | Streaming comment/string/backtick/`DELIMITER`-aware lexer + splitter |
| `domain/sql/StatementRiskClassifier.kt` | New | Fixed rule-table classifier (pure) |
| `domain/usecases/PreScanScriptUseCase.kt` | New | Phase A: cancelable pre-scan, progress `Flow`, `RiskReport` |
| `domain/usecases/ExecuteScriptUseCase.kt` | New | Phase B: streaming execution on one held-open connection |
| `core/database/engine/DatabaseEngine.kt` | Modified | Add generic `executeScript` streaming primitive |
| `core/database/engine/mysql/MySQLEngine.kt` | Modified | Implement `executeScript` (held connection, no result buffering, `setFetchSize(MIN_VALUE)`, `Statement.cancel()`) |
| `core/database/engine/mariadb/MariaDBEngine.kt` | Modified | Delegate `executeScript` to `MySQLEngine` |
| `core/database/repository/DatabaseRepository(.Impl).kt` | Modified | Add + delegate `executeScript` |
| `ui/screens/runscript/RunScriptScreen.kt` + `RunScriptViewModel.kt` | New | Sealed state machine, progress, one aggregated confirmation dialog |
| `ui/screens/queryeditor/*` open-`.sql` launcher | Modified | Above threshold: offer "Run script" instead of loading into editor |
| `ui/screens/databases/NewQueryOptionsSheet.kt` | New | "What do you want to do?" selector — 3 options, `ModalBottomSheet` reusing the `MoveToFolderSheet` row pattern |
| `ui/navigation/MyDataBasesNavHost.kt` | Modified | `new_query` modal action flips `showNewQueryOptionsSheet` instead of calling `openQueryCard` directly; renders the sheet + wires the 3 option callbacks (incl. `.sql` SAF pickers + `> 50,000`-line guard) |
| `ui/screens/databases/NewQueryScreen.kt` | Modified | Blank-editor launch survives as option 1's callback; direct-launch entry superseded by the selector |
| `res/values/strings.xml` + `values-es/strings.xml` | Modified | Progress, risk report, confirmation, outcome, error strings |
| `app/src/test/.../SqlStatementStreamSplitterTest.kt` | New | Escapes, comments, backticks, `;`-in-string, `DELIMITER`, top-level WHERE |
| `app/src/test/.../StatementRiskClassifierTest.kt` | New | Full rule table |
| `app/src/test/.../PreScanScriptUseCaseTest.kt`, `ExecuteScriptUseCaseTest.kt`, `RunScriptViewModelTest.kt` | New | Progress, cancellation, state transitions, error mapping |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| R4 — Mis-splitting a 600k-line dump silently corrupts execution | High | Splitter is the correctness-critical component; exhaustive unit tests for escapes, nested `/* */`, `#`/`--`, backticks, `;`-in-string, `DELIMITER`; fail loud on ambiguity |
| R1 — Engine contract gains a new member | Medium | Keep it a GENERIC streaming primitive, not a business method — preserves the clean-primitives boundary the `create-database-execution` ADR defended |
| R2 — Large SELECT inside script OOMs | Medium | `setFetchSize(Integer.MIN_VALUE)`; discard result rows, report counts only (Q5) |
| R3 — Cancel leaves statement running server-side | Medium | `Statement.cancel()` from a separate thread (Q6); pre-scan cancels cleanly |
| R6 — Long-lived single connection hits read timeout on huge scripts | Medium | Document/adjust `config.readTimeout` behavior for scripts; long-open connection is an intentional usage change, verified in design |
| R7 — Overlap with planned `backup-restore` "Restore from .sql" | Medium | No `backup-restore` change exists in `openspec/changes/` yet (only `.atl/product/features/backup-restore.md` roadmap). Ship the splitter as a reusable `domain/sql` component so restore consumes it instead of forking. **Flagged for user awareness.** |
| R8 — Line-number tracking cost on 600k lines | Low | Line numbers are already needed for the WHERE rule + "stopped at N"; measure and fall back to counts-only if profiling demands (Q3) |
| R9 — No coverage/lint tooling; strict TDD unenforced | Low | Discipline: tests written test-first per `strict_tdd`; `./gradlew compileDebugKotlin` is the available gate |

## Rollback Plan

1. Revert the change commit(s). All new code is additive — new `domain/sql` classes, new use cases, a new engine method + delegations, a new screen/VM, new strings, new tests — removed cleanly by revert. No local schema/persistence is mutated.
2. Remote DB effects from scripts already run persist regardless of revert (acceptable — user explicitly requested execution).
3. The editor's open-`.sql` guard is the only touch to existing behavior; reverting restores the prior load-into-editor path.
4. Partial-failure isolation: if only the new screen/VM misbehaves, keep the tested `domain/sql` + use cases and revert only the UI layer.

## Dependencies

- Existing `MySQLEngine` / `MariaDBEngine`, `DatabaseRepository(.Impl)`, `MySQLConnectionPool` (active connection's `config.database` = default DB — no new plumbing).
- Existing SAF `openInputStream(uri)` acquisition (reused; `readText()` explicitly avoided).
- Existing Hilt wiring; injected `CoroutineDispatcher` per codebase convention.
- No new third-party libraries.
- **Blocking**: the 7 user confirmations above before design/spec.

## Success Criteria

- [ ] A ~600,000-line `.sql` file runs via "Run script" without hanging the UI (never loaded into the editor).
- [ ] Pre-scan streams the whole file once, reports progress, and is cancelable.
- [ ] One aggregated confirmation dialog appears exactly once, only when risky statements exist, before any execution.
- [ ] Classification matches the locked rule table (DDL/DELETE always; UPDATE-no-WHERE confirms; UPDATE-with-WHERE / INSERT / SELECT clean).
- [ ] `USE` mid-script changes context sequentially; no context + no default DB → native engine error (e.g. 1046) surfaces unmodified.
- [ ] On failure/cancel: clear "stopped at statement N, DB may be partially updated" outcome; cancel actually halts the running statement (`Statement.cancel()`).
- [ ] A large `SELECT` inside the script does not OOM (`setFetchSize(Integer.MIN_VALUE)`; counts reported, rows discarded).
- [ ] `DELIMITER $$` stored-procedure dumps split correctly (or, if v1-rejected per Q4, fail with a clear localized message — never mis-split silently).
- [ ] Splitter unit tests cover escapes, comments (`--`/`#`/`/* */`), backticks, `;`-in-string, `DELIMITER`, and top-level-WHERE detection.
- [ ] All new user-facing strings localized (en + es).
- [ ] Tapping "New Query" opens the three-option selector (New Query / Open Query File / Run Script (No Edit)) as an overlay — no route change, no context switch, no double sheet (preserves the `2026-06-30-new-query-modal-fix` behavior).
- [ ] "Open Query File" loads a `.sql` file `≤ 50,000` lines into the editor; a file `> 50,000` lines is redirected to the pre-scan/confirm/execute flow instead of the editor.
- [ ] "Run Script (No Edit)" always enters pre-scan/confirm/execute regardless of file size, never touching the editor.
- [ ] `./gradlew test` and `./gradlew assembleDebug` are green.
