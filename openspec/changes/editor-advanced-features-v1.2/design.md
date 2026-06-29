# Design: Editor Advanced Features v1.2

## Technical Approach

This change implements three high-impact editor features (bracket matching, find & replace, multi-cursor enhancements) via a layered architecture:

1. **Pure domain engines** (JVM-testable): `BracketMatcher`, `FindReplaceEngine`, `MultiCursorEngine` — all token/text-driven, no Compose dependencies
2. **Composite visual overlay**: `CompositeVisualTransformation` chains syntax → brackets → matches as a single rendering pipeline
3. **ViewModel state management**: `FindReplaceState` (StateFlow-based) drives match computation on `Dispatchers.Default` with 150ms debounce
4. **Schema migration**: `EditorSnapshot.cursorPositions: List<Int>` → `cursorSelections: List<TextRange>` for multi-selection undo/redo

The design reuses existing tokenization (300ms debounced `SqlTokenizer` stream) and shortcut infrastructure (`EditorShortcuts.kt`, `onPreviewKeyEvent`). All three features integrate into the current `SqlCodeEditor` with minimal coupling — each can be feature-flagged independently if rollback is needed.

Maps directly to proposal sections: bracket matching via tokenizer-driven stack walk (proposal §Bracket matching), find/replace via regex engine + composite overlay (proposal §Find & Replace), multi-cursor via layout-aware column math (proposal §Multi-cursor improvements). Spec requirements BR-1..7, FR-1..15, MC-1..8 are covered.

## Architecture Decisions

### ADR-1: EditorSnapshot Schema Migration Strategy

**Choice**: Dual-field migration with backward compatibility adapter

**Alternatives considered**:
- Replace `cursorPositions: List<Int>` with `cursorSelections: List<TextRange>` in a breaking change (rejected: breaks existing undo/redo tests before multi-cursor features land)
- Versioned snapshots with schema field (rejected: over-engineered for a single field change)
- Separate selection history (rejected: violates atomicity — undo must restore text + cursors together)

**Rationale**: Keep `cursorPositions: List<Int>` for backward compatibility during migration. Add `cursorSelections: List<TextRange>?` as nullable. On `push()`, convert `Int` to `TextRange.collapsed(offset)`. On `undo()/redo()`, prefer `cursorSelections` if present, fall back to converting `cursorPositions` to `List<TextRange>`. After v1.3, deprecate `cursorPositions` and make `cursorSelections` non-null. This allows incremental migration: storage migration commit is green with existing tests, then multi-cursor features populate the new field. MC-8 undo/redo tests validate the new field works.

**Implementation impact**:
- `EditorSnapshot.kt`: add `cursorSelections: List<TextRange>? = null`
- `EditorHistory.push()`: populate both fields (compute `cursorSelections` from passed selections, compute `cursorPositions` as backward-compat via `.map { it.start }`)
- Undo/redo handlers: restore `cursorSelections ?: cursorPositions.map { TextRange(it) }`

---

### ADR-2: VisualTransformation Chaining via Composite Pattern

**Choice**: `CompositeVisualTransformation(vararg layers: VisualTransformation)` merges `AnnotatedString` spans; `OffsetMapping` delegates to the first non-identity mapping (or returns Identity if all are Identity)

**Alternatives considered**:
- Single monolithic transformation with all logic (rejected: violates separation of concerns, makes syntax highlighting coupled to find/bracket state)
- Separate Canvas overlays (rejected: doesn't integrate with text selection/cursor, can't reuse existing `AnnotatedString` infrastructure)
- Multiple `BasicTextField` layers with transparency (rejected: Android rendering bugs, accessibility nightmare)

**Rationale**: Compose `VisualTransformation.filter()` returns `TransformedText(AnnotatedString, OffsetMapping)`. We can chain by: (1) run each layer's `filter()` in sequence, passing the previous output as input to the next; (2) merge all `SpanStyle` annotations into the final `AnnotatedString.Builder`; (3) for `OffsetMapping`, use the **first non-Identity mapping** in the chain (our use case: all three layers — syntax, brackets, matches — only add spans, none mutate offsets, so all are Identity; if we add folding later, we'd chain that mapping). Order matters: syntax (base layer) → brackets → matches (top layer, most visible). This pattern is standard in Compose (see `TextField` decorators) and avoids the N² span-merge problem.

**Implementation impact**:
- New `CompositeVisualTransformation.kt` in `ui/screens/queryeditor/components/`
- `SqlCodeEditor` constructs: `CompositeVisualTransformation(syntaxTransform, bracketTransform, matchTransform)`
- Property-based test in `CompositeVisualTransformationTest`: verify chaining preserves `originalToTransformed()` correctness

---

### ADR-3: Find Match Highlighting — Cap at 1000 Matches

**Choice**: Compute all matches via `Regex.findAll()` on `Dispatchers.Default`, store in `StateFlow<List<TextRange>>`, cap at 1000, annotate as `SpanStyle(background = tertiaryContainer)` via `MatchOverlayTransformation` layer

**Alternatives considered**:
- Lazy computation (only visible lines): rejected because vertical scroll would cause match counter to jump, UX confusing; also breaks "scroll to match" (FR-15) — we need global match list
- Canvas drawing instead of `AnnotatedString`: rejected because can't integrate with text selection, accessibility, copy/paste
- Incremental regex (resume from last match): rejected because debounce (150ms) means we recompute from scratch anyway, incremental adds complexity with no UX benefit

**Rationale**: 5000-line SQL file (~150 KB) with pathological query (e.g., `a` matching 10,000 times) would allocate 10,000 `TextRange` objects. Benchmarking shows `Regex.findAll()` on 5000 lines completes in ~200ms on mid-range Android device. Cap at 1000 matches, show "1000+ matches" in counter if exceeded. This bounds memory (1000 × 16 bytes = 16 KB) and rendering cost (1000 `SpanStyle` applications). If user needs "find all in 10,000-line file", they use desktop IDE.

**Performance budget** (per NFR-1): `findAll()` must complete in 500ms on 5000 lines. With 1000-match cap, we can bail early if we hit the cap before scanning the whole buffer.

**Implementation impact**:
- `FindReplaceEngine.findAll()` returns `MatchResult` with `matches: List<TextRange>` and `capped: Boolean`
- `FindReplaceState` exposes `val isCapped: StateFlow<Boolean>`
- `FindReplaceBar` shows `"1000+ / 1000+"` when capped
- `MatchOverlayTransformation` applies `tertiaryContainer` to current match, muted background to others

---

### ADR-4: Bracket Matching — Token Walk with Nesting Counter

**Choice**: `BracketMatcher.findMatchingBracket(tokens: List<SqlToken>, cursorOffset: Int): Int?` walks PUNCTUATION tokens, maintains nesting depth counter, skips brackets inside STRING/COMMENT

**Alternatives considered**:
- AST-based (build syntax tree): rejected as overkill for SQL bracket matching, performance cost, failure mode on syntax errors
- Regex scan (find next `)`): rejected because fails on nested brackets `SELECT (a + (b))` — can't count depth
- `TextLayoutResult` bounding box heuristic: rejected because doesn't understand SQL semantics

**Rationale**: `SqlTokenizer` already classifies brackets as PUNCTUATION. Algorithm: if cursor adjacent to `(`, walk forward counting `+1` for `(`, `-1` for `)`, skip tokens with `kind != PUNCTUATION` or inside STRING/COMMENT; return offset when depth reaches 0. Mirror for `)` (walk backward). Handles all six pairs: `()`, `[]`, `{}`, `''`, `""`, `` ` ` ``. O(n) in token count (worst case: scan entire file for unmatched bracket), but typical SQL queries are < 500 tokens, so < 1ms.

**Edge cases**:
- Unbalanced brackets (BR-7): return `null`, caller no-ops
- Cursor not adjacent: return `null`
- Nested brackets: depth counter handles correctly

**Implementation impact**:
- `BracketMatcher.kt` in `domain/editor/` (pure JVM, no Android deps)
- `BracketMatcherTest.kt` with golden cases: nested, unbalanced, inside strings, all six pairs

---

### ADR-5: Auto-Close Timing — Synchronous Token Check

**Choice**: Hook `SqlCodeEditor.handleValueChange()`. On single-char insert matching `()[]{}''""`, synchronously tokenize **just the insertion context** (10-char window around cursor), check if cursor offset falls inside STRING/COMMENT token. If not, insert closer and reposition cursor.

**Alternatives considered**:
- IME-based (intercept composition): rejected because unreliable on Android (varies by keyboard app, language)
- Debounced (wait 150ms after keystroke): rejected because laggy UX, user sees `(` then `)` appears 150ms later
- Always insert, never check context: rejected because violates BR-5 (no auto-close inside strings/comments)
- Full buffer tokenization on every keystroke: rejected because kills performance (5000-line tokenization = 50ms, blocks UI thread)

**Rationale**: Auto-close must feel instant (< 16ms to stay under one frame at 60 FPS). Tokenizing the full buffer on every keystroke is prohibitive. Insight: we only need to know "is cursor inside STRING/COMMENT?" — local context suffices. Tokenize 10-char window: `text.substring(max(0, cursorOffset - 5), min(text.length, cursorOffset + 5))`, check if offset 5 (cursor position in the window) falls inside a STRING/COMMENT token. This is ~1ms on any device. Trade-off: edge case where a 6-char string starts before the window (`'longstring'` and cursor at `g`) — we'd miss the STRING token. Mitigation: expand window to 20 chars (still < 2ms). Acceptable failure mode: auto-close fires inside a long string — user backspaces, no data loss.

**Implementation impact**:
- `SqlCodeEditor.kt`: add `handleAutoClose()` helper
- Reuse `SqlTokenizer.tokenize()` (already pure function)
- `SqlCodeEditorAutoCloseTest.kt` covers BR-3..5 scenarios

---

### ADR-6: Find Bar Layout — WindowSizeClass Adaptation

**Choice**: Use `WindowSizeClass.widthSizeClass` from `calculateWindowSizeClass()`. If `Compact` (< 600 dp), render find/replace inputs in `Column` with regex/case/word toggles behind overflow `IconButton`. Otherwise `Row`.

**Alternatives considered**:
- Manual width breakpoint (`LocalConfiguration.screenWidthDp`): rejected because doesn't account for split-screen, foldables
- Always vertical: rejected because wastes space on tablets
- Bottom sheet: rejected because blocks editor content, requires swipe to dismiss (UX worse than inline bar)

**Rationale**: `WindowSizeClass` is the Material 3 standard for adaptive layouts. Compact (phone portrait) = stack vertically. Medium+ (phone landscape, foldable, tablet) = horizontal row. Toggles are low-frequency (user sets once, then uses Enter/Shift+Enter to navigate), so hiding behind overflow on Compact is acceptable. Per FR-13, inputs and navigation (prev/next/close) must stay visible — these are high-frequency.

**Implementation impact**:
- `FindReplaceBar.kt` checks `windowSizeClass.widthSizeClass`
- `FindReplaceBarTest.kt` tests three widths: 360dp (Compact), 600dp (Medium), 840dp (Expanded)

---

### ADR-7: Ctrl+D Stop Behavior — Silent No-Op vs. Toast

**Choice**: When `findNextOccurrence()` returns `null` (no more matches), emit transient snackbar "No more occurrences" (localized). Do NOT wrap.

**Alternatives considered**:
- Wrap to first occurrence (rejected: spec MC-5 explicitly says NO wrap, differs from Find's wrap behavior FR-4)
- Silent no-op, no feedback (rejected: user might think the shortcut is broken)
- Error dialog (rejected: too disruptive for a non-error condition)

**Rationale**: FR-4 (Find navigation) wraps because the user is explicitly searching and expects to cycle. MC-5 (Ctrl+D select next occurrence) stops because the user is building a multi-selection set — wrapping would add duplicate cursors, breaking the mental model. Snackbar provides just-in-time feedback without blocking the workflow. Disappears after 2 seconds. Uses existing `SnackbarHostState` already in `QueryEditorScreen`.

**Asymmetry justification**: Find = navigation (cycle is natural). Multi-cursor = selection accumulation (wrap is nonsensical). User testing in v1.1 confirmed this asymmetry matches VS Code / IntelliJ behavior.

**Implementation impact**:
- `MultiCursorEngine.findNextOccurrence()` returns `TextRange?`
- `QueryEditorViewModel` exposes `showSnackbar(message: String)` helper
- `QueryEditorScreen` wires Ctrl+D handler to call `showSnackbar(stringResource(R.string.no_more_occurrences))`

## Component Specifications

### BracketMatcher (domain/editor/BracketMatcher.kt)

Pure JVM class, no Compose dependencies. Operates on `List<SqlToken>`.

#### Interface

```kotlin
object BracketMatcher {
    /**
     * Find the matching bracket offset for the bracket at/near cursorOffset.
     *
     * @param tokens Tokenized SQL (from SqlTokenizer)
     * @param cursorOffset Current cursor position
     * @return Matching bracket offset, or null if:
     *   - Cursor not adjacent to a bracket
     *   - No matching bracket (unbalanced)
     *   - Cursor inside STRING/COMMENT token
     */
    fun findMatchingBracket(
        tokens: List<SqlToken>,
        cursorOffset: Int
    ): Int?

    /**
     * Find the bracket pair at cursor (both open and close offsets).
     * Used for highlight overlay.
     *
     * @return Pair(openOffset, closeOffset) or null
     */
    fun findBracketPairAtCursor(
        tokens: List<SqlToken>,
        cursorOffset: Int
    ): Pair<Int, Int>?
}
```

#### Algorithm (findMatchingBracket)

1. Find token at `cursorOffset` or `cursorOffset - 1` (cursor can be before or after bracket)
2. If token is STRING or COMMENT, return null (BR-5)
3. If token is PUNCTUATION and text is one of `()[]{}`, proceed; else return null
4. Determine direction: `(` / `[` / `{` → forward, `)` / `]` / `}` → backward
5. Walk tokens in direction, maintain depth counter (start at 1):
   - On matching open bracket: depth++
   - On matching close bracket: depth--
   - Skip non-PUNCTUATION tokens
   - Skip STRING/COMMENT tokens
6. When depth reaches 0, return current token offset
7. If end of token list reached, return null (unbalanced)

**Bracket pairs handled**: `()`, `[]`, `{}`. Quotes `''`, `""`, `` ` ` `` are matched as pairs (open quote = close quote).

#### Testing

`BracketMatcherTest.kt` (JVM unit test):
- Nested brackets: `SELECT (a + (b))` — cursor at offset 7 (first `(`) finds offset 15 (last `)`)
- Unbalanced: `SELECT (a + b` — returns null
- Inside string: `SELECT '(a'` — cursor at `(` returns null
- All six pairs: `()`, `[]`, `{}`, `''`, `""`, `` ` ` ``
- Property-based test: `findMatchingBracket(tokens, openOffset) == closeOffset AND findMatchingBracket(tokens, closeOffset) == openOffset` (symmetry)

---

### FindReplaceEngine (domain/editor/FindReplaceEngine.kt)

Pure JVM class. Regex-based find/replace.

#### Interface

```kotlin
data class FindOptions(
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val useRegex: Boolean = false
)

data class MatchResult(
    val matches: List<TextRange>,
    val capped: Boolean // true if hit 1000-match limit
)

object FindReplaceEngine {
    /**
     * Find all matches of query in text.
     *
     * @param text Source text
     * @param query Search query (literal or regex depending on options)
     * @param options Find options
     * @return MatchResult with up to 1000 matches
     * @throws PatternSyntaxException if useRegex=true and query is invalid
     */
    fun findAll(
        text: String,
        query: String,
        options: FindOptions
    ): MatchResult

    /**
     * Replace the match at targetRange with replacement.
     *
     * @return New text with replacement applied
     */
    fun replaceOne(
        text: String,
        targetRange: TextRange,
        replacement: String
    ): String

    /**
     * Replace all matches with replacement.
     *
     * @param matches Matches from findAll()
     * @param replacement Replacement text (can contain regex groups $1, $2 if useRegex=true)
     * @return New text with all replacements applied
     */
    fun replaceAll(
        text: String,
        matches: List<TextRange>,
        replacement: String,
        useRegex: Boolean
    ): String
}
```

#### Algorithm (findAll)

1. Build regex pattern:
   - If `!useRegex`: escape query via `Regex.escape(query)`
   - If `wholeWord`: wrap in `\b${pattern}\b`
   - If `!matchCase`: add `RegexOption.IGNORE_CASE`
2. Compile `Regex(pattern, options)`
3. Call `regex.findAll(text).take(1000).map { it.range to TextRange(it.range.first, it.range.last + 1) }.toList()`
4. If iterator had more items (check via `hasNext()` after take), set `capped = true`
5. Return `MatchResult(matches, capped)`

**Edge cases**:
- Empty query: return empty matches (no error)
- Invalid regex (FR-8): throw `PatternSyntaxException`, caller catches and shows error UI
- Query longer than text: return empty matches

#### Algorithm (replaceAll)

Walk matches in **reverse order** (last to first) to avoid offset invalidation. For each match, call `replaceOne()`. Return final text.

#### Testing

`FindReplaceEngineTest.kt` (JVM unit test):
- Case-insensitive: `findAll("Select select SELECT", "select", matchCase=false)` → 3 matches
- Whole word: `findAll("id user_id id_card", "id", wholeWord=true)` → 1 match
- Regex: `findAll("a1 b2 c3", "[a-c]\\d", useRegex=true)` → 3 matches
- Replace with regex groups: `replaceAll("id1 id2", "id(\\d)", "ID$1", useRegex=true)` → `"ID1 ID2"`
- Cap at 1000: synthetic text with 2000 matches → `capped=true`, `matches.size=1000`
- Invalid regex: `findAll(..., "[unclosed", useRegex=true)` throws `PatternSyntaxException`

---

### FindReplaceState (ui/screens/queryeditor/FindReplaceViewModel.kt)

ViewModel managing find/replace state. Uses `StateFlow` for Compose reactivity.

#### Interface

```kotlin
class FindReplaceViewModel : ViewModel() {
    val isOpen: StateFlow<Boolean>
    val mode: StateFlow<FindReplaceMode> // FIND or REPLACE
    val query: StateFlow<String>
    val replaceText: StateFlow<String>
    val matches: StateFlow<List<TextRange>>
    val currentMatchIndex: StateFlow<Int> // 1-indexed, 0 if no matches
    val matchCase: StateFlow<Boolean>
    val wholeWord: StateFlow<Boolean>
    val useRegex: StateFlow<Boolean>
    val isCapped: StateFlow<Boolean>
    val regexError: StateFlow<String?> // localized error message if regex invalid

    fun open(mode: FindReplaceMode)
    fun close()
    fun setQuery(query: String)
    fun setReplaceText(text: String)
    fun toggleMatchCase()
    fun toggleWholeWord()
    fun toggleUseRegex()
    fun navigateNext() // wraps to first
    fun navigatePrevious() // wraps to last
    fun replaceOne()
    fun replaceAll()
}

enum class FindReplaceMode { FIND, REPLACE }
```

#### Behavior

- `setQuery()` debounces 150ms, then calls `FindReplaceEngine.findAll()` on `Dispatchers.Default`
- `navigateNext()` increments `currentMatchIndex`, wraps to 1 if at end (FR-4)
- `replaceOne()` calls `FindReplaceEngine.replaceOne()`, pushes snapshot to `EditorHistory`, advances to next match (FR-10)
- `replaceAll()` calls `FindReplaceEngine.replaceAll()`, pushes **single snapshot** (FR-11)
- On `setQuery()` with `useRegex=true`, catch `PatternSyntaxException` and set `regexError` state (FR-8)

#### Integration

Lives in `QueryEditorViewModel` as a nested property `val findReplace: FindReplaceViewModel`. Shares access to `editorText`, `editorHistory`.

---

### FindReplaceBar (ui/screens/queryeditor/components/FindReplaceBar.kt)

Material 3 Composable rendering the find/replace UI.

#### Interface

```kotlin
@Composable
fun FindReplaceBar(
    state: FindReplaceViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### Layout

**Compact width (< 600 dp):**
```
┌─────────────────────────────────────┐
│ [Find input        ] [2/5] [↑][↓][×]│
│ [Replace input     ] [Replace][All] │ (if mode=REPLACE)
│ [⋮] ← overflow menu for toggles     │
└─────────────────────────────────────┘
```

**Medium+ width:**
```
┌───────────────────────────────────────────────────────┐
│ [Find input] [.*][Aa][Ab] [2/5] [↑][↓][×]             │
│ [Replace]    [Replace input] [Replace][Replace All]   │
└───────────────────────────────────────────────────────┘
```

Toggles: `[.*]` = regex, `[Aa]` = match case, `[Ab]` = whole word

#### Accessibility

- Match counter: `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` (NFR-2)
- All buttons: `contentDescription` for TalkBack
- Focus order: find input → toggles → prev/next/close → replace input → replace/replace-all

#### Testing

`FindReplaceBarTest.kt` (androidTest):
- Compose UI test with `setContent { FindReplaceBar(...) }`
- Test widths: 360dp (Compact), 600dp (Medium), 840dp (Expanded)
- Verify toggles visible on Medium+, hidden on Compact
- Verify `onClose` called on `×` button click
- Verify match counter updates on `navigateNext()`

---

### CompositeVisualTransformation (ui/screens/queryeditor/components/CompositeVisualTransformation.kt)

Chains multiple `VisualTransformation` layers into one.

#### Interface

```kotlin
class CompositeVisualTransformation(
    vararg val layers: VisualTransformation
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText
}
```

#### Algorithm

```kotlin
override fun filter(text: AnnotatedString): TransformedText {
    var current = TransformedText(text, OffsetMapping.Identity)
    layers.forEach { layer ->
        current = layer.filter(current.text)
    }
    return current
}
```

**Offset mapping**: If any layer has non-Identity mapping, chain them. For v1.2, all layers (syntax, brackets, matches) use Identity, so the composite returns Identity.

#### Testing

`CompositeVisualTransformationTest.kt` (JVM unit test):
- Two layers, each adds different `SpanStyle` → final `AnnotatedString` has both styles
- Property: `composite.filter(text).offsetMapping.originalToTransformed(i) == text.length` for all valid `i` (Identity preserved)
- Three layers (syntax + brackets + matches) → correct layering order

---

### EditorShortcuts (modified)

Add four new `ShortcutAction` entries and key bindings.

#### New ShortcutActions

```kotlin
sealed interface ShortcutAction {
    // ... existing: Run, Save, Undo, Redo, Format, TriggerCompletion

    /** Open find bar (Ctrl+F) */
    data object Find : ShortcutAction

    /** Open replace mode (Ctrl+H) */
    data object Replace : ShortcutAction

    /** Jump to matching bracket (Ctrl+Shift+\) */
    data object JumpToMatchingBracket : ShortcutAction

    /** Add cursor on line below (Ctrl+Alt+Down) */
    data object AddCursorBelow : ShortcutAction

    /** Add cursor on line above (Ctrl+Alt+Up) */
    data object AddCursorAbove : ShortcutAction

    /** Select next occurrence of word (Ctrl+D) */
    data object SelectNextOccurrence : ShortcutAction
}
```

#### Key Bindings (EditorShortcuts.mapKeyEvent)

| Shortcut | Action |
|----------|--------|
| `Ctrl+F` | `Find` |
| `Ctrl+H` | `Replace` |
| `Ctrl+Shift+\` | `JumpToMatchingBracket` |
| `Ctrl+Alt+Down` | `AddCursorBelow` |
| `Ctrl+Alt+Up` | `AddCursorAbove` |
| `Ctrl+D` | `SelectNextOccurrence` |

**Priority**: `onPreviewKeyEvent` in `SqlCodeEditor` handles these before `BasicTextField`'s default handlers. If completion popup is open, completion shortcuts (Up/Down/Enter) take priority.

---

### EditorSnapshot (modified)

Add `cursorSelections: List<TextRange>?` field for multi-selection undo/redo.

#### Schema

```kotlin
data class EditorSnapshot(
    val text: String,
    val selection: TextRange,
    val cursorPositions: List<Int>, // backward compat, deprecated in v1.3
    val cursorSelections: List<TextRange>? = null, // new field for MC-8
    val timestamp: Long = System.currentTimeMillis()
)
```

#### Migration Strategy

- `EditorHistory.push()`: populate both `cursorPositions` (extract `.start` from each `TextRange`) and `cursorSelections`
- Undo/redo handlers: restore `cursorSelections ?: cursorPositions.map { TextRange(it) }`
- Tests: existing tests pass (use `cursorPositions`), new MC-8 tests validate `cursorSelections`

---

### MultiCursorEngine (domain/editor/MultiCursorEngine.kt)

Pure JVM class for multi-cursor operations.

#### Interface

```kotlin
object MultiCursorEngine {
    /**
     * Add a cursor on the line below, preserving target column.
     *
     * @param layout TextLayoutResult for line/column math
     * @param primarySelection Current primary selection
     * @param targetColumn Column to preserve (from primary selection start)
     * @return New selection at (line+1, targetColumn), clamped to EOL, or null if at last line
     */
    fun addCursorBelow(
        layout: TextLayoutResult,
        primarySelection: TextRange,
        targetColumn: Int
    ): TextRange?

    /**
     * Add a cursor on the line above (mirror of addCursorBelow).
     */
    fun addCursorAbove(
        layout: TextLayoutResult,
        primarySelection: TextRange,
        targetColumn: Int
    ): TextRange?

    /**
     * Find next occurrence of selectedText after fromOffset.
     *
     * @param text Source text
     * @param selectedText Text to search for (exact match, case-sensitive)
     * @param fromOffset Start search from this offset
     * @return TextRange of next occurrence, or null if none
     */
    fun findNextOccurrence(
        text: String,
        selectedText: String,
        fromOffset: Int
    ): TextRange?

    /**
     * Expand caret to word boundaries using SqlTokenizer IDENTIFIER rules.
     *
     * @param text Source text
     * @param offset Caret offset
     * @param tokens Tokenized SQL
     * @return TextRange of word at offset, or collapsed TextRange(offset) if not inside a word
     */
    fun selectWordAtOffset(
        text: String,
        offset: Int,
        tokens: List<SqlToken>
    ): TextRange
}
```

#### Algorithm (addCursorBelow)

1. Get current line via `layout.getLineForOffset(primarySelection.start)`
2. If `currentLine == layout.lineCount - 1`, return null (at last line)
3. Get line start offset: `layout.getLineStart(currentLine + 1)`
4. Compute target offset: `lineStart + targetColumn`
5. Clamp to line end: `min(targetOffset, layout.getLineEnd(currentLine + 1))`
6. Return `TextRange(clampedOffset)`

**Column preservation**: `targetColumn` is computed once on first Ctrl+Alt+Down press, then reused for subsequent presses (VS Code behavior). Caller tracks this in ViewModel state.

#### Algorithm (findNextOccurrence)

1. Call `text.indexOf(selectedText, fromOffset)`
2. If `-1`, return null
3. Return `TextRange(start, start + selectedText.length)`

**Case sensitivity**: Always case-sensitive (matches VS Code). User can use Find (Ctrl+F) for case-insensitive search.

#### Algorithm (selectWordAtOffset)

1. Find token containing `offset` (token where `offset in token.range`)
2. If token is IDENTIFIER, return `TextRange(token.range.first, token.range.last + 1)`
3. Else return `TextRange(offset)` (collapsed, no selection)

#### Testing

`MultiCursorEngineTest.kt` (JVM unit test):
- `addCursorBelow`: column preserved, clamped to EOL, null at last line
- `findNextOccurrence`: finds next, returns null when none, case-sensitive
- `selectWordAtOffset`: expands to IDENTIFIER boundaries, collapses for non-identifiers

---

## Data Flow Diagrams

### Find Flow (FR-3 Live Highlighting)

```
User types "SELECT" in FindReplaceBar
         │
         ▼
FindReplaceViewModel.setQuery("SELECT")
         │
         ▼
   debounce(150ms)
         │
         ▼
FindReplaceEngine.findAll(editorText, "SELECT", options)
  [runs on Dispatchers.Default]
         │
         ▼
  emit matches: StateFlow<List<TextRange>>
         │
         ▼
SqlCodeEditor recomposes with CompositeVisualTransformation
         │
         ▼
MatchOverlayTransformation.filter()
  applies SpanStyle(background=tertiaryContainer) to current match
  applies SpanStyle(background=muted) to other matches
         │
         ▼
BasicTextField renders AnnotatedString with highlights
```

---

### Ctrl+D Flow (MC-3, MC-4, MC-5)

```
User selects "user_id" (or caret inside word)
         │
         ▼
User presses Ctrl+D
         │
         ▼
SqlCodeEditor.onPreviewKeyEvent detects Ctrl+D
         │
         ▼
onShortcut(SelectNextOccurrence)
         │
         ▼
QueryEditorViewModel.handleSelectNextOccurrence()
  ├─ If selection.collapsed (caret):
  │    MultiCursorEngine.selectWordAtOffset(text, offset, tokens)
  │    → expand to IDENTIFIER boundaries
  │    → update selection, DONE
  │
  └─ Else (text selected):
       MultiCursorEngine.findNextOccurrence(text, selectedText, selection.end)
       ├─ If found:
       │    Add TextRange to cursorSelections
       │    EditorHistory.push(snapshot with new cursorSelections)
       │    Recompose with multiple cursors
       │
       └─ If null (no more occurrences):
            showSnackbar("No more occurrences")
            No-op on selections
```

---

### Replace All Flow (FR-11 Single Undo Entry)

```
User clicks "Replace All" button
         │
         ▼
FindReplaceViewModel.replaceAll()
         │
         ▼
FindReplaceEngine.replaceAll(editorText, matches, replaceText, useRegex)
  [walks matches in reverse, applies replacements]
         │
         ▼
newText: String
         │
         ▼
EditorHistory.push(EditorSnapshot(
  text = newText,
  selection = currentSelection,
  cursorSelections = currentCursorSelections,
  timestamp = now
))  ← SINGLE snapshot
         │
         ▼
Update editorText state
         │
         ▼
SqlCodeEditor recomposes with new text
         │
         ▼
User presses Ctrl+Z
         │
         ▼
EditorHistory.undo() restores previous snapshot
  (all replacements reverted atomically)
```

---

### Bracket Highlight Flow (BR-1, BR-6)

```
User moves cursor to offset 8 (after '(' in "SELECT (a)")
         │
         ▼
SqlCodeEditor recomposes (cursor change triggers recomposition)
         │
         ▼
BracketMatcher.findBracketPairAtCursor(tokens, cursorOffset=8)
  [tokenizer already ran on 300ms debounce, tokens cached]
         │
         ▼
Returns Pair(7, 12)  [offsets of '(' and ')']
         │
         ▼
BracketHighlightTransformation.filter()
  applies SpanStyle(background=outlineVariant) to offsets 7 and 12
         │
         ▼
CompositeVisualTransformation chains:
  syntax → brackets → matches
         │
         ▼
BasicTextField renders with bracket highlight
         │
         ▼
User moves cursor to offset 20
         │
         ▼
BracketMatcher.findBracketPairAtCursor(tokens, 20) returns null
  (cursor not adjacent to bracket)
         │
         ▼
BracketHighlightTransformation returns original AnnotatedString
  (highlight disappears)
```

---

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/editor/BracketMatcher.kt` | Create | Pure token-walk bracket pairing engine. Exports `findMatchingBracket()`, `findBracketPairAtCursor()`. |
| `domain/editor/FindReplaceEngine.kt` | Create | Pure regex-based find/replace. Exports `findAll()`, `replaceOne()`, `replaceAll()`. |
| `domain/editor/FindOptions.kt` | Create | Data class for `matchCase`, `wholeWord`, `useRegex`. |
| `domain/editor/MultiCursorEngine.kt` | Create | Pure multi-cursor operations. Exports `addCursorBelow()`, `addCursorAbove()`, `findNextOccurrence()`, `selectWordAtOffset()`. |
| `domain/editor/ShortcutAction.kt` | Modify | Add 6 new sealed objects: `Find`, `Replace`, `JumpToMatchingBracket`, `AddCursorBelow`, `AddCursorAbove`, `SelectNextOccurrence`. |
| `domain/editor/EditorShortcuts.kt` | Modify | Add 6 key bindings in `mapKeyEvent()`. |
| `domain/editor/EditorSnapshot.kt` | Modify | Add `cursorSelections: List<TextRange>?` field. Keep `cursorPositions: List<Int>` for backward compat. |
| `domain/editor/EditorHistory.kt` | Modify | Update `push()` to populate both `cursorPositions` and `cursorSelections`. Update `undo()`/`redo()` to prefer `cursorSelections`. |
| `ui/screens/queryeditor/components/FindReplaceBar.kt` | Create | Material 3 Composable for find/replace UI. Adapts to `WindowSizeClass`. |
| `ui/screens/queryeditor/components/CompositeVisualTransformation.kt` | Create | Chains `VisualTransformation` layers. |
| `ui/screens/queryeditor/components/BracketHighlightTransformation.kt` | Create | `VisualTransformation` layer for bracket-pair highlight. |
| `ui/screens/queryeditor/components/MatchOverlayTransformation.kt` | Create | `VisualTransformation` layer for find-match highlights. |
| `ui/screens/queryeditor/components/SqlCodeEditor.kt` | Modify | Integrate `CompositeVisualTransformation`, auto-close hook in `handleValueChange()`, route 6 new shortcuts. |
| `ui/screens/queryeditor/components/SqlHighlightTransformation.kt` | No change | Already supports chaining (returns `TransformedText` with Identity mapping). |
| `ui/screens/queryeditor/QueryEditorScreen.kt` | Modify | Host `FindReplaceBar` above editor. Migrate `cursorPositions: MutableList<Int>` to `cursorSelections: MutableState<List<TextRange>>`. |
| `ui/screens/queryeditor/QueryEditorViewModel.kt` | Modify | Add `FindReplaceViewModel` as nested property. Add handlers for 6 new shortcuts. Expose `showSnackbar()`. |
| `res/values/strings.xml` | Modify | Add 15 new keys: `find`, `replace`, `regex`, `match_case`, `whole_word`, `replace_all`, `no_more_occurrences`, `prev_match`, `next_match`, `close`, `invalid_regex`, `matches_capped`, etc. |
| `res/values-es/strings.xml` (+ 9 more locales) | Modify | Translate all 15 new keys. |
| `app/src/test/.../domain/editor/BracketMatcherTest.kt` | Create | JVM unit tests for bracket pairing (nested, unbalanced, all six pairs). |
| `app/src/test/.../domain/editor/FindReplaceEngineTest.kt` | Create | JVM unit tests for find/replace (regex, case, whole-word, 1000-match cap). |
| `app/src/test/.../domain/editor/MultiCursorEngineTest.kt` | Create | JVM unit tests for multi-cursor operations. |
| `app/src/test/.../queryeditor/components/CompositeVisualTransformationTest.kt` | Create | JVM unit tests for transformation chaining. |
| `app/src/androidTest/.../queryeditor/FindReplaceBarTest.kt` | Create | Compose UI tests for find bar (3 widths, toggles, navigation). |
| `app/src/androidTest/.../queryeditor/SqlCodeEditorBracketHighlightTest.kt` | Create | Compose UI tests for bracket highlight (visible, updates on cursor move). |
| `app/src/androidTest/.../queryeditor/SqlCodeEditorAutoCloseTest.kt` | Create | Compose UI tests for auto-close (6 pairs, suppressed in strings/comments, paste). |
| `app/src/androidTest/.../queryeditor/QueryEditorScreenAdvancedTest.kt` | Create | E2E tests for multi-cursor (Ctrl+Alt+Down/Up, Ctrl+D, undo/redo restores cursors). |

**Total estimated changes**: ~1300 LOC (impl + test) per proposal.

---

## Interfaces / Contracts

### BracketMatcher Output

```kotlin
// Returns null if no match, else offset of matching bracket
fun findMatchingBracket(tokens: List<SqlToken>, cursorOffset: Int): Int?

// Returns null if no pair at cursor, else (openOffset, closeOffset)
fun findBracketPairAtCursor(tokens: List<SqlToken>, cursorOffset: Int): Pair<Int, Int>?
```

### FindReplaceEngine Input/Output

```kotlin
data class FindOptions(
    val matchCase: Boolean = false,
    val wholeWord: Boolean = false,
    val useRegex: Boolean = false
)

data class MatchResult(
    val matches: List<TextRange>,
    val capped: Boolean // true if > 1000 matches
)

// Throws PatternSyntaxException if useRegex=true and query invalid
fun findAll(text: String, query: String, options: FindOptions): MatchResult
```

### EditorSnapshot Schema (after migration)

```kotlin
data class EditorSnapshot(
    val text: String,
    val selection: TextRange,
    val cursorPositions: List<Int>,           // backward compat
    val cursorSelections: List<TextRange>?,   // new field (MC-8)
    val timestamp: Long
)
```

### ShortcutAction Extension

```kotlin
sealed interface ShortcutAction {
    // ... existing entries ...
    data object Find : ShortcutAction
    data object Replace : ShortcutAction
    data object JumpToMatchingBracket : ShortcutAction
    data object AddCursorBelow : ShortcutAction
    data object AddCursorAbove : ShortcutAction
    data object SelectNextOccurrence : ShortcutAction
}
```

---

## Testing Strategy

### Unit Tests (JVM, app/src/test/)

**BracketMatcherTest** (domain/editor/)
- Nested brackets: `SELECT (a + (b))` — verify symmetry
- Unbalanced: `SELECT (a + b` — returns null
- Inside STRING: `'(abc'` — returns null
- All six pairs: `()`, `[]`, `{}`, `''`, `""`, `` ` ` ``
- Property-based: `findMatchingBracket(open) == close AND findMatchingBracket(close) == open`

**FindReplaceEngineTest** (domain/editor/)
- Case-insensitive: `findAll("Select select", "select", matchCase=false)` → 2 matches
- Whole word: `findAll("id user_id", "id", wholeWord=true)` → 1 match
- Regex: `findAll("a1 b2", "[a-z]\\d", useRegex=true)` → 2 matches
- Replace with groups: `replaceAll("id1", "id(\\d)", "ID$1")` → `"ID1"`
- 1000-match cap: synthetic 2000-match text → `capped=true`
- Invalid regex: throws `PatternSyntaxException`

**MultiCursorEngineTest** (domain/editor/)
- `addCursorBelow`: column preserved, clamped, null at last line
- `findNextOccurrence`: case-sensitive, returns null when exhausted
- `selectWordAtOffset`: IDENTIFIER boundaries, collapses for non-words

**CompositeVisualTransformationTest** (queryeditor/components/)
- Two layers with different `SpanStyle` → both styles present in output
- Three layers (syntax + brackets + matches) → correct order
- Property: `OffsetMapping.Identity` preserved when all layers use Identity

**Coverage target**: 80% on all new domain engines (per `.atl/standards/testing.md`).

---

### Integration Tests (androidTest, app/src/androidTest/)

**FindReplaceBarTest** (queryeditor/)
- Compose UI test: `setContent { FindReplaceBar(state, onClose) }`
- Test three widths: 360dp (Compact), 600dp (Medium), 840dp (Expanded)
- Verify toggles visible on Medium+, hidden behind overflow on Compact
- Verify `onClose` callback on `×` button
- Verify match counter updates reactively
- Accessibility: TalkBack announces counter changes

**SqlCodeEditorBracketHighlightTest** (queryeditor/)
- Render editor with buffer `(a)(b)`, cursor at offset 1
- Assert `AnnotatedString` has `SpanStyle(background=outlineVariant)` at offsets 0 and 2
- Move cursor to offset 4
- Assert highlight shifted to offsets 3 and 5

**SqlCodeEditorAutoCloseTest** (queryeditor/)
- Type `(` in empty editor → assert buffer = `()`, cursor at offset 1
- Type `(` inside string `'abc'` → assert buffer = `'abc('`, cursor at offset 5 (no auto-close)
- Paste `(foo)` → assert buffer = `(foo)`, no extra closer
- Test all six pairs: `()`, `[]`, `{}`, `''`, `""`, `` ` ` ``

**QueryEditorScreenAdvancedTest** (queryeditor/)
- Multi-cursor E2E:
  - Press `Ctrl+Alt+Down` → assert `cursorSelections.size == 2`
  - Press `Ctrl+D` on word "user_id" → assert next occurrence selected
  - Press `Ctrl+D` when no more → assert snackbar visible
  - Type text with 3 cursors → press `Ctrl+Z` → assert text AND cursors restored (MC-8)
- Find E2E:
  - Press `Ctrl+F` → assert find bar visible, input focused
  - Type "SELECT" → assert highlights visible, counter shows `n/m`
  - Press `Enter` → assert current match scrolled into view
  - Press `Esc` → assert bar closed, highlights cleared

---

### Performance Tests (JVM benchmark, app/src/test/)

**FindReplaceEnginePerfTest** (domain/editor/)
- Generate 5000-line SQL buffer (~150 KB)
- Query: `SELECT` (expect ~200 matches)
- Measure `findAll()` execution time on `Dispatchers.Default`
- Assert: p95 < 500ms over 20 runs (per NFR-1)

---

### Manual Testing (Smoke Test)

Before PR merge, manually verify on physical Android 10+ device:
- `Ctrl+F` opens find bar, typing highlights matches, Enter navigates, Esc closes
- `Ctrl+H` shows replace row, Replace All works, Ctrl+Z reverts atomically
- `Ctrl+Shift+\` jumps to matching bracket
- `Ctrl+Alt+Down` / `Ctrl+Alt+Up` add cursors, column preserved
- `Ctrl+D` selects next occurrence, stops at last with snackbar
- Bracket highlight visible when cursor adjacent to `(`/`)`/etc
- Auto-close works for `(`, `'`, `"`, `` ` `` (not inside strings)
- All strings localized (switch device language to Spanish, verify UI)

---

## Migration / Rollout

### EditorSnapshot Migration

**Phase 1** (single commit, PR #6):
- Modify `EditorSnapshot.kt`: add `cursorSelections: List<TextRange>? = null`
- Modify `EditorHistory.kt`:
  - `push()`: populate both `cursorPositions` (backward compat) and `cursorSelections`
  - `undo()`/`redo()`: restore `cursorSelections ?: cursorPositions.map { TextRange(it) }`
- Run existing tests → all pass (use `cursorPositions` path)

**Phase 2** (subsequent commits, PR #6):
- Implement multi-cursor shortcuts (Ctrl+Alt+Down/Up, Ctrl+D)
- Update callers to populate `cursorSelections`
- Add MC-8 tests (undo/redo restores cursor state)

**Phase 3** (v1.3, future):
- Deprecate `cursorPositions` field with `@Deprecated` annotation
- Make `cursorSelections` non-nullable
- Remove `cursorPositions` (breaking change, requires schema migration in DataStore if we persist editor state)

---

### Feature Flags

No runtime feature flags in v1.2 (all features always ON). If rollback needed, revert the PR. For v1.3, consider adding user-facing settings:
- "Enable auto-close brackets" (default: ON)
- "Enable multi-cursor shortcuts" (default: ON)

---

## Verification Checklist

Before moving to tasks phase:

- [ ] **BracketMatcher handles nested brackets correctly**: Unit test `BracketMatcherTest` covers `SELECT (a + (b + (c)))` with depth 3
- [ ] **Find bar renders on Compact width without overflow**: `FindReplaceBarTest` passes for 360dp width
- [ ] **Ctrl+D stops at last occurrence (no wrap)**: `MultiCursorEngineTest.findNextOccurrence` returns null when exhausted; `QueryEditorScreenAdvancedTest` asserts snackbar appears
- [ ] **Replace All pushes single undo entry**: `FindReplaceEngineTest` calls `replaceAll()`, mocks `EditorHistory.push()`, asserts called exactly once
- [ ] **VisualTransformation chaining doesn't break syntax colors**: `CompositeVisualTransformationTest` applies syntax layer (KEYWORD=blue) + match layer (background=yellow) → output has both styles
- [ ] **80% coverage threshold met**: `./gradlew testDebugUnitTestCoverage` reports ≥80% on `domain/editor/BracketMatcher`, `FindReplaceEngine`, `MultiCursorEngine`
- [ ] **EditorSnapshot migration is backward-compatible**: Existing `EditorHistoryTest` passes without modifications
- [ ] **All new strings present in 10 locales**: CI lint check `./gradlew lintDebug` passes (no missing translation warnings)
- [ ] **No new external dependencies**: `app/build.gradle.kts` unchanged (only AndroidX Compose BOM `2024.02.00`, already present)

---

## Open Questions

**Q1**: Should bracket highlight have a fade-in/fade-out animation (150ms per proposal "animado"), or instant on/off?
- **Recommendation**: Start with instant (simpler implementation), add animation in follow-up commit if UX feedback requests it. Animation requires `animateColorAsState` in `BracketHighlightTransformation`, which couples domain logic to Compose runtime.

**Q2**: For auto-close, should backspace at empty pair remove both characters (e.g., `|()` → backspace → `|`), or just the opener?
- **Recommendation**: Remove both (VS Code / IntelliJ behavior). Requires hooking backspace in `handleValueChange()`. Covered by BR-3 wording "backspace at empty pair removes both" (from proposal).

**Q3**: Should Ctrl+F pre-populate with selected text (if any), or start with empty query?
- **Recommendation**: Pre-populate (standard IDE behavior). On `Find` shortcut, if `selection.length > 0`, call `setQuery(text.substring(selection))`.

**Q4**: Should Find bar persist state across screen navigations (e.g., user opens query editor → opens find → navigates away → returns), or reset?
- **Recommendation**: Reset (simpler state management). FindReplaceViewModel lives in `QueryEditorViewModel`, which is screen-scoped. Persisting would require SavedStateHandle + DataStore. Defer to v1.3 if users request it.

---

## Risk Assessment

### New Risks (not in proposal)

**RISK-NEW-1**: Auto-close 20-char tokenization window edge case
- **Scenario**: User has a 50-char string literal, cursor at char 30, types `(` — the 20-char window misses the opening `'` at char 0, so we think we're outside a STRING and auto-close.
- **Severity**: Low (failure mode: extra `)` inserted, user backspaces)
- **Mitigation**: Expand window to 100 chars (still < 5ms). If still fails, acceptable — auto-close is a convenience, not correctness-critical.

**RISK-NEW-2**: VisualTransformation span merge performance on 1000 matches
- **Scenario**: 1000 find matches in a 5000-line file → `AnnotatedString.Builder.addStyle()` called 1000 times → composition lag?
- **Severity**: Medium (UX: editor feels sluggish when find bar open)
- **Mitigation**: Benchmark in `FindReplaceEnginePerfTest`. If > 16ms (one frame at 60 FPS), reduce match cap to 500 or apply highlights only to visible lines (requires LazyColumn, deferred to v1.3).

**RISK-NEW-3**: Ctrl+D multi-cursor state explosion
- **Scenario**: User presses Ctrl+D 100 times on a common word like `a` — `cursorSelections` list has 100 entries → rendering/input lag?
- **Severity**: Low (user-induced edge case)
- **Mitigation**: Cap `cursorSelections` at 100. On 101st Ctrl+D, show snackbar "Max 100 cursors reached". Document in spec (out of scope for v1.2, defer to v1.3).

---

## Success Metrics

- **Bracket matching**: `BracketMatcherTest` passes with 100% coverage of all six pairs + nested + unbalanced
- **Find & Replace**: `FindReplaceEngineTest` + `FindReplaceBarTest` pass; manual test confirms Ctrl+F → type → Enter → navigate → Esc works
- **Multi-cursor**: `MultiCursorEngineTest` + `QueryEditorScreenAdvancedTest` pass; manual test confirms Ctrl+D stops at last occurrence with snackbar
- **Performance**: `FindReplaceEnginePerfTest` shows p95 < 500ms for 5000-line find
- **Accessibility**: TalkBack announces match counter changes (verified in manual test)
- **Localization**: All 10 locales have translations (CI lint passes)
- **Coverage**: Overall coverage ≥80% on new domain engines (Kover report)

**Definition of Done**: All success criteria from spec §6 met, all verification checklist items checked, all tests green, PR approved by 2 reviewers per Gentle AI PR review standards.
