# Create Database Execution Specification

## Purpose

Defines `CreateDatabaseUseCase` — the single domain entry point that builds and executes a `CREATE DATABASE` statement against the active MySQL/MariaDB connection. This capability owns identifier safety (backtick escaping, defense-in-depth regex validation), optional clause composition (`CHARACTER SET` / `COLLATE`), and the mapping from JDBC failures to typed `DatabaseError`s. It is consumed by `AddDatabaseViewModel` and delegates execution to `DatabaseRepository`.

## Requirements

### Requirement: Use Case Contract

The system MUST expose `CreateDatabaseUseCase` with a single suspend operator: `invoke(name: String, charset: String? = null, collation: String? = null): Result<Unit>`. The use case MUST trim `name`, `charset`, and `collation` before processing. The use case MUST treat empty or blank optional values as "not specified" and omit the corresponding clause from the SQL. The use case MUST return `Result.success(Unit)` when the repository reports success, and MUST propagate the repository's `Result.failure` unchanged.

#### Scenario: Successful creation returns Result.success(Unit)

- GIVEN an active MySQL connection and a valid name `analytics_2026`
- WHEN `CreateDatabaseUseCase("analytics_2026")` is invoked
- THEN the repository's `executeUpdate` is called with `` CREATE DATABASE `analytics_2026` ``
- AND the repository returns `Result.success(rowCount)`
- AND the use case returns `Result.success(Unit)`

#### Scenario: Blank optional fields are treated as not specified

- GIVEN name is `analytics_2026`, charset is `"   "`, collation is `""`
- WHEN the use case is invoked
- THEN the composed SQL is `` CREATE DATABASE `analytics_2026` ``
- AND no `CHARACTER SET` or `COLLATE` clause is present

#### Scenario: Repository failure is propagated unchanged

- GIVEN the repository returns `Result.failure(DatabaseError.QueryExecutionFailed(query, reason))`
- WHEN the use case is invoked with a valid name
- THEN the use case returns the same `Result.failure` with the same `DatabaseError` instance

### Requirement: SQL Composition

The use case MUST compose the `CREATE DATABASE` statement with the database name as a backtick-quoted identifier. When `charset` is provided and non-blank, the SQL MUST include `` CHARACTER SET `<charset>` `` immediately after the name. When `collation` is provided and non-blank, the SQL MUST include `` COLLATE `<collation>` ``. When both are provided, `CHARACTER SET` MUST appear before `COLLATE`. The SQL MUST NOT include `IF NOT EXISTS`, `DEFAULT`, or any other clause.

#### Scenario: Name only produces plain CREATE DATABASE

- GIVEN name is `analytics_2026`, charset is null, collation is null
- WHEN the use case composes the SQL
- THEN the SQL equals `` CREATE DATABASE `analytics_2026` ``

#### Scenario: Charset only appends CHARACTER SET clause

- GIVEN name is `analytics_2026`, charset is `utf8mb4`, collation is null
- WHEN the use case composes the SQL
- THEN the SQL equals `` CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` ``

#### Scenario: Collation only appends COLLATE clause

- GIVEN name is `analytics_2026`, charset is null, collation is `utf8mb4_unicode_ci`
- WHEN the use case composes the SQL
- THEN the SQL equals `` CREATE DATABASE `analytics_2026` COLLATE `utf8mb4_unicode_ci` ``

#### Scenario: Both clauses appear in CHARACTER SET then COLLATE order

- GIVEN name is `analytics_2026`, charset is `utf8mb4`, collation is `utf8mb4_unicode_ci`
- WHEN the use case composes the SQL
- THEN the SQL equals `` CREATE DATABASE `analytics_2026` CHARACTER SET `utf8mb4` COLLATE `utf8mb4_unicode_ci` ``

### Requirement: Identifier Safety and Injection Prevention

The use case MUST validate `name`, and each non-blank `charset` and `collation`, against `^[A-Za-z0-9_]{1,64}$` BEFORE composing the SQL. If any value fails validation, the use case MUST return `Result.failure(DatabaseError.InvalidConfiguration(...))` without invoking the repository. Backticks, spaces, semicolons, quotes, backslashes, and any other character outside the regex MUST be rejected. The use case MUST NOT attempt to escape, strip, or sanitize invalid input — it rejects.

#### Scenario: Name with backtick is rejected before SQL composition

- GIVEN name is `` my`db ``
- WHEN the use case is invoked
- THEN the use case returns `Result.failure(DatabaseError.InvalidConfiguration(...))`
- AND the repository's `executeUpdate` is never called

#### Scenario: Name with semicolon (injection attempt) is rejected

- GIVEN name is `analytics; DROP DATABASE prod`
- WHEN the use case is invoked
- THEN the use case returns `Result.failure(DatabaseError.InvalidConfiguration(...))`
- AND the repository's `executeUpdate` is never called

#### Scenario: Charset with space is rejected

- GIVEN name is `analytics_2026`, charset is `utf8 mb4`
- WHEN the use case is invoked
- THEN the use case returns `Result.failure(DatabaseError.InvalidConfiguration(...))`
- AND the repository's `executeUpdate` is never called

#### Scenario: Collation with quote is rejected

- GIVEN name is `analytics_2026`, collation is `utf8mb4_unicode_ci' OR '1'='1`
- WHEN the use case is invoked
- THEN the use case returns `Result.failure(DatabaseError.InvalidConfiguration(...))`
- AND the repository's `executeUpdate` is never called

#### Scenario: Name exceeding 64 characters is rejected

- GIVEN name is a 65-character alphanumeric string
- WHEN the use case is invoked
- THEN the use case returns `Result.failure(DatabaseError.InvalidConfiguration(...))`
- AND the repository's `executeUpdate` is never called

### Requirement: Error Mapping

When the repository returns `Result.failure(throwable)`, the use case MUST propagate the typed `DatabaseError` produced by the engine's `mapQueryError`. The use case MUST NOT translate error messages itself (translation is the ViewModel's responsibility), and MUST NOT swallow or wrap the original `DatabaseError`. When no engine is connected, the repository already returns `DatabaseError.ConnectionFailed`; the use case MUST propagate that unchanged.

#### Scenario: SQLException at the driver becomes DatabaseError.QueryExecutionFailed

- GIVEN the JDBC driver throws a `SQLException` with message `Can't create database 'x'; database exists`
- WHEN the engine's `executeUpdate` runs and `mapQueryError` maps it
- THEN the repository returns `Result.failure(DatabaseError.QueryExecutionFailed(query, reason))` where `reason` contains `database exists`
- AND the use case returns that same `Result.failure` unchanged

#### Scenario: Access-denied SQLException is propagated as QueryExecutionFailed

- GIVEN the JDBC driver throws a `SQLException` with message `Access denied for user 'u'@'h' to database 'x'`
- WHEN the engine maps the error
- THEN the repository returns `Result.failure(DatabaseError.QueryExecutionFailed(query, reason))` where `reason` contains `Access denied`
- AND the use case returns that same failure unchanged

#### Scenario: No active connection is surfaced as ConnectionFailed

- GIVEN no `MySQLEngine` is connected
- WHEN the use case is invoked with a valid name
- THEN the repository returns `Result.failure(DatabaseError.ConnectionFailed("No conectado"))`
- AND the use case returns that same failure unchanged
