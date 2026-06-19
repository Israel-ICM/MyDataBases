# Exploration: Database List Bottom Navigation

> **Change**: `database-list-bottom-nav`
> **Scope**: Add a contextual bottom navigation to the database list screen with 4 actions: Add database, New query, Monitor, Settings.
> **Date**: 2026-06-19

---

## Executive Summary

The app already implements a **clean contextual navigation system** — `NavigationContext.from(route)` parses the active route and decides which items the `AdaptiveNavigationScaffold` renders (BottomBar / Rail / Drawer). Today there are exactly **two contexts**: `OutsideConnection` (2 items) and `InsideConnection` (5 items).

The `database_list` screen falls in a **dead zone**:

1. It is reached via `Routes.DatabaseList = "database_list"` — a **flat legacy route** with no `connectionId` segment.
2. `NavigationContext.from("database_list")` does NOT match the `^connection/([^/]+)/.+` regex, so it returns `OutsideConnection`.
3. Even if it returned `OutsideConnection`, the BottomBar is **explicitly hidden** for `Routes.Connections` and `Routes.Settings` by the scaffold's `showMenu` filter, and `database_list` is currently shown WITHOUT a menu only because no context-appropriate items exist (the bar would render the `OutsideConnection` items, which is the wrong context for "inside a server").

There is a **TODO already in the codebase** that anticipates this exact problem:

```kotlin
// MyDataBasesNavHost.kt:92-94
onConnect = { connectionId ->
    // TODO: navegar a la nueva ruta contextual connection/{id}/tables
    // Por ahora usa la ruta legacy database_list
    navController.navigate(Routes.DatabaseList.route)   // <-- discards connectionId
}
```

The `connectionId` is **available at this point** but thrown away. That's the root cause.

The cleanest fix is to **promote `database_list` to a contextual route** (`connection/{id}/databases`) and introduce a **new `NavigationContext` variant** for the "inside a server, no DB selected yet" state. This keeps the contextual-derivation invariant intact and avoids special cases in the scaffold.

---

## Current State

### How navigation actually flows today

```
ConnectionsListScreen
    └─ user taps "Connect" on a card
        └─ onConnect(connectionId)
            └─ navController.navigate("database_list")   <-- connectionId LOST
                └─ DatabasesListScreen
                    └─ NavigationContext.from("database_list") == OutsideConnection
                        └─ scaffold shows: nothing (route matches showMenu filter? No, but the items shown would be Connections/Settings — semantically wrong)
```

### Why no BottomBar appears on `database_list`

Looking at `AdaptiveNavigationScaffold.kt:110-114`:

```kotlin
val showMenu = currentRoute != null &&
    currentRoute != Routes.Connections.route &&
    currentRoute != Routes.Settings.route &&
    !currentRoute.startsWith("connection_form")
```

`"database_list"` is NOT in the exclusion list, so `showMenu == true`. The bar SHOULD show — but it would show the **OutsideConnection items** (Connections, Settings), because `NavigationContext.from("database_list") == OutsideConnection`. That is semantically wrong for the screen, which represents "I am inside a server, browsing its DBs".

So the visible symptom is: the BottomBar renders the wrong items on `database_list`, OR (depending on prior testing) it appears empty / unhelpful. Either way, the user has no way to add a DB, open a query, see a monitor, or jump to settings without going back.

### Files involved in the current contextual system

| File | Role |
|------|------|
| `ui/navigation/Routes.kt` | Single source of routes. `DatabaseList` is a flat string; `Tables`, `Views`, `QueryEditor`, `Functions`, `Backup` use `connection/{connectionId}/...`. |
| `ui/navigation/NavigationContext.kt` | Sealed class `OutsideConnection | InsideConnection(id)`. Derived from route via regex `^connection/([^/]+)/.+`. |
| `ui/navigation/NavigationDestinations.kt` | `destinationsForContext(ctx)` returns 2 or 5 `NavigationDestination`s. |
| `ui/adaptive/AdaptiveNavigationScaffold.kt` | Renders the items. Decides BottomBar/Rail/Drawer by `WindowSizeClass`. Hides bar for Connections / Settings / ConnectionForm. |
| `ui/navigation/MyDataBasesNavHost.kt` | NavGraph + the TODO that throws away `connectionId`. |
| `ui/screens/databases/DatabasesListScreen.kt` | Pure presentation; does not know about `connectionId` (loads from injected `ViewModel`). |
| `ui/workspace/WorkspaceOverlay.kt` | Wraps the whole scaffold. The PlayStation-style workspace pager is on top of everything — adding a new context does NOT touch it. |

---

## Affected Areas

| Path | Why it's affected |
|------|-------------------|
| `ui/navigation/Routes.kt` | Add `Routes.Databases("connection/{connectionId}/databases")` (contextual) and optionally a `Routes.Monitor("connection/{connectionId}/monitor")` if "Monitor" becomes a real screen. The legacy `Routes.DatabaseList` can stay for one release as a deprecated alias or be removed. |
| `ui/navigation/NavigationContext.kt` | Either (a) add a third variant `InsideServer(connectionId)` distinct from `InsideConnection`, OR (b) keep one variant and **vary the destinations by route segment** (`/databases` vs `/tables`). Recommended: option (b) — simpler, see "Recommendation". |
| `ui/navigation/NavigationDestinations.kt` | New 4-item list for the "DatabaseList" context: Add DB, New Query, Monitor, Settings. |
| `ui/navigation/MyDataBasesNavHost.kt` | `onConnect` must navigate to `Routes.Databases.createRoute(connectionId)`. New `composable` entry for that route. The existing `database_list` `composable` either gets removed or becomes a thin redirect. |
| `ui/screens/databases/DatabasesListScreen.kt` | Must accept `connectionId: String` as a navArg-derived parameter (so it knows which server it's listing). The ViewModel may already use it via session / `MySQLConnectionPool.activeConnection`; need to confirm. |
| `ui/screens/databases/DatabasesListViewModel.kt` | Possibly read `connectionId` from `SavedStateHandle` instead of relying on a singleton's mutable state — cleaner and survives process death. |
| `ui/adaptive/AdaptiveNavigationScaffold.kt` | NO changes if we choose Option B (recommended). With Option C, would need a `when` over route — that's the special-case we want to avoid. |
| `res/values/strings.xml` + `res/values-es/strings.xml` | Add `nav_add_database`, `nav_new_query`, `nav_monitor` (keep existing `nav_settings`). |
| `ui/components/PhosphorAppIcons.kt` | Add icons for Add DB (`Plus` or `DatabasePlus`), New Query (`Code`/`Terminal`), Monitor (`Activity`/`ChartLine`/`Pulse`). Tabler has all three. |
| `ui/screens/connections/ConnectionsListScreen.kt` | NO direct changes — it already passes `connectionId` upward; the NavHost is the one wiring it wrong. |

---

## Approaches

### Option A — New `NavigationContext` variant + new contextual route

**Idea**: Introduce `NavigationContext.InsideServer(connectionId)` as a third variant. Define a new route `connection/{id}/databases`. Update the regex so `connection/{id}/databases` maps to `InsideServer`, while `connection/{id}/tables|views|editor|...` maps to `InsideConnection`. Add a new branch in `destinationsForContext` returning the 4 new items.

- **Pros**:
  - Explicit, type-safe context. Easy to read — every place that does `when (context) { ... }` is forced to handle the new state.
  - Clear semantic: "inside a server, no DB selected yet" vs "inside a connection, working a DB".
  - Future-proof — easy to add more "server-level" actions later.
- **Cons**:
  - More moving parts. Three contexts means three branches everywhere (`destinationsForContext` plus any future consumer).
  - Distinguishing `InsideServer` vs `InsideConnection` from a single regex requires segmenting on the path suffix (`/databases` → InsideServer; anything else → InsideConnection). That's brittle if route names change.
  - Conceptually overlapping: both "inside server" and "inside connection" share the same `connectionId`. Two names for almost-the-same state will cause confusion.
- **Effort**: **Medium**.

### Option B — Keep one `InsideConnection` context, branch destinations on route — RECOMMENDED

**Idea**: Keep `NavigationContext.InsideConnection(connectionId)` as the single "inside" state (matching the current regex). Promote `database_list` to `Routes.Databases = "connection/{id}/databases"` so it ALSO becomes `InsideConnection`. Change `destinationsForContext` signature to also receive the `currentRoute` (or a small `subContext` derived from it) and return the right item set:

```kotlin
fun destinationsForContext(
    context: NavigationContext,
    currentRoute: String?,
): List<NavigationDestination> = when (context) {
    is OutsideConnection -> listOf(Connections, Settings)
    is InsideConnection -> when {
        currentRoute?.endsWith("/databases") == true ->
            listOf(AddDatabase, NewQuery, Monitor, Settings)   // 4-item server view
        else ->
            listOf(Tables, Views, Editor, Functions, Backup)   // 5-item DB view
    }
}
```

- **Pros**:
  - Zero new types. The contextual derivation invariant ("context comes from the route") is preserved.
  - The change surface is small: Routes + Destinations + NavHost + strings + icons.
  - Easy to extend with a third sub-view later (just another `when` branch).
  - The scaffold remains agnostic — no special cases.
- **Cons**:
  - `destinationsForContext` now depends on two inputs (context + route). Slightly less pure.
  - The "sub-context" branching lives inline; if it grows, it should be extracted to a small `InsideConnectionView` enum.
- **Effort**: **Low**.

### Option C — Special-case `database_list` in the scaffold

**Idea**: Leave the legacy `database_list` route untouched. Inside `AdaptiveNavigationScaffold`, detect `currentRoute == "database_list"` and short-circuit `destinationsForContext` to return a hardcoded 4-item list.

- **Pros**:
  - Smallest diff today — touches only the scaffold and the destinations file.
  - No navigation refactor needed.
- **Cons**:
  - **Breaks the architectural invariant** ("context derives from route, scaffold is dumb"). Once you special-case one route, every future special-case is justified by precedent. This is exactly the kind of decision a senior architect will reject in review.
  - The scaffold becomes a god-component that knows about specific routes.
  - The `connectionId` is still lost — `database_list` has no `{id}` — so "New query" inside this context can't build the contextual editor route. We'd have to read it from the singleton `MySQLConnectionPool.activeConnection`, which couples nav to a mutable runtime state and breaks process-death recovery.
  - Same legacy debt remains.
- **Effort**: **Low** (today) — **High** (long-term cost of architectural erosion).

---

## Recommendation

**Option B**. Concrete plan:

1. **Rename the route**: `Routes.DatabaseList = "database_list"` → `Routes.Databases = "connection/{connectionId}/databases"` with `createRoute(id)`.
2. **Fix the TODO** in `MyDataBasesNavHost.onConnect`: navigate to `Routes.Databases.createRoute(connectionId)`. Remove the legacy `database_list` `composable`.
3. **Pass connectionId to the screen**: `DatabasesListScreen` accepts a `connectionId: String` (from `navArgument`), and `DatabasesListViewModel` reads it from `SavedStateHandle`. This finally breaks the implicit coupling to `MySQLConnectionPool.activeConnection`.
4. **Branch `destinationsForContext` on route suffix**: `/databases` → 4-item server menu; otherwise → 5-item DB menu. Both branches stay under `InsideConnection`.
5. **Add the 4 new `NavigationDestination`s** with new icons and new string resources (es + en).
6. **Wire the actions**:
   - **Add database** → opens a `connection/{id}/databases/new` route OR a bottom sheet (TBD with user — see Open Questions). Recommend bottom sheet for v1 because it keeps the user in the list.
   - **New query** → navigates to `Routes.QueryEditor.createRoute(connectionId)`. Already exists.
   - **Monitor** → new route `connection/{id}/monitor`, placeholder screen for v1. Needs scope clarification.
   - **Settings** → navigates to `Routes.Settings.route`. Already exists.

**Why this is the right call**: it preserves the architectural rule that drives the whole adaptive scaffold (route → context → items, pure derivation). It also forces us to fix the latent bug where `connectionId` was being thrown away. The cost is one new sub-context branch in a single function — proportional to what we're adding.

---

## Impact Analysis

### Files to modify

| File | Change |
|------|--------|
| `Routes.kt` | Replace `DatabaseList` with `Databases("connection/{connectionId}/databases")` + new `Monitor`. |
| `NavigationDestinations.kt` | New 4-item branch for the server sub-context. Signature gains `currentRoute: String?`. |
| `NavigationContext.kt` | Untouched (Option B keeps `OutsideConnection | InsideConnection`). |
| `MyDataBasesNavHost.kt` | Fix `onConnect` to pass `connectionId`. New `composable` for `Routes.Databases.route` with navArg. Add `composable` for `Monitor`. Remove legacy `database_list` block. Pass `currentRoute` into the scaffold's destination lookup. |
| `AdaptiveNavigationScaffold.kt` | Pass `currentRoute` to `destinationsForContext`. Check `showMenu` filter doesn't accidentally hide `/databases`. |
| `DatabasesListScreen.kt` | Add `connectionId: String` parameter. |
| `DatabasesListViewModel.kt` | Read `connectionId` from `SavedStateHandle`; remove implicit dependency on `MySQLConnectionPool.activeConnection`. |
| `PhosphorAppIcons.kt` | Add `Nav.databases`, `Nav.addDatabase`, `Nav.newQuery`, `Nav.monitor`. |
| `res/values/strings.xml`, `values-es/strings.xml` | `nav_add_database` / `nav_new_query` / `nav_monitor`. |
| `MySQLConnectionPool.kt` | Possibly stop relying on `activeConnection` mutable singleton; pool should be queried by id. Out of scope here but a follow-up. |

### Tests

- Unit: `NavigationContextTest` already exists pattern-wise — add cases for `connection/{id}/databases` → `InsideConnection(id)`. Add `destinationsForContextTest` covering the new sub-context branch (`endsWith("/databases")`).
- UI: a small Compose UI test that navigates Connections → tap Connect → asserts `BottomBar` shows the 4 new items.

### Risks

1. **Legacy route consumers**: any deep-link or test referencing `database_list` will break. Mitigation: grep before merging, keep the route as a deprecated alias for one release if necessary.
2. **`DatabasesListViewModel` coupling to `activeConnection`**: today the screen "magically" loads the current connection because of the singleton. Once we pass `connectionId` via navArg, the singleton may still hold a stale value. Mitigation: refactor `loadDatabases()` to accept/use the navArg id, or look up the pool by id.
3. **Scaffold filter regression**: the `showMenu` filter hides the bar for `Connections`/`Settings`/`connection_form`. The new `/databases` route is not in the filter, so the bar WILL show — verify in all three WindowSizeClasses.
4. **PlayStation workspace overlay**: `WorkspaceOverlay` wraps the scaffold. Its `Peek`/`Expanded` modes float OVER the BottomBar. Verify the new 4-item bar doesn't conflict (it shouldn't — same z-order as today's 5-item bar).
5. **Icon namespace**: confirm Tabler has `Activity`/`Pulse`/`ChartLine` for "Monitor"; if not, fall back to `TablerIcons.Heartbeat` or similar.
6. **i18n**: "Monitor" and "New query" need carefully translated Spanish labels (see Open Questions).

### Non-risks (verified)

- `WorkspaceOverlay` does NOT inspect routes; it operates on its own state. Safe.
- `NavigationContext` is exercised by tests located under `app/src/test` (per project setup). No prod consumer outside the scaffold + destinations file.

---

## Open Questions

The orchestrator MUST surface these to the user before `sdd-spec` runs:

1. **"Agregar database"** — what exactly?
   a. Create a **new schema/database** on the connected server (e.g. `CREATE DATABASE foo`)?
   b. Add a **new connection** (i.e. open the connection form)?
   c. Import a database file (SQLite/dump)?

   The wording suggests (a). If so: this needs server-side support per driver — does `MySQLConnectionPool` already expose a "create database" call, or is this a new domain capability? **Recommend**: scope v1 to MySQL only; show an error toast for unsupported drivers.

2. **"Monitor"** — what does it monitor?
   a. **Server health**: connections count, uptime, version, replication status?
   b. **Query performance**: slow query log, active queries, locks?
   c. **Storage**: DB sizes, table sizes, growth?
   d. All of the above on a single dashboard?

   This is the biggest unknown. **Recommend**: v1 as a placeholder screen with a "coming soon" + a list of 3-4 server-level metrics (uptime, version, total DBs, active connections) that can be fetched cheaply with `SHOW STATUS` / `SHOW VARIABLES`. Defer charts.

3. **"Nuevo query" target**: should it open the existing `QueryEditor` (`connection/{id}/editor`) with an empty buffer, or open a NEW WorkspaceCard (PlayStation pager) with an empty query? Both are valid; the second integrates better with the multi-tab IDE feel.

4. **Legacy `database_list` route**: do we keep it as a deprecated alias for one release (in case there are external entry points / saved instance state), or remove it cleanly?

---

## Ready for Proposal

**Yes**, with the four Open Questions answered. The architectural choice (Option B) is unambiguous. The product scope of "Monitor" and "Agregar database" is the only remaining gate before `sdd-propose`.

Suggested orchestrator message to the user:

> Listo para proponer el cambio. Antes de avanzar, necesito que confirmes 4 cosas:
> (1) "Agregar database" = crear un schema nuevo en el servidor conectado, ¿correcto?
> (2) "Monitor" v1 = pantalla básica con uptime / versión / # de DBs / conexiones activas — ¿OK como MVP?
> (3) "Nuevo query" abre el editor existente o crea una WorkspaceCard nueva?
> (4) ¿Mantenemos `database_list` como alias deprecado o lo eliminamos directo?
