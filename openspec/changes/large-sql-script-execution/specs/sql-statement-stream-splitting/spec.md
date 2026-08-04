# sql-statement-stream-splitting Specification

## Purpose

Defines `SqlStatementStreamSplitter` — a comment/string/backtick/`DELIMITER`-aware streaming lexer that reads a `.sql` script from a `java.io.Reader` character-by-character and emits statements one at a time, each tagged with the 1-based source line number where it begins. It NEVER materializes the whole file as a `String` (the file may be ~600,000 lines). It answers "does this statement contain a top-level `WHERE`?" to power the UPDATE classification rule. It is the correctness-critical component of large-script execution: a mis-split silently corrupts what runs against the database, so ambiguity MUST fail loud, never guess. It is greenfield and reusable by future importers (e.g. restore); the existing whole-`String` regex `SqlTokenizer` is reference-only and MUST NOT be reused.

## Requirements

### Requirement: Streaming Contract

The splitter MUST consume its input from a `Reader` (or equivalent character stream) and MUST NOT call `readText()` or otherwise buffer the entire input into a single `String`. It MUST emit each parsed statement as soon as its terminator is reached, exposing statements lazily (e.g. via `Sequence`/`Flow`) so a caller can process and discard each before the next is read. Peak memory MUST be bounded by the largest single statement, not the file size. Reads that cross an internal buffer boundary MUST NOT split or corrupt a token (a string, comment, backtick identifier, keyword, or statement may span any buffer boundary).

#### Scenario: Whole file is never materialized

- GIVEN a `.sql` script of ~600,000 lines supplied as a `Reader`
- WHEN the splitter runs to completion
- THEN statements are emitted incrementally as terminators are reached
- AND at no point is the full file content held as a single `String`
- AND peak retained memory is bounded by the largest individual statement

#### Scenario: Token spanning a read-buffer boundary is preserved

- GIVEN a statement whose string literal `'... long value ...'` straddles the boundary between two internal buffer fills
- WHEN the splitter reads across that boundary
- THEN the literal is reassembled intact
- AND the statement is emitted with its content uncorrupted and unsplit

### Requirement: Statement Termination

The splitter MUST treat the active statement terminator (default `;`) as a statement boundary ONLY when it appears at top level — i.e. outside any string literal, backtick-quoted identifier, line comment, or block comment. Consecutive terminators and terminators surrounded by whitespace MUST NOT produce empty statements. Leading/trailing whitespace around an emitted statement MUST be trimmed for classification purposes, but the emitted statement MUST remain semantically complete SQL.

#### Scenario: Top-level semicolon splits statements

- GIVEN the input `INSERT INTO t VALUES (1); INSERT INTO t VALUES (2);`
- WHEN the splitter runs
- THEN exactly two statements are emitted in order
- AND each carries the 1-based line number where it begins

#### Scenario: Empty statements between terminators are skipped

- GIVEN the input `SELECT 1;;;\n;\nSELECT 2;`
- WHEN the splitter runs
- THEN exactly two non-empty statements are emitted
- AND no empty statement is emitted for the redundant `;` characters

### Requirement: String and Identifier Awareness

A terminator, comment marker, or `DELIMITER` keyword appearing inside a single-quoted string (`'...'`), a double-quoted string (`"..."`), or a backtick-quoted identifier (`` `...` ``) MUST NOT be interpreted as SQL syntax. The splitter MUST honor escaping: a backslash inside a string escapes the next character, and a doubled quote (`''`, `""`, ` `` `) is a literal quote that does NOT close the quoted region.

#### Scenario: Semicolon inside a string is not a boundary

- GIVEN the input `INSERT INTO t VALUES ('a;b');`
- WHEN the splitter runs
- THEN exactly one statement is emitted
- AND the `;` inside `'a;b'` is preserved as data

#### Scenario: Backslash-escaped quote does not close the string

- GIVEN the input `INSERT INTO t VALUES ('it\'s; fine');`
- WHEN the splitter runs
- THEN exactly one statement is emitted
- AND the escaped `\'` and the inner `;` are preserved inside the literal

#### Scenario: Doubled quote is a literal, not a close

- GIVEN the input `INSERT INTO t VALUES ('O''Brien; Co');`
- WHEN the splitter runs
- THEN exactly one statement is emitted
- AND the `''` is treated as a literal apostrophe, keeping the inner `;` inside the string

#### Scenario: Semicolon inside a backtick identifier is not a boundary

- GIVEN the input `` SELECT * FROM `weird;name`; ``
- WHEN the splitter runs
- THEN exactly one statement is emitted
- AND the `;` inside the backtick identifier is preserved

#### Scenario: Comment marker inside a string is inert

- GIVEN the input `INSERT INTO t VALUES ('-- not a comment');`
- WHEN the splitter runs
- THEN exactly one statement is emitted
- AND the `--` inside the literal does not start a comment

### Requirement: Comment Awareness

The splitter MUST recognize and skip line comments introduced by `--` (followed by whitespace/EOL per MySQL) and by `#`, terminating at end-of-line, and block comments delimited by `/* ... */`. Terminators and `DELIMITER` directives inside comments MUST be ignored. Comment content MUST NOT be interpreted as SQL. Line numbers MUST continue to advance across comment lines so reported line numbers stay accurate.

#### Scenario: Dash line comment is skipped

- GIVEN the input `-- drop everything;\nSELECT 1;`
- WHEN the splitter runs
- THEN one statement `SELECT 1` is emitted
- AND the `;` inside the `--` comment is not a boundary

#### Scenario: Hash line comment is skipped

- GIVEN the input `# comment; still comment\nSELECT 2;`
- WHEN the splitter runs
- THEN one statement `SELECT 2` is emitted
- AND the `;` inside the `#` comment is not a boundary

#### Scenario: Block comment spanning lines is skipped

- GIVEN the input `/* multi\n line ; comment */ SELECT 3;`
- WHEN the splitter runs
- THEN one statement `SELECT 3` is emitted
- AND the `;` inside the block comment is not a boundary
- AND the emitted statement's line number reflects the line where `SELECT 3` begins

### Requirement: DELIMITER Directive Support

The splitter MUST recognize the client-side `DELIMITER` directive (as emitted by mysqldump and stored-procedure dumps) when it appears at top level as the first token on a line. It MUST switch the active statement terminator to the token that follows (e.g. `$$`), such that subsequent `;` characters are treated as ordinary content until a `DELIMITER ;` directive restores the default. The `DELIMITER` line itself MUST NOT be emitted as an executable statement. If a `DELIMITER` directive is present but its new terminator token cannot be parsed (empty, malformed, or ambiguous), the splitter MUST fail loud with a clear, typed error and MUST NOT silently mis-split — partial or corrupt execution is forbidden.

#### Scenario: DELIMITER switches the terminator for a stored procedure

- GIVEN the input `DELIMITER $$\nCREATE PROCEDURE p() BEGIN SELECT 1; SELECT 2; END$$\nDELIMITER ;`
- WHEN the splitter runs
- THEN exactly one executable statement (the full `CREATE PROCEDURE ... END`) is emitted
- AND the inner `;` characters are preserved as procedure body, not boundaries
- AND neither `DELIMITER` line is emitted as a statement

#### Scenario: Default terminator restored after DELIMITER ;

- GIVEN the input `DELIMITER $$\nCREATE PROCEDURE p() BEGIN END$$\nDELIMITER ;\nINSERT INTO t VALUES (1);`
- WHEN the splitter runs
- THEN two executable statements are emitted: the procedure, then the `INSERT`
- AND the `INSERT` is split on the restored default `;`

#### Scenario: Nested/repeated DELIMITER blocks are handled sequentially

- GIVEN a script with two consecutive `DELIMITER $$ ... $$ DELIMITER ;` blocks each defining a procedure
- WHEN the splitter runs
- THEN each procedure is emitted as exactly one statement
- AND the active terminator is correctly restored to `;` between and after the blocks

#### Scenario: Unparseable DELIMITER fails loud

- GIVEN a `DELIMITER` directive with no terminator token following it on the line
- WHEN the splitter reaches that directive
- THEN the splitter raises a typed "unsupported/invalid DELIMITER" error
- AND no statement from that point is emitted or executed
- AND no silent mis-split occurs

### Requirement: Line Number Reporting

Every emitted statement MUST carry the 1-based line number of its first non-whitespace, non-comment character, computed from newline counting across the streamed input (including newlines inside skipped comments and multi-line strings). Line numbers MUST remain accurate for statements that begin after multi-line comments, multi-line string literals, and `DELIMITER` blocks. These line numbers are the source of truth for the risk report and the "stopped at statement N (line L)" failure message.

#### Scenario: Line number after a multi-line block comment

- GIVEN a script whose first three lines are a `/* ... */` block comment and line 4 is `SELECT 1;`
- WHEN the splitter emits the statement
- THEN its reported line number is 4

#### Scenario: Line number after a multi-line string literal

- GIVEN a statement on lines 1–2 containing a newline inside a `'...'` literal, followed by `UPDATE t SET x=1;` starting on line 3
- WHEN the splitter emits the second statement
- THEN its reported line number is 3

### Requirement: Top-Level WHERE Detection

For each emitted statement, the splitter MUST expose whether a top-level `WHERE` keyword is present — i.e. a `WHERE` token that is not inside a string, backtick identifier, comment, or a parenthesized subquery. This flag exists solely to drive the UPDATE classification rule (`UPDATE` without top-level `WHERE` → confirm). Case MUST be ignored. A `WHERE` appearing only inside a subquery or a string MUST NOT count as top-level.

#### Scenario: UPDATE with top-level WHERE is flagged present

- GIVEN the statement `UPDATE users SET active = 1 WHERE id = 5`
- WHEN the splitter reports top-level WHERE presence
- THEN it reports `true`

#### Scenario: UPDATE without any WHERE is flagged absent

- GIVEN the statement `UPDATE users SET active = 1`
- WHEN the splitter reports top-level WHERE presence
- THEN it reports `false`

#### Scenario: WHERE only inside a subquery is not top-level

- GIVEN the statement `UPDATE users SET flag = 1 WHERE id IN (SELECT id FROM staging WHERE dirty = 1)`
- WHEN the splitter reports top-level WHERE presence
- THEN it reports `true` for the outer WHERE
- AND a statement `UPDATE users SET flag = (SELECT v FROM s WHERE s.k = users.k)` with no outer WHERE reports `false`

#### Scenario: WHERE inside a string is not top-level

- GIVEN the statement `UPDATE t SET note = 'apply WHERE ready'`
- WHEN the splitter reports top-level WHERE presence
- THEN it reports `false`

### Requirement: Edge Case Handling

The splitter MUST handle degenerate inputs deterministically: an empty file yields zero statements; a file containing only comments/whitespace yields zero statements; a final statement with no trailing terminator is still emitted; a single statement with no terminator at all (one giant statement) is emitted once at end-of-input.

#### Scenario: Empty file yields no statements

- GIVEN an empty `.sql` file
- WHEN the splitter runs
- THEN zero statements are emitted
- AND no error is raised

#### Scenario: Comments-only file yields no statements

- GIVEN a file containing only `-- header\n/* notes */\n#trailing`
- WHEN the splitter runs
- THEN zero executable statements are emitted
- AND no error is raised

#### Scenario: Final statement without trailing terminator is emitted

- GIVEN the input `SELECT 1;\nSELECT 2` (no trailing `;`)
- WHEN the splitter reaches end-of-input
- THEN two statements are emitted, the second being `SELECT 2`

#### Scenario: Single giant statement with no terminator

- GIVEN a very large single `INSERT ... VALUES (...),(...),...` with no `;` anywhere
- WHEN the splitter reaches end-of-input
- THEN exactly one statement is emitted containing the full content

## Non-Functional

- **Performance**: The splitter MUST complete a single streaming pass over a ~600,000-line script without unbounded memory growth; per-statement work MUST be O(statement length). Line-number tracking MUST be constant per character.
- **Security**: Script content MUST NOT be logged or persisted to disk by the splitter.
- **Testability**: The splitter MUST be a pure JVM component unit-testable without Compose, Android `Context`, or Robolectric, driven by an in-memory `Reader`. Tests MUST cover escapes, doubled quotes, `--`/`#`/`/* */` comments, backticks, `;`-in-string, `DELIMITER` (including nested and unparseable), top-level-WHERE (including subquery/string cases), buffer-boundary tokens, and all edge-case inputs above.
