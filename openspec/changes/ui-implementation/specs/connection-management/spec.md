# connection-management Specification

## Purpose

CRUD UI for database connections with encrypted credential persistence (Room + Android Keystore), test-connection flow, and a typed form for host, port, user, password, database, and SSH tunnel toggle.

**UI Pattern**: Flat list of all connections + FAB with type selector bottom sheet.

## Requirements

### Requirement: Connections List

The system MUST display all saved connections in a flat `LazyColumn` (no accordion grouping) showing connection name, type, host:port. Each item MUST show the database type icon with accent color and an edit button. An empty-state SHALL invite the user to tap the FAB to create the first connection.

#### Scenario: Empty state

- GIVEN no saved connections
- WHEN the user opens the Connections screen
- THEN an empty-state message "No connections" and "Tap + to add your first database connection" are shown
- AND a FAB with + icon is visible

#### Scenario: List rendering

- GIVEN three saved connections (MySQL, PostgreSQL, MongoDB)
- WHEN the screen renders
- THEN all three items are visible in a flat list
- AND each shows: type icon (colored), connection name, "TYPE • host:port" subtitle, and edit button
- AND connections are NOT grouped by type

#### Scenario: Add connection via FAB

- GIVEN the user is on the Connections list
- WHEN the user taps the FAB
- THEN a BottomSheet opens showing all 4 database types as cards (MySQL, PostgreSQL, SQLite, MariaDB)
- AND each card shows: type icon (48dp, colored), type name (bold), and description
- WHEN the user taps a type card
- THEN the type selector closes AND the connection form opens with that type preselected

### Requirement: Create / Edit Connection

The form MUST accept: name (required), type (4 types: MySQL, PostgreSQL, SQLite, MariaDB), host (required), port (required, 1–65535), user (required), password (required), database (optional), SSH tunnel toggle. Validation errors MUST display inline.

When creating via FAB → type selector, the type field is preselected and immutable. When editing, the type field shows current value and is editable.

#### Scenario: Valid create

- GIVEN all required fields are valid
- WHEN the user taps "Save"
- THEN the connection is persisted AND the user returns to the Connections list with the new item visible

#### Scenario: Invalid port

- GIVEN port = `70000`
- WHEN the user taps "Save"
- THEN an inline error appears on the port field AND no Room write occurs

#### Scenario: Edit existing

- GIVEN an existing connection with id 42
- WHEN the user opens the form for id 42, edits the alias, and saves
- THEN the same Room row is updated (not duplicated)

### Requirement: Delete Connection

Delete MUST require an explicit confirmation dialog. Successful delete MUST remove the row from Room and the list.

#### Scenario: Delete with confirmation

- GIVEN a saved connection
- WHEN the user swipes or selects "Delete" AND confirms
- THEN the row is removed from Room AND the list updates

#### Scenario: Delete cancelled

- GIVEN the confirmation dialog is shown
- WHEN the user taps "Cancel"
- THEN no delete occurs

### Requirement: Test Connection

The form MUST expose a "Test connection" action that attempts a real connection without saving. Result MUST be surfaced as success or a clear error message.

#### Scenario: Successful test

- GIVEN valid credentials reachable on the network
- WHEN the user taps "Test"
- THEN a success snackbar appears within 5 seconds

#### Scenario: Failed test

- GIVEN unreachable host or wrong credentials
- WHEN the user taps "Test"
- THEN a localized error message is shown AND the form remains editable

### Requirement: Encrypted Credential Persistence

Passwords MUST be encrypted at rest using `androidx.security.crypto` with a `MasterKey` backed by Android Keystore. Plaintext passwords MUST NOT appear in Room, logs, or DataStore.

#### Scenario: Password encryption round-trip

- GIVEN a saved connection with password "secret"
- WHEN the row is read from Room
- THEN the in-memory value matches "secret" AND the on-disk database byte sequence does NOT contain "secret"

#### Scenario: No plaintext in logs

- GIVEN any save, edit, or test action
- WHEN logcat is captured
- THEN no log line contains the password value

### Requirement: Last-Used Connection

The id of the last successfully connected connection SHALL be stored in DataStore for quick reopen.

#### Scenario: Last-used hint

- GIVEN the user connected to id 7 last session
- WHEN the user opens the Connections screen
- THEN item 7 is visually marked as "last used"

## UI Components

### DatabaseTypeSelectorCard

A card composable for the type selector BottomSheet:
- **Icon**: Type icon, 48dp, colored with `DbAccents.getColorFor(type)`
- **Title**: `type.displayName`, 18sp, SemiBold
- **Description**: Short description of the database (e.g., "Popular open-source relational database" for MySQL)
- **Interaction**: Full card clickable, closes BottomSheet and opens form with preselected type

### IOSListItem (for connection list)

Reuses existing iOS-style list item:
- **Leading icon**: Type icon, 32dp, colored
- **Title**: Connection name
- **Subtitle**: "TYPE • host:port"
- **Trailing**: Edit IconButton
- **Click**: Connects to the database

## Non-Functional

- **Security**: Master key alias MUST be unique per app install; key MUST NOT leave Keystore; key MUST be `AES256_GCM`.
- **Performance**: List render MUST complete within 200ms for up to 100 connections.
- **Testability**: Encryption round-trip MUST be covered by an integration test; ViewModel MUST be unit-testable with a fake `ConnectionRepository`.
- **Accessibility**: All form fields MUST have associated labels and content descriptions in es and en.
- **UX**: Type selector BottomSheet MUST show all 4 types (MySQL, PostgreSQL, SQLite, MariaDB); FAB MUST be visible when connections exist; empty state MUST guide user to FAB.
