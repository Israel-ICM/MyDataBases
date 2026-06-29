# Spec: editor-advanced-features-v1.2

> Delta spec for the SQL editor advanced features v1.2. IDs are stable and referenced by `tasks.md`, test names, and PR descriptions. RFC 2119 keywords (MUST, SHALL, SHOULD, MAY) define requirement strength.

## Capabilities Addressed

- `sql-editor-bracket-matching` (new) — `BR-*` requirements
- `sql-editor-find-replace` (new) — `FR-*` requirements
- `sql-editor-multi-cursor` (modified) — `MC-*` requirements

---

## 1. Bracket Matching (BR-)

### ADDED Requirements

#### Requirement: BR-1 — Highlight matching bracket pair

The system MUST visually highlight the matching bracket when the cursor is adjacent (immediately before or after) to any of `(`, `)`, `[`, `]`, `{`, `}` tokens, using `SpanStyle(background = outlineVariant)` over BOTH offsets.

##### Scenario: BR-1.1 cursor after open paren

- GIVEN buffer `SELECT (a + b)` and cursor at offset 8 (immediately after `(`)
- WHEN the editor renders
- THEN offsets 7 and 12 are highlighted with `outlineVariant` background

##### Scenario: BR-1.2 cursor before close brace

- GIVEN buffer `CREATE TABLE x { id INT }` and cursor at offset 24 (before `}`)
- WHEN the editor renders
- THEN offsets 15 and 24 are highlighted

##### Scenario: BR-1.3 no adjacent bracket

- GIVEN buffer `SELECT * FROM t` and cursor at offset 7
- WHEN the editor renders
- THEN no bracket highlight is applied

#### Requirement: BR-2 — Jump to matching bracket

The system MUST move the cursor to the matching bracket offset when the user presses the `JumpToMatchingBracket` shortcut (`Ctrl+Shift+\`) while the cursor is adjacent to a paired bracket.

##### Scenario: BR-2.1 jump from open to close

- GIVEN buffer `SELECT (a + b) FROM t` and cursor at offset 8 (after `(`)
- WHEN user presses `Ctrl+Shift+\`
- THEN cursor selection becomes `TextRange(13)` (at matching `)`)

##### Scenario: BR-2.2 jump from close to open

- GIVEN buffer `SELECT (a + b)` and cursor at offset 13 (before `)`)
- WHEN user presses `Ctrl+Shift+\`
- THEN cursor selection becomes `TextRange(7)`

#### Requirement: BR-3 — Auto-close paired characters

The system MUST insert the matching closer and position the cursor between when the user types `(`, `[`, `{`, `'`, `"`, or `` ` `` as a single-character insertion in a non-string, non-comment context. Auto-close is hardcoded ON for v1.2.

##### Scenario: BR-3.1 type open paren

- GIVEN empty buffer, cursor at offset 0
- WHEN user types `(`
- THEN buffer becomes `()` and cursor is at offset 1

##### Scenario: BR-3.2 type single quote in identifier context

- GIVEN buffer `SELECT ` with cursor at offset 7
- WHEN user types `'`
- THEN buffer becomes `SELECT ''` and cursor is at offset 8

#### Requirement: BR-4 — Auto-close covers all six pairs

The system MUST support auto-close for `()`, `[]`, `{}`, `''`, `""`, and `` ` ` ``.

##### Scenario: BR-4.1 every pair

- GIVEN empty buffer
- WHEN user types `(`, then moves to end and types `[`, then `{`, then `'`, then `"`, then `` ` ``
- THEN each insertion produces the matching closer and parks the cursor between

#### Requirement: BR-5 — Suppress auto-close inside strings and comments

The system MUST NOT auto-close when the insertion point is inside a `STRING` or `COMMENT` token as classified by `SqlTokenizer`. The system MUST NOT auto-close on multi-character insertions (paste).

##### Scenario: BR-5.1 inside string literal

- GIVEN buffer `SELECT 'abc'` and cursor at offset 10 (between `c` and `'`, inside STRING)
- WHEN user types `(`
- THEN buffer becomes `SELECT 'abc('` (no auto-close), cursor at offset 11

##### Scenario: BR-5.2 inside line comment

- GIVEN buffer `-- hello ` and cursor at offset 9 (inside COMMENT)
- WHEN user types `(`
- THEN buffer becomes `-- hello (`, cursor at offset 10

##### Scenario: BR-5.3 paste suppresses auto-close

- GIVEN empty buffer
- WHEN clipboard `(foo)` is pasted (multi-char insertion)
- THEN buffer becomes `(foo)` exactly (no extra closer)

#### Requirement: BR-6 — Highlight updates on cursor move

The system MUST recompute the bracket-pair highlight on every cursor movement; the highlight MUST disappear when the cursor leaves bracket adjacency.

##### Scenario: BR-6.1 highlight follows cursor

- GIVEN buffer `(a)(b)` with cursor at offset 1 (highlight at 0/2)
- WHEN cursor moves to offset 4
- THEN highlight shifts to offsets 3/5; offsets 0/2 are no longer highlighted

#### Requirement: BR-7 — Jump fails gracefully

The system MUST NOT crash or move the cursor when `JumpToMatchingBracket` is invoked and no matching bracket exists (unbalanced source, cursor not adjacent to a bracket).

##### Scenario: BR-7.1 unbalanced source

- GIVEN buffer `SELECT (a + b` (no closing paren) and cursor at offset 8
- WHEN user presses `Ctrl+Shift+\`
- THEN cursor stays at offset 8; no exception is thrown

##### Scenario: BR-7.2 cursor not adjacent to a bracket

- GIVEN buffer `SELECT x` and cursor at offset 4
- WHEN user presses `Ctrl+Shift+\`
- THEN cursor stays at offset 4; no-op

---

## 2. Find & Replace (FR-)

### ADDED Requirements

#### Requirement: FR-1 — Open inline find bar

The system MUST open an inline `FindReplaceBar` above the editor when the user presses `Ctrl+F`, and MUST focus the find input.

##### Scenario: FR-1.1 open and focus

- GIVEN the editor has focus, no find bar visible
- WHEN user presses `Ctrl+F`
- THEN `FindReplaceBar` is visible above the editor AND the find input holds keyboard focus

#### Requirement: FR-2 — Match counter

The find bar MUST display the current match index and total match count formatted as `"{index} / {total}"`. When there are zero matches, the counter MUST show `"0 / 0"`.

##### Scenario: FR-2.1 multiple matches

- GIVEN buffer contains `SELECT` five times and find bar is open
- WHEN user types `SELECT` and navigates to the 2nd match
- THEN the counter shows `2 / 5`

##### Scenario: FR-2.2 no matches

- GIVEN buffer `SELECT * FROM t` and find bar is open
- WHEN user types `xyz`
- THEN the counter shows `0 / 0`

#### Requirement: FR-3 — Live highlighting with 150 ms debounce

The system MUST highlight all matches in the editor as the user types in the find input, with a 150 ms debounce to coalesce keystrokes.

##### Scenario: FR-3.1 debounced update

- GIVEN find bar open with query `SELECT`
- WHEN user replaces the query with `WHERE`
- THEN within 150 ms after the last keystroke, all `WHERE` matches are highlighted and the `SELECT` highlights are cleared

#### Requirement: FR-4 — Navigate matches with Enter / Shift+Enter

The system MUST advance to the next match on `Enter` and to the previous match on `Shift+Enter`. Navigation MUST wrap from last to first and from first to last.

##### Scenario: FR-4.1 next

- GIVEN find bar open with 5 matches, current index 2
- WHEN user presses `Enter`
- THEN current index becomes 3

##### Scenario: FR-4.2 wrap previous from first

- GIVEN find bar open with 5 matches, current index 1
- WHEN user presses `Shift+Enter`
- THEN current index becomes 5

#### Requirement: FR-5 — Esc closes and clears

The system MUST close the find bar and clear all match highlights when the user presses `Esc`, and MUST restore focus to the editor.

##### Scenario: FR-5.1 close clears highlights

- GIVEN find bar open with 3 highlighted matches
- WHEN user presses `Esc`
- THEN the bar is hidden, all match highlights are removed, and the editor regains focus

#### Requirement: FR-6 — Match case toggle (default OFF)

The find bar MUST expose a `match case` toggle defaulting to OFF. When OFF, matching is case-insensitive; when ON, matching is case-sensitive.

##### Scenario: FR-6.1 case-insensitive default

- GIVEN buffer `Select select SELECT` and find bar open with query `select`
- WHEN match case is OFF
- THEN counter shows `3 / 3`

##### Scenario: FR-6.2 case-sensitive

- GIVEN same buffer and query
- WHEN user toggles match case ON
- THEN counter shows `1 / 1`

#### Requirement: FR-7 — Whole word toggle (default OFF)

The find bar MUST expose a `whole word` toggle defaulting to OFF. When ON, the engine MUST wrap the query in `\b…\b` word boundaries.

##### Scenario: FR-7.1 whole word filters partial

- GIVEN buffer `id, user_id, id_card` and query `id`
- WHEN whole word is ON
- THEN counter shows `1 / 1` (only the standalone `id`)

#### Requirement: FR-8 — Regex toggle with validation

The find bar MUST expose a `regex` toggle defaulting to OFF. When ON, the query MUST be compiled as a `Regex`; on invalid pattern, the system MUST display an inline error message and MUST NOT crash.

##### Scenario: FR-8.1 valid regex

- GIVEN buffer `a1 b2 c3`, regex ON, query `[a-c]\d`
- WHEN engine recomputes
- THEN counter shows `3 / 3`

##### Scenario: FR-8.2 invalid regex shows error

- GIVEN find bar with regex ON
- WHEN user types `[unclosed`
- THEN the input shows an inline error state with a localized "invalid regex" message AND no highlights are applied AND no exception escapes

#### Requirement: FR-9 — Ctrl+H switches to replace mode

The system MUST reveal the replace input row (and replace / replace-all buttons) when the user presses `Ctrl+H`. If the bar was closed, `Ctrl+H` MUST open it directly in replace mode.

##### Scenario: FR-9.1 toggle replace row

- GIVEN find bar open in find-only mode
- WHEN user presses `Ctrl+H`
- THEN the replace input row becomes visible

##### Scenario: FR-9.2 open directly into replace

- GIVEN bar closed
- WHEN user presses `Ctrl+H`
- THEN the bar opens with both find and replace rows visible; find input is focused

#### Requirement: FR-10 — Replace one advances to next

The system MUST replace the current match with the replacement text and MUST advance the current index to the next match (or wrap to the first) on the `replace` button.

##### Scenario: FR-10.1 replace and advance

- GIVEN buffer `SELECT a, SELECT b`, query `SELECT`, replacement `PICK`, current index 1
- WHEN user clicks `replace`
- THEN buffer becomes `PICK a, SELECT b` AND current index advances to the remaining match

#### Requirement: FR-11 — Replace all is a single undo entry

The system MUST replace every match in one operation when the user clicks `replace all`, AND MUST push exactly ONE snapshot to `EditorHistory` so that `Ctrl+Z` reverts the entire operation atomically.

##### Scenario: FR-11.1 single undo

- GIVEN buffer with 5 `foo` occurrences, query `foo`, replacement `bar`
- WHEN user clicks `replace all`
- THEN buffer contains 5 `bar` occurrences AND `Ctrl+Z` restores the original buffer in one step

#### Requirement: FR-12 — Replace respects all toggles

Replace operations MUST honor the active `match case`, `whole word`, and `regex` toggles when matching the targets.

##### Scenario: FR-12.1 regex replace

- GIVEN buffer `id1 id2 id3`, regex ON, query `id(\d)`, replacement `ID$1`
- WHEN user clicks `replace all`
- THEN buffer becomes `ID1 ID2 ID3`

#### Requirement: FR-13 — Compact width responsive layout

The find bar MUST adapt to `WindowSizeClass.Compact` (< 600 dp) by collapsing the regex / case / whole-word toggles into an overflow `IconButton`. Inputs and navigation buttons MUST remain visible.

##### Scenario: FR-13.1 compact collapses toggles

- GIVEN screen width 360 dp, find bar open
- WHEN the bar lays out
- THEN regex / case / whole-word toggles are hidden behind an overflow button; find input and prev/next/close stay visible

#### Requirement: FR-14 — Distinct match highlight color

Match highlights MUST use a background color distinct from the syntax highlighting palette. The current match MUST use `tertiaryContainer`; other matches MUST use a muted variant.

##### Scenario: FR-14.1 current vs other

- GIVEN 3 matches highlighted, current index 2
- WHEN editor renders
- THEN match 2 has `tertiaryContainer` background; matches 1 and 3 have a muted background; neither color collides with `KEYWORD` or `STRING` token colors

#### Requirement: FR-15 — Scroll to current match

When the current match changes (via `Enter`, `Shift+Enter`, `replace`, or initial search), the editor MUST scroll vertically so that the current match is visible within the viewport.

##### Scenario: FR-15.1 scroll into view

- GIVEN buffer with 200 lines, current match on line 150, viewport showing lines 1–40
- WHEN current index changes to that match
- THEN editor scrolls so line 150 is in view

---

## 3. Multi-Cursor Improvements (MC-)

### MODIFIED Requirements

#### Requirement: MC-1 — Ctrl+Alt+Down adds cursor below

The system MUST add a new cursor on the line immediately below the primary cursor at the same target column. If the line below is shorter than the target column, the cursor MUST clamp to that line's EOL. At the last line, the action MUST be a no-op.
(Previously: only `Alt+Click` could add cursors.)

##### Scenario: MC-1.1 column preserved

- GIVEN buffer `SELECT a\nFROM t` with primary cursor at line 0, column 4
- WHEN user presses `Ctrl+Alt+Down`
- THEN selections become `[TextRange(line0col4), TextRange(line1col4)]`

##### Scenario: MC-1.2 clamp to EOL

- GIVEN buffer `SELECT a\nFR` with primary cursor at line 0, column 8
- WHEN user presses `Ctrl+Alt+Down`
- THEN the new cursor is at line 1 EOL (column 2)

##### Scenario: MC-1.3 no-op at last line

- GIVEN primary cursor on last line
- WHEN user presses `Ctrl+Alt+Down`
- THEN selections are unchanged

#### Requirement: MC-2 — Ctrl+Alt+Up adds cursor above

The system MUST mirror MC-1 upward: add a cursor at the same target column on the line above, clamping to EOL when shorter; no-op on the first line.

##### Scenario: MC-2.1 column preserved upward

- GIVEN primary cursor at line 5, column 10
- WHEN user presses `Ctrl+Alt+Up`
- THEN a new cursor exists at line 4, column 10 (or line 4 EOL if shorter)

##### Scenario: MC-2.2 no-op at first line

- GIVEN primary cursor on line 0
- WHEN user presses `Ctrl+Alt+Up`
- THEN selections are unchanged

#### Requirement: MC-3 — Ctrl+D with selection adds next occurrence

When the primary selection has non-zero length, `Ctrl+D` MUST append the next forward occurrence of that exact substring to the selections list, honoring case sensitivity by default.

##### Scenario: MC-3.1 add next occurrence

- GIVEN buffer `user_id, name, user_id, user_id` with `user_id` selected at offsets 0–7
- WHEN user presses `Ctrl+D`
- THEN selections becomes `[TextRange(0,7), TextRange(15,22)]`

#### Requirement: MC-4 — Ctrl+D with no selection selects word first

When the primary selection is a caret (zero length), `Ctrl+D` MUST first expand it to the word under the cursor using `SqlTokenizer` `IDENTIFIER` boundaries; the next `Ctrl+D` press MUST add the next occurrence.

##### Scenario: MC-4.1 first press selects word

- GIVEN buffer `SELECT user_id FROM t` with caret at offset 10 (inside `user_id`)
- WHEN user presses `Ctrl+D`
- THEN selections becomes `[TextRange(7, 14)]` (the full word)

##### Scenario: MC-4.2 second press adds next

- GIVEN state from MC-4.1, buffer has another `user_id` at offsets 30–37
- WHEN user presses `Ctrl+D` again
- THEN selections becomes `[TextRange(7,14), TextRange(30,37)]`

#### Requirement: MC-5 — Ctrl+D stops at last occurrence (no wrap)

The system MUST NOT wrap from the last occurrence back to the first. When no further occurrence exists, `Ctrl+D` MUST be a no-op on selections and MUST emit a transient snackbar message "No more occurrences" (localized).

##### Scenario: MC-5.1 stop at last

- GIVEN buffer with two `foo` matches, both already in selections
- WHEN user presses `Ctrl+D`
- THEN selections unchanged AND the snackbar shows "No more occurrences"

#### Requirement: MC-6 — Alt+Click preserved

The system MUST preserve the pre-existing `Alt+Click` multi-cursor behavior: clicking with `Alt` held adds a caret at the click position to selections.

##### Scenario: MC-6.1 alt-click still works

- GIVEN primary cursor at offset 4
- WHEN user `Alt+Click` at offset 20
- THEN selections becomes `[TextRange(4), TextRange(20)]`

#### Requirement: MC-7 — All cursors visible

The editor MUST render every selection in `selections` as a blinking caret (zero-length) or highlighted range (non-zero length). All cursors MUST blink in sync.

##### Scenario: MC-7.1 multiple carets render

- GIVEN selections `[TextRange(4), TextRange(20), TextRange(35)]`
- WHEN editor renders
- THEN three blinking carets are visible at offsets 4, 20, 35; blink phases are synchronized

#### Requirement: MC-8 — Undo/Redo restores cursor state atomically

The system MUST record selections alongside text in `EditorHistory` snapshots; `Ctrl+Z` and `Ctrl+Shift+Z` MUST restore both text AND selections from the snapshot.

##### Scenario: MC-8.1 undo restores cursors

- GIVEN state with 3 cursors that just performed an edit
- WHEN user presses `Ctrl+Z`
- THEN buffer reverts to pre-edit text AND selections revert to the 3-cursor state captured at edit time

##### Scenario: MC-8.2 redo restores cursors

- GIVEN state from MC-8.1 immediately after undo
- WHEN user presses `Ctrl+Shift+Z`
- THEN buffer and selections both return to the post-edit state

---

## 4. Non-Functional Requirements

### Requirement: NFR-1 — Find performance on large buffers

`FindReplaceEngine.findAll` MUST complete within **500 ms** on a 5000-line SQL buffer (~150 KB) when invoked on the `Default` dispatcher.

##### Scenario: NFR-1.1 5000-line benchmark

- GIVEN a synthetic buffer of 5000 lines, query `SELECT`
- WHEN `findAll` runs on `Dispatchers.Default`
- THEN it returns within 500 ms (measured by JVM benchmark test, 95th percentile of 20 runs)

### Requirement: NFR-2 — Accessibility

The find bar MUST be reachable via TalkBack focus order, the match counter MUST announce changes via `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`, and `Esc` MUST close the bar regardless of which sub-control has focus.

##### Scenario: NFR-2.1 counter announces

- GIVEN TalkBack enabled, find bar open
- WHEN user navigates from 2/5 to 3/5
- THEN TalkBack announces the new counter value

### Requirement: NFR-3 — Internationalization

All new user-facing strings MUST be defined in `res/values/strings.xml` and translated for all 10 supported locales: `en` (default), `es`, `pt`, `fr`, `de`, `it`, `ja`, `ko`, `zh-rCN`, `ru`. No hardcoded strings in Composables.

##### Scenario: NFR-3.1 localization coverage

- GIVEN the project resource directories
- WHEN CI runs the missing-translation lint check
- THEN every new key has a translation in all 10 `values-*` directories

### Requirement: NFR-4 — Compatibility

The implementation MUST compile and run on Android API 29+ (SDK 29 = Android 10) and MUST use only Compose APIs available in BOM `2024.02.00`. No new external dependencies.

##### Scenario: NFR-4.1 min SDK build

- GIVEN `minSdk = 29` in `app/build.gradle.kts`
- WHEN CI runs `:app:assembleRelease`
- THEN build succeeds without API-level lint errors

---

## 5. Out of Scope (Deferred to v1.3)

The following items are EXPLICITLY out of scope for v1.2 and MUST NOT be implemented in tasks under this change:

- **Code folding** — token-driven analyzer, gutter chevrons, folded-region persistence.
- **Minimap (tablet only)** — `WindowSizeClass.Expanded` thumbnail navigation.
- **SQL refactoring** — rename identifier, extract CTE, any AST-based transformation.
- **Ctrl+D wrap-around** — v1.2 stops at last occurrence; wrap is deferred.
- **Auto-close settings toggle** — auto-close is hardcoded ON for v1.2; a user-visible setting to disable it is deferred.
- **Fold-aware navigation** — auto-unfold on cursor entry.

---

## 6. Success Criteria

v1.2 is complete when ALL of the following are true:

- All requirements above (BR-1..7, FR-1..15, MC-1..8, NFR-1..4) have at least one passing test in either `app/src/test/` (pure domain JVM) or `app/src/androidTest/` (Compose UI).
- Overall coverage on new `domain/` engines (`BracketMatcher`, `FindReplaceEngine`, `MultiCursorEngine`) is ≥ **80%** per `.atl/standards/testing.md`.
- Bracket-pair highlights are visible in `SqlCodeEditorBracketHighlightTest` snapshot/composition assertions.
- `FindReplaceBarTest` passes on Compact (360 dp), Medium (600 dp), and Expanded (840 dp) widths.
- Manual smoke test: `Ctrl+F`, `Ctrl+H`, `Ctrl+Shift+\`, `Ctrl+Alt+Down`, `Ctrl+Alt+Up`, `Ctrl+D` all behave per spec on a physical Android 10+ device.
- Strict TDD honored: every implementation commit has a preceding (or same-commit) failing test.

## 7. Traceability

| Requirement | Implementing module | Test module |
|---|---|---|
| BR-1..2, BR-6..7 | `domain/BracketMatcher.kt`, `SqlHighlightTransformation.kt` | `BracketMatcherTest`, `SqlCodeEditorBracketHighlightTest` |
| BR-3..5 | `SqlCodeEditor.handleValueChange` | `SqlCodeEditorAutoCloseTest` |
| FR-1..5, FR-9, FR-13..15 | `components/FindReplaceBar.kt`, `QueryEditorViewModel` | `FindReplaceBarTest`, `QueryEditorScreenAdvancedTest` |
| FR-6..8, FR-10..12 | `domain/FindReplaceEngine.kt` | `FindReplaceEngineTest` |
| MC-1..5 | `domain/MultiCursorEngine.kt` | `MultiCursorEngineTest` |
| MC-6..7 | `SqlCodeEditor.kt`, `QueryEditorScreen.kt` | `QueryEditorScreenAdvancedTest` |
| MC-8 | `EditorHistory`, `QueryEditorViewModel` | existing `EditorHistoryTest` + new MC cases |
| NFR-1 | `FindReplaceEngine` | `FindReplaceEnginePerfTest` (JVM benchmark) |
| NFR-2 | `FindReplaceBar.kt` semantics | `FindReplaceBarAccessibilityTest` |
| NFR-3 | `res/values*/strings.xml` | CI missing-translation lint |
| NFR-4 | `app/build.gradle.kts` | CI `assembleRelease` |
