# Exploration: Large SQL Script Execution

> Change: `large-sql-script-execution`
> Phase: exploration only. No proposal / spec / design / implementation decisions are locked here.
> Artifact store: openspec (this file) + Engram (`sdd/large-sql-script-execution/explore`).

## 1. Scope of This Exploration

A new "Run script" execution path that streams a `.sql` file directly from disk and executes it against the active connection, **without** loading the file into the visual text editor. It must survive files up to ~600,000 lines. It performs a two-phase flow: (A) a cancelable streaming pre-scan that classifies every statement and (B) one aggregated confirmation dialog before any execution begins. This document grounds that feature in the existing code and flags feasibility, seams, risks, and the open questions the proposal must resolve.

## 2. Project Reality Check (correcting the brief's module assumptions)

The brief references `feature-editor/`, `core-database/`, and `feature-explorer/` as modules. **They are not Gradle modules.** `settings.gradle.kts` includes only `:app`. These are logical package groupings inside the single `:app` module:

- `core-database` → `app/src/main/java/com/sphynxs/mydatabases/core/database/`
- `feature-editor` → `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/` (+ `domain/editor/`)
- `feature-explorer` → `ui/screens/databases/` (schema browsing lives here)

Any "new pipeline / module boundary" language in the proposal must be expressed as a **new package + new use cases**, not a new Gradle module. A prior exploration (`ui-implementation/explore.md`) already decided single-module-with-package-split is the standard until a second heavy feature justifies multi-module; this feature is a candidate to *reconsider* that, but that decision is out of scope here.

## 3. Current State — Execution Path

### 3.1 The engine abstraction (`core/database/engine/DatabaseEngine.kt`)

Relevant methods:

- `executeQuery(query, params): Result<QueryResult>` — SELECT, buffers **all** rows into `List<Map<String, Any?>>`.
- `executeUpdate(query, params): Result<Int>` — single DML/DDL, returns affected rows.
- `executeBatch(statements: List<String>): Result<List<BatchStatementResult>>` — the closest existing seam.

`executeBatch` (impl in `MySQLEngine.kt`, delegated by `MariaDBEngine.kt`) already does the two things this feature depends on:

1. Executes statements **sequentially on the SAME JDBC connection**, so `USE dbname;` mid-stream changes the context for subsequent statements — exactly the native session semantics required by requirement #4.
2. Classifies SELECT-like vs. non-SELECT by leading keyword (`SELECT/SHOW/DESCRIBE/EXPLAIN`).

**But it is not usable as-is for large scripts:**

- Its input is a fully-materialized `List<String>` — the caller must already hold every statement in memory.
- It **accumulates every result** (including full `QueryResult` row sets) into a `results` list before returning — memory grows with the whole script.
- It wraps the connection in `connection.use { ... }`, which **closes** the connection at batch end. So USE-context does not survive across separate `executeBatch` calls (fine for one script run, relevant if we ever chunk).
- It is all-or-nothing at the `Result` level; there is no incremental progress signal.

### 3.2 The current "Run" action (`QueryEditorViewModel.executeStatements`)

- Splits with a **naive `sql.split(";")`** — the KDoc itself warns `;` inside strings breaks it. There is **no comment/string-literal-aware splitter anywhere in the codebase**.
- Materializes the statement list, calls `ExecuteBatchStatementsUseCase`, aggregates into `SelectResult` / `UpdateSummary` / `Success` / `Error`.
- Cancellation is **UI-only** (`executionJob.cancel()`); it does not cancel the in-flight JDBC statement.
- Stops on first failure (best-effort is not the current behavior).

### 3.3 The connection layer (`MySQLConnectionPool.kt`)

- **"Pool" is a misnomer.** It uses `DriverManager.getConnection()` and creates a **fresh connection on every `getConnection()` call** — no pooling, no reuse.
- The **default database is baked into the JDBC URL path segment** (`jdbc:mysql://host:port/<config.database>`) at connect time. This is precisely the "active connection's current default database" from requirement #4 — no extra plumbing needed to honor it.
- Driver is `com.mysql.jdbc.Driver` **5.1.46** (2018, EOL; flagged as tech debt in multiple prior explorations). Consequence for this feature: **row-by-row ResultSet streaming requires `Statement.setFetchSize(Integer.MIN_VALUE)`** on that driver; the default fully buffers SELECT results client-side. Scripts containing large SELECTs would otherwise defeat the streaming intent.

### 3.4 Error + result models

- `QueryResult` (columns, rows, rowCount, executionTimeMs, warnings) — buffers all rows; fine for small statements, unbounded for large SELECTs.
- `DatabaseError` sealed class: `ConnectionFailed`, `AuthenticationFailed`, `QueryExecutionFailed(query, reason)`, `TimeoutError`, `InvalidConfiguration`, `UnknownError`. Requirement #4's "let the native `ERROR 1046 No database selected` surface" maps cleanly onto `QueryExecutionFailed.reason` — no custom preemptive UI needed.

### 3.5 File access primitives (already present)

- `QueryEditorScreen.kt` already opens `.sql` via SAF `GetContent()` + `contentResolver.openInputStream(uri)` — but then calls `BufferedReader.readText()` into a single `String` and pushes it into `TextFieldValue`. **This is the documented hang mechanism** (Compose lays out 600K lines). The new path must reuse the SAF/InputStream acquisition but **never** call `readText()` or touch `TextFieldValue`.
- `SSHKeyReader.kt`, `CertificateReader.kt`, and `ui/.../FilePicker.kt` all demonstrate the established `openInputStream(...).use { ... }` pattern to copy for streaming reads.

## 4. Feasibility Assessment (streaming vs. the existing engine abstraction)

**Overall: feasible, additive, no breaking changes to the engine contract.** The existing `DatabaseEngine`/`DatabaseRepository` contract does not need to be broken, but it does need to be **extended** with a streaming-capable execution entry point, because the current `executeBatch(List<String>)` is memory-bound on both input and output.

Key feasibility points:

- **Same-connection sequential execution already exists** — the hardest DB-session requirement (USE-context propagation) is a solved primitive; the new path just needs to feed it one statement at a time and hold the connection open for the whole stream instead of materializing a list.
- **Splitting is greenfield.** A cheap lexical splitter (comments `--`, `/* */`, `#`; string literals `'...'`, `"..."`, backtick identifiers) that consumes a `Reader` char-by-char and emits statements at top-level `;` is straightforward and 100% JVM-unit-testable. `SqlTokenizer.kt`'s regex rules are a **reference for the lexical states** (note it lacks `#` comments and is whole-String, so it cannot be reused directly).
- **Classification is a fixed rule table** over the leading keyword of each emitted statement — pure function, trivially testable, matches the agreed DDL/DELETE/UPDATE-without-WHERE rules. "UPDATE without top-level WHERE" needs the splitter/lexer to distinguish a top-level `WHERE` from one inside a subquery/string — the same lexical state machine that does splitting can answer this cheaply without a full AST.
- **Two-phase pre-scan then execute** fits the coroutine/Flow conventions already in the codebase (`.atl/agents/kotlin-expert.md`: inject `CoroutineDispatcher`, `Flow` for streams, cancel jobs in ViewModels). Pre-scan = one streaming pass emitting progress via `Flow`/`StateFlow`; execution = second streaming pass on the same file (or a lightweight index from pass 1).

## 5. Integration Points / Seams

| Seam | Location | Role in new feature |
|------|----------|---------------------|
| SAF file acquisition | `QueryEditorScreen` open-launcher pattern; `FilePicker.kt` | Reuse `openInputStream(uri)`; **do not** `readText()`. New entry point (e.g. a "Run script" action) triggers a launcher and hands the `Uri` to a use case, not to the editor. |
| Streaming splitter | **new** (`domain/` — e.g. `SqlStatementStreamSplitter`) | Char-stream lexer over a `Reader`; emits statements + line numbers; shared by pre-scan and execution. |
| Risk classifier | **new** (`domain/` — e.g. `StatementRiskClassifier`) | Pure rule table (DDL / DELETE / UPDATE-no-WHERE → confirm; INSERT/SELECT/UPDATE-with-WHERE → clean). |
| Pre-scan use case | **new** (`domain/usecases/`) | Streams file once, drives progress `Flow`, returns aggregated risk report. Cancelable. |
| Execution use case | **new** (`domain/usecases/`) | Streams + executes on one held-open connection. Candidate: a new `executeStreaming(Flow<String>)` / `Sequence<String>` entry on `DatabaseEngine`/`DatabaseRepository`, OR a new engine method that takes a `Reader`. Proposal must choose. |
| Engine execution | `MySQLEngine` / `MariaDBEngine` | Extend with a streaming execute that keeps one connection open, does NOT buffer all results, and sets `setFetchSize(Integer.MIN_VALUE)` for SELECTs. `MariaDBEngine` delegates to `MySQLEngine` (single implementation point). |
| Default-DB context | `MySQLConnectionPool` JDBC URL | Already correct — active connection's `config.database` is the default; `USE` statements override mid-stream naturally. No new code. |
| ViewModel + UI | **new** screen/VM, sibling to `queryeditor` | Sealed state machine (mirror `create-database-execution`'s `Idle/Submitting/Success/Error`, extended with `PreScanning(progress)`, `AwaitingConfirmation(report)`, `Executing(progress)`). One aggregated confirmation dialog. |
| i18n | `res/values/strings.xml` + `values-es/` | All new user-facing strings localized (android-dev skill: mandatory). Other locales fall back to default per established precedent. |

**Established precedent to follow** (`create-database-execution/design.md`): one use case per business action → repository → engine; SQL/lexical logic in the domain layer (pure, testable); error mapping to localized strings in the ViewModel (not the domain layer, which has no `Context`); sealed state machine as a thin VM.

## 6. Risks

- **R1 — Engine contract extension:** `executeBatch(List<String>)` cannot stream. Adding a streaming execute method to `DatabaseEngine` touches the abstraction and both engine impls. Additive, but it is the one place the "clean primitives only" boundary gets a new member. Must be justified in design (mirrors the `create-database-execution` ADR that resisted adding business methods to the engine — here the addition is a *generic* streaming primitive, not a business method, which keeps the boundary clean).
- **R2 — Large SELECT inside the script:** even with statement-level streaming, a single `SELECT * FROM huge_table;` will OOM because `QueryResult` buffers all rows and the 5.1.46 driver buffers client-side by default. Needs `setFetchSize(Integer.MIN_VALUE)` and a decision on how/whether to surface SELECT output at all in execution-only mode (counts vs. rows vs. discard).
- **R3 — Cancellation is currently UI-only:** true mid-execution cancel of a running JDBC statement is not implemented anywhere. The pre-scan (pure streaming) cancels cleanly; **execution cancel needs `Statement.cancel()` from another thread** or it will keep running server-side. Proposal must state the cancellation guarantee.
- **R4 — Lexical correctness at scale:** the splitter must correctly handle `''`/`""` escapes, backtick identifiers, nested `/* */`, `#` and `--` comments, `DELIMITER` changes (stored-procedure dumps use `DELIMITER $$`), and `;` inside strings. Getting "top-level WHERE" detection right for the UPDATE rule depends on this. Mis-splitting a 600K-line dump is a silent-corruption risk.
- **R5 — `DELIMITER` and multi-statement routines:** mysqldump output frequently contains `DELIMITER $$ ... $$ DELIMITER ;`. `DELIMITER` is a client directive, not server SQL. If unhandled, splitting breaks on the first trigger/procedure body. Needs an explicit decision (support vs. reject-with-message).
- **R6 — Old driver / connection semantics:** no pooling, fresh connection per call, EOL driver. Holding one connection open for the whole stream is a change in usage pattern (long-lived connection vs. per-op). Socket/read timeouts (`config.readTimeout`) may fire on a long-running script.
- **R7 — Scope overlap with backup-restore:** `.atl/product/features/backup-restore.md` already envisions "Restore from `.sql` files" (with GZIP/ZIP streams). The proposal should coordinate so this feature and restore don't build two divergent streaming importers.
- **R8 — Progress accuracy:** line-number tracking during streaming (for the risk report, open question) costs extra bookkeeping per char/newline. Counts-only is cheaper. This is both an open question and a perf risk on 600K lines.
- **R9 — No coverage/lint tooling:** `openspec/config.yaml` reports Kover/detekt not configured, `coverage_threshold: 0`. Strict TDD is on. Tests must be written but coverage is unenforced by tooling — discipline risk.

## 7. Open Questions (for the proposal phase to resolve — NOT decided here)

These are the user's explicitly-undecided items, framed with the technical context this exploration surfaced:

1. **Editor vs. execution-only threshold.** At what file size does "Run script" become the *only* option vs. still allowing the file to open in the visual editor? Technical input: the hang is a Compose text-layout limit, not a fixed byte count; a byte-size or line-count guard is the cheap gate. The proposal must pick a heuristic (bytes, line count, or both) and a fallback UX.

2. **Transaction wrapping vs. best-effort.** Wrap the whole script in a transaction (rollback on abort/failure) vs. best-effort statement-by-statement with partial completion? Technical input: (a) DDL in MySQL/MariaDB causes **implicit commits** — a single transaction cannot truly roll back a script that contains `CREATE/ALTER/DROP`, so "rollback everything" is partly a lie for DDL-heavy scripts; (b) a long-open transaction on 600K statements holds locks and undo log for the entire run (lock/memory cost). This interacts directly with the DDL classification rule and R6.

3. **Risk report detail — counts vs. exact line numbers.** Counts-per-category are cheap (single pass, O(1) state). Exact line numbers require tracking newline offsets per statement during streaming (R8). The proposal must decide the report granularity, knowing the pre-scan already reads the whole file once regardless.

Additional questions this exploration surfaced that the proposal should also address:
- **`DELIMITER` handling** (R5): support stored-procedure dumps, or reject with a clear message in v1?
- **SELECT output in execution-only mode** (R2): show row counts, stream a capped preview, or discard result sets entirely?
- **Execution cancellation guarantee** (R3): UI-only (matches current editor) or true `Statement.cancel()`?

## 8. Ready for Proposal?

**Yes.** The approach is feasible and additive; the hardest DB primitive (same-connection sequential execution with USE-context) already exists, and the default-DB resolution (requirement #4) needs no new code. The proposal must (a) decide how to extend the engine/repository with a streaming execution primitive without polluting the clean-primitives boundary, (b) resolve the three user open questions plus the three surfaced ones (`DELIMITER`, SELECT output, cancel guarantee), and (c) commit to a comment/string/`DELIMITER`-aware streaming splitter as the correctness-critical component. Recommend the orchestrator batch the six open questions to the user before `sdd-propose`.
