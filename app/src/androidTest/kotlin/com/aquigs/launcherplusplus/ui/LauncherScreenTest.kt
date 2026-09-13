package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.height
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppShortcut
import com.aquigs.launcherplusplus.domain.ClockFace
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.HomeApps
import com.aquigs.launcherplusplus.domain.HomePlace
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val layout = PageLayout()
    private val pager = PagerState(currentPage = layout.homeIndex) { layout.pages.size }
    private val homePresses = MutableSharedFlow<HomePress>(extraBufferCapacity = 1)
    private var apps by mutableStateOf<List<AppEntry>?>(listOf(clock, mail))
    private var homeApps by mutableStateOf(HomeApps())
    private var face by mutableStateOf(ClockFace("10:19", "Saturday 13 September"))
    private val opened = mutableListOf<String>()
    private val launched = mutableListOf<AppEntry>()
    private val composeMail = AppShortcut(mail.packageName, "compose", "Compose")
    private val started = mutableListOf<AppShortcut>()
    private val infoOpened = mutableListOf<AppEntry>()
    private val uninstalled = mutableListOf<AppEntry>()
    private var shortcutsLoaded = CompletableDeferred(Unit)
    private val actions = AppActions(
        icon = { null },
        launch = launched::add,
        shortcuts = {
            shortcutsLoaded.await()
            if (it == mail) listOf(composeMail) else emptyList()
        },
        shortcutIcon = { null },
        startShortcut = started::add,
        openAppInfo = infoOpened::add,
        uninstall = uninstalled::add,
    )

    private fun show() = compose.setContent {
        LauncherScreen(
            layout = layout,
            homePresses = homePresses,
            apps = apps,
            homeApps = homeApps,
            onHomeAppsChange = { homeApps = it },
            actions = actions,
            clock = face,
            onOpenClock = { opened += "clock" },
            onOpenCalendar = { opened += "calendar" },
            pagerState = pager,
        )
    }

    private fun pressHome(launcherInFront: Boolean) = compose.runOnIdle { assertTrue(homePresses.tryEmit(HomePress(launcherInFront))) }

    private fun assertSettledOn(page: LauncherPage) {
        compose.waitForIdle()
        assertEquals(page, layout.pages[pager.settledPage])
        compose.page(page).assertIsDisplayed()
    }

    private fun assertDrawerOpen(open: Boolean) {
        compose.waitForIdle()
        compose.drawerHandle().assertContentDescriptionEquals(if (open) "Close the app drawer" else "Open the app drawer")
        if (open) compose.onNodeWithText("Clock").assertIsDisplayed() else compose.onNodeWithText("Clock").assertIsNotDisplayed()
    }

    @Test
    fun startsOnTheHomePageWithTheDrawerClosed() {
        show()

        assertSettledOn(LauncherPage.Home)
        assertDrawerOpen(false)
        compose.drawerHandle().assertIsDisplayed()
        compose.emblem().assertIsDisplayed()
    }

    @Test
    fun theHomePageShowsTheClockAndKeepsItCurrent() {
        show()
        compose.onNodeWithText("10:19").assertIsDisplayed()

        compose.runOnIdle { face = ClockFace("10:20", "Saturday 13 September") }

        compose.onNodeWithText("10:20").assertIsDisplayed()
        compose.clockTime().performClick()
        compose.clockDate().performClick()
        assertEquals(listOf("clock", "calendar"), opened)
    }

    @Test
    fun closingTheDrawerEndsItsSearch() {
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        compose.searchField().performTextInput("ma")
        compose.onNodeWithText("Clock").assertDoesNotExist()

        compose.drawerHandle().performClick()
        assertDrawerOpen(false)
        compose.drawerHandle().performClick()

        assertDrawerOpen(true)
        compose.onNodeWithText("Mail").assertIsDisplayed()
    }

    @Test
    fun theEmblemOpensTheDrawerToPickAppsForTheRing() {
        show()

        compose.emblem().performClick()
        assertDrawerOpen(true)
        compose.placePicker().assertIsDisplayed()

        compose.onNodeWithText("Mail").performClick()
        compose.onNodeWithText("Mail").assertIsOn()
        compose.runOnIdle {
            assertTrue(mail in homeApps.ring)
            assertEquals(emptyList<AppEntry>(), launched)
        }

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.onNodeWithContentDescription("Mail").assertIsDisplayed()
    }

    @Test
    fun anEmptyRingInvitesYouToAddAppsBeforeTheAppListLoads() {
        apps = null
        show()

        compose.onNodeWithText("Add apps").assertIsDisplayed()
    }

    @Test
    fun storedFavouritesHoldBackTheHintUntilTheAppsLoadWithoutThem() {
        homeApps = HomeApps(ring = Favourites(listOf(clock.key)))
        apps = null
        show()
        compose.onNodeWithText("Add apps").assertDoesNotExist()

        apps = listOf(mail)

        compose.onNodeWithText("Add apps").assertIsDisplayed()
    }

    @Test
    fun closingTheDrawerEndsPicking() {
        show()
        compose.emblem().performClick()
        assertDrawerOpen(true)

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        compose.placePicker().assertDoesNotExist()
        compose.onNodeWithText("Mail").performClick()
        compose.runOnIdle { assertEquals(listOf(mail), launched) }
    }

    @Test
    fun aDragThatLeavesTheDrawerOpenKeepsPicking() {
        show()
        compose.emblem().performClick()
        assertDrawerOpen(true)
        val handle = compose.drawerHandle().fetchSemanticsNode().boundsInRoot.center

        compose.onRoot().performTouchInput {
            down(handle)
            moveBy(Offset(0f, height / 3f))
        }
        compose.waitForIdle()
        compose.onRoot().performTouchInput {
            moveBy(Offset(0f, -height / 3f))
            up()
        }

        assertDrawerOpen(true)
        compose.placePicker().assertIsDisplayed()
    }

    @Test
    fun pickingForTheDockFillsTheDockAndLeavesTheRing() {
        show()
        compose.emblem().performClick()
        assertDrawerOpen(true)

        compose.placeOption(HomePlace.Dock).performClick()
        compose.onNodeWithText("Mail").performClick()
        compose.runOnIdle { assertEquals(HomeApps(dock = Favourites(listOf(mail.key))), homeApps) }

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.dockSlot(mail).assertIsDisplayed()
        compose.ringSlot(mail).assertDoesNotExist()
    }

    @Test
    fun eachPlaceChecksTheAppsAlreadyThere() {
        homeApps = HomeApps(ring = Favourites(listOf(clock.key)), dock = Favourites(listOf(mail.key)))
        show()
        compose.emblem().performClick()
        assertDrawerOpen(true)
        compose.onNodeWithText("Clock").assertIsOn()
        compose.onNodeWithText("Mail").assertIsOff()

        compose.placeOption(HomePlace.Dock).performClick()

        compose.onNodeWithText("Clock").assertIsOff()
        compose.onNodeWithText("Mail").assertIsOn()
    }

    @Test
    fun anEmptyDockTakesNoSpace() {
        show()
        compose.dock().assertDoesNotExist()
        val undocked = compose.pager().getUnclippedBoundsInRoot().height

        homeApps = HomeApps(dock = Favourites(listOf(mail.key)))

        compose.dockSlot(mail).assertIsDisplayed()
        assertTrue("the dock takes space", compose.pager().getUnclippedBoundsInRoot().height < undocked)
    }

    @Test
    fun storedDockAppsHoldTheDockRowUntilTheAppsLoad() {
        homeApps = HomeApps(dock = Favourites(listOf(mail.key)))
        apps = null
        show()
        val loading = compose.pager().getUnclippedBoundsInRoot()

        apps = listOf(clock, mail)

        compose.dockSlot(mail).assertIsDisplayed()
        assertEquals(loading, compose.pager().getUnclippedBoundsInRoot())
    }

    @Test
    fun theDockStaysPutWhileThePagesSwipe() {
        homeApps = HomeApps(dock = Favourites(listOf(mail.key)))
        show()
        val docked = compose.dockSlot(mail).getUnclippedBoundsInRoot()

        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)

        compose.dockSlot(mail).assertIsDisplayed()
        assertEquals(docked, compose.dockSlot(mail).getUnclippedBoundsInRoot())
    }

    @Test
    fun longPressingARingAppShowsItsShortcutsThenItsOptions() {
        homeApps = HomeApps(ring = Favourites(listOf(mail.key)))
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }

        val tops = listOf("Compose", "Remove from the ring", "App info", "Uninstall").map {
            compose.onNodeWithText(it).assertIsDisplayed().getUnclippedBoundsInRoot().top
        }
        assertEquals(tops.sorted(), tops)
        compose.onNodeWithText("Remove from the dock").assertDoesNotExist()
    }

    @Test
    fun theMenuStartsShortcutsAndHandsTheOptionsToTheSystem() {
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        for (item in listOf("Compose", "App info", "Uninstall")) {
            compose.onNodeWithText("Mail").performTouchInput { longClick() }
            compose.onNodeWithText(item).performClick()
            compose.appOptionsMenu().assertDoesNotExist()
        }

        compose.runOnIdle {
            assertEquals(listOf(composeMail), started)
            assertEquals(listOf(mail), infoOpened)
            assertEquals(listOf(mail), uninstalled)
            assertEquals(emptyList<AppEntry>(), launched)
        }
    }

    @Test
    fun removingAnAppFromTheDockKeepsItOnTheRing() {
        homeApps = HomeApps(ring = Favourites(listOf(mail.key)), dock = Favourites(listOf(mail.key)))
        show()

        compose.dockSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("Remove from the dock").performClick()

        compose.dockSlot(mail).assertDoesNotExist()
        compose.ringSlot(mail).assertIsDisplayed()
        compose.runOnIdle { assertEquals(HomeApps(ring = Favourites(listOf(mail.key))), homeApps) }
    }

    @Test
    fun homeClosesTheMenu() {
        homeApps = HomeApps(ring = Favourites(listOf(mail.key)))
        show()
        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("App info").assertIsDisplayed()

        pressHome(launcherInFront = true)

        compose.appOptionsMenu().assertDoesNotExist()
    }

    @Test
    fun backClosesTheMenuBeforeTheDrawer() {
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        compose.onNodeWithText("Mail").performTouchInput { longClick() }
        compose.onNodeWithText("App info").assertIsDisplayed()

        Espresso.pressBack()

        compose.appOptionsMenu().assertDoesNotExist()
        assertDrawerOpen(true)
    }

    @Test
    fun aLongPressWhilePickingPicksTheAppAndOpensNoMenu() {
        show()
        compose.emblem().performClick()
        assertDrawerOpen(true)

        compose.onNodeWithText("Mail").performTouchInput { longClick() }

        compose.appOptionsMenu().assertDoesNotExist()
        compose.runOnIdle { assertEquals(HomeApps(ring = Favourites(listOf(mail.key))), homeApps) }
    }

    @Test
    fun homeWhileTheShortcutsLoadOpensNoMenu() {
        homeApps = HomeApps(ring = Favourites(listOf(mail.key)))
        shortcutsLoaded = CompletableDeferred()
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }
        pressHome(launcherInFront = true)
        shortcutsLoaded.complete(Unit)

        compose.appOptionsMenu().assertDoesNotExist()
    }

    @Test
    fun anAppThatGoesWithItsMenuOpenComesBackWithoutIt() {
        homeApps = HomeApps(ring = Favourites(listOf(mail.key)))
        show()
        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("App info").assertIsDisplayed()

        apps = listOf(clock)
        compose.ringSlot(mail).assertDoesNotExist()
        apps = listOf(clock, mail)

        compose.ringSlot(mail).assertIsDisplayed()
        compose.appOptionsMenu().assertDoesNotExist()
    }

    @Test
    fun aSecondTapWhileTheMenuClosesStartsNothingAgain() {
        homeApps = HomeApps(ring = Favourites(listOf(mail.key)))
        show()
        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("Uninstall").assertIsDisplayed()

        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Uninstall").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Uninstall").performClick()
        compose.mainClock.autoAdvance = true

        compose.runOnIdle { assertEquals(listOf(mail), uninstalled) }
    }

    @Test
    fun swipesToBothNeighbours() {
        show()

        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)

        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Home)

        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)
    }

    @Test
    fun theHandleTogglesTheDrawer() {
        show()

        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        compose.drawerHandle().performClick()
        assertDrawerOpen(false)
    }

    @Test
    fun draggingTheHandleUpOpensTheDrawer() {
        show()

        val handle = compose.drawerHandle().fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput { swipe(start = handle, end = Offset(handle.x, top + height / 4)) }

        assertDrawerOpen(true)
    }

    @Test
    fun slidingDownTheRailDoesNotCloseTheDrawer() {
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        val c = compose.railLetter('C').fetchSemanticsNode().boundsInRoot.center
        val m = compose.railLetter('M').fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput { swipe(start = c, end = m) }

        assertDrawerOpen(true)
    }

    @Test
    fun backWhileTheDrawerIsStillOpeningClosesIt() {
        show()
        compose.mainClock.autoAdvance = false
        compose.drawerHandle().performClick()
        compose.mainClock.advanceTimeBy(100)

        Espresso.pressBack()
        compose.mainClock.autoAdvance = true

        assertDrawerOpen(false)
    }

    @Test
    fun homeWhileInFrontClosesTheDrawerAndReturnsToTheHomePage() {
        show()
        compose.swipePager { swipeLeft() }
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        pressHome(launcherInFront = true)

        assertSettledOn(LauncherPage.Home)
        assertDrawerOpen(false)
    }

    @Test
    fun homeFromAnotherAppClosesTheDrawerAndKeepsThePage() {
        show()
        compose.swipePager { swipeLeft() }
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        pressHome(launcherInFront = false)

        assertSettledOn(LauncherPage.Collections)
        assertDrawerOpen(false)
    }

    @Test
    fun backClosesTheDrawerFirstAndThenReturnsToTheHomePage() {
        show()
        compose.swipePager { swipeRight() }
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        Espresso.pressBack()
        assertDrawerOpen(false)
        assertSettledOn(LauncherPage.Widgets)

        Espresso.pressBack()
        assertSettledOn(LauncherPage.Home)
    }
}
