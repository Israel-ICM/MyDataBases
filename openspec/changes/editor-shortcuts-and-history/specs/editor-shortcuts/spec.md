# editor-shortcuts Specification

## Purpose

Keyboard shortcut dispatch and bounded undo/redo history for the SQL query editor. Pure domain primitives (`EditorHistory`, `EditorShortcuts`) consumed by `QueryEditorViewModel` and dispatched by `QueryEditorScreen`. Multi-cursor safe; locale-aware (en + es; auto-fallback for 8 other locales).

## Requirements

### Requirement: Run Shortcut (Ctrl+Enter)

The system MUST execute the current SQL query when `Ctrl+Enter` is pressed and the SQL text is not blank. Behavior MUST be identical to clicking the Run button.

#### Scenario: Ctrl+Enter executes query with non-blank text

- GIVEN the editor contains `SELECT * FROM users`
- WHEN the user presses `Ctrl+Enter`
- THEN the query executes via the same code path as the Run button

#### Scenario: Ctrl+Enter is a no-op when text is blank

- GIVEN the editor is empty or whitespace-only
- WHEN the user presses `Ctrl+Enter`
- THEN no query executes AND the Run button remains disabled

### Requirement: Save Shortcut (Ctrl+S)

The system MUST open the Save dialog when `Ctrl+S` is pressed and SQL text is not blank. Behavior MUST be identical to clicking the Save button.

#### Scenario: Ctrl+S opens save dialog

- GIVEN the editor contains non-blank SQL
- WHEN the user presses `Ctrl+S`
- THEN the Save dialog appears

#### Scenario: Ctrl+S is a no-op when text is blank

- GIVEN the editor is empty
- WHEN the user presses `Ctrl+S`
- THEN no dialog opens AND the Save button remains disabled

### Requirement: Undo Shortcut (Ctrl+Z)

The system MUST revert the editor to the previous snapshot (text, selection, cursor positions) when `Ctrl+Z` is pressed and the undo stack is non-empty.

#### Scenario: Ctrl+Z restores prior text and cursor

- GIVEN the user typed `SELECT * FROM users` then deleted `users`
- WHEN the user presses `Ctrl+Z`
- THEN `users` reappears AND the cursor returns to its position before the deletion

#### Scenario: Ctrl+Z on empty history is a no-op

- GIVEN the undo stack is empty
- WHEN the user presses `Ctrl+Z`
- THEN the editor state does not change AND the Undo button is disabled

### Requirement: Redo Shortcut (Ctrl+Y / Ctrl+Shift+Z)

The system MUST reapply the most recently undone snapshot when `Ctrl+Y` or `Ctrl+Shift+Z` is pressed and the redo stack is non-empty.

#### Scenario: Ctrl+Y reapplies an undone change

- GIVEN the user undid one change
- WHEN the user presses `Ctrl+Y`
- THEN the change is reapplied (text, selection, cursors restored)

#### Scenario: Ctrl+Shift+Z is equivalent to Ctrl+Y

- GIVEN the user undid one change
- WHEN the user presses `Ctrl+Shift+Z`
- THEN the change is reapplied identically to `Ctrl+Y`

#### Scenario: Ctrl+Y on empty redo stack is a no-op

- GIVEN the redo stack is empty
- WHEN the user presses `Ctrl+Y`
- THEN the editor state does not change AND the Redo button is disabled

### Requirement: Typing Coalescing

Consecutive single-character typing edits MUST collapse into a single snapshot so one `Ctrl+Z` reverts the burst. The system MUST flush coalescing on: newline insertion, cursor jump greater than 1 position, paste, editor blur, explicit `flush()`, or 500 ms of typing inactivity.

#### Scenario: Six keystrokes collapse to one undo

- GIVEN the editor is empty
- WHEN the user types `S`, `E`, `L`, `E`, `C`, `T` continuously
- AND presses `Ctrl+Z` once
- THEN all 6 characters are removed in a single step

#### Scenario: Newline flushes the coalesce buffer

- GIVEN the user typed `SELECT` then pressed `Enter` then typed `FROM`
- WHEN the user presses `Ctrl+Z`
- THEN only `FROM` is removed (the newline closed the prior snapshot)

#### Scenario: 500 ms idle flushes the coalesce buffer

- GIVEN the user typed `SEL`, paused 500 ms or more, then typed `ECT`
- WHEN the user presses `Ctrl+Z`
- THEN only `ECT` is removed

### Requirement: Multi-Cursor State Preservation

Each snapshot MUST atomically capture text, selection, and all cursor positions. Undo and redo MUST restore the full multi-cursor state.

#### Scenario: Undo restores all cursor positions

- GIVEN the user has 3 cursors at positions `[10, 25, 40]`
- WHEN the user types `X` (inserting at all 3 positions)
- AND presses `Ctrl+Z`
- THEN `X` is removed from all 3 positions AND cursors return to `[10, 25, 40]`

### Requirement: Bounded History (100 snapshots)

The history stack MUST hold at most 100 snapshots. When a 101st snapshot is pushed, the oldest snapshot MUST be discarded. Memory footprint SHOULD remain under ~200 KB (100 snapshots × ~2 KB average).

#### Scenario: 101st action drops the oldest snapshot

- GIVEN the user has performed 101 distinct snapshot-producing actions
- WHEN the user presses `Ctrl+Z` repeatedly
- THEN undo works for the last 100 actions AND the oldest is no longer reachable

### Requirement: Redo Stack Invalidation on New Edit

A new user edit after one or more undos MUST clear the redo stack.

#### Scenario: Typing after undo clears redo

- GIVEN the user undid 3 changes (redo stack holds 3 items)
- WHEN the user types a new character
- THEN the redo stack is empty AND the Redo button is disabled

### Requirement: Soft Keyboard Fallback

When no physical `Ctrl` key is available (e.g., soft keyboard), all four actions MUST remain accessible via toolbar buttons (Run, Save, Undo, Redo).

#### Scenario: Soft keyboard user undoes via toolbar

- GIVEN the device exposes only a soft keyboard
- WHEN the user taps the Undo toolbar button
- THEN the editor reverts to the previous snapshot identically to `Ctrl+Z`

### Requirement: Non-Functional — Performance and Memory

The system MUST NOT drop frames during normal typing. History memory MUST stay at or below ~200 KB under the 100-snapshot bound. Coalescing inactivity threshold MUST be 500 ms.

#### Scenario: Sustained typing does not drop frames

- GIVEN the user types continuously for 10 seconds
- WHEN snapshots are coalesced per the 500 ms rule
- THEN no dropped frames are observed in the typing path

### Requirement: Localized Strings (en + es)

User-facing strings `Undo` and `Redo` plus the 4 shortcut tooltips MUST be localized in `values/` (en) and `values-es/`. The 8 other supported locales (`ar, de, fr, hi, ja, pt-rBR, ru, zh-rCN`) MAY ship `TODO` markers and rely on Android's automatic fallback to en until translation lands.

#### Scenario: Spanish locale shows translated labels

- GIVEN the device locale is `es`
- WHEN the toolbar renders
- THEN Undo shows `Deshacer` AND Redo shows `Rehacer`

#### Scenario: Untranslated locale falls back to English

- GIVEN the device locale is `ja`
- WHEN the toolbar renders
- THEN Undo and Redo display their English labels (Android string fallback)

### Requirement: Pure Domain Isolation

`EditorHistory` and `EditorShortcuts` MUST be pure Kotlin classes with no Compose, Android framework, or Hilt dependencies, so they are unit-testable on the JVM without Robolectric.

#### Scenario: Domain classes compile and test on JVM

- GIVEN `EditorHistory` and `EditorShortcuts` source files
- WHEN `./gradlew test` runs
- THEN their tests execute on the JVM with no Android runtime required
