# Design: Workspace Card Carousel

## Technical Approach

Additive UI feature over the existing `WorkspaceManager`/`WorkspaceOverlay`/`TopSheetFrame` stack. No changes to `WorkspaceManager`, `WorkspaceCard`, or call sites. A trigger `IconButton` is added to `TopSheetFrame`'s notch (mirroring `StepIcon`), local `WorkspaceOverlay` composable state toggles a new `WorkspaceCarousel` overlay built on the confirmed-available M3 `HorizontalMultiBrowseCarousel`.

**Verified APIs (do not assume otherwise):**
- `material3` resolves to **1.3.2** via `compose-bom:2025.05.01`. `androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel` **exists** in this version (`@ExperimentalMaterial3Api`), confirmed by decompiling the cached `material3-release.aar` (1.3.2) — class `androidx/compose/material3/carousel/CarouselKt$HorizontalMultiBrowseCarousel$*`. Signature: `HorizontalMultiBrowseCarousel(state: CarouselState, preferredItemWidth: Dp, modifier: Modifier = Modifier, itemSpacing: Dp = 0.dp, contentPadding: PaddingValues = PaddingValues(0.dp), content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit)`. State via `rememberCarouselState(initialItem: Int = 0) { itemCount }`.
- `material-icons-extended` resolves to **1.7.8**. Confirmed by decompiling the cached aar: `androidx/compose/material/icons/filled/ViewCarouselKt.class` exists → `Icons.Filled.ViewCarousel` is available. No `AutoMirrored` variant needed (symmetric icon).
- No fallback to `HorizontalPager` is required.

## Icon Family Decision

| Option | Evidence | Decision |
|---|---|---|
| Material (`Icons.Filled.*`) | `StepIcon` in `TopSheetFrame.kt` uses `Icons.Default.TableChart`/`Description`; `WorkspaceOverlay.kt` uses `Icons.Default.Close`. 100% of icons in the two files this change touches are Material. | **Chosen** — `Icons.Filled.ViewCarousel`, consistent with local convention. |
| Tabler (`PhosphorAppIcons`) | Used app-wide for Nav/Db/Action families (`TablesListScreen`, nav rail, etc.), zero usage inside `ui/workspace/`. | Rejected — would introduce a new icon family into files that currently have none. |

## Architecture Decisions

### Decision: Carousel visibility state ownership (ADR)

**Choice**: Local `var isCarouselOpen by remember { mutableStateOf(false) }` in `WorkspaceOverlay` (proposal's Option A). No new `WorkspaceState.Carousel` enum value.

**Alternatives considered**: Add `WorkspaceState.Carousel` to the existing enum; make it a 4th mutually-exclusive visual state in `WorkspaceManager`.

**Rationale**: The carousel is an **overlay layer**, not a replacement visual mode — it must work identically from both `Peek` and `Expanded` (per proposal, out-of-scope to touch `WorkspaceState.Peek`). Folding it into the enum would make it mutually exclusive with `Peek`/`Expanded`, which is wrong (the underlying sheet state doesn't change while the carousel is open — only what's drawn on top). It would also require passing carousel intent through `WorkspaceManager`, which is explicitly out of scope. Local state is sufficient because no other component needs to observe carousel visibility. **Confirmed, not overridden.**

### Decision: How card count reaches `TopSheetFrame`

**Choice**: New required param `totalCardCount: Int`, passed by `WorkspaceOverlay` as `activeCards.size`.

**Alternatives considered**: Pass the full `cards: List<WorkspaceCard>` into `TopSheetFrame` (bigger surface change, frame doesn't need the list, only the count and one `card`).

**Rationale**: Minimal signature change; `TopSheetFrame` already receives a single `card` for its own render — passing the count avoids leaking the full workspace list into a component whose job is to render one card.

## Data Flow

    WorkspaceOverlay (isCarouselOpen: Boolean, local state)
         │
         ├─ cards.size >= 2 ──► TopSheetFrame(totalCardCount, onShowCarousel)
         │                             │ tap
         │                             ▼
         │                    onShowCarousel() → isCarouselOpen = true
         │
         └─ isCarouselOpen ──► WorkspaceCarousel(cards, activeIndex, onSelectCard, onCloseCard, onDismiss)
                                       │ select(i)        │ close(i)         │ backdrop/back
                                       ▼                  ▼                  ▼
                          workspaceManager.setActiveIndex(i)  closeCard(i)   isCarouselOpen = false
                                  + isCarouselOpen = false

## File Changes

| File | Action | Description |
|---|---|---|
| `ui/workspace/WorkspaceCarousel.kt` | Create | `HorizontalMultiBrowseCarousel` + item composable, backdrop scrim, enter/exit animation |
| `ui/workspace/TopSheetFrame.kt` | Modify | Add `totalCardCount: Int`, `onShowCarousel: () -> Unit` params; `IconButton` at `Alignment.BottomEnd` in the existing icon `Box` |
| `ui/workspace/WorkspaceOverlay.kt` | Modify | `isCarouselOpen` state, `BackHandler(enabled = isCarouselOpen)`, conditional `WorkspaceCarousel` render (topmost `Box` child), pass new `TopSheetFrame` params |
| `res/values/strings.xml` | Modify | 2 new keys (see below) |
| `res/values-es/strings.xml` | Modify | Same 2 keys, es |
| `androidTest/.../workspace/WorkspaceCarouselTest.kt` | Create | Button visibility, open/select/close scenarios (sized in `sdd-tasks`) |

## Interfaces / Contracts

```kotlin
// TopSheetFrame.kt — new params (inserted before `modifier`, after `onClose`)
fun TopSheetFrame(
    expansionProgress: Float,
    isDragging: Boolean,
    card: WorkspaceCard,
    isExpanded: Boolean,
    onClose: () -> Unit,
    totalCardCount: Int,
    onShowCarousel: () -> Unit,
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1.8f,
    peekHeight: Dp = 60.dp
)

// WorkspaceCarousel.kt — new file
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceCarousel(
    cards: List<WorkspaceCard>,
    activeIndex: Int,
    onSelectCard: (Int) -> Unit,
    onCloseCard: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Button placement** (mirrors `StepIcon`, symmetric inset):
```kotlin
if (totalCardCount >= 2) {
    IconButton(
        onClick = onShowCarousel,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = configuration.screenWidthDp.dp * 0.18f, bottom = 24.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ViewCarousel,
            contentDescription = stringResource(R.string.workspace_carousel_button),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * expansionProgress)
        )
    }
}
```

**Carousel state init** (opens centered on the active card): `rememberCarouselState(initialItem = activeIndex) { cards.size }`. Active item highlighted via `Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)` when `i == activeIndex`; per-item `IconButton` (`Icons.Filled.Close`, existing pattern from `WorkspaceCardContent`) calls `onCloseCard(i)`.

## WorkspaceOverlay Wiring

```kotlin
var isCarouselOpen by remember { mutableStateOf(false) }

androidx.activity.compose.BackHandler(enabled = isCarouselOpen) { isCarouselOpen = false }

// ... existing TopSheet/TopSheetFrame/toolbar Box children ...

TopSheetFrame(
    // existing args...
    totalCardCount = activeCards.size,
    onShowCarousel = { isCarouselOpen = true }
)

// LAST child in the outer Box → renders on top (z-order)
if (isCarouselOpen && activeCards.isNotEmpty()) {
    WorkspaceCarousel(
        cards = activeCards,
        activeIndex = selectedCardIndex,
        onSelectCard = { workspaceManager.setActiveIndex(it); isCarouselOpen = false },
        onCloseCard = { workspaceManager.closeCard(it) },
        onDismiss = { isCarouselOpen = false },
        modifier = Modifier.fillMaxSize()
    )
}
```

`activeCards.isNotEmpty()` guard avoids rendering an empty carousel after the last card is closed from inside it — no `LaunchedEffect` needed.

## `WorkspaceManager.closeCard` clamping (verified, for implementers)

```kotlin
fun closeCard(index: Int) {
    _cards.value = _cards.value.filterIndexed { i, _ -> i != index }
    if (_cards.value.isEmpty()) {
        _state.value = WorkspaceState.Collapsed        // workspace fully collapses
    } else if (_activeIndex.value >= _cards.value.size) {
        _activeIndex.value = _cards.value.size - 1      // clamps to new last index
    }
}
```
- Closing a non-active card: `activeIndex` unaffected unless it now exceeds bounds (only relevant when the *last* index was closed).
- Closing the active card: `activeIndex` value is untouched by `closeCard` unless out of range — if the active card was not the last in the list, `activeIndex` may now point at a *different* card (the one that shifted into that slot). This is pre-existing behavior, not introduced by this change; the carousel must not assume `activeIndex` still refers to the same title after a close.
- Closing the last remaining card: `_state` → `Collapsed`, our `activeCards.isNotEmpty()` guard closes the carousel automatically.

## Back-Button Handling

No `BackHandler` exists anywhere in the app today (verified repo-wide). Add one **scoped to `WorkspaceOverlay`**, `enabled = isCarouselOpen` only — it intercepts system back exclusively while the carousel is open and is a no-op otherwise, so it cannot interfere with existing nav-graph back behavior.

## i18n

New keys (snake_case, feature-prefixed, matching `folder_delete_*` convention), added to both `res/values/strings.xml` and `res/values-es/strings.xml`:

| Key | en | es |
|---|---|---|
| `workspace_carousel_button` | "Show all open cards" | "Ver todas las tarjetas abiertas" |
| `workspace_carousel_close_card` | "Close %1$s" | "Cerrar %1$s" |

## Animation

Backdrop: reuse `TopSheet.kt`'s scrim pattern — `Canvas` + `detectTapGestures { onDismiss() }`, `Color.White.copy(alpha = 0.4f)` (fixed, no drag). Carousel surface: `AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut() + scaleOut())` around the `HorizontalMultiBrowseCarousel` — simple, no custom spring tuning needed; M3 carousel handles its own item-to-item transitions internally.

## Testing Strategy

| Layer | What | Approach |
|---|---|---|
| AndroidTest | Button hidden ≤1 card / visible ≥2, in both Peek and Expanded | Compose UI test, `onNodeWithContentDescription` |
| AndroidTest | Tap opens carousel; shows all cards; active highlighted | `WorkspaceCarouselTest.kt` |
| AndroidTest | Tap card → `setActiveIndex` + dismiss | Assert `activeIndex` state + carousel gone |
| AndroidTest | Per-item close → `closeCard`; last-card close collapses carousel | Assert count decreases / carousel absent |

## Migration / Rollout

No migration required. Fully additive; rollback = revert 3 files + delete `WorkspaceCarousel.kt` (per proposal's Rollback Plan).

## Open Questions

- None blocking. `HorizontalMultiBrowseCarousel`'s `@ExperimentalMaterial3Api` opt-in is a compile-time annotation, not a design risk.
