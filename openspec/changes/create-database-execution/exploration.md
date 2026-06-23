# Exploration: create-database-execution

## Current State

The app already has a complete, layered SQL execution stack — the `database-list-bottom-nav` change deliberately left wiring the `AddDatabaseScreen` "Create" button to it for a follow-up. All the infrastructure to execute `CREATE DATABASE` is in place and battle-tested by the connection/listing flows.

### Architecture (Clean Architecture + MVVM)

```
UI (Compose)             AddDatabaseScreen → AddDatabaseViewModel
                                                    │
Domain (UseCases)        GetCharacterSetsUseCase  ExecuteUpdateUseCase  (← we will reuse / wrap)
                                                    │
Data/Core Repository     DatabaseRepository (interface) → DatabaseRepositoryImpl
                                                    │
Engine                   DatabaseEngine (interface) → MySQLEngine | MariaDBEngine
                                                    │
Driver                   MySQLConnectionPool (HikariCP, JDBC)
```

### Existing SQL execution pattern (the one to reuse)

`DatabaseEngine.executeUpdate(query: String, params: List<Any> = emptyList()): Result<Int>` is the canonical path for any INSERT/UPDATE/DELETE/DDL. It returns affected row count (or driver-defined sentinel for DDL).

- `MySQLEngine.executeUpdate` (MySQLEngine.kt:147-165) uses `connection.prepareStatement(query).use { ... statement.executeUpdate() }` with positional params, wrapped in `runCatching { ... }.recoverCatching { throw mapQueryError(throwable, query) }`.
- All engine methods run on `withContext(Dispatchers.IO)` and return `kotlin.Result<T>`.
- `DatabaseRepositoryImpl.executeUpdate` (DatabaseRepositoryImpl.kt:39-42) delegates to `currentEngine?.executeUpdate(...)` and falls back to `Result.failure(DatabaseError.ConnectionFailed("No conectado"))` when no engine is connected.
- `ExecuteUpdateUseCase` (ExecuteUpdateUseCase.kt) is already defined: `suspend operator fun invoke(query: String, params: List<Any>): Result<Int>` — pure delegation to the repository.
- MariaDBEngine delegates the full surface to MySQLEngine, so `CREATE DATABASE` automatically works on MariaDB too.

### Error handling pattern

`DatabaseError` is a sealed class extending `Throwable`. Engine maps JDBC exceptions to typed errors via `mapQueryError(...)` / `mapConnectionError(...)`. ViewModels consume the `Result` and surface `.message` to the UI via state holders. Examples already in use:

- `DatabaseError.ConnectionFailed(reason)` — no active engine.
- `DatabaseError.AuthenticationFailed(reason)` — credentials.
- `DatabaseError.QueryExecutionFailed(query, reason)` — DDL / DML failure (this is what a CREATE DATABASE failure becomes today).
- `DatabaseError.TimeoutError(operation)`.
- `DatabaseError.UnknownError(throwable)`.

### Charsets / collations (already wired)

`GetCharacterSetsUseCase` exposes `getCharacterSets()` and `getCollations(charset)`. They run `SHOW CHARACTER SET` and `SHOW COLLATION WHERE Charset = ?` via `DatabaseRepository.getCharacterSets/getCollations` → `MySQLEngine.getCharacterSets/getCollations`. The form already loads them. **Nothing new required here for execution.**

### Current `AddDatabaseViewModel` state

`AddDatabaseViewModel` (AddDatabaseViewModel.kt) only handles:

- Loading charsets on init.
- Loading collations on charset selection (with in-memory cache).
- Exposes `charsetState` (`CharsetLoadState`) and `collationState` (`CollationLoadState`).

It does NOT yet:

- Inject `ExecuteUpdateUseCase` (or a new `CreateDatabaseUseCase`).
- Expose a `createState` / `submissionState`.
- Implement an `onSubmit(name, charset?, collation?)` method.

The screen's "Create" button currently shows a "Coming soon" snackbar inline (`AddDatabaseScreen.kt:235-249`). It does NOT call the ViewModel at all yet — submission must be moved into the ViewModel per the existing spec (`add-database-form/spec.md` Requirement: Submission Wiring, scenario "Create dispatches to ViewModel"). That spec actually anticipated this exact follow-up.

## Affected Areas

- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/AddDatabaseViewModel.kt` — inject `ExecuteUpdateUseCase` (or new `CreateDatabaseUseCase`), add `submissionState`, add `createDatabase(name, charset?, collation?)`. (~+60 lines)
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/AddDatabaseScreen.kt` — replace the inline snackbar with a `viewModel.createDatabase(...)` call, observe `submissionState`, show errors inline / via snackbar, close the sheet + refresh on success. (~+40 lines)
- `app/src/main/java/com/sphynxs/mydatabases/domain/usecases/CreateDatabaseUseCase.kt` — NEW. Builds the safe DDL (`CREATE DATABASE \`name\` [CHARACTER SET 'x'] [COLLATE 'y']`) and delegates to repository. Centralizes identifier escaping and charset/collation quoting so the ViewModel does not assemble SQL by hand. (~50 lines)
- `app/src/test/java/com/sphynxs/mydatabases/domain/usecases/CreateDatabaseUseCaseTest.kt` — NEW. Unit tests covering DDL composition (with/without charset/collation), backtick escaping, repository delegation, error propagation. (~100 lines)
- `app/src/test/java/com/sphynxs/mydatabases/ui/screens/databases/AddDatabaseViewModelTest.kt` — NEW (no existing VM test). Cover Idle → Loading → Success/Error flow.
- `app/src/main/res/values/strings.xml` + `values-es/strings.xml` — add `add_database_success`, `add_database_error_generic`, `add_database_error_already_exists`, `add_database_error_denied`. (~8 strings)
- `openspec/changes/database-list-bottom-nav/specs/add-database-form/spec.md` — supersede the "Coming soon" requirement with a real submission requirement; OR add a delta spec under this new change. (Decision in proposal phase.)
- `DatabasesListViewModel` — IF we want the list to refresh after creation, expose a `reload()` or pass a callback. Optional for v1.

### Files we DO NOT need to touch

- `DatabaseEngine`, `MySQLEngine`, `MariaDBEngine`, `DatabaseRepository`, `DatabaseRepositoryImpl` — already expose `executeUpdate`. No new engine method needed.
- `ExecuteUpdateUseCase` — exists and can be reused directly if we decide against a dedicated `CreateDatabaseUseCase`.
- DI modules (`DatabaseModule`, `RepositoryModule`) — `ExecuteUpdateUseCase` is `@Inject`-constructable; the new use case will be too.

## Approaches

1. **Dedicated `CreateDatabaseUseCase` wrapping `ExecuteUpdateUseCase` (RECOMMENDED)**
   - The use case owns identifier escaping (backticks + escape internal backticks), charset/collation single-quoting, and the optional clause composition. It exposes `suspend operator fun invoke(name: String, charset: String?, collation: String?): Result<Unit>`.
   - ViewModel stays thin: state machine + call.
   - Pros:
     - Matches the existing codebase pattern (one use case per business action — see `SaveConnectionUseCase`, `TestConnectionUseCase`, `GetCharacterSetsUseCase`).
     - SQL composition is unit-testable in isolation (cannot put DDL assembly in a ViewModel and TDD it cleanly).
     - Security-by-default (project standard): identifier escaping lives in one auditable place. Charset/collation cannot be parameterized via `?` placeholders (they are identifiers/literals in DDL context), so safe escaping MUST live somewhere — a use case is the right home.
     - Future-proof: a `DropDatabaseUseCase` / `RenameDatabaseUseCase` will live next to it.
   - Cons: One extra file. Negligible.
   - Effort: **Low**

2. **Inject `ExecuteUpdateUseCase` directly into `AddDatabaseViewModel`**
   - VM assembles the DDL string and calls `executeUpdate(sql)`.
   - Pros: No new file.
   - Cons:
     - ViewModel owns SQL assembly and identifier escaping → violates Clean Architecture (domain logic in presentation) and breaks the project's "use case per action" pattern.
     - Harder to unit-test (VM tests now have to assert SQL strings).
     - If a second screen ever needs to create a database, the logic gets duplicated.
   - Effort: **Low** but **architecturally regressive**.

3. **Add a new engine method `createDatabase(name, charset?, collation?)` and expose via repository**
   - Pros: Most type-safe at the engine layer.
   - Cons:
     - The engine layer currently only exposes generic primitives (`executeQuery`, `executeUpdate`, metadata readers). Adding business-level methods to the engine breaks that invariant and would lead to a fat interface (`createTable`, `createUser`, etc., on every engine).
     - More files to change (DatabaseEngine + MySQLEngine + MariaDBEngine + DatabaseRepository + DatabaseRepositoryImpl + 2 tests + Hilt).
   - Effort: **High** for no architectural gain.

## Recommendation

**Approach 1 — dedicated `CreateDatabaseUseCase` that delegates to the existing `executeUpdate` path.**

Concrete shape:

```kotlin
class CreateDatabaseUseCase @Inject constructor(
    private val repository: DatabaseRepository
) {
    suspend operator fun invoke(
        name: String,
        charset: String? = null,
        collation: String? = null,
    ): Result<Unit> {
        val sql = buildString {
            append("CREATE DATABASE ").append(escapeIdentifier(name))
            charset?.takeIf { it.isNotBlank() }?.let {
                append(" CHARACTER SET '").append(sanitizeLiteral(it)).append("'")
            }
            collation?.takeIf { it.isNotBlank() }?.let {
                append(" COLLATE '").append(sanitizeLiteral(it)).append("'")
            }
        }
        return repository.executeUpdate(sql).map { }
    }

    private fun escapeIdentifier(id: String): String = "`" + id.replace("`", "``") + "`"
    private fun sanitizeLiteral(s: String): String = s.replace("'", "''").replace("\\", "\\\\")
}
```

ViewModel additions:

```kotlin
sealed class CreateDatabaseState {
    data object Idle : CreateDatabaseState()
    data object Submitting : CreateDatabaseState()
    data object Success : CreateDatabaseState()
    data class Error(val message: String) : CreateDatabaseState()
}

private val _createState = MutableStateFlow<CreateDatabaseState>(CreateDatabaseState.Idle)
val createState: StateFlow<CreateDatabaseState> = _createState.asStateFlow()

fun createDatabase(name: String, charset: String?, collation: String?) {
    viewModelScope.launch {
        _createState.value = CreateDatabaseState.Submitting
        createDatabaseUseCase(name, charset, collation)
            .onSuccess { _createState.value = CreateDatabaseState.Success }
            .onFailure { _createState.value = CreateDatabaseState.Error(it.message ?: "Unknown") }
    }
}

fun resetCreateState() { _createState.value = CreateDatabaseState.Idle }
```

Why this is the right call:
- Mirrors `GetCharacterSetsUseCase` (already injected the same way).
- Reuses 100% of the existing JDBC / pool / error-mapping plumbing.
- Identifier escaping is in one auditable place.
- ViewModel becomes a thin state machine that's trivial to unit-test.
- Already lines up with the existing spec's "ViewModel's `onSubmit` receives `(name, charset?, collation?)`" scenario — we just replace the "Coming soon" snackbar branch with the real call.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| **SQL injection via the `name` field** — `CREATE DATABASE \`?\`` cannot use a JDBC `?` placeholder (identifiers don't bind), so the name MUST be escaped. | High if ignored | Centralize backtick-escape in `CreateDatabaseUseCase`. Keep the existing `^[A-Za-z0-9_]{1,64}$` regex in the form as a first line of defense. The escape function MUST still handle `` ` `` defensively. |
| **Charset/collation are identifiers too, not bind params.** They're already filtered by the dropdown to only show server-supplied values (from `SHOW CHARACTER SET` / `SHOW COLLATION`), but a defense-in-depth quote/escape is still recommended. | Medium | Single-quote + escape backslash and quote in `sanitizeLiteral`. Validate against the same `[A-Za-z0-9_]{1,64}` regex before sending. |
| **User lacks `CREATE` privilege.** Server returns `1044` / `1045` "Access denied for user 'x' to database 'y'". | Medium | `MySQLEngine.mapQueryError` already maps `SQLException` → `DatabaseError.QueryExecutionFailed(query, reason)`. UI surfaces `reason`. Optionally add a localized "permission denied" string for known SQLState `42000` or message `"Access denied"`. |
| **Database already exists** (error `1007` / SQLState `HY000`). | Medium | Surface the raw driver message via `DatabaseError.QueryExecutionFailed`. Optionally add localized "Database already exists" detection in the VM (`message?.contains("database exists")`). Decide in the spec whether to offer "use `IF NOT EXISTS`" (we don't, because then the form would silently succeed without creating anything — bad UX for an explicit Create action). |
| **MySQL vs MariaDB syntax divergence.** MySQL 8.0 also accepts `DEFAULT CHARACTER SET` and `DEFAULT COLLATE`; MariaDB supports both with/without `DEFAULT`. | Low | The `CHARACTER SET 'x' COLLATE 'y'` form (without `DEFAULT`) works on **both** MySQL 5.7/8.0+ and MariaDB 10.x. Verified by the SQL standard followed by both. |
| **No active connection** (user opens the form somehow without `currentEngine`). | Low | `DatabaseRepositoryImpl.executeUpdate` already returns `Result.failure(DatabaseError.ConnectionFailed("No conectado"))`. VM surfaces the message. |
| **Database list does not refresh after creation.** | Low (UX-only) | Either: (a) call a `reload()` on `DatabasesListViewModel` via shared event flow / `SavedStateHandle.set`, OR (b) dismiss the bottom sheet on Success and let user-driven reload happen. Phase 1 recommendation: dismiss the sheet on success and leave refresh to a small additional task (the list already uses `getDatabases()` from the engine, so a navArg or pull-to-refresh is enough). |
| **Charset/collation pair mismatch** (user picks a collation that doesn't belong to the charset — UI already prevents this by loading collations after charset selection, but a manual race is possible). | Very Low | The form's existing `LaunchedEffect(selectedCharset)` already resets `selectedCollation = null`. No code change. |
| **Spec drift** — the existing `add-database-form/spec.md` mandates "Coming soon" snackbar. | Certain | Author a delta spec under `openspec/changes/create-database-execution/specs/add-database-form/spec.md` that SUPERSEDES the "Submission Wiring (No SQL Execution in This Change)" requirement with a real "Submission Executes CREATE DATABASE" requirement. Archive both deltas together. |

## Ready for Proposal

**Yes.**

The orchestrator should tell the user:

- The codebase already has a clean SQL execution pipeline (`DatabaseEngine.executeUpdate` → `DatabaseRepository.executeUpdate` → `ExecuteUpdateUseCase`). We don't need to build anything new at the engine or repository layer.
- The recommended change is small and surgical: **a new `CreateDatabaseUseCase` (~50 lines), a state machine added to `AddDatabaseViewModel` (~30 lines), and wiring the Create button (~20 lines).** No new dependencies.
- The current spec explicitly defers SQL execution to "a follow-up change" — this change is that follow-up. A delta spec under `create-database-execution/specs/add-database-form/spec.md` will supersede the "Coming soon" requirement with the real "execute CREATE DATABASE" behavior.
- The one decision the user should weigh in on before we write the proposal: **should the database list auto-refresh after successful creation, or just dismiss the sheet?** (Recommend auto-refresh via a shared event so the user sees the new DB immediately.)
