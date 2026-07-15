# Tasks: Create Table (`create-table`)

Strict TDD active. RED→GREEN→REFACTOR triads apply to NEW logic only —
`ColumnDefinition` validation and `CreateTableUseCase` DDL building (per design.md).
Nav routes, `CreateTableViewModel` plumbing, and Compose UI wiring (`CreateTableFormContent`,
`FieldDefinitionDialog`) are mechanical — verification-only, out of TDD scope.

## Review Workload Forecast

Session budget cached at **800** lines (not the shared-skill default of 400); risk assessed
against 800 below, 400-default noted where it changes the recommendation.

| Slice | Scope | Est. LOC | Files |
|---|---|---|---|
| PR-1 Domain | `ColumnDefinition` + `SqlColumnType`/`GeneratedStorageMode` + validation fns, `CreateTableUseCase` (incl. generated-column DDL branch), both test files | ~495 | 4 |
| PR-2 Nav + Parent Sheet | `Routes.kt`, `MyDataBasesNavHost.kt`, `CreateTableViewModel.kt`, `CreateTableFormContent.kt`, `TablesListScreen.kt` | ~405 | 5 |
| PR-3 Nested Dialog + i18n | `FieldDefinitionDialog.kt`, 10× `strings.xml` | ~430 | 11 |
| **Total (unchained)** | | **~1330** | **20** |

Single PR not viable (1330 > 800 cached budget, exceeds it by ~66%). **3 slices are sufficient**:
each sits at 51–62% of the 800-line budget with comfortable headroom — no 4th slice is
warranted this session. Against the stricter 400-line shared-skill default, all three slices
modestly exceed it (PR-1 +24%, PR-2 +1%, PR-3 +8%); this only matters if the team later
tightens the review budget below 800 — not applicable this session. Slices are strictly
sequential: PR-2 consumes `ColumnDefinition`/`CreateTableUseCase` from PR-1; PR-3's dialog
consumes both PR-1's validation functions and PR-2's `CreateTableViewModel` callback contract.

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain (maintainer-approved)
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | PR | Notes |
|------|------|-----|-------|
| 1 | `ColumnDefinition`/`SqlColumnType`/`GeneratedStorageMode` + validation fns + `CreateTableUseCase` (incl. generated-column DDL) + unit tests | PR-1 | Base TBD by chosen chain strategy; no upstream dependency |
| 2 | Nav wiring (`new_table` route + `onModalAction` branch) + `CreateTableViewModel` + `CreateTableFormContent` + `TablesListScreen` hosting | PR-2 | Depends on PR-1 (`ColumnDefinition`, `CreateTableUseCase`) |
| 3 | `FieldDefinitionDialog` (nested field form) + all `create_table_*`/`field_def_*` strings ×10 locales | PR-3 | Depends on PR-1 (validation fns) and PR-2 (`onFieldConfirmed` contract into `CreateTableViewModel`) |

## Phase 1 (PR-1): Domain — `ColumnDefinition` & `CreateTableUseCase`

- [x] 1.1 RED: `ColumnDefinitionValidationTest` — length/decimals applicability (VARCHAR/CHAR/DECIMAL/NUMERIC enabled, else disabled), Llave→Nulo forced false (non-virtual), Expresión required-non-blank only when Virtual=true, Nulo hidden when Virtual=true
- [x] 1.2 GREEN: `core/database/models/ColumnDefinition.kt` — data class (`name`,`type`,`length`,`decimals`,`nullable`,`isVirtual`,`expression`,`isPrimaryKey`,`comment`) + `SqlColumnType` sealed hierarchy (`supportsLength`/`supportsDecimals` per type) + `GeneratedStorageMode` enum + pure validation fns satisfying 1.1
- [x] 1.3 REFACTOR: extract `generatedStorageMode` computed property (`VIRTUAL` default, `STORED` when `isVirtual && isPrimaryKey`) per design.md interface
- [x] 1.4 RED: `CreateTableUseCaseTest` — valid simple-table DDL, identifier rejection (`^[A-Za-z0-9_]{1,64}$`), per-type length/decimal formatting, PRIMARY KEY clause placement
- [x] 1.5 GREEN: `domain/usecases/CreateTableUseCase.kt` — validate name+columns, `buildString` DDL (design.md build-order steps 1–2,4–5), `executeUpdate` via `DatabaseRepository`, `Result<Int>.map { Unit }`
- [x] 1.6 RED: extend `CreateTableUseCaseTest` — generated-column cases: `GENERATED ALWAYS AS (...) VIRTUAL`/`STORED`, base-type length/decimals retained, no `NULL`/`DEFAULT` emitted, `COMMENT` still applied
- [x] 1.7 GREEN: implement `isVirtual` branch (design.md build-order step 3) in `CreateTableUseCase`'s DDL builder
- [x] 1.8 REFACTOR: align `CreateTableUseCase` structure/naming with `CreateDatabaseUseCase.kt` precedent (companion `IDENTIFIER_REGEX`, trimmed inputs)
- [ ] 1.9 Run `./gradlew testDebugUnitTest` — confirm both new test files pass, no pre-existing regressions — **NOT executed by sdd-apply per project HARD RULE (manual compilation only); maintainer must run this manually and report results**

## Phase 2 (PR-2): Nav Wiring & Parent Sheet

- [ ] 2.1 Add `NewTable` route to `ui/navigation/Routes.kt`, replacing the TODO placeholder
- [ ] 2.2 `ui/navigation/MyDataBasesNavHost.kt` — add `showAddTableSheet` state + `"new_table"` branch in `onModalAction` (mirrors `showAddDatabaseSheet`/`"add_database"`); pass `connectionId`/`showAddTableSheet`/`onDismissAddTableSheet` down to `TablesListScreen`
- [ ] 2.3 `ui/screens/tables/CreateTableViewModel.kt` — `StateFlow<List<ColumnDefinition>>`, `CreateTableState` sealed class (Idle/Loading/Success/Error), `addField()`, `createTable(connectionId, name, fields)` calling `CreateTableUseCase`, `reset()`
- [ ] 2.4 `ui/screens/tables/CreateTableFormContent.kt` — pure sheet content in exact spec order (table-name field → fields list → "+ Agregar campo" → OK/Cancel); OK enabled only when name non-blank AND fields non-empty
- [ ] 2.5 Wire `ui/screens/tables/TablesListScreen.kt` — new params `connectionId`/`showAddTableSheet`/`onDismissAddTableSheet`; host `ModalBottomSheet(CreateTableFormContent)`; fix hardcoded `"current"` connectionId → real value; on success dismiss+refresh; on error show message, keep sheet open with entered data intact
- [ ] 2.6 Verify: sheet reopens with cleared state after a prior dismiss (spec scenario "Sheet opens fresh after prior dismiss")
- [ ] 2.7 Run `./gradlew compileDebugKotlin` — confirm nav+VM+sheet wiring compiles, no regressions

## Phase 3 (PR-3): Nested Field Dialog & i18n

- [ ] 3.1 `ui/components/tables/FieldDefinitionDialog.kt` — sheet-styled `Dialog`, local form state (Nombre/Tipo/Longitud/Decimales/Nulo/Virtual/Expresión/Llave/Comentario) in spec order; Longitud/Decimales enabled per `SqlColumnType` flags; Nulo hidden when Virtual=true; Expresión shown+required only when Virtual=true; Llave forces Nulo=false when non-virtual
- [ ] 3.2 Wire dialog OK — reuse Phase 1 validation fns; on success `onFieldConfirmed(ColumnDefinition)` → `viewModel.addField()`; close dialog, reveal parent sheet with new field, no SQL executed
- [ ] 3.3 Wire dialog Cancel/dismiss — discard in-progress input, parent list unchanged
- [ ] 3.4 Add ~18 `create_table_*`/`field_def_*` keys (labels, validation errors, Expresión required-error, generated-column helper text) to `res/values/strings.xml`
- [ ] 3.5 Add the same ~18 keys to all 9 remaining locales: `values-es`, `values-ar`, `values-de`, `values-fr`, `values-hi`, `values-ja`, `values-pt-rBR`, `values-ru`, `values-zh-rCN`
- [ ] 3.6 Audit: grep source tree for hardcoded `Text("...")` literals in `FieldDefinitionDialog.kt`/`CreateTableFormContent.kt` (android-dev skill rule) — confirm zero matches
- [ ] 3.7 Run `./gradlew testDebugUnitTest compileDebugKotlin assembleDebug` — confirm full feature suite green, no regressions
