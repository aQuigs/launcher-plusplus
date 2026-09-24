package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.OTHER_INITIAL
import com.sqftware.orbitlauncher.domain.UnreadCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDrawerTest {
    @get:Rule
    val compose = createComposeRule()

    private val listState = LazyListState()

    // Each letter of the alphabet fixture is one header row followed by its apps.
    private val itemsPerLetter = APPS_PER_LETTER + 1

    private fun show(
        apps: List<AppEntry>,
        onLaunch: (AppEntry) -> Unit = {},
        picking: Picking? = null,
        unread: UnreadCounts = UnreadCounts(),
    ) = compose.setContent {
        var query by remember { mutableStateOf("") }
        AppDrawer(apps, icon = { null }, onLaunch, picking = picking, listState = listState, query = query, onQueryChange = { query = it }, unread = unread)
    }

    @Test
    fun filesEveryAppUnderItsInitialWithTheRailToMatch() {
        val zip7 = AppEntry("7zip", "com.example.zip", "Main")
        show(listOf(zip7, clock, mail))

        compose.appList().assertIsDisplayed()
        listOf("7zip", "Clock", "Mail").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        listOf(OTHER_INITIAL, 'C', 'M').forEach {
            compose.sectionHeader(it).assertIsDisplayed()
            compose.railLetter(it).assertIsDisplayed()
        }
    }

    @Test
    fun anAppWithUnreadNotificationsEndsItsRowWithTheirCount() {
        show(listOf(clock, mail), unread = UnreadCounts(mapOf(mail.packageName to 250, clock.packageName to 0)))

        compose.onNodeWithText("Mail").assert(hasText("250 unread"))
        compose.onNodeWithText("Clock").assert(hasText("0 unread").not())
    }

    @Test
    fun appsThatShareANameShowTheirPackages() {
        val google = AppEntry("Authenticator", "com.google.authenticator", "Main")
        val microsoft = AppEntry("Authenticator", "com.azure.authenticator", "Main")
        show(listOf(google, microsoft, clock))

        compose.onNodeWithText(google.packageName).assertIsDisplayed()
        compose.onNodeWithText(microsoft.packageName).assertIsDisplayed()
        compose.onNodeWithText("Clock").assert(hasText(clock.packageName).not())
    }

    @Test
    fun tappingAnAppLaunchesIt() {
        val launched = mutableListOf<AppEntry>()
        show(listOf(clock, mail), onLaunch = launched::add)

        compose.onNodeWithText("Mail").performClick()

        assertEquals(listOf(mail), launched)
    }

    @Test
    fun pickingTogglesAppsInsteadOfLaunchingThem() {
        val launched = mutableListOf<AppEntry>()
        val toggled = mutableListOf<AppEntry>()
        val picking = Picking(header = { Text("Pick apps") }, isPicked = { it == mail }, onToggle = toggled::add)
        show(listOf(clock, mail), onLaunch = launched::add, picking = picking)

        compose.onNodeWithText("Pick apps").assertIsDisplayed()
        compose.onNodeWithText("Mail").assertIsOn()
        compose.onNodeWithText("Clock").assertIsOff()
        compose.onNodeWithText("Clock").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))

        compose.onNodeWithText("Clock").performClick()

        assertEquals(listOf(clock), toggled)
        assertEquals(emptyList<AppEntry>(), launched)
    }

    @Test
    fun typingFiltersTheListWithoutSectionsOrRail() {
        show(alphabet)

        compose.searchField().performTextInput("b2")

        compose.onNodeWithText("B2").assertIsDisplayed()
        compose.onNodeWithText("A1").assertDoesNotExist()
        compose.sectionHeader('B').assertDoesNotExist()
        compose.railLetter('B').assertDoesNotExist()
    }

    @Test
    fun clearingTheSearchBringsTheSectionsBackWhereTheyWere() {
        show(alphabet)
        compose.railLetter('P').performClick()
        compose.sectionHeader('P').assertIsDisplayed()
        compose.searchField().performTextInput("b2")
        compose.onNodeWithText("P1").assertDoesNotExist()

        compose.onNodeWithContentDescription("Clear the search").performClick()

        compose.sectionHeader('P').assertIsDisplayed()
        compose.onNodeWithText("B2").assertDoesNotExist()
    }

    @Test
    fun eachNewSearchStartsAtTheTopOfItsMatches() {
        show(alphabet)
        compose.searchField().performTextInput("1")
        compose.appList().performScrollToNode(hasText("Z1"))
        compose.onNodeWithText("A1").assertIsNotDisplayed()

        compose.onNodeWithContentDescription("Clear the search").performClick()
        compose.searchField().performTextInput("2")

        compose.onNodeWithText("A2").assertIsDisplayed()
    }

    @Test
    fun theSearchKeyLaunchesTheFirstMatch() {
        val launched = mutableListOf<AppEntry>()
        show(listOf(clock, mail), onLaunch = launched::add)

        compose.searchField().performTextInput("cl")
        compose.searchField().performImeAction()

        assertEquals(listOf(clock), launched)
    }

    @Test
    fun theSearchKeyTogglesTheFirstMatchWhilePicking() {
        val toggled = mutableListOf<AppEntry>()
        show(listOf(clock, mail), picking = Picking(header = {}, isPicked = { false }, onToggle = toggled::add))

        compose.searchField().performTextInput("ma")
        compose.searchField().performImeAction()

        assertEquals(listOf(mail), toggled)
    }

    @Test
    fun noMatchSaysSo() {
        show(listOf(clock, mail))

        compose.searchField().performTextInput("zz")

        compose.onNodeWithText("No apps match").assertIsDisplayed()
    }

    @Test
    fun tappingARailLetterJumpsToItsSection() {
        show(alphabet)
        compose.onNodeWithText("T1").assertIsNotDisplayed()

        compose.railLetter('T').performTouchInput { click() }

        compose.onNodeWithText("T1").assertIsDisplayed()
        compose.onNodeWithText("A1").assertIsNotDisplayed()
        compose.railLetter('T').assertIsSelected()
        compose.runOnIdle { assertEquals(('T' - 'A') * itemsPerLetter, listState.firstVisibleItemIndex) }
    }

    @Test
    fun aLetterNearTheEndStaysHighlightedThoughTheListStopsShort() {
        show(alphabet)

        compose.railLetter('Z').performTouchInput { click() }

        compose.onNodeWithText("Z1").assertIsDisplayed()
        compose.railLetter('Z').assertIsSelected()
    }

    @Test
    fun noAppsMeansNoRail() {
        show(emptyList())

        compose.appList().assertIsDisplayed()
        compose.railLetter('A').assertDoesNotExist()
    }

    @Test
    fun slidingAlongTheRailKeepsJumping() {
        show(alphabet)

        // One finger from A down to M without lifting: the list should follow it, not just the first touch.
        val a = compose.railLetter('A').fetchSemanticsNode().boundsInRoot.center
        val m = compose.railLetter('M').fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput { swipe(start = a, end = m) }

        compose.runOnIdle {
            assertTrue("scrolled to item ${listState.firstVisibleItemIndex}", listState.firstVisibleItemIndex >= 10 * itemsPerLetter)
        }
        compose.onNodeWithText("A1").assertIsNotDisplayed()
        compose.railLetter('M').assertIsSelected()
    }
}
