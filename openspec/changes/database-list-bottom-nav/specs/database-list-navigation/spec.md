# Database List Navigation Specification

## Purpose

Defines the contextual bottom navigation that appears while the user is "inside a server, no database selected yet". The bar lives on the `connection/{connectionId}/databases` route and exposes four actions: Add database, New query, Monitor, Settings. Navigation state derives purely from the current route via `NavigationContext` and `destinationsForContext`.

## Requirements

### Requirement: Contextual Route for Database List

The system MUST route the database list under `connection/{connectionId}/databases` and MUST resolve the navigation context to `InsideConnection(connectionId)` for that route.

#### Scenario: Connect navigates to contextual database list

- GIVEN the user is on the connections screen with a saved connection whose id is `c-42`
- WHEN the user taps "Connect" on that connection card
- THEN the NavController navigates to `connection/c-42/databases`
- AND `NavigationContext.from("connection/c-42/databases")` returns `InsideConnection(connectionId = "c-42")`

#### Scenario: Legacy flat route is removed

- GIVEN the codebase no longer declares `Routes.DatabaseList = "database_list"`
- WHEN any non-test source file is searched for the literal `"database_list"`
- THEN no matches SHALL be found

#### Scenario: connectionId is preserved through navigation

- GIVEN the user is on `connection/c-42/databases`
- WHEN the user taps any bottom-nav item
- THEN the resulting route MUST contain `connection/c-42/` as its prefix
- AND `connectionId` MUST NOT be lost or replaced

### Requirement: Four-Item Server Menu

The system MUST render a 4-item bottom navigation bar when the current route ends with `/databases`. Items MUST appear in this order: Add database, New query, Monitor, Settings.

#### Scenario: Bar renders 4 items on database list

- GIVEN the user is on `connection/c-42/databases`
- WHEN the adaptive scaffold computes destinations via `destinationsForContext(InsideConnection("c-42"), "connection/c-42/databases")`
- THEN the returned list has exactly 4 entries
- AND the entries are, in order: Add database, New query, Monitor, Settings

#### Scenario: DB-level 5-item bar still renders elsewhere

- GIVEN the user is on `connection/c-42/tables` (or `/views`, `/editor`, `/functions`, `/backup`)
- WHEN `destinationsForContext` is called with that route
- THEN the returned list is the existing 5-item DB menu
- AND it MUST NOT contain Add database, New query, or Monitor

#### Scenario: OutsideConnection bar is unaffected

- GIVEN the user is on `connections`
- WHEN `destinationsForContext(OutsideConnection, "connections")` is called
- THEN the returned list is the existing Connections + Settings pair
- AND the 4-item server menu MUST NOT be rendered

### Requirement: Navigation Targets

Tapping a bottom-nav item MUST navigate to the route matching that destination, scoped to the current `connectionId`. Each target route MUST be reachable and MUST accept `connectionId` as a `navArgument`.

#### Scenario: Tap Add database

- GIVEN the user is on `connection/c-42/databases`
- WHEN the user taps the "Add database" item
- THEN the NavController navigates to `connection/c-42/add-database`
- AND the destination Composable receives `connectionId = "c-42"` via `SavedStateHandle`

#### Scenario: Tap New query

- GIVEN the user is on `connection/c-42/databases`
- WHEN the user taps the "New query" item
- THEN the NavController navigates to `connection/c-42/new-query`

#### Scenario: Tap Monitor

- GIVEN the user is on `connection/c-42/databases`
- WHEN the user taps the "Monitor" item
- THEN the NavController navigates to `connection/c-42/monitor`

#### Scenario: Tap Settings

- GIVEN the user is on `connection/c-42/databases`
- WHEN the user taps the "Settings" item
- THEN the NavController navigates to the existing `Routes.Settings` target scoped to `connectionId = "c-42"`
- AND the existing Settings screen renders without modification

### Requirement: Adaptive Behavior Across WindowSizeClass

The 4-item bar MUST render correctly in `Compact`, `Medium`, and `Expanded` WindowSizeClass, using the same adaptive container (`NavigationBar`/`NavigationRail`) the existing bars use.

#### Scenario: Compact renders bottom bar

- GIVEN WindowSizeClass is `Compact`
- WHEN the user is on `connection/c-42/databases`
- THEN the 4 items render in a bottom `NavigationBar`

#### Scenario: Medium and Expanded render rail

- GIVEN WindowSizeClass is `Medium` or `Expanded`
- WHEN the user is on `connection/c-42/databases`
- THEN the 4 items render in a `NavigationRail` (or the existing equivalent for that size class)
- AND no item is clipped or hidden

#### Scenario: showMenu filter passes the new route

- GIVEN the `showMenu` filter in `AdaptiveNavigationScaffold`
- WHEN the current route is `connection/{any}/databases`
- THEN `showMenu` returns true
- AND the bar is not suppressed

### Requirement: Localized Labels and Icons

Each of the four items MUST have a localized label in `es` and `en`, and a Phosphor icon registered under `PhosphorAppIcons.Nav`.

#### Scenario: English labels

- GIVEN the device locale is `en`
- WHEN the 4-item bar renders
- THEN labels read: "Add database", "New query", "Monitor", "Settings"

#### Scenario: Spanish labels

- GIVEN the device locale is `es`
- WHEN the 4-item bar renders
- THEN labels read: "Añadir base de datos", "Nueva consulta", "Monitor", "Ajustes"

#### Scenario: Icons resolve

- GIVEN the navigation destinations are built
- WHEN each destination's `icon` is referenced
- THEN `PhosphorAppIcons.Nav.addDatabase`, `Nav.newQuery`, and `Nav.monitor` MUST resolve to a non-null Phosphor ImageVector
- AND the existing `Nav.settings` icon is reused without modification

#### Scenario: Accessibility content description

- GIVEN any of the 4 items renders
- WHEN a screen reader inspects the item
- THEN the item exposes a `contentDescription` matching its localized label
- AND the item is focusable via TalkBack/keyboard

### Requirement: ViewModel Reads connectionId from SavedStateHandle

`DatabasesListViewModel` MUST receive `connectionId` from `SavedStateHandle` and MUST NOT depend on `MySQLConnectionPool.activeConnection` to load databases.

#### Scenario: ViewModel loads databases for the navArg connectionId

- GIVEN the user navigates to `connection/c-42/databases`
- WHEN `DatabasesListViewModel` is instantiated by Hilt
- THEN `SavedStateHandle["connectionId"]` equals `"c-42"`
- AND `loadDatabases()` queries using that `connectionId`

#### Scenario: Missing connectionId fails loudly

- GIVEN `SavedStateHandle` does not contain `connectionId` (programmer error)
- WHEN `DatabasesListViewModel` initializes
- THEN the ViewModel surfaces an error state (e.g. `UiState.Error`) instead of silently using a global singleton
- AND the screen renders an error message

#### Scenario: Singleton fallback is removed

- GIVEN the source of `DatabasesListViewModel`
- WHEN it is inspected for references to `MySQLConnectionPool.activeConnection`
- THEN no such reference SHALL remain in `loadDatabases()` or any code path reached from it
