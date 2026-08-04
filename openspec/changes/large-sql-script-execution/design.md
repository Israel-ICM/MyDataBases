# Design: Large SQL Script Execution

## Technical Approach

Follow the `create-database-execution` precedent: one use case per action → repository → engine, pure domain logic, VM-only error mapping. Two new domain components (`SqlStatementStreamSplitter`, `StatementRiskClassifier`) are Reader-in / Flow-out, source-agnostic, and reusable by a future `backup-restore` importer. Two new use cases drive the two-phase flow via `Flow`. One new generic engine primitive (`executeScript`) executes a `Flow<ScriptStatement>` on a single held-open connection without buffering the file or result sets. A new `runscript` package (screen + VM) is a sibling of `queryeditor`, untouched except for a line-count guard on the existing open-file launcher.

## Architecture Decisions

| Decision | Choice | Alternative(s) rejected | Rationale |
|---|---|---|---|
| Splitter re-run per phase | Phase B re-opens a fresh `Reader` over the same `Uri` and re-splits, instead of caching Phase A's statement list | Cache `List<ScriptStatement>` from Phase A | Caching reintroduces the exact "buffer the whole script" failure mode this feature exists to avoid. One extra sequential file read is I/O cost, not memory risk — acceptable on 600k lines. |
| Engine input shape | `executeScript(statements: Flow<ScriptStatement>, onProgress)` — engine consumes an already-split domain `Flow`, never a raw `Reader` | Engine accepts `Reader` and does its own lexing | Keeps ALL lexical logic in `domain/sql` (pure, testable, reusable by restore); engine stays a dumb JDBC executor, matching the existing `executeBatch`/`executeQuery` shape. |
| Splitter failure signaling | Dedicated sealed `ScriptError` (`MalformedDelimiterDirective`, `UnterminatedToken`) thrown from the `flow{}` builder | Reuse `DatabaseError.InvalidConfiguration` | Splitter failures are never server errors; a distinct type keeps VM error-mapping exhaustive and prevents conflating "bad script" with "bad connection config". |
| Cancellation propagation | `Job.invokeOnCompletion(onCancelling = true)` inside `executeScript` calls `Statement.cancel()` on the in-flight JDBC `Statement` | Rely on coroutine cancellation alone | JDBC calls block the IO thread; plain coroutine cancellation does NOT interrupt a blocking `executeUpdate()`/`executeQuery()`. `Statement.cancel()` is spec'd to be called from another thread — the handler fires on the cancelling caller's thread, satisfying that contract for free. |
| Editor threshold check | New tiny `domain/sql/LineThresholdGuard` counts newlines with early-exit at 50,001; runs BEFORE `readText()`, gates whether `readText()` is called at all | Modify `readText()`/`TextFieldValue` path | Zero changes to the documented-working small-file path; the guard is a pure pre-check, satisfying "never touch `readText()`". |
| SELECT handling in scripts | `fetchSize = Integer.MIN_VALUE`, iterate + discard, return count only | Buffer rows / capped preview | Locked decision; MySQL driver 5.1.x requires `MIN_VALUE` fetch size for true row streaming — anything else buffers client-side regardless of code intent. |

## Data Flow

```
Phase A (pre-scan, cancelable)
  Uri ──> Reader#1 ──> SqlStatementStreamSplitter.split() ──Flow<ScriptStatement>──>
      PreScanScriptUseCase ──> StatementRiskClassifier.classify() per item
          ──> emits PreScanEvent.Progress(...)   [VM: PreScanning]
          ──> emits PreScanEvent.Completed(RiskReport)
  VM: RiskReport.isRisky? ──yes──> AwaitingConfirmation(report)
                          ──no ──> auto-continue to Phase B

Phase B (execute, sequential, best-effort)
  Uri ──> Reader#2 ──> SqlStatementStreamSplitter.split() ──Flow<ScriptStatement>──>
      ExecuteScriptUseCase ──> DatabaseRepository.executeScript(statements, onProgress)
          ──> MySQLEngine: ONE connection.use { for each statement: execute, onProgress }
          ──> emits ExecutionEvent.Progress(...)  [VM: Executing]
          ──> on error/cancel: stop, stoppedAtStatement = N
          ──> emits ExecutionEvent.Completed(ScriptExecutionSummary)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/sql/ScriptModels.kt` | Create | `ScriptStatement`, `RiskCategory`, `RiskReport`, `ScriptError`, `ScriptExecutionProgress`, `ScriptExecutionSummary` |
| `domain/sql/SqlStatementStreamSplitter.kt` | Create | Reader → `Flow<ScriptStatement>` streaming lexer |
| `domain/sql/StatementRiskClassifier.kt` | Create | Pure rule-table classifier |
| `domain/sql/LineThresholdGuard.kt` | Create | Early-exit newline counter for the editor guard |
| `domain/usecases/PreScanScriptUseCase.kt` | Create | Phase A: `Flow<PreScanEvent>` |
| `domain/usecases/ExecuteScriptUseCase.kt` | Create | Phase B: `Flow<ExecutionEvent>`, `channelFlow` bridging `executeScript`'s callback |
| `core/database/engine/DatabaseEngine.kt` | Modify | Add `executeScript(...)` |
| `core/database/engine/mysql/MySQLEngine.kt` | Modify | Implement: held connection, fetch-size, `Statement.cancel()` hook |
| `core/database/engine/mariadb/MariaDBEngine.kt` | Modify | Delegate to `MySQLEngine` |
| `core/database/repository/DatabaseRepository(.Impl).kt` | Modify | Add + delegate `executeScript` |
| `ui/screens/runscript/RunScriptScreen.kt`, `RunScriptViewModel.kt` | Create | Sealed state machine, one confirmation dialog |
| `ui/screens/queryeditor/QueryEditorScreen.kt` | Modify | `openFileLauncher`: guard check before `readText()` |
| `res/values{,-es}/strings.xml` | Modify | Progress/report/confirmation/outcome/error strings |

## Interfaces / Contracts

```kotlin
// domain/sql
data class ScriptStatement(val sql: String, val lineNumber: Int, val hasTopLevelWhere: Boolean)
sealed class ScriptError(message: String) : Throwable(message) {
    data class MalformedDelimiterDirective(val lineNumber: Int) : ScriptError(...)
    data class UnterminatedToken(val lineNumber: Int, val kind: String) : ScriptError(...)
}
enum class RiskCategory { DDL, DELETE, UPDATE_NO_WHERE }
data class RiskReport(
    val totalStatements: Int,
    val counts: Map<RiskCategory, Int>,
    val lineNumbers: Map<RiskCategory, List<Int>>
) { val isRisky get() = counts.values.any { it > 0 } }

class SqlStatementStreamSplitter {
    fun split(reader: Reader): Flow<ScriptStatement> // throws ScriptError on malformed input
}
object StatementRiskClassifier {
    fun classify(statement: ScriptStatement): RiskCategory? // null = clean
}

// core/database/engine/DatabaseEngine.kt — NEW generic primitive
suspend fun executeScript(
    statements: Flow<ScriptStatement>,
    onProgress: suspend (ScriptExecutionProgress) -> Unit
): Result<ScriptExecutionSummary>
data class ScriptExecutionProgress(val statementIndex: Int, val lineNumber: Int, val totalStatements: Int?)
data class ScriptExecutionSummary(val statementsExecuted: Int, val stoppedAtStatement: Int?, val selectRowsDiscarded: Long)

// ui/screens/runscript/RunScriptViewModel.kt
sealed class RunScriptState {
    data object Idle : RunScriptState()
    data class PreScanning(val statementsScanned: Int, val lineNumber: Int) : RunScriptState()
    data class AwaitingConfirmation(val report: RiskReport) : RunScriptState()
    data class Executing(val progress: ScriptExecutionProgress) : RunScriptState()
    data class Success(val summary: ScriptExecutionSummary) : RunScriptState()
    data class Error(val message: String) : RunScriptState()
    data object Cancelled : RunScriptState()
}
```

## Splitter Tokenization (no full AST)

Char-by-char state machine over `BufferedReader`, one `StringBuilder` per in-progress statement (never the whole file):

| State | Enter on | Exit on | Effect |
|---|---|---|---|
| `NORMAL` | start / after terminator | — | tracks `parenDepth`; top-level `WHERE` keyword at `parenDepth == 0` sets `hasTopLevelWhere = true` |
| `LINE_COMMENT` | `--` or `#` | `\n` | chars ignored, not appended to statement |
| `BLOCK_COMMENT` | `/*` | `*/` | ignored; unterminated at EOF → `ScriptError.UnterminatedToken` |
| `SINGLE_QUOTE` / `DOUBLE_QUOTE` | `'` / `"` | matching unescaped quote (`''`/`\'` handled) | chars appended verbatim, `;`/keywords inside do not affect state |
| `BACKTICK` | `` ` `` | `` ` `` | chars appended verbatim |
| `DELIMITER` directive | line starts with `DELIMITER` (case-insensitive) at statement-start position in `NORMAL` | end of line | updates active terminator string (default `;`); malformed (no token after keyword) → `ScriptError.MalformedDelimiterDirective`; NOT emitted as a statement |

Statement boundary: in `NORMAL` state, buffer's trailing chars match the **active terminator string** (length-aware compare, not just `;`) → emit `ScriptStatement`, reset buffer/`parenDepth`/`hasTopLevelWhere`, keep line counter and active terminator.

## Concurrency & Cancellation

- Dispatcher: matches existing pattern — `withContext(Dispatchers.IO)` inside engine/use-case suspend functions; VM uses `viewModelScope.launch(exceptionHandler)`, stores the `Job`, `cancel()` calls `job.cancel()` (mirrors `QueryEditorViewModel`).
- Pre-scan cancellation: pure Kotlin `Flow` collection — standard cooperative cancellation, no extra hook needed.
- Execution cancellation (non-obvious): `executeScript` registers `coroutineContext[Job]?.invokeOnCompletion(onCancelling = true) { cause -> if (cause is CancellationException) currentStatement.get()?.cancel() }` before the loop, disposes it after. `currentStatement` is an `AtomicReference<Statement?>` updated before each `execute*` call. This unblocks the IO thread mid-JDBC-call instead of waiting for it to return.
- Progress bridging: `ExecuteScriptUseCase`/`PreScanScriptUseCase` use `channelFlow { }` so the engine's suspend `onProgress` callback can `send(...)` into the same `Flow` the VM collects.

## Memory-Safety Guarantees

- File never fully read into memory: `BufferedReader` (default buffer), one statement `StringBuilder` alive at a time, discarded after each emission.
- SELECT rows never buffered: `fetchSize = Integer.MIN_VALUE` + `resultSet.next()` loop counting only, no `getObject`/materialization.
- `RiskReport.lineNumbers` bounded by realistic worst case (~600k ints ≈ 2.4 MB even if every statement is risky) — accepted per locked decision #3; documented fallback (counts-only) if profiling proves otherwise.
- Connection held open for the whole run (deliberate usage change from per-op connections); closed in a `finally`/`use` block on completion, error, or cancellation.

## Integration Points

- **Editor (`QueryEditorScreen.openFileLauncher`)**: adds `LineThresholdGuard.exceedsThreshold(...)` check on a throwaway `Reader` BEFORE the existing `readText()` call. Below threshold: existing code path is unchanged. At/above: skip `readText()`/`TextFieldValue` entirely, navigate to `RunScriptScreen` with the `Uri`.
- **`core/database`**: additive-only — `executeBatch` untouched; `executeScript` is a new interface member implemented once in `MySQLEngine`, delegated by `MariaDBEngine`.
- **DB context**: no new plumbing — active connection's `config.database` (baked into the JDBC URL) and same-connection sequential `USE` semantics are already correct, reused as-is.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | Splitter: escapes, nested comments, backticks, `;`-in-string, `DELIMITER` (valid + malformed), top-level WHERE vs subquery WHERE | JUnit + `runTest`, feed `StringReader` |
| Unit | Classifier: full rule table | Parameterized cases per `RiskCategory` |
| Unit | `PreScanScriptUseCase`/`ExecuteScriptUseCase`: progress emission order, cancellation, `ScriptError` propagation | Mockk repository, `Flow` test via `Turbine`/`toList()` |
| Unit | `RunScriptViewModel`: full state sequence incl. auto-skip confirmation when clean, `Cancelled` on cancel | `runTest`, fake use cases |
| Integration (manual) | `DELIMITER $$` stored-procedure dump executes; `USE` mid-script switches DB; large `SELECT` doesn't OOM | Real MySQL/MariaDB server |

## Migration / Rollout

No migration required. All changes are additive except the editor's open-file guard (revert restores prior always-`readText()` behavior).

## Amendment: Entry-Point Selector Design

> Adds the `NewQueryOptionsSheet` UI entry point and its wiring. Does not alter any decision above — consumes the already-designed `LineThresholdGuard`, `PreScanScriptUseCase`, and `RunScriptViewModel` state machine as-is.

**Grounding correction (verified against current code):** `NewQueryScreen.kt` / `Routes.NewQuery` is reachable from **two independent entry points**: (1) the bottom-nav modal action `id="new_query"` (`onModalAction`, both server and table menus) — the one this amendment replaces — and (2) `DatabaseActionMenuScreen.onNavigateToQueries`, a real `navController.navigate(Routes.NewQuery...)` call from the "¿Qué quieres hacer?" DB menu. Only entry point (1) is touched. `NewQueryScreen.kt` and its `composable(Routes.NewQuery.route)` registration stay untouched and fully live.

### `NewQueryOptionsSheet` composable

Mirrors `MoveToFolderSheet`'s `FolderOption` row shape exactly: `ModalBottomSheet` + `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, static `Column` of three clickable rows (icon + `Text`), no `LazyColumn` needed (fixed 3 items). Stateless — no ViewModel; state lives in `MyDataBasesNavHost` like sibling sheets.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewQueryOptionsSheet(
    onNewQuery: () -> Unit,
    onOpenQueryFile: () -> Unit,
    onRunScript: () -> Unit,
    onDismiss: () -> Unit
)
```

Each row's `onClick` calls its callback then `onDismiss()` (same pattern as `FolderOption.onClick = { onSelectFolder(id); onDismiss() }`).

| Order | Label (`strings.xml`) | Icon | Callback |
|---|---|---|---|
| 1 | `new_query_option_new` "New Query" | `PhosphorAppIcons.Nav.newQuery` (existing, `TablerIcons.FileText`) | `onNewQuery` |
| 2 | `new_query_option_open_file` "Open Query File" | `PhosphorAppIcons.Nav.openQueryFile` — **new** (`TablerIcons.FileImport`) | `onOpenQueryFile` |
| 3 | `new_query_option_run_script` "Run Script (No Edit)" | `PhosphorAppIcons.Nav.runScript` — **new** (`TablerIcons.PlayerPlay`) | `onRunScript` |

**Icon choice**: no existing `PhosphorAppIcons` entry fits "import a file" or "run/execute." `TablerIcons.FileImport` and `TablerIcons.PlayerPlay` are confirmed present in the already-depended-on `tabler-icons-android:1.1.0` artifact (verified against the library sources jar) — no new icon library or drawable asset added, only two new documented properties on `PhosphorAppIcons.Nav`, following the file's established convention of adding rationale-commented entries (see `automations`, `duplicate`, `truncate`).

### NavHost wiring

```kotlin
// New state, next to showAddDatabaseSheet / showAddTableSheet
var showNewQueryOptionsSheet by remember { mutableStateOf(false) }
var pendingScriptUri by remember { mutableStateOf<Uri?>(null) }

onModalAction = { destinationId ->
    when (destinationId) {
        "add_database" -> showAddDatabaseSheet = true
        "new_table" -> showAddTableSheet = true
        "new_query" -> showNewQueryOptionsSheet = true   // was: workspaceManager.openQueryCard(...)
    }
}
```

`NewQueryOptionsSheet` renders as a direct sibling inside the `WorkspaceOverlay { ... }` trailing lambda — alongside `AdaptiveNavigationScaffold`, **not** threaded into `DatabasesListScreen`/`TablesListScreen` params like `showAddDatabaseSheet`/`showAddTableSheet` are. Reason: `new_query` is declared in **both** `destinationsForDatabaseList` and `destinationsForTablesList` (`NavigationDestinations.kt`), so scoping it to one route's screen would drop the sheet when the other menu triggers it. Rendering at the `WorkspaceOverlay` level guarantees availability from either route with zero context/route change, preserving the `2026-06-30-new-query-modal-fix` guarantee.

### Option 2 — "Open Query File" decision logic

```
GetContent() picker → Uri
    → openInputStream(uri) [stream #1, throwaway]
    → LineThresholdGuard.exceedsThreshold(reader)   // early-exit @ 50,001, NEVER readText()
    ├─ true  (> 50,000 lines) → pendingScriptUri = uri
    │                           navController.navigate(Routes.RunScript.createRoute(connectionId))
    └─ false (≤ 50,000 lines) → openInputStream(uri) [stream #2] → readText()
                                 workspaceManager.openQueryCard(connectionId, initialSql = fileContent)
```

`readText()` is called at most once, only for files that already passed the size guard — the existing small-file path (`TextFieldValue(initialSql)`, confirmed in `QueryEditorScreen`) is reused unmodified via `initialSql`, not reimplemented. **Supersedes** the original design's "Editor (`QueryEditorScreen.openFileLauncher`)" integration point: the guard now runs here, not inside `QueryEditorScreen`. `QueryEditorScreen`'s own in-session "open file" toolbar action (loading a second file into an already-open editor) is untouched — out of scope for this amendment.

### Option 3 — "Run Script (No Edit)"

Same picker, **no guard call** — `pendingScriptUri = uri; navController.navigate(Routes.RunScript.createRoute(connectionId))` unconditionally.

### Wiring into `RunScriptViewModel` / `PreScanScriptUseCase`

New route: `data object RunScript : Routes("connection/{connectionId}/run_script")`. Its `composable { }` block reads `pendingScriptUri` (hoisted `NavHost`-level state, set immediately before `navigate()`) and passes it as a plain composable parameter — not a route argument — to avoid encoding a `content://` `Uri` (arbitrary length/characters) into a nav path segment:

```kotlin
composable(Routes.RunScript.route, arguments = listOf(navArgument("connectionId") { type = NavType.StringType })) {
    val connectionId = it.arguments?.getString("connectionId") ?: ""
    val uri = pendingScriptUri ?: return@composable  // guarded: always set pre-navigate
    RunScriptScreen(uri = uri, connectionId = connectionId)
}
```

`RunScriptScreen(uri, connectionId, viewModel: RunScriptViewModel = hiltViewModel())` triggers `LaunchedEffect(uri) { viewModel.startPreScan(uri, connectionId) }`, entering the existing sealed state machine (`Idle → PreScanning → AwaitingConfirmation/Executing → ...`) already specified above. **Both options 2 (oversized) and 3 (always) converge on the identical `RunScriptScreen` entry** — the ViewModel/use cases have no knowledge of which selector option triggered navigation; `PreScanScriptUseCase` always runs Phase A regardless of trigger.

### New Architecture Decisions

| Decision | Choice | Alternative(s) rejected | Rationale |
|---|---|---|---|
| Sheet render scope | `NewQueryOptionsSheet` lives at `WorkspaceOverlay` level (sibling to `AdaptiveNavigationScaffold`), not per-route | Thread `showNewQueryOptionsSheet` into `DatabasesListScreen`/`TablesListScreen` like `showAddDatabaseSheet` | `new_query` is a dual-context destination; per-route scoping would break one of the two menus |
| Uri hand-off to `RunScriptScreen` | Hoisted `NavHost`-level `mutableStateOf<Uri?>` (mirrors `showAddDatabaseSheet` pattern), passed as a composable param | Encode `Uri.toString()` into the nav route string | `content://` URIs are long/opaque; avoids encode/decode entirely and matches the file's existing state-hoisting convention |
| Guard ownership | `LineThresholdGuard` check moves into the selector's "Open Query File" handler (`MyDataBasesNavHost.kt`) | Keep the guard inside `QueryEditorScreen.openFileLauncher` (original design) | Matches proposal's locked scope resolution — the guard is now owned by the entry-point selector, not the editor |

### File Changes (additions to the table above)

| File | Action | Description |
|------|--------|-------------|
| `ui/screens/databases/NewQueryOptionsSheet.kt` | Create | 3-option `ModalBottomSheet`, stateless |
| `ui/components/PhosphorAppIcons.kt` | Modify | Add `Nav.openQueryFile` (`FileImport`), `Nav.runScript` (`PlayerPlay`) |
| `ui/navigation/Routes.kt` | Modify | Add `RunScript` route (`connectionId` only; `Uri` passed via hoisted state) |
| `ui/navigation/MyDataBasesNavHost.kt` | Modify | `showNewQueryOptionsSheet` + `pendingScriptUri` state; `new_query` branch; sheet + `RunScript` composable registration |
| `res/values{,-es}/strings.xml` | Modify | `new_query_option_new/open_file/run_script` (title + description) |

`NavigationDestinations.kt` — **no change** (`isModal = true` on `new_query` already correct in both `destinationsForDatabaseList` and `destinationsForTablesList`).

## Open Questions

- [ ] If the SAF `Uri` content changes between Phase A and Phase B (external edit), statement count may drift from the `RiskReport`'s `totalStatements` — acceptable (progress bar shows a stale denominator only); no integrity guarantee is claimed beyond what native engine errors already surface.
- [ ] `pendingScriptUri` uses plain `remember` (matching `showAddDatabaseSheet`/`showAddTableSheet` convention in the same file) — does not survive process death. Accepted as consistent with existing sibling state; flag if this needs `rememberSaveable` (Uri is Parcelable) in a future hardening pass.
