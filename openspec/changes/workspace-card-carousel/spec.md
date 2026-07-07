# Spec: Workspace Card Carousel

Delta spec for change `workspace-card-carousel`. Introduces ONE new capability under the workspace surface: `workspace-carousel`. No existing capabilities are modified or removed — this is the first formal spec for the workspace overlay surface.

## ADDED Capabilities

- `workspace-carousel` — a carousel trigger `IconButton` in the bottom-right of the `TopSheetFrame` step-notch (mirroring the existing bottom-left `StepIcon`), visible only when ≥2 cards are open, pressable from BOTH `Peek` and `Expanded` states; opening surfaces a Material3 `HorizontalMultiBrowseCarousel` of ALL open cards with the active card visually distinguished; tap-to-activate (`setActiveIndex` + dismiss), per-item close (`closeCard`), and dismissal via backdrop/back without mutating `activeIndex`.

---

## Spec Author DECISIONS (not maintainer-confirmed)

Where the proposal left interaction/visual behavior unpinned, this spec makes the following calls. They are the SPEC AUTHOR's decisions — sensible defaults aligned with Material3 `HorizontalMultiBrowseCarousel` conventions and existing `WorkspaceManager` semantics — and are OPEN TO REVISION by the maintainer at design/tasks/apply time. They are NOT presented as confirmed facts.

- **DECISION D1 — active-card visual treatment**: The active card's carousel item is distinguished by a 2.dp border in `MaterialTheme.colorScheme.primary` PLUS an elevated tonal surface (`tonalElevation` higher than inactive items). Non-active items render with no border and base elevation. (Simplest concrete rule; carousel already centers/enlarges the focused item via `HorizontalMultiBrowseCarousel` layout, so the border is the semantic "this is active" marker, independent of scroll position.)
- **DECISION D2 — close-active-card fallback**: Closing a card from the carousel delegates entirely to the EXISTING `WorkspaceManager.closeCard(index)`; this spec does NOT introduce new fallback logic. The observable result is exactly whatever `closeCard`'s current clamping produces (Requirement "Per-item close respects existing WorkspaceManager clamping"). The carousel stays open after a close as long as ≥1 card remains, re-rendering the shortened list.
- **DECISION D3 — dismissal channels**: Backdrop tap, predictive-back gesture, and the system BACK button are ALL treated identically — they set the local `isCarouselOpen = false` and make NO call to `setActiveIndex` or `closeCard`.
- **DECISION D4 — tapping the active card**: Tapping the already-active item is a pure dismissal (close carousel, no `setActiveIndex` call), NOT a no-op-that-leaves-carousel-open. "No state change" means no `WorkspaceManager` mutation; the carousel still closes.
- **DECISION D5 — carousel item content**: Each item shows the card's `title` + a type icon (`TableChart` for `Table`, `Description` for `Query`) + a close affordance. No rendered/live card preview (out of scope per proposal).

---

## Capability: workspace-carousel

### Requirement: Carousel trigger button placement and visibility

The system MUST render a carousel trigger `IconButton` inside the `TopSheetFrame` step-notch, positioned symmetric to the existing `StepIcon`. Specifically:

- The button MUST be aligned `Alignment.BottomEnd` of the same offset `Box` that hosts the `StepIcon` (`StepIcon` sits at `BottomStart`), with `padding(end = screenWidthDp * 0.18f, bottom = 24.dp)` — the horizontal mirror of `StepIcon`'s `padding(start = screenWidthDp * 0.18f, bottom = 24.dp)`.
- The button icon MUST be `size(24.dp)` and share the `StepIcon` alpha treatment (`alpha = expansionProgress`) so it fades in/out with sheet expansion identically.
- `TopSheetFrame` MUST accept a new `onShowCarousel: () -> Unit` callback and a `showCarouselButton: Boolean` param; the button is rendered ONLY when `showCarouselButton == true`.
- `WorkspaceOverlay` MUST drive `showCarouselButton = activeCards.size >= 2` (visible at 2+ open cards; hidden at 0 or 1).

#### Scenario 1: Button hidden with 0 or 1 open card

- **GIVEN** the workspace has exactly 1 open card (`activeCards.size == 1`)
- **WHEN** the `TopSheetFrame` renders in any non-`Collapsed` state
- **THEN** the carousel trigger button MUST NOT be present in the composition (`showCarouselButton == false`).
- **AND** the same holds when `activeCards.size == 0` (the overlay renders nothing at all — sheet is `Collapsed`).
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::carouselButton_hiddenWithSingleCard()`
  - [ ] E2E: `WorkspaceCarouselTest::carouselButton_hiddenWithZeroCards()`

#### Scenario 2: Button visible with 2+ open cards

- **GIVEN** the workspace has 2 or more open cards (`activeCards.size >= 2`)
- **WHEN** the `TopSheetFrame` renders
- **THEN** the carousel trigger button MUST be present, aligned `BottomEnd` of the step-notch box with `padding(end = screenWidthDp * 0.18f, bottom = 24.dp)`, mirroring the `StepIcon` at `BottomStart`.
- **AND** its icon MUST be `24.dp` and use `alpha = expansionProgress`.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::carouselButton_visibleWithTwoOrMoreCards()`
  - [ ] E2E: `WorkspaceCarouselTest::carouselButton_placedBottomEndMirroringStepIcon()`

### Requirement: Trigger availability across Peek and Expanded

The carousel trigger MUST be pressable regardless of `WorkspaceState`, whenever the sheet is visible (`Peek` or `Expanded`) and `activeCards.size >= 2`. Pressing it MUST open the carousel WITHOUT changing `WorkspaceState` — the carousel is a standalone overlay layer, NOT a state transition, and it neither implements nor replaces the separate unimplemented `WorkspaceState.Peek` ("first 3 cards + FAB") concept.

#### Scenario 3: Pressing the button while state is Peek opens the carousel

- **GIVEN** `activeCards.size >= 2` and `WorkspaceState == Peek`
- **WHEN** the user taps the carousel trigger button
- **THEN** the carousel overlay MUST become visible (`isCarouselOpen == true`)
- **AND** `WorkspaceState` MUST remain `Peek` (no `expand()`/`peek()`/`collapse()` call is made).
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::pressButtonInPeek_opensCarouselWithoutStateChange()`

#### Scenario 4: Pressing the button while state is Expanded opens the carousel

- **GIVEN** `activeCards.size >= 2` and `WorkspaceState == Expanded`
- **WHEN** the user taps the carousel trigger button
- **THEN** the carousel overlay MUST become visible (`isCarouselOpen == true`)
- **AND** `WorkspaceState` MUST remain `Expanded` (carousel availability is independent of state).
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::pressButtonInExpanded_opensCarouselWithoutStateChange()`

### Requirement: Carousel renders ALL open cards with active card distinguished

When open, the carousel MUST render a Material3 `HorizontalMultiBrowseCarousel` over the FULL `activeCards` list (including the currently-active card, NOT only backgrounded ones). The item at `selectedCardIndex` (the active card) MUST be visually distinguished per DECISION D1: a 2.dp `primary` border plus higher tonal elevation than inactive items. Each item MUST show the card `title` + its type icon + a per-item close affordance (DECISION D5).

#### Scenario 5: Carousel shows every open card, active one highlighted

- **GIVEN** 3 open cards `[A, B, C]` with `selectedCardIndex == 1` (card `B` active)
- **WHEN** the carousel opens
- **THEN** the carousel MUST contain exactly 3 items (`A`, `B`, `C`) — all open cards, not just backgrounded ones.
- **AND** the item for card `B` MUST render the active treatment (2.dp `primary` border + elevated tonal surface) while `A` and `C` render inactive (no border, base elevation).
- **AND** each item MUST display its card `title` and type icon.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::carousel_showsAllCardsWithActiveHighlighted()`

### Requirement: Tapping a non-active card activates it and dismisses

Tapping a carousel item that is NOT the currently-active card MUST call `WorkspaceManager.setActiveIndex(index)` for that item's index, then dismiss the carousel (`isCarouselOpen = false`). After dismissal, the tapped card MUST be the one rendered as `TopSheetFrame` content (because `WorkspaceOverlay` reads `selectedCardIndex` from the manager).

#### Scenario 6: Tapping a non-active card sets it active and closes the carousel

- **GIVEN** open cards `[A, B, C]`, `selectedCardIndex == 0` (card `A` active), carousel open
- **WHEN** the user taps the item for card `C` (index 2)
- **THEN** `WorkspaceManager.setActiveIndex(2)` MUST be called exactly once
- **AND** the carousel MUST dismiss (`isCarouselOpen == false`)
- **AND** the `TopSheetFrame` MUST subsequently render card `C`'s content.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::tapNonActiveCard_setsActiveAndDismisses()`

### Requirement: Tapping the already-active card dismisses with no state change

Tapping the carousel item that IS the currently-active card MUST dismiss the carousel WITHOUT calling `setActiveIndex` and without any other `WorkspaceManager` mutation (DECISION D4). The only observable effect is the carousel closing.

#### Scenario 7: Tapping the active card is a pure dismissal

- **GIVEN** open cards `[A, B, C]`, `selectedCardIndex == 1` (card `B` active), carousel open
- **WHEN** the user taps the item for card `B` (index 1)
- **THEN** the carousel MUST dismiss (`isCarouselOpen == false`)
- **AND** `WorkspaceManager.setActiveIndex` MUST NOT be called
- **AND** `selectedCardIndex` MUST remain `1` (no state change beyond closing).
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::tapActiveCard_dismissesWithoutStateChange()`

### Requirement: Per-item close respects existing WorkspaceManager clamping

Each carousel item MUST expose a close affordance that calls `WorkspaceManager.closeCard(index)` with that item's index. This spec MUST NOT introduce new fallback/re-selection logic (DECISION D2) — the resulting active-card selection MUST be exactly whatever the EXISTING `closeCard` clamping produces:

- `closeCard` removes the card at `index` via `filterIndexed`.
- If the list becomes empty → `WorkspaceState` becomes `Collapsed` (see next Requirement).
- Else if `activeIndex >= newSize` → `activeIndex` is clamped to `newSize - 1`.
- Else `activeIndex` is left UNCHANGED (so when a card at or before the active slot is removed, the index now points at the card that shifted into that slot).

As long as ≥1 card remains after a close, the carousel MUST stay open and re-render the shortened list.

#### Scenario 8: Closing the active card falls back per existing clamping (active was last)

- **GIVEN** open cards `[A, B, C]`, `selectedCardIndex == 2` (card `C`, the LAST, active), carousel open
- **WHEN** the user taps the close affordance on card `C` (index 2)
- **THEN** `WorkspaceManager.closeCard(2)` MUST be called
- **AND** per existing clamping (`activeIndex 2 >= newSize 2`), `activeIndex` MUST become `1` (card `B` — the neighboring card)
- **AND** the carousel MUST remain open showing the 2 remaining cards `[A, B]`.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::closeActiveLastCard_clampsToNeighbor_carouselStaysOpen()`

#### Scenario 9: Closing the active card falls back per existing clamping (active not last)

- **GIVEN** open cards `[A, B, C]`, `selectedCardIndex == 1` (card `B` active, NOT last), carousel open
- **WHEN** the user taps the close affordance on card `B` (index 1)
- **THEN** `WorkspaceManager.closeCard(1)` MUST be called
- **AND** per existing clamping (`activeIndex 1 < newSize 2`, unchanged), `activeIndex` MUST remain `1`, which now points at card `C` (shifted into the freed slot)
- **AND** the carousel MUST remain open showing the 2 remaining cards `[A, C]`.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::closeActiveNonLastCard_indexUnchangedPointsToShiftedNeighbor()`

#### Scenario 10: Closing a non-active card leaves the active selection intact

- **GIVEN** open cards `[A, B, C]`, `selectedCardIndex == 2` (card `C` active), carousel open
- **WHEN** the user taps the close affordance on card `A` (index 0)
- **THEN** `WorkspaceManager.closeCard(0)` MUST be called
- **AND** per existing clamping (`activeIndex 2 >= newSize 2`), `activeIndex` MUST become `1`, still pointing at card `C` (now at index 1 after `A` removed)
- **AND** the carousel MUST remain open showing `[B, C]`.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::closeNonActiveCard_activeCardRemainsRendered()`

### Requirement: Closing the last card dismisses carousel and collapses sheet

Closing the FINAL open card from within the carousel (0 cards remain) MUST dismiss the carousel AND result in the sheet collapsing — consistent with the EXISTING zero-cards behavior (this spec does NOT invent new behavior): `WorkspaceManager.closeCard` sets `WorkspaceState = Collapsed` when the list becomes empty, and `WorkspaceOverlay` renders nothing (neither sheet nor carousel) when `activeCards.isEmpty()`.

#### Scenario 11: Closing the last card collapses the workspace

- **GIVEN** exactly 1 open card would normally hide the button, so this Requirement is reachable only transitively: open cards `[A, B]` with the carousel open, and the user closes one card leaving `[B]`, then — because the carousel stays open at ≥1 card — closes `B` as the final card
- **WHEN** the user taps close on the final remaining card (index 0, list size 1 → 0)
- **THEN** `WorkspaceManager.closeCard(0)` MUST be called
- **AND** `WorkspaceState` MUST become `Collapsed` (existing zero-cards behavior)
- **AND** the carousel MUST dismiss and `WorkspaceOverlay` MUST render only `backgroundContent` (no sheet, no carousel).
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::closeLastCardFromCarousel_dismissesAndCollapses()`

### Requirement: Dismissal without mutating activeIndex

The carousel MUST be dismissible via backdrop tap, predictive-back gesture, and the system BACK button; ALL three MUST set `isCarouselOpen = false` and MUST NOT call `setActiveIndex` or `closeCard` (DECISION D3). `selectedCardIndex` and `WorkspaceState` MUST be unchanged by any dismissal channel.

#### Scenario 12: Backdrop tap dismisses without changing activeIndex

- **GIVEN** open cards `[A, B, C]`, `selectedCardIndex == 1`, carousel open
- **WHEN** the user taps the scrim/backdrop outside the carousel items
- **THEN** the carousel MUST dismiss (`isCarouselOpen == false`)
- **AND** `selectedCardIndex` MUST remain `1` (no `setActiveIndex`), and no card is closed.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::backdropTap_dismissesWithoutChangingActiveIndex()`

#### Scenario 13: System back / back gesture dismisses without changing activeIndex

- **GIVEN** the carousel is open with `selectedCardIndex == 1`
- **WHEN** the user triggers the system BACK button or the predictive-back gesture
- **THEN** the carousel MUST dismiss (`isCarouselOpen == false`) and consume that back event
- **AND** `selectedCardIndex` MUST remain `1`; no `setActiveIndex`/`closeCard` call is made; `WorkspaceState` is unchanged.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::systemBack_dismissesCarouselOnlyWithoutStateChange()`

---

## i18n Requirements

### Requirement: Localized content-description strings

All user-facing strings introduced by this change MUST be defined in `res/values/strings.xml` and translated for `values-es/strings.xml`. The remaining 8 supported locales fall back to the default `values/strings.xml` (per the precedent set by prior changes, e.g. `editor-completion-and-format`). New string keys MUST be DISTINCT from any keys in in-flight changes (append-only, no logic overlap). Minimum string set:

- `workspace_carousel_button_description` — content description for the carousel trigger button.
- `workspace_carousel_close_card_description` — content description for the per-item close affordance (SHOULD accept the card title as a format arg, e.g. `Close %1$s`, so screen readers announce which card).

SQL/technical identifiers and card titles themselves are data, not UI strings, and are NOT translated.

#### Scenario 14: Content descriptions localized × en + es (full); rest fallback

- **GIVEN** the device locale is `en` or `es`
- **WHEN** the carousel button and a per-item close affordance render
- **THEN** their `contentDescription`s MUST come from the matching locale resources (`workspace_carousel_button_description`, `workspace_carousel_close_card_description`) — NOT hardcoded strings.
- **AND** when the device locale is any of the other 8 supported locales, the strings MUST fall back to `values/strings.xml` without crashing or showing raw keys.
- **Acceptance**:
  - [ ] Unit: `StringsResourceTest::workspace_carousel_strings_existInEnAndEs()`
  - [ ] E2E: `WorkspaceCarouselTest::carouselStrings_useLocalizedResources(locale=es)`

---

## Non-Functional Requirements

### Accessibility

- The carousel trigger button MUST have a non-empty `contentDescription` from `workspace_carousel_button_description`.
- Each per-item close affordance MUST have a non-empty `contentDescription` from `workspace_carousel_close_card_description`.
- All interactive targets — the trigger button, each carousel item (tap-to-activate), and each close affordance — MUST have a minimum touch target of 48dp × 48dp (via `IconButton` default sizing or an explicit `minimumInteractiveComponentSize`), even if the visual glyph is 24dp.

#### Scenario 15: Tap targets ≥48dp and content descriptions present

- **GIVEN** the carousel is open with ≥2 cards
- **WHEN** accessibility/layout is inspected
- **THEN** the trigger button and every close affordance MUST report a touch target of at least 48dp in both dimensions
- **AND** the trigger button and every close affordance MUST expose a non-empty `contentDescription`.
- **Acceptance**:
  - [ ] E2E: `WorkspaceCarouselTest::tapTargets_atLeast48dp_andContentDescriptionsPresent()`

### i18n

- All introduced UI strings MUST live in `res/values/strings.xml`; English and Spanish MUST be fully translated; the remaining 8 supported locales rely on Android's fallback to the default resource.

---

## Out of Scope (explicit non-requirements)

The following are explicitly NOT required by this spec and MUST NOT be implemented in this change:

- Implementing `WorkspaceState.Peek` ("first 3 cards + FAB") — a separate, independent affordance. The carousel neither implements nor replaces it.
- Any change to the `WorkspaceManager` data model or API (browse/select/close use existing `cards`, `activeIndex`, `setActiveIndex`, `closeCard`). No new shared flow/flag.
- Drag-to-reorder cards in the carousel.
- Card thumbnails / live rendered previews (items show title + type icon only).
- New fallback/re-selection logic when closing the active card — the existing `closeCard` clamping is authoritative.
- Changes to call sites (`TablesListScreen`, `MyDataBasesNavHost`, `NewQueryScreen`), `WorkspaceCard`, or any ViewModel.

These remain out of the test surface of this change.
