# Delta for add-database-form

## MODIFIED Requirements

### Requirement: Submission Wiring

Tapping Create MUST collect the form values and invoke `AddDatabaseViewModel.submit(name, charset?, collation?)`, which executes `CREATE DATABASE` against the active MySQL/MariaDB connection via `CreateDatabaseUseCase`. The ViewModel MUST expose a `submitState: StateFlow<SubmitState>` with values `Idle`, `Submitting`, `Success`, and `Error(message)`. The UI MUST react to each state: disable the Create button and show progress while `Submitting`; on `Success` show a localized success snackbar, dismiss the bottom sheet, and trigger a refresh of `DatabasesListViewModel`; on `Error` keep the sheet open, re-enable the button, and surface a localized, user-friendly error message (never raw SQL exception text). Charset and collation clauses MUST be omitted from the SQL when their corresponding fields are empty.
(Previously: Create only logged the submission and surfaced a "Coming soon" snackbar; no SQL was executed, no persistence touched, no list refreshed.)

#### Scenario: Create dispatches to ViewModel

- GIVEN name is `analytics_2026`, charset is empty, collation is `utf8mb4_unicode_ci`
- WHEN the user taps Create
- THEN `AddDatabaseViewModel.submit` is invoked with `("analytics_2026", null, "utf8mb4_unicode_ci")`
- AND `submitState` transitions from `Idle` to `Submitting`

#### Scenario: Loading state disables the form while executing

- GIVEN `submitState` is `Submitting`
- WHEN the UI recomposes
- THEN the Create button is disabled
- AND a progress indicator is visible on or next to the Create button
- AND the name, charset, and collation inputs are read-only or disabled

#### Scenario: Success dismisses sheet, shows snackbar, refreshes list

- GIVEN a valid form submission for name `analytics_2026`
- WHEN `CreateDatabaseUseCase` returns `Result.success(Unit)`
- THEN `submitState` transitions to `Success`
- AND a snackbar shows the localized success message: "Database created" (en) / "Base de datos creada" (es)
- AND the bottom sheet dismisses via `onDismiss()`
- AND the host invokes `DatabasesListViewModel.refresh()` so the new database appears in the list

#### Scenario: Generic error keeps sheet open and shows message

- GIVEN a valid form submission
- WHEN `CreateDatabaseUseCase` returns `Result.failure(DatabaseError.QueryExecutionFailed(...))` with a reason the VM does not specifically recognize
- THEN `submitState` transitions to `Error` with the localized generic message: "Could not create the database. Please try again." (en) / "No se pudo crear la base de datos. Intentá de nuevo." (es)
- AND the bottom sheet remains open
- AND the Create button re-enables
- AND no raw SQLException text leaks to the UI

#### Scenario: Already-exists error shows specific message

- GIVEN a valid form submission for a name that already exists on the server
- WHEN `CreateDatabaseUseCase` returns a failure whose reason contains `database exists` or error code `1007`
- THEN `submitState` transitions to `Error` with the localized message: "A database with that name already exists" (en) / "Ya existe una base de datos con ese nombre" (es)
- AND the bottom sheet remains open with the name field still populated

#### Scenario: Permission-denied error shows specific message

- GIVEN a valid form submission by a user without `CREATE` privilege
- WHEN `CreateDatabaseUseCase` returns a failure whose reason contains `Access denied` or `command denied`
- THEN `submitState` transitions to `Error` with the localized message: "You don't have permission to create databases" (en) / "No tenés permisos para crear bases de datos" (es)
- AND the bottom sheet remains open

#### Scenario: Charset only is included in the SQL; collation is omitted

- GIVEN name is `analytics_2026`, charset is `utf8mb4`, collation is empty
- WHEN the user taps Create
- THEN `AddDatabaseViewModel.submit` is invoked with `("analytics_2026", "utf8mb4", null)`
- AND the SQL composed by the use case includes a `CHARACTER SET` clause for `utf8mb4`
- AND the SQL contains no `COLLATE` clause

#### Scenario: Collation only is included in the SQL; charset is omitted

- GIVEN name is `analytics_2026`, charset is empty, collation is `utf8mb4_unicode_ci`
- WHEN the user taps Create
- THEN `AddDatabaseViewModel.submit` is invoked with `("analytics_2026", null, "utf8mb4_unicode_ci")`
- AND the SQL composed by the use case includes a `COLLATE` clause for `utf8mb4_unicode_ci`
- AND the SQL contains no `CHARACTER SET` clause

#### Scenario: No persistence on submit (Room/DataStore untouched)

- GIVEN the user submits a valid form
- WHEN `submitState` reaches `Success` or `Error`
- THEN no row is written to Room
- AND no DataStore entry is mutated
