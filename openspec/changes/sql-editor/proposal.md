# Proposal: SQL Editor

## Intent

Replace `NewQueryScreen` placeholder with a professional SQL editor inside the workspace: syntax highlighting, multi-statement execution, unified result display. Blocked today by `query-editor-placeholder` spec.

## Scope

### In Scope
- `WorkspaceCard.Query` workspace card type (parallel to `Table`).
- `SqlTokenizer` (pure JVM) + `VisualTransformation` on `BasicTextField`, monospace, tab handling.
- Execute button (Idle/Running); UI-only Cancel (coroutine cancel).
- Multi-statement: tokenizer-aware `;` split, dispatch each by first keyword, run sequentially.
  - SELECT-like → render **last** result in `ResultGrid`.
  - INSERT/UPDATE/DELETE/DDL → summary table (statement, rows affected, time).
- Extract `ResultGrid` from `TableViewerScreen.RowsTab` into shared component.
- New `query-editor` spec **supersedes** `query-editor-placeholder`.
- en + es strings.

### Out of Scope
- Autocomplete, real `Statement.cancel()`, history/favorites, export, transaction UI.
- Intermediate SELECT results (only last shown).
- Route-based editor (`Routes.QueryEditor` untouched).

## Capabilities

### New Capabilities
- `query-editor`: editor with highlighting, multi-statement execution, result grid, update summary. Supersedes `query-editor-placeholder` on archive.

### Modified Capabilities
- `workspace-system`: add `Query` card variant; manager opens/focuses/closes query cards.

## Approach

- **Editor**: `BasicTextField` + `VisualTransformation` driven by `SqlTokenizer.tokenize(text)`. 50ms debounce on `Default` dispatcher.
- **Dispatch**: tokenizer-aware split on `;`. First keyword in `SELECT|SHOW|DESCRIBE|EXPLAIN|WITH` → `ExecuteQueryUseCase`; else → `ExecuteUpdateUseCase`.
- **State**: `QueryEditorViewModel` (Hilt): `Idle | Running | SelectResult | UpdateSummary | Mixed | Error`. Cancellable `Job`.
- **Workspace**: `WorkspaceCard.Query(id, connectionId, initialSql)`; `NewQueryScreen` opens via `WorkspaceManager`.
- **Refactor**: lift `RowsTab` into `ui/components/ResultGrid.kt`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/workspace/WorkspaceCard.kt` | Modified | Add `Query` variant. |
| `ui/workspace/WorkspaceManager.kt` | Modified | Handle query cards. |
| `ui/screens/queryeditor/` | New | Screen, ViewModel, UiState. |
| `ui/screens/queryeditor/components/` | New | `SqlCodeEditor`, `SqlTokenizer`, `SqlHighlightTheme`. |
| `ui/components/ResultGrid.kt` | New | Shared grid. |
| `ui/screens/tableviewer/TableViewerScreen.kt` | Modified | Consume `ResultGrid`. |
| `ui/screens/databases/NewQueryScreen.kt` | Modified | Launch `WorkspaceCard.Query`. |
| `res/values{,-es}/strings.xml` | Modified | Run/Cancel/Rows/Time/Affected. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `VisualTransformation` jank on large queries | Med | 50ms debounce, `Default` dispatcher, <10KB target. |
| `;` split breaks on strings/comments | Med | Tokenizer-aware splitter; document edges. |
| `mysql-connector-java:5.1.46` EOL | Low | Follow-up; not blocking. |
| Spec conflict with `query-editor-placeholder` | Med | New spec declares supersession. |
| UI Cancel misleads (server keeps running) | Low | Document JDBC behavior in spec. |

## Rollback Plan

Independent commits per step:
1. `ResultGrid` extraction — revert restores inline grid.
2. `WorkspaceCard.Query` additive — revert removes variant.
3. Spec supersession is file-level — revert restores placeholder.

No migrations, no persisted state.

## Dependencies

Existing: `ExecuteQueryUseCase`, `ExecuteUpdateUseCase`, `WorkspaceManager`, `WorkspaceCard.Table`. No new third-party libs.

## Success Criteria

- [ ] "New Query" opens `WorkspaceCard.Query` with highlighted editor.
- [ ] Keywords, identifiers, operators, strings, comments highlight distinctly.
- [ ] SELECT run populates `ResultGrid`.
- [ ] `UPDATE …; SELECT …;` shows SELECT result + UPDATE summary row.
- [ ] Cancel during Running returns to Idle within one frame.
- [ ] `SqlTokenizer` tests cover all token kinds.
- [ ] `QueryEditorViewModel` tests cover all state transitions + cancellation.
- [ ] `query-editor-placeholder` archived; `query-editor` active.
