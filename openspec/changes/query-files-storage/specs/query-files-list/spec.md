# query-files-list Specification

## Purpose

Defines the Query Files list screen — a real, navigable route that sits between the two "New Query" entry points and the existing `NewQueryOptionsSheet` selector. The screen lists the `.sql` files in the active connection engine's managed folder (via `app-file-storage`'s `QueryFileStore`), with the folder's actual contents as the single source of truth (no metadata table). It renders an empty state when the folder is empty or unavailable, sorts files most-recent-first, filters to `.sql` only, and exposes a floating "+" button (FAB) that is the SOLE new trigger of the unchanged `NewQueryOptionsSheet` overlay. Both existing entry points are rewired to navigate to this route instead of opening the selector directly. This capability owns the route, the list ViewModel state machine, the refresh policy, and the entry-point rewiring; it does NOT change the selector's internals or the open/run pickers' behavior.

## Requirements

### Requirement: List Screen Is a Route, Selector Stays an Overlay

The Query Files list screen MUST be a real navigable route (`Routes.QueryFiles = "connection/{connectionId}/query_files"`, contextual per convention), NOT an overlay. The existing `NewQueryOptionsSheet` MUST remain an overlay and MUST be triggered ONLY by this screen's FAB. Navigating to the list route MUST NOT itself open the selector, MUST NOT cause a workspace context switch, and MUST NOT produce a double sheet — preserving the `2026-06-30` new-query-modal guarantees.

#### Scenario: Entry point navigates to the list route, not the selector

- GIVEN the user triggers a "New Query" entry point
- WHEN the action runs
- THEN the app navigates to the `QueryFiles` route
- AND the `NewQueryOptionsSheet` is not shown yet
- AND no double sheet or context switch occurs

#### Scenario: FAB is the only new trigger of the selector

- GIVEN the user is on the Query Files list screen
- WHEN the user taps the FAB
- THEN the unchanged `NewQueryOptionsSheet` opens as an overlay
- AND no navigation route change occurs when it opens

### Requirement: Both Entry Points Rewired

The system MUST rewire BOTH existing "New Query" entry points — the bottom-nav modal action (`MyDataBasesNavHost.kt:141-147`) and the `DatabaseActionMenuScreen` "Consultas" tile (`:225-231`) — to navigate to the Query Files list route. Neither entry point MUST open `NewQueryOptionsSheet` directly anymore. The selector MUST only be reachable via the list screen's FAB.

#### Scenario: Bottom-nav action opens the list screen

- GIVEN the user taps the bottom-nav "New Query" modal action
- WHEN it activates
- THEN the Query Files list route opens
- AND the selector is not opened directly

#### Scenario: Consultas tile opens the list screen

- GIVEN the user taps the "Consultas" tile in `DatabaseActionMenuScreen`
- WHEN it activates
- THEN the Query Files list route opens
- AND the selector is not opened directly

### Requirement: List Content, Sorting, and Extension Filter

The screen MUST list files from the active engine's managed folder via `QueryFileStore.list(engine)`. It MUST include ONLY files whose name ends with `.sql` (case-insensitive extension match) and MUST exclude all other files. Each row MUST show the file `name` and its `lastModified` timestamp. The list MUST be sorted by `lastModified` descending (most-recent first), with file `name` as the tiebreaker. The folder's actual contents MUST be the source of truth; the system MUST NOT read from or require any metadata database.

#### Scenario: Files sorted most-recent-first

- GIVEN the folder contains `a.sql` (older) and `b.sql` (newer)
- WHEN the list loads
- THEN `b.sql` appears before `a.sql`

#### Scenario: Same-timestamp files break ties by name

- GIVEN two `.sql` files share the same `lastModified`
- WHEN the list loads
- THEN they are ordered by name as a tiebreaker

#### Scenario: Non-.sql files are filtered out

- GIVEN the folder contains `notes.txt`, `data.csv`, and `report.sql`
- WHEN the list loads
- THEN only `report.sql` is shown
- AND `notes.txt` and `data.csv` are excluded

#### Scenario: Extension match is case-insensitive

- GIVEN the folder contains `Query.SQL` and `other.Sql`
- WHEN the list loads
- THEN both `Query.SQL` and `other.Sql` are included

### Requirement: List State Machine

The list ViewModel MUST expose an immutable `StateFlow` with states `Idle → Loading → Success(list) | Error`, and MUST represent an available-but-empty folder distinctly (empty `Success` or a dedicated `Empty` state) so the UI can show an empty state rather than an error. The ViewModel MUST remain `Context`-free and map any store failure to a localized message. Loading the list MUST NOT block the main thread.

#### Scenario: Successful load exposes the file list

- GIVEN a folder with three `.sql` files
- WHEN the list loads
- THEN the state transitions `Loading → Success` carrying the three refs sorted most-recent-first

#### Scenario: Empty folder shows the empty state, not an error

- GIVEN the active engine's managed folder exists and contains no `.sql` files
- WHEN the list loads
- THEN the state indicates an empty list (not `Error`)
- AND the UI shows the empty state with the FAB available

#### Scenario: Store failure surfaces a localized error

- GIVEN `QueryFileStore.list(engine)` fails unexpectedly
- WHEN the list loads
- THEN the state is `Error` with a localized (es/en) message
- AND the ViewModel never references an Android `Context`

### Requirement: Engine Context Determines the Listed Folder

The list screen MUST scope its contents to the `DatabaseType` of the connection in context (`connectionId` mapped to its `DatabaseType`). When the user opens the list under a different engine's connection, the screen MUST show THAT engine's folder. The list MUST NOT mix files from multiple engines in one view.

#### Scenario: List reflects the in-context engine

- GIVEN the list route opened under a PostgreSQL connection
- WHEN the list loads
- THEN it shows files from `MyDataBase/postgresql/queries/`
- AND it does not show any `mysql/queries/` files

#### Scenario: Different engine connection shows a different folder

- GIVEN the list was previously viewed under a MySQL connection
- WHEN the user opens the list under a SQLite connection
- THEN the list shows the `sqlite/queries/` contents for that engine
- AND MySQL files are not shown

### Requirement: Refresh Policy on Resume

The list MUST refresh its contents when the screen resumes (e.g. returning from the editor or the selector, or re-entering the screen), so a file created or saved elsewhere becomes visible without an app restart. The system is NOT required to watch the folder for real-time external changes; refresh-on-resume is the specified behavior. A query saved from the editor MUST appear on the next resume of the list without an app restart.

#### Scenario: Saved query appears after returning to the list

- GIVEN the user saves a query from the editor for the in-context engine
- WHEN the user returns to the Query Files list screen
- THEN the list refreshes and shows the newly saved file

#### Scenario: Externally created file appears on resume, not in real time

- GIVEN the list screen is open and a `.sql` file is created externally in the folder
- WHEN the screen is resumed (re-entered or returned to)
- THEN the newly created file appears in the refreshed list
- AND the system is not required to have shown it in real time while backgrounded

### Requirement: Unavailable-Folder Empty State

When the managed folder is unavailable because a saved SAF grant was lost (`app-file-storage` fallback active), the list MUST NOT crash or show a hard error. It MUST fall back to listing the app-private default folder for the in-context engine and MUST surface the one-time fallback notice defined by `app-file-storage`. If that fallback folder is also empty, the empty state MUST be shown.

#### Scenario: SAF grant lost falls back to private folder listing

- GIVEN the saved SAF tree is unavailable when the list loads
- WHEN the list loads
- THEN it lists the app-private default folder for the in-context engine
- AND the one-time fallback notice is surfaced
- AND no crash or hard error state occurs

#### Scenario: Empty fallback folder shows the empty state

- GIVEN the fallback app-private folder for the engine is empty
- WHEN the list loads after a SAF grant loss
- THEN the empty state is shown with the FAB available

### Requirement: Open/Run Files Are Not Auto-Listed

Files opened or run via the existing `NewQueryOptionsSheet` pickers ("Open Query File" / "Run Script") MUST continue to operate in place and MUST NOT be auto-copied into the managed folder. Such files MUST NOT appear in the list unless the user explicitly saves them into the managed folder via the editor's default Save.

#### Scenario: Opening an arbitrary file does not add it to the list

- GIVEN the user opens an arbitrary `.sql` file via the picker from outside the managed folder
- WHEN the file is opened or run
- THEN it stays in its original location
- AND it does not appear in the Query Files list

#### Scenario: Explicitly saving an opened file adds it to the list

- GIVEN an arbitrary file was opened in the editor
- WHEN the user performs the editor's default Save
- THEN the content is written into the managed folder for the in-context engine
- AND it appears in the list on the next resume

## Non-Functional

- **Performance**: Listing MUST remain responsive for folders with many `.sql` files; the ViewModel MUST perform listing off the main thread and MUST NOT load file contents to build the list (name + `lastModified` only). Large folders MAY page or lazily render rows, but MUST NOT read every file's body to display the list.
- **Adaptive UI**: The list screen MUST render correctly across Compact, Medium, and Expanded `WindowSizeClass`.
- **Localization**: All user-facing text (screen title, empty-state copy, FAB content description, error and fallback notices) MUST exist in `values/` (en) and `values-es/` (es), plus the full locale set per the android-dev skill.
- **Regression guard**: A test MUST verify that entry points navigate to the route and the selector is only FAB-triggered, protecting the `2026-06-30` no-double-sheet / no-route-regression guarantee.
- **Testability**: The list ViewModel MUST be unit-testable on the JVM (state transitions, sorting, filtering, error mapping) with a fake `QueryFileStore`; the screen SHOULD have a Compose UI test for the load → list → FAB-opens-selector happy path and the empty state.
