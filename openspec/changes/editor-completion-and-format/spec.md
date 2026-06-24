# Spec: Editor Completion & Format

Delta spec for change `editor-completion-and-format`. Introduces two new capabilities under the SQL editor surface: `sql-formatter` and `sql-completion`. No existing capabilities are modified or removed — these are the first formal specs for the editor surface.

## ADDED Capabilities

- `sql-formatter` — pure SQL formatter (UPPERCASE keywords, major-clause newlines, 2-space indent), toolbar + shortcut entry points, history-atomic apply, idempotency contract.
- `sql-completion` — context-aware suggestion engine (keywords + tables + columns), auto + manual trigger gating, popup UI anchored at cursor, multi-cursor exclusion, schema lifecycle (load + DDL/USE invalidation).

---

## Capability: sql-formatter

### Requirement: Pure formatter rules

The system MUST expose a pure function `SqlFormatter.format(sql: String): String` that, given any SQL string accepted by the existing `SqlTokenizer`, returns a normalized form satisfying ALL of:

- All KEYWORD tokens are emitted in UPPERCASE.
- A newline precedes each major clause keyword: `FROM`, `WHERE`, `JOIN` (including the prefixes `INNER`, `LEFT`, `RIGHT`, `OUTER`, `CROSS`, `FULL`), `GROUP BY`, `ORDER BY`, `HAVING`, `LIMIT`, `UNION`.
- Sub-clauses introduced by `ON`, `AND`, `OR` after a `JOIN` predicate SHOULD be indented by 2 spaces relative to their parent clause line.
- STRING tokens (single- and double-quoted) and COMMENT tokens (`--` line and `/* … */` block) MUST be preserved BYTE-FOR-BYTE.
- Projection lists (the token stream between `SELECT` and the next major clause) MUST keep the user's existing intra-list whitespace; the formatter MUST NOT insert or remove newlines between projection commas.
- Trailing whitespace on each line MUST be trimmed; runs of 3+ blank lines MUST collapse to a single blank line.
- The function MUST be idempotent: `format(format(x)) == format(x)` for all `x`.

This is the contract every consumer (toolbar button, shortcut, future API) relies on.

#### Scenario 1: Format simple SELECT with WHERE

- **GIVEN** input `select id, name from users where active = 1`
- **WHEN** `SqlFormatter.format(input)` is invoked
- **THEN** the result is exactly:
  ```sql
  SELECT id, name
  FROM users
  WHERE active = 1
  ```
- **Acceptance**:
  - [ ] Unit: `SqlFormatterTest::format_simpleSelectWithWhere_producesExpectedLayout()`

#### Scenario 2: Format multi-table JOIN

- **GIVEN** input `select u.id from users u inner join orders o on u.id = o.user_id where o.total > 100`
- **WHEN** `SqlFormatter.format(input)` is invoked
- **THEN** the result has `SELECT`, `FROM`, `INNER JOIN`, and `WHERE` each on its own line, with `ON u.id = o.user_id` indented 2 spaces under its `INNER JOIN`.
- **Acceptance**:
  - [ ] Unit: `SqlFormatterTest::format_innerJoinWithOnPredicate_indentsOnUnderJoin()`

#### Scenario 3: Format nested subquery (flat indent for v1)

- **GIVEN** input containing a nested subquery (e.g. `select id from users where id in (select user_id from orders where total > 100)`)
- **WHEN** `SqlFormatter.format(input)` is invoked
- **THEN** the OUTER query is formatted per the rules, AND the inner subquery contents inside parentheses are normalized for keyword case ONLY — no additional nested indentation is applied (v1 is flat).
- **Acceptance**:
  - [ ] Unit: `SqlFormatterTest::format_nestedSubquery_uppercasesKeywordsWithoutDeepIndent()`

#### Scenario 4: Preserve string literals (single and double quotes)

- **GIVEN** input `select 'select from where' as label, "JOIN" as kw from t`
- **WHEN** `SqlFormatter.format(input)` is invoked
- **THEN** both quoted literals MUST appear verbatim in the output, including any keyword-looking words inside them.
- **Acceptance**:
  - [ ] Unit: `SqlFormatterTest::format_stringLiterals_preservedVerbatim()`

#### Scenario 5: Preserve inline (`--`) comments

- **GIVEN** input containing `-- foo bar select` at end of line
- **WHEN** `SqlFormatter.format(input)` is invoked
- **THEN** the comment text and its leading `--` MUST be preserved unchanged on its original logical line.
- **Acceptance**:
  - [ ] Unit: `SqlFormatterTest::format_lineComment_preservedVerbatim()`

#### Scenario 6: Preserve block (`/* … */`) comments

- **GIVEN** input containing a multi-line `/* explanation FROM WHERE */` block
- **WHEN** `SqlFormatter.format(input)` is invoked
- **THEN** the block comment contents MUST appear in the output unchanged; keyword-looking words inside MUST NOT be uppercased.
- **Acceptance**:
  - [ ] Unit: `SqlFormatterTest::format_blockComment_preservedVerbatim()`

#### Scenario 7: Preserve user projection formatting (do not break at commas)

- **GIVEN** input `SELECT a, b, c FROM t`
- **WHEN** `SqlFormatter.format(input)` is invoked
- **THEN** the projection `a, b, c` MUST remain on a single line after `SELECT`. The formatter MUST NOT split into `SELECT a,\n  b,\n  c`.
- **Acceptance**:
  - [ ] Unit: `SqlFormatterTest::format_projectionList_keptOnOneLine()`

#### Scenario 8: Idempotent formatting

- **GIVEN** any input `x`
- **WHEN** `SqlFormatter.format(SqlFormatter.format(x))` is computed
- **THEN** the result MUST equal `SqlFormatter.format(x)`.
- **Acceptance**:
  - [ ] Unit (property-based / golden): `SqlFormatterTest::format_isIdempotent_acrossAllGoldenFixtures()`

### Requirement: Toolbar button entry point

The system MUST render a Format toolbar button in `QueryEditorScreen` placed in the left pill immediately after the Redo button. The button MUST:

- Show the localized `format_button_label` and have `contentDescription = format_button_description`.
- Be ENABLED iff `sqlText.text.isNotBlank()`.
- On click, invoke `QueryEditorViewModel.formatSql()`.

#### Scenario 9: Format via toolbar button click

- **GIVEN** the editor contains valid SQL and the Format button is enabled
- **WHEN** the user taps the Format button
- **THEN** the editor text is replaced with the formatted output AND the previous text is pushed onto `EditorHistory` as a single undo entry.
- **Acceptance**:
  - [ ] E2E: `QueryEditorScreenTest::tappingFormatButton_replacesTextAndPushesHistory()`

### Requirement: Keyboard shortcut entry point

The system MUST map `Ctrl+Shift+F` to `ShortcutAction.Format` via `EditorShortcuts.mapKeyEvent`. When dispatched, the action MUST invoke the same `formatSql()` pathway as the toolbar button.

#### Scenario 10: Format via Ctrl+Shift+F shortcut

- **GIVEN** the editor has focus on a physical-keyboard device and contains valid SQL
- **WHEN** the user presses `Ctrl+Shift+F`
- **THEN** the same behavior as the toolbar button click MUST occur (text replaced + history pushed).
- **Acceptance**:
  - [ ] Unit: `EditorShortcutsTest::mapKeyEvent_ctrlShiftF_returnsFormat()`
  - [ ] E2E: `QueryEditorScreenTest::ctrlShiftF_triggersFormat()`

### Requirement: History-atomic apply

The system MUST push the pre-format snapshot to `EditorHistory` BEFORE applying the formatted text. After a Format operation, a single `Ctrl+Z` (or Undo button) MUST restore the original pre-format text exactly.

#### Scenario 11: Format pushes to EditorHistory (undo restores original)

- **GIVEN** the editor contains `select 1` and the user formats it to `SELECT 1`
- **WHEN** the user presses Undo (or `Ctrl+Z`)
- **THEN** the editor text MUST equal `select 1` (the original, byte-for-byte).
- **Acceptance**:
  - [ ] E2E: `QueryEditorScreenTest::formatThenUndo_restoresOriginal()`

### Requirement: Multi-cursor handling on format

The system MUST clear `cursorPositions` when Format is applied, because the reflow invalidates pinned offsets.

#### Scenario 12: Format with multi-cursor active applies to full text and clears cursors

- **GIVEN** the editor has 2+ pinned multi-cursor positions
- **WHEN** Format is triggered (button or shortcut)
- **THEN** the formatter runs over the full text (NOT per-cursor) AND `cursorPositions` is emptied after the apply.
- **Acceptance**:
  - [ ] E2E: `QueryEditorScreenTest::formatWithMultiCursor_clearsCursorsAndFormatsFullText()`

---

## Capability: sql-completion

### Requirement: Suggestion engine

The system MUST expose a pure function `SqlCompletionProvider.suggest(text: String, offset: Int, schema: SchemaSnapshot, limit: Int = 8): List<CompletionSuggestion>` that:

- Returns at most `limit` suggestions, defaulting to 8.
- Filters by case-insensitive prefix match against the identifier token at `offset` (or the empty prefix when none).
- Returns `CompletionSuggestion(kind, label, insertText, typeLabel?)` where `kind ∈ { KEYWORD, TABLE, COLUMN }`.
- Returns `emptyList()` when the cursor sits inside a STRING or COMMENT token range as classified by `SqlTokenizer`.
- Ranks results by (1) context-biased kind ordering, then (2) alphabetical within each kind.

### Requirement: Context-aware ranking

The system MUST detect the last meaningful KEYWORD before the cursor (skipping whitespace, strings, comments) and bias kind ordering:

- After `FROM`, `JOIN`, `UPDATE`, `INTO` → TABLE first, then KEYWORD, then COLUMN.
- After `SELECT`, `WHERE`, `AND`, `OR`, `ON`, or a comma inside a projection → COLUMN first, then KEYWORD, then TABLE.
- Default (start of statement, no preceding keyword) → KEYWORD first.

#### Scenario 13: Show keyword suggestions after 2+ chars

- **GIVEN** an empty editor and `databaseName == null`
- **WHEN** the user types `SEL`
- **THEN** the auto-trigger fires and the popup contains `SELECT` ranked first.
- **Acceptance**:
  - [ ] Unit: `SqlCompletionProviderTest::suggest_prefixSEL_returnsSELECTFirst()`
  - [ ] E2E: `CompletionPopupTest::typingSEL_showsSelectAtTop()`

#### Scenario 14: No auto-popup for 1 char

- **GIVEN** an empty editor
- **WHEN** the user types a single character `S`
- **THEN** the popup MUST NOT auto-appear (prefix length gating: minimum 2 chars).
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::typingSingleChar_doesNotShowPopup()`

#### Scenario 15: Debounce 150 ms (rapid typing does not spam popup)

- **GIVEN** an empty editor
- **WHEN** the user types `S`, `E`, `L`, `E`, `C` within 150 ms total
- **THEN** the suggestion flow MUST emit at most one popup-render event after the burst (debounced).
- **Acceptance**:
  - [ ] Unit: `QueryEditorViewModelTest::suggestionsFlow_debouncesRapidInput()`

#### Scenario 16: Manual trigger Ctrl+Space bypasses gating

- **GIVEN** an empty editor (zero-length prefix at cursor)
- **WHEN** the user presses `Ctrl+Space`
- **THEN** the popup MUST appear immediately, listing the top-N suggestions for the current context with an empty prefix (no 2-char minimum applied).
- **Acceptance**:
  - [ ] Unit: `EditorShortcutsTest::mapKeyEvent_ctrlSpace_returnsTriggerCompletion()`
  - [ ] E2E: `CompletionPopupTest::ctrlSpace_opensPopupWithEmptyPrefix()`

#### Scenario 17: Context ranking — after FROM, tables first

- **GIVEN** `databaseName != null`, schema contains tables `[users, orders]`, and editor text `SELECT * FROM ` with cursor at end
- **WHEN** `Ctrl+Space` is pressed
- **THEN** the first suggestion kind MUST be `TABLE` and `users`, `orders` MUST appear in alphabetical order before any KEYWORD.
- **Acceptance**:
  - [ ] Unit: `SqlCompletionProviderTest::suggest_afterFROM_ranksTablesFirst()`

#### Scenario 18: Context ranking — after SELECT, columns first

- **GIVEN** `databaseName != null`, schema with table `users(id, name)`, editor text `SELECT `
- **WHEN** `Ctrl+Space` is pressed
- **THEN** the first suggestion kind MUST be `COLUMN` (columns from all loaded tables) before keywords.
- **Acceptance**:
  - [ ] Unit: `SqlCompletionProviderTest::suggest_afterSELECT_ranksColumnsFirst()`

### Requirement: Schema lifecycle and degradation

The system MUST treat `databaseName: String?` plumbed from `WorkspaceOverlay` as the schema-availability switch:

- `databaseName != null` → `LoadSchemaSnapshotUseCase` is invoked on screen entry; the resulting `SchemaSnapshot` populates a `StateFlow` on the ViewModel; suggestions include keywords, tables, and columns.
- `databaseName == null` → no schema fetch is attempted; `schemaSnapshot` stays empty; suggestions include keywords ONLY.

The system MUST refresh the schema when the editor successfully executes a statement containing `CREATE TABLE`, `ALTER TABLE`, `DROP TABLE`, or `USE <db>` (detected via `SqlTokenizer` over the executed SQL).

#### Scenario 19: Schema available — show tables + columns + keywords

- **GIVEN** screen opened with `databaseName = "shop"`, schema loaded as `tables=[users, orders]`, `columnsByTable={users:[id,name]}`
- **WHEN** the user triggers completion with no prefix
- **THEN** the popup MUST contain at least one KEYWORD, one TABLE, and one COLUMN suggestion (within the 8-item cap).
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::withSchema_showsAllThreeKinds()`

#### Scenario 20: Schema unavailable — show keywords only

- **GIVEN** screen opened with `databaseName == null`
- **WHEN** the user triggers completion
- **THEN** every suggestion in the popup MUST have `kind == KEYWORD` AND `LoadSchemaSnapshotUseCase` MUST NOT have been invoked.
- **Acceptance**:
  - [ ] Unit: `QueryEditorViewModelTest::nullDatabaseName_skipsSchemaLoad()`
  - [ ] E2E: `CompletionPopupTest::nullDatabase_showsKeywordsOnly()`

### Requirement: Column type display

When a suggestion has `kind == COLUMN`, the system MUST render the row label as `name : TYPE` (e.g. `id : INT`). `typeLabel` MUST be populated from `Column.type` returned by `GetColumnsUseCase`.

#### Scenario 21: Column suggestions show type

- **GIVEN** schema has column `users.id` of type `INT`
- **WHEN** the user triggers completion in a context where `id` appears
- **THEN** the rendered row text MUST contain `id : INT`.
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::columnSuggestion_displaysTypeLabel()`

### Requirement: Popup keyboard and pointer interactions

When the completion popup is visible, the system MUST route the following keys via `onPreviewKeyEvent` on the editor (popup is non-focusable):

- `Down` / `Up`: move selection by one, WRAPPING at the ends.
- `Enter`: accept the currently selected suggestion (insert `insertText` replacing the active prefix; place cursor at end of insert).
- `Tab`: identical to `Enter` (accept).
- `Esc`: dismiss the popup AND remember the current identifier token so auto-trigger does NOT re-open for the same token until the prefix changes.
- Any non-identifier character keystroke: dismiss the popup.

Pointer: tapping a row MUST accept that suggestion.

#### Scenario 22: Arrow key navigation cycles and wraps

- **GIVEN** the popup is visible with 3 suggestions and item 0 selected
- **WHEN** the user presses `Up`
- **THEN** the selected item MUST become index 2 (wrap to last).
- **AND WHEN** the user then presses `Down` twice
- **THEN** the selected item MUST be index 1 (wrap forward through 0 → 1).
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::arrowNavigation_wrapsAtBothEnds()`

#### Scenario 23: Enter accepts selected suggestion

- **GIVEN** the popup shows `SELECT` as the selected item and the editor contains the prefix `SEL` at the cursor
- **WHEN** the user presses `Enter`
- **THEN** the editor text MUST replace `SEL` with `SELECT`, the cursor MUST land immediately after the inserted text, AND the popup MUST dismiss.
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::enterKey_acceptsAndDismisses()`

#### Scenario 24: Tab accepts selected suggestion

- **GIVEN** same setup as Scenario 23
- **WHEN** the user presses `Tab`
- **THEN** the behavior MUST be identical to pressing `Enter` (accept + dismiss).
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::tabKey_acceptsAndDismisses()`

#### Scenario 25: Esc dismisses popup and remembers token

- **GIVEN** the popup is visible for prefix `SEL`
- **WHEN** the user presses `Esc`
- **THEN** the popup MUST dismiss AND no auto-trigger MUST fire while the user continues editing the same `SEL` token (e.g. backspacing to `SE` and re-typing `L`). A new auto-trigger MAY fire only when the token changes (e.g. user types a non-identifier char then a new identifier).
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::escDismiss_remembersTokenUntilChange()`

### Requirement: Multi-cursor exclusion

The system MUST disable completion entirely while `cursorPositions.isNotEmpty()`:

- The auto-trigger MUST NOT fire.
- `Ctrl+Space` MUST be a no-op (no popup).
- If a popup was already visible when a multi-cursor was added, the popup MUST dismiss.

#### Scenario 26: Completion disabled when multi-cursor active

- **GIVEN** the editor has 2 pinned multi-cursor positions
- **WHEN** the user types `SEL` or presses `Ctrl+Space`
- **THEN** the popup MUST NOT appear AND no suggestions MUST be computed.
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::multiCursorActive_disablesCompletion()`

### Requirement: Popup anchoring and capacity

The system MUST anchor the popup at the cursor's bounding box (`TextLayoutResult.getBoundingBox(cursorPos).bottomLeft`) so that the popup tracks the cursor as the editor scrolls. The popup MUST render up to 8 suggestions visible at once; when more than 8 results exist, the `LazyColumn` MUST be scrollable to access the rest.

#### Scenario 27: Popup anchored to cursor bounding box (follows scroll)

- **GIVEN** the popup is visible
- **WHEN** the user scrolls the editor
- **THEN** the popup MUST reposition with the cursor's new on-screen bounding box.
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::popup_repositionsWithCursorOnScroll()`

#### Scenario 28: Max 8 suggestions visible (scrollable for more)

- **GIVEN** the provider returns 12 suggestions for the current prefix
- **WHEN** the popup renders
- **THEN** at most 8 rows MUST be visible without scrolling AND the `LazyColumn` MUST be scrollable to reach the remaining 4.
- **Acceptance**:
  - [ ] Unit: `SqlCompletionProviderTest::suggest_respectsLimit8()`
  - [ ] E2E: `CompletionPopupTest::popup_capsVisibleRowsAndScrollsForRest()`

---

## i18n Requirements

### Requirement: Localized strings

All user-facing strings introduced by this change MUST be defined in `res/values/strings.xml` and translated for `values-es/`. Other 8 supported locales fall back to the default (per the precedent set by the `editor-shortcuts` change). The minimum string set:

- `format_button_label`
- `format_button_description`
- `completion_empty`
- `completion_loading_schema`
- `completion_keywords_only`
- `completion_aria_label`

#### Scenario 29: Format button label localized × en + es (full); rest fallback

- **GIVEN** the device locale is `en` or `es`
- **WHEN** the Format button renders
- **THEN** the button label and content description MUST come from the matching locale resources (NOT hardcoded strings, NOT the default fallback when a full translation exists).
- **AND** when the device locale is any of the other 8 supported locales, the strings MUST fall back to `values/strings.xml` without crashing.
- **Acceptance**:
  - [ ] Unit: `StringsResourceTest::format_button_label_existsInEnAndEs()`
  - [ ] E2E: `QueryEditorScreenTest::formatButton_usesLocalizedLabel(locale=es)`

#### Scenario 30: Completion popup empty state localized

- **GIVEN** the provider returns zero suggestions but the popup is forced open (e.g. `Ctrl+Space` with no matches)
- **WHEN** the popup renders
- **THEN** it MUST display the localized `completion_empty` string in the active locale.
- **Acceptance**:
  - [ ] E2E: `CompletionPopupTest::emptyResults_showsLocalizedEmptyState()`

---

## Non-Functional Requirements

### Performance

- `SqlCompletionProvider.suggest(...)` MUST return in under 50 ms for schemas with up to 1 000 columns total (measured on a mid-range physical device).
- Auto-trigger debounce MUST be exactly 150 ms.
- `SqlFormatter.format(...)` MUST run off the main thread (`Dispatchers.Default`) and complete in under 100 ms for inputs up to 5 000 lines.
- Schema column loading MUST be lazy per-table (NOT upfront on screen open).

### Accessibility

- The popup MUST be dismissible with `Esc` from a physical keyboard.
- The popup MUST be navigable via arrow keys without focus shifting from the editor.
- The popup container MUST expose `contentDescription = completion_aria_label` for screen readers.
- The Format button MUST have a non-empty `contentDescription` from `format_button_description`.
- Tap targets in the popup MUST be at least 48dp tall.

### i18n

- All UI strings introduced MUST live in `res/values/strings.xml`; English and Spanish MUST be fully translated; the remaining 8 supported locales rely on Android's fallback to the default resource.
- SQL keywords (in suggestions and formatter output) MUST remain English regardless of device locale — SQL is a language, not a UI string.

### Error handling

- When `databaseName != null` but `LoadSchemaSnapshotUseCase` fails (connection error, JDBC exception), the system MUST log the error and degrade silently to keywords-only completion. The popup MUST NOT show an error chrome to the user.
- When the cursor sits inside an unterminated string or comment (tokenizer reports incomplete token), suggestions MUST be suppressed (return `emptyList()`).
- The formatter MUST never throw on malformed input — it MUST return the original string unchanged if tokenization fails.

---

## Out of Scope (explicit non-requirements)

The following are explicitly NOT required by this spec and MUST NOT be implemented in this change:

- Smart indentation for nested subqueries (the formatter is flat in v1).
- Alias detection (e.g. `SELECT u.id FROM users u` resolving `u.` to `users` columns).
- JOIN-path / foreign-key-aware table suggestions.
- Custom snippet templates or user-defined completions.
- Format configuration (case style, indent width, projection break threshold) — locked to defaults.
- Settings UI for completion toggles.

These remain open for follow-up changes and MUST NOT influence the test surface of this change.
