package com.aquigs.launcherplusplus.ui

import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppShortcut
import com.aquigs.launcherplusplus.domain.ClockFace
import com.aquigs.launcherplusplus.domain.CollectionCard
import com.aquigs.launcherplusplus.domain.CollectionKind.MostUsed
import com.aquigs.launcherplusplus.domain.CollectionKind.NewApps
import com.aquigs.launcherplusplus.domain.CollectionsPage
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.ForegroundTime
import com.aquigs.launcherplusplus.domain.HomeApps
import com.aquigs.launcherplusplus.domain.HomePlace
import com.aquigs.launcherplusplus.domain.HostedWidget
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.domain.Ring
import com.aquigs.launcherplusplus.domain.RingSlot
import com.aquigs.launcherplusplus.domain.UnreadCounts
import com.aquigs.launcherplusplus.domain.WidgetPage
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
    private var homeAppsChanges = 0
    private val work = folderOf(clock, mail)
    private var face by mutableStateOf(ClockFace("10:19", "Saturday 13 September"))
    private var isHomeApp by mutableStateOf(true)
    private var homeRequests = 0
    private val opened = mutableListOf<String>()
    private val launched = mutableListOf<AppEntry>()
    private val composeMail = AppShortcut(mail.packageName, "compose", "Compose")
    private val started = mutableListOf<AppShortcut>()
    private val infoOpened = mutableListOf<AppEntry>()
    private val uninstalled = mutableListOf<AppEntry>()
    private var shortcutsLoaded = CompletableDeferred(Unit)
    private val search = HostedWidget(id = 3, rows = 1)
    private var widgetPage by mutableStateOf(WidgetPage())
    private val widgetsAdded = mutableListOf<Int>()
    private val widgetsRemoved = mutableListOf<Int>()
    private val widgets = WidgetActions(view = { context, _ -> View(context) }, add = widgetsAdded::add, remove = widgetsRemoved::add)
    // Empty rather than the default page, so the built-in cards do not double the apps the other tests look for.
    private var collections by mutableStateOf(CollectionsPage(emptyList()))
    private var foregroundTime by mutableStateOf<ForegroundTime?>(null)
    private var usageSettingsOpened = 0
    private var unread by mutableStateOf(UnreadCounts())
    private var badgesEnabled by mutableStateOf(false)
    private var badgeSettingsOpened = 0
    private var notificationsOpened = 0
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

    private fun show(modifier: Modifier = Modifier) = compose.setContent {
        LauncherScreen(
            layout = layout,
            homePresses = homePresses,
            apps = apps,
            homeApps = homeApps,
            onHomeAppsChange = {
                homeApps = it
                homeAppsChanges++
            },
            actions = actions,
            clock = face,
            onOpenClock = { opened += "clock" },
            onOpenCalendar = { opened += "calendar" },
            isHomeApp = isHomeApp,
            onBecomeHomeApp = { homeRequests++ },
            widgetPage = widgetPage,
            widgets = widgets,
            collections = collections,
            onCollectionsChange = { collections = it },
            foregroundTime = foregroundTime,
            onOpenUsageSettings = { usageSettingsOpened++ },
            unread = unread,
            badgesEnabled = badgesEnabled,
            onOpenBadgeSettings = { badgeSettingsOpened++ },
            onOpenNotifications = { notificationsOpened++ },
            modifier = modifier,
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

    private fun centreOf(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().boundsInRoot.center

    /** Opens the drawer, long-presses [app]'s row and drags it off the row, leaving the finger down where it started. */
    private fun startDraggingFromDrawer(app: AppEntry) {
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        val row = centreOf(compose.onNodeWithText(app.label))
        compose.onRoot().performTouchInput {
            down(row)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveTo(row + Offset(0f, -viewConfiguration.touchSlop * 2))
        }
        compose.dragGhost().assertIsDisplayed()
    }

    /** Continues a drag to [target], in root coordinates. */
    private fun dragTo(target: Offset) {
        compose.onRoot().performTouchInput {
            moveTo(target)
            advanceEventTime()
        }
        compose.waitForIdle()
    }

    private fun letGo() {
        compose.onRoot().performTouchInput { up() }
        compose.waitForIdle()
    }

    /** The home page's top-left corner in root coordinates, where the clock, the card and the ring are not. */
    private fun emptyHomeSpace() = compose.page(LauncherPage.Home).fetchSemanticsNode().boundsInRoot.topLeft + Offset(10f, 10f)

    private fun longPressEmptyHomeSpace() = compose.onRoot().performTouchInput { longClick(emptyHomeSpace()) }

    /**
     * Swipes down from [start], in root coordinates, only just past the touch slop: a finger that ends the swipe still on
     * what it started on would tap that too, unless the swipe cancels the tap.
     */
    private fun swipeDownFrom(start: Offset) =
        compose.onRoot().performTouchInput { swipe(start, start + Offset(0f, viewConfiguration.touchSlop * 3)) }

    private fun goToCollections() {
        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)
    }

    /** Long-presses [app] on the Tools card, leaving the finger down: the app lifts at once, as nothing else answers there. */
    private fun liftFromToolsCard(app: AppEntry) {
        val icon = centreOf(compose.collectionApp(tools, app))
        compose.onRoot().performTouchInput {
            down(icon)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, 1f))
        }
        compose.dragGhost().assertIsDisplayed()
        compose.collectionBin().assertIsDisplayed()
    }

    @Test
    fun startsOnTheHomePageWithTheDrawerClosed() {
        show()

        assertSettledOn(LauncherPage.Home)
        assertDrawerOpen(false)
        compose.drawerHandle().assertIsDisplayed()
        compose.emblem().assertIsDisplayed()
        compose.homeAppCard().assertDoesNotExist()
    }

    @Test
    fun theHomePageAsksToBeTheHomeAppUntilItIs() {
        isHomeApp = false
        show()
        val card = compose.homeAppCard().assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("the card sits under the clock", compose.clockDate().getUnclippedBoundsInRoot().bottom <= card.top)
        assertTrue("the card sits over the ring", card.bottom <= compose.emblem().getUnclippedBoundsInRoot().top)

        compose.becomeHomeAppButton().performClick()
        compose.runOnIdle { assertEquals(1, homeRequests) }

        isHomeApp = true

        compose.homeAppCard().assertDoesNotExist()
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
            assertTrue(mail in homeApps.ring.apps)
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
        homeApps = HomeApps(ring = ringOf(clock))
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
        homeApps = HomeApps(ring = ringOf(clock), dock = Favourites(listOf(mail.key)))
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
        homeApps = HomeApps(ring = ringOf(mail))
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }

        val tops = listOf("Compose", "Remove from the ring", "New folder", "App info", "Uninstall").map {
            compose.onNodeWithText(it).assertIsDisplayed().getUnclippedBoundsInRoot().top
        }
        assertEquals(tops.sorted(), tops)
        compose.onNodeWithText("Remove from the dock").assertDoesNotExist()
    }

    @Test
    fun aFolderOpensInPlaceOnTheRingAndLaunchesItsAppsWithoutClosing() {
        val other = alphabet[0]
        apps = listOf(clock, mail, other)
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(other.key), work)))
        show()
        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 2 apps").performClick()

        compose.emblem().assertDoesNotExist()
        compose.folderSlot(1).assertDoesNotExist()
        compose.ringSlot(other).assertDoesNotExist()
        compose.ringSlot(clock).assertIsDisplayed()
        compose.ringSlot(mail).assertIsDisplayed()
        compose.clockTime().assertIsDisplayed()

        compose.ringSlot(mail).performClick()

        compose.runOnIdle { assertEquals(listOf(mail), launched) }
        compose.ringSlot(mail).assertIsDisplayed()
        compose.closeFolder().performClick()
        compose.emblem().assertIsDisplayed()
        compose.folderSlot(1).assertIsDisplayed()
        compose.ringSlot(other).assertIsDisplayed()
    }

    @Test
    fun backAndHomeCloseTheFolderAndStayOnTheHomePage() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()
        compose.emblem().assertDoesNotExist()

        Espresso.pressBack()
        compose.emblem().assertIsDisplayed()
        assertSettledOn(LauncherPage.Home)

        compose.folderSlot(0).performClick()
        compose.emblem().assertDoesNotExist()
        pressHome(launcherInFront = true)

        compose.emblem().assertIsDisplayed()
    }

    @Test
    fun backClosesTheDrawerOverAnOpenFolderFirst() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.emblem().assertDoesNotExist()

        Espresso.pressBack()
        compose.emblem().assertIsDisplayed()
    }

    @Test
    fun backReturnsToTheHomePageBeforeClosingTheFolder() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()
        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)

        Espresso.pressBack()
        assertSettledOn(LauncherPage.Home)
        compose.emblem().assertDoesNotExist()

        Espresso.pressBack()
        compose.emblem().assertIsDisplayed()
    }

    @Test
    fun newFolderFromARingAppStartsOneInItsSlotAndFillsItFromTheDrawer() {
        homeApps = HomeApps(ring = ringOf(clock, mail))
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("New folder").performClick()

        assertDrawerOpen(true)
        compose.placePicker().assertIsDisplayed()
        compose.onNodeWithText("Adding to folder").assertIsDisplayed()
        compose.placeOption(HomePlace.Dock).assertDoesNotExist()
        compose.onNodeWithText("Mail").assertIsOn()
        compose.onNodeWithText("Clock").performClick()
        compose.runOnIdle { assertEquals(Ring(listOf(RingSlot.App(clock.key), folderOf(mail, clock))), homeApps.ring) }

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 2 apps").assertIsDisplayed()
        compose.ringSlot(mail).assertDoesNotExist()
    }

    @Test
    fun aFolderLeftWithOneAppWhenPickingEndsStaysAFolder() {
        homeApps = HomeApps(ring = ringOf(clock, mail))
        show()
        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("New folder").performClick()
        assertDrawerOpen(true)

        Espresso.pressBack()
        assertDrawerOpen(false)

        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 1 app").assertIsDisplayed()
        compose.ringSlot(mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(Ring(listOf(RingSlot.App(clock.key), folderOf(mail))), homeApps.ring) }
    }

    @Test
    fun removingAppsFromAnOpenFolderLeavesItOpenDownToAnEmptyBadge() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()

        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("New folder").assertDoesNotExist()
        compose.onNodeWithText("Add apps").assertDoesNotExist()
        compose.onNodeWithText("Remove from folder").performClick()

        compose.emblem().assertDoesNotExist()
        compose.ringSlot(clock).assertIsDisplayed()
        compose.ringSlot(mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(Ring(listOf(folderOf(clock))), homeApps.ring) }

        compose.ringSlot(clock).performTouchInput { longClick() }
        compose.onNodeWithText("Remove from folder").performClick()

        compose.ringSlot(clock).assertDoesNotExist()
        compose.closeFolder().performClick()
        compose.folderSlot(0).assertContentDescriptionEquals("Folder, 0 apps").performClick()
        compose.emblem().assertIsDisplayed()
        compose.folderSlot(0).performTouchInput { longClick() }
        compose.onNodeWithText("Add apps").assertIsDisplayed()
        compose.onNodeWithText("Remove folder").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Ring(listOf(folderOf())), homeApps.ring) }
    }

    @Test
    fun theFolderMenuAddsAppsAndRemovesTheFolder() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performTouchInput { longClick() }
        compose.folderOptionsMenu().assertIsDisplayed()
        compose.onNodeWithText("Add apps").performClick()
        assertDrawerOpen(true)
        compose.onNodeWithText("Adding to folder").assertIsDisplayed()
        compose.onNodeWithText("Clock").assertIsOn()
        Espresso.pressBack()
        assertDrawerOpen(false)

        compose.folderSlot(0).performTouchInput { longClick() }
        compose.onNodeWithText("Remove folder").performClick()

        compose.folderSlot(0).assertDoesNotExist()
        compose.onNodeWithText("Add apps").assertIsDisplayed()
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }
    }

    @Test
    fun aRingOfFoldersOnlyShowsNoHint() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        apps = null
        show()
        compose.onNodeWithText("Add apps").assertDoesNotExist()

        apps = listOf(clock, mail)

        compose.onNodeWithText("Add apps").assertDoesNotExist()
        compose.folderSlot(0).assertIsDisplayed()
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
        homeApps = HomeApps(ring = ringOf(mail), dock = Favourites(listOf(mail.key)))
        show()

        compose.dockSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("Remove from the dock").performClick()

        compose.dockSlot(mail).assertDoesNotExist()
        compose.ringSlot(mail).assertIsDisplayed()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(mail)), homeApps) }
    }

    @Test
    fun homeClosesTheMenu() {
        homeApps = HomeApps(ring = ringOf(mail))
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
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(mail)), homeApps) }
    }

    @Test
    fun aLongPressInTheDrawerThatStaysPutOpensTheMenuAndDragsNothing() {
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        compose.onNodeWithText("Mail").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(viewConfiguration.touchSlop / 2, 0f))
            up()
        }

        compose.appOptionsMenu().assertIsDisplayed()
        compose.dragGhost().assertDoesNotExist()
        assertDrawerOpen(true)
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }
    }

    @Test
    fun draggingAnAppFromTheDrawerOntoTheRingAddsItThereAndClosesTheDrawer() {
        // Padded like the real launcher under its insets, so the ghost's position is checked against a moved origin.
        show(Modifier.padding(top = 24.dp))
        startDraggingFromDrawer(mail)
        compose.appOptionsMenu().assertDoesNotExist()

        val target = centreOf(compose.emblem())
        dragTo(target)
        assertDrawerOpen(false)
        val ghost = centreOf(compose.dragGhost())
        assertTrue("the ghost $ghost sits on the finger $target", (ghost - target).getDistance() < 2f)
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }
        letGo()

        compose.dragGhost().assertDoesNotExist()
        compose.ringSlot(mail).assertIsDisplayed()
        assertDrawerOpen(false)
        compose.runOnIdle {
            assertEquals(HomeApps(ring = ringOf(mail)), homeApps)
            assertEquals(emptyList<AppEntry>(), launched)
        }
    }

    @Test
    fun draggingAnAppOntoTheDockAddsItToTheDock() {
        show()
        compose.dock().assertDoesNotExist()
        startDraggingFromDrawer(mail)

        // An empty dock has no row until an app is dragged, so its place is only known once the drag is under way.
        dragTo(centreOf(compose.dock()))
        letGo()

        compose.dragGhost().assertDoesNotExist()
        compose.dockSlot(mail).assertIsDisplayed()
        compose.ringSlot(mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(HomeApps(dock = Favourites(listOf(mail.key))), homeApps) }
    }

    @Test
    fun droppingAnAppOffTheRingAndTheDockAddsNothing() {
        show()
        startDraggingFromDrawer(mail)

        dragTo(centreOf(compose.clockTime()))
        letGo()

        compose.dragGhost().assertDoesNotExist()
        assertDrawerOpen(false)
        compose.runOnIdle {
            assertEquals(HomeApps(), homeApps)
            assertEquals(0, homeAppsChanges)
        }
    }

    @Test
    fun draggingAnAppAlreadyOnTheRingOntoItChangesNothing() {
        homeApps = HomeApps(ring = ringOf(mail, clock))
        show()
        startDraggingFromDrawer(mail)

        dragTo(centreOf(compose.emblem()))
        letGo()

        compose.dragGhost().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(HomeApps(ring = ringOf(mail, clock)), homeApps)
            assertEquals(0, homeAppsChanges)
        }
    }

    @Test
    fun homeDuringADragCancelsIt() {
        show()
        startDraggingFromDrawer(mail)
        dragTo(centreOf(compose.emblem()))

        pressHome(launcherInFront = true)
        compose.dragGhost().assertDoesNotExist()
        letGo()

        assertDrawerOpen(false)
        compose.ringSlot(mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }
    }

    @Test
    fun backDuringADragCancelsItAndStaysOnTheHomePage() {
        show()
        startDraggingFromDrawer(mail)
        dragTo(centreOf(compose.emblem()))

        Espresso.pressBack()
        compose.dragGhost().assertDoesNotExist()
        letGo()

        assertSettledOn(LauncherPage.Home)
        compose.ringSlot(mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }
    }

    @Test
    fun aDragWhoseTouchIsTakenAwayAddsNothing() {
        show()
        startDraggingFromDrawer(mail)
        dragTo(centreOf(compose.emblem()))

        compose.onRoot().performTouchInput { cancel() }

        compose.dragGhost().assertDoesNotExist()
        assertDrawerOpen(false)
        compose.ringSlot(mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }
    }

    @Test
    fun homeWhileTheShortcutsLoadOpensNoMenu() {
        homeApps = HomeApps(ring = ringOf(mail))
        shortcutsLoaded = CompletableDeferred()
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }
        pressHome(launcherInFront = true)
        shortcutsLoaded.complete(Unit)

        compose.appOptionsMenu().assertDoesNotExist()
    }

    @Test
    fun anAppThatGoesWithItsMenuOpenComesBackWithoutIt() {
        homeApps = HomeApps(ring = ringOf(mail))
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
        homeApps = HomeApps(ring = ringOf(mail))
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
    fun unreadCountsReachTheRingTheDockTheDrawerAndAnOpenFolder() {
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), work)), dock = Favourites(listOf(mail.key)))
        unread = UnreadCounts(mapOf(clock.packageName to 3, mail.packageName to 7))
        show()

        compose.ringSlot(clock).assertContentDescriptionEquals("Clock, 3 unread")
        compose.badgeOn(HomeRingTags.slot(clock)).assertTextEquals("3")
        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 2 apps, 10 unread")
        compose.dockSlot(mail).assertContentDescriptionEquals("Mail, 7 unread")
        compose.badgeOn(DockTags.slot(mail)).assertTextEquals("7")

        compose.folderSlot(1).performClick()
        compose.ringSlot(mail).assertContentDescriptionEquals("Mail, 7 unread")
        compose.badgeOn(HomeRingTags.slot(mail)).assertTextEquals("7")
        compose.closeFolder().performClick()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        compose.onNodeWithText("Clock").assert(hasText("3 unread"))

        unread = UnreadCounts()

        compose.onNodeWithText("Clock").assert(hasText("3 unread").not())
        compose.ringSlot(clock).assertContentDescriptionEquals("Clock")
        compose.badgeOn(HomeRingTags.slot(clock)).assertDoesNotExist()
    }

    @Test
    fun aLongPressOnEmptyHomeSpaceOpensTheLauncherMenuWhoseRowShowsAndOpensBadgeAccess() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()

        longPressEmptyHomeSpace()

        compose.launcherMenu().assertIsDisplayed()
        compose.onNodeWithText("Unread badges").assert(hasStateDescription("Off"))
        badgesEnabled = true
        compose.onNodeWithText("Unread badges").assert(hasStateDescription("On"))

        compose.onNodeWithText("Unread badges").performClick()

        compose.launcherMenu().assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, badgeSettingsOpened) }
        assertSettledOn(LauncherPage.Home)
    }

    @Test
    fun aLongPressOnTheRingOrTheClockOpensNoLauncherMenu() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.appOptionsMenu().assertIsDisplayed()
        compose.launcherMenu().assertDoesNotExist()
        Espresso.pressBack()

        compose.clockTime().performTouchInput { longClick() }
        compose.launcherMenu().assertDoesNotExist()
        compose.appOptionsMenu().assertDoesNotExist()
    }

    @Test
    fun backAndHomeCloseTheLauncherMenu() {
        show()
        longPressEmptyHomeSpace()
        compose.launcherMenu().assertIsDisplayed()

        Espresso.pressBack()
        compose.launcherMenu().assertDoesNotExist()
        assertSettledOn(LauncherPage.Home)

        longPressEmptyHomeSpace()
        compose.launcherMenu().assertIsDisplayed()
        pressHome(launcherInFront = true)
        compose.launcherMenu().assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, badgeSettingsOpened) }
    }

    @Test
    fun addCollectionOpensThePickerWhereATileAddsASeededCardAndAgainRemovesIt() {
        collections = CollectionsPage()
        show()
        goToCollections()

        compose.addCollectionButton().performClick()

        compose.collectionPicker().assertIsDisplayed()
        compose.collectionTile(NewApps).assertIsSelected()
        compose.collectionTile(tools).assertIsNotSelected()

        compose.collectionTile(tools).performClick()

        compose.onNodeWithText("Collection Added").assertIsDisplayed()
        compose.collectionTile(tools).assertIsSelected()
        // Seeded by the words in the package names: "clock" belongs in Tools, "mail" is nobody's word.
        compose.runOnIdle {
            assertEquals(listOf(NewApps, MostUsed, tools), collections.cards.map { it.kind })
            assertEquals(Favourites(listOf(clock.key)), collections.card(tools)!!.apps)
        }

        compose.collectionTile(tools).performClick()

        compose.onNodeWithText("Collection Removed").assertIsDisplayed()
        compose.collectionTile(tools).assertIsNotSelected()
        compose.runOnIdle { assertEquals(CollectionsPage(), collections) }

        Espresso.pressBack()
        compose.collectionPicker().assertDoesNotExist()
        assertSettledOn(LauncherPage.Collections)
        Espresso.pressBack()
        assertSettledOn(LauncherPage.Home)
    }

    @Test
    fun thePencilOpensAnEditorThatAddsATappedAppOnceAndBackReturnsToThePage() {
        collections = CollectionsPage().add(tools)
        show()
        goToCollections()

        compose.collectionEditButton(tools).performClick()

        compose.collectionEditor().assertIsDisplayed()
        compose.editorRow("Mail").assert(hasText("added").not())
        compose.editorRow("Mail").performClick()
        compose.editorRow("Mail").assert(hasText("added"))
        compose.editorRow("Mail").performClick()
        compose.editorRow("Clock").performClick()
        compose.runOnIdle {
            assertEquals(Favourites(listOf(mail.key, clock.key)), collections.card(tools)!!.apps)
            assertEquals(emptyList<AppEntry>(), launched)
        }

        Espresso.pressBack()

        compose.collectionEditor().assertDoesNotExist()
        assertSettledOn(LauncherPage.Collections)
        compose.collectionApp(tools, mail).assertIsDisplayed()
        compose.collectionApp(tools, clock).assertIsDisplayed()

        // The dots are for this visit's taps, as in Arc.
        compose.collectionEditButton(tools).performClick()
        compose.editorRow("Mail").assert(hasText("added").not())
    }

    @Test
    fun anAppLiftedOffACategoryCardLeavesItOnTheBinAndStaysAnywhereElse() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key, clock.key)))))
        show()
        goToCollections()
        compose.collectionBin().assertDoesNotExist()

        liftFromToolsCard(mail)
        dragTo(centreOf(compose.collectionBin()))
        letGo()

        compose.dragGhost().assertDoesNotExist()
        compose.collectionBin().assertDoesNotExist()
        compose.collectionApp(tools, mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(Favourites(listOf(clock.key)), collections.card(tools)!!.apps) }

        liftFromToolsCard(clock)
        dragTo(centreOf(compose.collectionCard(tools)))
        letGo()

        compose.dragGhost().assertDoesNotExist()
        compose.collectionApp(tools, clock).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(Favourites(listOf(clock.key)), collections.card(tools)!!.apps)
            assertEquals(emptyList<AppEntry>(), launched)
        }
    }

    @Test
    fun backDuringALiftCancelsItAndStaysOnTheCollectionsPage() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key)))))
        show()
        goToCollections()
        liftFromToolsCard(mail)

        Espresso.pressBack()
        compose.dragGhost().assertDoesNotExist()
        compose.collectionBin().assertDoesNotExist()
        letGo()

        assertSettledOn(LauncherPage.Collections)
        compose.runOnIdle { assertEquals(Favourites(listOf(mail.key)), collections.card(tools)!!.apps) }
    }

    @Test
    fun homeClosesTheEditorAndThePicker() {
        collections = CollectionsPage().add(tools)
        show()
        goToCollections()
        compose.collectionEditButton(tools).performClick()
        compose.collectionEditor().assertIsDisplayed()

        pressHome(launcherInFront = true)

        compose.collectionEditor().assertDoesNotExist()
        assertSettledOn(LauncherPage.Home)

        goToCollections()
        compose.addCollectionButton().performClick()
        compose.collectionPicker().assertIsDisplayed()
        pressHome(launcherInFront = false)

        compose.collectionPicker().assertDoesNotExist()
        assertSettledOn(LauncherPage.Collections)
    }

    @Test
    fun mostUsedAsksForUsageAccessAndOpensItsSettings() {
        collections = CollectionsPage()
        show()
        goToCollections()

        compose.onNodeWithText("Permission Required").performClick()
        compose.runOnIdle { assertEquals(1, usageSettingsOpened) }

        foregroundTime = ForegroundTime(mapOf(mail.packageName to 1_000L))

        compose.onNodeWithText("Permission Required").assertDoesNotExist()
        compose.collectionApp(MostUsed, mail).assertIsDisplayed()
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
    fun aSwipeDownAnywhereOnTheHomePageOpensTheNotificationsAndNothingElse() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()

        listOf(emptyHomeSpace(), centreOf(compose.clockTime()), centreOf(compose.ringSlot(mail)), centreOf(compose.emblem()))
            .forEach(::swipeDownFrom)

        compose.runOnIdle {
            assertEquals(4, notificationsOpened)
            assertEquals(emptyList<AppEntry>(), launched)
            assertEquals(emptyList<String>(), opened)
        }
        assertDrawerOpen(false)
    }

    @Test
    fun aPageSwipeThatDriftsDownChangesThePageWithoutOpeningTheNotifications() {
        show()

        compose.swipePager { swipe(center, center + Offset(-width / 2f, height / 10f)) }

        assertSettledOn(LauncherPage.Collections)
        compose.runOnIdle { assertEquals(0, notificationsOpened) }
    }

    @Test
    fun aLongPressThatMovesDownOpensTheMenuRatherThanTheNotifications() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()
        val icon = centreOf(compose.ringSlot(mail))

        compose.onRoot().performTouchInput {
            down(icon)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, height / 4f))
            up()
        }

        compose.appOptionsMenu().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, notificationsOpened) }
    }

    @Test
    fun theWidgetPageAddsAndRemovesWidgetsAndBackClosesItsMenuFirst() {
        widgetPage = WidgetPage(listOf(search))
        show()
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)

        compose.addWidgetButton().performClick()
        compose.runOnIdle { assertEquals(1, widgetsAdded.size) }
        assertTrue("the page holds ${widgetsAdded.single()} rows", widgetsAdded.single() >= 4)

        compose.widget(search).performTouchInput { longClick() }
        compose.widgetOptionsMenu().assertIsDisplayed()
        Espresso.pressBack()
        compose.widgetOptionsMenu().assertDoesNotExist()
        assertSettledOn(LauncherPage.Widgets)

        compose.widget(search).performTouchInput { longClick() }
        compose.onNodeWithText("Remove").performClick()
        compose.runOnIdle { assertEquals(listOf(search.id), widgetsRemoved) }
        widgetPage = WidgetPage()

        compose.widget(search).assertDoesNotExist()
        compose.onNodeWithText("No widgets yet").assertIsDisplayed()
        pressHome(launcherInFront = true)
        assertSettledOn(LauncherPage.Home)
    }

    @Test
    fun aSlowDragAcrossAWidgetSwipesThePageAndOpensNoMenu() {
        widgetPage = WidgetPage(listOf(search))
        show()
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)

        compose.widget(search).performTouchInput {
            down(center)
            // Steps under the touch slop for longer than a long press, so the pager follows the finger the whole way and
            // the widget under it barely moves in its own frame; then a fling the rest of the way.
            repeat(20) { moveBy(Offset(-10f, 0f), delayMillis = viewConfiguration.longPressTimeoutMillis / 8) }
            repeat(8) { moveBy(Offset(-100f, 0f)) }
            up()
        }

        compose.widgetOptionsMenu().assertDoesNotExist()
        assertSettledOn(LauncherPage.Home)
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
