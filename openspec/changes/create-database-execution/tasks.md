# Tasks: Create Database Execution

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350-400 lines |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-always |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Complete database creation execution feature | Single PR | Tests and localization included; merges to main |

## Phase 1: Foundation (Domain Layer)

- [x] 1.1 Create `app/src/main/java/com/sphynxs/mydatabases/domain/usecases/CreateDatabaseUseCase.kt` with `invoke(name, charset?, collation?): Result<Unit>`
- [x] 1.2 Add identifier validation regex `^[A-Za-z0-9_]{1,64}$` for name, charset, collation
- [x] 1.3 Implement SQL composition: `` CREATE DATABASE `name` [CHARACTER SET `x`] [COLLATE `y`] ``
- [x] 1.4 Add trimming logic for all input parameters; treat blank optionals as null
- [x] 1.5 Delegate to `repository.executeUpdate(sql)` and map `Result<Int>` to `Result<Unit>`

## Phase 2: ViewModel Layer (State Machine)

- [x] 2.1 Add `CreateDatabaseState` sealed class in `AddDatabaseViewModel.kt`: `Idle | Submitting | Success | Error(message)`
- [x] 2.2 Inject `CreateDatabaseUseCase` into `AddDatabaseViewModel` constructor
- [x] 2.3 Add `submitState: StateFlow<CreateDatabaseState>` property, initialized to `Idle`
- [x] 2.4 Create `createDatabase(name, charset?, collation?)` function launching in `viewModelScope`
- [x] 2.5 Implement state transition: `Idle → Submitting → Success` or `Idle → Submitting → Error(message)`
- [x] 2.6 Add error mapping: `QueryExecutionFailed` with "database exists" → specific message
- [x] 2.7 Add error mapping: `QueryExecutionFailed` with "Access denied" → permission message
- [x] 2.8 Add error mapping: `ConnectionFailed` → connection-lost message
- [x] 2.9 Add error mapping: `InvalidConfiguration` → invalid-name message
- [x] 2.10 Add error mapping: generic `QueryExecutionFailed` → generic failure message
- [x] 2.11 Add `resetSubmitState()` function to return state to `Idle`

## Phase 3: UI Layer (Form Wiring)

- [x] 3.1 Modify `AddDatabaseFormContent` signature to add `onDatabaseCreated: () -> Unit` callback
- [x] 3.2 Replace "Coming soon" snackbar with `viewModel.createDatabase(name, charset, collation)` call
- [x] 3.3 Collect `submitState` in the composable and react to state changes
- [x] 3.4 Disable Create button and show progress indicator when `submitState is Submitting`
- [x] 3.5 On `Success`: show localized success snackbar, call `onDismiss()`, call `onDatabaseCreated()`
- [x] 3.6 On `Error`: show error snackbar with `error.message`, keep sheet open, re-enable button
- [x] 3.7 Wire host screen's `onDatabaseCreated` to call `DatabasesListViewModel.loadDatabases()`

## Phase 4: Localization

- [x] 4.1 Add to `res/values/strings.xml`: `create_database_success` ("Database created")
- [x] 4.2 Add to `res/values/strings.xml`: `error_database_exists` ("A database with that name already exists")
- [x] 4.3 Add to `res/values/strings.xml`: `error_permission_denied` ("You don't have permission to create databases")
- [x] 4.4 Add to `res/values/strings.xml`: `error_create_database_failed` ("Could not create the database. Please try again.")
- [x] 4.5 Add to `res/values-es/strings.xml`: `create_database_success` ("Base de datos creada")
- [x] 4.6 Add to `res/values-es/strings.xml`: `error_database_exists` ("Ya existe una base de datos con ese nombre")
- [x] 4.7 Add to `res/values-es/strings.xml`: `error_permission_denied` ("No tenés permisos para crear bases de datos")
- [x] 4.8 Add to `res/values-es/strings.xml`: `error_create_database_failed` ("No se pudo crear la base de datos. Intentá de nuevo.")

## Phase 5: Testing (Use Case)

- [x] 5.1 Create `app/src/test/java/com/sphynxs/mydatabases/domain/usecases/CreateDatabaseUseCaseTest.kt`
- [x] 5.2 Test scenario: name-only SQL equals `` CREATE DATABASE `analytics_2026` ``
- [x] 5.3 Test scenario: charset-only SQL equals `` CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` ``
- [x] 5.4 Test scenario: collation-only SQL equals `` CREATE DATABASE `analytics_2026` COLLATE `utf8mb4_unicode_ci` ``
- [x] 5.5 Test scenario: both clauses SQL equals `` CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` ``
- [x] 5.6 Test scenario: name with backtick returns `InvalidConfiguration`, repository never called
- [x] 5.7 Test scenario: name with semicolon returns `InvalidConfiguration`, repository never called
- [x] 5.8 Test scenario: charset with space returns `InvalidConfiguration`, repository never called
- [x] 5.9 Test scenario: name > 64 chars returns `InvalidConfiguration`, repository never called
- [x] 5.10 Test scenario: blank charset/collation treated as null (no clause in SQL)
- [x] 5.11 Test scenario: repository success propagates to `Result.success(Unit)`
- [x] 5.12 Test scenario: repository failure propagates unchanged

## Phase 6: Testing (ViewModel)

- [x] 6.1 Create `app/src/test/java/com/sphynxs/mydatabases/ui/screens/databases/AddDatabaseViewModelTest.kt`
- [x] 6.2 Test scenario: initial state is `Idle`
- [x] 6.3 Test scenario: calling `createDatabase` transitions to `Submitting`
- [x] 6.4 Test scenario: use case success transitions to `Success`
- [x] 6.5 Test scenario: generic `QueryExecutionFailed` maps to generic error message
- [x] 6.6 Test scenario: "database exists" in reason maps to specific error message
- [x] 6.7 Test scenario: "Access denied" in reason maps to permission error message
- [x] 6.8 Test scenario: `ConnectionFailed` maps to connection-lost message
- [x] 6.9 Test scenario: `InvalidConfiguration` maps to invalid-name message
- [x] 6.10 Test scenario: `resetSubmitState` returns state to `Idle`

## Phase 7: Manual Verification

- [ ] 7.1 Run `./gradlew test` and verify all new tests pass (Pre-existing test failures block full suite — see apply-progress notes)
- [x] 7.2 Run `./gradlew assembleDebug` and verify build succeeds
- [ ] 7.3 Connect to local MySQL/MariaDB server via the app
- [ ] 7.4 Submit valid form (name only) and verify database is created on server
- [ ] 7.5 Verify success snackbar shows, sheet dismisses, and new DB appears in list
- [ ] 7.6 Submit duplicate name and verify "already exists" error shows
- [ ] 7.7 Submit form with charset only and verify `SHOW CREATE DATABASE` matches
- [ ] 7.8 Submit form with collation only and verify `SHOW CREATE DATABASE` matches
- [ ] 7.9 Submit form with both charset and collation and verify `SHOW CREATE DATABASE` matches
