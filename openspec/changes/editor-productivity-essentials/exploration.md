# Exploration: Editor Productivity Essentials (Level 1)

Scope: code completion, undo/redo, find & replace, SQL format, keyboard shortcuts.

## Current State

The SQL editor is a `BasicTextField`-based component (`SqlCodeEditor.kt`) wrapped by `QueryEditorScreen.kt`. Key facts:

- **State**: `TextFieldValue` is hoisted in `QueryEditorScreen` via `remember { mutableStateOf(TextFieldValue(...)) }`. A single source of truth. No history is preserved — every edit replaces the previous value.
- **Highlighting**: `SqlTokenizer` (pure function) + `SqlHighlightTransformation` (VisualTransformation). Tokens are debounced 300ms via a `MutableStateFlow` — already a strong pattern we can reuse for completion.
- **Multi-cursor**: Custom logic inside `handleValueChange` that re-applies insertions/deletions across `cursorPositions`. This makes any history feature tricky — undo must restore both `TextFieldValue` AND `cursorPositions`.
- **Key handling today**: `Modifier.onKeyEvent` already used to track `Ctrl` left/right for multi-cursor. There is NO `onPreviewKeyEvent` yet. Tab/Enter/shortcut keys are NOT intercepted.
- **Toolbar**: Pill-shaped `Surface` with `IconButton`s. New buttons (Format, Find) fit naturally here.
- **i18n**: 10 locales (`values`, `values-es`, `-de`, `-fr`, `-ar`, `-hi`, `-ja`, `-pt-rBR`, `-ru`, `-zh-rCN`). Every new UI string MUST land in all 10.
- **Testing**: JVM unit tests via JUnit4 + Mockk. Compose UI tests live in `androidTest/`. Pure logic (tokenizer-style) is the easy win for TDD.
- **OpenSpec**: Config loaded (`strict_tdd: true`, `coverage_threshold: 0`, no linter/formatter). Change folder created at `openspec/changes/editor-productivity-essentials/`.

## Affected Areas

- `ui/screens/queryeditor/QueryEditorScreen.kt` — toolbar additions, shortcut wiring, dialog hosting
- `ui/screens/queryeditor/components/SqlCodeEditor.kt` — key event handling, completion popup
- `ui/screens/queryeditor/QueryEditorViewModel.kt` — possibly own history / format state
- `ui/screens/queryeditor/components/` — NEW: `EditorHistory.kt`, `SqlCompletionProvider.kt`, `SqlFormatter.kt`, `FindReplaceBar.kt`
- `app/src/main/res/values*/strings.xml` — 10 locales × new strings
- `app/src/test/java/.../queryeditor/` — unit tests for history, completion, formatter
- `app/src/androidTest/java/.../queryeditor/` — UI tests for shortcuts and Find/Replace

## Approaches per Feature

### 1. Code Completion

| Option | Pros | Cons |
|---|---|---|
| **A. Hardcoded keyword list + popup** | Zero deps, instant, fully offline, easy TDD on `SqlCompletionProvider` (pure function), tiny APK delta | No schema awareness (no table/column completion); manual list maintenance |
| **B. SQL grammar lib (JSqlParser / ANTLR)** | Full parse tree, smarter context | +1–3 MB APK, heavyweight on JVM, overkill for keywords, complicates testing, possible licensing notes (JSqlParser=Apache2 ✅, ANTLR runtime is bigger) |

**Recommendation: A**. Start with a curated keyword list (~120 SQL/MySQL/MariaDB keywords) reusing the existing `SqlKeywords` set from `SqlTokenizer`. The provider is a pure function `(text, cursorOffset) -> List<Suggestion>` — perfect for TDD. UI: a `Popup` anchored to the cursor's bounding box (we already capture `TextLayoutResult`). Debounce the same way as tokenization (≤150 ms, lighter than tokenizer). When schema-aware completion arrives later, plug a second provider — `B` becomes additive, not a rewrite.

**Performance**: trigger only on identifier-character keystrokes; filter by current word prefix; cap suggestions at 8; render via `LazyColumn` inside `Popup`. No frame drops at 1k LOC SQL.

**Complexity: Medium**.

### 2. Undo / Redo

| Option | Pros | Cons |
|---|---|---|
| **A. Manual stack of `TextFieldValue` snapshots** | Trivially correct, works today on Compose 1.6, simple TDD on a pure `EditorHistory` class | Memory grows with edits — needs bounding (cap 100 entries) and coalescing |
| **B. Compose built-in undo** | Free if it exists | `BasicTextField` (legacy) has NO public undo API in Compose BOM 2024.02.00. The new `BasicTextField2` / `TextFieldState` (Compose 1.7+) ships undo, but the project is on Compose BOM 2024.02.00 (1.6.x) — not available |
| **C. Command pattern (Insert/Delete commands)** | Smaller memory, semantic operations | Overengineered for a single-text editor; multi-cursor inserts already make commands non-trivial |

**Recommendation: A** with coalescing. Build a pure `EditorHistory(maxEntries = 100)` that stores `Snapshot(text, selection, cursorPositions)`. Coalesce consecutive single-character insertions of the same kind (typing a word = 1 undo step), and consecutive deletions. Flush coalescing on: cursor move > 1 char, newline, paste, blur, format. Memory budget: 100 × ~2 KB avg = ~200 KB — negligible.

Lives in the ViewModel (survives recomposition, allows tests without Compose).

**Complexity: Medium**. The tricky part is multi-cursor + coalescing interaction — needs explicit tests.

### 3. Find & Replace

| Option | Pros | Cons |
|---|---|---|
| **A. Modal `AlertDialog`** | Easy, consistent with save dialog | Blocks the editor; user can't see matches in context — bad UX for Find |
| **B. Inline top bar (VS Code style)** | Editor stays visible, matches highlight live, modern feel | Slightly more layout work; needs adaptive behavior for Compact width |
| **C. Bottom sheet** | Touch-friendly | Covers the bottom of the editor where results pane lives; awkward |

**Recommendation: B**. A slim row above the editor `Card` that slides in when active. State (`isFindOpen`, `findQuery`, `replaceQuery`, `currentMatchIndex`, `matches: List<IntRange>`) in the ViewModel. Matches rendered as a second pass on top of syntax highlighting via a small extension to the existing `VisualTransformation` (or an overlay `Canvas` like multi-cursor). Live highlight as user types in the find field (debounce 150 ms).

Adaptive: on Compact width, the bar stacks find/replace inputs vertically. For accessibility, the bar is focusable, supports `Esc` to close, `Enter` for next, `Shift+Enter` for previous, `Ctrl+Alt+Enter` for replace-all.

**Complexity: Medium-Large** (the largest of the five features).

### 4. Format SQL

| Option | Pros | Cons |
|---|---|---|
| **A. Custom formatter using existing tokenizer** | Zero deps, full control, reuses `SqlTokenizer`, pure TDD, tiny | Some edge cases (CTEs, window functions) need careful rules |
| **B. External lib (`sql-formatter` JS port / `JSqlParser` pretty-printer)** | Off the shelf | JS ports aren't great for JVM/Android; JSqlParser adds weight (~1.5 MB) and doesn't really format, it serializes |
| **C. Server-side API** | None — requires network, breaks offline | Project is offline-first by design |

**Recommendation: A**. Implement `SqlFormatter` as a pure function over the existing `SqlToken` list. Rules: uppercase keywords (configurable later), newline before major clauses (`FROM`, `WHERE`, `JOIN`, `ORDER BY`, `GROUP BY`, `HAVING`, `LIMIT`), 2-space indent for sub-clauses, single space around binary operators, normalize commas. Skip formatting inside strings/comments (already separate token kinds — free correctness).

Trigger from a toolbar button **and** `Ctrl+Shift+F`. Push the pre-format value to `EditorHistory` so undo restores it.

**Complexity: Small-Medium**. TDD-friendly with golden-file fixtures.

### 5. Keyboard Shortcuts

| Option | Pros | Cons |
|---|---|---|
| **A. `Modifier.onPreviewKeyEvent` at editor root** | Intercepts before TextField, works for physical keyboards, can be composed at screen level for shortcuts that don't need focus | Some Android IMEs don't dispatch Ctrl+key through soft keyboard |
| **B. `LocalSoftwareKeyboardController` + IME actions** | Works on soft keyboards | IME actions are single (e.g. "Done"), can't express Ctrl+Enter / Ctrl+S |
| **C. Custom KeyEventHandler at Activity level** | Catches everything | Bypasses Compose state, harder to test, leaks UI logic into Activity |

**Recommendation: A**. `onPreviewKeyEvent` at the `Column` root in `QueryEditorScreen` for screen-level shortcuts (Ctrl+Enter run, Ctrl+S save, Ctrl+F find, Ctrl+H replace, Ctrl+Shift+F format) and at the `BasicTextField` for editor-local (Ctrl+Z undo, Ctrl+Y / Ctrl+Shift+Z redo, Tab indent — already handled implicitly). Physical keyboards are the realistic target; document that on soft keyboards shortcuts may not fire (most Android soft keyboards don't send Ctrl combos anyway — toolbar buttons cover that path).

Helper: `EditorShortcuts.handle(event, actions)` — a pure function from `KeyEvent` to an optional `EditorAction` sealed class. Testable without Compose by passing fake key events.

**Complexity: Small**.

## Recommended Implementation Order

1. **Keyboard shortcuts scaffold** (Small) — pure `EditorShortcuts` mapper + wiring for existing actions (Ctrl+Enter run, Ctrl+S save). Lands fast, validates the key-event pipeline, no UI change.
2. **Undo/Redo** (Medium) — `EditorHistory` in ViewModel + Ctrl+Z/Ctrl+Y. Foundation that Format & multi-cursor edits depend on.
3. **Format SQL** (Small-Medium) — `SqlFormatter` pure function, toolbar button, Ctrl+Shift+F. Reuses tokenizer and history.
4. **Code completion** (Medium) — `SqlCompletionProvider` + popup. Debounced like tokenizer.
5. **Find & Replace** (Medium-Large) — inline bar + match highlighting. Biggest UX surface; lands last so it benefits from the shortcut and history infra.

This order keeps each step under the 800-line review budget (D2) and lets you ship value after each PR.

## Testing Strategy

- **Pure logic (JVM unit tests, fast)**:
  - `EditorHistoryTest` — push/undo/redo, coalescing, bound enforcement, multi-cursor snapshots
  - `SqlCompletionProviderTest` — prefix filtering, context (no completion inside strings/comments), keyword set coverage
  - `SqlFormatterTest` — golden-file fixtures (`input.sql` → `expected.sql`), idempotency (format(format(x)) == format(x)), preservation of strings/comments
  - `EditorShortcutsTest` — key event → action mapping table-driven
- **Compose UI tests (`androidTest/`)**:
  - Find bar opens on Ctrl+F, focuses input, highlights matches, Esc closes
  - Completion popup appears, arrow keys navigate, Enter inserts
  - Toolbar Format button reformats text and undo restores
- **No Robolectric** — the architecture already keeps logic out of composables; lean on pure JVM tests.

## Risks & Challenges

1. **Multi-cursor × Undo** — `handleValueChange` mutates `cursorPositions` as a side effect. The history snapshot must capture both `TextFieldValue` and `cursorPositions`, and `undo()` must restore both atomically. Mitigation: ViewModel owns both, history stores `Snapshot(value, cursors)`.
2. **Soft keyboard shortcut delivery** — most Android IMEs don't emit Ctrl combos. Mitigation: document physical-keyboard expectation; always provide a toolbar/menu equivalent for every shortcut.
3. **Compose BOM 2024.02.00 limits** — no `TextFieldState`/`BasicTextField2` undo. Mitigation: build our own; revisit when the project upgrades Compose.
4. **i18n scale-out** — 10 locales × ~15 new strings = 150 translation entries. Mitigation: land English/Spanish copy first with TODO markers in other locales, batch-translate in a follow-up.
5. **Find highlighting × existing syntax highlighting** — two `VisualTransformation`s don't compose cleanly. Mitigation: extend `SqlHighlightTransformation` to accept an optional list of match ranges, OR draw match backgrounds via an overlay `Canvas` (same trick as multi-cursor — already proven in this codebase).
6. **Performance on 5k+ LOC SQL** — completion and find on huge files. Mitigation: debounce 150 ms, cap matches at 1000 with "show more" affordance, do search work in `Default` dispatcher.
7. **Format button on multi-cursor active state** — formatting invalidates manual cursor positions. Mitigation: clear `cursorPositions` on format and push the operation onto the history stack.

## Open Questions for Orchestrator (before proposal)

1. **Completion trigger UX**: auto-popup on every identifier char, or only on `Ctrl+Space`? (Affects perceived "intrusiveness".)
2. **Format style**: uppercase keywords by default, or preserve user case? (Affects diff noise.)
3. **Replace All confirmation**: do we need a confirm dialog or trust the undo?
4. **Single change vs five changes** in OpenSpec: bundle all five into one proposal (cohesive "Productivity Essentials"), or split? Given the 800-line review budget, splitting into 2–3 chained proposals (shortcuts+undo, format+completion, find/replace) is the realistic path.

## Ready for Proposal

**Yes — with the four questions above answered first**. Recommendation to orchestrator: ask the user the four questions in a single batched message (not one by one). Once answered, propose splitting into 3 chained OpenSpec changes to respect the 800-line review budget:

- `editor-shortcuts-and-history` (shortcuts + undo/redo)
- `editor-format-and-completion` (formatter + completion)
- `editor-find-replace` (find & replace bar)
