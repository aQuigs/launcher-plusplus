package com.aquigs.launcherplusplus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppCategory
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.CollectionKind
import com.aquigs.launcherplusplus.domain.CollectionKind.MostUsed
import com.aquigs.launcherplusplus.domain.CollectionKind.NewApps
import com.aquigs.launcherplusplus.domain.CollectionsPage
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.ForegroundTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionsColumnTest {
    @get:Rule
    val compose = createComposeRule()

    private val recent = (1..12).map { n -> AppEntry("R$n", "com.example.r$n", "Main", installedAt = n.toLong()) }
    private val apps = recent + clock + mail
    private var page by mutableStateOf(CollectionsPage())
    private var foregroundTime by mutableStateOf<ForegroundTime?>(null)
    private val launched = mutableListOf<AppEntry>()
    private val edited = mutableListOf<CollectionKind>()
    private var adds = 0
    private var usageSettingsOpened = 0

    private fun show() = compose.setContent {
        CollectionsColumn(
            page = page,
            apps = apps,
            foregroundTime = foregroundTime,
            icon = { null },
            onLaunch = launched::add,
            onToggleExpanded = { page = page.toggleExpanded(it) },
            onMove = { from, to -> page = page.move(from, to) },
            onEdit = edited::add,
            onAdd = { adds++ },
            onOpenUsageSettings = { usageSettingsOpened++ },
        )
    }

    private fun topOf(node: SemanticsNodeInteraction) = node.getUnclippedBoundsInRoot().top

    @Test
    fun aFreshPageShowsNewAppsThenMostUsedThenTheButton() {
        show()

        val newApps = topOf(compose.collectionCard(NewApps).assertIsDisplayed())
        val mostUsed = topOf(compose.collectionCard(MostUsed).assertIsDisplayed())
        val button = topOf(compose.addCollectionButton().assertIsDisplayed())
        assertTrue("$newApps, $mostUsed, $button", newApps < mostUsed && mostUsed < button)
        // Compact: the newest five, no labels, and no pencil on a built-in card.
        (8..12).forEach { compose.collectionApp(NewApps, recent[it - 1]).assertIsDisplayed() }
        compose.collectionApp(NewApps, recent[6]).assertDoesNotExist()
        compose.onNodeWithText("R12").assertDoesNotExist()
        compose.collectionEditButton(NewApps).assertDoesNotExist()
        compose.onNodeWithText("Permission Required").assertIsDisplayed()
    }

    @Test
    fun aLongPressOnABuiltInCardsAppDoesNothing() {
        show()

        compose.collectionApp(NewApps, recent[11]).performTouchInput { longClick() }
        compose.waitForIdle()

        assertTrue(launched.toString(), launched.isEmpty())
    }

    @Test
    fun aCategoryCardWithNoAppsSaysHowToFillIt() {
        page = CollectionsPage().add(CollectionKind.Category(AppCategory.Kids))
        show()

        compose.onNodeWithText("Tap the pencil to add apps").assertIsDisplayed()
    }

    @Test
    fun mostUsedAsksForUsageAccessUntilGrantedThenListsTheMostUsedFirst() {
        show()

        compose.onNodeWithText("Permission Required").performClick()
        compose.onNodeWithText("Allow usage access").performClick()
        compose.runOnIdle { assertEquals(2, usageSettingsOpened) }

        foregroundTime = ForegroundTime(mapOf(mail.packageName to 5_000L, clock.packageName to 100L))

        compose.onNodeWithText("Permission Required").assertDoesNotExist()
        val mailIcon = compose.collectionApp(MostUsed, mail).assertIsDisplayed().getUnclippedBoundsInRoot()
        val clockIcon = compose.collectionApp(MostUsed, clock).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("Mail comes first", mailIcon.left < clockIcon.left)
        compose.collectionApp(MostUsed, recent[0]).assertDoesNotExist()
    }

    @Test
    fun theChevronExpandsACardToEveryAppWithLabelsAndCollapsesItAgain() {
        show()

        compose.collectionChevron(NewApps).performClick()

        compose.onNodeWithText("R12").assertIsDisplayed()
        compose.collectionApp(NewApps, recent[2]).assertIsDisplayed()
        compose.collectionApp(NewApps, recent[1]).assertDoesNotExist()
        compose.runOnIdle { assertTrue(page.card(NewApps)!!.expanded) }

        compose.collectionChevron(NewApps).performClick()

        compose.onNodeWithText("R12").assertDoesNotExist()
        compose.collectionApp(NewApps, recent[2]).assertDoesNotExist()
    }

    @Test
    fun anAppLaunchesOnATapAndACategoryOrCustomCardHasAPencil() {
        val custom = CollectionKind.Custom("Utilities")
        page = CollectionsPage(emptyList()).add(tools, Favourites(listOf(clock.key))).add(custom)
        show()

        compose.collectionApp(tools, clock).performClick()
        compose.runOnIdle { assertEquals(listOf(clock), launched) }

        compose.collectionEditButton(tools).performClick()
        compose.collectionEditButton(custom).performClick()
        compose.runOnIdle { assertEquals(listOf(tools, custom), edited) }

        compose.addCollectionButton().performClick()
        compose.runOnIdle { assertEquals(1, adds) }
    }

    @Test
    fun draggingAHandleLiftsTheCardMakesTheOthersWayAndReordersOnRelease() {
        page = CollectionsPage().add(tools)
        show()
        val newAppsTop = topOf(compose.collectionCard(NewApps))
        val toolsTop = topOf(compose.collectionCard(tools))
        val handle = compose.collectionHandle(tools).fetchSemanticsNode().boundsInRoot.center

        compose.onRoot().performTouchInput {
            down(handle)
            moveBy(Offset(0f, -viewConfiguration.touchSlop * 2))
            // Past New Apps' middle: to its top and a little further, since the two cards are the same height.
            repeat(10) { moveBy(Offset(0f, -(toolsTop - newAppsTop + 20.dp).toPx() / 10)) }
        }
        compose.waitForIdle()

        assertTrue("Tools lifted", topOf(compose.collectionCard(tools)) < toolsTop - 10.dp)
        assertTrue("New Apps made way", topOf(compose.collectionCard(NewApps)) > newAppsTop + 10.dp)
        compose.runOnIdle { assertEquals(listOf(NewApps, MostUsed, tools), page.cards.map { it.kind }) }

        compose.onRoot().performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(tools, NewApps, MostUsed), page.cards.map { it.kind }) }
        assertTrue("Tools now on top", topOf(compose.collectionCard(tools)) < topOf(compose.collectionCard(NewApps)))
    }
}
