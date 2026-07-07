package com.sphynxs.mydatabases.ui.workspace

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Compose UI tests para la capability `workspace-carousel` (15 spec scenarios, 17 methods here
 * + 1 in `StringsResourceTest`). See spec.md / design.md / tasks.md under
 * `openspec/changes/workspace-card-carousel/`.
 *
 * TDD note (`strict_tdd: true`): written FIRST, in RED->GREEN order per tasks.md, against
 * code implemented in this same apply batch. Execution (`./gradlew connectedAndroidTest`)
 * is DEFERRED to the maintainer per explicit session instruction — see apply-progress TDD
 * Cycle Evidence table; GREEN is NOT confirmed by this session.
 *
 * `WorkspaceManager` is instantiated directly (real class, no-arg constructor) rather than
 * mocked. All fixtures use `Table` cards to avoid `QueryEditorScreen`'s Hilt `hiltViewModel()`.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class WorkspaceCarouselTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun tableCard(idSuffix: String, title: String): WorkspaceCard.Table =
        WorkspaceCard.Table(
            id = "table:conn:db:$idSuffix",
            title = title,
            connectionId = "conn",
            databaseName = "db",
            tableName = idSuffix
        )

    // ---- Scenario 1: Button hidden with 0 or 1 open card ----

    /**
     * GIVEN 1 open card WHEN TopSheetFrame renders THEN the trigger button MUST NOT exist.
     */
    @Test
    fun carouselButton_hiddenWithSingleCard() {
        val card = tableCard("a", "A")
        composeTestRule.setContent {
            TopSheetFrame(
                expansionProgress = 1f,
                isDragging = false,
                card = card,
                isExpanded = true,
                onClose = {},
                totalCardCount = 1,
                onShowCarousel = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Show all open cards")
            .assertDoesNotExist()
    }

    /**
     * GIVEN 0 open cards WHEN TopSheetFrame renders THEN the trigger button MUST NOT exist.
     */
    @Test
    fun carouselButton_hiddenWithZeroCards() {
        val card = tableCard("a", "A")
        composeTestRule.setContent {
            TopSheetFrame(
                expansionProgress = 1f,
                isDragging = false,
                card = card,
                isExpanded = true,
                onClose = {},
                totalCardCount = 0,
                onShowCarousel = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Show all open cards")
            .assertDoesNotExist()
    }

    // ---- Scenario 2: Button visible with 2+ open cards ----

    /**
     * GIVEN 2+ open cards WHEN TopSheetFrame renders THEN the trigger button MUST exist.
     */
    @Test
    fun carouselButton_visibleWithTwoOrMoreCards() {
        val card = tableCard("a", "A")
        composeTestRule.setContent {
            TopSheetFrame(
                expansionProgress = 1f,
                isDragging = false,
                card = card,
                isExpanded = true,
                onClose = {},
                totalCardCount = 2,
                onShowCarousel = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Show all open cards")
            .assertExists()
    }

    /**
     * The trigger button's bounds MUST be the horizontal mirror of StepIcon's bounds:
     * same bottom inset, and (screen width - button.right) == StepIcon.left.
     */
    @Test
    fun carouselButton_placedBottomEndMirroringStepIcon() {
        val card = tableCard("a", "A")
        composeTestRule.setContent {
            TopSheetFrame(
                expansionProgress = 1f,
                isDragging = false,
                card = card,
                isExpanded = true,
                onClose = {},
                totalCardCount = 2,
                onShowCarousel = {}
            )
        }

        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        val stepIconBounds = composeTestRule
            .onNodeWithContentDescription(card.title)
            .getBoundsInRoot()
        val triggerBounds = composeTestRule
            .onNodeWithContentDescription("Show all open cards")
            .getBoundsInRoot()

        // Same bottom inset (both mirror `padding(bottom = 24.dp)`)
        assertEquals(stepIconBounds.bottom.value, triggerBounds.bottom.value, 1f)

        // Horizontal mirror: distance-from-left of StepIcon == distance-from-right of trigger
        val stepIconLeftInset = stepIconBounds.left.value
        val triggerRightInset = rootBounds.right.value - triggerBounds.right.value
        assertEquals(stepIconLeftInset, triggerRightInset, 2f)
    }

    // ---- Scenario 3 / 4: Trigger availability across Peek and Expanded ----

    /**
     * GIVEN WorkspaceState == Peek WHEN the trigger is tapped THEN the carousel opens
     * AND WorkspaceState remains Peek (no expand()/peek()/collapse() call).
     */
    @Test
    fun pressButtonInPeek_opensCarouselWithoutStateChange() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)
        manager.peek() // force Peek (openCard() always sets Expanded)

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()

        assertEquals(WorkspaceState.Peek, manager.state.value)
        // Carousel open: card A (not the active/last-opened card) is now visible as an item
        composeTestRule.onNodeWithText("A").assertExists()
    }

    /**
     * GIVEN WorkspaceState == Expanded WHEN the trigger is tapped THEN the carousel opens
     * AND WorkspaceState remains Expanded.
     */
    @Test
    fun pressButtonInExpanded_opensCarouselWithoutStateChange() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB) // openCard() sets state = Expanded

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()

        assertEquals(WorkspaceState.Expanded, manager.state.value)
        composeTestRule.onNodeWithText("A").assertExists()
    }

    // ---- Scenario 5: Carousel renders ALL open cards with active distinguished ----

    /**
     * GIVEN [A, B, C] with activeIndex == 1 WHEN the carousel renders THEN exactly 3 items
     * exist, B is marked selected (DECISION D1 proxy — see WorkspaceCarouselItem KDoc),
     * and A/C are not.
     */
    @Test
    fun carousel_showsAllCardsWithActiveHighlighted() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val cardC = tableCard("c", "C")

        composeTestRule.setContent {
            WorkspaceCarousel(
                cards = listOf(cardA, cardB, cardC),
                activeIndex = 1,
                onSelectCard = {},
                onCloseCard = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("B").assertExists()
        composeTestRule.onNodeWithText("C").assertExists()

        composeTestRule.onNodeWithText("B").assertIsSelected()
        composeTestRule.onNodeWithText("A").assertIsNotSelected()
        composeTestRule.onNodeWithText("C").assertIsNotSelected()
    }

    // ---- Scenario 6: Tapping a non-active card activates it and dismisses ----

    /**
     * GIVEN [A, B, C] with A active WHEN the user taps C THEN `setActiveIndex(2)` is called
     * (observed via `manager.activeIndex.value`) AND the carousel dismisses.
     */
    @Test
    fun tapNonActiveCard_setsActiveAndDismisses() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val cardC = tableCard("c", "C")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)
        manager.openCard(cardC)
        manager.setActiveIndex(0) // A active

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()
        composeTestRule.onNodeWithText("C").performClick()

        assertEquals(2, manager.activeIndex.value)
        // Carousel dismissed: A (only ever rendered inside the carousel) is gone from the tree
        composeTestRule.onNodeWithText("A").assertDoesNotExist()
    }

    // ---- Scenario 7: Tapping the already-active card dismisses with no state change ----

    /**
     * GIVEN [A, B, C] with B active WHEN the user taps B (already active) THEN the carousel
     * dismisses AND `selectedCardIndex` remains 1.
     *
     * LIMITATION (flagged, not hidden): proving `setActiveIndex` is literally NOT invoked
     * (D4) would need spying `WorkspaceManager` — `mockk-android` isn't an androidTest
     * dependency and adding it is out of file-scope. Also, `setActiveIndex(1)` when already
     * 1 is observably identical to a no-op, so this asserts the externally-observable
     * contract only (index unchanged + dismissed).
     */
    @Test
    fun tapActiveCard_dismissesWithoutStateChange() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val cardC = tableCard("c", "C")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)
        manager.openCard(cardC)
        manager.setActiveIndex(1) // B active

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()
        composeTestRule.onNodeWithText("B").performClick() // tap the ALREADY active card

        assertEquals(1, manager.activeIndex.value) // unchanged
        composeTestRule.onNodeWithText("A").assertDoesNotExist() // carousel dismissed
    }

    // ---- Scenario 8/9/10: Per-item close respects existing WorkspaceManager clamping ----

    /**
     * GIVEN [A, B, C] with C (last) active WHEN close(C) is tapped THEN activeIndex clamps
     * to 1 (neighbor) AND the carousel remains open showing [A, B].
     */
    @Test
    fun closeActiveLastCard_clampsToNeighbor_carouselStaysOpen() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val cardC = tableCard("c", "C")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)
        manager.openCard(cardC)
        manager.setActiveIndex(2) // C active (last)

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()
        composeTestRule
            .onNodeWithContentDescription("Close C", useUnmergedTree = true)
            .performClick()

        assertEquals(2, manager.cards.value.size)
        assertEquals(1, manager.activeIndex.value) // clamped to neighbor
        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("B").assertExists()
    }

    /**
     * GIVEN [A, B, C] with B (not last) active WHEN close(B) is tapped THEN activeIndex
     * stays 1 (now pointing at the shifted C) AND the carousel remains open showing [A, C].
     */
    @Test
    fun closeActiveNonLastCard_indexUnchangedPointsToShiftedNeighbor() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val cardC = tableCard("c", "C")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)
        manager.openCard(cardC)
        manager.setActiveIndex(1) // B active (not last)

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()
        composeTestRule
            .onNodeWithContentDescription("Close B", useUnmergedTree = true)
            .performClick()

        assertEquals(2, manager.cards.value.size)
        assertEquals(1, manager.activeIndex.value) // unchanged, now points to shifted C
        assertEquals("C", manager.cards.value[manager.activeIndex.value].title)
        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("C").assertExists()
    }

    /**
     * GIVEN [A, B, C] with C active WHEN close(A) (non-active) is tapped THEN activeIndex
     * clamps from 2 to 1, still pointing at C AND the carousel remains open showing [B, C].
     */
    @Test
    fun closeNonActiveCard_activeCardRemainsRendered() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val cardC = tableCard("c", "C")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)
        manager.openCard(cardC)
        manager.setActiveIndex(2) // C active

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()
        composeTestRule
            .onNodeWithContentDescription("Close A", useUnmergedTree = true)
            .performClick()

        assertEquals(2, manager.cards.value.size)
        assertEquals(1, manager.activeIndex.value)
        assertEquals("C", manager.cards.value[manager.activeIndex.value].title)
        composeTestRule.onNodeWithText("B").assertExists()
        composeTestRule.onNodeWithText("C").assertExists()
    }

    // ---- Scenario 11 (critical nuance 3.11/3.12): Closing the last card ----

    /**
     * GIVEN [A, B] open, carousel open, WHEN closing A (2 -> 1) THEN the carousel MUST stay
     * open showing exactly 1 item (B) — the `totalCardCount >= 2` threshold gates ONLY the
     * TopSheetFrame trigger button, NEVER an already-open carousel (guard is
     * `activeCards.isNotEmpty()`, not `size >= 2`). WHEN then closing B (1 -> 0) THEN
     * WorkspaceState becomes Collapsed and neither sheet nor carousel render.
     */
    @Test
    fun closeLastCardFromCarousel_dismissesAndCollapses() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB) // activeIndex defaults to 1 (B, last opened)

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()

        // Close A (index 0) — 2 -> 1 remaining
        composeTestRule
            .onNodeWithContentDescription("Close A", useUnmergedTree = true)
            .performClick()

        // CRITICAL NUANCE: carousel MUST stay open with exactly 1 item (B); the trigger
        // BUTTON correctly disappears (count < 2) but the carousel itself does not.
        assertEquals(1, manager.cards.value.size)
        composeTestRule
            .onNodeWithContentDescription("Close B", useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onNodeWithContentDescription("Show all open cards")
            .assertDoesNotExist()

        // Close B (the final card) — 1 -> 0
        composeTestRule
            .onNodeWithContentDescription("Close B", useUnmergedTree = true)
            .performClick()

        assertEquals(0, manager.cards.value.size)
        assertEquals(WorkspaceState.Collapsed, manager.state.value)
        composeTestRule
            .onNodeWithContentDescription("Close B", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    // ---- Scenario 12: Dismissal without mutating activeIndex (backdrop) ----

    /**
     * GIVEN [A, B, C] with B active, carousel open WHEN the backdrop scrim is tapped THEN
     * the carousel dismisses AND `selectedCardIndex` remains 1 (no card closed).
     */
    @Test
    fun backdropTap_dismissesWithoutChangingActiveIndex() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val cardC = tableCard("c", "C")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)
        manager.openCard(cardC)
        manager.setActiveIndex(1) // B active

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()
        composeTestRule.onNodeWithText("A").assertExists() // carousel open, showing A

        // Tap the backdrop scrim in a corner away from the centered carousel item band
        composeTestRule.onRoot().performTouchInput { click(Offset(10f, 10f)) }

        assertEquals(1, manager.activeIndex.value) // unchanged
        assertEquals(3, manager.cards.value.size) // no card closed
        composeTestRule.onNodeWithText("A").assertDoesNotExist() // carousel dismissed
    }

    // ---- Scenario 13: Dismissal without mutating activeIndex (system back) ----

    /**
     * GIVEN carousel open with selectedCardIndex == 1 WHEN the system BACK button fires
     * THEN the carousel dismisses (consuming the event) AND selectedCardIndex/WorkspaceState
     * remain unchanged.
     */
    @Test
    fun systemBack_dismissesCarouselOnlyWithoutStateChange() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB) // activeIndex = 1 (B), state = Expanded

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()
        composeTestRule.onNodeWithText("A").assertExists() // carousel open

        Espresso.pressBack()
        composeTestRule.waitForIdle()

        assertEquals(1, manager.activeIndex.value)
        assertEquals(WorkspaceState.Expanded, manager.state.value) // unchanged
        composeTestRule.onNodeWithText("A").assertDoesNotExist() // carousel dismissed
    }

    // ---- Scenario 15: Tap targets >= 48dp and content descriptions present ----

    /**
     * GIVEN the carousel is open with >= 2 cards THEN the trigger button and every close
     * affordance MUST report a touch target of at least 48dp x 48dp and a non-empty
     * `contentDescription`.
     */
    @Test
    fun tapTargets_atLeast48dp_andContentDescriptionsPresent() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")
        val manager = WorkspaceManager()
        manager.openCard(cardA)
        manager.openCard(cardB)

        composeTestRule.setContent {
            WorkspaceOverlay(workspaceManager = manager, backgroundContent = {})
        }

        val triggerBounds = composeTestRule
            .onNodeWithContentDescription("Show all open cards")
            .assertExists()
            .getBoundsInRoot()
        assertTrue((triggerBounds.right - triggerBounds.left) >= 48.dp)
        assertTrue((triggerBounds.bottom - triggerBounds.top) >= 48.dp)

        composeTestRule.onNodeWithContentDescription("Show all open cards").performClick()

        val closeBounds = composeTestRule
            .onNodeWithContentDescription("Close A", useUnmergedTree = true)
            .assertExists()
            .getBoundsInRoot()
        assertTrue((closeBounds.right - closeBounds.left) >= 48.dp)
        assertTrue((closeBounds.bottom - closeBounds.top) >= 48.dp)
    }

    // ---- Scenario 14 (E2E half — Unit half lives in StringsResourceTest) ----

    /**
     * GIVEN the device/composition locale is `es` WHEN the trigger button and a per-item
     * close affordance render THEN their `contentDescription`s MUST come from the es
     * resources, NOT hardcoded strings.
     */
    @Test
    fun carouselStrings_useLocalizedResources_locale_es() {
        val cardA = tableCard("a", "A")
        val cardB = tableCard("b", "B")

        composeTestRule.setContent {
            val context = LocalContext.current
            val esConfiguration = Configuration(context.resources.configuration).apply {
                setLocale(Locale("es"))
            }
            val esContext = context.createConfigurationContext(esConfiguration)

            CompositionLocalProvider(LocalContext provides esContext) {
                Box {
                    TopSheetFrame(
                        expansionProgress = 1f,
                        isDragging = false,
                        card = cardA,
                        isExpanded = true,
                        onClose = {},
                        totalCardCount = 2,
                        onShowCarousel = {}
                    )
                    WorkspaceCarousel(
                        cards = listOf(cardA, cardB),
                        activeIndex = 0,
                        onSelectCard = {},
                        onCloseCard = {},
                        onDismiss = {}
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Ver todas las tarjetas abiertas")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription("Cerrar A", useUnmergedTree = true)
            .assertExists()
    }
}
