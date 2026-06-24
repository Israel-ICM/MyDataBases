# Proposal: Editor Shortcuts and History

> **PR #1 of 3** — chained delivery of `editor-productivity-essentials`. Foundation for PR #2 (format + completion) and PR #3 (find/replace).

## Intent

SQL editor has no undo/redo and no shortcuts. Typos are destructive; physical-keyboard users (tablets, Chromebooks, DeX) can't run/save without the toolbar. PR #1 builds the key-dispatch + history foundation.

## Scope

### In Scope
- Pure `EditorHistory` (bounded stack, max 100, consecutive-typing coalescing).
- Pure `EditorShortcuts` mapper: `KeyEvent → ShortcutAction?`.
- Four shortcuts: `Ctrl+Enter` Run, `Ctrl+S` Save, `Ctrl+Z` Undo, `Ctrl+Y`/`Ctrl+Shift+Z` Redo.
- `QueryEditorViewModel`: `canUndo`/`canRedo` state, `undo`/`redo`/`onUserEdit` actions.
- `SqlCodeEditor`: new `onPreviewKeyEvent` hook. `QueryEditorScreen`: root key dispatch + Undo/Redo toolbar buttons.
- i18n: en + es now; TODO markers in 8 other locales (Android falls back to en).
- Unit tests (JVM) for both domain classes; Compose UI tests for the 4 shortcuts.

### Out of Scope
- Format SQL, code completion → PR #2. Find & Replace → PR #3.
- Shortcuts beyond the 4 core. Translation of 8 remaining locales. Coverage tooling.

## Capabilities

### New
- `editor-shortcuts`: keyboard dispatch + undo/redo history for the SQL editor.

### Modified
- None.

## Approach

- **Pure domain, no Compose** — JVM-testable without Robolectric.
- **Multi-cursor safe** — `Snapshot(text, selection, cursorPositions)` is atomic.
- **Coalescing** — same-kind single-char edits collapse; flushes on newline, cursor jump > 1, paste, blur, explicit `flush()`.
- **Bound** — head drops past 100 (~200 KB worst case).
- **Dispatch** — screen-root `onPreviewKeyEvent` for Run/Save; editor-local for Undo/Redo (focus required). Mapper returns `null` for non-shortcuts → propagation unchanged.
- **ViewModel owns history** — survives recomposition; integrates with existing multi-cursor `handleValueChange`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `queryeditor/domain/EditorHistory.kt` | New | Snapshot stack + coalescing. |
| `queryeditor/domain/EditorShortcuts.kt` | New | Pure mapper + `ShortcutAction` sealed class. |
| `queryeditor/QueryEditorViewModel.kt` | Modified | Owns history; exposes undo/redo state + actions. |
| `queryeditor/components/SqlCodeEditor.kt` | Modified | Adds `onPreviewKeyEvent` parameter. |
| `queryeditor/QueryEditorScreen.kt` | Modified | Root dispatch + 2 toolbar buttons. |
| `res/values/strings.xml` + `values-es/` | Modified | 6 strings (Undo, Redo, 4 tooltips). |
| `values-{ar,de,fr,hi,ja,pt-rBR,ru,zh-rCN}/` | Modified | TODO comments (auto-fallback to en). |
| `test/.../EditorHistoryTest.kt` | New | Snapshot/coalesce/bound/multi-cursor. |
| `test/.../EditorShortcutsTest.kt` | New | Table-driven key → action. |
| `androidTest/.../EditorShortcutsUiTest.kt` | New | E2E for the 4 shortcuts. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Multi-cursor desync on undo. | Med | Atomic snapshot; dedicated test. |
| Coalescing drops a wanted snapshot. | Med | Explicit flush rules, each tested. |
| Ctrl+Z conflicts with IME. | Low | Gate on physical key source. |
| Root `onPreviewKeyEvent` swallows other keys. | Low | Mapper returns `null` → propagates; UI test guards typing. |
| 8 locales show English. | Low | Auto-fallback; follow-up translation issue. |

## Rollback Plan

Revert merge commit. Additive change: new files delete cleanly; `onPreviewKeyEvent` defaults `null`; history records edits without gating them. No DB, DataStore, or migration touched.

## Dependencies

None.

## Follow-Ups

- **PR #2 `editor-format-and-completion`** — depends on `EditorHistory` + `ShortcutAction` extension.
- **PR #3 `editor-find-replace`** — depends on `EditorShortcuts`.
- Locale translation for 8 TODO locales.

## Success Criteria

- [ ] `EditorHistory` unit tests pass (push/undo/redo, coalescing, bound, multi-cursor, flush).
- [ ] `EditorShortcuts` unit tests pass (mapping matrix + null for non-shortcuts).
- [ ] Compose UI tests pass (4 shortcuts + plain typing unaffected).
- [ ] `./gradlew test` and `compileDebugKotlin` green.
- [ ] Undo/Redo buttons disabled when state forbids it (UI test).
- [ ] Existing multi-cursor tests still pass.
- [ ] Production diff ≤ ~250 LOC.
