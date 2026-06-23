# Exploration: sql-editor

## Current State

A real SQL editor does **not** exist yet. Today there are TWO empty placeholders that need consolidation:

1. **`NewQueryScreen`** (`ui/screens/databases/NewQueryScreen.kt`) — wired into the bottom-nav at route `connection/{connectionId}/new_query`. Renders an icon + `R.string.query_editor_title` + "Coming soon" message. Spec: `query-editor-placeholder` in change `database-list-bottom-nav` (active, not yet archived).
2. **`Routes.QueryEditor`** (`connection/{connectionId}/editor`) — declared but routed to a generic `PlaceholderScreen("Editor — …")` in `MyDataBasesNavHost.kt`. Unused by the bottom-nav.

The backend stack to execute queries is **fully built and tested**:

- `DatabaseEngine.executeQuery(query, params): Result<QueryResult>` — interface, with `MySQLEngine` and `MariaDBEngine` implementations using JDBC prepared statements and `MySQLConnectionPool`.
- `DatabaseEngine.executeUpdate(query, params): Result<Int>` — for INSERT/UPDATE/DELETE/DDL.
- `DatabaseRepository` exposes both, plus `getVersion()`, `getSupportedFeatures()`, transactions, and metadata readers.
- `ExecuteQueryUseCase` and `ExecuteUpdateUseCase` already injected via Hilt and used by `TableViewerViewModel`.
- `QueryResult(columns, rows, rowCount, executionTimeMs, warnings)` is the result model.

A **prior spec** under `openspec/changes/ui-implementation/specs/query-runner/spec.md` already describes a plain-text query runner (no highlighting), with explicit decisions on: 1000-row cap, NULL display, error inline above editor, cancel-long-query, no on-disk history. That spec is the natural ancestor; this change supersedes it by adding professional syntax highlighting and consolidating the placeholder.

The existing data-grid pattern in `TableViewerScreen.RowsTab` (LazyColumn + `horizontalScroll(rememberScrollState())` + fixed `150.dp` column width + `HorizontalDivider` header) is the reusable template for query results — **but it is currently `private` and inlined**. It should be extracted to a shared `ResultTable` / `DataGrid` component.

## Affected Areas

- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/NewQueryScreen.kt` — **replace** placeholder body with the real editor screen (or rename/move to `ui/screens/queryeditor/`).
- `app/src/main/java/com/sphynxs/mydatabases/ui/navigation/MyDataBasesNavHost.kt` — keep the existing `Routes.NewQuery` binding; decide whether to deprecate the duplicate `Routes.QueryEditor` placeholder.
- `app/src/main/java/com/sphynxs/mydatabases/ui/navigation/Routes.kt` — possibly collapse `QueryEditor` into `NewQuery` (one route, one screen).
- `app/src/main/java/com/sphynxs/mydatabases/domain/usecases/` — `ExecuteQueryUseCase` and `ExecuteUpdateUseCase` already exist; **no domain changes needed** for v1. May add a thin `RunSqlUseCase` that dispatches SELECT vs non-SELECT.
- `app/src/main/res/values/strings.xml` + `values-es/strings.xml` — new strings for "Run", "Running…", "Cancel", "Rows: N", "Time: Nms", "Affected rows: N", error labels.
- `app/build.gradle.kts` — add the chosen highlighter dependency (see Approaches below).
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/<new-package>/` — new ViewModel test mirroring the `TableViewerViewModelTest` pattern (mockk + Turbine + `StandardTestDispatcher`).
- `app/src/androidTest/java/com/sphynxs/mydatabases/ui/screens/<new-package>/` — new Compose UI test (the repo already has `compose.ui.ui-test-junit4` wired).
- `openspec/changes/database-list-bottom-nav/specs/query-editor-placeholder/spec.md` — this is currently the contract for `NewQueryScreen`. Note the conflict: that spec **forbids** `TextField`, syntax highlighter, or Run controls. This change MUST add a delta spec that supersedes that placeholder requirement (most likely path: keep the placeholder change as-is for archive, and have `sql-editor` write a new delta domain `query-editor` that replaces `query-editor-placeholder` on archive).
- `openspec/changes/ui-implementation/specs/query-runner/spec.md` — superseded conceptually; the new spec should reference and extend it (syntax highlighting + tab key + monospace).

## Approaches

### 1. Custom `BasicTextField` + AnnotatedString tokenizer — **RECOMMENDED**

Build a lightweight SQL tokenizer that returns `AnnotatedString` with `SpanStyle` per token (KEYWORD, STRING, NUMBER, COMMENT, IDENTIFIER, OPERATOR). Drive it from a `VisualTransformation` over a `BasicTextField` (or the new `BasicTextField2` / `TextFieldState` API on Compose 1.7+), with a monospace `FontFamily.Monospace` and theme-driven colors from `MaterialTheme.colorScheme` (so it follows light/dark + branded theme).

- **Pros**:
  - Zero third-party dependency, zero supply-chain risk.
  - Renders 100% natively in Compose — no WebView, no extra APK weight.
  - Full control over color tokens, integrates cleanly with `DesignTokens` / `BrandedColors` already in the repo.
  - SQL keyword list is small and well-defined (MySQL/MariaDB ~250 reserved words) — a regex/state-machine tokenizer is ~200 LOC.
  - Easy to extend later with autocomplete (you reuse the same tokens).
  - Testable in plain JVM JUnit (tokenizer is a pure function returning a list of `Token(range, kind)`).
- **Cons**:
  - You own the tokenizer (edge cases: nested quotes, `--` vs `-` operator, `/* … */` block comments crossing lines, backtick identifiers, dollar-quoted strings if PG is added later).
  - `VisualTransformation` recomputes spans on every keystroke — must cap re-tokenization (debounce or only re-tokenize the changed line). For multi-KB queries this matters.
- **Effort**: **Medium** (2–3 days incl. tests). Tokenizer + VisualTransformation + theme colors + Compose UI test for the highlight contract.

### 2. `Qawaz/compose-code-editor` (third-party library)

A multiplatform Compose code editor with built-in syntax highlighting. Renders natively (no WebView).

- **Pros**: Out-of-the-box editor with line numbers and several language modes. Compose-multiplatform compatible.
- **Cons**:
  - Distributed primarily via GitHub Packages (auth required) — JitPack is "best effort" and historically flaky. This is friction for the project's current Gradle setup (Maven Central + Google + JitPack only).
  - Last meaningful update is old (2.0.3 line); community notes (Cantilever devlog, 2025-09) call it "not updated recently".
  - SQL is not a first-class mode in the bundled set — you would still write the lexer.
  - Adds a transitive dependency footprint to a single-feature surface.
  - Theme integration is fixed to library tokens, not `MaterialTheme`.
- **Effort**: **Low to integrate, Medium to theme**, but the dependency-acquisition risk is real.

### 3. `hossain-khan/android-compose-highlight` (Highlight.js in hidden WebView)

A Compose library that runs Highlight.js inside a hidden WebView and converts the tokenized HTML to `AnnotatedString` for display.

- **Pros**: 190+ languages including SQL, accurate tokenization (it's Highlight.js).
- **Cons**:
  - Designed for **display** of code blocks, NOT for an editable `TextField`. The README/blog explicitly frame it as "selectable, copyable code blocks". Wiring it into a live editor would require running highlight.js on every keystroke through a WebView bridge — unacceptable latency.
  - Adds a WebView to a screen that today has none → memory/cold-start cost, plus a Bluetooth-permission-style policy review surface.
  - Overkill for a single language (SQL).
- **Effort**: **High** (and likely the wrong tool for an *editor*).

### 4. Plain `TextField` with NO highlighting (just monospace)

Ship a `BasicTextField` with `FontFamily.Monospace`, Run button, result grid, error pane. No colors at all.

- **Pros**: Trivial (½ day), zero risk, matches the existing `query-runner` spec already written under `ui-implementation`.
- **Cons**: User explicitly asked for **professional syntax highlighting**. This fails the brief.
- **Effort**: **Low**, but does not meet requirements.

## Recommendation

**Approach 1 — Custom tokenizer + `BasicTextField` + `VisualTransformation`.**

Rationale:

- The brief says "lo más profesional posible" — but "professional" in this codebase context means **integrated with the design system**, not "the most features". Approach 1 is the only one that honors `BrandedColors`, `DesignTokens`, dark mode, branded theme accents, and reduced-motion all at once without adapter glue.
- Zero new dependencies aligns with the project's current dependency posture (Maven Central + Google + JitPack only, no GitHub Packages auth wired into Gradle, no WebView in any current screen).
- Testability: a pure-function `SqlTokenizer.tokenize(text: String): List<Token>` is JVM-unit-testable with no Compose harness, no Robolectric, no instrumentation. That matches the project's `strict_tdd: true` config.
- Future-proofs the autocomplete extension you already plan to add later — both autocomplete and highlighting share the same token stream.
- Performance is manageable: SQL queries are typically <2 KB; line-level re-tokenization caps the cost.

**Proposed architecture (Clean Architecture + MVVM, matches `TableViewerViewModel` shape):**

```
ui/screens/queryeditor/
├── QueryEditorScreen.kt           // @Composable, TopAppBar + Editor + Toolbar + ResultPane
├── QueryEditorViewModel.kt        // @HiltViewModel — holds TextFieldState + UiState
├── QueryEditorUiState.kt          // sealed: Idle | Running | SelectResult | UpdateResult | Error
└── components/
    ├── SqlCodeEditor.kt           // BasicTextField + VisualTransformation
    ├── SqlSyntaxTokenizer.kt      // pure: String -> List<SqlToken>
    ├── SqlHighlightTheme.kt       // maps TokenKind -> SpanStyle via MaterialTheme
    └── ResultGrid.kt              // extracted from TableViewerScreen.RowsTab (reusable)

domain/usecases/
└── (reuse ExecuteQueryUseCase + ExecuteUpdateUseCase — no new use cases needed for v1)
```

**Statement dispatch logic** in the ViewModel:

- Trim and inspect the first SQL keyword (skipping `--` and `/* */` comments).
- If it starts with `SELECT`, `SHOW`, `DESCRIBE`, `EXPLAIN`, `WITH` → `ExecuteQueryUseCase` → render `ResultGrid`.
- Otherwise → `ExecuteUpdateUseCase` → render affected-row count card.
- Run inside `viewModelScope.launch { ... }` with a cancellable `Job` so a "Cancel" button can `job.cancel()`.

**Result grid extraction**: refactor `TableViewerScreen.RowsTab` (lines 215–267) into a shared `ResultGrid(columns, rows, modifier)` component. Both `TableViewerScreen` and `QueryEditorScreen` consume it. This is a low-risk refactor and improves the broader codebase.

## Risks

- **Spec conflict with `query-editor-placeholder`**: the active `database-list-bottom-nav` change spec says `NewQueryScreen` MUST NOT render a `TextField` or editor. The new `sql-editor` change must explicitly declare a delta that supersedes this. Mitigation: in `sdd-propose`, list `database-list-bottom-nav` as a prerequisite to archive first, OR add a "supersedes" note in the proposal and write the new delta under domain `query-editor` so archival merges cleanly.
- **Conflict with `ui-implementation` change `query-runner`**: that spec already covers run/grid/error/cancel/row-cap. The new spec should `## ADDED` only the highlighting + monospace + tab requirements and `## MODIFIED` the existing run-button requirement to reference the new editor. Mitigation: read that spec first when writing the delta.
- **`VisualTransformation` performance on long queries**: re-tokenizing the full text on every keystroke is O(n). For queries >5 KB this becomes janky on low-end devices. Mitigation: tokenize on a background `Default` dispatcher with `derivedStateOf` + 50ms debounce, OR adopt the new `TextFieldState` API (Compose 1.7+, which the BOM `2025.05.01` includes) and react to `text` changes via snapshotFlow with debounce.
- **Multi-statement queries**: users will paste `SELECT … ; UPDATE …;`. JDBC's `Statement.execute(sql)` only runs the first by default. Decide v1 policy: reject multi-statement (split on `;` outside strings/comments → error if >1) or run sequentially showing the last result. Lean toward "v1 = single statement, show error otherwise".
- **MySQL driver version**: `mysql:mysql-connector-java:5.1.46` is ancient (2018, end-of-life). Not a blocker for this change but worth flagging in the proposal's risks section.
- **No coverage tool configured** (per `config.yaml`): tests will run via `./gradlew test` but no coverage threshold is enforced. The tokenizer is the highest-value test surface — invest there.
- **Tab character handling**: the existing `query-runner` spec already says tabs must be preserved. With `BasicTextField`, the IME's "next" action may swallow tab — verify on physical keyboard input via instrumentation test.
- **Cancel semantics with JDBC**: cancelling a coroutine does NOT cancel an in-flight JDBC query unless `Statement.cancel()` is called explicitly from another thread. The current `MySQLEngine` does not expose this. v1 may have to ship "Cancel" as "abandon UI" only, with a follow-up to wire real statement cancellation. Document this in the proposal.

## Ready for Proposal

**Yes** — with the following points the orchestrator should confirm with the user before `sdd-propose`:

1. **Single route**: collapse the duplicate `Routes.NewQuery` and `Routes.QueryEditor` into one (`new_query`), or keep both? Recommendation: keep `new_query` (already in bottom-nav), delete `QueryEditor` route + its `PlaceholderScreen`.
2. **Multi-statement policy for v1**: reject with error (recommended) or execute first only?
3. **Cancel semantics for v1**: UI-only cancel (recommended for scope) or proper `Statement.cancel()` (adds work to `DatabaseEngine` interface)?
4. **Result grid extraction**: do the `TableViewerScreen.RowsTab` → shared `ResultGrid` refactor inside this change (recommended), or as a separate prep change?
5. **Spec strategy**: write the delta as a new domain `query-editor` under `openspec/changes/sql-editor/specs/query-editor/` that supersedes `query-editor-placeholder` on archive — confirm this is acceptable, or prefer to archive `database-list-bottom-nav` first.

Once these are answered, the proposal can be drafted with confidence. Strict TDD is on; expect tasks ordered as: tokenizer tests → tokenizer impl → ViewModel tests → ViewModel impl → screen UI test → screen impl.
