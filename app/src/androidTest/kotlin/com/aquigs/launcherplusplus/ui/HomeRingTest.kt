package com.aquigs.launcherplusplus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.RingItem
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

        compose.emblem().performClick()

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
    fun iconsKeepFullSizeUpToSixThenShrink() {
        show(alphabet.take(1))
        val alone = iconWidth(alphabet[0])

        ring = alphabet.take(6).asRingItems()
        compose.waitForIdle()
        compose.ringSlot(alphabet[0]).assertWidthIsEqualTo(alone)

        ring = alphabet.take(12).asRingItems()
        compose.waitForIdle()
        assertTrue("12 icons are smaller than 6", iconWidth(alphabet[0]) < alone)
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
