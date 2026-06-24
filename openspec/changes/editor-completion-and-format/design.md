# Design: Editor Completion & Format

## 1. Architecture Overview

### Component Layers

```
┌─────────────────────────────────────────────────────────────┐
│ UI Layer (Compose)                                          │
├─────────────────────────────────────────────────────────────┤
│ QueryEditorScreen.kt                                        │
│  - Format toolbar button                                    │
│  - Shortcuts wiring (Ctrl+Shift+F, Ctrl+Space)              │
│  - Receives databaseName: String? from WorkspaceOverlay     │
│                                                              │
│ SqlCodeEditor.kt                                            │
│  - Popup anchoring via TextLayoutResult.getBoundingBox      │
│  - Navigation key routing (↓↑ Enter Tab Esc)                │
│  - Auto-trigger debounce (150ms)                            │
│                                                              │
│ CompletionPopup.kt (NEW)                                    │
│  - Popup + LazyColumn (8 visible, scroll for rest)          │
│  - Renders CompletionSuggestion list                        │
│  - Tap-to-accept, arrow navigation                          │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│ ViewModel Layer                                             │
├─────────────────────────────────────────────────────────────┤
│ QueryEditorViewModel.kt                                     │
│  - schemaSnapshot: StateFlow<SchemaSnapshot>                │
│  - formatSql(): suspend fun (Dispatchers.Default)           │
│  - getSuggestions(prefix, context): List<CompletionSugg>    │
│  - DDL/USE detection → schema reload trigger                │
└─────────────────────────────────────────────────────────────┘
                            ↕
┌─────────────────────────────────────────────────────────────┐
│ Domain Layer (Pure, JVM-testable)                           │
├─────────────────────────────────────────────────────────────┤
│ SqlKeywords.kt (NEW)                                        │
│  - val KEYWORDS: Set<String> (single source of truth)       │
│                                                              │
│ SqlFormatter.kt (NEW)                                       │
│  - format(sql: String): String                              │
│  - Pure function, idempotent contract                       │
│                                                              │
│ SqlCompletionProvider.kt (NEW)                              │
│  - getSuggestions(prefix, context, schema): List<Sugg>      │
│  - Context detection, ranking, filtering                    │
│                                                              │
│ LoadSchemaSnapshotUseCase.kt (NEW)                          │
│  - Wraps GetTablesUseCase + GetColumnsUseCase               │
│  - Lazy per-table column loading                            │
│                                                              │
│ EditorShortcuts.kt (MODIFIED)                               │
│  - Add Format, TriggerCompletion mappings                   │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow: Format

```
User clicks Format button
  ↓
QueryEditorScreen.formatSql()
  ↓
QueryEditorViewModel.formatSql()
  ├─ pushHistory(currentSnapshot)   [pre-format]
  ├─ withContext(Dispatchers.Default) {
  │    SqlFormatter.format(sqlText.text)
  │  }
  ├─ sqlText = TextFieldValue(formatted)
  └─ pushHistory(newSnapshot)        [post-format, enables Ctrl+Z]
```

### Data Flow: Completion (Auto-Trigger)

```
User types "SEL"
  ↓
SqlCodeEditor.onValueChange
  ↓
Extract prefix at cursor position
  ↓
if (prefix.length >= 2 && !isInString && !isInComment && cursorPositions.isEmpty())
  ↓
launch debounced (150ms) {
  viewModel.getSuggestions(prefix, context)
    ↓
  SqlCompletionProvider.getSuggestions(prefix, context, schema)
    ├─ Detect last keyword before cursor (FROM/SELECT/WHERE/...)
    ├─ Filter by prefix (case-insensitive)
    ├─ Rank: context-biased kind order + alphabetical
    └─ Cap at 8 items
      ↓
  showPopup = true, suggestions = result
}
  ↓
CompletionPopup renders anchored to cursor
  ↓
User presses ↓ (selectedIndex++)
User presses Enter
  ↓
Insert suggestion.insertText at cursor
Clear popup
```

### Data Flow: Completion (Manual Trigger)

```
User presses Ctrl+Space
  ↓
SqlCodeEditor.onPreviewKeyEvent → TriggerCompletion
  ↓
Extract prefix at cursor (NO length/string gating)
  ↓
suggestions = viewModel.getSuggestions(prefix, context)
  ↓
showPopup = true
```

---

## 2. Key Design Decisions (ADRs)

### ADR 1: SqlKeywords as Single Source of Truth

**Context**: Three consumers need the same keyword list: SqlTokenizer (syntax highlighting), SqlFormatter (UPPERCASE transformation), SqlCompletionProvider (keyword suggestions). Drift between these would cause inconsistent UX.

**Decision**: Extract `object SqlKeywords` with a single `val KEYWORDS: Set<String>` containing ~75 MySQL/MariaDB keywords. SqlTokenizer builds its regex from this set. Formatter and provider read from it directly.

**Alternatives considered**:
- Duplicate lists in each consumer → rejected: high drift risk, no single update point
- Tokenizer regex as source of truth → rejected: regex is implementation detail, harder to consume for completion/format

**Rationale**: Single source of truth prevents drift. Test enforces non-emptiness and presence of canonical keywords (SELECT, FROM, WHERE, JOIN). Any keyword addition automatically propagates to all consumers.

**Consequences**:
- ✅ No drift between tokenizer, formatter, completion
- ✅ One place to add new keywords
- ✅ Testable: `SqlKeywordsTest` asserts canonical set
- ⚠️ SqlTokenizer regex rebuild on initialization (negligible cost)

---

### ADR 2: Formatter is Pure Function

**Context**: Format operation must be testable, idempotent, reversible via undo/redo. No side effects allowed.

**Decision**: `SqlFormatter.format(sql: String): String` — stateless, no dependencies, no I/O. Idempotency enforced by contract: `format(format(x)) == format(x)`.

**Alternatives considered**:
- Stateful formatter with configuration → rejected: complicates testing, v1 locks formatting rules
- Mutating formatter (modifies TextFieldValue in place) → rejected: breaks EditorHistory integration

**Rationale**: Pure function enables golden-file testing, idempotency verification, and atomic history integration (pre-format snapshot → apply → post-format snapshot).

**Consequences**:
- ✅ 100% JVM unit testable (no Android dependencies)
- ✅ EditorHistory integration trivial (snapshot before/after)
- ✅ Idempotency testable via property-based test
- ⚠️ v1 locked to UPPERCASE + newlines + 2-space indent (no configuration)

---

### ADR 3: Completion Provider is Pure Function

**Context**: Suggestion generation must be deterministic, testable, independent of UI state or async flows.

**Decision**: `SqlCompletionProvider.getSuggestions(prefix: String, context: String, schema: SchemaSnapshot?): List<CompletionSuggestion>` — pure function, no coroutines, no Compose. ViewModel wraps this in a suspend function for debouncing.

**Alternatives considered**:
- ViewModel owns suggestion logic → rejected: mixes concerns, hard to test ranking independently
- Provider as suspend function → rejected: no I/O needed, pure logic doesn't need coroutines

**Rationale**: Pure function enables table-driven unit tests for all 16 spec scenarios (context ranking, prefix filtering, string/comment suppression, multi-cursor exclusion).

**Consequences**:
- ✅ 100% JVM testable with table-driven tests
- ✅ ViewModel only handles debouncing + StateFlow emissions
- ✅ Deterministic output for same inputs
- ⚠️ No async schema loading inside provider (caller's responsibility)

---

### ADR 4: Popup Anchored via TextLayoutResult.getBoundingBox

**Context**: Popup must appear at cursor position, follow scroll, not block editor interaction.

**Decision**: Reuse existing `SqlCodeEditor` `TextLayoutResult` → `getBoundingBox(cursorPos)` → `IntOffset` → `Popup` anchoring. Popup is non-focusable; editor retains focus for typing.

**Alternatives considered**:
- DropdownMenu → rejected: designed for menus, not free positioning
- Custom offset calculation → rejected: duplicates existing multi-cursor logic
- Bottom sheet → rejected: hides editor area, bad for keyboard users

**Rationale**: `getBoundingBox` already used for multi-cursor markers; battle-tested. Popup automatically follows scroll. Non-focusable design allows arrow navigation via editor's `onPreviewKeyEvent`.

**Consequences**:
- ✅ No new anchor logic needed
- ✅ Scroll tracking out-of-the-box
- ✅ Keyboard navigation via existing `onPreviewKeyEvent` hook
- ⚠️ Popup must consume arrow keys when visible to prevent editor cursor movement

---

### ADR 5: Schema Snapshot is Lazy-Loaded

**Context**: `GetColumnsUseCase` hits JDBC for each table. Databases can have hundreds of tables. Loading all columns upfront blocks editor open.

**Decision**: `LoadSchemaSnapshotUseCase` runs on editor mount IF `databaseName != null`. Loads tables immediately, columns lazily per-table on first completion request. Cached in ViewModel `StateFlow<SchemaSnapshot>`.

**Alternatives considered**:
- Load all columns upfront → rejected: blocks editor open for large DBs
- Load on first completion request → rejected: 150ms debounce already tight, adds latency

**Rationale**: Lazy per-table loading balances responsiveness (editor opens fast) with completion quality (columns available when needed). StateFlow pattern allows UI to react when columns load.

**Consequences**:
- ✅ Editor opens instantly even for large DBs
- ✅ First completion shows keywords + tables, columns arrive incrementally
- ✅ Schema cached for session (no re-fetch on subsequent completions)
- ⚠️ Refresh on DDL/USE requires re-run (detected via SqlTokenizer on executed SQL)

---

### ADR 6: Multi-Cursor Disables Completion

**Context**: Multi-cursor mode (`cursorPositions.isNotEmpty()`) is ambiguous for completion — which cursor should receive the inserted suggestion?

**Decision**: `if (cursorPositions.size > 0) { /* don't show popup */ }`. Auto-trigger ignores multi-cursor text. `Ctrl+Space` becomes no-op when multi-cursor active.

**Alternatives considered**:
- Insert at all cursors → rejected: unexpected behavior, breaks ranking (which cursor's prefix?)
- Insert at primary cursor → rejected: UX confusion (other cursors ignored)

**Rationale**: Multi-cursor is power-user mode; keeping primary path (single cursor) simple avoids ambiguity. Matches existing convention (Format clears cursors on apply).

**Consequences**:
- ✅ Clear UX rule: completion requires single cursor
- ✅ No ambiguity in insertion target
- ⚠️ Feature loss for multi-cursor users (acceptable — niche mode)

---

## 3. Component Specifications

### SqlKeywords.kt (NEW)

**Purpose**: Single source of truth for MySQL/MariaDB keyword list.

**Interface**:
```kotlin
package com.sphynxs.mydatabases.domain.editor

object SqlKeywords {
    val KEYWORDS: Set<String> = setOf(
        "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT",
        "OUTER", "CROSS", "FULL", "ON", "AND", "OR", "NOT", "IN",
        "EXISTS", "LIKE", "BETWEEN", "IS", "NULL", "TRUE", "FALSE",
        "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET",
        "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
        "CREATE", "TABLE", "ALTER", "DROP", "USE", "DATABASE",
        "UNION", "ALL", "DISTINCT", "AS", "CASE", "WHEN", "THEN",
        "ELSE", "END", "COUNT", "SUM", "AVG", "MIN", "MAX",
        // ... ~75 keywords total
    )
}
```

**Responsibilities**:
- Provide canonical keyword set for all consumers
- Keywords MUST be in UPPERCASE (normalized form)

**Dependencies**: None (pure object)

**Test Strategy**:
- `SqlKeywordsTest::keywords_notEmpty()`
- `SqlKeywordsTest::keywords_containsCanonicalSet()` (SELECT, FROM, WHERE, JOIN, INSERT, UPDATE, DELETE)
- `SqlKeywordsTest::keywords_allUppercase()`

---

### SqlFormatter.kt (NEW)

**Purpose**: Transform SQL text to UPPERCASE keywords + major clause newlines + 2-space indent.

**Interface**:
```kotlin
package com.sphynxs.mydatabases.domain.editor

object SqlFormatter {
    fun format(sql: String): String
}
```

**Responsibilities**:
- Tokenize via `SqlTokenizer`
- UPPERCASE tokens where `token.kind == KEYWORD`
- Insert newline before FROM/WHERE/JOIN/GROUP BY/ORDER BY/HAVING/LIMIT/UNION
- Insert 2-space indent after newline for ON/AND/OR subclauses
- Preserve STRING/COMMENT tokens verbatim (byte-for-byte)
- Preserve user projection formatting (no comma breaks)
- Trim trailing whitespace per line
- Collapse 3+ blank lines to 1
- Idempotent: `format(format(x)) == format(x)`

**Dependencies**: `SqlTokenizer`, `SqlKeywords`

**Test Strategy**: Unit tests with 12 format scenarios from spec.md:
- `format_simpleSelectWithWhere_producesExpectedLayout()`
- `format_innerJoinWithOnPredicate_indentsOnUnderJoin()`
- `format_nestedSubquery_uppercasesKeywordsWithoutDeepIndent()`
- `format_stringLiterals_preservedVerbatim()`
- `format_lineComment_preservedVerbatim()`
- `format_blockComment_preservedVerbatim()`
- `format_projectionList_keptOnOneLine()`
- `format_isIdempotent_acrossAllGoldenFixtures()` (property-based)

---

### SqlCompletionProvider.kt (NEW)

**Purpose**: Generate ranked, context-aware completion suggestions.

**Interface**:
```kotlin
package com.sphynxs.mydatabases.domain.editor

object SqlCompletionProvider {
    fun getSuggestions(
        prefix: String,
        context: String,  // Full SQL text
        cursorOffset: Int,
        schema: SchemaSnapshot?,
        limit: Int = 8
    ): List<CompletionSuggestion>
}
```

**Responsibilities**:
- Extract identifier prefix at cursor position
- Return `emptyList()` if cursor inside STRING/COMMENT token
- Detect last meaningful keyword before cursor (FROM/SELECT/WHERE/JOIN/UPDATE/INTO)
- Rank results:
  1. Context-biased kind ordering (TABLE first after FROM, COLUMN first after SELECT)
  2. Alphabetical within kind
- Filter by case-insensitive prefix match
- Cap at `limit` items (default 8)

**Dependencies**: `SqlKeywords`, `SqlTokenizer`, `SchemaSnapshot`, `CompletionSuggestion`

**Test Strategy**: Unit tests with 16 completion scenarios from spec.md:
- `suggest_prefixSEL_returnsSELECTFirst()`
- `suggest_afterFROM_ranksTablesFirst()`
- `suggest_afterSELECT_ranksColumnsFirst()`
- `suggest_insideString_returnsEmpty()`
- `suggest_insideComment_returnsEmpty()`
- `suggest_nullSchema_keywordsOnly()`
- `suggest_respectsLimit8()`
- Context ranking tests for FROM/JOIN/UPDATE/INTO/SELECT/WHERE/ON/comma

---

### CompletionSuggestion.kt (NEW)

**Purpose**: Data class representing a single completion suggestion.

**Interface**:
```kotlin
package com.sphynxs.mydatabases.domain.editor

data class CompletionSuggestion(
    val kind: SuggestionKind,
    val label: String,
    val insertText: String,
    val typeLabel: String? = null  // For COLUMN: "INT", "VARCHAR(255)", etc.
)

enum class SuggestionKind {
    KEYWORD,
    TABLE,
    COLUMN
}
```

**Responsibilities**:
- Immutable value object
- `label`: display text (e.g. "id : INT" for columns)
- `insertText`: actual text to insert on accept (e.g. "id")
- `typeLabel`: SQL type for COLUMN suggestions (from `Column.type`)

**Dependencies**: None (pure data class)

**Test Strategy**: No dedicated tests (data class). Tested via `SqlCompletionProviderTest`.

---

### SchemaSnapshot.kt (NEW)

**Purpose**: Immutable snapshot of database schema for completion context.

**Interface**:
```kotlin
package com.sphynxs.mydatabases.domain.editor

data class SchemaSnapshot(
    val tables: List<String>,
    val columnsByTable: Map<String, List<ColumnInfo>>
)

data class ColumnInfo(
    val name: String,
    val type: String
)
```

**Responsibilities**:
- Immutable schema state
- `tables`: sorted list of table names
- `columnsByTable`: lazy-loaded columns per table

**Dependencies**: None (pure data class)

**Test Strategy**: No dedicated tests (data class). Tested via `LoadSchemaSnapshotUseCase` and `SqlCompletionProviderTest`.

---

### LoadSchemaSnapshotUseCase.kt (NEW)

**Purpose**: Load database schema for completion context.

**Interface**:
```kotlin
package com.sphynxs.mydatabases.domain.usecase

class LoadSchemaSnapshotUseCase @Inject constructor(
    private val getTablesUseCase: GetTablesUseCase,
    private val getColumnsUseCase: GetColumnsUseCase
) {
    suspend operator fun invoke(databaseName: String): Result<SchemaSnapshot>
}
```

**Responsibilities**:
- Call `getTablesUseCase(databaseName)` once (immediate)
- Build `SchemaSnapshot` with tables populated, columns empty
- Lazy column loading triggered by ViewModel on first completion request per table
- Return `Result.failure` if JDBC error (ViewModel degrades to keywords-only)

**Dependencies**: `GetTablesUseCase`, `GetColumnsUseCase`, `SchemaSnapshot`

**Test Strategy**:
- Unit: `LoadSchemaSnapshotUseCaseTest::invoke_validDatabase_returnsTables()`
- Unit: `LoadSchemaSnapshotUseCaseTest::invoke_invalidDatabase_returnsFailure()`
- Unit: `LoadSchemaSnapshotUseCaseTest::invoke_jdbcError_returnsFailure()`

---

### CompletionPopup.kt (NEW)

**Purpose**: Render completion suggestions as anchored popup.

**Interface**:
```kotlin
package com.sphynxs.mydatabases.ui.screens.queryeditor.components

@Composable
fun CompletionPopup(
    suggestions: List<CompletionSuggestion>,
    selectedIndex: Int,
    anchorOffset: IntOffset,
    onSuggestionClick: (CompletionSuggestion) -> Unit,
    onDismiss: () -> Unit
)
```

**Responsibilities**:
- Render `Popup` anchored at `anchorOffset` (from `TextLayoutResult.getBoundingBox`)
- `LazyColumn` with 8 visible rows, scrollable for rest
- Highlight selected row via `selectedIndex`
- `onSuggestionClick` on tap
- `onDismiss` on Esc or outside click
- Accessibility: `contentDescription`, 48dp tap targets

**Dependencies**: Compose, `CompletionSuggestion`

**Test Strategy**:
- E2E: `CompletionPopupTest::popup_rendersAllSuggestions()`
- E2E: `CompletionPopupTest::popup_highlightsSelectedIndex()`
- E2E: `CompletionPopupTest::popup_tapRow_invokesOnClick()`
- E2E: `CompletionPopupTest::popup_esc_invokesOnDismiss()`

---

### QueryEditorViewModel.kt (MODIFIED)

**Purpose**: Extend with format, completion, schema state.

**NEW Interface**:
```kotlin
class QueryEditorViewModel @Inject constructor(
    private val executeBatchStatementsUseCase: ExecuteBatchStatementsUseCase,
    private val loadSchemaSnapshotUseCase: LoadSchemaSnapshotUseCase  // NEW
) : ViewModel() {
    
    // NEW: Schema snapshot for completion
    private val _schemaSnapshot = MutableStateFlow<SchemaSnapshot?>(null)
    val schemaSnapshot: StateFlow<SchemaSnapshot?> = _schemaSnapshot.asStateFlow()
    
    // NEW: Load schema on screen mount
    fun loadSchema(databaseName: String?) {
        if (databaseName == null) {
            _schemaSnapshot.value = null
            return
        }
        viewModelScope.launch {
            loadSchemaSnapshotUseCase(databaseName)
                .onSuccess { _schemaSnapshot.value = it }
                .onFailure { /* log error, degrade to keywords-only */ }
        }
    }
    
    // NEW: Format SQL text
    suspend fun formatSql(currentText: String): String = withContext(Dispatchers.Default) {
        SqlFormatter.format(currentText)
    }
    
    // NEW: Get completion suggestions
    fun getSuggestions(prefix: String, context: String, cursorOffset: Int): List<CompletionSuggestion> {
        return SqlCompletionProvider.getSuggestions(
            prefix, context, cursorOffset, _schemaSnapshot.value
        )
    }
    
    // MODIFIED: executeStatements now detects DDL/USE
    fun executeStatements(sql: String) {
        // ... existing code ...
        
        // NEW: After successful execution, detect DDL/USE
        detectSchemaMutation(sql)?.let { newDatabase ->
            loadSchema(newDatabase)
        }
    }
    
    private fun detectSchemaMutation(sql: String): String? {
        val tokens = SqlTokenizer.tokenize(sql)
        val keywords = tokens.filter { it.kind == TokenKind.KEYWORD }.map { it.text.uppercase() }
        
        // CREATE/ALTER/DROP TABLE → refresh current DB
        if (keywords.any { it in setOf("CREATE", "ALTER", "DROP") } && "TABLE" in keywords) {
            return _currentDatabase  // Refresh current
        }
        
        // USE <db> → switch database
        if ("USE" in keywords) {
            return tokens.firstOrNull { it.kind == TokenKind.IDENTIFIER }?.text
        }
        
        return null
    }
}
```

**Responsibilities**:
- Expose `schemaSnapshot: StateFlow<SchemaSnapshot?>` for UI
- `loadSchema(databaseName)` on screen mount
- `formatSql(text)` wraps `SqlFormatter.format` on Dispatchers.Default
- `getSuggestions(...)` delegates to `SqlCompletionProvider`
- DDL/USE detection triggers schema reload

**Dependencies**: `SqlFormatter`, `SqlCompletionProvider`, `LoadSchemaSnapshotUseCase`, existing dependencies

**Test Strategy**:
- Unit: `QueryEditorViewModelTest::loadSchema_validDatabase_populatesStateFlow()`
- Unit: `QueryEditorViewModelTest::loadSchema_nullDatabase_clearsStateFlow()`
- Unit: `QueryEditorViewModelTest::formatSql_validSql_returnsFormatted()`
- Unit: `QueryEditorViewModelTest::getSuggestions_delegatesToProvider()`
- Unit: `QueryEditorViewModelTest::executeStatements_ddl_triggersSchemaReload()`

---

### SqlCodeEditor.kt (MODIFIED)

**Purpose**: Extend with popup anchoring, navigation routing.

**NEW Interface**:
```kotlin
@Composable
fun SqlCodeEditor(
    // ... existing params ...
    showCompletionPopup: Boolean,
    completionSuggestions: List<CompletionSuggestion>,
    selectedSuggestionIndex: Int,
    onCompletionAccept: (CompletionSuggestion) -> Unit,
    onCompletionDismiss: () -> Unit,
    onCompletionNavigate: (direction: Int) -> Unit  // +1 for Down, -1 for Up
)
```

**Responsibilities**:
- Existing: BasicTextField, tokenization, syntax highlighting
- NEW: Anchor `CompletionPopup` at cursor via `textLayoutResult.getBoundingBox(cursorPos)`
- NEW: Route ↓↑ Enter Tab Esc keys when `showCompletionPopup == true`
- NEW: Auto-trigger debounce (150ms) on text change
- NEW: Multi-cursor gating (disable completion when `cursorPositions.isNotEmpty()`)

**Dependencies**: Existing + `CompletionPopup`

**Test Strategy**:
- E2E: `SqlCodeEditorTest::popup_anchorsAtCursor()`
- E2E: `SqlCodeEditorTest::popup_followsScroll()`
- E2E: `SqlCodeEditorTest::arrowKeys_routedToPopup_whenVisible()`
- E2E: `SqlCodeEditorTest::autoTrigger_debounces150ms()`

---

### QueryEditorScreen.kt (MODIFIED)

**Purpose**: Wire Format button, shortcuts, databaseName plumbing.

**NEW Interface**:
```kotlin
@Composable
fun QueryEditorScreen(
    connectionId: String,
    initialSql: String,
    databaseName: String?,  // NEW: from WorkspaceOverlay
    // ... existing params ...
)
```

**Responsibilities**:
- Existing: toolbar, editor, results display
- NEW: Format toolbar button (left pill, after Redo)
- NEW: `Ctrl+Shift+F` → formatSql()
- NEW: `Ctrl+Space` → trigger completion
- NEW: Pass `databaseName` to ViewModel.loadSchema()
- NEW: Manage completion popup state (show, suggestions, selectedIndex)

**Dependencies**: Existing + `CompletionPopup`

**Test Strategy**:
- E2E: `QueryEditorScreenTest::formatButton_click_formatsText()`
- E2E: `QueryEditorScreenTest::ctrlShiftF_formatsText()`
- E2E: `QueryEditorScreenTest::ctrlSpace_opensPopup()`
- E2E: `QueryEditorScreenTest::formatThenUndo_restoresOriginal()`

---

## 4. State Management

### ViewModel StateFlows

| StateFlow | Type | Purpose | Initialization | Updates |
|-----------|------|---------|----------------|---------|
| `schemaSnapshot` | `StateFlow<SchemaSnapshot?>` | Current DB schema for completion | `null` | `loadSchema(databaseName)` on mount, DDL/USE detection |

### Screen State (QueryEditorScreen.kt)

| State Variable | Type | Purpose | Initialization | Updates |
|----------------|------|---------|----------------|---------|
| `showCompletionPopup` | `Boolean` | Popup visibility | `false` | Auto-trigger, `Ctrl+Space`, Esc, accept |
| `completionSuggestions` | `List<CompletionSuggestion>` | Current suggestions | `emptyList()` | Debounced flow from text changes |
| `selectedSuggestionIndex` | `Int` | Arrow navigation cursor | `0` | ↓↑ keys, wraps at ends |
| `lastDismissedToken` | `String?` | Esc-remembers-token gating | `null` | Esc sets current token, cleared on token change |

### State Transitions: Format

```
Idle
  ↓ [Format button click / Ctrl+Shift+F]
pushHistory(currentSnapshot)
  ↓
Dispatchers.Default { SqlFormatter.format(text) }
  ↓
Update sqlText.value
  ↓
pushHistory(newSnapshot)
  ↓
Idle [Ctrl+Z → undo to pre-format]
```

### State Transitions: Completion Popup

```
Hidden
  ↓ [Auto-trigger: prefix ≥ 2 chars, 150ms debounce]
  ↓ [Manual trigger: Ctrl+Space]
Visible (selectedIndex = 0)
  ↓ [↓ key] → selectedIndex++ (wrap)
  ↓ [↑ key] → selectedIndex-- (wrap)
  ↓ [Enter / Tab] → accept → Hidden
  ↓ [Esc] → dismiss → Hidden (set lastDismissedToken)
  ↓ [non-identifier char] → dismiss → Hidden
```

---

## 5. Testing Strategy

### Unit Tests (JVM, no Android dependencies)

| Test File | Coverage | Key Tests |
|-----------|----------|-----------|
| `SqlKeywordsTest.kt` | SqlKeywords | Non-empty, canonical set, all uppercase |
| `SqlFormatterTest.kt` | SqlFormatter | 12 scenarios from spec (golden files, idempotency) |
| `SqlCompletionProviderTest.kt` | SqlCompletionProvider | 16 scenarios from spec (ranking, filtering, context) |
| `EditorShortcutsTest.kt` | EditorShortcuts | Ctrl+Shift+F → Format, Ctrl+Space → TriggerCompletion |
| `LoadSchemaSnapshotUseCaseTest.kt` | LoadSchemaSnapshotUseCase | Valid DB, invalid DB, JDBC error |
| `QueryEditorViewModelTest.kt` | QueryEditorViewModel | Schema load, format, suggestions, DDL detection |

### E2E Tests (androidTest, Compose)

| Test File | Coverage | Key Tests |
|-----------|----------|-----------|
| `QueryEditorScreenTest.kt` | Format integration | Button click, Ctrl+Shift+F, undo restores, multi-cursor clears |
| `CompletionPopupTest.kt` | Popup interactions | Auto-trigger, Ctrl+Space, arrow nav, Enter/Tab/Esc, tap-to-accept |
| `SqlCodeEditorTest.kt` | Popup anchoring | Cursor position, scroll tracking, debounce 150ms |

### Manual Smoke Tests

- [ ] Open editor with null databaseName → completion shows keywords only
- [ ] Open editor with valid databaseName → completion shows tables + columns
- [ ] Type `SEL` → popup appears with SELECT
- [ ] Arrow down/up → selection wraps
- [ ] Enter → inserts suggestion
- [ ] Esc → dismisses, re-typing same token does NOT re-trigger
- [ ] Format button → UPPERCASE keywords, newlines before major clauses
- [ ] Ctrl+Shift+F → same as button
- [ ] Format → Ctrl+Z → restores original
- [ ] Multi-cursor active → completion does NOT trigger

---

## 6. Sequence Diagrams

### Format Flow

```
┌────────┐                ┌──────────┐              ┌───────────────┐         ┌──────────┐
│  User  │                │  Screen  │              │   ViewModel   │         │ Formatter│
└───┬────┘                └────┬─────┘              └───────┬───────┘         └────┬─────┘
    │                          │                            │                      │
    │ Click Format button      │                            │                      │
    ├─────────────────────────>│                            │                      │
    │                          │ formatSql()                │                      │
    │                          ├───────────────────────────>│                      │
    │                          │                            │ pushHistory(current) │
    │                          │                            ├──────────┐           │
    │                          │                            │          │           │
    │                          │                            │<─────────┘           │
    │                          │                            │                      │
    │                          │                            │ format(sql)          │
    │                          │                            ├─────────────────────>│
    │                          │                            │                      │
    │                          │                            │     formatted        │
    │                          │                            │<─────────────────────┤
    │                          │                            │                      │
    │                          │    formatted text          │                      │
    │                          │<───────────────────────────┤                      │
    │                          │                            │                      │
    │                          │ update sqlText             │                      │
    │                          ├────────────┐               │                      │
    │                          │            │               │                      │
    │                          │<───────────┘               │                      │
    │                          │                            │ pushHistory(new)     │
    │                          │                            ├──────────┐           │
    │                          │                            │          │           │
    │                          │                            │<─────────┘           │
    │                          │                            │                      │
    │  Formatted text visible  │                            │                      │
    │<─────────────────────────┤                            │                      │
    │                          │                            │                      │
```

### Completion Auto-Trigger Flow

```
┌────────┐          ┌──────────┐        ┌───────────────┐      ┌──────────┐
│  User  │          │  Editor  │        │   ViewModel   │      │ Provider │
└───┬────┘          └────┬─────┘        └───────┬───────┘      └────┬─────┘
    │                    │                      │                   │
    │ Type "SEL"         │                      │                   │
    ├───────────────────>│                      │                   │
    │                    │ onValueChange        │                   │
    │                    ├──────────┐           │                   │
    │                    │          │           │                   │
    │                    │<─────────┘           │                   │
    │                    │                      │                   │
    │                    │ Extract prefix="SEL" │                   │
    │                    │ if len>=2 && !string │                   │
    │                    ├──────────┐           │                   │
    │                    │          │           │                   │
    │                    │<─────────┘           │                   │
    │                    │                      │                   │
    │                    │ launch debounced(150ms)                  │
    │                    ├──────────────────────────────────────┐   │
    │                    │                      │               │   │
    │                    │                      │               │   │
    │   [150ms passes]   │                      │               │   │
    │                    │                      │               │   │
    │                    │                      │<──────────────┘   │
    │                    │  getSuggestions()    │                   │
    │                    │                      ├──────────────────>│
    │                    │                      │                   │
    │                    │                      │ getSuggestions()  │
    │                    │                      │ - detect context  │
    │                    │                      │ - filter prefix   │
    │                    │                      │ - rank + cap 8    │
    │                    │                      │                   │
    │                    │                      │  [SELECT, ...]    │
    │                    │                      │<──────────────────┤
    │                    │  suggestions         │                   │
    │                    │<─────────────────────┤                   │
    │                    │                      │                   │
    │                    │ showPopup = true     │                   │
    │                    ├──────────┐           │                   │
    │                    │          │           │                   │
    │                    │<─────────┘           │                   │
    │                    │                      │                   │
    │  Popup visible     │                      │                   │
    │  [SELECT]          │                      │                   │
    │<───────────────────┤                      │                   │
    │                    │                      │                   │
```

### Completion Accept Flow

```
┌────────┐          ┌──────────┐        ┌───────────────┐
│  User  │          │  Editor  │        │   Popup       │
└───┬────┘          └────┬─────┘        └───────┬───────┘
    │                    │                      │
    │  Popup visible     │                      │
    │  [SELECT] selected │                      │
    │                    │                      │
    │ Press ↓            │                      │
    ├───────────────────>│                      │
    │                    │ onPreviewKeyEvent    │
    │                    ├──────────┐           │
    │                    │ consume  │           │
    │                    │<─────────┘           │
    │                    │                      │
    │                    │ selectedIndex++      │
    │                    ├─────────────────────>│
    │                    │                      │
    │  [FROM] selected   │                      │
    │<───────────────────┴──────────────────────┤
    │                    │                      │
    │ Press Enter        │                      │
    ├───────────────────>│                      │
    │                    │ onPreviewKeyEvent    │
    │                    ├──────────┐           │
    │                    │ consume  │           │
    │                    │<─────────┘           │
    │                    │                      │
    │                    │ onAccept(FROM)       │
    │                    ├─────────────────────>│
    │                    │                      │
    │                    │ insert "FROM"        │
    │                    │ replace prefix       │
    │                    ├──────────┐           │
    │                    │          │           │
    │                    │<─────────┘           │
    │                    │                      │
    │                    │ showPopup = false    │
    │                    ├─────────────────────>│
    │                    │                      │
    │  "FROM" inserted   │                      │
    │  Popup hidden      │                      │
    │<───────────────────┴──────────────────────┤
    │                    │                      │
```

---

## 7. PR Breakdown (3 PRs)

### PR #1: SQL Formatter (~300 LOC)

**Purpose**: Ship Format end-to-end (toolbar + shortcut + tests).

**Files**:
- `domain/editor/SqlKeywords.kt` (NEW, ~80 lines) — keyword set extraction
- `domain/editor/SqlFormatter.kt` (NEW, ~120 lines) — pure formatter
- `domain/editor/ShortcutAction.kt` (MODIFIED, +1 line) — add `Format` case
- `domain/editor/EditorShortcuts.kt` (MODIFIED, +2 lines) — map `Ctrl+Shift+F`
- `ui/screens/queryeditor/QueryEditorViewModel.kt` (MODIFIED, +15 lines) — `formatSql()` method
- `ui/screens/queryeditor/QueryEditorScreen.kt` (MODIFIED, +20 lines) — Format toolbar button, shortcut wiring
- `ui/screens/queryeditor/components/SqlTokenizer.kt` (MODIFIED, ~10 lines) — build keyword regex from `SqlKeywords`
- `res/values/strings.xml` (MODIFIED, +2 strings) — `format_button_label`, `format_button_description`
- `res/values-es/strings.xml` (MODIFIED, +2 strings)
- `test/.../editor/SqlKeywordsTest.kt` (NEW, ~40 lines)
- `test/.../editor/SqlFormatterTest.kt` (NEW, ~100 lines) — 12 scenarios
- `test/.../editor/EditorShortcutsTest.kt` (MODIFIED, +10 lines) — `Ctrl+Shift+F` test
- `androidTest/.../QueryEditorScreenTest.kt` (MODIFIED, +30 lines) — E2E format tests

**Total**: ~300 LOC (within review budget)

**Dependencies**: None (self-contained)

**Acceptance Criteria**:
- [ ] Toolbar Format button visible, enabled when text non-blank
- [ ] Ctrl+Shift+F formats SQL
- [ ] Keywords UPPERCASE, newlines before major clauses
- [ ] Ctrl+Z undoes format atomically
- [ ] 12 unit tests green, 3 E2E tests green

---

### PR #2: Completion Schema + Provider (~400 LOC)

**Purpose**: Schema loading + suggestion engine (no UI yet — staged).

**Files**:
- `domain/editor/CompletionSuggestion.kt` (NEW, ~20 lines) — data class
- `domain/editor/SchemaSnapshot.kt` (NEW, ~15 lines) — data class
- `domain/editor/SqlCompletionProvider.kt` (NEW, ~150 lines) — pure provider
- `domain/usecase/LoadSchemaSnapshotUseCase.kt` (NEW, ~60 lines)
- `ui/screens/queryeditor/QueryEditorViewModel.kt` (MODIFIED, +50 lines) — schema StateFlow, `getSuggestions()`, DDL detection
- `ui/screens/queryeditor/QueryEditorScreen.kt` (MODIFIED, +5 lines) — accept `databaseName: String?` param
- `ui/screens/workspace/WorkspaceOverlay.kt` (MODIFIED, +5 lines) — pass `databaseName` to screen
- `test/.../editor/SqlCompletionProviderTest.kt` (NEW, ~120 lines) — 16 scenarios
- `test/.../usecase/LoadSchemaSnapshotUseCaseTest.kt` (NEW, ~40 lines)
- `test/.../QueryEditorViewModelTest.kt` (MODIFIED, +40 lines) — schema load tests

**Total**: ~400 LOC (within review budget)

**Dependencies**: PR #1 (SqlKeywords)

**Acceptance Criteria**:
- [ ] `LoadSchemaSnapshotUseCase` loads tables immediately
- [ ] Schema StateFlow populates on editor mount if `databaseName != null`
- [ ] `SqlCompletionProvider.getSuggestions()` returns correct ranked suggestions
- [ ] Context ranking: TABLE first after FROM, COLUMN first after SELECT
- [ ] 16 unit tests green for provider, 3 tests green for use case

**NOTE**: This PR ships invisible code — no UX change yet. Provider fully tested, ready for UI wiring in PR #3.

---

### PR #3: Completion Popup UI (~550 LOC)

**Purpose**: Wire completion popup, auto-trigger, keyboard navigation.

**Files**:
- `ui/screens/queryeditor/components/CompletionPopup.kt` (NEW, ~150 lines) — Popup + LazyColumn UI
- `ui/screens/queryeditor/components/SqlCodeEditor.kt` (MODIFIED, +100 lines) — popup anchoring, navigation routing, auto-trigger debounce
- `ui/screens/queryeditor/QueryEditorScreen.kt` (MODIFIED, +80 lines) — popup state management, arrow navigation, accept/dismiss
- `domain/editor/ShortcutAction.kt` (MODIFIED, +1 line) — add `TriggerCompletion` case
- `domain/editor/EditorShortcuts.kt` (MODIFIED, +2 lines) — map `Ctrl+Space`
- `res/values/strings.xml` (MODIFIED, +6 strings) — `completion_empty`, `completion_loading_schema`, `completion_keywords_only`, `completion_aria_label`, etc.
- `res/values-es/strings.xml` (MODIFIED, +6 strings)
- `androidTest/.../CompletionPopupTest.kt` (NEW, ~150 lines) — popup interaction tests
- `androidTest/.../SqlCodeEditorTest.kt` (MODIFIED, +60 lines) — anchoring, debounce tests

**Total**: ~550 LOC (within review budget)

**Dependencies**: PR #2 (CompletionSuggestion, SqlCompletionProvider, schema StateFlow)

**Acceptance Criteria**:
- [ ] Typing `SEL` shows popup with SELECT (auto-trigger)
- [ ] Ctrl+Space opens popup with empty prefix (manual trigger)
- [ ] Arrow ↓↑ navigation wraps at ends
- [ ] Enter/Tab accepts, inserts suggestion
- [ ] Esc dismisses, remembers token (no re-trigger same token)
- [ ] Popup does NOT appear inside strings/comments
- [ ] Popup does NOT appear when multi-cursor active
- [ ] Popup anchored at cursor, follows scroll
- [ ] 10+ E2E tests green

---

## 8. Open Questions / Risks

### Open Questions

**Q1: databaseName plumbing path**  
**Status**: RESOLVED in proposal phase.  
**Decision**: Plumb `databaseName: String?` from `WorkspaceOverlay → QueryEditorScreen → QueryEditorViewModel.loadSchema()`. Existing WorkspaceOverlay already holds `card.databaseName`.  
**Impact**: PR #2 modifies WorkspaceOverlay signature (+1 param). Single internal caller, no backward compatibility concern.

**Q2: Column type display format**  
**Status**: RESOLVED in proposal phase.  
**Decision**: Show types as `name : TYPE` (e.g. `id : INT`) on COLUMN suggestions.  
**Impact**: `CompletionSuggestion` has `typeLabel: String?` field. CompletionPopup renders `label` (pre-formatted by provider).

**Q3: Projection list formatting**  
**Status**: RESOLVED in proposal phase.  
**Decision**: Keep `SELECT a, b, c` on one line (preserve user formatting). No comma breaks in v1.  
**Impact**: SqlFormatter preserves projection list intra-whitespace. Spec scenario 7 enforces this.

**Q4: PR split strategy**  
**Status**: RESOLVED.  
**Decision**: 3 PRs — Format (PR #1), Schema + Provider (PR #2), Popup UI (PR #3). Each under review budget.  
**Impact**: PR #2 ships invisible code (provider fully tested, no UX until PR #3).

---

### Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| **databaseName null on main-screen entry** | High | Completion degrades to keywords-only | ✅ Graceful degradation tested (spec scenario 20) |
| **Keyword list drift** | Medium | Formatter/provider out of sync with tokenizer | ✅ SqlKeywords extraction + non-drift test |
| **Multi-cursor × completion ambiguity** | Medium | UX confusion | ✅ Hard disable when cursors active (spec scenario 26) |
| **Auto-popup intrusive** | Medium | Annoys fast typers | ✅ 2-char min + 150ms debounce + Esc-remembers-token |
| **Soft keyboard Ctrl combos unreliable** | High | Shortcuts don't work | ✅ Toolbar Format button canonical, auto-trigger for completion |
| **PR #2 ships invisible code** | Medium | Reviewer confusion | ✅ PR description flags staging, 100% test coverage proves correctness |
| **Stale schema after external DDL** | Low | Completion suggests dropped tables | ✅ Refresh on in-editor DDL, manual refresh in follow-up |
| **Schema cost on huge DBs** | Low | Slow editor open | ✅ Lazy per-table column load, only on completion request |
| **Popup focus stealing** | Medium | Editor loses typing focus | ✅ Popup is non-focusable, navigation via onPreviewKeyEvent |
| **Format reflow invalidates multi-cursor** | High | Cursor positions meaningless | ✅ Format clears cursors (spec scenario 12) |

---

## Summary

This design delivers SQL formatting and context-aware completion through:
- **Pure domain layer** (SqlFormatter, SqlCompletionProvider, SqlKeywords) — 100% JVM testable
- **ViewModel orchestration** (format on Dispatchers.Default, schema StateFlow, DDL detection)
- **Compose UI** (Format toolbar button, CompletionPopup anchored at cursor, keyboard navigation)
- **3 reviewable PRs** (Format ~300 LOC, Schema + Provider ~400 LOC, Popup UI ~550 LOC)

All 30 spec scenarios covered. All ADRs documented with rationale. All risks mitigated or accepted.

**Next Step**: Tasks (sdd-tasks) to break down implementation into atomic commits.
