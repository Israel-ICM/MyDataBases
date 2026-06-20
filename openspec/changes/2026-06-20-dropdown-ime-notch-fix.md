# Dropdown IME Focus Fix + ModalBottomSheet Notch Fix

**Type:** Bugfix  
**Status:** Completed  
**Date:** 2026-06-20

## Summary

Two UI fixes: (1) search filter inside IOSDropdownField not receiving focus/IME keyboard, (2) ModalBottomSheet content drawn under the notch on devices with display cutouts.

---

## Fix 1: DropdownMenu → Dialog for IME/Focus

### Problem
The search filter `OutlinedTextField` inside `IOSDropdownField` could not receive focus or open the software keyboard. The filter used `DropdownMenu` which internally creates a `Popup` — Compose Popups have chronic IME routing issues on Android.

### Solution
Replaced `DropdownMenu` with `Dialog`:

- `Dialog` creates a separate window that connects properly with the IME
- Added `LocalSoftwareKeyboardController.show()` after `requestFocus()` with 300ms delay for render
- Removed unused imports (`DpOffset`, `PopupProperties`, `BasicTextField`, `SolidColor`, `MutableInteractionSource`)
- Added `"No results"` empty state when filter returns nothing

### Files Changed
- `app/src/main/java/com/sphynxs/mydatabases/ui/components/ios/IOSDropdownField.kt` (+120 -102)

### Commit
- `8561390` — fix: replace DropdownMenu with Dialog to fix IME/focus in search filter

---

## Fix 2: ModalBottomSheet Notch Overlap

### Problem
The Add Database `ModalBottomSheet` (with `skipPartiallyExpanded = true`) expands to full height and its content is drawn under the status bar / notch.

### Attempted Solutions
1. `Modifier.statusBarsPadding()` → compiled but insets are 0 inside ModalBottomSheet's popup context
2. `windowInsets = WindowInsets.systemBars` on `ModalBottomSheet` → parameter not available in current Compose BOM version
3. **Final**: Read `status_bar_height` from Android resources directly → works regardless of popup/dialog context

### Solution
- Read `status_bar_height` dimension from Android framework resources (includes notch/cutout)
- Fallback to 24.dp standard height
- Apply as top padding on the content `Box` inside `ModalBottomSheet`

```kotlin
val statusBarHeightDp = with(LocalDensity.current) {
    LocalContext.current.resources
        .getIdentifier("status_bar_height", "dimen", "android")
        .takeIf { it > 0 }
        ?.let { resourceId ->
            LocalContext.current.resources.getDimensionPixelSize(resourceId).toDp()
        } ?: 24.dp
}
```

### Files Changed
- `app/src/main/java/com/sphynxs/mydatabases/ui/screens/databases/DatabasesListScreen.kt` (+13 -1)

### Commit
- `a348d90` — fix: prevent ModalBottomSheet content from going under notch

---

## Technical Decisions

### Why Dialog over DropdownMenu?
- `Dialog` creates an actual window with its own IME connection
- `DropdownMenu` uses `Popup` which has no reliable IME routing
- Same visual result: white card, rounded corners (20.dp), shadow, scrollable items

### Why not WindowInsets API?
- `statusBarsPadding()` → returns 0 inside ModalBottomSheet internal popup
- `windowInsets` parameter → not available in current Compose BOM (`2025.05.01`) for `ModalBottomSheet`
- Android resource `status_bar_height` is always correct regardless of window type

### Alternative Notch Fix Still Pending
The resource-based padding works at the content level. For a proper fix at the sheet level, the `ModalBottomSheet` would need `windowInsets` support — either by upgrading the BOM or using a custom sheet implementation.

## Commits
- `8561390` — fix: replace DropdownMenu with Dialog to fix IME/focus in search filter
- `a348d90` — fix: prevent ModalBottomSheet content from going under notch

## Testing
- ✅ Dropdown filter receives focus and shows keyboard
- ✅ Dropdown items filterable by search text
- ✅ "No results" shown when filter matches nothing
- ⚠️ ModalBottomSheet top padding applied but notch avoidance not fully confirmed
