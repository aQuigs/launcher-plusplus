package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.ringLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class HomeRingTest {
    @get:Rule
    val compose = createComposeRule()

    private var ring by mutableStateOf(emptyList<RingItem>())
    private var unread by mutableStateOf(UnreadCounts())
    private var openFolder by mutableStateOf<RingItem.Folder?>(null)

    private fun show(
        favourites: List<AppEntry>,
        showHint: Boolean = false,
        onLaunch: (AppEntry) -> Unit = {},
        onEdit: () -> Unit = {},
    ) = showItems(favourites.asRingItems(), showHint, onLaunch, onEdit)

    private fun showItems(
        items: List<RingItem>,
        showHint: Boolean = false,
        onLaunch: (AppEntry) -> Unit = {},
        onEdit: () -> Unit = {},
        onOpenFolder: (RingItem.Folder) -> Unit = {},
        onCloseFolder: () -> Unit = {},
    ) {
        ring = items
        compose.setContent {
            HomeRing(
                ring = ring,
                showHint = showHint,
                icon = { null },
                onLaunch = onLaunch,
                onOpenFolder = onOpenFolder,
                onCloseFolder = onCloseFolder,
                onEdit = onEdit,
                openFolder = openFolder,
                unread = unread,
            )
        }
    }

    private fun iconWidth(app: AppEntry) = compose.ringSlot(app).getUnclippedBoundsInRoot().width

    @Test
    fun theHintInvitesYouToAddApps() {
        show(emptyList(), showHint = true)

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

    @Test
    fun tappingTheEmblemEditsTheRing() {
        var edits = 0
        show(listOf(clock), onEdit = { edits++ })

        compose.emblem().assertContentDescriptionEquals("Favourites").performClick()

        assertEquals(1, edits)
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

        listOf(1, 8, 12).forEach { count ->
            ring = alphabet.take(count).asRingItems()
            compose.waitForIdle()

            val expected = ringLayout(RING_ICON_SIZE.value, side, count, RING_EDGE_MARGIN.value)
            val top = compose.ringSlot(alphabet[0]).getUnclippedBoundsInRoot()
            assertEquals("the size of $count icons", expected.iconSize, top.width.value, 1f)
            assertEquals("the radius of $count icons", expected.radius, (page.top + page.bottom - top.top - top.bottom).value / 2, 1f)
        }
    }

    @Test
    fun aFolderSlotIsTheSizeOfAnAppSlotAndOpensOnATap() {
        val opened = mutableListOf<RingItem.Folder>()
        val work = RingItem.Folder(1, listOf(mail, clock, alphabet[0]))
        showItems(listOf(RingItem.App(clock), work), onOpenFolder = opened::add)

        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 3 apps").assertWidthIsEqualTo(iconWidth(clock))
        compose.folderSlot(1).performClick()

        assertEquals(listOf(work), opened)
    }

    @Test
    fun unreadCountsBadgeAppsAddUpOnFoldersAndReachAnOpenFoldersApps() {
        val work = RingItem.Folder(1, listOf(mail, alphabet[0]))
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
    fun anEmptyFolderIsABadgeThatDoesNotOpen() {
        val opened = mutableListOf<RingItem.Folder>()
        showItems(listOf(RingItem.App(clock), RingItem.Folder(1, emptyList())), onOpenFolder = opened::add)

        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 0 apps").assertWidthIsEqualTo(iconWidth(clock))
        compose.folderSlot(1).performClick()

        assertEquals(emptyList<RingItem.Folder>(), opened)
    }

    @Test
    fun anOpenFolderPutsItsAppsInTheRingSlotsInPlaceOfTheEmblem() {
        var closes = 0
        val work = RingItem.Folder(1, listOf(mail, alphabet[0]))
        showItems(listOf(RingItem.App(clock), work), onCloseFolder = { closes++ })
        val emblem = compose.emblem().getUnclippedBoundsInRoot()
        val slotOfClock = compose.ringSlot(clock).getUnclippedBoundsInRoot()

        openFolder = work

        compose.emblem().assertDoesNotExist()
        compose.ringSlot(clock).assertDoesNotExist()
        compose.folderSlot(1).assertDoesNotExist()
        assertEquals("the first app takes the top slot", slotOfClock, compose.ringSlot(mail).getUnclippedBoundsInRoot())
        val second = compose.ringSlot(alphabet[0]).getUnclippedBoundsInRoot()
        assertTrue("the second app sits at the bottom", second.top > emblem.bottom)
        assertEquals("the close target is the emblem's size", emblem, compose.closeFolder().getUnclippedBoundsInRoot())

        compose.closeFolder().performClick()

        assertEquals(1, closes)
    }
}
