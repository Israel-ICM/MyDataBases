# query-runner Specification

## Purpose

Ad-hoc SQL editor with execution, paginated result grid, and clear error surfacing. Plain `TextField` input (no syntax highlighting in this delivery).

## Requirements

### Requirement: SQL Input

The system MUST provide a multi-line `TextField` for SQL input that preserves whitespace and line breaks.

#### Scenario: Multi-line query

- GIVEN the user types a 5-line `SELECT`
- WHEN focus leaves the field
- THEN all 5 lines are preserved exactly as typed

#### Scenario: Tab character

- GIVEN the user pastes a query containing tab characters
- WHEN the query renders
- THEN tab characters are preserved (no replacement with spaces)

### Requirement: Execute Action

An explicit "Run" action MUST execute the current query against the active connection and database. The action MUST be disabled when the input is empty or whitespace-only.

#### Scenario: Run disabled

- GIVEN the input is empty
- WHEN the screen renders
- THEN the "Run" button is disabled

#### Scenario: Run enabled

- GIVEN the input contains non-whitespace
- WHEN the screen renders
- THEN the "Run" button is enabled

### Requirement: Result Grid

`SELECT`-style results MUST render in a 2D grid: column headers row + data rows. Cells MUST be horizontally scrollable.

#### Scenario: Wide result

- GIVEN a query returns 20 columns
- WHEN results render
- THEN the user can horizontally scroll to reach all columns AND the header row scrolls in sync

#### Scenario: Null value display

- GIVEN a cell value is SQL `NULL`
- WHEN it renders
- THEN it displays as a visually distinct `NULL` indicator (not empty string)

### Requirement: Non-SELECT Result

For `INSERT`/`UPDATE`/`DELETE`/DDL, the system MUST display the affected row count or a success confirmation.

#### Scenario: Update result

- GIVEN the user runs `UPDATE users SET active = 1`
- WHEN the query succeeds
- THEN a confirmation shows the affected row count

### Requirement: Error Display

Engine errors MUST be displayed inline above the editor with the SQL error code (if any) and a localized prose message. The editor MUST remain focused on the input for editing.

#### Scenario: Syntax error

- GIVEN the query has a SQL syntax error
- WHEN the user taps Run
- THEN the error message is shown above the editor AND the cursor remains in the input

### Requirement: Row Cap

Initial result rendering MUST be capped at 1000 rows with a "Load more" or pagination control.

#### Scenario: Large result

- GIVEN the query returns 50,000 rows
- WHEN the result renders
- THEN at most 1000 rows are loaded into memory initially

### Requirement: Cancel Long Query

The system SHOULD allow cancelling a query that has not yet returned. Cancellation MUST close the underlying statement.

#### Scenario: User cancels

- GIVEN a query is running
- WHEN the user taps "Cancel"
- THEN the coroutine is cancelled AND the underlying JDBC statement is closed

## Non-Functional

- **Performance**: First result row MUST appear within 500ms of receiving engine response. UI thread MUST NOT block.
- **Security**: Query text MUST NOT be persisted to disk in this delivery (no history feature). Query text MUST NOT appear in logs.
- **Testability**: ViewModel MUST be unit-testable with a fake `QueryExecutor`. Result grid MUST be exercised by Compose UI test for the critical "run and display" path.
- **Accessibility**: Result cells MUST expose row/column position via content description; "Run" button MUST be reachable by keyboard.
