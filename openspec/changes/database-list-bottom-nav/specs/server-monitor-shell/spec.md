# Server Monitor Shell Specification

## Purpose

Defines the placeholder `MonitorScreen` reached at `connection/{connectionId}/monitor`. This change introduces ONLY the UI shell: a top app bar and a `TabRow` with three tabs — Metrics, Queries, Health. Each tab renders an empty placeholder. Real metrics collection, query log streaming, and health checks are out of scope.

## Requirements

### Requirement: Three-Tab Shell

The screen MUST render a `TabRow` with exactly three tabs in this order: Metrics, Queries, Health. The first tab (Metrics) MUST be selected by default.

#### Scenario: Initial render shows three tabs with Metrics active

- GIVEN the user navigates to `connection/c-42/monitor`
- WHEN `MonitorScreen` renders for the first time
- THEN a `TabRow` is visible with three tabs in order: Metrics, Queries, Health
- AND the Metrics tab is selected
- AND the Metrics placeholder content is visible

#### Scenario: Tap switches active tab

- GIVEN the Metrics tab is selected
- WHEN the user taps the Queries tab
- THEN the Queries tab becomes selected
- AND the Queries placeholder content replaces the Metrics placeholder

#### Scenario: All three tabs are independently tappable

- GIVEN any tab is currently selected
- WHEN the user taps another tab
- THEN that tab becomes selected and its placeholder content renders
- AND the previously selected tab returns to its unselected visual state

### Requirement: Placeholder Content per Tab

Each tab MUST display a placeholder body composed of an icon (or illustration), a localized title, and a localized "Coming soon" message. No real data MUST be fetched.

#### Scenario: Metrics placeholder

- GIVEN the Metrics tab is selected
- WHEN its content renders
- THEN a placeholder body shows a chart-style icon, a title "Server metrics" (en) / "Métricas del servidor" (es), and the message "Coming soon" (en) / "Próximamente" (es)

#### Scenario: Queries placeholder

- GIVEN the Queries tab is selected
- WHEN its content renders
- THEN a placeholder body shows a list/log icon, title "Query log" (en) / "Registro de consultas" (es), and "Coming soon" / "Próximamente"

#### Scenario: Health placeholder

- GIVEN the Health tab is selected
- WHEN its content renders
- THEN a placeholder body shows a heart/pulse icon, title "Health check" (en) / "Estado del servidor" (es), and "Coming soon" / "Próximamente"

#### Scenario: No network or driver calls are made

- GIVEN the Monitor screen is rendered or any tab is switched
- WHEN the operation completes
- THEN no MySQL driver call is made
- AND no metric collector is started
- AND `MySQLConnectionPool.activeConnection` is not accessed

### Requirement: Localized Tab Labels

Tab labels MUST be localized in `es` and `en`.

#### Scenario: English tab labels

- GIVEN the device locale is `en`
- WHEN the `TabRow` renders
- THEN the labels read: "Metrics", "Queries", "Health"

#### Scenario: Spanish tab labels

- GIVEN the device locale is `es`
- WHEN the `TabRow` renders
- THEN the labels read: "Métricas", "Consultas", "Estado"

### Requirement: Navigation and connectionId

The screen MUST receive `connectionId` from `SavedStateHandle` and the system back action MUST return to `connection/{connectionId}/databases`.

#### Scenario: connectionId is wired through

- GIVEN the user navigates to `connection/c-42/monitor`
- WHEN `MonitorScreen`'s ViewModel (or screen state) is instantiated
- THEN `SavedStateHandle["connectionId"]` equals `"c-42"`
- AND the value is available for future implementations (no consumer in this change)

#### Scenario: System back returns to database list

- GIVEN the user is on `connection/c-42/monitor`
- WHEN the user presses the system back button
- THEN the NavController pops to `connection/c-42/databases`

#### Scenario: Bottom navigation stays visible

- GIVEN the user is on `connection/c-42/monitor` in `Compact` WindowSizeClass
- WHEN the screen renders
- THEN the 4-item server menu (Add database, New query, Monitor, Settings) remains visible
- AND the Monitor item is shown as the active destination

### Requirement: Adaptive Layout

The shell MUST render correctly across `Compact`, `Medium`, and `Expanded` WindowSizeClass.

#### Scenario: Compact uses standard TabRow

- GIVEN WindowSizeClass is `Compact`
- WHEN the screen renders
- THEN tabs are arranged in a horizontal `TabRow` with equal weight

#### Scenario: Medium/Expanded keeps tabs visible

- GIVEN WindowSizeClass is `Medium` or `Expanded`
- WHEN the screen renders
- THEN all three tab labels remain visible (no overflow to a menu)
- AND the active tab indicator is clearly visible

### Requirement: Accessibility

The tabs and placeholder content MUST be accessible.

#### Scenario: Tab selection is announced

- GIVEN TalkBack is on
- WHEN the user taps a tab
- THEN the tab is announced with its localized label and "selected" state

#### Scenario: Placeholder content is announced

- GIVEN TalkBack is on
- WHEN a tab's placeholder body renders
- THEN the icon has a non-empty `contentDescription` matching the tab title
- AND the "Coming soon" message is part of the readable order
