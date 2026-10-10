package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.ringLayout
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRingTest {
    @get:Rule
    val compose = createComposeRule()

    private var ring by mutableStateOf(emptyList<RingItem>())
    private var unread by mutableStateOf(UnreadCounts())
    private var openFolder by mutableStateOf<RingItem.Folder?>(null)
    private var theme by mutableStateOf(Theme.Space)
    private var folderLook by mutableStateOf(FolderLook.Rim)
    private var shownHint by mutableStateOf<String?>(null)
    private var names by mutableStateOf(false)
    private var direction by mutableStateOf(LayoutDirection.Ltr)

    private fun show(
        favourites: List<AppEntry>,
        hint: String? = null,
        onLaunch: (AppEntry) -> Unit = {},
        onEdit: () -> Unit = {},
        onSearch: () -> Unit = {},
    ) = showItems(favourites.asRingItems(), hint, onLaunch, onEdit, onSearch)

    private fun showItems(
        items: List<RingItem>,
        hint: String? = null,
        onLaunch: (AppEntry) -> Unit = {},
        onEdit: () -> Unit = {},
        onSearch: () -> Unit = {},
        onOpenFolder: (RingItem.Folder) -> Unit = {},
        onCloseFolder: () -> Unit = {},
        onClearBadge: (AppEntry) -> Unit = {},
    ) {
        ring = items
        shownHint = hint
        compose.setContent {
            Themed(theme, theme.colourways.first(), lightWallpaper = false) {
                CompositionLocalProvider(LocalFolderStyle provides FolderStyle(folderLook), LocalLayoutDirection provides direction) {
                    val spin = rememberRingSpin()
                    Box(Modifier.spinsRing(spin)) {
                        HomeRing(
                            ring = ring,
                            hint = shownHint,
                            icon = { null },
                            onLaunch = onLaunch,
                            onOpenFolder = onOpenFolder,
                            onCloseFolder = onCloseFolder,
                            onEdit = onEdit,
                            onSearch = onSearch,
                            openFolder = openFolder,
                            unread = unread,
                            onClearBadge = onClearBadge,
                            names = names,
                            spin = spin,
                        )
                    }
                }
            }
        }
    }

    private val DpRect.middle get() = Offset((left + right).value / 2, (top + bottom).value / 2)

    private fun iconWidth(app: AppEntry) = compose.ringSlot(app).getUnclippedBoundsInRoot().width

    @Test
    fun theHintTakesTheMarksPlace() {
        show(emptyList(), hint = "Add apps")

        compose.emblem().assertIsDisplayed()
        compose.onNodeWithText("Add apps").assertIsDisplayed()
    }

    @Test
    fun tappingAFavouriteLaunchesIt() {
        val launched = mutableListOf<AppEntry>()
        show(listOf(clock, mail), onLaunch = launched::add)

        compose.onNodeWithContentDescription("Clock").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mail").performClick()

        assertEquals(listOf(mail), launched)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun doubleTappingAnAppWithDismissedNotificationsClearsItsBadgeWithoutLaunchingIt() {
        val launched = mutableListOf<AppEntry>()
        val cleared = mutableListOf<AppEntry>()
        unread = UnreadCounts(mapOf(mail.packageName to 2, clock.packageName to 1), dismissed = setOf(mail.packageName))
        showItems(listOf(clock, mail).asRingItems(), onLaunch = launched::add, onClearBadge = cleared::add)

        compose.ringSlot(mail).performTouchInput { doubleClick() }
        compose.ringSlot(clock).performTouchInput { doubleClick() }
        compose.ringSlot(mail).performCustomAccessibilityActionWithLabel("Clear badge")

        assertEquals(listOf(mail, mail), cleared)
        assertEquals(listOf(clock, clock), launched)
    }

    @Test
    fun tappingTheEmblemSearchesAndALongPressEditsTheRingButAHintTapEdits() {
        var edits = 0
        var searches = 0
        show(listOf(clock), onEdit = { edits++ }, onSearch = { searches++ })

        compose.emblem().assertContentDescriptionEquals("Favourites").performClick()
        assertEquals(1 to 0, searches to edits)
        compose.emblem().performTouchInput { longClick() }
        assertEquals(1 to 1, searches to edits)

        shownHint = "Add apps"
        compose.emblem().performClick()
        assertEquals(1 to 2, searches to edits)
    }

    @Test
    fun favouritesStartAtTheTopAndGoClockwise() {
        val four = alphabet.take(4)
        show(four)

        val (top, right, bottom, left) = four.map { compose.ringSlot(it).getUnclippedBoundsInRoot() }
        assertTrue("top above the others", top.top < minOf(right.top, left.top, bottom.top))
        assertTrue("second on the right", right.left > maxOf(top.left, bottom.left, left.left))
        assertTrue("third at the bottom", bottom.top > maxOf(top.top, right.top, left.top))
        assertTrue("fourth on the left", left.left < minOf(top.left, right.left, bottom.left))
    }

    @Test
    fun iconsTakeTheSizeAndRadiusOfTheRingLayout() {
        show(alphabet.take(1))
        val page = compose.onRoot().getUnclippedBoundsInRoot()
        val side = min(page.width.value, page.height.value)

        listOf(1, 11, 14).forEach { count ->
            ring = alphabet.take(count).asRingItems()
            compose.waitForIdle()

            val expected = ringLayout(RING_ICON_SIZE.value, side, count, RING_EDGE_MARGIN.value)
            val top = compose.ringSlot(alphabet[0]).getUnclippedBoundsInRoot()
            assertEquals("the size of $count icons", expected.iconSize, top.width.value, 1f)
            assertEquals("the radius of $count icons", expected.radius, (page.top + page.bottom - top.top - top.bottom).value / 2, 1f)
        }
    }

    @Test
    fun anAppsNameHangsCentredUnderItsIconEitherWayRound() {
        names = true
        show(listOf(clock, mail))

        LayoutDirection.entries.forEach { way ->
            direction = way
            compose.waitForIdle()

            listOf(clock, mail).forEach { app ->
                val icon = compose.ringSlot(app).getUnclippedBoundsInRoot()
                val name = compose.onNode(hasText(app.label) and hasAnyAncestor(hasTestTag(HomeRingTags.slot(app))), useUnmergedTree = true)
                    .getUnclippedBoundsInRoot()
                assertEquals("$way: ${app.label} centred", icon.middle.x, name.middle.x, 1f)
                assertTrue("$way: ${app.label} under its icon", name.top >= icon.bottom - 0.5.dp)
            }
        }
    }

    @Test
    fun aFolderSlotIsTheSizeOfAnAppSlotAndOpensOnATap() {
        val opened = mutableListOf<RingItem.Folder>()
        val work = RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(mail, clock, alphabet[0]))
        showItems(listOf(RingItem.App(clock), work), onOpenFolder = opened::add)

        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 3 apps").assertWidthIsEqualTo(iconWidth(clock))
        compose.folderSlot(1).performClick()

        assertEquals(listOf(work), opened)
    }

    @Test
    fun unreadCountsBadgeAppsAddUpOnFoldersAndReachAnOpenFoldersApps() {
        val work = RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(mail, alphabet[0]))
        unread = UnreadCounts(mapOf(clock.packageName to 3, mail.packageName to 98, alphabet[0].packageName to 4))
        showItems(listOf(RingItem.App(clock), work, RingItem.App(alphabet[1])))

        compose.ringSlot(clock).assertContentDescriptionEquals("Clock, 3 unread")
        compose.badgeOn(HomeRingTags.slot(clock)).assertIsDisplayed().assertTextEquals("3")
        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 2 apps, 102 unread")
        compose.badgeOn(HomeRingTags.folder(1)).assertTextEquals("99+")
        compose.ringSlot(alphabet[1]).assertContentDescriptionEquals(alphabet[1].label)
        compose.badgeOn(HomeRingTags.slot(alphabet[1])).assertDoesNotExist()

        openFolder = work

        compose.ringSlot(mail).assertContentDescriptionEquals("Mail, 98 unread")
        compose.badgeOn(HomeRingTags.slot(mail)).assertIsDisplayed().assertTextEquals("98")

        openFolder = null
        unread = UnreadCounts(mapOf(clock.packageName to 0))

        compose.ringSlot(clock).assertContentDescriptionEquals("Clock")
        compose.badgeOn(HomeRingTags.slot(clock)).assertDoesNotExist()
        compose.badgeOn(HomeRingTags.folder(1)).assertDoesNotExist()
    }

    @Test
    fun everyThemeDrawsTheRingItsEmblemWithAndWithoutAHintAndEachOfItsFolderLooksOpenAndClosed() {
        val work = RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(mail, alphabet[0]))
        showItems(listOf(RingItem.App(clock), work, RingItem.App(alphabet[1])), hint = "Add apps")

        Theme.entries.forEach { shown ->
            shown.folderLooks.forEach { look ->
                theme = shown
                folderLook = look
                shownHint = "Add apps"

                compose.onNodeWithText("Add apps").assertIsDisplayed()
                shownHint = null
                compose.emblem().assertIsDisplayed()
                compose.folderSlot(1).assertContentDescriptionEquals("Folder, 2 apps")
                openFolder = work
                compose.ringSlot(mail).assertIsDisplayed()
                compose.onNodeWithContentDescription("Close folder").assertIsDisplayed()
                openFolder = null
                compose.folderSlot(1).assertIsDisplayed()
            }
        }
    }

    @Test
    fun anEmptyFolderIsABadgeThatDoesNotOpen() {
        val opened = mutableListOf<RingItem.Folder>()
        showItems(listOf(RingItem.App(clock), RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), emptyList())), onOpenFolder = opened::add)

        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 0 apps").assertWidthIsEqualTo(iconWidth(clock))
        compose.folderSlot(1).performClick()

        assertEquals(emptyList<RingItem.Folder>(), opened)
    }

    @Test
    fun anOpenFolderPutsItsAppsInTheRingSlotsInPlaceOfTheEmblem() {
        var closes = 0
        val work = RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(mail, alphabet[0]))
        showItems(listOf(RingItem.App(clock), work), onCloseFolder = { closes++ })
        val emblem = compose.emblem().getUnclippedBoundsInRoot()
        val iconWidth = iconWidth(clock)

        openFolder = work

        compose.emblem().assertDoesNotExist()
        compose.ringSlot(clock).assertDoesNotExist()
        compose.folderSlot(1).assertDoesNotExist()
        // Side by side, as the closed folder's previews show them.
        val first = compose.ringSlot(mail).getUnclippedBoundsInRoot()
        val second = compose.ringSlot(alphabet[0]).getUnclippedBoundsInRoot()
        assertTrue("the first app sits left of the centre", first.right < emblem.left)
        assertTrue("the second app sits right of the centre", second.left > emblem.right)
        assertEquals("level with each other", first.top, second.top)
        assertEquals("each the size of a ring slot", iconWidth, first.width)
        assertEquals("the close target is the emblem's size", emblem, compose.closeFolder().getUnclippedBoundsInRoot())

        compose.closeFolder().performClick()

        assertEquals(1, closes)
    }

    @Test
    fun aFolderSpreadsOutOfItsPlanetAndFoldsBackIntoIt() {
        val launched = mutableListOf<AppEntry>()
        // Stored in slot 2, but second on the ring, as an app missing from the ring before it leaves it.
        val work = RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 2), listOf(mail, alphabet[0]))
        showItems(listOf(RingItem.App(clock), work), onLaunch = launched::add)
        val planet = compose.folderSlot(2).getUnclippedBoundsInRoot().middle
        val emblem = compose.emblem().getUnclippedBoundsInRoot()
        compose.mainClock.autoAdvance = false

        openFolder = work
        compose.mainClock.advanceTimeByFrame()

        val leaving = compose.ringSlot(mail).getUnclippedBoundsInRoot().middle
        compose.onRoot().performTouchInput { click(Offset(planet.x.dp.toPx(), planet.y.dp.toPx())) }
        assertEquals("a second tap on the planet launches nothing unseen", emptyList<AppEntry>(), launched)
        compose.mainClock.advanceTimeBy(1_000)
        val slot = compose.ringSlot(mail).getUnclippedBoundsInRoot()
        assertTrue("the first app starts at its planet", (leaving - planet).getDistance() < (leaving - slot.middle).getDistance())
        assertTrue("then reaches its slot", slot.right < emblem.left)

        openFolder = null
        compose.mainClock.advanceTimeByFrame()

        compose.folderSlot(2).assertExists()
        compose.onNodeWithContentDescription(mail.label).assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }

    /** Presses the ring, at rest, by its top and flings it clockwise round, or only swipes across it unless [held]. */
    private fun fling(held: Boolean = true) {
        val rest = topNow()
        val ring = compose.onRoot().getUnclippedBoundsInRoot().middle
        val reach = ring.y - rest.y
        val from = px(Offset(ring.x - reach / 2, rest.y))
        val to = px(Offset(ring.x + reach / 2, rest.y))
        compose.onRoot().performTouchInput {
            down(from)
            if (held) advanceEventTime(250)
            for (step in 1..8) moveTo(from + (to - from) * (step / 8f), delayMillis = 10)
            up()
        }
        compose.mainClock.advanceTimeByFrame()
    }

    /** How far, in dp, the next frame moves the top app. */
    private fun stepFrame(): Float {
        val before = topNow()
        compose.mainClock.advanceTimeByFrame()
        return (topNow() - before).getDistance()
    }

    private fun topNow() = compose.ringSlot(alphabet[0]).getUnclippedBoundsInRoot().middle

    private fun px(at: Offset) = at * compose.density.density

    private val pixel get() = 1 / compose.density.density

    @Test
    fun aPressAndDragRoundTheRingSpinsItOnAndItSettlesWhereItWasButASwipeDoesNot() {
        val launched = mutableListOf<AppEntry>()
        show(alphabet.take(4), onLaunch = launched::add)
        compose.mainClock.autoAdvance = false
        val rest = topNow()
        val ring = compose.onRoot().getUnclippedBoundsInRoot().middle
        compose.onRoot().performTouchInput { swipe(px(Offset(ring.x, rest.y - 10f)), px(Offset(ring.x, rest.y - 160f)), 80) }
        compose.mainClock.advanceTimeBy(300)
        assertEquals("a drag outward leaves it", rest.x, topNow().x, 0.5f)
        fling(held = false)
        compose.mainClock.advanceTimeBy(300)
        assertEquals("so does a swipe across it with no press first", rest.x, topNow().x, 0.5f)

        fling()
        val letGo = topNow()
        compose.mainClock.advanceTimeBy(300)
        assertTrue("turned clockwise with the finger", letGo.x > rest.x + 20f)
        assertTrue("still turning after the finger left", (topNow() - letGo).getDistance() > 10f)

        // It eases all the way in: once it creeps, no frame moves it more than the pixel it is rounded to.
        var crept = false
        repeat(15_000 / 16) {
            val step = stepFrame()
            if (crept) assertTrue("a jump of ${step / pixel} px at rest", step < 1.5f * pixel)
            if (step < pixel / 2) crept = true
        }
        assertEquals(emptyList<AppEntry>(), launched)
        assertEquals("back where it was", rest.x, topNow().x, 0.5f)
        assertEquals(rest.y, topNow().y, 0.5f)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aTapCatchesASpinningRingWhichStopsUnderTheFingerAndSwingsStraightHome() {
        val launched = mutableListOf<AppEntry>()
        show(alphabet.take(4), onLaunch = launched::add)
        compose.mainClock.autoAdvance = false
        val rest = topNow()
        fling()
        compose.mainClock.advanceTimeBy(300)

        val second = compose.ringSlot(alphabet[1])
        second.performTouchInput { down(center) }
        val caught = topNow()
        compose.mainClock.advanceTimeBy(300)
        assertEquals("stopped under the finger", caught.x, topNow().x, 0.5f)
        assertEquals(caught.y, topNow().y, 0.5f)
        second.performTouchInput { up() }

        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(emptyList<AppEntry>(), launched)
        assertEquals("home within a second", rest.x, topNow().x, 0.5f)
        assertEquals(rest.y, topNow().y, 0.5f)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aTapOnARingCreepingTheLastBitHomeOpensWhatItLandsOn() {
        val launched = mutableListOf<AppEntry>()
        show(alphabet.take(4), onLaunch = launched::add)
        compose.mainClock.autoAdvance = false
        fling()

        var still = 0
        var frames = 0
        while (still < 10) {
            still = if (stepFrame() < pixel / 2) still + 1 else 0
            assertTrue("never creeps", ++frames < 15_000 / 16)
        }
        compose.ringSlot(alphabet[1]).performTouchInput { click() }
        compose.mainClock.advanceTimeByFrame()

        assertEquals(listOf(alphabet[1]), launched)
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aFolderThatGoesWhileOpenClosesAtOnce() {
        val work = RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(mail))
        openFolder = work
        showItems(listOf(RingItem.App(clock), work))
        compose.mainClock.autoAdvance = false

        openFolder = null
        ring = listOf(RingItem.App(clock))
        compose.mainClock.advanceTimeByFrame()

        compose.ringSlot(mail).assertDoesNotExist()
        compose.onNodeWithContentDescription(mail.label).assertDoesNotExist()
        compose.ringSlot(clock).assertIsDisplayed()
        compose.emblem().assertIsDisplayed()
    }
}
