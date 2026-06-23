# Proposal: Create Database Execution

## Intent

`database-list-bottom-nav` shipped the `AddDatabaseScreen` form (name + charset dropdown + collation dropdown), but its Create button only shows a "Coming soon" snackbar — no SQL runs. This change completes that deferred wiring so users can actually create a database on the connected MySQL/MariaDB server, with proper error feedback, loading state, and a refreshed database list on success.

## Scope

### In Scope
- New `CreateDatabaseUseCase` that builds and executes a `CREATE DATABASE` statement
- Extend `DatabaseRepository` + `DatabaseRepositoryImpl` + `MySQLEngine` with `createDatabase(name, charset?, collation?)`
- Identifier-safe SQL building: backtick-quote the database name, validate against `^[A-Za-z0-9_]{1,64}$` before reaching the engine (defense-in-depth — UI already validates)
- Optional charset / collation: clauses only appended when provided
- `AddDatabaseViewModel` gains a `submit(name, charset?, collation?)` function exposing a `SubmitState` (`Idle | Submitting | Success | Error(message)`)
- `AddDatabaseScreen` Create button wires to `submit`, shows progress (button loading state), success snackbar, dismisses the sheet, and triggers a refresh of `DatabasesListViewModel`
- Localized strings (en + es) for success and the common error messages (duplicate database, permission denied, connection lost, generic failure)
- Unit tests for `CreateDatabaseUseCase` (SQL building, identifier guard) and `AddDatabaseViewModel.submit` (state transitions, error mapping)

### Out of Scope
- Database deletion (`DROP DATABASE`) — separate change
- Database editing / `ALTER DATABASE` (rename, change default charset/collation post-creation)
- Permission / GRANT management
- Charset/collation options beyond what the existing form already exposes (no new dropdowns, no encryption clause, no tablespace)
- Engines other than MySQL/MariaDB (no PostgreSQL, no SQLite path)
- Pre-flight existence check ("does this name already exist?") — we let the server return the error
- Replacing the `MySQLConnectionPool.activeConnection` singleton (still tracked as separate cleanup)

## Capabilities

### New Capabilities
- `create-database-execution`: executes `CREATE DATABASE` against the active MySQL/MariaDB connection on form submit, surfaces success/error to the UI, and triggers a database-list refresh on success

### Modified Capabilities
- `add-database-form`: the "Submission Wiring (No SQL Execution in This Change)" requirement is replaced by real execution wiring — Submit now invokes the use case instead of showing a "Coming soon" notice

## Approach

Follow the existing Hexagonal layering already in place (UI → ViewModel → UseCase → Repository → Engine):

1. **Engine layer** — Add `MySQLEngine.createDatabase(name, charset?, collation?)`. Build the statement as `` CREATE DATABASE `${name}` `` plus optional `` CHARACTER SET `${charset}` `` / `` COLLATE `${collation}` ``. Use `executeUpdate` internally so error mapping (`mapQueryError` → `DatabaseError.QueryExecutionFailed`) is reused. Re-validate the identifier regex inside the engine and throw `DatabaseError.InvalidConfiguration` on mismatch — never interpolate unchecked input.
2. **Repository layer** — Add `suspend fun createDatabase(name, charset?, collation?): Result<Unit>` on the interface and delegate from `DatabaseRepositoryImpl` (cast to `MySQLEngine` like the existing charset/collation methods do, returning `ConnectionFailed("No conectado a MySQL")` otherwise).
3. **Domain layer** — `CreateDatabaseUseCase` wraps the repository call, performs the same identifier validation, and trims input. Returns `Result<Unit>`.
4. **ViewModel layer** — `AddDatabaseViewModel` gains an injected `CreateDatabaseUseCase` and a `submitState: StateFlow<SubmitState>`. `submit()` sets `Submitting`, calls the use case, and maps `Result` → `Success` or `Error(message)`. Error mapping converts `DatabaseError` subtypes into user-friendly localized strings (passed in from UI via a resolver, OR mapped to a stable enum/key the UI translates — pick the resolver pattern to keep VM free of `Context`).
5. **UI layer** — `AddDatabaseFormContent` collects `submitState`, disables the Create button while `Submitting` and shows a progress indicator on it, shows the success snackbar then calls `onDismiss()` plus a new `onCreated()` callback. The host (databases screen / nav scaffold) uses `onCreated` to call `DatabasesListViewModel.refresh()` so the new DB appears immediately.

SQL injection is mitigated by: (a) UI-side regex validation, (b) ViewModel-side validation before the call, (c) UseCase-side validation, (d) Engine-side validation before string interpolation. Backticks are NOT escaped — the regex forbids them entirely, which is the standard MySQL identifier-safety pattern.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `core/database/engine/mysql/MySQLEngine.kt` | Modified | Add `createDatabase(name, charset?, collation?)` |
| `core/database/repository/DatabaseRepository.kt` | Modified | Add interface method `createDatabase(...)` |
| `core/database/repository/DatabaseRepositoryImpl.kt` | Modified | Delegate `createDatabase(...)` to `MySQLEngine` |
| `domain/usecases/CreateDatabaseUseCase.kt` | New | Wraps repository, validates identifiers, trims input |
| `ui/screens/databases/AddDatabaseViewModel.kt` | Modified | Inject use case; add `SubmitState` + `submit()` |
| `ui/screens/databases/AddDatabaseScreen.kt` | Modified | Wire Create button to `submit`; loading + snackbar + dismiss + refresh callback |
| `ui/screens/databases/DatabasesListViewModel.kt` | Modified | Public `refresh()` (or expose existing reload entry point) |
| `res/values/strings.xml` + `values-es/strings.xml` | Modified | Success and error message strings |
| `app/src/test/.../CreateDatabaseUseCaseTest.kt` | New | SQL shape, identifier guard, optional clauses |
| `app/src/test/.../AddDatabaseViewModelTest.kt` | New | `submit` state machine, error mapping |
| `app/src/test/.../MySQLEngineTest.kt` | Modified | New cases for `createDatabase` (where unit-testable) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SQL injection via unvalidated identifier | Low | Four-layer regex guard (UI, VM, UseCase, Engine); never interpolate unchecked input; backticks disallowed by regex |
| Server returns "permission denied" — confusing message | Medium | Map `DatabaseError.QueryExecutionFailed` whose `reason` contains "Access denied" / "command denied" to a localized "No tienes permisos para crear bases de datos" / "You don't have permission to create databases" |
| Duplicate database name (server error 1007) | Medium | Detect `reason` containing "database exists" / error 1007 and map to a localized "Ya existe una base de datos con ese nombre" / "A database with that name already exists" |
| Connection dropped between form open and submit | Low | `executeUpdate` already throws `ConnectionFailed`; map to localized "Conexión perdida. Reconectá e intentá de nuevo" |
| Charset/collation mismatch (e.g. collation not valid for charset) | Low | Server returns 1273; surface raw reason via a generic error mapping, no special-casing |
| `DatabasesListViewModel.refresh()` races the new DB visibility | Low | `CREATE DATABASE` is synchronous in MySQL; refresh after `Success` is safe |
| Bottom sheet dismissed mid-`Submitting` cancels coroutine | Low | Launch in `viewModelScope`, not the sheet's scope — already the case; verify no UI-scope coroutine remains |

## Rollback Plan

1. Revert the commit(s) for this change. No schema or persistence is mutated locally; only remote MySQL servers may have new databases created — those persist regardless of revert (acceptable: user explicitly requested them).
2. Reverting restores the "Coming soon" snackbar behavior. The `add-database-form` spec's deferred-execution scenario becomes accurate again.
3. New use case, repository method, and engine method are removed cleanly with the revert — no migration needed.
4. If only the UI wiring misbehaves but the use case is sound: keep the use case file and revert only `AddDatabaseScreen.kt` + `AddDatabaseViewModel.kt` to the "Coming soon" version.

## Dependencies

- Existing `MySQLEngine` with active `MySQLConnectionPool`
- Existing `DatabaseRepository` / `DatabaseRepositoryImpl` with Hilt wiring
- Existing `DatabasesListViewModel` and its reload pathway
- Existing `AddDatabaseValidation` regex (reused — single source of truth)
- No new third-party libraries

## Success Criteria

- [ ] Submitting a valid form actually creates the database on the server (verified manually against a real MySQL/MariaDB instance)
- [ ] On success: snackbar shows localized success message, the bottom sheet dismisses, the database list refreshes and shows the new DB
- [ ] On failure: the sheet stays open, Create button re-enables, an inline error or snackbar shows a localized, user-friendly message (no raw SQL exception text leaks to the user)
- [ ] Submitting with charset only (no collation) generates `CREATE DATABASE \`name\` CHARACTER SET \`charset\``
- [ ] Submitting with collation only (no charset) generates `CREATE DATABASE \`name\` COLLATE \`collation\``
- [ ] Submitting with neither generates plain `CREATE DATABASE \`name\``
- [ ] An identifier containing backticks, spaces, or non-allowed chars is rejected at every layer and never reaches the JDBC driver
- [ ] `./gradlew test` and `./gradlew assembleDebug` are green
- [ ] Unit tests cover: SQL shape for the three optional-combinations, identifier-guard rejection, VM state transitions (Idle → Submitting → Success and Idle → Submitting → Error), error-message mapping for `Access denied`, `database exists`, and `ConnectionFailed`
