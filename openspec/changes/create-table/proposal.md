# Proposal: Create Table

## Intent

Users inside a database can list tables but cannot create one. This adds a "Crear tabla" flow: a table-name field, a dynamic list of column definitions built via a nested "Agregar campo" form, and an OK action that executes real `CREATE TABLE` DDL against the active MySQL/MariaDB connection — mirroring the existing Add Database precedent.

## Scope

### In Scope
- `new_table` bottom-nav modal action wired end-to-end (nav entry already exists).
- Parent "Crear tabla" `ModalBottomSheet` (name field → fields list → "+ Agregar campo" → OK/Cancel).
- Nested "Agregar campo" form rendered as a sheet-styled `Dialog` (appends to in-memory list; no execution).
- `ColumnDefinition` input model + MySQL/MariaDB column-type list.
- `CreateTableUseCase` builds `CREATE TABLE` DDL and runs `repository.executeUpdate`.
- Parent OK executes DDL; on success dismiss + refresh `TablesListScreen`; on error show message.
- All new `create_table_*` / `field_def_*` strings in all 10 locales.

### Out of Scope
- Stacked second `ModalBottomSheet` (rejected — IME/focus risk; using `Dialog` per `IOSDropdownField` precedent).
- Editing/reordering/deleting already-added fields, ALTER/DROP, indexes/foreign keys, PostgreSQL/SQLite (no engines).
- Local Room persistence of schema (schema is always read live).

## Capabilities

### New Capabilities
- `create-table`: Create a table with ordered column definitions via nested form + `CREATE TABLE` DDL execution.

### Modified Capabilities
- None.

## Approach

Reuse the Add Database sheet pattern. `CreateTableFormContent` (pure sheet content) + `CreateTableViewModel` (`StateFlow<List<ColumnDefinition>>`, `CreateTableState` sealed class). "+ Agregar campo" opens a sheet-styled `Dialog`; its OK returns a `ColumnDefinition` appended by the VM. Parent OK (enabled when name non-blank) calls `CreateTableUseCase` → `executeUpdate`. `CreateTableUseCase` mirrors `CreateDatabaseUseCase` (identifier regex `^[A-Za-z0-9_]{1,64}$`, `buildString` DDL, `Result<Int>.map { Unit }`). Field validation: `Llave` forces `Nulo=false`; `Longitud` enabled only for length-bearing types (VARCHAR/CHAR/DECIMAL); `Decimales` only for DECIMAL/NUMERIC; `Virtual` omits length/default handling.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/navigation/Routes.kt` | Modified | Add `NewTable` route to replace TODO placeholder (line 148). |
| `ui/navigation/MyDataBasesNavHost.kt` | Modified | Add `showAddTableSheet` state + `"new_table"` branch (line 82); pass down to `TablesListScreen`. |
| `ui/screens/tables/TablesListScreen.kt` | Modified | New params `showAddTableSheet` + `onDismissAddTableSheet`; host sheet; refresh on success. Note: `connectionId` is hardcoded `"current"` (line 122) — resolve for DDL. |
| `ui/screens/tables/CreateTableFormContent.kt` + `CreateTableViewModel.kt` | New | Sheet content + VM + `CreateTableState`. |
| `ui/components/tables/FieldDefinitionDialog.kt` | New | Nested field form (Dialog). |
| `core/database/models/ColumnDefinition.kt` | New | Input model + column-type list. |
| `domain/usecases/CreateTableUseCase.kt` | New | Builds/executes `CREATE TABLE`. |
| `res/values*/strings.xml` (×10) | Modified | ~14 new keys × 10 locales. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `connectionId` hardcoded `"current"` in `TablesListScreen` | Med | Confirm how active connection resolves for DDL during design. |
| Cross-field field validation (Llave/Virtual/Longitud/Decimales) | Med | Specify exact rules in spec/design. |
| Sheet state not auto-resetting after dismiss | Med | `sheetState.hide()` before flipping state + reset VM on completion. |
| i18n load (~14 keys × 10 locales) blocking | Low | Add strings first (android-dev rule). |

## Rollback Plan

Revert the change branch. New files (`ColumnDefinition`, `CreateTableUseCase`, `CreateTableFormContent`, `CreateTableViewModel`, `FieldDefinitionDialog`) are additive. Nav edits are localized diffs to `Routes.kt`, `MyDataBasesNavHost.kt`, `TablesListScreen.kt`; restore the TODO placeholder route. No data migrations (no local schema persistence).

## Dependencies

- Active MySQL/MariaDB connection with CREATE privileges (runtime).
- Existing: `DatabaseRepository.executeUpdate`, `IOSButton`/`IOSTextField`/`IOSDropdownField`/`IOSGroupedCard`, `Switch`.

## Success Criteria

- [ ] Tapping `new_table` opens the "Crear tabla" sheet.
- [ ] "Agregar campo" OK appends a field without executing SQL.
- [ ] Parent OK (name non-blank) runs `CREATE TABLE`; success refreshes the table list.
- [ ] DDL/connection errors surface a localized message; sheet stays open.
- [ ] All new strings present in all 10 locales; no hardcoded UI text.
- [ ] `CreateTableUseCase` unit-tested (valid DDL, identifier rejection, field mapping).

## Size Estimate (for review-budget gauge)

Rough LOC — Domain `ColumnDefinition` + type list ~80, `CreateTableUseCase` ~90; UI `CreateTableFormContent` ~180, `CreateTableViewModel` + state ~120, `FieldDefinitionDialog` ~200; nav wiring ~40; i18n ~14 keys × 10 = ~140 lines; tests ~200. **Total ≈ 1,050 LOC**, above the 800-line review budget. Recommend chained slices: (1) domain + use case + tests, (2) nav wiring + parent sheet, (3) nested field dialog + i18n. Flag for `sdd-tasks` delivery-strategy decision.
