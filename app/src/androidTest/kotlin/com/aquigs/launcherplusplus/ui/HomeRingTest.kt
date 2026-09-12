package com.aquigs.launcherplusplus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRingTest {
    @get:Rule
    val compose = createComposeRule()

    private val noIcon: suspend (AppEntry) -> ImageBitmap? = { null }

    private fun ringOf(apps: List<AppEntry>) = Favourites(apps.map { it.key })

    private fun show(favourites: List<AppEntry>, onLaunch: (AppEntry) -> Unit = {}, onEdit: () -> Unit = {}) =
        compose.setContent {
            HomeRing(favourites = ringOf(favourites), apps = favourites, icon = noIcon, onLaunch = onLaunch, onEdit = onEdit)
        }

    private fun iconWidth(app: AppEntry): Dp = compose.onNodeWithTag(HomeRingTags.slot(app)).getUnclippedBoundsInRoot().width

    @Test
    fun anEmptyRingInvitesYouToAddApps() {
        show(emptyList())

        compose.onNodeWithTag(HomeRingTags.EMBLEM).assertIsDisplayed()
        compose.onNodeWithText("Add apps").assertIsDisplayed()
    }

    @Test
    fun noHintWhileTheFavouritesAppsAreStillLoading() {
        compose.setContent { HomeRing(favourites = ringOf(listOf(clock)), apps = emptyList(), icon = noIcon, onLaunch = {}, onEdit = {}) }

        compose.onNodeWithText("Add apps").assertDoesNotExist()
        compose.onNodeWithTag(HomeRingTags.EMBLEM).assertIsDisplayed()
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

        compose.onNodeWithTag(HomeRingTags.EMBLEM).performClick()

        assertEquals(1, edits)
    }

    @Test
    fun favouritesStartAtTheTopAndGoClockwise() {
        val four = alphabet.take(4)
        show(four)

        val (top, right, bottom, left) = four.map { compose.onNodeWithTag(HomeRingTags.slot(it)).getUnclippedBoundsInRoot() }
        assertTrue("top above the others", top.top < minOf(right.top, left.top, bottom.top))
        assertTrue("second on the right", right.left > maxOf(top.left, bottom.left, left.left))
        assertTrue("third at the bottom", bottom.top > maxOf(top.top, right.top, left.top))
        assertTrue("fourth on the left", left.left < minOf(top.left, right.left, bottom.left))
    }

    @Test
    fun iconsKeepFullSizeUpToSixThenShrink() {
        var favourites by mutableStateOf(alphabet.take(1))
        compose.setContent { HomeRing(favourites = ringOf(favourites), apps = alphabet, icon = noIcon, onLaunch = {}, onEdit = {}) }
        val alone = iconWidth(alphabet[0])

        favourites = alphabet.take(6)
        compose.waitForIdle()
        assertEquals(alone.value, iconWidth(alphabet[0]).value, 0.5f)

        favourites = alphabet.take(12)
        compose.waitForIdle()
        assertTrue("12 icons are smaller than 6", iconWidth(alphabet[0]) < alone)
    }
}
