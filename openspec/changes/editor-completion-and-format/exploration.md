# Exploration: Editor Completion & Format (Level 2)

Scope: SQL code completion (auto + manual `Ctrl+Space`) with schema awareness, and on-demand SQL formatting (UPPERCASE keywords, toolbar button + `Ctrl+Shift+F`). Builds on Level 1 (shortcuts + undo/redo, already merged).

## Current State

Level 1 just landed and gives us a strong foundation we can extend without rewrites:

- **Editor anatomy** (`ui/screens/queryeditor/components/SqlCodeEditor.kt`, 396 LOC):
  - `BasicTextField` with hoisted `TextFieldValue`.
  - Already uses `Modifier.onPreviewKeyEvent` to intercept shortcuts BEFORE the IME — this is exactly the hook we need for `Ctrl+Space` and `Ctrl+Shift+F`.
  - Tokenizer runs on a `MutableStateFlow` with `debounce(300)` — same pattern can drive completion suggestions (with a shorter debounce).
  - Captures `TextLayoutResult` and already uses `getBoundingBox(pos)` to draw multi-cursor markers — we can reuse this to anchor a completion `Popup` at the cursor.
  - Multi-cursor logic lives in `handleValueChange`; format and completion-accept paths must respect it (or explicitly clear cursors).
- **Tokenizer** (`ui/screens/queryeditor/components/SqlTokenizer.kt`, pure object, JVM-testable): already classifies KEYWORD vs IDENTIFIER vs STRING vs COMMENT. The keyword regex is the canonical list (~75 keywords) — completion provider and formatter MUST consume the SAME source of truth to avoid drift.
- **ViewModel** (`QueryEditorViewModel.kt`):
  - Owns `EditorHistory` (push/undo/redo) with `canUndo`/`canRedo` StateFlows.
  - Only depends on `ExecuteBatchStatementsUseCase` today.
  - No schema access — completion will need new use cases or a dedicated `SchemaProvider`.
- **Shortcut pipeline** (`domain/editor/EditorShortcuts.kt`): pure mapper from `KeyEvent` → `ShortcutAction`. Adding `Format` and `TriggerCompletion` is a 4-line change. Already tested in JVM unit tests.
- **Toolbar** (`QueryEditorScreen.kt`, 691 LOC): pill `Surface` + `IconButton`s. Format button drops in next to Undo/Redo. Already shows enable/disable states based on `sqlText.text.isNotBlank()`.
- **Schema access today**:
  - `DatabaseRepository.getTables(database)` and `getColumns(table)` exist and are wrapped by `GetTablesUseCase` / `GetColumnsUseCase`.
  - But `QueryEditorScreen(connectionId, initialSql)` only knows the connectionId. It does NOT know the active database — that's deduced from `USE <db>` statements in the SQL itself OR from the workspace card context (`WorkspaceOverlay` passes `databaseName` to the card but not directly to the screen).
  - There is NO live snapshot of "current tables + columns for the open connection" cached anywhere. Each `getTables`/`getColumns` call hits the JDBC metadata reader.
- **i18n** (10 locales): we will add ~6–8 new UI strings (Format button label/tooltip, completion empty state, etc.). Spanish must ship alongside English; the other 8 locales can fall back per the Level 1 precedent (`TODO` markers or just default to en — already documented in `editor-shortcuts/spec.md` §"Localized Strings").
- **Testing convention**: pure logic → JVM unit tests (no Robolectric). UI → `androidTest/` Compose tests. Coverage threshold is 0 but strict TDD is enabled — every new module ships with red-first tests.

## Affected Areas

- `domain/editor/ShortcutAction.kt` — add `Format` and `TriggerCompletion` cases
- `domain/editor/EditorShortcuts.kt` — map `Ctrl+Shift+F` and `Ctrl+Space`
- `domain/editor/SqlCompletionProvider.kt` — NEW (pure): `(text, cursorOffset, schema) -> List<CompletionSuggestion>`
- `domain/editor/SqlFormatter.kt` — NEW (pure): `(sql) -> String`, reuses `SqlTokenizer`
- `domain/editor/SqlKeywords.kt` — NEW (or extract from tokenizer): single source of truth for the keyword list
- `domain/editor/CompletionSuggestion.kt` — NEW data class (kind = KEYWORD/TABLE/COLUMN, label, insertText)
- `domain/editor/SchemaSnapshot.kt` — NEW data class (tables, columnsByTable)
- `ui/screens/queryeditor/QueryEditorViewModel.kt` — add completion StateFlow, `format()` method, schema loader, debounced suggestion stream
- `ui/screens/queryeditor/components/SqlCodeEditor.kt` — anchor `Popup` at cursor, handle arrow/Enter/Esc navigation, plumb `onTriggerCompletion`
- `ui/screens/queryeditor/components/CompletionPopup.kt` — NEW Composable: `Popup` + `LazyColumn` of suggestions
- `ui/screens/queryeditor/QueryEditorScreen.kt` — Format toolbar button, wire `onShortcut` Format/TriggerCompletion
- `app/src/main/res/values*/strings.xml` — new strings × 10 locales (en + es full, rest fallback)
- `app/src/test/java/.../editor/` — `SqlCompletionProviderTest`, `SqlFormatterTest`, updated `EditorShortcutsTest`
- `app/src/androidTest/java/.../queryeditor/` — Compose UI tests for popup behavior and Format button

## Approaches per Feature

### 1. Code Completion (Auto + Ctrl+Space)

#### 1.1 Trigger strategy

| Option | Pros | Cons |
|---|---|---|
| **A. Auto on every identifier keystroke (debounced) + Ctrl+Space manual** | Matches modern IDEs; user choice 1C explicitly says "auto + manual" | Risk of intrusive popups if not gated well |
| **B. Manual only (`Ctrl+Space`)** | Zero-cost typing, no surprises | Contradicts user's 1C decision |
| **C. Auto only** | Simpler logic | No way to re-trigger after dismissal without re-typing |

**Recommendation: A**. Trigger auto-popup when ALL of these hold: (1) the token at the cursor is an IDENTIFIER of length ≥ 2, (2) the popup was not just dismissed by Esc for this token, (3) at least one suggestion matches the prefix. `Ctrl+Space` ignores conditions (1) and (2) — forces the popup even on length-0 prefix or after Esc. Debounce 150 ms (lighter than the 300 ms tokenizer debounce — completion must feel immediate).

#### 1.2 Suggestion sources

| Option | Pros | Cons |
|---|---|---|
| **A. Keywords + Tables + Columns from live schema** | Maximum signal; matches user expectation | Needs schema snapshot wiring (see 1.3) |
| **B. Keywords only** | Trivial to ship | User's explicit context list includes tables and columns — would underdeliver |
| **C. Keywords + tables (no columns)** | Cheaper schema load | Columns are exactly where completion has the highest payoff (after `SELECT`/`WHERE`) |

**Recommendation: A**. Implementation: `SqlCompletionProvider.suggest(text, offset, schema, limit = 8)` returns ranked suggestions:
1. Exact-prefix matches first (case-insensitive).
2. Within each kind, alphabetical.
3. Kind ordering biased by **lightweight context detection** (no parser):
   - After `FROM` / `JOIN` / `UPDATE` / `INTO` → TABLE first, then KEYWORD.
   - After `SELECT` / `WHERE` / `AND` / `OR` / `ON` / `,` (in projection) → COLUMN first, then KEYWORD.
   - Default → KEYWORD first.
4. Suppress completion entirely when the cursor sits INSIDE a string or comment (free — `SqlTokenizer` already classifies these ranges).

Pure function — no Compose, no coroutines. 100% JVM-testable with table-driven tests.

#### 1.3 Schema access

| Option | Pros | Cons |
|---|---|---|
| **A. Direct Room DAO** | Fastest | `Connection`/`ConnectionEntity` are stored in Room, but **schema (tables/columns) is NOT in Room** — it lives on the remote DB and is fetched via JDBC. Not applicable. |
| **B. Reuse `GetTablesUseCase` + `GetColumnsUseCase` via repository** | Already the canonical path; pattern already used by table browser screens | One JDBC round-trip per table for columns; needs caching |
| **C. New dedicated `LoadSchemaSnapshotUseCase`** | Single bulk operation; clean boundary | More code; can wrap B internally |

**Recommendation: C wrapping B**. Add `LoadSchemaSnapshotUseCase(database)` that:
- Calls `getTables(database)` once.
- Calls `getColumns(table)` lazily on first use of each table (NOT upfront — could be hundreds of tables).
- Returns a `SchemaSnapshot(tables: List<String>, columnsByTable: Map<String, List<String>>)` exposed as a `StateFlow` on the ViewModel so the popup re-renders when columns finally arrive.
- TTL/invalidation: refresh on explicit user action (toolbar refresh button, future) and on any successful `CREATE TABLE`/`ALTER TABLE`/`DROP TABLE` statement detected in `executeStatements` — track via `SqlTokenizer` keywords on the executed SQL (`CREATE`+`TABLE`, `ALTER`+`TABLE`, `DROP`+`TABLE`).
- **Open question** (carry to proposal): the screen doesn't currently know the active database. We have to either (a) plumb `databaseName` through `QueryEditorScreen(connectionId, databaseName, ...)` from `WorkspaceOverlay`, or (b) parse `USE <db>` from the editor text. Option (a) is cleaner and matches the Workspace card model.

#### 1.4 Completion UI

| Option | Pros | Cons |
|---|---|---|
| **A. `Popup` anchored to cursor bounding box** | Matches every IDE; works inside scrollable editor; doesn't steal focus | Need to compute screen position from `TextLayoutResult.getBoundingBox(pos)` (already done for multi-cursor) |
| **B. `DropdownMenu`** | Built-in Compose component | Designed for menus, not free positioning; anchors to a clickable parent, awkward for cursor following |
| **C. Inline ghost-text suggestion (single item)** | Minimal UI | User asked for a context list — single ghost text underdelivers; not testable for "navigate suggestions" |
| **D. Bottom sheet** | Touch friendly | Hides the editor area where the user is typing; bad for tablet keyboard users |

**Recommendation: A**. `Popup` with `LazyColumn` (cap 8 visible, scroll for the rest), positioned via `IntOffset` derived from the line bottom of the active cursor. Keyboard navigation: `↓`/`↑` move selection, `Enter` / `Tab` accept, `Esc` dismiss, any non-identifier char dismisses too. Touch: tap a row to accept. Reuses the same `onPreviewKeyEvent` pipeline already in place.

#### 1.5 Multi-cursor interaction

| Option | Pros | Cons |
|---|---|---|
| **A. Disable completion when multi-cursor is active** | Avoids ambiguity (insert in which cursor?) | Slight feature loss in an already niche mode |
| **B. Insert at every cursor** | Symmetric with current multi-cursor behavior | Tricky ranking: which cursor's prefix wins? |

**Recommendation: A** (with explicit user-visible behavior). When `cursorPositions.isNotEmpty()`, the popup does NOT auto-trigger and `Ctrl+Space` is a no-op. Matches the existing convention that multi-cursor is a power-user mode and the simpler primary path stays clean.

**Complexity: Medium**.

### 2. SQL Formatting (UPPERCASE keywords, on-demand)

#### 2.1 Implementation strategy

| Option | Pros | Cons |
|---|---|---|
| **A. Custom formatter over existing `SqlTokenizer`** | Zero deps, pure function, reuses canonical token kinds (strings/comments preserved for free), tiny APK delta, golden-file TDD | Edge cases (CTEs, window functions) need explicit rules |
| **B. External lib (`JSqlParser`, ports of `sql-formatter`)** | Off the shelf | JSqlParser adds ~1.5 MB and is a parser/serializer, not a formatter. JS ports require a runtime. Heavy for the value. |
| **C. Server-side API** | None | Project is offline-first; non-starter |

**Recommendation: A**. `SqlFormatter.format(sql: String): String` rules (locked to user choice 2A):
1. UPPERCASE all KEYWORD tokens (using `SqlTokenizer`'s classification — no risk of touching identifiers, strings, comments).
2. Newline before major clauses: `FROM`, `WHERE`, `JOIN` (incl. `INNER`/`LEFT`/`RIGHT`/`OUTER` prefix), `ORDER BY`, `GROUP BY`, `HAVING`, `LIMIT`, `UNION`.
3. 2-space indent for clause continuations and `ON` predicates.
4. Single space around binary operators; normalize commas (no space before, single space after).
5. Preserve STRING and COMMENT tokens VERBATIM (already a distinct kind — free correctness).
6. Trim trailing whitespace per line, collapse 3+ blank lines to 1.

Idempotency contract: `format(format(x)) == format(x)`. Enforced by test.

#### 2.2 Trigger and history integration

- Toolbar button (Format icon — `Icons.Default.FormatAlignLeft` or similar) in the left pill, placed after Redo.
- `Ctrl+Shift+F` shortcut via `EditorShortcuts.mapKeyEvent` → new `ShortcutAction.Format`.
- Before applying, push the CURRENT snapshot to `EditorHistory` so `Ctrl+Z` undoes the format atomically. Then call `onValueChange(formatted)`.
- If multi-cursor is active, clear `cursorPositions` (formatting invalidates them) — same as the existing pattern in `QueryEditorScreen.kt`.
- Disabled state when `sqlText.text.isBlank()` (matches the Save/Execute disabling convention).

#### 2.3 Performance

- Formatter is O(n) over tokens. 5k LOC SQL formats in single-digit ms in Default dispatcher.
- Run in `viewModelScope.launch(Dispatchers.Default)` to keep UI thread free; small enough to skip a progress indicator (no perceptible delay).

**Complexity: Small-Medium**.

## Cross-Cutting Concerns

### Performance

- **Completion** triggers on identifier keystrokes only (cheap precheck via the token at the cursor offset). Debounce 150 ms. Suggestion list capped at 8. Schema lookups (`columnsByTable`) lazy and cached. No frame drops on typical typing.
- **Format** is fire-and-forget on Default dispatcher; user perceives instant.
- **Schema load** is async on screen open; popup shows keywords first, refreshes when tables/columns arrive. No spinner on the popup — the list just grows.

### Schema sync / invalidation

- Listen to `executeStatements` results: if the executed batch contains a `CREATE TABLE`, `ALTER TABLE`, or `DROP TABLE` keyword (detect via `SqlTokenizer` on the executed text), trigger `LoadSchemaSnapshotUseCase` again.
- Cross-database `USE <db>` switching: also detect `USE` keyword and re-load with the new database.
- No file watcher needed — this is a remote DB.

### Multi-cursor

- Completion: disabled when `cursorPositions.isNotEmpty()` (see 1.5).
- Format: clears `cursorPositions` (invalidated by reflow).

### i18n

- SQL keywords stay in English regardless of locale (SQL is English, not user locale).
- UI strings (`format_button`, `format_button_description`, `completion_empty`, `completion_loading_schema`, `completion_keywords_only`, `completion_aria_label`) — full translations for en and es; other 8 locales fall back via Android string fallback, per the precedent already documented in `editor-shortcuts/spec.md`.

### Persistence

- No new Room tables. Schema snapshot is in-memory only (rebuilt on screen recreate).
- No new settings (UPPERCASE keyword case is locked per user choice 2A; if we add a toggle later, it goes in `SettingsRepository`).

## Recommended Implementation Order

1. **Shortcuts + actions extension** (XS) — add `Format`, `TriggerCompletion` to `ShortcutAction`; extend `EditorShortcuts` mapping; tests. ~50 lines, no UX yet.
2. **`SqlFormatter` pure module + Format toolbar button + `Ctrl+Shift+F`** (S-M) — golden-file tests, history integration, i18n strings. Lands the first user-visible feature. ~250–350 lines.
3. **`SchemaSnapshot` + `LoadSchemaSnapshotUseCase` + ViewModel wiring** (M) — async schema loading, invalidation on DDL/USE. Includes plumbing `databaseName` into `QueryEditorScreen` (resolves the open question). ~300 lines.
4. **`SqlCompletionProvider` pure module + tests** (M) — context detection, ranking, kind filtering, string/comment suppression. Pure JVM. ~250 lines.
5. **`CompletionPopup` Composable + cursor anchoring + key navigation + `Ctrl+Space`** (M-L) — UI, accept/dismiss, auto-trigger gating, multi-cursor disabling, androidTest coverage. ~400 lines.

Splits naturally into 2 chained PRs to stay under the 800-line review budget:
- **PR A**: steps 1 + 2 (Format end-to-end). Independently shippable.
- **PR B**: steps 3 + 4 + 5 (Completion end-to-end). Depends on A only for the shortcut action enum.

## Testing Strategy

**Pure JVM (fast, no Robolectric):**
- `SqlFormatterTest` — golden-file fixtures (`input.sql` → `expected.sql`), idempotency, string/comment preservation, UPPERCASE conversion, edge cases (empty, single keyword, mixed case, embedded `;`, line/block comments).
- `SqlCompletionProviderTest` — table-driven cases:
  - prefix filtering (case-insensitive)
  - ranking by kind context (after `FROM`/`SELECT`/`WHERE`/etc.)
  - suppression inside strings and comments
  - empty schema → keywords only
  - large schema (1000 columns) → capped at 8
  - Unicode identifiers passthrough
- `EditorShortcutsTest` — add cases for `Ctrl+Shift+F` → `Format`, `Ctrl+Space` → `TriggerCompletion`.
- `SqlKeywordsTest` — single source of truth doesn't drift from the tokenizer regex (assert keyword set equality).

**Compose UI (`androidTest/`):**
- Format toolbar button reformats and is undoable via toolbar Undo.
- `Ctrl+Shift+F` formats end-to-end.
- Typing `SEL` shows a popup with `SELECT` highlighted; `Enter` inserts; cursor lands after the inserted text.
- `Ctrl+Space` opens the popup with empty prefix.
- `Esc` dismisses; further typing of the same prefix does NOT re-open until prefix changes.
- Popup does not appear inside a string literal or comment.
- Popup does not appear when multi-cursor mode has cursors set.

## Risks & Challenges

1. **Active database is not in the screen signature**. `QueryEditorScreen(connectionId, initialSql)` is missing `databaseName`. Mitigation: extend the signature in step 3 and propagate from `WorkspaceOverlay` (which already holds `card.databaseName`). Backward compatibility is not a concern — single internal caller.
2. **Schema cost on huge DBs**. Loading columns for every table upfront could mean hundreds of JDBC round-trips. Mitigation: lazy per-table column loading on first completion request for that table; cache in `SchemaSnapshot`.
3. **Stale schema after external DDL**. The user could ALTER a table outside the app. Mitigation: refresh on any executed DDL inside the editor; explicit "refresh schema" affordance can land later if pain emerges.
4. **Popup focus stealing on physical keyboards**. `Popup` must NOT steal focus from `BasicTextField` — completion must be navigable with arrow keys while typing continues. Mitigation: drive navigation via `onPreviewKeyEvent` on the editor (consume `↓`/`↑`/`Enter`/`Esc` when popup visible), do NOT make popup focusable.
5. **Multi-cursor × format**. Format reflows the text; pinned cursor positions become meaningless. Mitigation: clear cursors on format (decision documented).
6. **Tokenizer keyword list drift**. Adding the formatter and completion provider creates THREE consumers of "the keyword list". Mitigation: extract `SqlKeywords` set as a single source of truth in step 2; tokenizer regex builds from it; formatter and completion read from it; a test asserts the set is non-empty and contains the canonical anchors (`SELECT`, `FROM`, `WHERE`, `JOIN`).
7. **Auto-popup feels intrusive**. Mitigation: 2-char prefix minimum, 150 ms debounce, Esc-remembers-token so a user can dismiss and keep typing without nagging.
8. **Soft keyboard `Ctrl+Space` and `Ctrl+Shift+F`**. Most Android IMEs don't send Ctrl combos. Mitigation: toolbar Format button is the canonical path; completion auto-triggers anyway (no shortcut required for the common path). Soft-keyboard parity is acceptable.
9. **i18n bandwidth**. 6–8 new strings × 10 locales. Mitigation: ship en + es full per the Level 1 precedent; the remaining 8 locales fall back automatically; queue a translation pass in a follow-up.
10. **SQL dialect coverage**. Tokenizer keyword list today is MySQL/MariaDB-flavored. Mitigation: in scope — the app is MySQL/MariaDB focused. Document explicitly that completion/format target this dialect.

## Open Questions for Proposal Phase

1. **Where does `databaseName` come from?** Plumb through `QueryEditorScreen` signature (recommended) OR derive from `USE <db>` in the editor text? Affects #1 risk and `LoadSchemaSnapshotUseCase` design.
2. **Should completion show types/comments on COLUMN suggestions?** `getColumns` returns rich `Column` metadata (type, nullable). Showing the type next to the column name (`id : INT`) is a small UX win but adds layout/accessibility surface. Default proposal: NO for v1, YES as a follow-up if requested.
3. **Format SELECT projection lists**: keep `SELECT a, b, c` on one line, or break to one column per line when projection is long? Default proposal: keep on one line up to ~80 chars, break beyond.
4. **Two PRs or one?** Recommended split: PR A (Format) + PR B (Completion). Stays under the 800-line review budget and lets Format ship and prove the keyword-extraction work before completion lands.

## Ready for Proposal

**Yes**, with the four questions above carried forward to the proposal phase. Recommendation to the orchestrator: ask the user the four questions in a single batched message; the answers shape `databaseName` plumbing (Q1), `CompletionSuggestion` shape (Q2), and the formatter rules (Q3). Q4 is an implementation/PR sequencing question — can be decided by us if the user has no preference.
