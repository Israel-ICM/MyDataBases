# Proposal: Workspace Card Carousel

## Intent

MyDataBases already supports multiple simultaneously-open `WorkspaceCard`s (table view, table edit, new query) — one is active/focused, the rest sit backgrounded and invisible. Today there is NO way to see them: `WorkspaceOverlay` collects the full `cards` list and `activeIndex` but only ever renders the single active card. The user cannot browse open cards or switch between them from the UI. This change adds a "view carousel" affordance — an icon button in the empty bottom-right of the step-notch (mirroring the existing `StepIcon` at bottom-left) that, pressed AT ANY TIME (Peek or Expanded), surfaces a browsable carousel of all open cards so the user can pick which one becomes active. It is a standalone action that coexists with Peek/Expanded and does NOT implement or replace the separate, still-unimplemented `WorkspaceState.Peek` ("first 3 cards + FAB") concept.

## Scope

### In Scope
- View-carousel `IconButton` at `Alignment.BottomEnd` of the notch in `TopSheetFrame`, symmetric to the existing `StepIcon` (`BottomStart`), sharing the same `expansionProgress` alpha and 24.dp bottom padding.
- Button visible only when `cards.size > 1` (hidden for zero/one card).
- New `TopSheetFrame` callback param (e.g. `onShowCarousel: () -> Unit`) — the frame owns the button but delegates the toggle to `WorkspaceOverlay`.
- Carousel open/close state held locally in `WorkspaceOverlay` (`remember { mutableStateOf(false) }`) — Option A, no `WorkspaceManager` change for basic browse+select.
- New `WorkspaceCarousel.kt`: Material3 `HorizontalMultiBrowseCarousel` rendering ALL open cards (active card visually distinguished), tap → `setActiveIndex(index)` + dismiss.
- Per-item close affordance (`closeCard(index)`, already exists) — "×" or long-press.
- Available from BOTH Peek and Expanded, independent of `WorkspaceState`.
- New strings in `strings.xml` + `strings-es.xml` (button + close content-descriptions).

### Out of Scope
- Implementing `WorkspaceState.Peek` ("first 3 cards + FAB") — separate, independent affordance.
- Any change to the list+index data model in `WorkspaceManager` (browse/select/close all use existing API).
- Drag-to-reorder cards in the carousel.
- Card thumbnails/live previews (carousel items show title + type icon, not rendered content).
- Changes to call sites (`TablesListScreen`, `MyDataBasesNavHost`, `NewQueryScreen`), `WorkspaceCard`, or any ViewModel.

## Capabilities

### New Capabilities
- `workspace-carousel`: carousel trigger button (placement, visibility threshold), all-cards browse view, active-card highlight, tap-to-activate, per-item close, availability across Peek/Expanded states.

### Modified Capabilities
- None. No existing `openspec/specs/` capabilities cover the workspace surface; this is the first formal spec for it.

## Approach

**State (Option A — no data-model change):**
- `WorkspaceManager` already exposes `cards`, `activeIndex`, `setActiveIndex(index)`, `closeCard(index)` — sufficient for browse/select/close.
- Carousel visibility is UI-local: `var isCarouselOpen by remember { mutableStateOf(false) }` in `WorkspaceOverlay`. No shared flag needed; simpler and keeps `WorkspaceManager` untouched.

**UI:**
- `TopSheetFrame`: add `onShowCarousel: () -> Unit` param and a `showCarouselButton: Boolean` (driven by `cards.size > 1` from the overlay). Render an `IconButton` (view-carousel icon) in the existing notch `Box` at `Alignment.BottomEnd`, mirroring `StepIcon` placement and alpha.
- `WorkspaceCarousel.kt`: `HorizontalMultiBrowseCarousel` (`androidx.compose.material3.carousel`, stable in BOM 2025.05.01) over `cards`; each item = title + type icon + close button; active index visually distinguished (border/elevation/tint); item tap → `onSelect(index)`; close tap → `onClose(index)`.
- `WorkspaceOverlay`: own `isCarouselOpen`; pass `showCarouselButton = activeCards.size > 1` and `onShowCarousel = { isCarouselOpen = true }` to `TopSheetFrame`; render `WorkspaceCarousel` as an overlay layer when `isCarouselOpen`, wiring `onSelect = { workspaceManager.setActiveIndex(it); isCarouselOpen = false }` and `onClose = { workspaceManager.closeCard(it) }`.

**Icon:** verify exact name in `material-icons-extended` during spec/design (candidate `Icons.Filled.ViewCarousel`) — do not assume it exists.

**Tests (TDD red-first):**
- AndroidTest: button hidden with ≤1 card, visible with ≥2; button available in Peek and Expanded; tapping opens carousel; carousel shows all open cards; tapping a card sets it active + dismisses; per-item close removes the card.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/workspace/WorkspaceCarousel.kt` | New | M3 `HorizontalMultiBrowseCarousel` UI + item composable |
| `ui/workspace/TopSheetFrame.kt` | Modified | New `onShowCarousel` + `showCarouselButton` params; carousel `IconButton` at `BottomEnd` |
| `ui/workspace/WorkspaceOverlay.kt` | Modified | Local `isCarouselOpen` state; conditional carousel render; wire select/close |
| `res/values/strings.xml` | Modified | Carousel button + close content-descriptions (en) |
| `res/values-es/strings.xml` | Modified | Same strings (es) |
| `androidTest/.../workspace/` | New | Carousel button + browse/select/close E2E scenarios |

No changes to `WorkspaceManager`, `WorkspaceCard`, `TablesListScreen`, `MyDataBasesNavHost`, `NewQueryScreen`, or any ViewModel.

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `HorizontalMultiBrowseCarousel` not present/stable in BOM 2025.05.01 | Low | BOM verified recent; confirm import compiles in design; fallback to `HorizontalPager` if absent |
| `Icons.Filled.ViewCarousel` not in `material-icons-extended` | Low | `material-icons-extended` confirmed as dependency; verify exact icon name in spec, pick alternative (Tabler icons also available) |
| `strings.xml` merge with in-flight `editor-completion-and-format` | Medium | Both `strings.xml` files also touched there; append DISTINCT keys only — not a blocker, no logic overlap |
| Carousel z-order/gesture conflict with TopSheet drag | Medium | Render carousel as top overlay layer above `TopSheetFrame`; dismiss on selection; scope drag to sheet only |
| Notch `BottomEnd` crowding on narrow screens | Low | Mirror `StepIcon` padding math; 24.dp bottom, symmetric horizontal inset |

## Rollback Plan

Fully additive UI feature — no data model, no persisted state, no migrations. Rollback = revert the four touched files + delete `WorkspaceCarousel.kt`:
- Remove `onShowCarousel`/`showCarouselButton` params and the `IconButton` from `TopSheetFrame`.
- Remove `isCarouselOpen` state and carousel render block from `WorkspaceOverlay`.
- Delete `WorkspaceCarousel.kt`.
- Remove the new string keys from both `strings.xml` files.

`WorkspaceManager` is never modified, so no state-machine rollback risk. Single-PR revert.

## Dependencies

- Existing: `WorkspaceManager` (`cards`, `activeIndex`, `setActiveIndex`, `closeCard`), `TopSheetFrame`, `WorkspaceOverlay`, `WorkspaceCard`.
- Existing libs: `androidx.compose.material3` (carousel), `material-icons-extended` — both already in `app/build.gradle.kts` (BOM 2025.05.01).
- No new third-party libraries, Gradle modules, or Room tables.

## Success Criteria

- [ ] Carousel button appears at bottom-right of the notch only when ≥2 cards are open; hidden for 0 or 1.
- [ ] Button is pressable from BOTH Peek and Expanded states.
- [ ] Pressing it opens a carousel showing ALL open cards, with the active card visually distinguished.
- [ ] Tapping a card sets it active (`setActiveIndex`) and dismisses the carousel.
- [ ] Each carousel item can close its card (`closeCard`).
- [ ] `WorkspaceManager`, `WorkspaceCard`, and all call sites are unchanged.
- [ ] en + es content-description strings shipped.
- [ ] AndroidTest scenarios green; single PR under the 400-line review budget.

## Assumptions flagged for maintainer review

These were sensible defaults chosen during exploration — confirm or correct BEFORE spec/design proceeds:

1. **Button visibility threshold**: hidden when `cards.size <= 1`, visible only at ≥2 open cards. (A carousel of one card is pointless; the active card is already shown.) — *Reconfirm.*
2. **All cards vs backgrounded-only**: carousel shows ALL open cards including the active one (highlighted), not only the hidden ones. — *Reconfirm.*
3. **Close-from-carousel in scope**: per-item close is included since `closeCard(index)` already exists and it is expected UX. — *Confirm/reject for v1 scope.*
4. **State ownership**: carousel open/close lives as local `WorkspaceOverlay` composable state, NOT a new `WorkspaceManager` flag. — *Confirm; switch to a shared flow only if a future requirement needs cross-component observation.*
