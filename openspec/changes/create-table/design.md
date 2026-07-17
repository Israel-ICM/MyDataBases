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

## Addendum: ENUM/SET Support

ENUM and SET are string types that take a parenthesized list of allowed literal values instead
of a length/decimals pair, so they don't fit the existing `supportsLength`/`supportsDecimals`
model. A new `supportsValues: Boolean` capability was added to `SqlColumnType` (default `false`
for all pre-existing entries), set to `true` only for the new `SqlColumnType.Enum` (`"ENUM"`) and
`SqlColumnType.Set` (`"SET"`) entries. `ColumnDefinition` gained a `values: List<String> = emptyList()`
property holding the permitted literal values, ignored for all other types.

`ColumnDefinitionValidation` gained `isValuesApplicable(type)` (mirrors `isLengthApplicable`/
`isDecimalsApplicable`) and `isValuesValid(values, type)`: always valid when not applicable;
when applicable, requires at least one value, no case-sensitive duplicates, and every value
non-blank after `trim()`.

In `CreateTableUseCase.buildColumnClause`, when `column.type.supportsValues` is true the type
clause is built as `TYPE('v1','v2',...)` (each value trimmed and single-quote-escaped the same
way `comment` already is) instead of `sqlName + buildLengthAndDecimalsSuffix(...)`. This only
replaces the type-clause construction — the surrounding nullable/virtual/comment DDL logic is
unchanged and applies identically to ENUM/SET columns.

In `FieldDefinitionDialog`, a new "Valores" `IOSTextField` renders in the same slot as
Longitud/Decimales (mutually exclusive with them, since ENUM/SET support neither), gated by
`ColumnDefinitionValidation.isValuesApplicable(type)`. Input is free-text, comma-separated
(e.g. `activo, inactivo, pendiente`), parsed via `split(",").map { trim() }.filter { isNotBlank() }`
into the `values` list on confirm; a `valuesError` inline error (same pattern as `nameError`/
`typeError`/`expressionError`) is shown at OK-press time when applicable and invalid.

**Pre-existing defect found and fixed while implementing this addendum**: the uncommitted,
unconfirmed direct edits that added 12 other new `SqlColumnType` entries (MediumInt, Bit,
TinyText, MediumText, Binary, VarBinary, TinyBlob, Blob, MediumBlob, LongBlob, Json, Year) to
`FieldDefinitionDialog.kt`'s `ALL_SQL_COLUMN_TYPES` list left `ColumnDefinition.kt` in a
non-compiling state: none of those 12 sealed `data object` entries exist in `SqlColumnType`
(the dialog referenced undefined symbols), and a corrupted identifier (`SqlColsetumnType`
instead of `SqlColumnType`) had been introduced on the `VarChar` entry. The typo was fixed as
part of this change since it sat directly adjacent to the `supportsValues` edit and blocked the
whole file from parsing. The 12 missing sealed entries were intentionally left unadded — they
are out of scope for ENUM/SET support and belong to whatever unconfirmed work introduced them;
see the apply-progress/report for the maintainer decision needed there.

## Addendum: Extended Field Attributes

Six new field-definition attributes were added: Valor predeterminado (Default value),
Autoincrement, Rellenar con ceros (ZeroFill), Conjunto de caracteres (Character Set) +
Collation, and Actualización automática de fecha/hora (`ON UPDATE CURRENT_TIMESTAMP`). Each
new attribute follows the existing capability-flag pattern on `SqlColumnType`
(`supportsAutoIncrement`, `supportsZeroFill`, `supportsCharset`, `supportsAutoUpdateTimestamp`,
all default `false`) rather than introducing a parallel gating mechanism.

### Applicability rules

| Attribute | Applicable when | Types |
|---|---|---|
| Valor predeterminado | `!isVirtual && !autoIncrement` (cross-field rule, not type-gated) | all |
| Autoincrement | `type.supportsAutoIncrement && !isVirtual` | Int, TinyInt, SmallInt, MediumInt, BigInt |
| ZeroFill | `type.supportsZeroFill` | the 5 integer types above + Decimal, Numeric, Float, Double |
| Charset/Collation | `type.supportsCharset` | Char, VarChar, Text, TinyText, MediumText, LongText, Enum, Set |
| Auto-update timestamp | `type.supportsAutoUpdateTimestamp` | Timestamp, DateTime only (per MySQL/MariaDB docs — NOT Date/Time/Year) |

### Cross-field forcing

Autoincrement forces Llave (`isPrimaryKey`) to `true` via a new
`ColumnDefinitionValidation.resolvePrimaryKeyForAutoIncrement(currentIsPrimaryKey, autoIncrement)`
resolver, mirroring the existing `resolveNullable` pattern. Forcing Llave=true then re-triggers
the pre-existing Llave→Nulo=false rule, so AUTO_INCREMENT columns end up NOT NULL without a
separate rule (MySQL requires AUTO_INCREMENT columns to be indexed and NOT NULL). Autoincrement
is hidden/disabled and force-cleared when Virtual is toggled on (mutually exclusive — generated
columns cannot be AUTO_INCREMENT).

### DDL clause order (`CreateTableUseCase.buildColumnClause`)

Extends the existing build order (name → type+suffix → virtual/comment) by inserting three new
steps between the type suffix and the existing virtual/nullable branch:

1. `` `name` ``
2. type + length/decimals-or-values suffix (unchanged)
3. `UNSIGNED ZEROFILL` if `zeroFill == true` (numeric attribute, immediately after the type)
4. `CHARACTER SET x COLLATE y` if `type.supportsCharset` and either `characterSet`/`collation`
   is non-null (emits `CHARACTER SET x` alone, `COLLATE y` alone, or both space-separated)
5. Branch on `isVirtual` (unchanged generated-column path — never emits DEFAULT/AUTO_INCREMENT/
   ON UPDATE) or, for non-generated columns: `NOT NULL` (existing) → `DEFAULT <valor>` (raw,
   unquoted/uncited) → `ON UPDATE CURRENT_TIMESTAMP` → `AUTO_INCREMENT`
6. `COMMENT '...'` (unchanged, always last)

Valor predeterminado is OPAQUE, following the same philosophy as `expression`: no client-side
SQL parsing/quoting. The user types `'text'` themselves for string literals, or an unquoted
`CURRENT_TIMESTAMP`/`0`/etc. as appropriate.

### Charset/Collation live-loading

`CreateTableViewModel` gained its own `FieldCharsetLoadState`/`FieldCollationLoadState` sealed
states, `loadCharacterSets()` (called from `init`), `loadCollations(charset)`, and
`clearCollations()` — mirroring `AddDatabaseViewModel`'s existing charset/collation pattern
(same `GetCharacterSetsUseCase`, same in-memory collation cache keyed by charset). These are
duplicated locally rather than reused from `AddDatabaseViewModel` (even though the latter's
sealed states aren't Kotlin-`private`) to avoid a cross-feature dependency between
`ui/screens/databases` and `ui/screens/tables` — the two features should stay independently
evolvable. `FieldDefinitionDialog` receives `charsets`/`charsetsLoading`/`collations`/
`collationsLoading`/`onCharsetSelected` as plain parameters (lists + booleans + a callback),
not the sealed states themselves, keeping the dialog decoupled from any particular ViewModel
shape. Selecting a charset clears the selected collation and invokes `onCharsetSelected`, which
`CreateTableFormContent` wires to `viewModel.loadCollations(charset)`.

### UI placement

New controls are inserted at these points in `FieldDefinitionDialog`'s existing vertical order
(Nombre, Tipo, Longitud, Decimales, Nulo, Virtual, Expresión, Llave, Comentario — plus Valores
from the ENUM/SET addendum): ZeroFill switch immediately after Decimales; Charset/Collation
dropdowns immediately after Longitud/Decimales/Valores; Valor predeterminado text field before
Nulo; Actualización automática de fecha/hora switch near Nulo; Autoincrement switch near Llave.
