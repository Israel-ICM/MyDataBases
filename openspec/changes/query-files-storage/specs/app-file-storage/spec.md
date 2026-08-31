# app-file-storage Specification

## Purpose

Defines the reusable, `DatabaseType`-scoped storage abstraction that owns where the app writes and reads its `.sql` query files. This capability provides a `domain` interface (`QueryFileStore`) built over a generic, engine-keyed `AppFolder` resolver, backed by two `data` implementations — one over app-private external `File` storage and one over a user-granted SAF tree — unified behind a `DocumentFile`-shaped `QueryFileRef` that exposes a `content://`/`file://` `Uri` compatible with existing `contentResolver.openInputStream(...)` consumers. It also owns the Settings-driven storage-location preference (nullable SAF-tree URI), the copy-on-relocation migration, the SAF permission-loss fallback, and the convergence of the query editor's default Save onto this store (removing the legacy MediaStore `Documents/MyDatabase/query` path while keeping explicit "Save As" export). Every path-resolving operation takes a `DatabaseType` engine key from day one so future folder types and engines add no breaking API change.

## Requirements

### Requirement: Engine-Scoped Store Contract

The system MUST expose a `domain` interface `QueryFileStore` whose every path-resolving operation takes a `DatabaseType` engine parameter. The interface MUST provide at minimum: `list(engine): List<QueryFileRef>`, `read(ref): String`, `write(engine, name, content): QueryFileRef`, and `delete(ref)`. The engine parameter MUST NOT be optional on any path-resolving call. `QueryFileRef` MUST carry `uri: Uri`, `name: String`, and `lastModified: Long`, and MUST NOT expose the underlying `File`/`DocumentFile` type to the `domain` layer. The interface MUST be defined independently of Android `Context` at the `domain` boundary and be unit-testable via fakes.

#### Scenario: Every path-resolving call requires an engine key

- GIVEN the `QueryFileStore` interface
- WHEN its `list`, `write`, and path-resolving signatures are inspected
- THEN each accepts a `DatabaseType` parameter with no default value
- AND adding a hypothetical future folder type would not change these signatures

#### Scenario: QueryFileRef hides the storage backend

- GIVEN a `QueryFileRef` returned by `list(engine)` or `write(...)`
- WHEN the `domain` layer consumes it
- THEN it sees only `uri`, `name`, and `lastModified`
- AND it cannot tell whether the source is a private `File` or a SAF tree

### Requirement: Engine-Keyed Path Layout

The system MUST resolve every query folder to the path `MyDataBase/{engineType}/queries/`, where `{engineType}` is `DatabaseType.name.lowercase()` — exactly `mysql`, `mariadb`, `postgresql`, or `sqlite`. The `queries` leaf MUST be constant and MUST sit below the `{engineType}` segment so future folder types share the same `{engineType}/` parent. A generic `AppFolder` sealed type (with only `Queries` defined now) MUST resolve via `AppFolder.resolve(engine, folder)`. The on-disk engine segment MUST be lowercase; the raw enum name (`MYSQL`) and `displayName` (`MySQL`) MUST NOT be used for the path.

#### Scenario: MySQL query resolves to the mysql segment

- GIVEN `engine = DatabaseType.MYSQL` and `folder = AppFolder.Queries`
- WHEN `AppFolder.resolve(engine, folder)` runs
- THEN the resolved base path ends with `MyDataBase/mysql/queries/`

#### Scenario: PostgreSQL query resolves to the postgresql segment

- GIVEN `engine = DatabaseType.POSTGRESQL`
- WHEN the folder is resolved for `Queries`
- THEN the base path ends with `MyDataBase/postgresql/queries/`

#### Scenario: Engine segment is lowercase, not the enum name

- GIVEN any `DatabaseType`
- WHEN its path segment is computed
- THEN the segment equals `DatabaseType.name.lowercase()`
- AND it is never `MYSQL`, `MariaDB`, or any mixed-case form

### Requirement: Per-Engine Isolation

The system MUST keep each engine's queries isolated: a query written under one engine MUST NOT appear in another engine's list. Connections that share the same `DatabaseType` MUST share the same engine folder; the individual `connectionId` MUST NOT scope storage. The `connectionId` MAY still flow through execution context and MUST be mapped to its `DatabaseType` for storage scoping.

#### Scenario: A MySQL query does not appear in the Postgres list

- GIVEN a query saved via `write(MYSQL, "a.sql", ...)`
- WHEN `list(POSTGRESQL)` is called
- THEN the returned list does not include `a.sql`

#### Scenario: Same-engine connections share one folder

- GIVEN two distinct MySQL connections
- WHEN each saves a query for engine `MYSQL`
- THEN both files land under `MyDataBase/mysql/queries/`
- AND both appear in `list(MYSQL)`

### Requirement: Storage Model Selection

The system MUST select the backing storage model from a single Settings preference: a nullable SAF-tree URI string. When the preference is `null`, the system MUST use the app-private default at `getExternalFilesDir(null)/MyDataBase/{engineType}/queries/` via `DocumentFile.fromFile(...)`. When the preference holds a valid, permission-backed tree URI, the system MUST resolve `{engineType}/queries/` lazily under that single granted root via `DocumentFile.fromTreeUri(...)`. Both paths MUST expose the same `QueryFileRef.uri`, and consumers MUST use one code path (`contentResolver.openInputStream(ref.uri)`) for both. The user MUST grant exactly ONE tree root that covers all engine types; the system MUST NOT require a separate tree per engine. Missing `{engineType}/queries/` subfolders MUST be created lazily on first write.

#### Scenario: Null preference uses the app-private default

- GIVEN the storage-location preference is `null`
- WHEN `write(MYSQL, "q.sql", ...)` runs
- THEN the file is written under `getExternalFilesDir(null)/MyDataBase/mysql/queries/`
- AND `QueryFileRef.uri` is a `file://` URI readable via `openInputStream`

#### Scenario: SAF preference uses one granted root for all engines

- GIVEN the preference holds a valid tree URI granted once by the user
- WHEN queries are written for both `MYSQL` and `POSTGRESQL`
- THEN both resolve under the same root as `root/mysql/queries/` and `root/postgresql/queries/`
- AND no additional tree grant is requested

#### Scenario: Missing engine subfolder is created lazily

- GIVEN a granted SAF root with no `sqlite/queries/` subfolder yet
- WHEN the first `write(SQLITE, ...)` runs
- THEN the `sqlite/queries/` subfolders are created under the root
- AND the write succeeds

#### Scenario: Both models expose an openable Uri

- GIVEN a `QueryFileRef` from either storage model
- WHEN `contentResolver.openInputStream(ref.uri)` is called
- THEN it returns a readable stream for both `file://` and `content://` URIs

### Requirement: SAF Permission Persistence and Loss Fallback

When the user picks a SAF tree, the system MUST call `takePersistableUriPermission` so the grant survives process death. On list load and on app start, the system MUST detect an unavailable saved tree URI (grant revoked, SD card removed, or app data cleared) and MUST fall back to the app-private default for reads and writes, MUST surface a one-time notice to the user, and MUST retain the preference value so the user can re-pick. The system MUST NOT crash, MUST NOT present a hard error state, and MUST NOT silently discard the preference.

#### Scenario: Persisted permission survives process death

- GIVEN the user granted a SAF tree in a prior process
- WHEN the app restarts and reads the preference
- THEN the tree URI is still usable without re-prompting

#### Scenario: Revoked grant falls back to private default

- GIVEN the saved tree URI is no longer permission-backed (revoked or SD removed)
- WHEN the list loads or the app starts
- THEN the system reads/writes from the app-private default instead
- AND a one-time notice is surfaced
- AND the preference value is retained so the user can re-pick
- AND no crash or hard error state occurs

#### Scenario: Grant lost mid-session

- GIVEN a SAF grant was valid at load but becomes unavailable during the session
- WHEN the next store operation runs against the tree
- THEN the system detects the loss and falls back to the app-private default
- AND surfaces the one-time notice without crashing

### Requirement: Copy-on-Relocation Migration

When the user changes the storage location in Settings AND `.sql` files exist in the previous location, the system MUST offer a one-time prompt ("Copy existing query files to the new location?"). If the user confirms, the system MUST COPY (not move) the `.sql` files to the new location, leaving the originals in place. If the user declines, the new location MUST start empty and the old files MUST remain in place (orphaned but recoverable by reverting the preference). The system MUST NOT move or delete files from the old location during relocation. Migration MUST preserve the per-engine `{engineType}/queries/` partitioning.

#### Scenario: Confirmed relocation copies files and preserves originals

- GIVEN the old location has `mysql/queries/a.sql` and the user picks a new tree
- WHEN the user confirms the copy prompt
- THEN `a.sql` is copied into the new location's `mysql/queries/`
- AND the original `a.sql` still exists in the old location

#### Scenario: Declined relocation starts fresh without data loss

- GIVEN existing `.sql` files in the old location and a newly picked location
- WHEN the user declines the copy prompt
- THEN the new location starts empty
- AND the old files remain in place and become visible again if the preference is reverted

#### Scenario: No prompt when the old location is empty

- GIVEN the previous location contains no `.sql` files
- WHEN the user picks a new location
- THEN no copy prompt is shown
- AND the new location is used directly

### Requirement: Save Convergence to the Managed Folder

The system MUST make the query editor's default Save (Save button and Save shortcut) write through `QueryFileStore.write(engine, name, content)` targeting the managed folder for the active connection's `DatabaseType`, so saved queries appear in the list without an app restart. The legacy `Environment.getExternalStorageDirectory()` / MediaStore `Documents/MyDatabase/query` save path MUST be removed and MUST NOT be produced by the default Save. The explicit "Save As" export (`CreateDocument` picker) MUST remain available for exporting to arbitrary user-chosen locations, and files exported that way MUST NOT be required to appear in the managed list.

#### Scenario: Default Save lands in the engine's managed folder

- GIVEN an active MySQL connection and an edited query
- WHEN the user taps the default Save
- THEN the content is written via `QueryFileStore.write(MYSQL, name, content)` under `MyDataBase/mysql/queries/`
- AND the file appears in `list(MYSQL)` without an app restart

#### Scenario: Legacy MediaStore path is not written

- GIVEN the user performs a default Save
- WHEN the write completes
- THEN no file is written to `Documents/MyDatabase/query` via MediaStore

#### Scenario: Save As still exports elsewhere

- GIVEN an edited query
- WHEN the user chooses "Save As" and picks an external location
- THEN the file is exported to that location via `CreateDocument`
- AND it is not required to appear in the managed list

## Non-Functional

- **Security**: Query file content MUST NOT be written to logs. SAF grants MUST use `takePersistableUriPermission`; the app MUST NOT request broad legacy external-storage permissions for this feature.
- **Durability**: The app-private default is wiped on uninstall; the system MUST message this and offer the SAF redirect for durable storage.
- **Compatibility**: One `QueryFileStore` code path MUST serve both storage models via `DocumentFile` (`fromFile()` / `fromTreeUri()`) exposing `QueryFileRef.uri`.
- **Testability**: `AppFolder` resolution and the store contract MUST be unit-testable on the JVM with fakes/Mockk (no Robolectric); the two `data` implementations SHOULD have instrumented coverage for the `file://` and `content://` paths.
- **Localization**: The relocation prompt, the SAF permission-loss notice, and any storage error message MUST exist in `values/` (en) and `values-es/` (es), plus the full locale set per the android-dev skill.
