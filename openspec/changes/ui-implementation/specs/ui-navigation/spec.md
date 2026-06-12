# ui-navigation Specification

## Purpose

Single-activity navigation skeleton using Navigation Compose with typed routes, predictable back stack, and adaptive scaffolding for Compact/Medium/Expanded `WindowSizeClass`.

## Requirements

### Requirement: Typed Routes

The system MUST expose all destinations as a sealed `Route` hierarchy. Untyped string routes MUST NOT appear in `NavHost` call sites.

#### Scenario: Navigate to connection form

- GIVEN the user is on the Connections list screen
- WHEN the user taps "New connection"
- THEN the app navigates to `Route.ConnectionForm(id = null)` and the back stack contains `Connections → ConnectionForm`

#### Scenario: Navigate with typed argument

- GIVEN the user opens an existing connection
- WHEN navigation pushes `Route.Databases(connectionId = 42L)`
- THEN the destination ViewModel receives `connectionId = 42L` via `SavedStateHandle`

### Requirement: Single-Activity Scaffold

The system SHALL host a single `MainActivity` containing one `NavHost`. Each screen MUST be a composable destination, not an `Activity` or `Fragment`.

#### Scenario: Configuration change preserves back stack

- GIVEN the user is on `Tables` after navigating `Connections → Databases → Tables`
- WHEN the device rotates
- THEN the back stack is preserved and pressing back returns to `Databases`

### Requirement: Deep-Link Safe Back Stack

The system MUST guarantee that the back button always reaches a valid previous destination or exits the app cleanly. Orphaned destinations MUST NOT exist.

#### Scenario: Back from top-level Settings

- GIVEN the user opened Settings from the Connections screen
- WHEN the user presses back
- THEN the app returns to Connections (not exit)

#### Scenario: Back from start destination

- GIVEN the user is on the start destination (Connections)
- WHEN the user presses back
- THEN the app exits

### Requirement: Adaptive Scaffold

The system SHALL compute `WindowSizeClass` at activity level and expose it via `CompositionLocal`. Each screen MAY adapt its layout based on the class.

#### Scenario: Compact phone layout

- GIVEN `WindowWidthSizeClass.Compact`
- WHEN any list+detail-capable screen renders
- THEN it renders a single pane

#### Scenario: Medium/Expanded layout

- GIVEN `WindowWidthSizeClass.Medium` or `Expanded`
- WHEN a list+detail-capable screen renders
- THEN it MAY render list and detail side-by-side

### Requirement: Navigation Graph Coverage

The graph MUST include: `Connections`, `ConnectionForm(id?)`, `Databases(connectionId)`, `Tables(connectionId, database)`, `TableViewer(connectionId, database, table)`, `QueryEditor(connectionId, database)`, `Settings`.

#### Scenario: All routes resolvable

- GIVEN the app starts cold
- WHEN any route is dispatched programmatically
- THEN it resolves without `IllegalArgumentException`

## Non-Functional

- **Performance**: Navigation transition MUST start within 100ms of user action on a mid-range device.
- **Testability**: Each route SHALL be unit-testable via `TestNavHostController`.
- **Accessibility**: Top app bar MUST expose a content description for the back action localized in es and en.
