# New Query Modal Fix

**Date**: 2026-06-30  
**Type**: Bug Fix  
**Priority**: High  
**Status**: ✅ Completed  
**Commit**: `bfe1658`

---

## Problem Statement

When pressing the "New Query" button in the bottom navigation menu from the databases list screen, the application exhibited undesired behavior:

1. **Window went blank** - The databases list disappeared
2. **Menu changed** - The bottom navigation switched to a different context menu
3. **Double sheet issue** - An empty bottom sheet appeared behind the actual query editor top sheet

This was caused by navigation triggering a route change, which updated the `NavigationContext` and caused a complete UI reconfiguration.

---

## Root Cause

The "New Query" navigation destination was configured to **navigate** to a new route instead of triggering a **modal action**. This caused:

1. `navController.navigate()` changed the route to `Routes.NewQuery.createRoute(connectionId)`
2. Route change updated `NavigationContext` from `InsideConnection` with `/databases` to a new context
3. `destinationsForContext()` returned a different menu (DB menu instead of server menu)
4. The entire screen content was replaced

Additionally, `NewQueryScreen` doesn't render UI itself - it only calls `workspaceManager.openQueryCard()` via `LaunchedEffect`. Wrapping it in a `ModalBottomSheet` created a double-sheet scenario.

---

## Solution

### 1. Mark "New Query" as Modal

**File**: `NavigationDestinations.kt`

```kotlin
NavigationDestination(
    id = "new_query",
    labelRes = R.string.nav_new_query,
    icon = com.sphynxs.mydatabases.ui.components.PhosphorAppIcons.Nav.newQuery,
    route = Routes.NewQuery.createRoute(connectionId),
    isModal = true, // ✅ Added - Opens modal instead of navigating
),
```

### 2. Handle Modal Action Directly

**File**: `MyDataBasesNavHost.kt`

Instead of navigating or wrapping in a sheet, call `WorkspaceManager` directly:

```kotlin
onModalAction = { destinationId ->
    when (destinationId) {
        "add_database" -> showAddDatabaseSheet = true
        "new_query" -> {
            // Extract connectionId from current context
            val connectionId = when (navigationContext) {
                is NavigationContext.InsideConnection -> navigationContext.connectionId
                else -> ""
            }
            // WorkspaceManager handles its own sheet/overlay
            workspaceManager.openQueryCard(
                connectionId = connectionId,
                initialSql = null
            )
        }
    }
}
```

---

## Technical Details

### Navigation Flow (Before)

```
User taps "New Query"
    ↓
AdaptiveNavigationScaffold.onNavigate()
    ↓
navController.navigate(Routes.NewQuery.createRoute(connectionId))
    ↓
Route changes to "connection/{id}/new-query"
    ↓
NavigationContext updates
    ↓
destinationsForContext() returns different menu
    ↓
UI reconfigures with new menu and blank content
```

### Navigation Flow (After)

```
User taps "New Query"
    ↓
AdaptiveNavigationScaffold.onModalAction("new_query")
    ↓
workspaceManager.openQueryCard()
    ↓
WorkspaceManager opens its overlay sheet
    ↓
Route stays unchanged ("connection/{id}/databases")
    ↓
NavigationContext stays unchanged
    ↓
Menu stays the same
    ↓
Databases list stays visible behind the sheet
```

---

## Key Learnings

### 1. Modal vs Navigation Pattern

When a destination should **overlay** the current screen without changing context:
- ✅ Use `isModal = true` in `NavigationDestination`
- ✅ Handle in `onModalAction` instead of `onNavigate`
- ✅ Present as `ModalBottomSheet` or overlay
- ❌ Do NOT call `navController.navigate()`

### 2. Manager-Based UI Pattern

When a screen delegates UI rendering to a manager (like `WorkspaceManager`):
- ✅ Call the manager method directly
- ❌ Do NOT wrap the screen in your own sheet/overlay
- The manager already handles its own presentation layer

Example:
```kotlin
// ❌ Wrong - Creates double sheet
ModalBottomSheet {
    NewQueryScreen() // This calls workspaceManager internally
}

// ✅ Correct - Direct call
workspaceManager.openQueryCard()
```

### 3. Context Preservation

Server-level actions (Add Database, New Query, Monitor) should preserve the server context:
- They appear in the server menu (when at `/databases`)
- They should NOT change the `NavigationContext`
- They should NOT trigger menu switching
- User should return to the same screen and menu after dismissing

---

## Testing Verification

### Test Case 1: New Query Opens Without Navigation Change
1. Navigate to Databases List (connection/{id}/databases)
2. Verify bottom menu shows: Add Database, New Query, Monitor, Settings
3. Tap "New Query"
4. **Expected**: Query editor overlay appears
5. **Expected**: Databases list stays visible behind
6. **Expected**: Bottom menu stays unchanged
7. Close query editor
8. **Expected**: Returns to databases list with same menu

### Test Case 2: No Double Sheets
1. From Databases List, tap "New Query"
2. **Expected**: Only ONE sheet appears (query editor)
3. **Expected**: No empty bottom sheet behind it

### Test Case 3: Connection ID Passed Correctly
1. Connect to a database server (capture connection ID)
2. Navigate to Databases List
3. Tap "New Query"
4. **Expected**: Query editor receives correct connection ID
5. **Expected**: Can execute queries on the connected server

---

## Files Modified

| File | Lines Changed | Description |
|------|---------------|-------------|
| `NavigationDestinations.kt` | +1 | Added `isModal = true` to "new_query" |
| `MyDataBasesNavHost.kt` | +12, -1 | Added direct `workspaceManager.openQueryCard()` call in `onModalAction` |

**Total**: 2 files, 13 insertions, 1 deletion

---

## Related Issues

- Similar pattern already existed for "Add Database" (also `isModal = true`)
- "Editor" destination in DB menu was incorrectly marked as modal (should be navigation since it appears in a different context)
- Monitor destination should potentially follow the same modal pattern

---

## Future Considerations

### Other Modal Candidates

Destinations that might benefit from the modal pattern:
- **Monitor**: Server monitoring should probably overlay the databases list
- **Settings**: Could be modal from any context for quick access

### Consistency Check

Review all `NavigationDestination` entries:
- Server menu items (at `/databases`) → consider modal for context preservation
- DB menu items (at `/tables`, `/views`) → navigation is appropriate (context switch intended)

---

## Commit Message

```
fix: open New Query as modal without changing navigation context

- Mark 'new_query' as isModal in NavigationDestinations
- Call workspaceManager.openQueryCard() directly in onModalAction
- Prevents window and menu changes when opening New Query from databases list
- Fixes double sheet issue by using WorkspaceManager's native overlay
```

---

## Implementation Status

- [x] Mark "new_query" as `isModal = true`
- [x] Implement direct `workspaceManager.openQueryCard()` call
- [x] Remove navigation route call
- [x] Test context preservation
- [x] Test single sheet rendering
- [x] Verify connection ID passing
- [x] Code review
- [x] Commit and push
- [x] Documentation

**Status**: ✅ Complete and deployed
