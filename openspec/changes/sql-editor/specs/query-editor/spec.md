# query-editor Specification

## Purpose

Professional SQL editor with native syntax highlighting, monospace input, and an embedded execution pipeline. Replaces the `query-editor-placeholder` and extends the prior plain-text `query-runner` with token-coloured highlighting, tab insertion, multi-statement execution, and cancellation. Renders inside a `WorkspaceCard.Query` instance so multiple editors can coexist.

## Requirements

### Requirement: Editor Surface

The editor MUST render a multi-line text input with monospace font, native Compose rendering, and SQL syntax highlighting that follows the active `MaterialTheme.colorScheme`.

#### Scenario: Open new editor

- GIVEN the user opens a new query card
- WHEN the editor mounts
- THEN an empty editable surface is shown with monospace font
- AND the Execute button is disabled
- AND no error or result is displayed

#### Scenario: Theme follows app theme

- GIVEN the app is in dark theme
- WHEN the editor renders highlighted SQL
- THEN keyword, string, comment, and number token colours come from `MaterialTheme.colorScheme` (no hard-coded colours)

### Requirement: Syntax Highlighting

The editor MUST colour SQL tokens by category as the user types. Tokenization MUST run natively (no WebView) and MUST NOT block the UI thread.

The categories that MUST be highlighted:

| Token Category | Examples |
|---|---|
| Keyword | `SELECT`, `FROM`, `WHERE`, `JOIN`, `INSERT`, `UPDATE`, `DELETE`, `CREATE`, `DROP`, `ALTER`, `INTO`, `VALUES`, `SET`, `AND`, `OR`, `NOT`, `NULL`, `AS`, `ON` |
| String | `'value'`, `"value"` |
| Comment | `-- line comment`, `/* block comment */` |
| Number | `123`, `45.67` |

#### Scenario: Keyword highlight

- GIVEN the editor is empty
- WHEN the user types `SELECT * FROM users`
- THEN `SELECT` and `FROM` render in the keyword colour
- AND `*` and `users` render in the default text colour

#### Scenario: String highlight

- GIVEN the editor contains `WHERE name = 'Ada'`
- WHEN highlighting runs
- THEN the substring `'Ada'` renders in the string colour

#### Scenario: Comment highlight

- GIVEN the editor contains `-- old query\nSELECT 1`
- WHEN highlighting runs
- THEN the line `-- old query` renders in the comment colour
- AND `SELECT` on the next line renders in the keyword colour

#### Scenario: Block comment across lines

- GIVEN the editor contains `/* multi\n   line */ SELECT 1`
- WHEN highlighting runs
- THEN every character from `/*` through `*/` renders in the comment colour

#### Scenario: Number highlight

- GIVEN the editor contains `WHERE price > 45.67 AND qty = 3`
- WHEN highlighting runs
- THEN `45.67` and `3` render in the number colour

#### Scenario: Case-insensitive keywords

- GIVEN the editor contains `select * from Users where Id = 1`
- WHEN highlighting runs
- THEN `select`, `from`, and `where` render in the keyword colour regardless of case

### Requirement: Tab Key Behaviour

Pressing the Tab key MUST insert four space characters at the caret position. The editor MUST NOT move focus away on Tab.

#### Scenario: Tab inserts spaces

- GIVEN the caret is at column 0 of an empty line
- WHEN the user presses Tab on a physical keyboard
- THEN four space characters are inserted
- AND the caret advances by four columns
- AND focus stays in the editor

### Requirement: Execute Action

The editor MUST expose an Execute control that runs the editor contents against the active connection and database. The control MUST be disabled when the input is empty or whitespace-only and while a query is in flight.

#### Scenario: Execute disabled when empty

- GIVEN the editor contents are empty or only whitespace
- WHEN the toolbar renders
- THEN the Execute button is disabled

#### Scenario: Execute enabled with content

- GIVEN the editor contains non-whitespace SQL
- WHEN the toolbar renders
- THEN the Execute button is enabled

### Requirement: Multi-Statement Execution

When the editor contents contain multiple statements separated by `;` (semicolons outside string and comment regions), the system MUST execute every statement in declaration order.

#### Scenario: Multiple SELECTs show last result

- GIVEN the editor contains `SELECT 1; SELECT 2; SELECT 3;`
- WHEN the user taps Execute
- THEN all three statements execute in order
- AND only the result of `SELECT 3` is displayed in the result grid

#### Scenario: Mixed statements with trailing SELECT

- GIVEN the editor contains `UPDATE users SET active = 1; SELECT COUNT(*) FROM users;`
- WHEN the user taps Execute
- THEN both statements execute in order
- AND the result grid displays the `SELECT COUNT(*)` result
- AND no separate update summary is shown for the first statement

#### Scenario: Semicolon inside string is not a separator

- GIVEN the editor contains `SELECT 'a;b' FROM dual`
- WHEN the user taps Execute
- THEN a single statement is executed
- AND the `;` inside the string literal is preserved

### Requirement: Result Presentation

When the final executed statement is a row-returning statement (`SELECT`, `SHOW`, `DESCRIBE`, `EXPLAIN`, `WITH`), the system MUST render results using the shared `result-grid` component.

When the final executed statement is a non-row-returning statement (`INSERT`, `UPDATE`, `DELETE`, DDL), the system MUST render a summary table showing the affected row count and execution time.

#### Scenario: SELECT renders grid

- GIVEN the user executes `SELECT id, name FROM users LIMIT 3`
- WHEN the engine returns rows
- THEN the result panel renders the shared `result-grid` with columns `id` and `name`
- AND the three data rows are visible

#### Scenario: UPDATE renders summary table

- GIVEN the user executes `UPDATE users SET active = 1 WHERE id = 5`
- WHEN the engine returns one affected row
- THEN a summary table is shown with the affected row count `1`
- AND the result-grid is not displayed

#### Scenario: INSERT renders summary table

- GIVEN the user executes `INSERT INTO logs (msg) VALUES ('hi')`
- WHEN the engine confirms one affected row
- THEN a summary table is shown with the affected row count `1`

### Requirement: Loading State

While a query is in flight, the editor MUST show a loading indicator and MUST disable Execute. Editor contents MUST remain editable visually but MUST NOT be re-runnable until the in-flight query resolves or is cancelled.

#### Scenario: Loading indicator visible

- GIVEN the user taps Execute on a valid query
- WHEN the engine has not yet returned
- THEN a loading indicator is visible in the toolbar
- AND the Execute button is disabled
- AND the Cancel button is enabled

### Requirement: Cancel Action

The editor MUST expose a Cancel control that is visible only while a query is in flight. Tapping Cancel MUST abandon the in-flight UI work and return the editor to its idle state. v1 cancellation is UI-only: the underlying JDBC statement MAY continue to completion on the engine side.

#### Scenario: Cancel during execution

- GIVEN a query is in flight and the loading indicator is visible
- WHEN the user taps Cancel
- THEN the coroutine collecting the result is cancelled
- AND the loading indicator disappears
- AND the Execute button becomes enabled again
- AND no result or error is rendered for that run

#### Scenario: Cancel hidden when idle

- GIVEN the editor is idle (no query in flight)
- WHEN the toolbar renders
- THEN the Cancel button is not visible or is disabled

### Requirement: Error Display

Engine errors MUST be displayed inline above the editor surface with the SQL error code (if any) and a localized message. The editor MUST retain its current contents and caret position so the user can edit and re-run.

#### Scenario: Syntax error

- GIVEN the editor contains `SELEKT * FROM users`
- WHEN the user taps Execute
- THEN an error banner appears above the editor with the engine's error message
- AND the editor contents remain `SELEKT * FROM users`
- AND no result grid or summary is rendered

#### Scenario: Error clears on next run

- GIVEN an error banner is displayed from a previous failed run
- WHEN the user taps Execute on a corrected query that succeeds
- THEN the error banner is removed
- AND the new result is rendered

### Requirement: Independent Editor Instances

Each `WorkspaceCard.Query` instance MUST own its own editor state: contents, caret, scroll position, loading state, result, and error. Switching between cards MUST NOT swap state between editors.

#### Scenario: Two query cards open

- GIVEN card A has contents `SELECT 1` and card B has contents `SELECT 2`
- WHEN the user switches focus from card A to card B
- THEN card B shows `SELECT 2`
- AND switching back to card A shows `SELECT 1` unchanged

#### Scenario: Independent execution

- GIVEN card A is currently running a query
- WHEN the user taps Execute on card B with a different query
- THEN card B starts its own execution independently
- AND card A's loading state and result are unaffected by card B's run

## Non-Functional

- **Performance**: Tokenization MUST remain responsive for queries up to 5 KB. Re-tokenization MAY be debounced or scoped to changed regions to avoid UI jank.
- **Security**: Query text MUST NOT be persisted to disk and MUST NOT appear in logs.
- **Testability**: The SQL tokenizer MUST be a pure function unit-testable on the JVM without Compose or Robolectric. The ViewModel MUST be unit-testable with fake use cases. The screen MUST have at least one Compose UI test covering the run-and-display happy path.
- **Accessibility**: The Execute and Cancel controls MUST be reachable by keyboard and MUST expose content descriptions.
