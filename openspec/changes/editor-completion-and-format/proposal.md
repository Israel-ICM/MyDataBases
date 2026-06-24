# Proposal: Editor Completion & Format

## Intent

The Level 1 editor (shortcuts + undo/redo) shipped a solid foundation, but writing SQL by hand still has zero assistance: no keyword/table/column hints, no on-demand normalization. Users typing against a MySQL/MariaDB schema want two things that every modern SQL client offers — context-aware completion and a one-tap formatter — and the existing primitives (`SqlTokenizer`, `onPreviewKeyEvent`, `EditorHistory`, `TextLayoutResult.getBoundingBox`) make both achievable without rewrites. This change closes the gap so MyDataBases stops feeling like a plain text box and starts feeling like an IDE-lite for offline DB administration.

## Scope

### In Scope
- `SqlFormatter` pure module: UPPERCASE keywords, newline before major clauses (`FROM`/`WHERE`/`JOIN`/`GROUP BY`/`ORDER BY`/`HAVING`/`LIMIT`/`UNION`), 2-space indent for subclauses, preserve strings/comments/projection lists verbatim, idempotent.
- Format toolbar button (left pill, after Redo) + `Ctrl+Shift+F` shortcut; pushes current snapshot to `EditorHistory` before applying so `Ctrl+Z` undoes atomically.
- `SqlCompletionProvider` pure module: ranked suggestions (keywords always, tables + columns when schema available), context bias (after `FROM`/`JOIN`/`UPDATE`/`INTO` rank TABLE first; after `SELECT`/`WHERE`/`ON`/`,` rank COLUMN first), suppression inside strings/comments, cap 8 items.
- `SqlKeywords` extracted as single source of truth (tokenizer + formatter + provider consume it).
- `SchemaSnapshot` + `LoadSchemaSnapshotUseCase` wrapping existing `GetTablesUseCase` + `GetColumnsUseCase`; lazy per-table column load; refresh on DDL (`CREATE`/`ALTER`/`DROP TABLE`) or `USE <db>`.
- Hybrid schema source: `databaseName: String?` plumbed `WorkspaceOverlay → QueryEditorScreen`. Non-null → schema-aware completion. Null → keywords only.
- Column type display: `name : VARCHAR` format on COLUMN suggestions.
- `CompletionPopup` Composable: `Popup` anchored at cursor via `TextLayoutResult.getBoundingBox`, `LazyColumn` (8 visible, scroll for rest), arrow navigation, Enter/Tab accept, Esc dismiss, tap to accept.
- Auto-trigger: identifier prefix ≥ 2 chars, 150 ms debounce, Esc-remembers-token. Manual trigger (`Ctrl+Space` or future toolbar button) bypasses gating.
- Multi-cursor safety: completion disabled when `cursorPositions.isNotEmpty()`; format clears cursors.
- i18n: ~8 strings × 10 locales (full en + es; other 8 fall back per Level 1 precedent).

### Out of Scope
- Smart indentation for nested subqueries (v1 formatter is flat).
- Format restructuring of projection lists (`SELECT a, b, c` stays on one line — user explicitly opted out).
- Alias detection (`SELECT u.id FROM users u` → `u.` member suggestions).
- JOIN-path / FK-aware suggestions.
- Custom snippet templates.
- Format configuration (case, indent width) — locked to defaults for v1.
- Settings UI for completion toggles.

## Capabilities

### New Capabilities
- `sql-formatter`: pure formatter rules, idempotency contract, string/comment preservation, toolbar + shortcut integration, history-atomic apply.
- `sql-completion`: trigger gating, suggestion ranking, context detection, popup behavior, multi-cursor exclusion, schema lifecycle.

### Modified Capabilities
- None. No existing `openspec/specs/` capabilities exist yet; this change introduces the first formal capability specs for the editor surface.

## Approach

**Domain layer (pure, JVM-testable):**
- `SqlKeywords` — single keyword set; tokenizer regex builds from it; test asserts non-drift.
- `SqlFormatter.format(sql) -> String` — token-stream rewriter over `SqlTokenizer`. Idempotency enforced by golden-file test.
- `SqlCompletionProvider.suggest(text, offset, schema, limit=8) -> List<CompletionSuggestion>` — context detection by scanning last meaningful keyword before cursor; ranking by kind bias + alphabetical.
- `CompletionSuggestion(kind, label, insertText, typeLabel?)` and `SchemaSnapshot(tables, columnsByTable)` data classes.
- `LoadSchemaSnapshotUseCase` wraps existing repository use cases; lazy column load.

**ViewModel layer (`QueryEditorViewModel`):**
- `schemaSnapshot: StateFlow<SchemaSnapshot>` (empty when `databaseName == null`).
- `suggestions: StateFlow<List<CompletionSuggestion>>` driven by a debounced (150 ms) trigger flow from cursor/text changes.
- `formatSql()`: pushes history → runs formatter on `Dispatchers.Default` → emits new `TextFieldValue`.
- DDL/USE detection in `executeStatements` triggers schema reload.

**UI layer:**
- `CompletionPopup.kt` — `Popup` + `LazyColumn`, non-focusable (editor keeps focus), driven by ViewModel state.
- `SqlCodeEditor` — extend `onPreviewKeyEvent` to route arrow/Enter/Esc when popup visible; anchor popup via existing `TextLayoutResult.getBoundingBox(cursorPos)`.
- `QueryEditorScreen` — Format toolbar button, wire `onShortcut(Format)` and `onShortcut(TriggerCompletion)`, accept new `databaseName: String?` param.

**Tests (TDD red-first):**
- Unit: `SqlFormatterTest` (golden-file), `SqlCompletionProviderTest` (table-driven), `SqlKeywordsTest` (non-drift), updated `EditorShortcutsTest`.
- AndroidTest: format button click, `Ctrl+Shift+F`, popup appears on `SEL`, arrow nav, Enter inserts, Esc dismisses, popup absent in strings/comments and in multi-cursor mode.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/editor/ShortcutAction.kt` | Modified | Add `Format`, `TriggerCompletion` cases |
| `domain/editor/EditorShortcuts.kt` | Modified | Map `Ctrl+Shift+F`, `Ctrl+Space` |
| `domain/editor/SqlKeywords.kt` | New | Single source of truth for keyword set |
| `domain/editor/SqlFormatter.kt` | New | Pure formatter |
| `domain/editor/SqlCompletionProvider.kt` | New | Pure suggestion engine |
| `domain/editor/CompletionSuggestion.kt` | New | Data class (kind, label, insertText, typeLabel) |
| `domain/editor/SchemaSnapshot.kt` | New | Data class (tables, columnsByTable) |
| `domain/usecase/LoadSchemaSnapshotUseCase.kt` | New | Wraps `GetTablesUseCase` + `GetColumnsUseCase` |
| `ui/screens/queryeditor/components/SqlTokenizer.kt` | Modified | Build keyword regex from `SqlKeywords` |
| `ui/screens/queryeditor/components/SqlCodeEditor.kt` | Modified | Popup anchoring, navigation key routing |
| `ui/screens/queryeditor/components/CompletionPopup.kt` | New | Popup + LazyColumn UI |
| `ui/screens/queryeditor/QueryEditorViewModel.kt` | Modified | Schema flow, suggestions flow, `formatSql()` |
| `ui/screens/queryeditor/QueryEditorScreen.kt` | Modified | Format button, shortcut wiring, `databaseName` param |
| `ui/screens/workspace/WorkspaceOverlay.kt` | Modified | Pass `databaseName` to `QueryEditorScreen` |
| `res/values*/strings.xml` | Modified | ~8 new strings × 10 locales (en + es full) |
| `test/.../editor/` | New | `SqlFormatterTest`, `SqlCompletionProviderTest`, `SqlKeywordsTest` |
| `androidTest/.../queryeditor/` | New | Format + completion E2E tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `databaseName` nullable on main-screen entry | High | Completion gracefully degrades to keywords-only; covered by spec scenario |
| Keyword list drift across 3 consumers | Medium | `SqlKeywords` extraction + non-drift test; tokenizer regex rebuilt from set |
| Multi-cursor × completion ambiguity | Medium | Hard disable when cursors active (documented UX rule) |
| Auto-popup intrusive on fast typing | Medium | 2-char min + 150 ms debounce + Esc-remembers-token |
| Soft keyboard Ctrl combos unreliable | High | Toolbar Format button is canonical path; auto-trigger doesn't need shortcut |
| PR #2 ships invisible code | Medium | Schema + provider fully unit-tested; PR description flags the staging |
| Stale schema after external DDL | Low | Refresh on in-editor DDL execution; manual refresh follow-up if needed |
| Schema cost on huge DBs | Low | Lazy per-table column load; only loaded on completion request for that table |

## Rollback Plan

Format and completion are additive — no existing data model or persisted state changes. Rollback per PR:
- **PR #1 (Format)**: revert formatter module, toolbar button, shortcut mapping for `Format`. `EditorHistory` semantics unchanged. No migrations.
- **PR #2 (Schema + Provider)**: revert `LoadSchemaSnapshotUseCase`, schema StateFlow, `databaseName` plumbing. Existing `getTables`/`getColumns` use cases untouched.
- **PR #3 (Popup)**: revert `CompletionPopup`, popup wiring in `SqlCodeEditor`, `Ctrl+Space` mapping. Editor returns to pre-completion behavior.

A full revert removes the `SqlKeywords` extraction; tokenizer falls back to its current inlined regex (preserve the pre-change regex as a git tag for fast restore).

## Dependencies

- Existing: `SqlTokenizer`, `EditorHistory`, `EditorShortcuts`, `QueryEditorViewModel`, `SqlCodeEditor`, `GetTablesUseCase`, `GetColumnsUseCase`, `WorkspaceOverlay`.
- New internal: `LoadSchemaSnapshotUseCase`.
- No new third-party libraries. No new Gradle modules. No new Room tables.

## Success Criteria

- [ ] Toolbar Format button and `Ctrl+Shift+F` produce idempotent, history-atomic UPPERCASE-keyword formatted SQL with major-clause newlines.
- [ ] Typing `SEL` in an empty editor opens a popup with `SELECT` ranked first; Enter inserts and dismisses.
- [ ] Editor opened with non-null `databaseName` suggests tables after `FROM` and columns after `SELECT`/`WHERE` with `name : TYPE` rendering.
- [ ] Editor opened with null `databaseName` suggests keywords only (no schema fetch attempted).
- [ ] Popup never appears inside strings, comments, or when multi-cursor is active.
- [ ] All unit tests green (formatter golden files, provider table-driven, keywords non-drift); all androidTest UI scenarios green.
- [ ] PR sizes: #1 ≤ 350 LOC, #2 ≤ 450 LOC, #3 ≤ 600 LOC (review budget respected).
- [ ] en + es strings shipped; remaining 8 locales documented as fallback.
