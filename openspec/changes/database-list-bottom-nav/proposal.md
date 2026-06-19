# Proposal: Database List Bottom Navigation

## Intent

The database list screen currently has no contextual navigation: users land on `database_list` after connecting to a server with no way to add a database, open a query editor, view server health, or jump to settings without going back. The root cause is that `database_list` is a flat legacy route that does not match the `connection/{id}/...` regex used by `NavigationContext.from(route)`, so the adaptive scaffold falls back to the wrong context (`OutsideConnection`) and renders semantically incorrect items.

This change introduces a contextual bottom navigation for the "inside a server, no DB selected yet" state with four actions: Add database, New query, Monitor, Settings.

## Scope

### In Scope
- Promote `database_list` to contextual route `connection/{connectionId}/databases` (Option B from exploration)
- Add four new navigation destinations: `AddDatabase`, `NewQuery`, `Monitor`, `Settings` (Settings reused)
- Add three new routes: `Routes.AddDatabase`, `Routes.NewQuery`, `Routes.Monitor` (all contextual on `connectionId`)
- Add three new screens: `AddDatabaseScreen` (functional form), `NewQueryScreen` (placeholder), `MonitorScreen` (placeholder with tabs)
- Branch `destinationsForContext` on route suffix so `/databases` returns the 4-item server menu while other `InsideConnection` routes keep the 5-item DB menu
- Wire `DatabasesListScreen` and `DatabasesListViewModel` to receive `connectionId` from `SavedStateHandle`
- Fix the existing TODO in `MyDataBasesNavHost.onConnect` that discards `connectionId`
- Localized strings (es + en) for the four nav items
- Add Phosphor icons for the new destinations
- Unit tests for `NavigationContext`, `destinationsForContext`, and route generation
- UI test asserting the 4-item bar renders after Connect

### Out of Scope
- Full `Monitor` implementation — only UI shell with three tabs (server metrics, query log, health check) showing placeholders
- Full `NewQuery` editor — placeholder screen only; final implementation deferred
- Backend `CREATE DATABASE` execution — only the form UI (name, charset, collation) is in scope; SQL execution and driver integration deferred
- Migration of the `MySQLConnectionPool.activeConnection` singleton (separate cleanup change)
- Charts, slow query log integration, or replication monitoring
- Deprecated-alias retention for `database_list` (route is replaced, not aliased)

## Capabilities

### New Capabilities
- `database-list-navigation`: contextual bottom navigation that appears on the database list screen with four actions (Add database, New query, Monitor, Settings), driven by the `connection/{id}/databases` route
- `add-database-form`: form UI to create a new database/schema on the connected server (name, charset, collation inputs; no SQL execution in this change)
- `server-monitor-shell`: placeholder Monitor screen with three tabs (server metrics, query log, health check) ready for future implementation
- `query-editor-placeholder`: placeholder New Query screen wired into navigation, replaced in a follow-up change

### Modified Capabilities
- None. No existing specs in `openspec/specs/` are affected; this is the first feature being formalized under SDD.

## Approach

Implement **Option B** from the exploration:

1. Replace `Routes.DatabaseList` (`"database_list"`) with `Routes.Databases` (`"connection/{connectionId}/databases"`) plus `createRoute(id)`. Add `Routes.AddDatabase`, `Routes.NewQuery`, `Routes.Monitor`, all contextual on `connectionId`.
2. Keep `NavigationContext` as `OutsideConnection | InsideConnection(id)`. Change `destinationsForContext` signature to `(context, currentRoute) -> List<NavigationDestination>` and branch on `currentRoute?.endsWith("/databases")` to return the new 4-item server menu.
3. Fix `MyDataBasesNavHost.onConnect` to navigate to `Routes.Databases.createRoute(connectionId)`. Add new `composable` entries for the three new routes with `navArgument("connectionId")`. Remove the legacy `database_list` block.
4. Pass `currentRoute` from `AdaptiveNavigationScaffold` into `destinationsForContext`. No special-cases in the scaffold itself — the routing rule stays intact.
5. `DatabasesListScreen` accepts `connectionId: String`; `DatabasesListViewModel` reads it from `SavedStateHandle` instead of `MySQLConnectionPool.activeConnection`.
6. New screens are minimal: `AddDatabaseScreen` is a real form; `NewQueryScreen` and `MonitorScreen` are placeholders (Monitor has a `TabRow` with three empty tabs).

This preserves the architectural invariant "route → context → items, pure derivation" and fixes the latent bug where `connectionId` was discarded.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/navigation/Routes.kt` | Modified | Replace `DatabaseList` with `Databases`; add `AddDatabase`, `NewQuery`, `Monitor` |
| `ui/navigation/NavigationContext.kt` | Unchanged | Sealed class stays the same (Option B) |
| `ui/navigation/NavigationDestinations.kt` | Modified | Signature gains `currentRoute`; new 4-item branch under `InsideConnection` |
| `ui/navigation/MyDataBasesNavHost.kt` | Modified | Fix `onConnect` TODO; new composable entries; remove legacy `database_list` block |
| `ui/adaptive/AdaptiveNavigationScaffold.kt` | Modified | Pass `currentRoute` to `destinationsForContext`; verify `showMenu` filter passes `/databases` |
| `ui/screens/databases/DatabasesListScreen.kt` | Modified | Accept `connectionId: String` parameter |
| `ui/screens/databases/DatabasesListViewModel.kt` | Modified | Read `connectionId` from `SavedStateHandle` |
| `ui/screens/databases/AddDatabaseScreen.kt` | New | Form: name + charset + collation (no SQL execution) |
| `ui/screens/databases/NewQueryScreen.kt` | New | Placeholder screen |
| `ui/screens/databases/MonitorScreen.kt` | New | Placeholder with 3 tabs (metrics, queries, health) |
| `ui/components/PhosphorAppIcons.kt` | Modified | Add `Nav.addDatabase`, `Nav.newQuery`, `Nav.monitor` |
| `res/values/strings.xml` + `values-es/strings.xml` | Modified | `nav_add_database`, `nav_new_query`, `nav_monitor` (es + en) |
| `app/src/test/.../NavigationContextTest.kt` | New/Modified | Cases for `connection/{id}/databases` |
| `app/src/test/.../NavigationDestinationsTest.kt` | New | Sub-context branching tests |
| `app/src/androidTest/.../BottomNavTest.kt` | New | UI test: Connect → 4-item bar visible |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Legacy deep links / saved state referencing `database_list` break | Low | Grep entire codebase before merge; no external deep-link entry points exist (verified in exploration) |
| `DatabasesListViewModel` retains coupling to `MySQLConnectionPool.activeConnection` singleton | Medium | Refactor `loadDatabases()` to consume the navArg `connectionId`; document the singleton-cleanup follow-up |
| `showMenu` filter regression hides the new `/databases` bar | Low | Add UI test in all three WindowSizeClasses (Compact/Medium/Expanded) |
| `WorkspaceOverlay` (PlayStation pager) overlaps new bar | Low | Already verified in exploration: same z-order as current 5-item bar |
| Tabler icon set missing `Activity`/`Pulse` for Monitor | Low | Fall back to `TablerIcons.Heartbeat` or `ChartLine` |
| Spanish translations for "Monitor" / "Nuevo query" feel forced | Low | Use neutral professional Spanish: "Monitor", "Nueva consulta" |

## Rollback Plan

The change is contained to navigation wiring + three new screen files. To roll back:

1. Revert the commit(s) for this change — no schema, persistence, or driver state is mutated.
2. The legacy `Routes.DatabaseList = "database_list"` returns; `onConnect` reverts to the TODO state.
3. No data migration is needed — `CREATE DATABASE` is NOT executed in this change (form UI only), so no server-side rollback is required.
4. New string resources and icon references are removed automatically with the revert.

If only the contextual route causes problems but the new screens are good: keep the new screens and re-add `database_list` as a deprecated alias `composable` that internally navigates to `Routes.Databases.createRoute(currentConnectionId)`.

## Dependencies

- Existing `Routes.Settings` and `Routes.QueryEditor` (already contextual on `connectionId`) — no changes required to those targets.
- Phosphor / Tabler icon set already vendored under `PhosphorAppIcons.kt`.
- Hilt-injected `DatabasesListViewModel` already supports `SavedStateHandle` (standard `@HiltViewModel`).
- No new third-party libraries.

## Success Criteria

- [ ] Bottom navigation bar renders 4 items (Add database, New query, Monitor, Settings) when the active route ends in `/databases`
- [ ] Tapping "Connect" on a connection card navigates to `connection/{id}/databases` and the bar appears
- [ ] Each of the four bar items navigates to its target route without losing `connectionId`
- [ ] The 5-item DB-level bar still renders correctly on `connection/{id}/tables`, `/views`, `/editor`, `/functions`, `/backup`
- [ ] `OutsideConnection` bar (Connections + Settings) still renders on `connections` and is still hidden where it was hidden before
- [ ] `DatabasesListViewModel` reads `connectionId` from `SavedStateHandle` and `loadDatabases()` no longer depends on `MySQLConnectionPool.activeConnection`
- [ ] Localized labels render correctly in both `es` and `en`
- [ ] Unit tests pass: `NavigationContextTest`, `NavigationDestinationsTest`
- [ ] UI test passes: Connect → 4-item bar visible in Compact, Medium, Expanded WindowSizeClass
- [ ] `./gradlew test` and `./gradlew assembleDebug` both green
- [ ] No reference to `"database_list"` remains in non-test code
