# Tasks: Editor Shortcuts and History

> **PR #1 of 3** — Foundation for editor productivity (undo/redo + 4 core shortcuts)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 230–280 production code + 180–220 test code = ~460 total |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR (pure additive change, minimal integration points) |
| Delivery strategy | ask-always |
| Chain strategy | N/A |

Decision needed before apply: Yes
Chained PRs recommended: No
Chain strategy: N/A
400-line budget risk: Medium

### Rationale
While the total estimate approaches 460 lines, this is a **highly cohesive change** with:
- Pure domain layer (2 new files, JVM-testable, zero coupling)
- Minimal ViewModel integration (3 new methods, 2 StateFlows)
- Focused UI changes (1 composable parameter, 2 toolbar buttons, 1 key handler)
- Additive i18n (6 strings × 2 locales, 8 TODO markers)

Splitting would create artificial boundaries with no independent value. All tests verify the same user story (undo/redo + shortcuts). Risk is **Low** despite line count because:
- No existing behavior modified (additive only)
- Pure functions dominate the diff
- Integration surface is 3 ViewModel methods + 1 composable param
- Rollback = revert merge commit (no migrations, no DataStore, no schema)

## Testing Strategy

| Test Type | Files | Focus | Estimated LOC |
|-----------|-------|-------|---------------|
| **JVM Unit** | `EditorHistoryTest.kt`, `EditorShortcutsTest.kt` | Pure domain logic, coalescing, bounds, mapping | ~130 |
| **Compose UI** | `EditorShortcutsUiTest.kt` | E2E shortcuts, multi-cursor undo, button states | ~90 |
| **Total** | 3 test files | 100% domain coverage, 4 E2E flows | ~220 |

### Coverage Goals
- **Domain**: 100% (pure functions, table-driven tests)
- **ViewModel**: Integration via domain unit tests (EditorHistory exercises ViewModel logic)
- **UI**: 4 critical paths (Run, Save, Undo, Redo) + multi-cursor restoration

---

## Phase 1: Pure Domain Layer (JVM-testable, zero Android deps)

### T1: EditorSnapshot Data Class
**Type**: domain  
**Estimated LOC**: 15 production  
**Dependencies**: None  

**TDD Steps**:
1. **RED**: Write `EditorSnapshotTest.kt` asserting:
   - Snapshot captures `text`, `selection`, `cursorPositions`
   - Two snapshots with identical state are equal
   - `copy()` preserves all fields
2. **GREEN**: Create `com/sphynxs/mydatabases/ui/screens/queryeditor/domain/EditorSnapshot.kt`:
   ```kotlin
   data class EditorSnapshot(
       val text: String,
       val selection: TextRange,
       val cursorPositions: List<Int>
   )
   ```
3. **VERIFY**: `./gradlew test --tests EditorSnapshotTest` → all pass

**Acceptance Criteria**:
- [x] `EditorSnapshot` is a pure data class with no Android imports
- [x] Unit test verifies equality and copy semantics
- [x] Compiles on JVM without Robolectric

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/EditorSnapshot.kt` (new)
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/EditorSnapshotTest.kt` (new)

---

### T2: ShortcutAction Sealed Interface
**Type**: domain  
**Estimated LOC**: 12 production  
**Dependencies**: None  

**TDD Steps**:
1. **RED**: Write `ShortcutActionTest.kt` asserting:
   - All 4 action types are distinct sealed instances
   - Pattern match exhaustiveness compiles
2. **GREEN**: Create `domain/ShortcutAction.kt`:
   ```kotlin
   sealed interface ShortcutAction {
       data object Run : ShortcutAction
       data object Save : ShortcutAction
       data object Undo : ShortcutAction
       data object Redo : ShortcutAction
   }
   ```
3. **VERIFY**: `./gradlew test --tests ShortcutActionTest` → all pass

**Acceptance Criteria**:
- [x] Sealed interface with 4 `data object` implementations
- [x] Unit test verifies exhaustive `when` branches compile
- [x] No Android framework dependencies

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/ShortcutAction.kt` (new)
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/ShortcutActionTest.kt` (new)

---

### T3: EditorShortcuts Mapper
**Type**: domain  
**Estimated LOC**: 35 production  
**Dependencies**: T2 (ShortcutAction)  

**TDD Steps**:
1. **RED**: Write `EditorShortcutsTest.kt` with table-driven assertions:
   - `Ctrl+Enter` → `ShortcutAction.Run`
   - `Ctrl+S` → `ShortcutAction.Save`
   - `Ctrl+Z` → `ShortcutAction.Undo`
   - `Ctrl+Y` → `ShortcutAction.Redo`
   - `Ctrl+Shift+Z` → `ShortcutAction.Redo`
   - `Ctrl+A`, `Tab`, `Backspace` → `null` (propagate)
2. **GREEN**: Create `domain/EditorShortcuts.kt`:
   ```kotlin
   object EditorShortcuts {
       fun mapKeyEvent(
           key: Key,
           isCtrlPressed: Boolean,
           isShiftPressed: Boolean
       ): ShortcutAction? = when {
           isCtrlPressed && !isShiftPressed && key == Key.Enter -> ShortcutAction.Run
           // ... 4 more mappings
           else -> null
       }
   }
   ```
3. **VERIFY**: `./gradlew test --tests EditorShortcutsTest` → all pass

**Acceptance Criteria**:
- [x] Pure function returns `null` for non-shortcuts (propagation preserved)
- [x] Table test covers all 4 shortcuts + 3 non-shortcuts
- [x] No `KeyEvent` Android class used (pure `Key` enum)

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/EditorShortcuts.kt` (new)
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/EditorShortcutsTest.kt` (new)

---

### T4: EditorHistory with Coalescing Logic
**Type**: domain  
**Estimated LOC**: 90 production  
**Dependencies**: T1 (EditorSnapshot)  

**TDD Steps**:
1. **RED**: Write `EditorHistoryTest.kt` asserting:
   - `push(snapshot)` adds to undo stack
   - `undo()` returns previous snapshot, moves current to redo stack
   - `redo()` reapplies, moves snapshot to undo stack
   - `canUndo`/`canRedo` reflect stack states
   - Consecutive single-char edits coalesce (6 chars → 1 undo)
   - Newline insertion flushes coalesce buffer
   - Cursor jump > 1 flushes buffer
   - Paste (multi-char delta) flushes buffer
   - Explicit `flush()` closes active coalesce
   - 101st push drops oldest snapshot (bounded at 100)
   - Multi-cursor snapshot restores all cursor positions on undo
   - New edit after undo clears redo stack
2. **GREEN**: Create `domain/EditorHistory.kt`:
   ```kotlin
   class EditorHistory(private val maxSize: Int = 100) {
       private val undoStack = ArrayDeque<EditorSnapshot>()
       private val redoStack = ArrayDeque<EditorSnapshot>()
       private var coalesceBuffer: EditorSnapshot? = null
       
       fun push(snapshot: EditorSnapshot, isCoalesceable: Boolean) { /*...*/ }
       fun undo(): EditorSnapshot? { /*...*/ }
       fun redo(): EditorSnapshot? { /*...*/ }
       fun flush() { /*...*/ }
       val canUndo: Boolean
       val canRedo: Boolean
   }
   ```
3. **VERIFY**: `./gradlew test --tests EditorHistoryTest` → all 12 test cases pass

**Acceptance Criteria**:
- [x] Push/undo/redo cycle preserves snapshots
- [x] Coalescing verified with 6-char typing → 1 undo
- [x] 4 flush triggers tested (newline, cursor jump, paste, explicit)
- [x] Bounded stack drops oldest at 101st push
- [x] Multi-cursor test verifies `List<Int>` restoration
- [x] Redo stack clears on new edit after undo
- [x] No Android or Compose dependencies

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/EditorHistory.kt` (new)
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/queryeditor/domain/EditorHistoryTest.kt` (new)

---

## Phase 2: ViewModel Integration

### T5: Wire EditorHistory to ViewModel
**Type**: viewmodel  
**Estimated LOC**: 45 production  
**Dependencies**: T4 (EditorHistory)  

**TDD Steps**:
1. **RED**: Write `QueryEditorViewModelTest.kt` assertions:
   - `canUndo` StateFlow emits `false` initially, `true` after text change
   - `canRedo` StateFlow emits `false` initially, `true` after undo
   - `undo()` updates `queryText` StateFlow to previous snapshot
   - `redo()` updates `queryText` to redone snapshot
   - `onUserEdit()` pushes to history and clears redo stack if needed
2. **GREEN**: Modify `QueryEditorViewModel.kt`:
   ```kotlin
   private val editorHistory = EditorHistory()
   private val _canUndo = MutableStateFlow(false)
   val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
   private val _canRedo = MutableStateFlow(false)
   val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()
   
   fun onUserEdit(snapshot: EditorSnapshot, isCoalesceable: Boolean) {
       editorHistory.push(snapshot, isCoalesceable)
       _canUndo.value = editorHistory.canUndo
       _canRedo.value = editorHistory.canRedo
   }
   
   fun undo() {
       editorHistory.undo()?.let { snapshot ->
           _queryText.value = snapshot.text
           _canUndo.value = editorHistory.canUndo
           _canRedo.value = editorHistory.canRedo
       }
   }
   
   fun redo() { /*...*/ }
   ```
3. **VERIFY**: `./gradlew test --tests QueryEditorViewModelTest` → all new tests pass

**Acceptance Criteria**:
- [ ] `canUndo`/`canRedo` StateFlows update on push/undo/redo
- [ ] `undo()` restores previous `queryText`
- [ ] `redo()` reapplies undone text
- [ ] ViewModel test verifies state transitions without UI

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorViewModel.kt` (modified, ~45 new lines)
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorViewModelTest.kt` (modified, ~30 new test lines)

---

### T6: Integrate onTextChange with History Coalescing
**Type**: viewmodel  
**Estimated LOC**: 25 production  
**Dependencies**: T5  

**TDD Steps**:
1. **RED**: Extend `QueryEditorViewModelTest.kt`:
   - Consecutive single-char edits coalesce (type `SELECT` → undo once removes all)
   - Newline insertion flushes buffer (type `SEL`, `\n`, `FROM` → undo removes `FROM` only)
   - Paste flushes buffer (multi-char delta detected)
2. **GREEN**: Modify `handleValueChange` in `QueryEditorViewModel.kt`:
   ```kotlin
   private var lastSnapshot: EditorSnapshot? = null
   
   fun handleValueChange(newValue: TextFieldValue) {
       val snapshot = EditorSnapshot(
           text = newValue.text,
           selection = newValue.selection,
           cursorPositions = /* extract from composition */
       )
       val isCoalesceable = detectSingleCharEdit(lastSnapshot, snapshot)
       onUserEdit(snapshot, isCoalesceable)
       lastSnapshot = snapshot
       _queryText.value = newValue.text
   }
   ```
3. **VERIFY**: `./gradlew test --tests QueryEditorViewModelTest` → coalescing tests pass

**Acceptance Criteria**:
- [ ] Single-char typing coalesces
- [ ] Newline and paste flush coalesce buffer
- [ ] Multi-cursor edits handled atomically
- [ ] Unit tests verify coalescing rules

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorViewModel.kt` (modified, ~25 new lines)
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorViewModelTest.kt` (modified, ~20 new test lines)

---

## Phase 3: UI Layer

### T7: Add onPreviewKeyEvent to SqlCodeEditor
**Type**: ui  
**Estimated LOC**: 8 production  
**Dependencies**: None  

**TDD Steps**:
1. **RED**: Write `SqlCodeEditorTest.kt` (if not exists, or manual smoke test):
   - Composable compiles with new `onPreviewKeyEvent` parameter
   - Parameter defaults to `null` (backward compatible)
2. **GREEN**: Modify `components/SqlCodeEditor.kt`:
   ```kotlin
   @Composable
   fun SqlCodeEditor(
       value: TextFieldValue,
       onValueChange: (TextFieldValue) -> Unit,
       modifier: Modifier = Modifier,
       onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null // NEW
   ) {
       BasicTextField(
           value = value,
           onValueChange = onValueChange,
           modifier = modifier.then(
               if (onPreviewKeyEvent != null) 
                   Modifier.onPreviewKeyEvent(onPreviewKeyEvent)
               else Modifier
           )
       )
   }
   ```
3. **VERIFY**: `./gradlew compileDebugKotlin` → no errors

**Acceptance Criteria**:
- [ ] New parameter is nullable with default `null`
- [ ] Existing call sites unaffected (backward compatible)
- [ ] `onPreviewKeyEvent` hook applied when non-null

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/components/SqlCodeEditor.kt` (modified, ~8 new lines)

---

### T8: Wire Shortcuts to ViewModel Actions
**Type**: ui  
**Estimated LOC**: 35 production  
**Dependencies**: T3 (EditorShortcuts), T5 (ViewModel undo/redo), T7 (onPreviewKeyEvent)  

**TDD Steps**:
1. **RED**: Write `EditorShortcutsUiTest.kt`:
   - Type text, press `Ctrl+Enter` → query executes
   - Type text, press `Ctrl+S` → save dialog appears
   - Type text, press `Ctrl+Z` → text reverts
   - Press `Ctrl+Z`, press `Ctrl+Y` → text reapplies
2. **GREEN**: Modify `QueryEditorScreen.kt`:
   ```kotlin
   val onKeyEvent: (KeyEvent) -> Boolean = { event ->
       if (event.type == KeyEventType.KeyDown) {
           EditorShortcuts.mapKeyEvent(
               key = event.key,
               isCtrlPressed = event.isCtrlPressed,
               isShiftPressed = event.isShiftPressed
           )?.let { action ->
               when (action) {
                   ShortcutAction.Run -> viewModel.executeQuery()
                   ShortcutAction.Save -> viewModel.showSaveDialog()
                   ShortcutAction.Undo -> viewModel.undo()
                   ShortcutAction.Redo -> viewModel.redo()
               }
               true // consumed
           } ?: false // propagate
       } else false
   }
   
   SqlCodeEditor(
       value = uiState.queryText,
       onValueChange = viewModel::handleValueChange,
       onPreviewKeyEvent = onKeyEvent
   )
   ```
3. **VERIFY**: `./gradlew connectedAndroidTest --tests EditorShortcutsUiTest` → all 4 shortcuts pass

**Acceptance Criteria**:
- [ ] 4 shortcuts invoke correct ViewModel methods
- [ ] Non-shortcut keys propagate (return `false`)
- [ ] E2E test verifies Run, Save, Undo, Redo on device

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorScreen.kt` (modified, ~35 new lines)
- `app/src/androidTest/java/com/sphynxs/mydatabases/ui/screens/queryeditor/EditorShortcutsUiTest.kt` (new, ~70 lines)

---

### T9: Add Undo/Redo Toolbar Buttons
**Type**: ui  
**Estimated LOC**: 30 production  
**Dependencies**: T5 (canUndo/canRedo StateFlows)  

**TDD Steps**:
1. **RED**: Extend `EditorShortcutsUiTest.kt`:
   - Undo button is disabled when `canUndo = false`
   - Undo button is enabled after typing
   - Redo button is disabled when `canRedo = false`
   - Redo button is enabled after undo
   - Tap Undo button → text reverts (same as `Ctrl+Z`)
2. **GREEN**: Modify `QueryEditorScreen.kt`:
   ```kotlin
   IconButton(
       onClick = { viewModel.undo() },
       enabled = uiState.canUndo
   ) {
       Icon(Icons.Default.Undo, contentDescription = stringResource(R.string.undo))
   }
   IconButton(
       onClick = { viewModel.redo() },
       enabled = uiState.canRedo
   ) {
       Icon(Icons.Default.Redo, contentDescription = stringResource(R.string.redo))
   }
   ```
3. **VERIFY**: `./gradlew connectedAndroidTest --tests EditorShortcutsUiTest` → button state tests pass

**Acceptance Criteria**:
- [ ] 2 new toolbar buttons added
- [ ] Buttons disabled when actions unavailable
- [ ] Tap behavior identical to keyboard shortcuts
- [ ] UI test verifies enabled/disabled states

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorScreen.kt` (modified, ~30 new lines)
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorUiState.kt` (modified, add `canUndo`/`canRedo` fields)
- `app/src/androidTest/java/com/sphynxs/mydatabases/ui/screens/queryeditor/EditorShortcutsUiTest.kt` (modified, ~20 new test lines)

---

### T10: Multi-Cursor Restoration on Undo
**Type**: ui  
**Estimated LOC**: 15 production  
**Dependencies**: T6 (history integration), T9 (undo UI)  

**TDD Steps**:
1. **RED**: Write `MultiCursorUndoTest.kt` (androidTest):
   - Set 3 cursors at positions `[10, 25, 40]`
   - Type `X` (inserts at all 3 positions)
   - Press `Ctrl+Z`
   - Assert cursors restored to `[10, 25, 40]`
2. **GREEN**: Modify `QueryEditorViewModel.undo()`:
   ```kotlin
   fun undo() {
       editorHistory.undo()?.let { snapshot ->
           _queryText.value = snapshot.text
           _selection.value = snapshot.selection
           _cursorPositions.value = snapshot.cursorPositions // restore multi-cursor
           _canUndo.value = editorHistory.canUndo
           _canRedo.value = editorHistory.canRedo
       }
   }
   ```
3. **VERIFY**: `./gradlew connectedAndroidTest --tests MultiCursorUndoTest` → multi-cursor test passes

**Acceptance Criteria**:
- [ ] Undo restores `selection` and `cursorPositions` atomically
- [ ] E2E test verifies all 3 cursors return to original positions
- [ ] Existing multi-cursor tests still pass

**Files Affected**:
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/queryeditor/QueryEditorViewModel.kt` (modified, ~15 new lines)
- `app/src/androidTest/java/com/sphynxs/mydatabases/ui/screens/queryeditor/MultiCursorUndoTest.kt` (new, ~40 lines)

---

## Phase 4: Internationalization

### T11: Add i18n Strings (en + es, TODO for 8 others)
**Type**: i18n  
**Estimated LOC**: 20 production (6 strings × 2 locales + 8 TODO files)  
**Dependencies**: T9 (toolbar buttons)  

**TDD Steps**:
1. **RED**: Compile `QueryEditorScreen.kt` → verify `R.string.undo` and `R.string.redo` resolve
2. **GREEN**: 
   - Add to `app/src/main/res/values/strings.xml`:
     ```xml
     <string name="undo">Undo</string>
     <string name="redo">Redo</string>
     <string name="shortcut_run">Run Query (Ctrl+Enter)</string>
     <string name="shortcut_save">Save (Ctrl+S)</string>
     <string name="shortcut_undo">Undo (Ctrl+Z)</string>
     <string name="shortcut_redo">Redo (Ctrl+Y)</string>
     ```
   - Add to `app/src/main/res/values-es/strings.xml`:
     ```xml
     <string name="undo">Deshacer</string>
     <string name="redo">Rehacer</string>
     <string name="shortcut_run">Ejecutar Consulta (Ctrl+Enter)</string>
     <string name="shortcut_save">Guardar (Ctrl+S)</string>
     <string name="shortcut_undo">Deshacer (Ctrl+Z)</string>
     <string name="shortcut_redo">Rehacer (Ctrl+Y)</string>
     ```
   - Add TODO markers to `values-{ar,de,fr,hi,ja,pt-rBR,ru,zh-rCN}/strings.xml`:
     ```xml
     <!-- TODO: Translate Undo, Redo, and 4 shortcut tooltips. Falls back to English. -->
     ```
3. **VERIFY**: 
   - `./gradlew compileDebugKotlin` → no missing resource errors
   - Manual test: switch device locale to `es` → verify "Deshacer" / "Rehacer" display
   - Manual test: switch to `ja` → verify English fallback

**Acceptance Criteria**:
- [ ] 6 strings added to `values/` (en) and `values-es/` (es)
- [ ] 8 other locale files contain TODO comments
- [ ] Spanish locale shows translated labels
- [ ] Untranslated locales fall back to English

**Files Affected**:
- `app/src/main/res/values/strings.xml` (modified, +6 strings)
- `app/src/main/res/values-es/strings.xml` (modified, +6 strings)
- `app/src/main/res/values-{ar,de,fr,hi,ja,pt-rBR,ru,zh-rCN}/strings.xml` (modified, +1 TODO comment each)

---

## Phase 5: Final Verification

### T12: End-to-End Test Coverage
**Type**: test  
**Estimated LOC**: 0 production (test-only)  
**Dependencies**: All prior tasks  

**TDD Steps**:
1. **Verify** all test suites pass:
   ```bash
   ./gradlew test                    # JVM unit tests
   ./gradlew connectedAndroidTest    # Compose UI tests
   ./gradlew compileDebugKotlin      # Compilation check
   ```
2. **Manual smoke test** checklist:
   - [ ] Type `SELECT * FROM users` → press `Ctrl+Z` → text reverts
   - [ ] Press `Ctrl+Y` → text reapplies
   - [ ] Type 6 chars continuously → `Ctrl+Z` once removes all 6
   - [ ] Type `SEL`, press `Enter`, type `FROM` → `Ctrl+Z` removes `FROM` only
   - [ ] Press `Ctrl+Enter` with non-blank text → query executes
   - [ ] Press `Ctrl+S` with non-blank text → save dialog opens
   - [ ] Tap Undo toolbar button → identical to `Ctrl+Z`
   - [ ] Undo button disabled when history empty
   - [ ] Redo button disabled when redo stack empty
   - [ ] Multi-cursor typing + undo restores all cursor positions
   - [ ] Switch locale to `es` → "Deshacer" / "Rehacer" display
   - [ ] Switch locale to `ja` → English labels display (fallback)
3. **Confirm**:
   - [ ] No regressions in existing multi-cursor tests
   - [ ] No dropped frames during sustained typing (manual observation)

**Acceptance Criteria**:
- [ ] All unit tests pass on JVM
- [ ] All Compose UI tests pass on device
- [ ] Compilation succeeds with no warnings
- [ ] 12-point smoke test checklist complete

**Files Affected**:
- No new files (verification only)

---

## Risk Assessment

| Task | Risk | Likelihood | Impact | Mitigation |
|------|------|------------|--------|------------|
| T4 (EditorHistory coalescing) | Complex state logic | Medium | High | Comprehensive unit tests (12 test cases); table-driven flush triggers |
| T6 (onTextChange integration) | Multi-cursor desync | Medium | Medium | Atomic `EditorSnapshot`; dedicated multi-cursor test (T10) |
| T8 (Shortcut wiring) | Key event conflicts with IME | Low | Medium | Gate on `KeyEventType.KeyDown` + physical key source; E2E test guards typing |
| T10 (Multi-cursor restoration) | Cursor positions lost on undo | Medium | High | Dedicated `MultiCursorUndoTest.kt`; verify with 3-cursor scenario |
| Overall LOC estimate | Underestimate by ~50 lines | Low | Low | Pure functions dominate; additive change minimizes integration risk |

### Notes
- **Coalescing complexity**: 4 flush triggers (newline, cursor jump, paste, explicit) are independently testable. Table-driven tests reduce risk.
- **Multi-cursor safety**: `EditorSnapshot` is atomic. Unit test verifies `List<Int>` restoration. E2E test confirms UI integration.
- **Shortcut conflicts**: Mapper returns `null` for non-shortcuts → propagation preserved. UI test verifies plain typing unaffected.

---

## Implementation Order

Tasks MUST be completed in order due to dependencies:

```
T1 (EditorSnapshot) → T4 (EditorHistory) → T5 (ViewModel wire) → T6 (onTextChange)
                                             ↓
T2 (ShortcutAction) → T3 (EditorShortcuts) → T8 (UI shortcuts)
                                             ↓
                      T7 (onPreviewKeyEvent) → T9 (toolbar buttons) → T10 (multi-cursor)
                                                                     ↓
                                                                  T11 (i18n)
                                                                     ↓
                                                                  T12 (verify)
```

**Suggested work-unit commits** (following `work-unit-commits` skill):
1. `feat(editor): add EditorSnapshot and ShortcutAction domain types` (T1 + T2, tests included)
2. `feat(editor): implement EditorShortcuts mapper` (T3, tests included)
3. `feat(editor): implement EditorHistory with coalescing` (T4, tests included)
4. `feat(editor): wire EditorHistory to ViewModel` (T5 + T6, tests included)
5. `feat(editor): add keyboard shortcut dispatch` (T7 + T8, E2E tests included)
6. `feat(editor): add undo/redo toolbar buttons` (T9, UI tests included)
7. `feat(editor): restore multi-cursor on undo` (T10, E2E test included)
8. `chore(i18n): add undo/redo strings (en + es)` (T11)
9. `test(editor): verify E2E shortcut coverage` (T12, verification only)

Each commit is independently reviewable and includes test + implementation + update (docs/comments if needed).

---

## Summary

| Phase | Tasks | Total LOC (prod + test) | Risk |
|-------|-------|------------------------|------|
| Phase 1: Domain | T1–T4 | ~150 + ~80 test | Low (pure functions) |
| Phase 2: ViewModel | T5–T6 | ~70 + ~50 test | Low (domain tested) |
| Phase 3: UI | T7–T10 | ~88 + ~130 test | Medium (integration) |
| Phase 4: i18n | T11 | ~20 (no tests) | Low (additive) |
| Phase 5: Verify | T12 | 0 (test-only) | Low (guard rail) |
| **Total** | **12** | **~460** | **Medium** |

**Ready for implementation** (pending user decision on single PR vs chained PRs).
