# Exploration: editor-advanced-features-v1.2

> Six advanced editor features promised by the product roadmap (`.atl/product/features/sql-editor.md`, lines 448–456): Find & Replace, Multi-cursor improvements, Code folding, Bracket matching, Minimap (tablet), and SQL refactoring (rename table / extract query).

---

## 1. Current State

The SQL editor lives in `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/` and is built on top of `BasicTextField` + a custom `VisualTransformation`. What is already in place:

- **`SqlCodeEditor.kt`** — single `BasicTextField` with line numbers, monospace text, scroll state, debounced re-tokenization (300 ms), Tab insertion, `onPreviewKeyEvent` shortcut interception, `onTextLayout` capturing `TextLayoutResult`, custom multi-cursor rendering via `Canvas` overlay (blinking cursors driven by `infiniteTransition`).
- **`SqlTokenizer.kt`** — pure regex tokenizer (KEYWORD, STRING, COMMENT, NUMBER, IDENTIFIER, OPERATOR, PUNCTUATION, WHITESPACE) producing `List<SqlToken>` with `IntRange` ranges. Drives both highlighting and (after this change) the new features.
- **`SqlHighlightTransformation.kt`** — applies span styles per token kind into an `AnnotatedString`.
- **`domain/EditorShortcuts.kt`** — pure mapper `KeyEvent → ShortcutAction` (Run, Save, Undo, Redo, Format, TriggerCompletion). New shortcuts will be added here.
- **`domain/EditorHistory.kt`** — bounded undo/redo with coalescing (max 100 snapshots).
- **Multi-cursor today** — `cursorPositions: MutableList<Int>` hoisted into `QueryEditorScreen`. Activation is **Ctrl + tap** or a toggle, then taps add cursors. Insertions/deletions are mirrored to all cursors inside `handleValueChange`. There is no `Ctrl+D` (select next occurrence) and no `Ctrl+Alt+Down/Up` (vertical add cursor).
- **Completion popup** — `CompletionPopup.kt` consumes Arrow/Enter/Escape via `onPreviewKeyEvent` with highest priority. New popups (Find bar, refactor dialogs) must integrate with the same priority chain.
- **`QueryEditorScreen.kt`** (991 lines) — owns `sqlText: TextFieldValue`, `cursorPositions`, `scrollState`, dispatches shortcuts, hosts toolbar.
- **`QueryEditorViewModel.kt`** — owns `EditorHistory`, exposes `canUndo`/`canRedo`. State machine in `QueryEditorUiState`.

What is **missing** that this change has to add:
- No search/find infrastructure (no incremental matcher, no match-range highlighting on top of tokenizer).
- No bracket/paren matcher (`SqlTokenizer` knows `PUNCTUATION` but doesn't pair them).
- No folding domain model. SQL has no curly braces — folding regions must be derived from statement boundaries (`;`) and parenthesized subqueries.
- No minimap component.
- No refactoring engine. Identifiers are already classified by the tokenizer, but there is no rename-aware traversal that respects quoted identifiers (backticks), aliases, or string literals.

---

## 2. Affected Areas

| Feature | Files created | Files modified |
|---|---|---|
| Find & Replace | `domain/FindReplaceEngine.kt`, `domain/FindMatch.kt`, `components/FindReplaceBar.kt` | `SqlCodeEditor.kt` (match overlay), `QueryEditorScreen.kt` (host bar + state), `domain/ShortcutAction.kt` (+ `Find`, `Replace`), `EditorShortcuts.kt` (Ctrl+F, Ctrl+H) |
| Multi-cursor improvements | `domain/MultiCursorEngine.kt` (pure: addAbove, addBelow, selectNextOccurrence, dedupe/sort) | `SqlCodeEditor.kt` (route new shortcuts; selection state per cursor), `QueryEditorScreen.kt`, `ShortcutAction.kt` (+ `AddCursorAbove`, `AddCursorBelow`, `SelectNextOccurrence`), `EditorShortcuts.kt` |
| Code folding | `domain/FoldingRegion.kt`, `domain/SqlFoldingAnalyzer.kt` (pure; consumes `List<SqlToken>`), `domain/FoldingState.kt` (set of collapsed region ids + persistence), `components/FoldingGutter.kt` | `SqlCodeEditor.kt` (gutter column + collapsed-line rendering via `VisualTransformation` chain), `QueryEditorViewModel.kt` (persist state per tab) |
| Bracket matching | `domain/BracketMatcher.kt` (pure; uses tokenizer output, stack-based pairing) | `SqlCodeEditor.kt` (overlay box for matched pair, auto-close `(` `'` `"` `` ` ``), `ShortcutAction.kt` (+ `JumpToMatchingBracket`), `EditorShortcuts.kt` (Ctrl+Shift+`\`) |
| Minimap (tablet) | `components/EditorMinimap.kt` | `QueryEditorScreen.kt` (conditional render via `WindowSizeClass`), `SqlCodeEditor.kt` (expose scroll + token data) |
| SQL refactoring | `domain/refactor/RenameTableRefactor.kt`, `domain/refactor/ExtractQueryRefactor.kt`, `domain/refactor/RefactorEngine.kt`, `components/RenameDialog.kt`, `components/ExtractQueryDialog.kt` | `QueryEditorScreen.kt` (menu entries + dialog hosts), `ShortcutAction.kt` (+ `RenameSymbol` F2, `ExtractQuery`), `EditorShortcuts.kt` |

Shared cross-cutting touches:
- `SqlTokenizer` becomes the **single source of truth** for: bracket pairing, folding region detection, refactor identifier scanning. No regex-on-raw-text outside tokens (avoids breaking strings/comments).
- `strings.xml` (all 10 locales per `.atl/standards/localization.md`) — every new label/button/menu must be localized.
- Tests:
  - `app/src/test/java/.../queryeditor/domain/` — pure JVM tests for engines (Find, MultiCursor, Folding, Bracket, Refactor).
  - `app/src/androidTest/java/.../queryeditor/` — Compose UI tests for bar, gutter, minimap, dialogs.

---

## 3. Approaches per Feature

### 3.1 Find & Replace

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| **A. Inline bar above editor** (VSCode/IntelliJ style, slides in from the top of the editor area) | Keeps editor + matches visible; mobile-friendly with compact layout; persistent until dismissed; matches VS Code muscle memory | Eats vertical space (~64 dp expanded with replace); on Compact width needs careful layout (2 rows) | Medium |
| B. Full modal `AlertDialog` | Simple to build; no layout interference | Hides the text being searched — terrible UX; not standard | Low |
| C. Bottom sheet | Familiar Material 3 on Android; non-blocking | Distance from cursor; hard to keep current match visible | Medium |

**Recommended: A (inline bar)** with two compact rows: row 1 = find input + match counter (`3/12`) + prev/next/close, row 2 (only when Replace mode `Ctrl+H`) = replace input + replace / replace-all. On Compact width (`< 600 dp`) the option toggles (regex, case, whole word) collapse behind an overflow `IconButton`.

**Match highlighting strategy**: extend `SqlHighlightTransformation` to accept a `List<IntRange>` of match ranges and apply a `SpanStyle(background = MaterialTheme.colorScheme.tertiaryContainer)` on top of token styles. The current match gets a stronger background. This composes cleanly with existing syntax highlighting — no second `BasicTextField`.

**Matching engine** lives in `FindReplaceEngine` (pure):
```
data class FindOptions(val regex: Boolean, val caseSensitive: Boolean, val wholeWord: Boolean)
fun findAll(text: String, query: String, options: FindOptions): List<IntRange>
fun replaceOne(text: String, range: IntRange, replacement: String, options: FindOptions): String
fun replaceAll(text: String, query: String, replacement: String, options: FindOptions): String
```
All four operations stream off the same compiled `Regex` (escaped for non-regex mode, `\b…\b` injected for whole-word).

### 3.2 Multi-cursor improvements

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| **A. Pure domain `MultiCursorEngine` driven by `TextLayoutResult`** | Same pattern already used (`getLineForOffset` / `getBoundingBox`); JVM-testable for column math; integrates with existing `cursorPositions` list | Needs careful column preservation when moving across shorter lines | Medium |
| B. Bake all logic into `SqlCodeEditor` | Fewer files | Already a 430-line file; adds untestable logic to Compose | — |

**Recommended: A**. New shortcuts:

- `Ctrl+Alt+Down` → `MultiCursorEngine.addCursorBelow(layout, positions, currentColumn)` — preserves the **target column** of the primary cursor (VS Code behavior) and clamps to line length.
- `Ctrl+Alt+Up` → symmetric `addCursorAbove`.
- `Ctrl+D` → `selectNextOccurrence(text, currentSelection)`. First press selects the word under the cursor (using `SqlTokenizer` IDENTIFIER boundaries). Subsequent presses add the next occurrence as an additional selection. Wrap on EOF.
- `Alt+Click` → already wired conceptually; finalize so `Alt` (not Ctrl) is the canonical add-cursor modifier and Ctrl stays free for shortcuts. Both should coexist for a transition period.

**Storage**: today only `MutableList<Int>` of cursor offsets exists. `Ctrl+D` introduces **selections**, not just cursors → upgrade to `MutableList<TextRange>` with the primary represented by `TextFieldValue.selection`. The existing single-cursor list is migrated by treating each `Int` as `TextRange(i, i)`.

### 3.3 Code folding

SQL has no braces. Foldable regions must be inferred. Options:

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| **A. Token-driven `SqlFoldingAnalyzer`** detecting: (1) parenthesized blocks spanning ≥ 2 lines (subqueries, `CREATE TABLE (...)`, `IN (...)` lists), (2) statement boundaries `;` grouping multi-line statements, (3) block comments `/* ... */`, (4) `BEGIN … END` blocks | Reuses existing tokenizer; deterministic; testable; produces real foldable units that map to user intent | Needs a small stack walker; multi-statement folding can be opinionated | Medium |
| B. Indentation-based folding (Python-style) | Trivial to compute | SQL isn't indentation-sensitive; produces meaningless regions for un-formatted SQL | — |
| C. User-marked regions only (`-- region` / `-- endregion`) | Predictable | Requires the user to mark; product spec wants automatic folding | — |

**Recommended: A**. Region detection runs on the debounced token stream (same 300 ms cadence) so it never blocks typing.

**Rendering**: visual collapse uses a second `VisualTransformation` chained after the highlight one — collapsed ranges are replaced with `… (n lines)` plus an `OffsetMapping` that translates cursor positions across hidden text. Folding gutter lives in the line-number column with a `▾ / ▸` chevron per foldable line.

**UI affordances**: chevron in the gutter (tap), `Ctrl+Shift+[` / `Ctrl+Shift+]` for fold/unfold at cursor, `Ctrl+K Ctrl+0` / `Ctrl+K Ctrl+J` for fold-all / unfold-all (VS Code parity, optional).

**Persistence**: `FoldingState` is per-tab and persisted in DataStore alongside tab state (already persisted per the v1.1 multi-tab work). Keys are stable line-anchored hashes (region start line + region kind) so folds survive minor edits but invalidate on big rewrites.

### 3.4 Bracket matching

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| **A. Tokenizer-based pairing** — single-pass stack over `PUNCTUATION` tokens (`(`, `)`); paren depth ignores `STRING` and `COMMENT` tokens automatically | Correct by construction; reuses tokens; O(n) | Needs a tiny stack walker | Low |
| B. Manual char scan | No tokenizer dependency | Re-implements quote/comment skipping → bug magnet | — |

**Recommended: A**. The matcher returns `Map<Int, Int>` (offset → matching offset) computed on the debounced token stream.

- **Highlight**: when the cursor is adjacent to `(`, `)`, `[`, `]`, draw a Material 3 `outlineVariant` background on both offsets via the same highlight overlay used by Find. Animation: brief 150 ms color fade (matches the "animado" wording in the product spec line 454).
- **Jump**: `Ctrl+Shift+\` (VS Code), maps to `JumpToMatchingBracket` → reads the matcher map and sets `selection = TextRange(target)`.
- **Auto-close**: intercept `(`, `'`, `"`, `` ` `` in `handleValueChange`. Insert pair only when the next character is whitespace, end-of-line, EOF, or another closer. Selection is preserved between the pair. Backspace at the empty pair removes both chars. Auto-close MUST NOT fire inside `STRING` or `COMMENT` tokens.

### 3.5 Minimap (tablet only)

The product spec is explicit: **tablet only** (`.atl/product/features/sql-editor.md`, line 453).

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| **A. Block visualization** — `Canvas` drawing 1-px-per-char colored blocks driven by `SqlTokenizer` output; viewport rectangle overlays current scroll window; tap-to-scroll, drag-to-pan | Very fast; works for thousands of lines; readable at glance; reuses token colors | Not pixel-perfect text — just a visual silhouette | Medium |
| B. Scaled-down BasicTextField | Looks like a real preview | Compose `BasicTextField` rescaling is expensive; second tokenization pass; not worth the cost on Android | High |

**Recommended: A**. Conditional render gated by `currentWindowAdaptiveInfo().windowSizeClass` — show only when width ≥ `Medium` (per `.atl/standards/adaptive-layouts.md`). Sticks to the right edge of the editor, ~80 dp wide. Synced bidirectionally with `scrollState`: scrolling the editor moves the viewport box; tapping/dragging on the minimap calls `scrollState.scrollTo`.

Performance: redraw the Canvas only when (a) tokens change (debounced 300 ms — same as highlight), or (b) scroll offset changes. Use `derivedStateOf` to avoid recomposition on every keystroke.

### 3.6 SQL refactoring (rename table + extract query)

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| **A. Token-aware identifier rewrite** — walk `SqlTokenizer` output, find `IDENTIFIER` tokens matching the target name, skip `STRING`/`COMMENT`, honor backtick-quoted variants | Reuses tokenizer; correct around strings/comments; fast | Doesn't understand aliases or scope (rename will rename matching identifiers regardless of role — table vs column) | Medium |
| B. Full SQL AST parser (e.g., JSqlParser) | True semantic rename, alias-aware | Heavy dependency, slow on mobile, parser failures on partial/invalid SQL block UX | High |
| C. Plain regex find/replace | Trivial | Will rewrite inside strings/comments; unsafe | — |

**Recommended: A for v1.2** with a clearly scoped UX: "Rename identifier" (not "Rename table semantically"). The dialog tells the user "renaming N occurrences" and offers a preview list with line numbers and toggleable per-occurrence checkboxes. This gives users control where the tokenizer can't infer intent. The action becomes a single `EditorHistory` snapshot so `Ctrl+Z` reverts atomically.

**Extract query**: select a sub-expression (typically a subquery in parens or a SELECT block), invoke `Extract Query` → engine wraps the selection in `WITH extracted AS (…)` at the top of the statement and replaces the selection with `extracted`. Naming is requested via a small dialog. v1.2 scope = CTE-based extraction only; "extract as view" is out of scope.

**UX entry points**:
- Long-press on identifier → context menu "Rename" / "Extract".
- Keyboard: `F2` rename, `Ctrl+Alt+M` extract (VS Code/IntelliJ parity).

---

## 4. Recommended Implementation Order

Sequenced so each feature builds on the prior layer's tested pieces:

1. **Bracket matching** — smallest, purely additive, validates the "token-driven highlight overlay" pattern. Establishes the multi-overlay rendering approach reused by Find and Folding.
2. **Find & Replace (without regex)** — introduces the match-highlight overlay and the inline bar pattern; reuses the overlay approach from step 1.
3. **Find & Replace (regex + replace-all + options)** — small follow-up commit, separates risk.
4. **Multi-cursor improvements** — depends on tokenizer (`Ctrl+D` needs IDENTIFIER tokens) and on TextRange-based selection storage. Refactors the existing `MutableList<Int>` to `MutableList<TextRange>` once, then adds the three new shortcuts.
5. **Code folding** — depends on a stable token stream and on the visual-transformation chaining proven in steps 1–3. Persistence layer reuses the existing tab DataStore.
6. **Minimap (tablet)** — pure rendering work that consumes the now-stable token list and `scrollState`. No dependencies on the others except token output.
7. **SQL refactoring (rename + extract)** — most semantically risky; benefits from all prior infrastructure (token traversal, dialogs, history snapshot pattern). Ship rename first, extract second.

This order keeps each merge under the 800-line review budget (likely 3–4 chained PRs total).

---

## 5. Testing Strategy

Per `.atl/standards/testing.md` and the project's Strict TDD policy, every domain engine is JVM-testable; every Compose surface gets at least one UI test.

| Layer | Tooling | Coverage targets |
|---|---|---|
| **Pure domain (JUnit 4)** | `FindReplaceEngineTest`, `MultiCursorEngineTest`, `SqlFoldingAnalyzerTest`, `BracketMatcherTest`, `RenameTableRefactorTest`, `ExtractQueryRefactorTest` | All branches; edge cases listed below |
| **Tokenizer extensions** | `SqlTokenizerTest` (existing) — add cases for matched-bracket pairing inputs | Strings/comments must not contribute paren depth |
| **Compose UI** | `FindReplaceBarTest`, `SqlCodeEditorBracketHighlightTest`, `FoldingGutterTest`, `EditorMinimapTest`, `RenameDialogTest` | Rendering + key/tap interaction |
| **Integration (Compose)** | `QueryEditorScreenAdvancedTest` | End-to-end: open Find → next match → replace → undo restores |

**Edge cases to assert**:
- Find: empty query (no matches; no crash), regex that matches empty string (must not infinite-loop), match counter at 0/0, replace-all in 10 000-line document (perf budget 200 ms).
- Multi-cursor: `Ctrl+Alt+Down` at last line (no-op), `Ctrl+D` past last occurrence (wrap or stop?), column preservation across shorter lines.
- Folding: nested foldable regions, cursor inside collapsed region (move cursor or auto-unfold?), region invalidation after big paste.
- Bracket matching: cursor between two adjacent parens `()|()` → which pair?, mismatched parens (no highlight, no crash), auto-close inside string suppressed.
- Minimap: rendering at 1, 100, 10 000 lines; tap-to-scroll target accuracy; orientation change preserves state.
- Refactoring: rename collides with existing identifier (warn), extract query when selection isn't a valid sub-expression (disabled menu), undo restores in one step.

**How to test folding/minimap in Compose**:
- Folding: `ComposeTestRule` taps the gutter chevron, asserts `onNodeWithText("… (3 lines)")` appears, then asserts the underlying `TextFieldValue.text` is unchanged (collapse is visual-only).
- Minimap: render in a `setContent` with a known token list, use `onNodeWithTag("editor-minimap")`, assert `Canvas` draws by capturing semantics or by injecting a `MinimapRenderer` interface that can be unit-tested separately from the Compose wrapper.

---

## 6. Risks & Challenges

| # | Risk | Severity | Mitigation |
|---|---|---|---|
| R1 | **Minimap rendering perf** on long queries — redrawing thousands of token blocks each scroll tick can stutter | High | Render to a cached `ImageBitmap` keyed on `tokens.hashCode()`; only the viewport rectangle redraws on scroll |
| R2 | **Find bar layout on Compact width** — running out of horizontal space with input + counter + 4 buttons | Medium | Collapse options into overflow menu below 600 dp; replace counter format `3/12` with abbreviated `3/12` (no labels) |
| R3 | **Refactor correctness** — token-based rename will rewrite identifiers regardless of scope (column named `users` would be renamed when renaming table `users`) | High | Preview dialog with per-occurrence checkboxes; document the limitation as v1.2 trade-off; v1.3 can introduce alias-aware analysis |
| R4 | **VisualTransformation chaining** — chaining highlight + match overlay + folding requires careful `OffsetMapping` composition; off-by-one bugs corrupt cursor placement | High | Build a `CompositeVisualTransformation` helper with a single `OffsetMapping` resolver; cover with property-based tests |
| R5 | **Shortcut collisions** — Ctrl+D already exists in some Android IMEs as "duplicate", Ctrl+F may be intercepted | Medium | Use `onPreviewKeyEvent` (already the pattern); document in user help; allow remap in a future settings screen |
| R6 | **Multi-cursor selection storage migration** — moving from `MutableList<Int>` to `MutableList<TextRange>` touches existing tested code | Medium | Migrate in step 4 as a single isolated commit; keep existing tests green by treating `Int` as `TextRange(i, i)` adapter |
| R7 | **Folding persistence drift** — region anchors are line-based; large edits can resurrect stale folds in wrong places | Low | Tag each region with `(startLine, kind, contentHash)`; discard on hash mismatch |
| R8 | **Bracket auto-close + paste** — pasting `(foo)` should not auto-add a closing paren | Low | Auto-close only fires on single-char input events, not on `newText.length - oldText.length > 1` |
| R9 | **Review budget overflow** — six features in one PR easily exceeds 800 lines | Medium | Chain into 4 PRs per implementation order; raise to orchestrator (chained-pr strategy = `ask-always`) |

---

## 7. Open Questions for Orchestrator

Before kicking off the proposal, please confirm with the user:

1. **Feature subset / staging** — All six features in v1.2, or split into v1.2 (Find&Replace + Bracket + Multi-cursor) and v1.3 (Folding + Minimap + Refactor)? The proposal can scope either way. *(Recommendation: keep all six but expect 4 chained PRs.)*
2. **Minimap scope** — Tablet-only as the product spec states (`Compact = hidden`), or surface a settings toggle so phone users can opt in? *(Recommendation: tablet-only per spec; revisit after telemetry.)*
3. **Refactoring depth** — v1.2 ships:
   - (a) Rename-only (extract deferred to v1.3), or
   - (b) Rename + CTE-based extract (recommended), or
   - (c) Full alias-aware rename via SQL AST (much bigger scope; recommend deferring).
4. **Ctrl+D semantics on EOF** — VS Code wraps to start; IntelliJ stops. Which behavior? *(Recommendation: stop on EOF, with a snackbar "No more occurrences".)*
5. **Folding-aware navigation** — when the cursor lands inside a collapsed region (e.g., via search), should we auto-unfold or move the cursor to the region header? *(Recommendation: auto-unfold; matches VS Code.)*
6. **Auto-close brackets default** — on by default, or behind a settings flag? Some power users dislike auto-close. *(Recommendation: on by default; settings flag added in a later v1.x.)*
7. **Chained PR slicing** — confirm the 4-PR split: (PR1) Bracket matching, (PR2) Find&Replace, (PR3) Multi-cursor + Folding, (PR4) Minimap + Refactor. Or another split?

---

## 8. Ready for Proposal

**Yes** — once the orchestrator collects answers to the seven open questions, the proposal can be drafted with a confirmed scope. Default assumption if the user declines to answer: all six features in v1.2, minimap tablet-only, rename + CTE extract, Ctrl+D stops on EOF, folding auto-unfolds, auto-close on by default, 4 chained PRs.
