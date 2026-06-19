# Tasks: Database List Bottom Navigation

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350-400 (13 files modified/created, 3 screens, 4 routes, tests) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No (prefer single atomic commit per proposal) |
| Suggested split | Single PR (all navigation wiring atomic) |
| Delivery strategy | Single PR with maintainer review |
| Chain strategy | N/A (single PR) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: single-pr
400-line budget risk: Medium

### Rationale for Single PR

This change is architecturally atomic: promoting a legacy route to contextual navigation requires all pieces (routes, destinations, screens, ViewModel wiring) to land together. Splitting would leave the codebase in a broken state where half the nav items point to non-existent routes. The ~350-400 line estimate is at the budget edge but acceptable given the atomic nature and placeholder screen simplicity.

## Phase 1: Foundation (Routes, Strings, Icons)

- [x] 1.1 Replace `Routes.DatabaseList` with `Routes.Databases` in `ui/navigation/Routes.kt` — add `data object Databases : Routes("connection/{connectionId}/databases")` with `createRoute(connectionId)` function
- [x] 1.2 Add three new route objects in `ui/navigation/Routes.kt` — `AddDatabase`, `NewQuery`, `Monitor`, all with `connection/{connectionId}/...` pattern and `createRoute()` functions
- [x] 1.3 Add localized strings to `res/values/strings.xml` — `nav_add_database`, `nav_new_query`, `nav_monitor` (en: "Add Database", "New Query", "Monitor")
- [x] 1.4 Add localized strings to `res/values-es/strings.xml` — same keys (es: "Nueva base de datos", "Nueva consulta", "Monitor")
- [x] 1.5 Add Phosphor icons to `ui/components/PhosphorAppIcons.kt` — `Nav.addDatabase` (Plus), `Nav.newQuery` (FileText), `Nav.monitor` (Pulse/Activity)
- [x] 1.6 Add placeholder strings for Add Database form to `strings.xml` + `strings-es.xml` — field labels, validation errors, submit button, "coming soon" toast

**Estimated lines**: ~60 (routes: 20, strings: 30, icons: 10)

**Dependencies**: None — this is the foundation layer

**Risk**: Low — pure data definition, no logic

## Phase 2: Navigation Context Updates

- [x] 2.1 Modify `destinationsForContext` signature in `ui/navigation/NavigationDestinations.kt` — add `currentRoute: String? = null` parameter
- [x] 2.2 Add route suffix branching logic in `NavigationDestinations.kt` — when `context is InsideConnection` and `currentRoute?.endsWith("/databases") == true`, return new 4-item list instead of existing 5-item DB menu
- [x] 2.3 Create `NavigationDestination` entries for the 4 new items — `AddDatabase`, `NewQuery`, `Monitor` (reuse existing `Settings`) with correct icons, labels, route generators
- [x] 2.4 Update `AdaptiveNavigationScaffold.kt` — pass `currentRoute` from NavController current backstack entry to `destinationsForContext` call
- [x] 2.5 Verify `showMenu` filter in `AdaptiveNavigationScaffold.kt` — confirm `connection/{id}/databases` passes the existing filter logic

**Estimated lines**: ~50 (signature change: 5, branching logic: 25, destination entries: 15, scaffold: 5)

**Dependencies**: Phase 1 (routes and strings must exist)

**Risk**: Medium — core navigation logic; must preserve existing 5-item and 2-item bars

## Phase 3: NavHost Wiring

- [x] 3.1 Fix TODO in `MyDataBasesNavHost.kt` at `onConnect` callback (line 92-94) — replace with `navController.navigate(Routes.Databases.createRoute(connectionId))`
- [x] 3.2 Add `composable` entry for `Routes.Databases` in `MyDataBasesNavHost.kt` — with `navArgument("connectionId")`, call `DatabasesListScreen(connectionId = it.arguments?.getString("connectionId")!!)`
- [x] 3.3 Add `composable` entry for `Routes.AddDatabase` — with `navArgument("connectionId")`, call `AddDatabaseScreen(connectionId = ...)`
- [x] 3.4 Add `composable` entry for `Routes.NewQuery` — with `navArgument("connectionId")`, call `NewQueryScreen(connectionId = ...)`
- [x] 3.5 Add `composable` entry for `Routes.Monitor` — with `navArgument("connectionId")`, call `MonitorScreen(connectionId = ...)`
- [x] 3.6 Remove legacy `database_list` composable block (line 116-122 in current NavHost)

**Estimated lines**: ~40 (onConnect fix: 2, 4 composable entries: 30, removal: -8)

**Dependencies**: Phase 1 (routes), Phase 3 screens must exist to reference

**Risk**: High — route removal could break deep links; must grep for `"database_list"` before merge

## Phase 4: DatabasesListScreen and ViewModel Refactor

- [x] 4.1 Add `connectionId: String` parameter to `DatabasesListScreen` composable signature in `ui/screens/databases/DatabasesListScreen.kt`
- [x] 4.2 Inject `SavedStateHandle` into `DatabasesListViewModel` constructor in `ui/screens/databases/DatabasesListViewModel.kt`
- [x] 4.3 Read `connectionId` from `SavedStateHandle` in `DatabasesListViewModel.init` — `private val connectionId: String = savedStateHandle["connectionId"] ?: throw IllegalStateException(...)`
- [x] 4.4 Refactor `loadDatabases()` in `DatabasesListViewModel` — pass `connectionId` to `GetDatabasesUseCase` instead of relying on `MySQLConnectionPool.activeConnection` singleton
- [x] 4.5 Add error state handling in `DatabasesListViewModel` — if `connectionId` is missing from SavedStateHandle, emit `UiState.Error` with localized message

**Estimated lines**: ~25 (screen signature: 2, ViewModel: 20, error handling: 3)

**Dependencies**: Phase 1 (routes)

**Risk**: Medium — ViewModel refactor could break existing database loading if `GetDatabasesUseCase` doesn't already support connectionId parameter

## Phase 5: AddDatabaseScreen Implementation

- [x] 5.1 Create `AddDatabaseScreen.kt` in `ui/screens/databases/` — composable with `connectionId: String` parameter
- [x] 5.2 Add three input fields — name (required, `TextField`), charset (optional, `TextField`), collation (optional, `TextField`) with localized labels
- [x] 5.3 Implement inline validation for name field — regex `^[A-Za-z0-9_]{1,64}$`, show error on invalid input
- [x] 5.4 Implement optional validation for charset and collation — same regex as name, only validate if non-empty
- [x] 5.5 Add Create button — enabled only when name is valid and charset/collation (if present) are valid
- [x] 5.6 Wire Create button to ViewModel `onSubmit(name, charset?, collation?)` — show "Coming soon" toast/snackbar (no SQL execution)
- [x] 5.7 Add Cancel/Back affordance — system back navigates to `/databases`

**Estimated lines**: ~80 (screen structure: 20, fields + validation: 40, button logic: 15, back handling: 5)

**Dependencies**: Phase 1 (strings, routes)

**Risk**: Low — form UI only, no backend execution

## Phase 6: MonitorScreen Placeholder

- [x] 6.1 Create `MonitorScreen.kt` in `ui/screens/databases/` — composable with `connectionId: String` parameter
- [x] 6.2 Add `TabRow` with three tabs — "Metrics" / "Métricas", "Queries" / "Consultas", "Health" / "Estado" (localized)
- [x] 6.3 Add tab selection state — default to Metrics (index 0), switch on tap
- [x] 6.4 Add placeholder content for each tab — centered icon + localized title + "Coming soon" / "Próximamente" message
- [x] 6.5 Wire `SavedStateHandle["connectionId"]` for future use (no consumer in this change)

**Estimated lines**: ~60 (screen structure: 15, TabRow: 20, 3 placeholders: 20, connectionId: 5)

**Dependencies**: Phase 1 (strings)

**Risk**: Low — placeholder only, no data fetching

## Phase 7: NewQueryScreen Placeholder

- [x] 7.1 Create `NewQueryScreen.kt` in `ui/screens/databases/` — composable with `connectionId: String` parameter
- [x] 7.2 Add centered placeholder body — icon (code/query icon), title "Query editor" / "Editor de consultas", message "Coming soon" / "Próximamente"
- [x] 7.3 Wire `SavedStateHandle["connectionId"]` for future use (no consumer in this change)
- [x] 7.4 Verify system back navigates to `/databases`

**Estimated lines**: ~30 (screen structure: 10, placeholder: 15, connectionId: 5)

**Dependencies**: Phase 1 (strings)

**Risk**: Low — simplest placeholder

## Phase 8: Unit Tests

- [ ] 8.1 Add test case to `NavigationContextTest.kt` — verify `from("connection/abc-123/databases")` returns `InsideConnection("abc-123")`
- [ ] 8.2 Create `NavigationDestinationsTest.kt` — verify `destinationsForContext(InsideConnection("x"), "connection/x/databases")` returns 4-item list with correct destinations
- [ ] 8.3 Add test case to `NavigationDestinationsTest.kt` — verify `destinationsForContext(InsideConnection("x"), "connection/x/tables")` still returns existing 5-item DB menu
- [ ] 8.4 Add test case to `NavigationDestinationsTest.kt` — verify `destinationsForContext(OutsideConnection, "connections")` still returns 2-item menu unchanged
- [ ] 8.5 Create `DatabasesListViewModelTest.kt` — mock `SavedStateHandle` with `connectionId`, verify `loadDatabases()` passes connectionId to use case
- [ ] 8.6 Add test to `DatabasesListViewModelTest.kt` — verify missing `connectionId` in SavedStateHandle emits `UiState.Error`

**Estimated lines**: ~70 (NavigationContext: 10, NavigationDestinations: 30, ViewModel: 30)

**Dependencies**: Phases 2, 4 (navigation logic and ViewModel must be implemented)

**Risk**: Low — standard unit testing

## Phase 9: UI Tests

- [ ] 9.1 Create `BottomNavTest.kt` in `androidTest/` — UI test using Espresso or Compose Testing
- [ ] 9.2 Test: Connect action navigates to `/databases` and 4-item bar renders — verify bottom navigation has 4 items with correct labels
- [ ] 9.3 Test: Tap "Add database" item navigates to `/add-database` route — verify route change via NavController test observer
- [ ] 9.4 Test: Tap "New query" item navigates to `/new-query` route
- [ ] 9.5 Test: Tap "Monitor" item navigates to `/monitor` route
- [ ] 9.6 Test: Tap "Settings" item navigates to existing Settings route scoped to connectionId
- [ ] 9.7 Parameterized test: Verify 4-item bar renders correctly in Compact, Medium, Expanded WindowSizeClass

**Estimated lines**: ~80 (test setup: 20, 6 navigation tests: 50, WindowSizeClass test: 10)

**Dependencies**: Phases 3, 5, 6, 7 (screens and NavHost wiring must exist)

**Risk**: Medium — UI tests can be flaky; need stable test environment

## Phase 10: Cleanup and Verification

- [ ] 10.1 Grep entire codebase for `"database_list"` string literal — verify no references remain outside test fixtures
- [ ] 10.2 Run `./gradlew test` — verify all unit tests pass
- [ ] 10.3 Run `./gradlew assembleDebug` — verify clean build
- [ ] 10.4 Run `./gradlew connectedAndroidTest` or manual UI test — verify 4-item bar renders on real device
- [ ] 10.5 Manual smoke test: Connect to server → verify 4 items appear → tap each item → verify correct screen renders
- [ ] 10.6 Manual smoke test: Navigate to `/tables` route → verify existing 5-item DB menu still renders
- [ ] 10.7 Update any affected documentation or ADR if needed (likely none for this change)

**Estimated lines**: 0 (verification only, no code changes)

**Dependencies**: All previous phases

**Risk**: Low — final validation step

---

## Task Summary

| Phase | Tasks | Focus | Est. Lines |
|-------|-------|-------|------------|
| Phase 1 | 6 | Routes, Strings, Icons | ~60 |
| Phase 2 | 5 | Navigation Context Updates | ~50 |
| Phase 3 | 6 | NavHost Wiring | ~40 |
| Phase 4 | 5 | DatabasesListScreen/ViewModel Refactor | ~25 |
| Phase 5 | 7 | AddDatabaseScreen Implementation | ~80 |
| Phase 6 | 5 | MonitorScreen Placeholder | ~60 |
| Phase 7 | 4 | NewQueryScreen Placeholder | ~30 |
| Phase 8 | 6 | Unit Tests | ~70 |
| Phase 9 | 7 | UI Tests | ~80 |
| Phase 10 | 7 | Cleanup and Verification | 0 |
| **Total** | **58** | | **~495** |

**Note**: Estimated ~495 lines is higher than the design doc's 350-400 estimate due to comprehensive test coverage. If splitting is needed, the atomic boundary is: **Foundation + Navigation + Wiring (Phases 1-3) → Screens + Tests (Phases 4-9)**. However, this violates the architectural invariant that routes must have screens, so single PR is still recommended with maintainer `size:exception` approval if needed.

## Implementation Order Rationale

1. **Phase 1 (Foundation)** must come first — routes, strings, and icons are dependencies for all downstream work
2. **Phase 2 (Navigation)** depends on routes existing and establishes the branching logic
3. **Phase 3 (NavHost)** wires routes to screens but can't reference screens until they exist
4. **Phases 4-7 run in parallel** — each screen is independent once routes exist
5. **Phase 8-9 (Tests)** validate implementation after core logic is done
6. **Phase 10 (Cleanup)** is the final gate before merge

## High-Risk Tasks Requiring Extra Review

- **Task 3.6** (Remove legacy `database_list` route): Grep verification MANDATORY before merge
- **Task 4.4** (ViewModel refactor): Ensure `GetDatabasesUseCase` supports `connectionId` parameter; if not, add it
- **Task 2.2** (Route suffix branching): Must preserve existing 5-item and 2-item bars; regression test critical

## Next Step

Ready for implementation via `sdd-apply`. Maintainer review recommended for the single-PR strategy given ~495 estimated lines (above 400 budget). Consider `size:exception` label or split at Phase 3/4 boundary if team policy requires strict 400-line limit.
