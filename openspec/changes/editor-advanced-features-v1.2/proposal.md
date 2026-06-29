# Proposal: editor-advanced-features-v1.2

## Intent

The product roadmap (`.atl/product/features/sql-editor.md`, lines 448–456) promises six advanced editor features as part of v1.2 to make the in-app SQL editor competitive with desktop IDEs (VS Code / DataGrip) on Android. v1.2 ships the **three highest-leverage / lowest-risk** features so users get immediate productivity gains while the more complex ones (folding, minimap, refactoring) are split into a follow-up v1.3 change.

v1.2 targets developers who write multi-statement SQL on phones and tablets and currently suffer from: (a) no way to find/replace inside a query, (b) primitive multi-cursor (Alt+Click only, no keyboard-driven cursor stacking), and (c) no bracket guidance for deeply nested `SELECT` / `CREATE TABLE` statements.

## Scope

### In Scope

- **Bracket matching** — highlight matching `(`/`)` pair when cursor is adjacent; `Ctrl+Shift+\` jump to match; auto-close `(`, `'`, `"`, `` ` `` (ON by default, suppressed inside `STRING`/`COMMENT` tokens, suppressed on paste).
- **Find & Replace** — inline bar above editor; `Ctrl+F` opens find, `Ctrl+H` toggles replace row; options: regex, match case, whole word; live match highlighting; match counter `n/m`; Enter / Shift+Enter cycles matches; replace one / replace all; single `EditorHistory` snapshot per replace-all.
- **Multi-cursor improvements** — `Ctrl+Alt+Down` / `Ctrl+Alt+Up` add cursor on consecutive line (column-preserving); `Ctrl+D` selects next occurrence of word under cursor (stops at last occurrence, no wrap); pre-existing `Alt+Click` multi-cursor preserved.
- Storage migration: `cursorPositions: MutableList<Int>` → `MutableList<TextRange>` to support selections.
- Full localization across all 10 supported locales for every new label.
- Test coverage at the project minimum (`.atl/standards/testing.md` → 80%).

### Out of Scope (deferred to v1.3)

- Code folding (token-driven analyzer, gutter chevrons, persistence).
- Minimap (tablet-only, `WindowSizeClass.Expanded`).
- SQL refactoring (rename identifier + extract CTE).
- Ctrl+D wrap-around behavior, fold-aware cursor navigation (auto-unfold on cursor entry), settings flag to disable auto-close.

## Capabilities

### New Capabilities

- `sql-editor-find-replace`: Inline find & replace UX, matching engine, match highlighting, replace operations, regex / case / whole-word options.
- `sql-editor-bracket-matching`: Tokenizer-based bracket pairing, highlight overlay, jump-to-match shortcut, auto-close behavior.

### Modified Capabilities

- `sql-editor-multi-cursor`: Existing multi-cursor capability gains `Ctrl+Alt+Down/Up` (add cursor on consecutive line) and `Ctrl+D` (select next occurrence, stop at last). Selection storage upgraded from `List<Int>` to `List<TextRange>`.

## Approach

### Bracket matching

- Extend `SqlTokenizer` consumer with a new pure `BracketMatcher.kt` (`domain/`): single-pass stack walk over `PUNCTUATION` tokens, skips `STRING`/`COMMENT`, returns `Map<Int, Int>` (offset → matching offset). Recomputed on the existing debounced (300 ms) token stream.
- Highlight via `SqlHighlightTransformation` extension: when cursor is adjacent to a bracket, layer `SpanStyle(background = outlineVariant)` on both offsets. 150 ms fade animation matches product spec wording ("animado").
- Jump: new `ShortcutAction.JumpToMatchingBracket` mapped to `Ctrl+Shift+\` in `EditorShortcuts.kt`; handler sets `selection = TextRange(target)`.
- Auto-close: intercept `(`, `'`, `"`, `` ` `` inside `SqlCodeEditor.handleValueChange` when the diff is a **single-char insertion** (suppresses paste); insert closer only if the next char is whitespace, EOF, EOL, or another closer; backspace at empty pair removes both; never fires inside `STRING`/`COMMENT`.

### Find & Replace

- New `FindReplaceBar.kt` (`components/`): Material 3 inline bar above the editor. Row 1 = find input + `n/m` counter + prev/next/close. Row 2 (when `Ctrl+H`) = replace input + replace/replace-all. On Compact width (`< 600 dp`) the regex/case/word toggles collapse behind an overflow `IconButton`.
- State lives in `QueryEditorViewModel`: `findQuery`, `replaceQuery`, `options: FindOptions`, `matches: List<TextRange>`, `currentIndex`. Emits on every keystroke via `debounce(150)` to avoid recomputation on every char.
- Pure engine `FindReplaceEngine.kt` (`domain/`): `findAll`, `replaceOne`, `replaceAll`, all driven by a single compiled `Regex` (escaped in literal mode, `\b…\b` injected for whole-word).
- Match highlight: composite `VisualTransformation` chains syntax highlight + match overlay; current match gets a stronger `tertiaryContainer` background, other matches get muted background.
- `replaceAll` pushes a **single snapshot** to `EditorHistory` so `Ctrl+Z` reverts atomically.
- Shortcuts via `EditorShortcuts.kt`: `Ctrl+F` → `Find`, `Ctrl+H` → `Replace`, `Esc` closes bar, `Enter` / `Shift+Enter` cycle matches. Bar uses `onPreviewKeyEvent` at priority just under completion popup.

### Multi-cursor improvements

- New pure `MultiCursorEngine.kt` (`domain/`): `addCursorBelow(layout, selections, targetColumn)`, `addCursorAbove(...)`, `selectNextOccurrence(text, selections, tokens)`. JVM-testable; consumes `TextLayoutResult` for column math.
- `Ctrl+Alt+Down/Up` — preserves the **target column** of the primary cursor (VS Code semantics), clamps to line length, no-op at first/last line.
- `Ctrl+D` — first press: select word under primary cursor using `SqlTokenizer` `IDENTIFIER` boundaries. Subsequent presses: append next forward occurrence to selections. **Stop at last occurrence** (no wrap); UI emits a snackbar "No more occurrences" via existing `SnackbarHostState`.
- Storage migration: `cursorPositions: MutableList<Int>` → `selections: MutableList<TextRange>` in `QueryEditorScreen`. Single isolated commit treats every existing `Int i` as `TextRange(i, i)` adapter to keep existing tests green.
- New `ShortcutAction` entries: `AddCursorAbove`, `AddCursorBelow`, `SelectNextOccurrence`. Mapped in `EditorShortcuts.kt`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/BracketMatcher.kt` | New | Pure stack-based bracket pairing. |
| `domain/FindReplaceEngine.kt` | New | Pure find/replace operations + `FindOptions`. |
| `domain/FindMatch.kt` | New | Match data class (range, line, column). |
| `domain/MultiCursorEngine.kt` | New | Pure add-cursor + select-next-occurrence. |
| `components/FindReplaceBar.kt` | New | Inline bar Composable. |
| `domain/ShortcutAction.kt` | Modified | + `Find`, `Replace`, `JumpToMatchingBracket`, `AddCursorAbove`, `AddCursorBelow`, `SelectNextOccurrence`. |
| `domain/EditorShortcuts.kt` | Modified | New key bindings. |
| `SqlCodeEditor.kt` | Modified | Composite `VisualTransformation` overlay, auto-close in `handleValueChange`, route new shortcuts, render multi-selection. |
| `SqlHighlightTransformation.kt` | Modified | Accept `List<TextRange>` for match overlay + bracket-pair offsets. |
| `QueryEditorScreen.kt` | Modified | Host `FindReplaceBar`, migrate cursor storage to `TextRange`. |
| `QueryEditorViewModel.kt` | Modified | Find/Replace state, `replaceAll` snapshot push. |
| `SqlTokenizerTest.kt` | Modified | New cases for bracket pairing inputs. |
| `app/src/test/.../domain/` | New tests | `BracketMatcherTest`, `FindReplaceEngineTest`, `MultiCursorEngineTest`. |
| `app/src/androidTest/.../queryeditor/` | New tests | `FindReplaceBarTest`, `SqlCodeEditorBracketHighlightTest`, `QueryEditorScreenAdvancedTest`. |
| `res/values*/strings.xml` (10 locales) | Modified | New labels: find, replace, regex, match case, whole word, replace all, no more occurrences. |

## Estimated Changed Lines

| Feature | LOC (impl + test) |
|---|---|
| Bracket matching + Auto-close | ~400 |
| Find & Replace | ~600 |
| Multi-cursor improvements + storage migration | ~300 |
| **Total** | **~1300** |

Per-PR sizes all under the 800-line review budget.

## PR Breakdown

Chained PR strategy = `ask-always`; this proposal recommends three slices and will surface the decision at the apply phase.

- **PR #4 — Bracket matching + Auto-close** (~400 LOC). Smallest, additive, validates the highlight-overlay pattern reused by Find. Depends only on existing tokenizer.
- **PR #5 — Find & Replace** (~600 LOC). Chains on PR #4 (reuses overlay pattern). Splits internally if needed: 5a non-regex find + highlight, 5b replace + regex/options.
- **PR #6 — Multi-cursor improvements + storage migration** (~300 LOC). Chains on PR #5 (uses `SqlTokenizer` IDENTIFIER boundaries finalized in earlier PR tests). Storage migration first commit, three shortcut handlers second commit.

Total ~1300 LOC across 3 PRs — safely within the 3 × 800 = 2400 cumulative budget.

## Risks

| # | Risk | Severity | Mitigation |
|---|------|----------|------------|
| R1 | **VisualTransformation chaining** — composing syntax highlight + match overlay + bracket overlay requires careful `OffsetMapping`; off-by-one corrupts cursor placement | High | Build a `CompositeVisualTransformation` helper with a single `OffsetMapping` resolver; cover with property-based JVM tests in PR #4 (where it's introduced). |
| R2 | **Multi-cursor selection storage migration** — moving from `MutableList<Int>` to `MutableList<TextRange>` touches the existing tested multi-cursor code path | Medium | Single isolated commit in PR #6 with `Int → TextRange(i, i)` adapter; existing tests stay green before any new behavior lands. |
| R3 | **Find bar layout on Compact width** — input + counter + 4 toggles + 4 buttons does not fit < 600 dp | Medium | Two-row layout; option toggles collapse into overflow `IconButton`; verified in `FindReplaceBarTest` with `setContent` size override. |
| R4 | **Auto-close + paste** — pasting `(foo)` would auto-add a closing paren and corrupt the text | Medium | Auto-close fires only when `newText.length - oldText.length == 1` (single-char insertion); covered by integration test. |
| R5 | **Shortcut collisions with IMEs** — Ctrl+D / Ctrl+F may be intercepted by some Android keyboards | Low | Use `onPreviewKeyEvent` (already the pattern); document in user help; remap surfaced as a v1.x settings task. |

## Rollback Plan

Each PR is independently revertible:

- **PR #6** (multi-cursor) — revert restores `MutableList<Int>` storage. The `Alt+Click` flow remained unchanged so users keep that capability.
- **PR #5** (find & replace) — revert removes `FindReplaceBar` host call and the find/replace shortcut entries; `EditorHistory` snapshots from prior `replaceAll` calls are preserved in DataStore but harmless.
- **PR #4** (bracket matching) — revert removes overlay + auto-close hooks; tokenizer remains as before.

DataStore migrations are append-only (find/replace adds no persisted state, multi-cursor selection state lives only in memory), so no DB schema rollback is required. Existing user queries are untouched.

## Dependencies

- **Depends on**: change `editor-completion-and-format` (merged). Provides `SqlTokenizer`, `EditorShortcuts`, `EditorHistory`, `onPreviewKeyEvent` priority chain, debounced 300 ms re-tokenization.
- **Blocks**: v1.3 features — `code-folding`, `editor-minimap-tablet`, `sql-refactoring`. These reuse the composite `VisualTransformation` introduced here.
- **No external library** additions (no JSqlParser, no third-party regex). Pure Kotlin + existing AndroidX.

## Success Criteria

- [ ] Cursor adjacent to `(` highlights both `(` and matching `)` within one frame after the 300 ms debounce; `Ctrl+Shift+\` moves cursor to the match.
- [ ] Typing `(` inserts `()` with cursor between; backspace removes both; auto-close does not fire inside strings/comments or on paste.
- [ ] `Ctrl+F` opens find bar; typing a query highlights all matches live; counter shows `n/m`; Enter / Shift+Enter cycle through matches; `Esc` closes.
- [ ] `Ctrl+H` toggles replace row; replace one / replace all work; regex / match-case / whole-word options behave per VS Code parity; `replaceAll` is a single `Ctrl+Z`-undoable step.
- [ ] `Ctrl+Alt+Down` / `Ctrl+Alt+Up` add column-preserving cursors on consecutive lines; no-op at document edges.
- [ ] `Ctrl+D` selects word under cursor on first press; subsequent presses add next forward occurrence; stops at last occurrence and shows snackbar "No more occurrences".
- [ ] All new strings present in all 10 locales (`res/values*/strings.xml`).
- [ ] Strict TDD honored: tests written before implementation per `.atl/standards/sdd-workflow.md`; coverage ≥ 80% project minimum on all new `domain/` engines.
- [ ] Every spec scenario from `sdd-spec` passes in `app/src/test/` (pure domain) and `app/src/androidTest/` (Compose UI).
