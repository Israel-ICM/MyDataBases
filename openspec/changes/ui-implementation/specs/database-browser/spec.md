# database-browser Specification

## Purpose

Hierarchical browser for a live connection: databases → tables → rows, with a schema/columns view, paginated row loading, and clear error surfacing.

## Requirements

### Requirement: Databases List

After successful connection, the system MUST list all accessible databases for the current user.

#### Scenario: Successful listing

- GIVEN a successful connection
- WHEN the Databases screen loads
- THEN a `LazyColumn` of database names is shown within 2 seconds (mid-range device, LAN)

#### Scenario: Connection lost mid-load

- GIVEN the connection drops while databases are loading
- WHEN the load fails
- THEN a localized error is shown with a "Retry" action

### Requirement: Tables List

When the user selects a database, the system MUST list all tables in that database.

#### Scenario: Tables for selected database

- GIVEN the user selects database `payroll`
- WHEN the Tables screen loads
- THEN all tables in `payroll` are listed alphabetically

#### Scenario: Empty database

- GIVEN the selected database has no tables
- WHEN the screen loads
- THEN an empty-state message is shown

### Requirement: Table Viewer with Tabs

The table viewer MUST expose two tabs: `Rows` and `Schema`. The default tab SHALL be `Rows`.

#### Scenario: Switch to schema

- GIVEN the table viewer is open on `Rows`
- WHEN the user taps the `Schema` tab
- THEN column name, data type, nullable flag, and key info are shown

### Requirement: Paginated Row Loading

The Rows tab MUST page results. Initial fetch MUST cap at 1000 rows. A "Load more" action SHALL fetch the next page.

#### Scenario: Initial page

- GIVEN a table with 5000 rows
- WHEN the user opens it
- THEN at most 1000 rows are fetched AND a "Load more" button is shown at the bottom

#### Scenario: Load more

- GIVEN the user is at the bottom of an initial page
- WHEN the user taps "Load more"
- THEN the next page (up to 1000 rows) is appended

#### Scenario: Stable scroll keys

- GIVEN a long list of rows
- WHEN the user scrolls
- THEN `LazyColumn` uses a stable key per row (primary key or row index) AND no flicker occurs

### Requirement: Error Surfacing

All engine errors MUST be displayed as user-readable, localized messages. Raw SQL exceptions MUST NOT leak into the UI.

#### Scenario: Permission denied

- GIVEN the user lacks SELECT privilege on a table
- WHEN they tap the table
- THEN a localized "Insufficient permissions" message is shown

### Requirement: Loading and Empty States

Every list (databases, tables, rows) MUST distinguish: `Loading`, `Empty`, `Error`, `Success` states via a sealed `UiState`.

#### Scenario: Loading state

- GIVEN a list is being fetched
- WHEN the screen renders
- THEN a progress indicator is shown AND the list area is not interactive

## Non-Functional

- **Performance**: Initial page render MUST complete within 1.5 seconds on LAN for up to 1000 rows. Memory MUST stay under 50MB for the table viewer.
- **Security**: Result data MUST NOT be cached on disk; in-memory only for the screen lifecycle.
- **Testability**: ViewModels MUST be unit-testable with a fake `DatabaseRepository`. Pagination logic MUST be covered by unit tests.
- **Accessibility**: List items MUST have content descriptions; tabs MUST be reachable via keyboard / d-pad navigation.
