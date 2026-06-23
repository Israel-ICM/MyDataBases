# Design: Create Database Execution

## Technical Approach

Follow the existing Clean Architecture + MVVM pattern already in place: `AddDatabaseViewModel` gains a new `CreateDatabaseUseCase` dependency and exposes a `submitState` state machine. The use case composes the SQL (`CREATE DATABASE \`name\` [CHARACTER SET \`x\`] [COLLATE \`y\`]`) with defense-in-depth identifier validation (reject anything that doesn't match `^[A-Za-z0-9_]{1,64}$`), then delegates to `DatabaseRepository.executeUpdate`. On success, the UI shows a snackbar, dismisses the bottom sheet, and triggers `DatabasesListViewModel.loadDatabases()` via a new `onDatabaseCreated` callback. Error mapping translates `DatabaseError` subtypes into localized user-friendly messages.

This reuses 100% of the existing SQL execution stack (no engine, repository, or DI changes) and mirrors the existing `GetCharacterSetsUseCase` injection pattern.

## Architecture Decisions

### Decision: Dedicated CreateDatabaseUseCase vs inline SQL in ViewModel

**Choice**: New `CreateDatabaseUseCase` that wraps `DatabaseRepository.executeUpdate`

**Alternatives considered**:
- Inject `ExecuteUpdateUseCase` directly into VM and assemble SQL there
- Add `createDatabase(...)` method to `DatabaseEngine` + `DatabaseRepository`

**Rationale**:
- Matches the existing codebase pattern (one use case per business action — see `GetCharacterSetsUseCase`, `GetDatabasesUseCase`).
- SQL composition and identifier escaping must live in the domain layer (testable in isolation; ViewModel should be a thin state machine).
- Security-by-default: identifier validation and backtick escaping live in one auditable place instead of duplicated across screens.
- Future-proof: `DropDatabaseUseCase` / `RenameDatabaseUseCase` will live next to it.
- Engine layer currently exposes only generic primitives (`executeQuery`, `executeUpdate`); adding business methods (`createDatabase`, `createTable`, etc.) would break that clean boundary.

### Decision: Backtick escaping vs parameterized statement

**Choice**: Backtick-quote the database name as `` `${name}` ``; reject any input that doesn't match `^[A-Za-z0-9_]{1,64}$`

**Alternatives considered**:
- Use JDBC `?` placeholder for the identifier

**Rationale**:
- JDBC prepared statements do NOT support binding identifiers (database names, table names, column names) via `?` placeholders — only values. MySQL syntax for `CREATE DATABASE` requires the identifier inline.
- Defense-in-depth: the regex rejects backticks, spaces, semicolons, quotes, and any character that could break the SQL syntax. No escaping needed because the input is pre-validated.
- This matches the MySQL documentation's recommended pattern for identifier safety: alphanumeric + underscore only, length ≤ 64.

### Decision: Error mapping location (ViewModel vs UseCase)

**Choice**: ViewModel maps `DatabaseError` subtypes to localized messages; UseCase propagates the original error unchanged

**Alternatives considered**:
- UseCase translates errors to user-friendly strings
- Use a dedicated ErrorMapper class

**Rationale**:
- Localization requires Android `Context` (to access `stringResource` / `R.string`), which violates Clean Architecture if injected into the domain layer.
- ViewModel already has access to localized strings via Composable context (`stringResource` called inline in the UI).
- Pattern-matching on `DatabaseError.QueryExecutionFailed.reason` (checking for `"database exists"`, `"Access denied"`, etc.) is presentation logic — the ViewModel decides WHAT to show based on the typed error, the UseCase just propagates the error type.
- Existing ViewModels (`DatabasesListViewModel`, `AddDatabaseViewModel`) already follow this pattern: they map `error.message` to UI state, not the UseCases.

### Decision: Refresh strategy after successful creation

**Choice**: Add `onDatabaseCreated: () -> Unit` callback to `AddDatabaseFormContent`, invoked from the host (`DatabasesListScreen` composable), which then calls `DatabasesListViewModel.loadDatabases()`

**Alternatives considered**:
- Shared event flow (`SharedFlow<DatabaseEvent>`) between ViewModels
- ViewModel-to-ViewModel communication via Hilt scope
- No auto-refresh (user manually refreshes the list)

**Rationale**:
- The callback pattern matches the existing `onDismiss: () -> Unit` parameter already on `AddDatabaseFormContent` — simple, testable, no shared state.
- Shared event flows require more infrastructure (singleton event bus or scoped ViewModel holder) and are overkill for a single-screen parent-child communication.
- `DatabasesListViewModel.loadDatabases()` is already public and designed for refresh (it's called on screen init). Reusing it is zero-cost.
- `CREATE DATABASE` is synchronous in MySQL — no eventual consistency or race. Calling `loadDatabases()` immediately after `Success` guarantees the new DB is visible.

### Decision: SubmitState vs reusing CharsetLoadState pattern

**Choice**: New sealed class `CreateDatabaseState` with `Idle | Submitting | Success | Error(message: String)`

**Alternatives considered**:
- Reuse `CharsetLoadState` and rename it to `LoadState<T>`
- Use `Result<Unit>` directly as StateFlow

**Rationale**:
- `Idle` is semantically different from `Loading` — the form has NOT started loading yet. `CharsetLoadState` models a one-time load on init; `CreateDatabaseState` models a user-triggered action with a "not started" initial state.
- `Success` needs to be distinguished from `Idle` so the UI can trigger side effects (snackbar, dismiss, refresh) exactly once. `Result<Unit>` doesn't carry that "pending action" vs "action completed" distinction without additional boolean flags.
- Explicit sealed class makes the state machine exhaustive and readable in the UI (`when (submitState) { ... }`).

## Data Flow

```
User taps Create button
    │
    ├──> AddDatabaseFormContent collects form values (name, charset?, collation?)
    │        │
    │        ├──> Calls viewModel.createDatabase(name, charset, collation)
    │                   │
    │                   ├──> Sets submitState = Submitting
    │                   │
    │                   ├──> Launches coroutine: createDatabaseUseCase(name, charset, collation)
    │                             │
    │                             ├──> UseCase validates identifiers (regex ^[A-Za-z0-9_]{1,64}$)
    │                             │      │
    │                             │      ├─ INVALID ──> Result.failure(DatabaseError.InvalidConfiguration)
    │                             │      │
    │                             │      └─ VALID ───> Compose SQL: CREATE DATABASE `name` [CHARACTER SET `x`] [COLLATE `y`]
    │                             │                         │
    │                             │                         └──> repository.executeUpdate(sql)
    │                             │                                   │
    │                             │                                   ├──> MySQLEngine.executeUpdate(sql)
    │                             │                                   │        │
    │                             │                                   │        ├─ Success ──> Result.success(rowCount)
    │                             │                                   │        │
    │                             │                                   │        └─ SQLException ──> mapQueryError() ──> Result.failure(DatabaseError)
    │                             │                                   │
    │                             │                                   └──> Return Result<Int>
    │                             │
    │                             └──> UseCase returns Result<Unit>
    │
    ├──> ViewModel.onSuccess { submitState = Success }
    │                 │
    │                 └──> UI collectAsState(submitState)
    │                           │
    │                           ├──> Show success snackbar ("Database created")
    │                           │
    │                           ├──> Call onDismiss() (close bottom sheet)
    │                           │
    │                           └──> Call onDatabaseCreated() ──> DatabasesListViewModel.loadDatabases()
    │
    └──> ViewModel.onFailure { submitState = Error(mappedMessage) }
                  │
                  ├──> Map DatabaseError.QueryExecutionFailed.reason:
                  │      - "database exists" / "1007" ──> R.string.error_database_exists
                  │      - "Access denied" / "command denied" ──> R.string.error_permission_denied
                  │      - else ──> R.string.error_create_database_failed + reason
                  │
                  ├──> Map DatabaseError.ConnectionFailed ──> R.string.error_connection_lost
                  │
                  ├──> Map DatabaseError.InvalidConfiguration ──> R.string.error_invalid_database_name
                  │
                  └──> UI shows error snackbar, keeps sheet open, re-enables Create button
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/src/main/java/com/sphynxs/mydatabases/domain/usecases/CreateDatabaseUseCase.kt` | Create | Domain use case: validates identifiers, composes SQL, delegates to repository |
| `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/AddDatabaseViewModel.kt` | Modify | Inject `CreateDatabaseUseCase`, add `CreateDatabaseState` sealed class, add `submitState: StateFlow`, add `createDatabase(name, charset, collation)` function, add error-mapping logic |
| `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/AddDatabaseScreen.kt` | Modify | Replace "Coming soon" snackbar with `viewModel.createDatabase(...)` call, collect `submitState`, wire Create button loading state, add success/error handling, add `onDatabaseCreated` callback parameter |
| `app/src/main/res/values/strings.xml` | Modify | Add `create_database_success`, `error_database_exists`, `error_permission_denied`, `error_create_database_failed`, `error_invalid_database_name`, `error_connection_lost` |
| `app/src/main/res/values-es/strings.xml` | Modify | Add Spanish translations for the new strings |
| `app/src/test/java/com/sphynxs/mydatabases/domain/usecases/CreateDatabaseUseCaseTest.kt` | Create | Unit tests: SQL composition (name only, charset only, collation only, both), identifier validation rejection cases, repository delegation |
| `app/src/test/java/com/sphynxs/mydatabases/ui/screens/databases/AddDatabaseViewModelTest.kt` | Create | Unit tests: state transitions (Idle → Submitting → Success, Idle → Submitting → Error), error mapping (database exists, permission denied, generic) |

## Interfaces / Contracts

### CreateDatabaseUseCase

```kotlin
class CreateDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    suspend operator fun invoke(
        name: String,
        charset: String? = null,
        collation: String? = null
    ): Result<Unit>
}
```

**Behavior**:
- Trim `name`, `charset`, `collation`
- Treat blank optional values as `null` (omit from SQL)
- Validate all non-null identifiers against `^[A-Za-z0-9_]{1,64}$`
- Return `Result.failure(DatabaseError.InvalidConfiguration(...))` if validation fails
- Compose SQL: `` CREATE DATABASE `name` [CHARACTER SET `charset`] [COLLATE `collation`] ``
- Call `repository.executeUpdate(sql)` and map `Result<Int>` → `Result<Unit>`

### CreateDatabaseState (sealed class in AddDatabaseViewModel.kt)

```kotlin
sealed class CreateDatabaseState {
    data object Idle : CreateDatabaseState()
    data object Submitting : CreateDatabaseState()
    data object Success : CreateDatabaseState()
    data class Error(val message: String) : CreateDatabaseState()
}
```

### AddDatabaseViewModel additions

```kotlin
// New property
val submitState: StateFlow<CreateDatabaseState>

// New function
fun createDatabase(name: String, charset: String?, collation: String?)

// New function (resets state after success side effects complete)
fun resetSubmitState()
```

### AddDatabaseFormContent signature change

```kotlin
@Composable
fun AddDatabaseFormContent(
    connectionId: String,
    onDismiss: () -> Unit = {},
    onDatabaseCreated: () -> Unit = {},  // ← NEW callback
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: AddDatabaseViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
)
```

### DatabasesListViewModel (expose existing method)

No change needed — `loadDatabases()` is already `public`. The host screen will call it directly.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| **Unit (Use Case)** | SQL composition with name only | Assert SQL equals `` CREATE DATABASE `analytics_2026` `` |
| | SQL composition with charset only | Assert SQL equals `` CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` `` |
| | SQL composition with collation only | Assert SQL equals `` CREATE DATABASE `analytics_2026` COLLATE `utf8mb4_unicode_ci` `` |
| | SQL composition with both | Assert SQL equals `` CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` `` |
| | Name with backtick is rejected | Assert result is `failure(DatabaseError.InvalidConfiguration)`, repository never called |
| | Name with semicolon is rejected | Assert result is `failure(DatabaseError.InvalidConfiguration)`, repository never called |
| | Charset with space is rejected | Assert result is `failure(DatabaseError.InvalidConfiguration)`, repository never called |
| | Name exceeding 64 chars is rejected | Assert result is `failure(DatabaseError.InvalidConfiguration)`, repository never called |
| | Repository success propagates | Mock repository returns `success(1)`, assert use case returns `success(Unit)` |
| | Repository failure propagates | Mock repository returns `failure(DatabaseError.QueryExecutionFailed)`, assert use case returns same failure |
| **Unit (ViewModel)** | Initial state is Idle | Assert `submitState.value == CreateDatabaseState.Idle` |
| | Submitting state shows while coroutine runs | Suspend repository call, assert `submitState.value == Submitting`, then resume |
| | Success sets Success state | Mock use case returns `success(Unit)`, call `createDatabase`, assert `submitState.value == Success` |
| | Generic error maps to localized message | Mock use case returns `failure(DatabaseError.QueryExecutionFailed(_, "Unknown error"))`, assert `Error.message` is the generic string |
| | "database exists" error maps to specific message | Mock reason contains `"database exists"`, assert `Error.message` is the specific string |
| | "Access denied" error maps to specific message | Mock reason contains `"Access denied"`, assert `Error.message` is the permission string |
| | ConnectionFailed error maps correctly | Mock use case returns `failure(DatabaseError.ConnectionFailed)`, assert `Error.message` is connection-lost string |
| | InvalidConfiguration error maps correctly | Mock use case returns `failure(DatabaseError.InvalidConfiguration)`, assert `Error.message` is invalid-name string |
| **Integration (Manual)** | Form submission creates DB on real server | Connect to local MySQL, submit valid form, verify DB exists via `SHOW DATABASES` |
| | Duplicate name shows error | Create DB manually, submit same name, verify error snackbar shows "already exists" |
| | Permission-denied scenario | Connect as restricted user, submit form, verify permission error |
| | Success refreshes list | Submit valid form, verify new DB appears in the list without manual refresh |
| | Charset-only clause works | Submit with charset, no collation, verify `SHOW CREATE DATABASE` output matches |
| | Collation-only clause works | Submit with collation, no charset, verify `SHOW CREATE DATABASE` output matches |

## Migration / Rollout

No migration required.

This change only adds new execution logic to an existing form. Rolling back removes the execution capability and restores the "Coming soon" snackbar behavior. No local or remote schema is mutated automatically — only databases explicitly created by the user during the rollout window would persist (acceptable: user explicitly requested them).

## Open Questions

- [ ] Should the success snackbar be dismissible or auto-dismiss after 3 seconds? (Recommendation: auto-dismiss to match the existing pattern in `DatabasesListScreen` error snackbars.)
- [ ] Should the error message be shown as a snackbar or inline below the Create button? (Recommendation: snackbar for consistency with the existing "Coming soon" pattern and to avoid layout shift.)
- [ ] Should we add a loading spinner *inside* the Create button or next to it? (Recommendation: disabled state + CircularProgressIndicator overlaid on the button, matching `IOSButton` existing loading pattern if it has one; otherwise, a small spinner to the left of the button text.)
