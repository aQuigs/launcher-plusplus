package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
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
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import com.sqftware.orbitlauncher.domain.DrawerLayout
import com.sqftware.orbitlauncher.domain.DrawerOrder
import com.sqftware.orbitlauncher.domain.DrawerStyle
import com.sqftware.orbitlauncher.domain.ForegroundTime
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

    private val gridState = LazyGridState()
    private var style by mutableStateOf(DrawerStyle())

    // Each letter of the alphabet fixture is one header row followed by its apps.
    private val itemsPerLetter = APPS_PER_LETTER + 1

    private fun show(
        apps: List<AppEntry>,
        onLaunch: (AppEntry) -> Unit = {},
        picking: Picking? = null,
        unread: UnreadCounts = UnreadCounts(),
        style: DrawerStyle = DrawerStyle(),
        onStyleChange: (DrawerStyle) -> Unit = { this.style = it },
        foregroundTime: ForegroundTime? = null,
        onOpenUsageSettings: () -> Unit = {},
    ) {
        this.style = style
        compose.setContent {
            var query by remember { mutableStateOf("") }
            var sorting by remember { mutableStateOf(false) }
            AppDrawer(
                apps,
                icon = { null },
                onLaunch,
                picking = picking,
                gridState = gridState,
                query = query,
                onQueryChange = { query = it },
                unread = unread,
                controls = DrawerControls(this.style, onStyleChange, sorting, onSortingChange = { sorting = it }),
                foregroundTime = foregroundTime,
                onOpenUsageSettings = onOpenUsageSettings,
            )
        }
    }

    private fun top(label: String) = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.top

    @Test
    fun theGridSetsAppsSideBySideWithoutHeadersAndTheRailFindsTheirFirstApp() {
        val launched = mutableListOf<AppEntry>()
        show(alphabet, onLaunch = launched::add, style = DrawerStyle(layout = DrawerLayout.Grid))

        assertEquals(top("A1"), top("A3"))
        assertTrue(top("B2") > top("A1"))
        compose.sectionHeader('A').assertDoesNotExist()

        compose.railLetter('H').performClick()

        // A grid scrolls by rows, so H1's row, which starts with G3, comes to the top.
        compose.runOnIdle { assertEquals(('G' - 'A') * APPS_PER_LETTER + 2, gridState.firstVisibleItemIndex) }
        compose.onNodeWithText("H1").performClick()
        assertEquals("H1", launched.single().label)
    }

    @Test
    fun oneButtonTogglesBetweenTheListAndTheGrid() {
        show(listOf(clock, mail))

        compose.onNodeWithContentDescription("Switch to list view").assertDoesNotExist()
        compose.onNodeWithContentDescription("Switch to grid view").performClick()
        compose.runOnIdle { assertEquals(DrawerLayout.Grid, style.layout) }

        compose.onNodeWithContentDescription("Switch to grid view").assertDoesNotExist()
        compose.onNodeWithContentDescription("Switch to list view").performClick()
        compose.runOnIdle { assertEquals(DrawerLayout.List, style.layout) }
    }

    @Test
    fun theSortMenuSetsTheOrderAndTheMostUsedRow() {
        val changes = mutableListOf<DrawerStyle>()
        show(listOf(clock, mail), onStyleChange = changes::add)

        compose.onNodeWithContentDescription("Sort apps").performClick()
        compose.onNodeWithText("A to Z").assertIsSelected()
        compose.onNodeWithText("Date added").performClick()
        compose.onNodeWithTag(AppDrawerTags.SORT_MENU).assertDoesNotExist()
        compose.onNodeWithContentDescription("Sort apps").performClick()
        compose.onNodeWithText("Most used row").assertIsOff().performClick()

        assertEquals(
            listOf(
                DrawerStyle(order = DrawerOrder.Newest),
                DrawerStyle(mostUsedRow = true),
            ),
            changes,
        )
    }

    @Test
    fun anotherOrderListsTheAppsInOneRunWithoutSectionsOrRail() {
        val old = AppEntry("Aardvark", "com.example.old", "Main", installedAt = 1)
        val new = AppEntry("Zebra", "com.example.new", "Main", installedAt = 2)
        show(listOf(old, new), style = DrawerStyle(order = DrawerOrder.Newest))

        assertTrue(top("Zebra") < top("Aardvark"))
        compose.sectionHeader('A').assertDoesNotExist()
        compose.railLetter('A').assertDoesNotExist()
    }

    @Test
    fun theMostUsedRowHeadsTheListAndTheRailStillFindsItsSections() {
        val time = ForegroundTime(mapOf("com.example.b2" to 20L, "com.example.t1" to 10L))
        show(alphabet, style = DrawerStyle(mostUsedRow = true), foregroundTime = time)

        val inRow = hasAnyAncestor(hasTestTag(AppDrawerTags.MOST_USED))
        val b2 = compose.onNode(hasText("B2") and inRow).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val t1 = compose.onNode(hasText("T1") and inRow).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(b2.left < t1.left)
        assertTrue(t1.bottom <= compose.sectionHeader('A').fetchSemanticsNode().boundsInRoot.top)

        compose.railLetter('T').performClick()

        compose.runOnIdle { assertEquals(1 + ('T' - 'A') * itemsPerLetter, gridState.firstVisibleItemIndex) }
    }

    @Test
    fun aNewStyleStartsTheListAtItsTopWithTheNewRowInSight() {
        show(alphabet, foregroundTime = ForegroundTime(mapOf("com.example.m1" to 5L)))
        compose.onNodeWithText("A1").assertIsDisplayed()

        compose.runOnIdle { style = style.copy(mostUsedRow = true) }

        compose.onNodeWithTag(AppDrawerTags.MOST_USED).assertIsDisplayed()
        compose.railLetter('T').performClick()
        compose.runOnIdle { style = style.copy(order = DrawerOrder.Newest) }
        compose.runOnIdle { assertEquals(0, gridState.firstVisibleItemIndex) }
    }

    @Test
    fun withoutUsageAccessTheDrawerAsksForIt() {
        var opened = 0
        show(listOf(clock, mail), style = DrawerStyle(mostUsedRow = true), onOpenUsageSettings = { opened++ })

        compose.onNodeWithTag(AppDrawerTags.MOST_USED).assertDoesNotExist()
        compose.onNodeWithTag(AppDrawerTags.USAGE_ACCESS).performClick()

        assertEquals(1, opened)
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
        compose.runOnIdle { assertEquals(('T' - 'A') * itemsPerLetter, gridState.firstVisibleItemIndex) }
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
            assertTrue("scrolled to item ${gridState.firstVisibleItemIndex}", gridState.firstVisibleItemIndex >= 10 * itemsPerLetter)
        }
        compose.onNodeWithText("A1").assertIsNotDisplayed()
        compose.railLetter('M').assertIsSelected()
    }
}
