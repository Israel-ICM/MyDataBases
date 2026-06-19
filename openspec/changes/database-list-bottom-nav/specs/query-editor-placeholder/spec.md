# Query Editor Placeholder Specification

## Purpose

Defines the `NewQueryScreen` reached at `connection/{connectionId}/new-query`. This change introduces ONLY a placeholder: a top app bar, an illustration/icon, a localized title, and a "Coming soon" message. The real SQL editor is deferred to a follow-up change. This spec exists to keep the bottom-nav destination reachable and discoverable without committing to editor behavior now.

## Requirements

### Requirement: Placeholder UI

The screen MUST render a centered placeholder body containing an icon, a localized title, and a localized message. The screen MUST NOT render any text input, syntax highlighter, or query history.

#### Scenario: Initial render shows placeholder body

- GIVEN the user navigates to `connection/c-42/new-query`
- WHEN `NewQueryScreen` renders
- THEN a centered placeholder is visible with: a code/query icon, a title, and a message
- AND no `TextField`, `BasicTextField`, or editor surface is present

#### Scenario: English placeholder text

- GIVEN the device locale is `en`
- WHEN the placeholder renders
- THEN the title reads "Query editor" and the message reads "Coming soon"

#### Scenario: Spanish placeholder text

- GIVEN the device locale is `es`
- WHEN the placeholder renders
- THEN the title reads "Editor de consultas" and the message reads "Próximamente"

#### Scenario: No actions beyond navigation

- GIVEN the placeholder is rendered
- WHEN the user inspects the screen
- THEN there are no actionable buttons besides the system back affordance and the bottom-nav items
- AND no "Run", "Save", or "History" controls are present

### Requirement: Navigation and connectionId

The screen MUST receive `connectionId` from `SavedStateHandle`. The system back action MUST return to `connection/{connectionId}/databases`.

#### Scenario: connectionId is wired through

- GIVEN the user navigates to `connection/c-42/new-query`
- WHEN the screen is instantiated
- THEN `SavedStateHandle["connectionId"]` equals `"c-42"`
- AND the value is exposed in screen state for future consumers (no consumer in this change)

#### Scenario: System back returns to database list

- GIVEN the user is on `connection/c-42/new-query`
- WHEN the user presses the system back button
- THEN the NavController pops to `connection/c-42/databases`

#### Scenario: Bottom navigation stays visible

- GIVEN the user is on `connection/c-42/new-query` in `Compact` WindowSizeClass
- WHEN the screen renders
- THEN the 4-item server menu remains visible
- AND the "New query" item is shown as the active destination

### Requirement: No Backend or State Side Effects

The placeholder MUST NOT touch driver, network, or persistence layers.

#### Scenario: No driver calls

- GIVEN the screen is rendered or interacted with
- WHEN any user gesture occurs
- THEN no MySQL driver call is made
- AND `MySQLConnectionPool.activeConnection` is not accessed

#### Scenario: No persistence

- GIVEN the screen is rendered
- WHEN the lifecycle completes
- THEN no Room write occurs
- AND no DataStore entry is mutated

### Requirement: Adaptive Layout

The placeholder MUST render correctly across `Compact`, `Medium`, and `Expanded` WindowSizeClass.

#### Scenario: Compact centers content

- GIVEN WindowSizeClass is `Compact`
- WHEN the screen renders
- THEN the placeholder body is vertically and horizontally centered in the available content area

#### Scenario: Medium/Expanded keeps content centered with max width

- GIVEN WindowSizeClass is `Medium` or `Expanded`
- WHEN the screen renders
- THEN the placeholder body is centered
- AND its content is constrained to a readable max width (e.g. 480dp)
- AND it does not stretch edge-to-edge

### Requirement: Accessibility

The placeholder MUST be readable by assistive technologies.

#### Scenario: Icon has content description

- GIVEN the placeholder icon renders
- WHEN a screen reader inspects it
- THEN the icon exposes a `contentDescription` equal to (or describing) the localized title

#### Scenario: Title and message are in reading order

- GIVEN TalkBack is on
- WHEN the user navigates the screen
- THEN the title is read first, followed by the "Coming soon" message
- AND no decorative element interrupts the reading order
