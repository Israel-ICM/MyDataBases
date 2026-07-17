# Exploration: create-table

> SDD phase: explore · Change: `create-table` · Artifact store: openspec
> Feature: A "Create Table" panel opened as a `ModalBottomSheet` inside a database, with a
> table-name field at the top, a dynamic list of column/field definitions, a "+ Add field"
> button that opens a **nested** `ModalBottomSheet` field-definition form, and an OK action
> that appends the defined field to the list in the first sheet.

---

## 1. Current State

### 1.1 Reference pattern — "Add Database" ModalBottomSheet (the direct template)
The user explicitly wants the same container pattern as "Add Database". It is fully implemented and is the canonical reference:

- **Form content composable**: `AddDatabaseFormContent(...)` in
  `ui/screens/databases/AddDatabaseScreen.kt`. It is **pure sheet content** (no `Scaffold`, no `ModalBottomSheet` of its own) — a scrollable `Column` with an iOS-style large title, an `IOSGroupedCard` holding fields, an `IOSButton` submit, and a loading overlay. This is the exact shape a `CreateTableFormContent` should follow.
- **Sheet host**: `DatabasesListScreen.kt` (lines ~196-241) renders the actual `ModalBottomSheet` when `showAddDatabaseSheet == true`. Key styling to mirror:
  - `sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)`
  - `containerColor = LocalDesignTokens.current.backgroundPrimary`
  - `sheetMaxWidth = 10000.dp`
  - `scrimColor = LocalDesignTokens.current.backdropScrim`
  - `tonalElevation = 16.dp`
  - Inner `Scaffold(containerColor = Color.Transparent)` wrapping a `Box` padded with `top = statusBarHeightDp` (real status bar height computed via `status_bar_height` dimen resource).
- **ViewModel**: `AddDatabaseViewModel` (`@HiltViewModel`, injected via `hiltViewModel()`), exposes `StateFlow` load/submit states as sealed classes (`CharsetLoadState`, `CollationLoadState`, `CreateDatabaseState`) and a `createDatabase(...)` action. This is the pattern for a future `CreateTableViewModel`.
- **State holder / UI states**: sealed classes live alongside the ViewModel in the same file.

### 1.2 Modal trigger wiring (how the sheet is opened)
Two distinct mechanisms exist — pick the right one for "create table":

- **Modal-action pattern (bottom-nav driven)** — used by Add Database (observations #1922/#1923):
  - `NavigationDestinations.kt` marks a destination `isModal = true`.
  - `AdaptiveNavigationScaffold` intercepts clicks on modal items and calls `onModalAction(destinationId)` instead of `navController.navigate(...)`.
  - `MyDataBasesNavHost.kt` holds `var showAddDatabaseSheet by remember { mutableStateOf(false) }` and flips it in `onModalAction { "add_database" -> showAddDatabaseSheet = true }`, then passes it down to `DatabasesListScreen`.
- **In-screen sheet state** — used by `WorkspaceManager.openQueryCard(...)` for "new_query", and by `FolderFormSheet` (see 1.4). Sheet visibility is local `remember { mutableStateOf(...) }` state inside the screen that owns the "+" affordance.

**Contextually, "create table" belongs to a database's Tables screen**, not the databases list. The natural trigger is `TablesListScreen` (`ui/screens/tables/TablesListScreen.kt`), which currently lists tables for a `databaseName` but has **no add affordance**. There is a `NavigationContext.InsideConnection` context and contextual destinations, but the current live table list is `Routes.TableList` reached from `DatabasesListScreen.onNavigateToTables`. Decision for proposal phase: whether the "+ Create Table" trigger is (a) an in-screen FAB/header button on `TablesListScreen` with local sheet state (simplest, matches `FolderFormSheet`), or (b) a bottom-nav modal action mirroring `add_database`.

### 1.3 Nested / stacked ModalBottomSheet handling
**There is currently NO stacked/nested `ModalBottomSheet` in the codebase.** The closest analogues:
- `IOSDropdownField` opens a **`Dialog`** (not a bottom sheet) on top of the sheet to avoid IME/focus routing issues with `Popup`/`DropdownMenu` (explicit comment in `IOSDropdownField.kt` lines 147-153). This is a strong signal: **Compose has known focus/IME problems with nested overlays inside a `ModalBottomSheet`.**
- The nested "field definition form" (requirement #4) is a genuinely new UI composition. Two viable renderings: **(A)** second `ModalBottomSheet` stacked over the first (true to the user's request, but higher risk — each `ModalBottomSheet` uses its own dialog window; managing two `sheetState`s + dismiss ordering + IME is the tricky part), or **(B)** a full-screen `Dialog` with sheet-like styling (consistent with the existing `IOSDropdownField` decision and lower risk). The user asked for a bottom sheet; the proposal must weigh fidelity vs. the documented IME risk.

### 1.4 Other sheet reference
`ui/components/folders/FolderFormSheet.kt` is a **self-contained** `ModalBottomSheet` composable (owns its own `sheetState`, title, single text field, Save/Cancel `IOSButton`s, validation `isValid = name.isNotBlank() && name.length <= 50`, calls `onSave` then `onDismiss`). This is the simplest reusable pattern for the nested field form if we go with an owned-sheet component.

### 1.5 The "+ Add field" button — DOES NOT EXIST YET
Exhaustive search (`AddField`, `AddColumn`, `add_field`, `+ Agregar`, `onAddField`, `AddParam`, `DynamicList`, `AddItem`, etc.) across `app/src/main/java` returned **zero matches**. The SSH tunneling work (observation #2030) referenced in the brief is `SSHTunnelSection` in `ui/screens/connections/AdvancedConnectionSections.kt` — it is a **static** toggle + fixed fields section, NOT a dynamic "add item to a list" control. **Conclusion: the "+ Add field" button the user remembers does not exist in this codebase.** It must be built new. Building blocks that DO exist to compose it:
- `IOSButton` (`IOSButtonStyle.Primary/Secondary/Destructive`, full-width, rounded 12.dp) — a `Secondary` style button labeled "+ Add field" is the closest match.
- `IOSGroupedCard`, `IOSTextField`, `IOSDropdownField`, and a `Switch` (used in `SSHTunnelSection`) cover every field control the nested form needs.

### 1.6 Domain & persistence layer
- **Models**: `core/database/models/Table.kt` (`data class Table` — read-only metadata: name, database, type, engine, rowCount…) and `core/database/models/Column.kt` (`data class Column(name, type: String, nullable, key: ColumnKey, default, extra, comment)` + `enum ColumnKey { PRIMARY, UNIQUE, MULTIPLE, NONE }`). These are **read models** produced by metadata readers; they do NOT model the *creation* inputs the user's form needs (Length, Decimals, Virtual are absent). A new **input/draft model** (e.g. `ColumnDefinition` / `NewColumnDraft`) is required.
- **DDL execution is FEASIBLE and already precedented.** `CreateDatabaseUseCase` composes a `CREATE DATABASE …` string and runs it via `DatabaseRepository.executeUpdate(sql, emptyList())`. A `CreateTableUseCase` can follow the identical shape: validate identifiers against `^[A-Za-z0-9_]{1,64}$`, build a `CREATE TABLE` DDL string, call `repository.executeUpdate(...)`, map `Result<Int>` → `Result<Unit>`.
- **Repository & engine**: `DatabaseRepository.executeUpdate(query, params)` → `DatabaseEngine.executeUpdate(...)`. Concrete engines: `MySQLEngine`, `MariaDBEngine` (PostgreSQL/SQLite in `DatabaseType` but no engine files present — only MySQL/MariaDB engines exist). So real table creation currently works only against MySQL/MariaDB servers.
- **Room / local persistence**: `data/local/` persists **connections and folders only** (`ConnectionEntity`, `ConnectionDao`, `FolderDao`, `AppDatabase`). There is **no** Room entity for user databases/tables/columns — schema objects are always read live from the target server. A "create table" feature therefore persists to the **remote DB via DDL**, not locally. (Open question for proposal: is v1 "compose + execute real DDL" or "in-memory form only / preview SQL"? The Add Database precedent executes real DDL, so parity suggests real execution.)

### 1.7 Type ("Tipo") dropdown source
- `DatabaseType` enum = engine types (MySQL/MariaDB/PostgreSQL/SQLite) — **NOT** column data types. There is **no existing SQL column-type enum/list** in the codebase.
- The "Tipo" dropdown needs a **new** list of SQL column data types (INT, VARCHAR, TEXT, DATETIME, DECIMAL, BOOLEAN, etc.). Since only MySQL/MariaDB engines are wired, v1 can ship a MySQL/MariaDB type list. Length/Decimals relevance is type-dependent (VARCHAR→length; DECIMAL→length+decimals; INT→optional display width; TEXT/DATETIME→neither).

### 1.8 i18n status
- All 10 required language folders exist: `values`, `values-es`, `values-ar`, `values-de`, `values-fr`, `values-hi`, `values-ja`, `values-pt-rBR`, `values-ru`, `values-zh-rCN`.
- Naming convention (from `values/strings.xml`): feature-prefixed keys — `add_database_title`, `add_database_field_name`, `add_database_field_name_hint`, `add_database_button_create`, `add_database_creating`; common actions `action_save`, `action_cancel`; column read labels `column_type`, `column_nullable`, `column_key`. **New strings should follow `create_table_*` / `field_definition_*` prefixes** (e.g. `create_table_title`, `create_table_field_name_hint`, `create_table_add_field`, `field_def_name`, `field_def_type`, `field_def_length`, `field_def_decimals`, `field_def_nullable`, `field_def_virtual`, `field_def_key`, `field_def_comment`, `action_ok`). Every new string MUST be added to **all 10** `strings.xml` files (android-dev skill: mandatory).
- Gotcha found: `TablesListScreen` currently hardcodes `ScreenTitle(title = "Tables", ...)` and `FolderFormSheet` hardcodes some validation strings — pre-existing i18n debt; not in scope but relevant if the trigger lives on `TablesListScreen`.

---

## 2. Affected Areas

- `ui/screens/tables/TablesListScreen.kt` — most likely host for the "+ Create Table" trigger (currently no add affordance); would render the first `ModalBottomSheet`.
- `ui/screens/databases/` (new files) — `CreateTableFormContent` + `CreateTableViewModel` + UI-state sealed classes, mirroring `AddDatabaseScreen.kt` / `AddDatabaseViewModel.kt`.
- **New** nested field-definition sheet composable (new file, e.g. `ui/components/tables/FieldDefinitionSheet.kt`) — modeled on `FolderFormSheet.kt` + `IOSGroupedCard` fields.
- **New** "+ Add field" button — composed from existing `IOSButton` (`Secondary` style); no reusable component exists to reuse.
- `core/database/models/` (new) — `ColumnDefinition` / `NewColumnDraft` input model (Name, Type, Length, Decimals, Nullable, Virtual, Key, Comment) + a new SQL column-type list/enum for the "Tipo" dropdown.
- `domain/usecases/` (new) — `CreateTableUseCase` mirroring `CreateDatabaseUseCase` (validate identifiers, build `CREATE TABLE` DDL, `repository.executeUpdate`).
- `ui/navigation/NavigationDestinations.kt` + `AdaptiveNavigationScaffold.kt` + `MyDataBasesNavHost.kt` — **only if** the trigger uses the bottom-nav modal-action pattern (add `isModal`, `onModalAction` case, hoisted sheet state).
- `res/values*/strings.xml` (×10) — all new user-facing strings.

---

## 3. Approaches

### Approach A — Two stacked `ModalBottomSheet`s (literal to the request)
First sheet (name + fields list + "+ Add field") hosts a second `ModalBottomSheet` (field form) via its own local `sheetState`; OK closes sheet 2 and appends to sheet 1's list.
- **Pros**: Exactly what the user asked; visually consistent with Add Database / Folder sheets; sheet 1 stays composed behind sheet 2.
- **Cons**: **No existing precedent for nested sheets**; documented Compose IME/focus fragility inside `ModalBottomSheet` (the reason `IOSDropdownField` uses a `Dialog`); careful dismiss ordering (`sheetState.hide()` before toggling state) needed; potential double-scrim visual.
- **Effort**: Medium-High.

### Approach B — First sheet is a `ModalBottomSheet`; nested field form is a `Dialog` styled like a sheet
Follows the existing `IOSDropdownField` decision to avoid nested-overlay IME issues.
- **Pros**: Lowest risk for keyboard/focus (the field form is text-heavy); consistent with an existing in-repo decision; simpler dismiss logic.
- **Cons**: Slight visual divergence from "a second bottom sheet" (bottom-anchored vs centered); may not perfectly match the user's mental model.
- **Effort**: Medium.

### Approach C — Single sheet, inline expanding field editor (no nested overlay)
"+ Add field" appends an editable row/card inline (or expands an inline editor) within the same sheet instead of opening a second overlay.
- **Pros**: Zero nested-overlay risk; simplest state; fast.
- **Cons**: Contradicts the explicit requirement (nested sheet with OK). Rejected unless user relaxes the requirement.
- **Effort**: Low.

---

## 4. Recommendation

**Approach A (two stacked `ModalBottomSheet`s) as the target, with Approach B as the documented fallback** if stacked-sheet IME/focus proves unstable during apply. Rationale: the user was explicit about a *second bottom sheet* opening on "+ Add field", and Add Database already establishes the exact sheet styling to reuse. Build order that de-risks it:
1. New input model (`ColumnDefinition`) + SQL column-type list.
2. `CreateTableFormContent` + `CreateTableViewModel` (fields list held in ViewModel `StateFlow<List<ColumnDefinition>>`; `CreateTableState` sealed class mirroring `CreateDatabaseState`).
3. Nested field-definition form composable (its own sheet), wired so OK returns a `ColumnDefinition` to the parent VM (`onFieldAdded(column)`), which appends and dismisses.
4. `CreateTableUseCase` (`CREATE TABLE` DDL) + repository `executeUpdate`, matching `CreateDatabaseUseCase`.
5. Trigger + host sheet on `TablesListScreen` with local sheet state (simplest), OR bottom-nav modal action if the orchestrator prefers nav parity with Add Database.
6. All `create_table_*` / `field_def_*` strings in **all 10** locales, added FIRST (android-dev rule).

The **field-name-first** ordering (requirement #1 follow-up) is naturally satisfied: table-name `IOSTextField` at the top of the sheet `Column`, then the fields list, then the "+ Add field" button — same top-to-bottom order as `AddDatabaseFormContent`.

---

## 5. Risks

- **Nested `ModalBottomSheet` IME/focus** — no precedent; documented Compose fragility (`IOSDropdownField` deliberately avoided it). Mitigation: fallback to `Dialog`-based field form (Approach B).
- **Dismiss ordering** — must `sheetState.hide()` before flipping visibility state, and reset ViewModel state on completion (observation #2030: `ModalBottomSheet` state does not auto-reset; needs `invokeOnCompletion`).
- **No column-creation input model / no SQL type list** — both are new; must be designed (Length/Decimals/Virtual/Key semantics).
- **Validation semantics** — "Llave" (PRIMARY) implies NOT NULL; "Virtual" (generated column) changes/omits length and default handling; "Decimales" only valid for numeric types (DECIMAL/NUMERIC); "Longitud" only for length-bearing types (VARCHAR/CHAR/DECIMAL). Cross-field validation needed in the field form.
- **Engine coverage** — only MySQL/MariaDB engines are implemented; DDL syntax/type list should target MySQL/MariaDB for v1 (PostgreSQL/SQLite enum values exist but have no engine).
- **Persistence scope ambiguity** — no local Room persistence for schema; must confirm v1 executes real `CREATE TABLE` DDL (parity with Add Database) vs. preview-only. Flag for proposal.
- **Trigger location undecided** — `TablesListScreen` in-screen affordance vs. bottom-nav modal action. Needs a product/nav decision in the proposal.
- **i18n load** — 12+ new strings × 10 locales; the android-dev skill makes this mandatory and blocking before commit.

---

## 6. Ready for Proposal

**Yes.** The reference pattern (Add Database sheet), the field controls (IOS* components + Switch), the DDL execution path (`executeUpdate` via a `CreateTableUseCase` mirroring `CreateDatabaseUseCase`), and the i18n convention are all identified. Two design decisions must be resolved in the proposal/design phase:
1. **Nested rendering**: stacked `ModalBottomSheet` (fidelity) vs. `Dialog` field form (lower IME risk).
2. **Trigger host**: in-screen affordance on `TablesListScreen` vs. bottom-nav modal action (parity with Add Database).
Plus confirm **v1 scope**: execute real `CREATE TABLE` DDL (recommended, matches Add Database) vs. form/preview only. The "+ Add field" button must be built new (no existing component to reuse — the user's memory of it does not match this codebase).
