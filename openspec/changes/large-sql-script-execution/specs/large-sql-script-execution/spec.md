# large-sql-script-execution Specification

## Purpose

Defines the "Run script" path that lets users execute large `.sql` files (up to ~600,000 lines) that cannot be loaded into the visual editor without hanging Compose text layout. The path is fully decoupled from the editor: it streams the file from a SAF `Uri` via `SqlStatementStreamSplitter` (never `readText()` / `TextFieldValue`), runs a two-phase flow — Phase A pre-scans and classifies every statement into one aggregated `RiskReport`, then a single confirmation dialog, then Phase B executes statements sequentially on one held-open active connection with progress and true cancellation. Owns: `PreScanScriptUseCase`, `ExecuteScriptUseCase`, the `RunScriptViewModel` sealed state machine, the aggregated confirmation dialog, the 50,000-line editor guard that routes oversized files here, the `new_query` entry-point selector (three options: New Query / Open Query File / Run Script (No Edit)) that surfaces this path to users, best-effort partial-failure reporting, and localized (es + en) user-facing strings. Execution semantics (context inheritance, sequential `USE`, error mapping) follow the `create-database-execution` precedent and reuse the same-connection model validated by `executeBatch`, via a NEW generic `executeScript` engine primitive.

## Requirements

### Requirement: Statement Risk Classification

`StatementRiskClassifier` MUST classify each statement as `CONFIRM` or `CLEAN` using this fixed, locked rule table (case-insensitive on the leading keyword; runtime-configurable rules are out of scope):

| Statement kind | Rule | Class |
|---|---|---|
| DDL: `CREATE` / `ALTER` / `DROP` / `TRUNCATE` / `RENAME` | Always confirm | CONFIRM |
| `DELETE` (with or without WHERE) | Always confirm | CONFIRM |
| `UPDATE` without top-level `WHERE` | Confirm | CONFIRM |
| `UPDATE` with top-level `WHERE` | No confirm | CLEAN |
| `INSERT` | Never confirm | CLEAN |
| `SELECT` | Never confirm | CLEAN |

The classifier MUST be a pure function of the statement text plus the splitter's top-level-WHERE flag. It MUST NOT execute or connect to anything. `DELIMITER`-defined stored-procedure bodies (e.g. `CREATE PROCEDURE`) classify by their leading DDL keyword (`CONFIRM`).

#### Scenario: DDL always confirms

- GIVEN the statements `DROP TABLE t`, `ALTER TABLE t ADD c INT`, `TRUNCATE t`, `CREATE TABLE u (id INT)`, `RENAME TABLE a TO b`
- WHEN each is classified
- THEN every one is classified `CONFIRM`

#### Scenario: DELETE always confirms regardless of WHERE

- GIVEN `DELETE FROM t` and `DELETE FROM t WHERE id = 1`
- WHEN each is classified
- THEN both are classified `CONFIRM`

#### Scenario: UPDATE without top-level WHERE confirms

- GIVEN `UPDATE t SET x = 1` (top-level WHERE absent per the splitter)
- WHEN it is classified
- THEN it is classified `CONFIRM`

#### Scenario: UPDATE with top-level WHERE is clean

- GIVEN `UPDATE t SET x = 1 WHERE id = 5` (top-level WHERE present)
- WHEN it is classified
- THEN it is classified `CLEAN`

#### Scenario: INSERT and SELECT are clean

- GIVEN `INSERT INTO t VALUES (1)` and `SELECT * FROM t`
- WHEN each is classified
- THEN both are classified `CLEAN`

### Requirement: Pre-Scan Phase (Phase A)

`PreScanScriptUseCase` MUST perform exactly one cancelable streaming pass over the script, classifying every statement without executing any of them, and MUST return an aggregated `RiskReport`. The report MUST contain, per risk category (DDL, DELETE, UPDATE-no-WHERE), a count of matching statements AND the exact 1-based source line numbers of each. The report MUST also expose the total statement count. Pre-scan MUST emit progress (e.g. statements scanned / bytes read) via a `Flow` and MUST cancel cleanly at any point, since it only reads (no DB writes). If the splitter raises an unparseable-`DELIMITER` (or other structural) error, pre-scan MUST surface it as an error outcome and MUST NOT proceed to confirmation or execution.

#### Scenario: Pre-scan aggregates counts and line numbers

- GIVEN a script with 3 DDL, 2 `DELETE`, and 1 `UPDATE`-without-WHERE statements among 5,000 total
- WHEN pre-scan completes
- THEN the `RiskReport` reports 3 DDL, 2 DELETE, 1 UPDATE-no-WHERE
- AND it lists the exact line number of each of those 6 risky statements
- AND it reports the total statement count 5,000

#### Scenario: Pre-scan runs without executing

- GIVEN a script containing `DROP TABLE prod`
- WHEN pre-scan runs
- THEN no statement is executed against the connection
- AND `DROP TABLE prod` appears in the DDL category with its line number

#### Scenario: Pre-scan is cancelable mid-pass

- GIVEN pre-scan is in progress on a large script
- WHEN the user cancels
- THEN the streaming pass stops promptly
- AND the state machine transitions to `Cancelled`
- AND no `RiskReport` is finalized and no execution begins

#### Scenario: Pre-scan surfaces a structural parse error

- GIVEN a script whose `DELIMITER` directive is unparseable
- WHEN pre-scan reaches it
- THEN the outcome is `Error` carrying the splitter's typed message
- AND no confirmation dialog is shown and no statement is executed

### Requirement: Aggregated Confirmation

The system MUST show EXACTLY ONE aggregated confirmation dialog, once, after a successful pre-scan and BEFORE any statement executes, and ONLY when the `RiskReport` contains at least one `CONFIRM` statement. The dialog MUST present the per-category counts and the affected line numbers. If the report contains zero risky statements, the system MUST proceed directly to execution with no dialog. Dismissing/declining the dialog MUST abort without executing anything. Confirming MUST start Phase B. The system MUST NOT prompt again per-statement during execution.

#### Scenario: Risky script shows one aggregated dialog

- GIVEN a `RiskReport` with 3 DDL, 2 DELETE, 1 UPDATE-no-WHERE
- WHEN pre-scan completes
- THEN one confirmation dialog appears showing the three categories, their counts, and affected line numbers
- AND no execution has started yet

#### Scenario: Clean script skips the dialog

- GIVEN a `RiskReport` whose only statements are `INSERT`, `SELECT`, and `UPDATE ... WHERE`
- WHEN pre-scan completes
- THEN no confirmation dialog is shown
- AND execution begins directly

#### Scenario: Declining the dialog aborts

- GIVEN the confirmation dialog is shown
- WHEN the user declines/dismisses it
- THEN no statement is executed
- AND the state machine returns to `Idle`

#### Scenario: Confirmation is requested only once

- GIVEN a script with many risky statements across the file
- WHEN the user confirms the single dialog
- THEN execution runs all statements without any further prompt

### Requirement: Execution Phase (Phase B)

`ExecuteScriptUseCase` MUST stream the script a second time and execute statements sequentially against the active connection through the engine's generic `executeScript` primitive, holding ONE connection open for the entire run and NOT buffering all statements or all results in memory. It MUST emit per-statement progress via a `Flow`. Execution MUST use best-effort semantics with NO whole-script transaction or rollback: DDL causes implicit commits, so a global rollback is not offered. On success, the outcome MUST report completion (e.g. statements executed, total affected rows where meaningful, elapsed time).

#### Scenario: Sequential execution to success

- GIVEN a confirmed script of N statements against an active connection
- WHEN execution runs
- THEN each statement executes in declaration order on one held-open connection
- AND per-statement progress is emitted
- AND on completion the state machine transitions to `Success` reporting the executed count

#### Scenario: Results are not buffered

- GIVEN a script mixing many `INSERT` and `SELECT` statements
- WHEN execution runs
- THEN result sets are not accumulated across statements
- AND memory does not grow with the number of executed statements

### Requirement: Database Context Inheritance and USE Override

Execution MUST inherit the default database context from the active connection (`config.database`). A `USE <db>` statement encountered in the script MUST change the active context for all subsequent statements on the same connection, sequentially. When no default database is set and no `USE` has run, a statement requiring a database MUST surface the native engine error unmodified (e.g. MySQL error 1046 "No database selected") — the system MUST NOT invent a custom "pick a database" dialog.

#### Scenario: Statements run against the connection's default database

- GIVEN the active connection has `config.database = app_db`
- WHEN the script runs `INSERT INTO users VALUES (1)` with no `USE`
- THEN the insert targets `app_db.users`

#### Scenario: USE switches context for subsequent statements

- GIVEN a script `USE reporting; SELECT COUNT(*) FROM events; USE app_db; INSERT INTO users VALUES (1);`
- WHEN execution runs
- THEN the `SELECT` runs against `reporting`
- AND the `INSERT` runs against `app_db`

#### Scenario: No database context surfaces native error 1046

- GIVEN the active connection has no default database and no `USE` has run
- WHEN a statement requiring a database executes
- THEN the native engine error (e.g. 1046 "No database selected") is surfaced unmodified as the failure reason

### Requirement: Partial-Failure Reporting

When any statement fails mid-execution, the system MUST STOP immediately (no further statements) and MUST report a clear outcome identifying the failing statement's ordinal N and its source line, plus the native engine error, and MUST warn that the database may be partially updated. The system MUST NOT attempt to roll back already-executed statements and MUST NOT report false success. The failure outcome MUST be localized (es + en).

#### Scenario: Mid-execution failure stops and reports partial update

- GIVEN a script where statement 42 (line 613) violates a constraint
- WHEN execution reaches statement 42 and the engine throws
- THEN execution stops before statement 43
- AND the outcome is `Error` stating it stopped at statement 42 (line 613) with the native error message
- AND the outcome warns the database may be partially updated
- AND no rollback of statements 1–41 is attempted

#### Scenario: No false success after failure

- GIVEN a statement fails partway through the script
- WHEN the outcome is produced
- THEN the state machine is `Error`, never `Success`

### Requirement: Execution Cancellation

The system MUST expose a Cancel control during execution that performs a TRUE mid-execution cancel by invoking `Statement.cancel()` from a separate thread, not merely cancelling the UI coroutine. On cancel, in-flight server-side work MUST be signalled to stop, execution MUST halt before the next statement, and the outcome MUST report cancellation with the same partial-update warning as a failure (already-run statements are not rolled back). Pre-scan cancellation (read-only) needs no server-side cancel and MUST stop cleanly.

#### Scenario: Cancel halts the running statement server-side

- GIVEN a long-running statement is executing
- WHEN the user taps Cancel
- THEN `Statement.cancel()` is invoked from a separate thread against the running statement
- AND execution does not proceed to the next statement
- AND the outcome is `Cancelled` with the partial-update warning

#### Scenario: Cancel does not roll back completed statements

- GIVEN 10 statements have already committed and the 11th is running
- WHEN the user cancels
- THEN statements 1–10 remain applied (no rollback)
- AND the outcome reports cancellation at statement 11

### Requirement: SELECT Handling in Execution Mode

`SELECT` (and other row-returning) statements inside a script MUST execute with `setFetchSize(Integer.MIN_VALUE)` so the driver streams rows instead of buffering them, and their result rows MUST be discarded — only the row count MAY be reported in progress. A `SELECT * FROM huge_table` MUST NOT cause an OutOfMemoryError. Full result rendering is the editor's job and is out of scope here.

#### Scenario: Large SELECT streams without OOM

- GIVEN a script containing `SELECT * FROM huge_table` (millions of rows)
- WHEN it executes in script mode
- THEN the driver fetch size is set to stream row-by-row (`Integer.MIN_VALUE`)
- AND the rows are discarded (only a count may be surfaced)
- AND no OutOfMemoryError occurs

### Requirement: Oversized-File Editor Guard

When the user opens a `.sql` file through the editor, the system MUST cheaply determine the file's line count while acquiring the stream. If the line count is strictly greater than 50,000, the editor MUST NOT load the file into `TextFieldValue` and MUST instead offer only the "Run script" action for that file. At 50,000 lines or below, the editor MUST open the file normally (and MAY also offer "Run script"). The guard MUST NOT call `readText()` on an oversized file. This threshold is a fixed constant for v1 (not user-configurable) — it protects against a Compose text-rendering limitation, not a user preference.

#### Scenario: File above 50,000 lines routes to Run script only

- GIVEN the user selects a `.sql` file with 120,000 lines
- WHEN the open action runs
- THEN the file is not loaded into the editor
- AND only the "Run script" action is offered for it
- AND `readText()` is never called on the file

#### Scenario: File below 50,000 lines opens in the editor

- GIVEN the user selects a `.sql` file with 8,000 lines
- WHEN the open action runs
- THEN the file opens normally in the editor

#### Scenario: File exactly at the threshold opens in the editor

- GIVEN the user selects a `.sql` file with exactly 50,000 lines
- WHEN the open action runs
- THEN the file opens normally in the editor (threshold is exclusive; only files strictly above 50,000 lines are execution-only)

### Requirement: New-Query Entry-Point Selector

Invoking the `new_query` entry point MUST open a "What do you want to do?" selector as an overlay instead of launching a blank query card directly. The selector MUST present exactly three options in this fixed order: (1) **New Query**, (2) **Open Query File**, (3) **Run Script (No Edit)**. Selecting an option MUST perform its action and dismiss the selector.

- **New Query** MUST launch a blank query card in the editor with no initial SQL — behaviorally identical to the prior direct `new_query` action.
- **Open Query File** MUST present a `.sql` file picker, then apply the **Oversized-File Editor Guard** (defined above): a file strictly greater than 50,000 lines MUST be redirected to the "Run script" pre-scan/confirm/execute flow instead of the editor, while a file at or below 50,000 lines MUST load into the editor. The guard is the single authority for this decision and MUST NOT be re-specified here.
- **Run Script (No Edit)** MUST present a `.sql` file picker, then always enter the pre-scan/confirm/execute flow regardless of file size, never touching the editor.

The selector MUST be presented as an overlay that preserves the `2026-06-30-new-query-modal-fix` guarantees: it MUST NOT trigger a navigation route change, MUST NOT cause a workspace context switch, and MUST NOT produce a double sheet. All selector user-facing text (title and the three option labels/descriptions) MUST be localized in `values/` (en) and `values-es/` (es).

#### Scenario: New-query entry opens the three-option selector

- GIVEN the user is in a workspace and taps "New Query"
- WHEN the entry point activates
- THEN the selector appears as an overlay showing exactly three options in order: New Query, Open Query File, Run Script (No Edit)
- AND no navigation route change, context switch, or double sheet occurs

#### Scenario: New Query option launches a blank editor card

- GIVEN the selector is shown
- WHEN the user chooses "New Query"
- THEN a blank query card opens in the editor with no initial SQL
- AND the selector is dismissed

#### Scenario: Open Query File with a small file loads into the editor

- GIVEN the selector is shown
- WHEN the user chooses "Open Query File" and picks a `.sql` file with 8,000 lines
- THEN the Oversized-File Editor Guard permits it and the file opens in the editor
- AND the selector is dismissed

#### Scenario: Open Query File with an oversized file redirects to Run script

- GIVEN the selector is shown
- WHEN the user chooses "Open Query File" and picks a `.sql` file with 120,000 lines
- THEN the Oversized-File Editor Guard redirects it to the pre-scan/confirm/execute flow instead of the editor
- AND `readText()` is never called on the file

#### Scenario: Run Script (No Edit) always enters pre-scan regardless of size

- GIVEN the selector is shown
- WHEN the user chooses "Run Script (No Edit)" and picks any `.sql` file
- THEN the pre-scan/confirm/execute flow starts regardless of the file's line count
- AND the editor is never involved

### Requirement: State Machine and Outcome Surfacing

`RunScriptViewModel` MUST expose a sealed state as an immutable `StateFlow` with states `Idle → PreScanning(progress) → AwaitingConfirmation(report) → Executing(progress) → Success | Error | Cancelled`. The ViewModel MUST remain `Context`-free and map engine `DatabaseError`s to localized strings via a resolver (per the `create-database-execution` precedent). All user-facing text — progress labels, risk report, confirmation dialog, success, error (including stopped-at-N and native-error), and cancellation — MUST be defined as localized string resources in both `values/` (en) and `values-es/` (es). The screen MUST adapt to Compact / Medium / Expanded `WindowSizeClass`.

#### Scenario: State transitions across a full risky run

- GIVEN a risky script and an active connection
- WHEN the user runs it, confirms, and it completes
- THEN the state moves `Idle → PreScanning → AwaitingConfirmation → Executing → Success`
- AND each transition exposes its progress/report payload

#### Scenario: Engine error is surfaced localized

- GIVEN execution fails with a native `SQLException`
- WHEN the ViewModel maps it
- THEN the `Error` state carries a localized (es/en) message including the native reason
- AND the ViewModel never references an Android `Context`

#### Scenario: No active connection reports ConnectionFailed

- GIVEN no engine is connected
- WHEN the user attempts to run a script
- THEN the outcome is `Error` derived from `DatabaseError.ConnectionFailed`, surfaced as a localized message

## Non-Functional

- **Performance**: A ~600,000-line script MUST run via "Run script" without hanging the UI and without loading the file into the editor. Pre-scan and execution each stream the file once; neither buffers the whole file or all results.
- **Security**: Script content MUST NOT be persisted to disk or written to logs. Execution runs only against the user's active connection.
- **Adaptive UI**: The Run Script screen MUST render correctly across Compact, Medium, and Expanded `WindowSizeClass`.
- **Localization**: Every user-facing string (progress, risk report, confirmation, success, error, cancellation, partial-update warning, oversized-file guard message) MUST exist in `values/` (en) and `values-es/` (es).
- **Testability**: `StatementRiskClassifier` and the use cases MUST be unit-testable on the JVM with fakes/Mockk (no Compose/Robolectric); the ViewModel MUST be unit-testable for state transitions, cancellation, and error mapping; the screen SHOULD have a Compose UI test for the pre-scan → confirm → execute happy path.
