# Design: Create Table

## Technical Approach

Mirror the Add Database sheet exactly: a pure-content composable (`CreateTableFormContent`) hosted by a `ModalBottomSheet` in `TablesListScreen`, backed by a `CreateTableViewModel`. The nested "Agregar campo" form is a sheet-styled `Dialog` (per `IOSDropdownField` precedent, avoiding un-precedented nested-`ModalBottomSheet` IME risk — Approach B from exploration). `CreateTableUseCase` mirrors `CreateDatabaseUseCase`'s validate → `buildString` DDL → `executeUpdate` shape.

## Architecture Decisions

| Decision | Choice | Alternative rejected | Rationale |
|---|---|---|---|
| Nested field form | Sheet-styled `Dialog` | Stacked 2nd `ModalBottomSheet` | No nested-sheet precedent exists; `IOSDropdownField` already avoided this for documented IME/focus fragility |
| Field-list ownership | `CreateTableViewModel` owns `StateFlow<List<ColumnDefinition>>` | List in local `remember` on `CreateTableFormContent` | VM must survive dialog open/close and is the natural place to append after DDL-adjacent validation |
| Dialog↔parent contract | `onFieldConfirmed: (ColumnDefinition) -> Unit` callback | Dialog writes directly to VM | Keeps `FieldDefinitionDialog` stateless/reusable; parent VM stays the single source of truth |
| `connectionId` source | Thread from NavHost route arg → param chain | Re-derive via `NavigationContext` inside `TablesListScreen` | `NavHost` already extracts it once from route args (`Routes.TableList`); avoids duplicate derivation logic |
| Virtual column semantics | Emit real `GENERATED ALWAYS AS (<expresión>) [VIRTUAL\|STORED]` DDL, backed by a new free-text Expresión field | Keep flag-only (no DDL effect) | Amended spec adds an Expresión input; MySQL generated-column syntax is now fully buildable — resolves the prior open question |
| Expression validation | Opaque free-text; only non-blank client-side check | Client-side SQL parsing/validation | Spec explicitly forbids semantic validation; real errors surface via existing DDL-failure path (`Parent OK Executes DDL`), no new error mechanism needed |
| Storage mode resolution | Derived (`VIRTUAL` default, `STORED` when also `isPrimaryKey`) — not user-settable | Expose a Virtual/Stored picker | MySQL mandates `STORED` for generated PK columns; deriving avoids an invalid-state UI control and matches spec's "MUST NOT require a separate user input" |

## Data Flow

    MyDataBasesNavHost (extracts connectionId from route args)
         │ connectionId, showAddTableSheet
         ▼
    TablesListScreen ──hosts──▶ ModalBottomSheet
         │                          │
         │                          ▼
         │                 CreateTableFormContent (connectionId, onDismiss, onTableCreated)
         │                          │  observes
         │                          ▼
         │                 CreateTableViewModel (StateFlow<List<ColumnDefinition>>, CreateTableState)
         │                          │  "+ Agregar campo" opens
         │                          ▼
         │                 FieldDefinitionDialog (local form state)
         │                          │  Virtual toggle ON ─▶ shows required Expresión, hides Nulo
         │                          │  Virtual toggle OFF ─▶ hides Expresión, restores Nulo
         │                          │  onFieldConfirmed(ColumnDefinition)
         │                          └────────────▶ viewModel.addField(...)
         │
         │  OK ─▶ viewModel.createTable(connectionId, name, fields)
         │              └─▶ CreateTableUseCase(connectionId, name, columns)
         │                        └─▶ DatabaseRepository.executeUpdate(sql, [])
         ▼
    on Success: refresh list + dismiss · on Error: snackbar, sheet stays open

## File Changes

| File | Action | LOC (est.) | Description |
|---|---|---|---|
| `core/database/models/ColumnDefinition.kt` | Create | ~105 | `ColumnDefinition` data class (+ `expression`, `generatedStorageMode`) + `SqlColumnType` + `GeneratedStorageMode` enum + pure validation functions |
| `domain/usecases/CreateTableUseCase.kt` | Create | ~130 | Builds/executes `CREATE TABLE` DDL, incl. generated-column clause branch |
| `ui/screens/tables/CreateTableViewModel.kt` | Create | ~130 | `StateFlow<List<ColumnDefinition>>`, `CreateTableState`, `addField`, `createTable`, `reset` |
| `ui/screens/tables/CreateTableFormContent.kt` | Create | ~190 | Sheet content: name field, fields list, "+ Agregar campo", OK/Cancel |
| `ui/components/tables/FieldDefinitionDialog.kt` | Create | ~250 | Sheet-styled `Dialog`; local form state; Expresión input wired to Virtual; Nulo hidden when Virtual; cross-field validation display |
| `ui/navigation/MyDataBasesNavHost.kt` | Modify | ~15 | `showAddTableSheet` state, `"new_table"` `onModalAction` branch, pass `connectionId` to `TablesListScreen` |
| `ui/screens/tables/TablesListScreen.kt` | Modify | ~70 | New params `connectionId`, `showAddTableSheet`, `onDismissAddTableSheet`; host sheet; fix hardcoded `"current"` → real `connectionId`; refresh on success |
| `res/values*/strings.xml` (×10) | Modify | ~180 | ~18 `create_table_*`/`field_def_*` keys × 10 locales (+3 keys: Expresión label, required-error, generated-column helper text) |
| `domain/usecases/CreateTableUseCaseTest.kt` | Create | ~160 | Valid DDL, identifier rejection, per-type formatting, generated-column DDL (VIRTUAL/STORED, base-type length/decimals, COMMENT) |
| `core/database/models/ColumnDefinitionValidationTest.kt` | Create | ~100 | Cross-field rules (Llave/Nulo/Longitud/Decimales/Virtual), Expresión required-when-Virtual, Nulo hidden-when-Virtual |

**Refined total ≈ 1,330 LOC** (prior design version: ~1,175 LOC; +155 LOC for the Expresión field, `expression`/`generatedStorageMode` domain properties, and the generated-column DDL branch) — confirms the 800-line budget is exceeded; chained slices still required.

## Interfaces / Contracts

```kotlin
data class ColumnDefinition(
    val name: String,
    val type: SqlColumnType,
    val length: Int? = null,
    val decimals: Int? = null,
    val nullable: Boolean = true,
    val isVirtual: Boolean = false,
    val expression: String? = null, // required non-blank when isVirtual; null/ignored otherwise
    val isPrimaryKey: Boolean = false,
    val comment: String? = null,
) {
    /** Resolved generated-column storage mode; null for non-generated columns.
     *  STORED is forced when isVirtual && isPrimaryKey (MySQL requirement); VIRTUAL otherwise. */
    val generatedStorageMode: GeneratedStorageMode?
        get() = when {
            !isVirtual -> null
            isPrimaryKey -> GeneratedStorageMode.STORED
            else -> GeneratedStorageMode.VIRTUAL
        }
}

enum class GeneratedStorageMode(val sqlKeyword: String) {
    VIRTUAL("VIRTUAL"),
    STORED("STORED"),
}

sealed class SqlColumnType(val sqlName: String, val supportsLength: Boolean, val supportsDecimals: Boolean) {
    data object Int : SqlColumnType("INT", supportsLength = true, supportsDecimals = false)
    data object VarChar : SqlColumnType("VARCHAR", supportsLength = true, supportsDecimals = false)
    data object Decimal : SqlColumnType("DECIMAL", supportsLength = true, supportsDecimals = true)
    data object Text : SqlColumnType("TEXT", supportsLength = false, supportsDecimals = false)
    data object DateTime : SqlColumnType("DATETIME", supportsLength = false, supportsDecimals = false)
    data object Boolean : SqlColumnType("BOOLEAN", supportsLength = false, supportsDecimals = false)
    // + remaining MySQL/MariaDB types
}

// CreateTableUseCase — mirrors CreateDatabaseUseCase's Result<Unit> contract
suspend operator fun invoke(name: String, columns: List<ColumnDefinition>): Result<Unit>
```

**DDL build order** (per column, comma-joined, wrapped in `CREATE TABLE `name` (...)`):
1. `` `name` `` (backtick-quoted, validated against `^[A-Za-z0-9_]{1,64}$`)
2. Type + `(length[,decimals])` if `supportsLength`/`supportsDecimals` (base type applies whether or not the column is generated)
3. **Branch on `isVirtual`**:
   - `isVirtual = true` → append `` GENERATED ALWAYS AS (`${expression}`) ${generatedStorageMode.sqlKeyword}``. No `NULL`/`NOT NULL` clause and no `DEFAULT` clause are ever emitted here — MySQL derives nullability from the expression, and generated columns cannot carry a `DEFAULT`.
   - `isVirtual = false` → `NOT NULL` if `!nullable` (forced when `isPrimaryKey`); omit clause if nullable (MySQL default)
4. `COMMENT '<escaped>'` if `comment` non-blank (applies identically to generated and non-generated columns — MySQL supports `COMMENT` on both)
5. After all columns: `, PRIMARY KEY (`col1`, ...)` clause appended if any column has `isPrimaryKey = true` (a generated PK column is eligible because its storage mode was already forced to `STORED` in step 3)

This ordering matches MySQL's column-definition grammar (`type → GENERATED ALWAYS AS (expr) [VIRTUAL|STORED] → COMMENT`), so no reordering is needed relative to the non-generated path beyond substituting step 3's clause.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | `CreateTableUseCase` DDL string building, identifier rejection, per-type length/decimal formatting, PK clause placement, generated-column clause (`VIRTUAL`/`STORED`, base-type length/decimals retained, no `NULL`/`DEFAULT` emitted, `COMMENT` still applied) | JUnit + `runTest`, mock `DatabaseRepository` (mirrors `CreateDatabaseUseCaseTest`) |
| Unit | Cross-field validation (`Llave→Nulo=false` when non-generated, `Longitud` only for length-bearing types, `Decimales` only for DECIMAL/NUMERIC, Expresión required-non-blank only when Virtual=true, Nulo control hidden/disabled when Virtual=true, storage mode flips `VIRTUAL`↔`STORED` on Llave toggle) | Pure function tests, no Compose/Android deps |
| Integration | `CreateTableViewModel` list append/reset, `CreateTableState` transitions | `runTest` + fake use case |
| E2E | Not in scope for v1 (no Compose UI test infra found for sheets) | N/A |

## Migration / Rollout

No migration required — all new files are additive; nav/screen edits are localized diffs with a documented revert path (proposal's Rollback Plan).

## Open Questions

- [x] ~~**Virtual column DDL**~~ — Resolved: the amended spec adds an Expresión free-text input; `CreateTableUseCase` now emits real `GENERATED ALWAYS AS (<expresión>) [VIRTUAL|STORED]` DDL (see DDL build order above). No longer open.
- [ ] Confirm chained-PR slice boundaries against the refined ~1,330 LOC estimate in `sdd-tasks` (proposal suggested 3 slices: domain+use case+tests / nav+parent sheet / nested dialog+i18n).
