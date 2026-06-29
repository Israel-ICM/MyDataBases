# Tasks: Editor Advanced Features v1.2

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1300 total across 3 PRs |
| Max PR size | 600 LOC (PR #5) |
| 400-line budget risk | Medium (PR #5 exceeds 400) |
| 800-line budget risk | Low (all PRs under 800) |
| Chained PRs recommended | Yes (3 sequential PRs) |
| Delivery strategy | ask-always |
| Chain strategy | sequential-to-main (not stacked) |

**Guard contract (literal match required):**
```
Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: sequential-to-main
400-line budget risk: Medium
```

### Rationale

- **PR #4** (400 LOC): Smallest, establishes `CompositeVisualTransformation` pattern
- **PR #5** (600 LOC): Largest but under 800-line budget; uses proven VisualTransformation chaining
- **PR #6** (300 LOC): Includes risky `EditorSnapshot` migration, benefits from stable test coverage

All PRs fit the 800-line review budget. User confirmed 3-PR split. No additional approval needed before apply.

---

## PR #4: Bracket Matching + Auto-close (~400 LOC)

**Budget**: 800 lines | **Estimate**: ~400 LOC | **Status**: ✅ SAFE

**Goal**: Ship bracket pair highlighting, jump-to-match shortcut, and auto-close for six pairs. Establishes `CompositeVisualTransformation` pattern reused by PR #5.

**Verification**: 8 unit scenarios (BracketMatcher), 4 E2E scenarios (bracket highlight), 5 E2E scenarios (auto-close)

---

### Phase 4.1: Domain — BracketMatcher Engine (Pure JVM)

**Estimated**: ~80 LOC impl + 120 LOC tests

- [x] **4.1.1 — TDD RED**: Write `BracketMatcherTest::findMatchingBracket_openParen_returnsCloseParen()` asserting open `(` at offset 7 → close `)` at offset 12 for buffer `"SELECT (a + b)"` — FAIL
- [x] **4.1.2 — TDD GREEN**: Create `domain/editor/BracketMatcher.kt` with `fun findMatchingBracket(tokens: List<SqlToken>, offset: Int): Int?` returning `null` — still FAIL
- [x] **4.1.3 — TDD GREEN**: Implement forward token walk for `(` → `)` matching with depth counter, skip non-PUNCTUATION tokens — PASS
- [x] **4.1.4 — TDD RED**: Write `BracketMatcherTest::findMatchingBracket_closeParen_returnsOpenParen()` asserting reverse direction (close `)` at offset 12 → open `(` at offset 7) — FAIL
- [x] **4.1.5 — TDD GREEN**: Implement backward token walk for close brackets — PASS
- [x] **4.1.6 — TDD RED**: Write `BracketMatcherTest::findMatchingBracket_nested_correctDepth()` asserting `"SELECT ((a + b))"` cursor at offset 7 (first `(`) → offset 16 (last `)`) — FAIL
- [x] **4.1.7 — TDD GREEN**: Fix depth counter to handle nested pairs correctly — PASS
- [x] **4.1.8 — TDD RED**: Write `BracketMatcherTest::findMatchingBracket_insideString_returnsNull()` asserting cursor at `(` inside `"SELECT '(abc'"` → `null` (BR-5) — FAIL
- [x] **4.1.9 — TDD GREEN**: Add token type filter — skip brackets inside STRING or COMMENT tokens — PASS
- [x] **4.1.10 — TDD RED**: Write `BracketMatcherTest::findMatchingBracket_unbalanced_returnsNull()` asserting `"SELECT (a + b"` (no closer) → `null` (BR-7) — FAIL
- [x] **4.1.11 — TDD GREEN**: Return `null` when token walk exhausts list without finding match — PASS
- [x] **4.1.12 — TDD RED**: Write `BracketMatcherTest::findMatchingBracket_allSixPairs_symmetric()` asserting all pairs `()[]{}''""` ` ` `` work in both directions — FAIL
- [x] **4.1.13 — TDD GREEN**: Extend matching logic to handle `[]`, `{}`, `'`, `"`, `` ` `` pairs — PASS
- [x] **4.1.14 — REFACTOR**: Extract helper functions: `isOpenBracket(char)`, `getMatchingBracket(char)`, `skipTokenType(token, types)`

---

### Phase 4.2: ViewModel — Bracket Highlight State

**Estimated**: ~40 LOC impl + 30 LOC tests

- [x] **4.2.1 — TDD RED**: Write `QueryEditorViewModelTest::bracketPairAtCursor_adjacent_emitsPair()` asserting cursor at offset 7 (after `(`) emits `Pair(7, 12)` — FAIL
- [x] **4.2.2 — TDD GREEN**: Add `val bracketPairAtCursor: StateFlow<Pair<Int, Int>?>` to `QueryEditorViewModel`, compute via `BracketMatcher.findBracketPairAtCursor(tokens, cursorOffset)` — PASS
- [x] **4.2.3 — TDD RED**: Write `QueryEditorViewModelTest::bracketPairAtCursor_notAdjacent_emitsNull()` asserting cursor not at bracket → `null` — FAIL
- [x] **4.2.4 — TDD GREEN**: Update bracket pair computation to return `null` when cursor not adjacent to bracket — PASS
- [x] **4.2.5 — REFACTOR**: Hook bracket pair computation to existing 300ms debounced tokenizer stream (reuse existing flow)

---

### Phase 4.3: UI — CompositeVisualTransformation + Bracket Overlay

**Estimated**: ~60 LOC impl + 40 LOC tests

- [x] **4.3.1 — TDD RED**: Write `CompositeVisualTransformationTest::filter_twoLayers_mergesSpanStyles()` asserting layer1 (blue text) + layer2 (yellow bg) → output has both styles — FAIL
- [x] **4.3.2 — TDD GREEN**: Create `components/CompositeVisualTransformation.kt` with `filter()` chaining layers sequentially, merge `AnnotatedString` spans — PASS
- [x] **4.3.3 — TDD RED**: Write `CompositeVisualTransformationTest::offsetMapping_allIdentity_returnsIdentity()` asserting composite returns `OffsetMapping.Identity` when all layers use Identity — FAIL
- [x] **4.3.4 — TDD GREEN**: Implement `OffsetMapping` delegation (return Identity if all layers are Identity) — PASS
- [x] **4.3.5 — TDD RED**: Write `BracketHighlightTransformationTest::filter_cursorAdjacentToBracket_highlightsBothOffsets()` asserting `SpanStyle(background=outlineVariant)` at offsets 7 and 12 — FAIL
- [x] **4.3.6 — TDD GREEN**: Create `components/BracketHighlightTransformation.kt` receiving `bracketPair: Pair<Int,Int>?`, apply `SpanStyle` to both offsets — PASS
- [ ] **4.3.7 — TDD GREEN**: Integrate `CompositeVisualTransformation(syntaxTransform, bracketTransform)` into `SqlCodeEditor.kt` — PASS (no new test, existing editor tests validate rendering)
- [ ] **4.3.8 — REFACTOR**: Extract `AnnotatedString.Builder.addStyleAtRange(style, range)` helper for reuse

---

### Phase 4.4: UI — Auto-close in handleValueChange

**Estimated**: ~40 LOC impl + 60 LOC tests

- [ ] **4.4.1 — TDD RED**: Write `SqlCodeEditorAutoCloseTest::typeOpenParen_notInString_insertsCloser()` asserting typing `(` in empty buffer → `"()"` with cursor at offset 1 (BR-3.1) — FAIL
- [ ] **4.4.2 — TDD GREEN**: Add `handleAutoClose()` in `SqlCodeEditor.kt` detecting single-char insertion of `(`, insert `)`, reposition cursor — PASS
- [ ] **4.4.3 — TDD RED**: Write `SqlCodeEditorAutoCloseTest::typeOpenParen_insideString_noAutoClose()` asserting typing `(` inside `"SELECT 'abc'"` → `"SELECT 'abc('"`, cursor at offset 11 (BR-5.1) — FAIL
- [ ] **4.4.4 — TDD GREEN**: Tokenize 20-char window around cursor, check if cursor inside STRING or COMMENT token, suppress auto-close if true — PASS
- [ ] **4.4.5 — TDD RED**: Write `SqlCodeEditorAutoCloseTest::paste_multiCharInsertion_noAutoClose()` asserting paste `"(foo)"` → `"(foo)"` exactly (BR-5.3) — FAIL
- [ ] **4.4.6 — TDD GREEN**: Detect multi-char insertion (`newText.length - oldText.length > 1`), skip auto-close — PASS
- [ ] **4.4.7 — TDD RED**: Write `SqlCodeEditorAutoCloseTest::allSixPairs_autoClose()` asserting `()[]{}''""` ` ` `` all insert closers (BR-4.1) — FAIL
- [ ] **4.4.8 — TDD GREEN**: Extend `handleAutoClose()` to recognize all six opener chars — PASS
- [ ] **4.4.9 — TDD RED**: Write `SqlCodeEditorAutoCloseTest::backspaceAtEmptyPair_removesBoth()` asserting `"(|)"` + backspace → `"|"` — FAIL
- [ ] **4.4.10 — TDD GREEN**: Hook backspace detection in `handleValueChange()`, if deletion at `(|)` pattern, remove both chars — PASS
- [ ] **4.4.11 — REFACTOR**: Extract `isInsideStringOrComment(tokens, offset): Boolean` helper, expand window to 100 chars for edge case safety

---

### Phase 4.5: Shortcuts — JumpToMatchingBracket

**Estimated**: ~20 LOC impl + 20 LOC tests

- [ ] **4.5.1 — TDD RED**: Write `EditorShortcutsTest::mapKeyEvent_ctrlShiftBackslash_mapsToJumpToMatchingBracket()` asserting `Ctrl+Shift+\` → `ShortcutAction.JumpToMatchingBracket` — FAIL
- [ ] **4.5.2 — TDD GREEN**: Add `JumpToMatchingBracket` to `ShortcutAction` sealed interface in `domain/editor/ShortcutAction.kt` — PASS
- [ ] **4.5.3 — TDD GREEN**: Add key binding in `EditorShortcuts.mapKeyEvent()` mapping `Ctrl+Shift+\` → `JumpToMatchingBracket` — PASS
- [ ] **4.5.4 — TDD RED**: Write `SqlCodeEditorTest::jumpToMatchingBracket_cursorAdjacentToOpen_jumpsToClose()` asserting cursor at offset 7 `(` jumps to offset 12 `)` (BR-2.1) — FAIL
- [ ] **4.5.5 — TDD GREEN**: Add handler in `SqlCodeEditor.onShortcut()` for `JumpToMatchingBracket`, call `BracketMatcher.findMatchingBracket()`, update `selection = TextRange(target)` — PASS
- [ ] **4.5.6 — TDD RED**: Write `SqlCodeEditorTest::jumpToMatchingBracket_noMatch_noOp()` asserting unbalanced source stays at same offset (BR-7.1) — FAIL
- [ ] **4.5.7 — TDD GREEN**: Handle `null` return from `findMatchingBracket()` gracefully (no cursor move) — PASS

---

### Phase 4.6: i18n — Localization Strings

**Estimated**: ~40 lines (4 keys × 10 locales in strings.xml)

- [ ] **4.6.1**: Add `<string name="jump_to_matching_bracket">Jump to matching bracket</string>` to `res/values/strings.xml` (English)
- [ ] **4.6.2**: Add `<string name="bracket_highlight">Bracket highlight</string>` to `res/values/strings.xml`
- [ ] **4.6.3**: Add `<string name="auto_close_brackets">Auto-close brackets</string>` to `res/values/strings.xml`
- [ ] **4.6.4**: Add `<string name="backspace_removes_pair">Backspace removes empty pair</string>` to `res/values/strings.xml`
- [ ] **4.6.5**: Translate all 4 keys to Spanish (`values-es/`), Portuguese (`values-pt/`), French (`values-fr/`), German (`values-de/`), Italian (`values-it/`), Japanese (`values-ja/`), Korean (`values-ko/`), Chinese (`values-zh-rCN/`), Russian (`values-ru/`)

---

### Phase 4.7: E2E Verification

**Estimated**: ~60 LOC tests

- [ ] **4.7.1 — TDD RED**: Write `SqlCodeEditorBracketHighlightTest::cursorAdjacentToOpenParen_highlightsMatchingPair()` (Compose UI test) asserting `AnnotatedString` has `outlineVariant` background at both offsets (BR-1.1) — FAIL
- [ ] **4.7.2 — TDD GREEN**: Fix any UI integration issues — PASS
- [ ] **4.7.3 — TDD RED**: Write `SqlCodeEditorBracketHighlightTest::cursorMovesAwayFromBracket_highlightDisappears()` asserting highlight removed when cursor not adjacent (BR-6.1) — FAIL
- [ ] **4.7.4 — TDD GREEN**: Verify reactive state updates correctly — PASS

---

**PR #4 Total Tasks**: 56 tasks | **Estimated LOC**: ~400 (impl + tests)

---

## PR #5: Find & Replace (~600 LOC)

**Budget**: 800 lines | **Estimate**: ~600 LOC | **Status**: ✅ SAFE (under budget)

**Goal**: Ship inline find/replace bar with regex/case/whole-word toggles, match highlighting, navigation, replace-one/replace-all. Reuses `CompositeVisualTransformation` from PR #4.

**Verification**: 12 unit scenarios (FindReplaceEngine), 8 E2E scenarios (FindReplaceBar), 4 E2E scenarios (match highlight), 1 perf test (NFR-1)

**Dependencies**: PR #4 merged (`CompositeVisualTransformation` available)

---

### Phase 5.1: Domain — FindReplaceEngine (Pure JVM)

**Estimated**: ~100 LOC impl + 150 LOC tests

- [ ] **5.1.1 — TDD RED**: Write `FindReplaceEngineTest::findAll_caseInsensitive_findsAllMatches()` asserting query `"select"` with `matchCase=false` finds 3 matches in `"Select select SELECT"` (FR-6.1) — FAIL
- [ ] **5.1.2 — TDD GREEN**: Create `domain/editor/FindReplaceEngine.kt`, create `data class FindOptions(matchCase, wholeWord, useRegex)`, implement `findAll(text, query, options): MatchResult` escaping query as literal regex, apply `RegexOption.IGNORE_CASE` — PASS
- [ ] **5.1.3 — TDD RED**: Write `FindReplaceEngineTest::findAll_matchCase_onlyExactCase()` asserting `matchCase=true` finds 1 match in `"Select select SELECT"` (FR-6.2) — FAIL
- [ ] **5.1.4 — TDD GREEN**: Implement case-sensitive path (omit `IGNORE_CASE` option) — PASS
- [ ] **5.1.5 — TDD RED**: Write `FindReplaceEngineTest::findAll_wholeWord_filtersPartialMatches()` asserting query `"id"` with `wholeWord=true` finds 1 match in `"id, user_id, id_card"` (FR-7.1) — FAIL
- [ ] **5.1.6 — TDD GREEN**: Implement whole-word wrapping: `"\\b${Regex.escape(query)}\\b"` — PASS
- [ ] **5.1.7 — TDD RED**: Write `FindReplaceEngineTest::findAll_validRegex_findsMatches()` asserting query `"[a-c]\\d"` with `useRegex=true` finds 3 matches in `"a1 b2 c3"` (FR-8.1) — FAIL
- [ ] **5.1.8 — TDD GREEN**: Implement regex mode: compile query directly as `Regex` when `useRegex=true` (skip escaping) — PASS
- [ ] **5.1.9 — TDD RED**: Write `FindReplaceEngineTest::findAll_invalidRegex_throwsPatternSyntaxException()` asserting query `"[unclosed"` with `useRegex=true` throws exception (FR-8.2) — FAIL
- [ ] **5.1.10 — TDD GREEN**: Let `Regex()` constructor throw `PatternSyntaxException` naturally (no catch) — PASS
- [ ] **5.1.11 — TDD RED**: Write `FindReplaceEngineTest::findAll_over1000Matches_capsAt1000()` asserting synthetic 2000-match text → `MatchResult(matches.size=1000, capped=true)` (ADR-3) — FAIL
- [ ] **5.1.12 — TDD GREEN**: Implement `regex.findAll(text).take(1000).toList()`, detect if iterator has more via `hasNext()`, set `capped=true` — PASS
- [ ] **5.1.13 — TDD RED**: Write `FindReplaceEngineTest::replaceOne_replacesTargetRange()` asserting `replaceOne("SELECT a", TextRange(0,6), "PICK")` → `"PICK a"` (FR-10.1) — FAIL
- [ ] **5.1.14 — TDD GREEN**: Implement `fun replaceOne(text, targetRange, replacement): String` using `text.replaceRange(targetRange.start, targetRange.end, replacement)` — PASS
- [ ] **5.1.15 — TDD RED**: Write `FindReplaceEngineTest::replaceAll_replacesAllMatches()` asserting 5 `"foo"` → 5 `"bar"` (FR-11.1) — FAIL
- [ ] **5.1.16 — TDD GREEN**: Implement `fun replaceAll(text, matches, replacement, useRegex): String` walking matches in reverse order, calling `replaceOne()` for each — PASS
- [ ] **5.1.17 — TDD RED**: Write `FindReplaceEngineTest::replaceAll_regexWithGroups_replacesWithCaptures()` asserting `"id1 id2"` with regex `"id(\\d)"` replacement `"ID$1"` → `"ID1 ID2"` (FR-12.1) — FAIL
- [ ] **5.1.18 — TDD GREEN**: Use `matchResult.value.replace(Regex(query), replacement)` when `useRegex=true` to support capture groups — PASS
- [ ] **5.1.19 — REFACTOR**: Extract `buildRegexPattern(query, options): String` helper consolidating escape/whole-word/case logic

---

### Phase 5.2: ViewModel — FindReplaceState

**Estimated**: ~120 LOC impl + 80 LOC tests

- [ ] **5.2.1 — TDD RED**: Write `FindReplaceViewModelTest::setQuery_emitsMatches()` asserting `setQuery("SELECT")` emits matches via `StateFlow<List<TextRange>>` — FAIL
- [ ] **5.2.2 — TDD GREEN**: Create `FindReplaceViewModel` in `QueryEditorViewModel.kt`, add `val matches: StateFlow<List<TextRange>>`, debounce `setQuery()` 150ms, call `FindReplaceEngine.findAll()` on `Dispatchers.Default` — PASS
- [ ] **5.2.3 — TDD RED**: Write `FindReplaceViewModelTest::navigateNext_wrapsToFirst()` asserting current index 5 of 5 → `navigateNext()` → index 1 (FR-4.2) — FAIL
- [ ] **5.2.4 — TDD GREEN**: Implement `fun navigateNext()` incrementing `currentMatchIndex`, wrap to 1 if at end — PASS
- [ ] **5.2.5 — TDD RED**: Write `FindReplaceViewModelTest::navigatePrevious_wrapsToLast()` asserting index 1 → `navigatePrevious()` → index 5 (FR-4.2) — FAIL
- [ ] **5.2.6 — TDD GREEN**: Implement `fun navigatePrevious()` decrementing index, wrap to `matches.size` if at 1 — PASS
- [ ] **5.2.7 — TDD RED**: Write `FindReplaceViewModelTest::setQuery_invalidRegex_emitsError()` asserting `setQuery("[unclosed")` with `useRegex=true` emits `regexError` state (FR-8.2) — FAIL
- [ ] **5.2.8 — TDD GREEN**: Wrap `findAll()` in `try-catch`, on `PatternSyntaxException` emit localized error string to `val regexError: StateFlow<String?>` — PASS
- [ ] **5.2.9 — TDD RED**: Write `FindReplaceViewModelTest::replaceOne_updatesTextAndAdvances()` asserting `replaceOne()` updates editor text, pushes `EditorHistory` snapshot, advances to next match (FR-10.1) — FAIL
- [ ] **5.2.10 — TDD GREEN**: Implement `fun replaceOne()` calling `FindReplaceEngine.replaceOne()`, update `editorText`, push snapshot, call `navigateNext()` — PASS
- [ ] **5.2.11 — TDD RED**: Write `FindReplaceViewModelTest::replaceAll_pushesOnlyOneSnapshot()` asserting `replaceAll()` calls `EditorHistory.push()` exactly once (FR-11) — FAIL
- [ ] **5.2.12 — TDD GREEN**: Implement `fun replaceAll()` calling `FindReplaceEngine.replaceAll()`, push **single snapshot** before updating text — PASS
- [ ] **5.2.13 — REFACTOR**: Extract `scrollToMatch(index)` helper for FR-15 (scroll current match into view)

---

### Phase 5.3: UI — FindReplaceBar Composable

**Estimated**: ~150 LOC impl + 100 LOC tests

- [ ] **5.3.1 — TDD RED**: Write `FindReplaceBarTest::findBarOpen_inputFocused()` (Compose UI test) asserting find input has focus on open (FR-1.1) — FAIL
- [ ] **5.3.2 — TDD GREEN**: Create `components/FindReplaceBar.kt`, add `TextField` for find input, request focus on composition — PASS
- [ ] **5.3.3 — TDD RED**: Write `FindReplaceBarTest::matchCounter_displaysCurrentAndTotal()` asserting counter shows `"2 / 5"` when `currentMatchIndex=2`, `matches.size=5` (FR-2.1) — FAIL
- [ ] **5.3.4 — TDD GREEN**: Add `Text("${currentMatchIndex} / ${matches.size}")` to bar layout — PASS
- [ ] **5.3.5 — TDD RED**: Write `FindReplaceBarTest::matchCounter_zeroMatches_displaysZero()` asserting counter shows `"0 / 0"` (FR-2.2) — FAIL
- [ ] **5.3.6 — TDD GREEN**: Handle edge case when `matches.isEmpty()`, show `"0 / 0"` — PASS
- [ ] **5.3.7 — TDD RED**: Write `FindReplaceBarTest::compactWidth_togglesCollapsedToOverflow()` asserting 360dp width hides regex/case/word toggles, shows overflow button (FR-13.1) — FAIL
- [ ] **5.3.8 — TDD GREEN**: Use `WindowSizeClass` from `calculateWindowSizeClass()`, if `Compact` wrap toggles in overflow `IconButton` with dropdown menu — PASS
- [ ] **5.3.9 — TDD RED**: Write `FindReplaceBarTest::mediumWidth_togglesVisible()` asserting 600dp width shows all toggles inline — FAIL
- [ ] **5.3.10 — TDD GREEN**: Layout toggles inline when `widthSizeClass >= Medium` — PASS
- [ ] **5.3.11 — TDD RED**: Write `FindReplaceBarTest::ctrlH_showsReplaceRow()` asserting `Ctrl+H` reveals replace input and buttons (FR-9.1) — FAIL
- [ ] **5.3.12 — TDD GREEN**: Add `val mode: StateFlow<FindReplaceMode>` to ViewModel, show second row (replace input + buttons) when `mode == REPLACE` — PASS
- [ ] **5.3.13 — TDD RED**: Write `FindReplaceBarTest::escKey_closesBarAndClearsHighlights()` asserting `Esc` hides bar, clears matches, restores editor focus (FR-5.1) — FAIL
- [ ] **5.3.14 — TDD GREEN**: Add `onPreviewKeyEvent` detecting `Esc`, call `onClose()`, clear matches — PASS
- [ ] **5.3.15 — TDD RED**: Write `FindReplaceBarTest::matchCounter_liveRegion_announcesTalkBack()` asserting counter has `semantics { liveRegion = Polite }` (NFR-2.1) — FAIL
- [ ] **5.3.16 — TDD GREEN**: Add `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` to counter `Text` — PASS
- [ ] **5.3.17 — REFACTOR**: Extract `FindBarRow()` and `ReplaceBarRow()` sub-composables for readability

---

### Phase 5.4: UI — Match Highlight Overlay

**Estimated**: ~40 LOC impl + 40 LOC tests

- [ ] **5.4.1 — TDD RED**: Write `MatchOverlayTransformationTest::filter_currentMatch_usesTertiaryContainer()` asserting current match (index 2) gets `tertiaryContainer` background (FR-14.1) — FAIL
- [ ] **5.4.2 — TDD GREEN**: Create `components/MatchOverlayTransformation.kt` receiving `matches: List<TextRange>`, `currentIndex: Int`, apply `SpanStyle(background=tertiaryContainer)` to `matches[currentIndex]` — PASS
- [ ] **5.4.3 — TDD RED**: Write `MatchOverlayTransformationTest::filter_otherMatches_useMutedBackground()` asserting non-current matches get muted background — FAIL
- [ ] **5.4.4 — TDD GREEN**: Apply `SpanStyle(background=surfaceVariant.copy(alpha=0.5f))` to all other matches — PASS
- [ ] **5.4.5 — TDD GREEN**: Integrate `MatchOverlayTransformation` into `CompositeVisualTransformation(syntax, brackets, matches)` in `SqlCodeEditor.kt` — PASS (existing editor tests validate)
- [ ] **5.4.6 — REFACTOR**: Reuse `AnnotatedString.Builder.addStyleAtRange()` helper from PR #4

---

### Phase 5.5: Shortcuts — Ctrl+F and Ctrl+H

**Estimated**: ~20 LOC impl + 20 LOC tests

- [ ] **5.5.1 — TDD RED**: Write `EditorShortcutsTest::mapKeyEvent_ctrlF_mapsToFind()` asserting `Ctrl+F` → `ShortcutAction.Find` — FAIL
- [ ] **5.5.2 — TDD GREEN**: Add `Find` and `Replace` to `ShortcutAction`, add key bindings in `EditorShortcuts.kt` — PASS
- [ ] **5.5.3 — TDD RED**: Write `SqlCodeEditorTest::ctrlF_opensBarAndPrePopulatesSelection()` asserting `Ctrl+F` with selected text pre-populates find input (design Q3 recommendation) — FAIL
- [ ] **5.5.4 — TDD GREEN**: Add handler for `Find` shortcut in `SqlCodeEditor.onShortcut()`, call `findReplaceViewModel.open(FIND)`, if `selection.length > 0` call `setQuery(text.substring(selection))` — PASS
- [ ] **5.5.5 — TDD GREEN**: Add handler for `Replace` shortcut opening in `REPLACE` mode — PASS

---

### Phase 5.6: i18n — Localization Strings

**Estimated**: ~120 lines (12 keys × 10 locales)

- [ ] **5.6.1**: Add 12 new keys to `res/values/strings.xml` (English): `find`, `replace`, `regex`, `match_case`, `whole_word`, `replace_one`, `replace_all`, `prev_match`, `next_match`, `close`, `invalid_regex`, `matches_capped`
- [ ] **5.6.2**: Translate all 12 keys to 9 non-English locales (`values-es/`, `values-pt/`, `values-fr/`, `values-de/`, `values-it/`, `values-ja/`, `values-ko/`, `values-zh-rCN/`, `values-ru/`)

---

### Phase 5.7: E2E Verification

**Estimated**: ~80 LOC tests

- [ ] **5.7.1 — TDD RED**: Write `QueryEditorScreenFindTest::ctrlF_type_enter_navigates()` (E2E) asserting `Ctrl+F` → type `"SELECT"` → `Enter` cycles through matches — FAIL
- [ ] **5.7.2 — TDD GREEN**: Fix any integration issues — PASS
- [ ] **5.7.3 — TDD RED**: Write `QueryEditorScreenFindTest::replaceAll_ctrlZ_revertsAtomically()` asserting replace-all is single undo entry (FR-11.1) — FAIL
- [ ] **5.7.4 — TDD GREEN**: Verify `EditorHistory` integration — PASS
- [ ] **5.7.5 — TDD RED**: Write `QueryEditorScreenFindTest::scrollToMatch_matchOutsideViewport_scrollsIntoView()` asserting FR-15 behavior — FAIL
- [ ] **5.7.6 — TDD GREEN**: Implement scroll logic in `scrollToMatch()` helper — PASS

---

### Phase 5.8: Performance Verification

**Estimated**: ~30 LOC test

- [ ] **5.8.1 — TDD RED**: Write `FindReplaceEnginePerfTest::findAll_5000Lines_under500ms()` (JVM benchmark) asserting p95 < 500ms over 20 runs (NFR-1.1) — FAIL
- [ ] **5.8.2 — TDD GREEN**: If failing, optimize `findAll()` (early exit at 1000 matches, use `Sequence` instead of `List`) — PASS

---

**PR #5 Total Tasks**: 62 tasks | **Estimated LOC**: ~600 (impl + tests)

---

## PR #6: Multi-cursor Improvements + EditorSnapshot Migration (~300 LOC)

**Budget**: 800 lines | **Estimate**: ~300 LOC | **Status**: ✅ SAFE

**Goal**: Ship `Ctrl+Alt+Down/Up` (add cursor on consecutive line), `Ctrl+D` (select next occurrence), and `EditorSnapshot` schema migration for multi-selection undo/redo.

**Verification**: 6 unit scenarios (MultiCursorEngine), 5 E2E scenarios (multi-cursor), 3 unit scenarios (EditorHistory migration)

**Dependencies**: PR #4-5 merged (tokenizer stable, undo/redo tested)

---

### Phase 6.1: Schema Migration — EditorSnapshot Dual-Field

**Estimated**: ~30 LOC impl + 40 LOC tests

- [ ] **6.1.1 — TDD RED**: Write `EditorSnapshotTest::push_populatesBothCursorFields()` asserting `push(snapshot)` populates `cursorPositions: List<Int>` (backward compat) AND `cursorSelections: List<TextRange>?` (new field) — FAIL
- [ ] **6.1.2 — TDD GREEN**: Modify `domain/editor/EditorSnapshot.kt`, add `val cursorSelections: List<TextRange>? = null` field — PASS
- [ ] **6.1.3 — TDD GREEN**: Modify `EditorHistory.push()`, populate `cursorSelections` from passed selections, populate `cursorPositions` via `.map { it.start }` for backward compat — PASS
- [ ] **6.1.4 — TDD RED**: Write `EditorHistoryTest::undo_prefersCursorSelections()` asserting `undo()` restores from `cursorSelections` when present, falls back to `cursorPositions.map { TextRange(it) }` when `null` (ADR-1) — FAIL
- [ ] **6.1.5 — TDD GREEN**: Modify `EditorHistory.undo()` and `redo()` to restore `cursorSelections ?: cursorPositions.map { TextRange(it) }` — PASS
- [ ] **6.1.6 — TDD GREEN**: Run existing `EditorHistoryTest` suite — all pass (backward compat validated)

---

### Phase 6.2: Domain — MultiCursorEngine (Pure JVM)

**Estimated**: ~60 LOC impl + 90 LOC tests

- [ ] **6.2.1 — TDD RED**: Write `MultiCursorEngineTest::addCursorBelow_columnPreserved()` asserting cursor at line 0 col 4 → `addCursorBelow()` → cursor at line 1 col 4 (MC-1.1) — FAIL
- [ ] **6.2.2 — TDD GREEN**: Create `domain/editor/MultiCursorEngine.kt`, implement `fun addCursorBelow(layout, primarySelection, targetColumn): TextRange?` using `layout.getLineForOffset()`, `layout.getLineStart()`, add `targetColumn` to line start — PASS
- [ ] **6.2.3 — TDD RED**: Write `MultiCursorEngineTest::addCursorBelow_clampsToEOL()` asserting target col 8 on 2-char line → clamps to col 2 (MC-1.2) — FAIL
- [ ] **6.2.4 — TDD GREEN**: Clamp to `min(targetOffset, layout.getLineEnd(line))` — PASS
- [ ] **6.2.5 — TDD RED**: Write `MultiCursorEngineTest::addCursorBelow_lastLine_returnsNull()` asserting no-op at last line (MC-1.3) — FAIL
- [ ] **6.2.6 — TDD GREEN**: Return `null` when `currentLine == layout.lineCount - 1` — PASS
- [ ] **6.2.7 — TDD RED**: Write `MultiCursorEngineTest::addCursorAbove_columnPreserved()` asserting upward mirror (MC-2.1) — FAIL
- [ ] **6.2.8 — TDD GREEN**: Implement `fun addCursorAbove()` (mirror logic, walk upward) — PASS
- [ ] **6.2.9 — TDD RED**: Write `MultiCursorEngineTest::findNextOccurrence_caseSensitive_findsNext()` asserting `"user_id"` at offset 0 → `findNextOccurrence()` → offset 15 (MC-3.1) — FAIL
- [ ] **6.2.10 — TDD GREEN**: Implement `fun findNextOccurrence(text, selectedText, fromOffset): TextRange?` using `text.indexOf(selectedText, fromOffset)` — PASS
- [ ] **6.2.11 — TDD RED**: Write `MultiCursorEngineTest::findNextOccurrence_noMoreMatches_returnsNull()` asserting exhausted search → `null` (MC-5.1) — FAIL
- [ ] **6.2.12 — TDD GREEN**: Return `null` when `indexOf()` returns `-1` — PASS
- [ ] **6.2.13 — TDD RED**: Write `MultiCursorEngineTest::selectWordAtOffset_insideIdentifier_expandsToBoundaries()` asserting caret inside `"user_id"` → expands to full word (MC-4.1) — FAIL
- [ ] **6.2.14 — TDD GREEN**: Implement `fun selectWordAtOffset(text, offset, tokens): TextRange` finding token at offset, if `IDENTIFIER` return token range, else `TextRange(offset)` — PASS
- [ ] **6.2.15 — REFACTOR**: Extract `getTokenAtOffset(tokens, offset): SqlToken?` helper

---

### Phase 6.3: ViewModel — Multi-cursor State Update

**Estimated**: ~40 LOC impl + 50 LOC tests

- [ ] **6.3.1 — TDD RED**: Write `QueryEditorViewModelTest::handleAddCursorBelow_appendsSelection()` asserting `cursorSelections` grows by 1 — FAIL
- [ ] **6.3.2 — TDD GREEN**: Add `fun handleAddCursorBelow()` in `QueryEditorViewModel`, call `MultiCursorEngine.addCursorBelow()`, append result to `cursorSelections` mutable state — PASS
- [ ] **6.3.3 — TDD RED**: Write `QueryEditorViewModelTest::handleSelectNextOccurrence_firstPress_selectsWord()` asserting collapsed selection → first `Ctrl+D` expands to word (MC-4.1) — FAIL
- [ ] **6.3.4 — TDD GREEN**: Add `fun handleSelectNextOccurrence()`, if `selection.collapsed` call `MultiCursorEngine.selectWordAtOffset()`, update `selection` — PASS
- [ ] **6.3.5 — TDD RED**: Write `QueryEditorViewModelTest::handleSelectNextOccurrence_secondPress_addsNextOccurrence()` asserting word selected → next `Ctrl+D` appends next occurrence (MC-4.2) — FAIL
- [ ] **6.3.6 — TDD GREEN**: If selection not collapsed, call `MultiCursorEngine.findNextOccurrence()`, append to `cursorSelections` — PASS
- [ ] **6.3.7 — TDD RED**: Write `QueryEditorViewModelTest::handleSelectNextOccurrence_noMoreOccurrences_showsSnackbar()` asserting `null` return → snackbar (MC-5.1) — FAIL
- [ ] **6.3.8 — TDD GREEN**: Add `showSnackbar(stringResource(R.string.no_more_occurrences))` on `null` return — PASS
- [ ] **6.3.9 — REFACTOR**: Cache `targetColumn` state for `Ctrl+Alt+Down/Up` (preserve column across multiple presses per ADR design note)

---

### Phase 6.4: UI — Cursor Rendering Update

**Estimated**: ~20 LOC impl

- [ ] **6.4.1 — TDD GREEN**: Modify `SqlCodeEditor.kt` cursor rendering to iterate `cursorSelections: List<TextRange>`, render each as caret (if `collapsed`) or highlighted range (if `length > 0`) — PASS (existing multi-cursor rendering supports `TextRange`, just needs state hookup)
- [ ] **6.4.2 — TDD GREEN**: Ensure all cursors blink in sync (existing behavior, validate with visual inspection) — PASS

---

### Phase 6.5: Shortcuts — Ctrl+Alt+Down/Up, Ctrl+D

**Estimated**: ~30 LOC impl + 30 LOC tests

- [ ] **6.5.1 — TDD RED**: Write `EditorShortcutsTest::mapKeyEvent_ctrlAltDown_mapsToAddCursorBelow()` asserting key binding — FAIL
- [ ] **6.5.2 — TDD GREEN**: Add `AddCursorBelow`, `AddCursorAbove`, `SelectNextOccurrence` to `ShortcutAction`, add 3 key bindings in `EditorShortcuts.kt` — PASS
- [ ] **6.5.3 — TDD RED**: Write `SqlCodeEditorTest::ctrlAltDown_addsCursor()` asserting shortcut handler calls ViewModel — FAIL
- [ ] **6.5.4 — TDD GREEN**: Add handlers in `SqlCodeEditor.onShortcut()` for all 3 shortcuts — PASS

---

### Phase 6.6: i18n — Localization Strings

**Estimated**: ~30 lines (3 keys × 10 locales)

- [ ] **6.6.1**: Add 3 new keys to `res/values/strings.xml`: `add_cursor_below`, `add_cursor_above`, `no_more_occurrences`
- [ ] **6.6.2**: Translate to 9 non-English locales

---

### Phase 6.7: E2E Verification

**Estimated**: ~80 LOC tests

- [ ] **6.7.1 — TDD RED**: Write `QueryEditorScreenMultiCursorTest::ctrlAltDown_preservesColumn()` (E2E) asserting column math correct (MC-1.1) — FAIL
- [ ] **6.7.2 — TDD GREEN**: Fix any integration issues — PASS
- [ ] **6.7.3 — TDD RED**: Write `QueryEditorScreenMultiCursorTest::ctrlD_stopsAtLast_showsSnackbar()` asserting snackbar appears (MC-5.1) — FAIL
- [ ] **6.7.4 — TDD GREEN**: Verify snackbar integration — PASS
- [ ] **6.7.5 — TDD RED**: Write `QueryEditorScreenMultiCursorTest::multiCursor_type_undo_restoresCursors()` asserting `Ctrl+Z` restores text AND cursors (MC-8.1) — FAIL
- [ ] **6.7.6 — TDD GREEN**: Verify `EditorHistory` restores `cursorSelections` correctly — PASS
- [ ] **6.7.7 — TDD RED**: Write `QueryEditorScreenMultiCursorTest::altClick_stillWorks()` asserting pre-existing `Alt+Click` multi-cursor preserved (MC-6.1) — FAIL
- [ ] **6.7.8 — TDD GREEN**: Verify existing `Alt+Click` handler still functional — PASS

---

**PR #6 Total Tasks**: 49 tasks | **Estimated LOC**: ~300 (impl + tests)

---

## Verification Plan

### Unit Tests (JVM, `app/src/test/`)

| Test Suite | Scenarios | Coverage Target |
|------------|-----------|-----------------|
| `BracketMatcherTest` | 8 | ≥80% on `BracketMatcher.kt` |
| `FindReplaceEngineTest` | 12 | ≥80% on `FindReplaceEngine.kt` |
| `MultiCursorEngineTest` | 6 | ≥80% on `MultiCursorEngine.kt` |
| `CompositeVisualTransformationTest` | 4 | ≥80% on `CompositeVisualTransformation.kt` |
| `EditorSnapshotTest` | 3 | Validates backward compat |

**Total**: 33 unit tests

---

### E2E Tests (androidTest, `app/src/androidTest/`)

| Test Suite | Scenarios | Focus |
|------------|-----------|-------|
| `SqlCodeEditorBracketHighlightTest` | 4 | Bracket highlight visible, updates on cursor move |
| `SqlCodeEditorAutoCloseTest` | 5 | Auto-close 6 pairs, suppressed in strings/paste |
| `FindReplaceBarTest` | 8 | Compact/Medium/Expanded widths, toggles, navigation |
| `QueryEditorScreenFindTest` | 4 | Find E2E (Ctrl+F, highlight, navigate, replace-all undo) |
| `QueryEditorScreenMultiCursorTest` | 5 | Multi-cursor E2E (Ctrl+Alt+Down/Up, Ctrl+D, undo restores cursors) |

**Total**: 26 E2E tests

---

### Performance Tests (JVM benchmark)

- `FindReplaceEnginePerfTest::findAll_5000Lines_under500ms()` — NFR-1 validation

---

### Manual Smoke Test Checklist

Before PR merge, verify on physical Android 10+ device:

- [ ] Bracket highlight visible when cursor adjacent to `(`, `)`, `[`, `]`, `{`, `}`
- [ ] `Ctrl+Shift+\` jumps to matching bracket
- [ ] Auto-close inserts closer for `(`, `'`, `"`, `` ` ``; suppressed inside strings/comments; backspace at empty pair removes both
- [ ] `Ctrl+F` opens find bar, typing highlights matches, counter shows `n/m`, Enter/Shift+Enter cycles
- [ ] `Ctrl+H` shows replace row, replace-all works, `Ctrl+Z` reverts atomically
- [ ] Regex/case/whole-word toggles work, invalid regex shows inline error
- [ ] Find bar adapts to Compact width (360dp) — toggles behind overflow
- [ ] `Ctrl+Alt+Down` / `Ctrl+Alt+Up` add cursors on consecutive lines, column preserved
- [ ] `Ctrl+D` selects word first press, adds next occurrence second press, stops at last with snackbar "No more occurrences"
- [ ] Multi-cursor edit + `Ctrl+Z` restores text and cursors atomically
- [ ] All strings localized (test with device language = Spanish, verify UI labels)

---

## Implementation Order Rationale

### Why This PR Sequence?

1. **PR #4 (Brackets)**: Smallest, establishes `CompositeVisualTransformation` pattern that PR #5 reuses for match highlighting. Low risk, validates tokenizer integration.

2. **PR #5 (Find/Replace)**: Largest but under 800-line budget. Chains on PR #4's proven VisualTransformation pattern. Ships high-value search feature. Performance-tested (NFR-1).

3. **PR #6 (Multi-cursor)**: Last because it includes risky `EditorSnapshot` migration (dual-field schema change). Depends on stable tokenizer and undo/redo from PR #4-5. Benefits from solid test coverage established earlier.

**Sequential merges to `main`**: Each PR is independent, no parallel work. User can revert any PR without breaking others.

---

## Dependencies

- **PR #5** depends on **PR #4**: `CompositeVisualTransformation` must exist before `MatchOverlayTransformation` can be chained
- **PR #6** depends on **PR #4-5**: All shortcuts in place (`EditorShortcuts.kt` stable), undo/redo thoroughly tested
- **No external library additions**: All features use existing AndroidX Compose BOM `2024.02.00`, Kotlin stdlib, existing `SqlTokenizer`

---

## Delivery Strategy Confirmation

**Review budget**: 800 lines per PR
**Chained PR strategy**: `ask-always` (user explicitly confirmed 3-PR split in proposal)
**Forecast**: All 3 PRs under 800-line budget ✅

| PR | Estimate | Budget | Status |
|----|----------|--------|--------|
| PR #4 | 400 LOC | 800 | ✅ SAFE |
| PR #5 | 600 LOC | 800 | ✅ SAFE |
| PR #6 | 300 LOC | 800 | ✅ SAFE |

**Action**: Proceed to `sdd-apply` without additional approval. No `size:exception` needed.

---

## Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| **Task count high** (167 total tasks) | Low | Tasks granular due to strict TDD (RED-GREEN-REFACTOR triples count). Each task is 1-2 lines max, completable in < 10 minutes. |
| **VisualTransformation span merge performance on 1000 matches** | Medium | Benchmark in `FindReplaceEnginePerfTest`. If > 16ms, reduce cap to 500 or defer to v1.3. |
| **EditorSnapshot migration breaks existing tests** | Low | Phase 6.1 validates backward compat; existing tests pass before new behavior lands. |
| **Auto-close tokenization window edge case** (50-char string) | Low | Expanded window to 100 chars per ADR-5. Acceptable failure mode: user backspaces extra closer. |
| **Ctrl+D state explosion** (100+ cursors) | Low | Out of scope for v1.2. If user reports lag, cap at 100 cursors in v1.3. |

---

## Next Step

**Recommended**: `sdd-apply` with PR #4 as starting point.

**Entry point**: Phase 4.1 (BracketMatcher domain engine)

**Verification before PR #4 merge**: All 56 tasks complete, 17 tests passing (8 unit + 4 E2E bracket + 5 E2E auto-close), coverage ≥80% on `BracketMatcher.kt`, manual smoke test passed.
