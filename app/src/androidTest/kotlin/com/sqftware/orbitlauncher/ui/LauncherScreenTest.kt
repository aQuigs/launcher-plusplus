package com.sqftware.orbitlauncher.ui

import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.toOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.AppCategory
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppNames
import com.sqftware.orbitlauncher.domain.AppSettings
import com.sqftware.orbitlauncher.domain.AppShortcut
import com.sqftware.orbitlauncher.domain.ClockFace
import com.sqftware.orbitlauncher.domain.CardColour
import com.sqftware.orbitlauncher.domain.CardLook
import com.sqftware.orbitlauncher.domain.CardSetting
import com.sqftware.orbitlauncher.domain.CollectionCard
import com.sqftware.orbitlauncher.domain.CollectionKind
import com.sqftware.orbitlauncher.domain.CollectionPages
import com.sqftware.orbitlauncher.domain.CollectionKind.MostUsed
import com.sqftware.orbitlauncher.domain.CollectionKind.NewApps
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.Colourway
import com.sqftware.orbitlauncher.domain.Colourways
import com.sqftware.orbitlauncher.domain.DrawerStyle
import com.sqftware.orbitlauncher.domain.EntryKind
import com.sqftware.orbitlauncher.domain.Favourites
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.FolderLooks
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.domain.ForegroundTime
import com.sqftware.orbitlauncher.domain.HomeApps
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.PageKind
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.Planet
import com.sqftware.orbitlauncher.domain.PlanetPick
import com.sqftware.orbitlauncher.domain.Ring
import com.sqftware.orbitlauncher.domain.ReorderMode
import com.sqftware.orbitlauncher.domain.RingPages
import com.sqftware.orbitlauncher.domain.RingSlot
import com.sqftware.orbitlauncher.domain.RingerMode
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetPages
import com.sqftware.orbitlauncher.domain.WidgetSizing
import com.sqftware.orbitlauncher.domain.pairOf
import kotlin.math.abs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val FOLD_TARGET = "Drop to put in a folder"

@RunWith(AndroidJUnit4::class)
class LauncherScreenTest {
    @get:Rule
    val compose = createComposeRule()

    // Set before showing the screen, which starts on its home page.
    private var layout by mutableStateOf(PageLayout())
    // Counts the pages of the layout the screen was handed, as the screen's own does, not of one set since.
    private lateinit var pager: PagerState
    private val homePresses = MutableSharedFlow<HomePress>(extraBufferCapacity = 1)
    private val pinRequests = MutableSharedFlow<PinRequest>(extraBufferCapacity = 1)
    private var apps by mutableStateOf<List<AppEntry>?>(listOf(clock, mail))
    private var pinnedShortcuts by mutableStateOf<List<AppEntry>?>(emptyList())
    private val squoosh = AppEntry("Squoosh", "com.example.browser", "com.example.browser.Main", canUninstall = false, kind = EntryKind.Shortcut("squoosh"))
    private var accepts = 0
    private var ringPages by mutableStateOf(RingPages())
    private var homeApps: HomeApps
        get() = ringPages.on(LauncherPage.Home.id)
        set(value) {
            ringPages = ringPages.with(LauncherPage.Home.id, value)
        }
    private var homeAppsChanges = 0
    private var setUps = 0
    private val work = folderOf(clock, mail)
    private var face by mutableStateOf(ClockFace("10:19", "Saturday 13 September", twentyFourHour = true, minuteOfDay = 619))
    private val hourStylesChosen = mutableListOf<Boolean>()
    private var theme by mutableStateOf(Theme.Space)
    private var previewTheme by mutableStateOf<Theme?>(null)
    private var folderLooks by mutableStateOf(FolderLooks().with(Theme.Space, FolderLook.SolarSystem))
    private var colourways by mutableStateOf(Colourways())
    private var folderLook: FolderLook
        get() = folderLooks.of(previewTheme ?: theme)
        set(look) {
            folderLooks = folderLooks.with(previewTheme ?: theme, look)
        }
    private var ambientMotion by mutableStateOf(true)
    private var appNames by mutableStateOf(false)
    private var homeOnReturn by mutableStateOf(false)
    private var drawerStyle by mutableStateOf(DrawerStyle())
    private var reorderMode by mutableStateOf(ReorderMode.Insert)
    private var ringerMode by mutableStateOf(RingerMode.Normal)
    private var ringerTaps = 0
    private var updateAvailable by mutableStateOf(false)
    private var updatesOpened = 0
    private var checkForUpdates by mutableStateOf(true)
    private var isHomeApp by mutableStateOf(true)
    private var homeRequests = 0
    private val opened = mutableListOf<String>()
    private val launched = mutableListOf<AppEntry>()
    private val composeMail = AppShortcut(mail.packageName, "compose", "Compose")
    private val started = mutableListOf<AppShortcut>()
    private val infoOpened = mutableListOf<AppEntry>()
    private val storePagesOpened = mutableListOf<AppEntry>()
    private var hasStorePage = false
    private val uninstalled = mutableListOf<AppEntry>()
    private var shortcutsLoaded = CompletableDeferred(Unit)
    private val search = HostedWidget(id = 3, row = 0, column = 0, rows = 1, columns = 4)
    private var widgetPages by mutableStateOf(WidgetPages())
    private var widgetPage: WidgetPage
        get() = widgetPages[LauncherPage.Widgets.id]
        set(value) {
            widgetPages = WidgetPages(widgetPages.pages + (LauncherPage.Widgets.id to value))
        }
    private val widgetsAdded = mutableListOf<Int>()
    private val widgetPagesAddedTo = mutableListOf<String>()
    private val widgets = { page: String ->
        WidgetActions(
            view = { context, _ -> View(context) },
            add = { rows, _, _ ->
                widgetsAdded += rows
                widgetPagesAddedTo += page
            },
            remove = {},
            arrange = {},
            sizing = { WidgetSizing() },
        )
    }
    // Empty rather than the default page, so the built-in cards do not double the apps the other tests look for.
    private var collectionPages by mutableStateOf(CollectionPages(mapOf(LauncherPage.Collections.id to CollectionsPage(emptyList()))))
    private var collections: CollectionsPage
        get() = collectionPages.on(LauncherPage.Collections.id)
        set(value) {
            collectionPages = collectionPages.with(LauncherPage.Collections.id, value)
        }
    private var foregroundTime by mutableStateOf<ForegroundTime?>(null)
    private var usageSettingsOpened = 0
    private var unread by mutableStateOf(UnreadCounts())
    private var badgesEnabled by mutableStateOf(false)
    private var badgeSettingsOpened = 0
    private var appSettings by mutableStateOf(AppSettings())
    private var notificationsOpened = 0
    private val barsShown = mutableListOf<Boolean>()
    private val wallpapersSet = mutableListOf<ImageBitmap>()
    private var art by mutableStateOf<ThemeArt>(SpaceArt)
    private var restarts = 0
    private var resets = 0
    private val actions = AppActions(
        icon = { null },
        launch = launched::add,
        clearBadge = {},
        shortcuts = {
            shortcutsLoaded.await()
            if (it.key == mail.key) listOf(composeMail) else emptyList()
        },
        shortcutIcon = { null },
        startShortcut = started::add,
        openAppInfo = infoOpened::add,
        hasStorePage = { hasStorePage },
        openStorePage = storePagesOpened::add,
        uninstall = uninstalled::add,
    )

    // The screen's own, so a test can take the launcher out of sight and back without the activity.
    private val sight = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    private fun show(modifier: Modifier = Modifier, restoration: StateRestorationTester? = null) {
        if (restoration != null) restoration.setContent { Screen(modifier) } else compose.setContent { Screen(modifier) }
    }

    @Composable
    private fun Screen(modifier: Modifier) = CompositionLocalProvider(LocalThemeArt provides art, LocalLifecycleOwner provides sight) {
        val shown = layout
        pager = rememberPagerState(shown.homeIndex) { shown.pages.size }
        LauncherScreen(
            layout = shown,
            onLayoutChange = { layout = it },
            homePresses = homePresses,
            pinRequests = pinRequests,
            apps = apps,
            pinnedShortcuts = pinnedShortcuts,
            ringPages = ringPages,
            onRingPagesChange = {
                ringPages = it
                homeAppsChanges++
            },
            onSetUpHome = { setUps++ },
            actions = actions,
            reorderMode = reorderMode,
            onReorderModeChange = { reorderMode = it },
            clock = face,
            onTwentyFourHourChange = {
                hourStylesChosen += it
                face = face.copy(twentyFourHour = it)
            },
            theme = previewTheme ?: theme,
            onThemeChange = { theme = it },
            previewing = previewTheme != null,
            onPreviewThemeChange = { previewTheme = it },
            folderLooks = folderLooks,
            onFolderLooksChange = { folderLooks = it },
            colourways = colourways,
            onColourwaysChange = { colourways = it },
            ambientMotion = ambientMotion,
            onAmbientMotionChange = { ambientMotion = it },
            appNames = appNames,
            onAppNamesChange = { appNames = it },
            homeOnReturn = homeOnReturn,
            onHomeOnReturnChange = { homeOnReturn = it },
            drawerStyle = drawerStyle,
            onDrawerStyleChange = { drawerStyle = it },
            onOpenClock = { opened += "clock" },
            onOpenCalendar = { opened += "calendar" },
            ringerMode = ringerMode,
            onRingerTap = { ringerTaps++ },
            updateAvailable = updateAvailable,
            onOpenUpdate = { updatesOpened++ },
            checkForUpdates = checkForUpdates,
            onCheckForUpdatesChange = { checkForUpdates = it },
            isHomeApp = isHomeApp,
            onBecomeHomeApp = { homeRequests++ },
            widgetPages = widgetPages,
            widgets = widgets,
            collectionPages = collectionPages,
            onCollectionPagesChange = { collectionPages = it },
            foregroundTime = foregroundTime,
            onOpenUsageSettings = { usageSettingsOpened++ },
            unread = unread,
            badgesEnabled = badgesEnabled,
            onOpenBadgeSettings = { badgeSettingsOpened++ },
            appSettings = appSettings,
            onAppSettingsChange = { appSettings = it },
            onOpenNotifications = { notificationsOpened++ },
            onSystemBarsShownChange = { barsShown += it },
            onSetWallpaper = { wallpapersSet += it() },
            onRestart = { restarts++ },
            onReset = { resets++ },
            modifier = modifier,
            pagerState = pager,
        )
    }

    private fun pressHome(launcherInFront: Boolean) = compose.runOnIdle { assertTrue(homePresses.tryEmit(HomePress(launcherInFront))) }

    /** Asks to pin [squoosh], as the browser would; the system pins it on accept unless [refused]. */
    private fun requestPin(refused: Boolean = false) = compose.runOnIdle {
        val request = PinRequest(squoosh, appLabel = "Browser", icon = { null }) {
            accepts++
            if (!refused) pinnedShortcuts = listOf(squoosh)
            !refused
        }
        assertTrue(pinRequests.tryEmit(request))
    }

    /** Takes the launcher out of sight, as the screen going off or an app opening does, and brings it back. */
    private fun leaveAndReturn() {
        compose.runOnIdle { sight.lifecycle.currentState = Lifecycle.State.CREATED }
        compose.runOnIdle { sight.lifecycle.currentState = Lifecycle.State.RESUMED }
    }

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
        compose.onRoot().performTouchInput { liftOut(row) }
        compose.dragGhost().assertIsDisplayed()
    }

    private fun TouchInjectionScope.liftOut(row: Offset) {
        down(row)
        advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
        moveTo(row + Offset(0f, -viewConfiguration.touchSlop * 2))
    }

    /** Long-presses what [node] shows and moves it off its place, leaving the finger down: the item is on the move. */
    private fun pickUp(node: SemanticsNodeInteraction) {
        val at = centreOf(node)
        compose.onRoot().performTouchInput { liftOut(at) }
        compose.dragGhost().assertIsDisplayed()
        compose.reorderSwitch().assertIsDisplayed()
    }

    private fun assertNear(expected: Offset, actual: Offset) =
        assertTrue("$actual is at $expected", (actual - expected).getDistance() < 2f)

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

    /**
     * Holds the finger still for a little longer than [millis], the frames since it stopped aside, then nudges it, so the
     * clock runs on as it rests.
     */
    private fun rest(millis: Long) {
        compose.onRoot().performTouchInput { moveBy(Offset(0f, 0.5f), delayMillis = millis + 100) }
        compose.waitForIdle()
    }

    /** A point over [node]'s place on the ring but off its middle, out from the ring's centre at [hub], away from the others. */
    private fun edgeOf(node: SemanticsNodeInteraction, hub: Offset = centreOf(compose.emblem())): Offset {
        val bounds = node.fetchSemanticsNode().boundsInRoot
        val out = bounds.center - hub
        return bounds.center + out / out.getDistance() * bounds.width * 0.6f
    }

    /**
     * Swipes [down] or up from [start], in root coordinates, only just past the touch slop: a finger that ends the swipe
     * still on what it started on would tap that too, unless the swipe cancels the tap.
     */
    private fun swipeFrom(start: Offset, down: Boolean) = compose.onRoot().performTouchInput {
        val slop = viewConfiguration.touchSlop * 3
        swipe(start, start + Offset(0f, if (down) slop else -slop))
    }

    private fun openResetDialog() {
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Reset launcher").performClick()
        compose.launcherMenu().assertDoesNotExist()
        compose.resetDialog().assertIsDisplayed()
    }

    /**
     * Drags a long collections page [up] or down and lets go, runs [meanwhile] with the clock stopped, then swipes across
     * to the home page before whatever the drag set going can settle.
     */
    private fun swipeHomeFromCollectionsAfter(up: Boolean, durationMillis: Long, meanwhile: () -> Unit) {
        collections = CollectionsPage(AppCategory.entries.map { CollectionCard(CollectionKind.Category(it), Favourites(listOf(mail.key))) })
        show()
        goToCollections()

        compose.mainClock.autoAdvance = false
        // Off the centre line, where each card has its reorder handle.
        compose.swipePager {
            val start = Offset(width / 4f, centerY)
            swipe(start, start + Offset(0f, if (up) -200.dp.toPx() else 200.dp.toPx()), durationMillis)
        }
        meanwhile()
        compose.swipePager { swipeRight() }
        compose.mainClock.autoAdvance = true

        assertSettledOn(LauncherPage.Home)
    }

    private fun goToCollections() {
        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)
    }

    /** Long-presses [app] on the Tools card and moves it off its place, leaving the finger down: the app is lifted. */
    private fun liftFromToolsCard(app: AppEntry) {
        val icon = centreOf(compose.collectionApp(tools, app))
        compose.onRoot().performTouchInput { liftOut(icon) }
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
        homeApps = HomeApps(ring = ringOf(clock), dock = ringOf(mail))
        show()
        val card = compose.homeAppCard().assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("the card sits under the ring", compose.ringSlot(clock).getUnclippedBoundsInRoot().bottom <= card.top)
        assertTrue("the card sits over the dock", card.bottom <= compose.dock().getUnclippedBoundsInRoot().top)

        compose.becomeHomeAppButton().performClick()
        compose.runOnIdle { assertEquals(1, homeRequests) }

        isHomeApp = true

        compose.homeAppCard().assertDoesNotExist()
    }

    @Test
    fun anEmptyHomeOffersToSetItselfUpOrToPickAppsByHand() {
        show()
        compose.onNodeWithText(AUTO_SET_UP).assertIsDisplayed()

        compose.emblem().performClick()
        compose.runOnIdle { assertEquals(1, setUps) }
        compose.pickAppsButton().performClick()
        assertDrawerOpen(true)
        compose.placePicker().assertIsDisplayed()
        Espresso.pressBack()

        homeApps = HomeApps(dock = ringOf(mail))
        compose.onNodeWithText(AUTO_SET_UP).assertDoesNotExist()
        compose.pickAppsButton().assertDoesNotExist()
        compose.onNodeWithText("Add apps").assertIsDisplayed()
    }

    @Test
    fun theHomePageShowsTheClockAndKeepsItCurrent() {
        show()
        compose.onNodeWithText("10:19").assertIsDisplayed()

        compose.runOnIdle { face = face.copy(time = "10:20") }

        compose.onNodeWithText("10:20").assertIsDisplayed()
        compose.clockTime().performClick()
        compose.clockDate().performClick()
        assertEquals(listOf("clock", "calendar"), opened)
    }

    @Test
    fun theRingerShowsItsModeBetweenTheDateAndTheRingAndHandsOnATap() {
        ringerMode = RingerMode.Vibrate
        show()
        val row = compose.ringer().assert(hasStateDescription("Vibrate")).getUnclippedBoundsInRoot()
        assertTrue("the ringer sits under the date", compose.clockDate().getUnclippedBoundsInRoot().bottom <= row.top)
        assertTrue("the ringer sits over the ring", row.bottom <= compose.emblem().getUnclippedBoundsInRoot().top)

        compose.ringer().performClick()

        compose.runOnIdle { assertEquals(1, ringerTaps) }
    }

    @Test
    fun theUpdateButtonShowsBesideTheRingerOnlyWhileAnUpdateIsOutAndHandsOnATap() {
        show()
        compose.onNodeWithTag(HomeClockTags.UPDATE).assertDoesNotExist()

        updateAvailable = true

        val button = compose.onNodeWithTag(HomeClockTags.UPDATE).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("the button sits beside the ringer", compose.ringer().getUnclippedBoundsInRoot().right <= button.left)
        compose.onNodeWithTag(HomeClockTags.UPDATE).performClick()
        compose.runOnIdle { assertEquals(1, updatesOpened) }
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
        homeApps = HomeApps(dock = ringOf(clock))
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
    fun anEmptyHomeOffersToSetItselfUpBeforeTheAppListLoads() {
        apps = null
        show()

        compose.onNodeWithText(AUTO_SET_UP).assertIsDisplayed()
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
        compose.pickAppsButton().performClick()
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
        compose.pickAppsButton().performClick()
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
        compose.pickAppsButton().performClick()
        assertDrawerOpen(true)

        compose.placeOption(HomePlace.Dock).performClick()
        compose.onNodeWithText("Mail").performClick()
        compose.runOnIdle { assertEquals(HomeApps(dock = ringOf(mail)), homeApps) }

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.dockSlot(mail).assertIsDisplayed()
        compose.ringSlot(mail).assertDoesNotExist()
    }

    @Test
    fun eachPlaceChecksTheAppsAlreadyThere() {
        homeApps = HomeApps(ring = ringOf(clock), dock = ringOf(mail))
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
        // Not an empty home, whose set-up choice would leave as the dock arrives and confound the measure.
        homeApps = HomeApps(ring = ringOf(clock))
        show()
        compose.dock().assertDoesNotExist()
        val undocked = compose.emblem().getUnclippedBoundsInRoot().bottom

        homeApps = HomeApps(ring = ringOf(clock), dock = ringOf(mail))

        compose.dockSlot(mail).assertIsDisplayed()
        assertTrue("the dock takes space", compose.emblem().getUnclippedBoundsInRoot().bottom < undocked)
    }

    @Test
    fun storedDockAppsHoldTheDockRowUntilTheAppsLoad() {
        homeApps = HomeApps(dock = ringOf(mail))
        apps = null
        show()
        val loading = compose.emblem().getUnclippedBoundsInRoot()

        apps = listOf(clock, mail)

        compose.dockSlot(mail).assertIsDisplayed()
        assertEquals(loading, compose.emblem().getUnclippedBoundsInRoot())
    }

    @Test
    fun theDockLeavesWithTheHomePageAndTheOtherPagesReachTheDrawerHandle() {
        homeApps = HomeApps(dock = ringOf(mail))
        show()
        compose.dockSlot(mail).assertIsDisplayed()

        goToCollections()
        compose.dockSlot(mail).assertIsNotDisplayed()
        val pageBottom = compose.page(LauncherPage.Collections).getUnclippedBoundsInRoot().bottom
        val handleTop = compose.drawerHandle().getUnclippedBoundsInRoot().top
        assertTrue("nothing lies between the page and the drawer handle", pageBottom <= handleTop && handleTop - pageBottom < 16.dp)
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Home)

        compose.dockSlot(mail).assertIsDisplayed()
    }

    @Test
    fun aSidewaysSwipeWhileTheCollectionsPageStillScrollsTurnsThePage() {
        val card = compose.collectionCard(CollectionKind.Category(AppCategory.Video))
        swipeHomeFromCollectionsAfter(up = true, durationMillis = 100) {
            compose.mainClock.advanceTimeBy(50)
            val flinging = card.getUnclippedBoundsInRoot().top
            compose.mainClock.advanceTimeByFrame()
            assertTrue("the page still scrolls", card.getUnclippedBoundsInRoot().top < flinging)
        }
    }

    @Test
    fun aSidewaysSwipeWhileTheCollectionsPageSpringsBackFromItsTopTurnsThePage() {
        swipeHomeFromCollectionsAfter(up = false, durationMillis = 300) { compose.mainClock.advanceTimeByFrame() }
    }

    @Test
    fun longPressingARingAppShowsItsShortcutsThenItsOptions() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }

        val tops = listOf("Compose", "Remove from the ring", "New folder", "Hide from New & Most Used", "App info", "Uninstall").map {
            compose.onNodeWithText(it).assertIsDisplayed().getUnclippedBoundsInRoot().top
        }
        assertEquals(tops.sorted(), tops)
        compose.onNodeWithText("Remove from the dock").assertDoesNotExist()
        compose.onNodeWithText("Add to the ring").assertDoesNotExist()
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
    fun aTapOnEmptySpaceBackAndHomeCloseTheFolderAndStayOnTheHomePage() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()
        compose.emblem().assertDoesNotExist()

        compose.tapEmptyHomeSpace()
        compose.emblem().assertIsDisplayed()
        compose.launcherMenu().assertDoesNotExist()

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
    fun backWithNothingOpenKeepsTheLauncherOnTheHomePage() {
        homeApps = HomeApps(ring = ringOf(clock, mail))
        show()

        Espresso.pressBack()

        compose.ringSlot(clock).assertIsDisplayed()
        assertSettledOn(LauncherPage.Home)
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
    fun aFolderPickedEmptyStaysUntilThePickEnds() {
        homeApps = HomeApps(ring = ringOf(clock, mail))
        show()
        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("New folder").performClick()
        assertDrawerOpen(true)

        compose.onNodeWithText("Mail").performClick()
        compose.onNodeWithText("Adding to folder").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Ring(listOf(RingSlot.App(clock.key), folderOf())), homeApps.ring) }

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.folderSlot(1).assertDoesNotExist()
        compose.runOnIdle { assertEquals(ringOf(clock), homeApps.ring) }
    }

    @Test
    fun removingTheLastAppFromAnOpenFolderRemovesTheFolder() {
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(mail.key), work)))
        show()
        compose.folderSlot(1).performClick()

        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("New folder").assertDoesNotExist()
        compose.onNodeWithText("Add apps").assertDoesNotExist()
        compose.onNodeWithText("Remove from folder").performClick()

        compose.emblem().assertDoesNotExist()
        compose.ringSlot(clock).assertIsDisplayed()
        compose.runOnIdle { assertEquals(Ring(listOf(RingSlot.App(mail.key), folderOf(clock))), homeApps.ring) }

        compose.ringSlot(clock).performTouchInput { longClick() }
        compose.onNodeWithText("Remove from folder").performClick()

        compose.emblem().assertIsDisplayed()
        compose.folderSlot(1).assertDoesNotExist()
        compose.ringSlot(mail).assertIsDisplayed()
        compose.runOnIdle { assertEquals(ringOf(mail), homeApps.ring) }
    }

    @Test
    fun emptyingAnOpenFolderAheadOfAnotherClosesItRatherThanOpeningTheNext() {
        val first = alphabet[0]
        apps = listOf(clock, mail, first)
        homeApps = HomeApps(ring = Ring(listOf(folderOf(first), work)))
        show()
        compose.folderSlot(0).performClick()

        compose.ringSlot(first).performTouchInput { longClick() }
        compose.onNodeWithText("Remove from folder").performClick()

        compose.emblem().assertIsDisplayed()
        compose.folderSlot(0).assertContentDescriptionEquals("Folder, 2 apps").assertIsDisplayed()
        compose.ringSlot(clock).assertDoesNotExist()
        compose.runOnIdle { assertEquals(Ring(listOf(work)), homeApps.ring) }
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
        compose.onNodeWithText(AUTO_SET_UP).assertIsDisplayed()
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }
    }

    @Test
    fun aLongPressOnAnOpenFoldersMiddleAddsAppsToItAndLeavesItOpen() {
        val other = alphabet[0]
        apps = listOf(clock, mail, other)
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()

        compose.closeFolder().performTouchInput { longClick() }
        assertDrawerOpen(true)
        compose.onNodeWithText("Adding to folder").assertIsDisplayed()
        compose.onNodeWithText(other.label).performClick()
        Espresso.pressBack()

        assertDrawerOpen(false)
        compose.ringSlot(other).assertIsDisplayed()
        compose.closeFolder().assertIsDisplayed()
    }

    @Test
    fun theFolderMenuRenamesAFolderWhoseNameHangsUnderItWhileTheRingShowsNames() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        appNames = true
        show()
        compose.folderSlot(0).performTouchInput { longClick() }
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithTag(FolderTags.NAME).performTextReplacement("Work")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithTag(FolderTags.NAME_DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertEquals(HomeApps(ring = Ring(listOf(work.copy(name = "Work")))), homeApps) }
        compose.folderSlot(0).assertContentDescriptionEquals("Work folder, 2 apps")
        compose.onNode(hasText("Work") and hasAnyAncestor(hasTestTag(HomeRingTags.folder(0))), useUnmergedTree = true).assertExists()
    }

    // The dock shows no names, so a name given there would never be seen.
    @Test
    fun aDockFoldersMenuOffersNoRename() {
        homeApps = HomeApps(dock = Ring(listOf(work)))
        show()
        compose.dockFolder(0).performTouchInput { longClick() }

        compose.folderOptionsMenu().assertIsDisplayed()
        compose.onNodeWithText("Rename").assertDoesNotExist()
    }

    @Test
    fun theFolderMenuPicksTheFoldersPlanetInTheSolarSystemOnly() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performTouchInput { longClick() }
        compose.onNodeWithText("Change planet").performClick()
        compose.onNodeWithTag(FolderTags.PLANET_DIALOG).assertIsDisplayed()
        compose.onNodeWithText("Mercury").assertIsSelected()

        compose.onNodeWithText("Saturn").performClick()

        compose.onNodeWithTag(FolderTags.PLANET_DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertEquals(HomeApps(ring = Ring(listOf(work.copy(pick = PlanetPick.Of(Planet.Saturn))))), homeApps) }

        folderLook = FolderLook.Rim
        compose.folderSlot(0).performTouchInput { longClick() }
        compose.folderOptionsMenu().assertIsDisplayed()
        compose.onNodeWithText("Change planet").assertDoesNotExist()
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
        hasStorePage = true
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        for (item in listOf("Compose", "Open in Play Store", "App info", "Uninstall")) {
            compose.onNodeWithText("Mail").performTouchInput { longClick() }
            compose.onNodeWithText(item).performClick()
            compose.appOptionsMenu().assertDoesNotExist()
        }

        compose.runOnIdle {
            assertEquals(listOf(composeMail), started)
            assertEquals(listOf(mail), storePagesOpened)
            assertEquals(listOf(mail), infoOpened)
            assertEquals(listOf(mail), uninstalled)
            assertEquals(emptyList<AppEntry>(), launched)
        }
    }

    @Test
    fun splittingARingAppWithAnotherPutsThePairOnTheRingWhereATapLaunchesIt() {
        homeApps = HomeApps(ring = ringOf(clock))
        show()

        compose.ringSlot(clock).performTouchInput { longClick() }
        compose.onNodeWithText("Split with…").performClick()
        assertDrawerOpen(true)
        compose.onNodeWithText("Pick an app to split with Clock").assertIsDisplayed()
        compose.onNodeWithText("Mail").performClick()

        val pair = pairOf(clock, mail)!!
        assertDrawerOpen(false)
        compose.runOnIdle { assertEquals(HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), RingSlot.App(pair.key)))), homeApps) }
        compose.ringSlot(pair).assertContentDescriptionEquals("Clock | Mail").performClick()
        compose.runOnIdle { assertEquals(listOf(pair), launched) }

        compose.ringSlot(pair).performTouchInput { longClick() }
        compose.onNodeWithText("Remove from the ring").assertIsDisplayed()
    }

    @Test
    fun aPairSplitFromTheDrawerGoesOnTheRingAndBackEndsThePickWithout() {
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        compose.onNodeWithText("Mail").performTouchInput { longClick() }
        compose.onNodeWithText("Split with…").performClick()

        compose.onNodeWithText("Mail").assertIsNotEnabled()
        compose.onNodeWithText("Pick an app to split with Mail").assertIsDisplayed()
        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.runOnIdle { assertEquals(HomeApps(), homeApps) }

        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        compose.placePicker().assertDoesNotExist()
        compose.onNodeWithText("Mail").performTouchInput { longClick() }
        compose.onNodeWithText("Split with…").performClick()
        compose.onNodeWithText("Clock").performClick()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(pairOf(mail, clock)!!)), homeApps) }
    }

    @Test
    fun aPairSplitOnAHandPickedCardGoesOnThatCardAndOnABuiltInOneOnTheRing() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(clock.key))), CollectionCard(NewApps)))
        show()
        goToCollections()

        compose.collectionApp(tools, clock).performTouchInput { longClick() }
        compose.onNodeWithText("Split with…").performClick()
        compose.onNodeWithText("Mail").performClick()

        val pair = pairOf(clock, mail)!!
        assertDrawerOpen(false)
        assertSettledOn(LauncherPage.Collections)
        compose.collectionApp(tools, pair).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(Favourites(listOf(clock.key, pair.key)), collections.card(tools)!!.apps)
            assertEquals(HomeApps(), homeApps)
        }

        compose.collectionApp(NewApps, mail).performTouchInput { longClick() }
        compose.onNodeWithText("Split with…").performClick()
        compose.onNodeWithText("Clock").performClick()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(pairOf(mail, clock)!!)), homeApps) }
    }

    @Test
    fun aPairsMenuFlipsItsOrderInPlaceOnTheRingAndOnACard() {
        val pair = pairOf(clock, mail)!!
        val flipped = pairOf(mail, clock)!!
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(pair.key), RingSlot.App(clock.key))))
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(pair.key, clock.key)))))
        show()

        compose.ringSlot(pair).performTouchInput { longClick() }
        compose.onNodeWithText("Flip order").performClick()
        compose.runOnIdle { assertEquals(HomeApps(ring = Ring(listOf(RingSlot.App(flipped.key), RingSlot.App(clock.key)))), homeApps) }

        goToCollections()
        compose.collectionApp(tools, pair).performTouchInput { longClick() }
        compose.onNodeWithText("Flip order").performClick()
        compose.runOnIdle { assertEquals(Favourites(listOf(flipped.key, clock.key)), collections.card(tools)!!.apps) }
    }

    @Test
    fun theDrawerMenuAddsAnAppToEachHomePlaceItIsNotIn() {
        show()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        compose.onNodeWithText("Mail").performTouchInput { longClick() }
        compose.onNodeWithText("Add to the ring").performClick()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(mail)), homeApps) }

        compose.onNodeWithText("Mail").performTouchInput { longClick() }
        compose.onNodeWithText("Add to the ring").assertDoesNotExist()
        compose.onNodeWithText("Add to the dock").performClick()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(mail), dock = ringOf(mail)), homeApps) }
    }

    @Test
    fun anAppsMenuTurnsItsBadgeOffAndOn() {
        homeApps = HomeApps(ring = ringOf(mail))
        badgesEnabled = true
        show()

        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("Hide badge").performClick()
        compose.runOnIdle { assertEquals(AppSettings(badgeOff = setOf(mail.packageName)), appSettings) }

        compose.ringSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("Show badge").performClick()
        compose.runOnIdle { assertEquals(AppSettings(), appSettings) }
    }

    @Test
    fun aPairsMenuTurnsEachOfItsAppsBadgesOffAndOn() {
        val pair = pairOf(clock, mail)!!
        homeApps = HomeApps(ring = ringOf(pair))
        badgesEnabled = true
        show()

        compose.ringSlot(pair).performTouchInput { longClick() }
        compose.onNodeWithText("Hide Mail badge").performClick()
        compose.runOnIdle { assertEquals(AppSettings(badgeOff = setOf(mail.packageName)), appSettings) }

        compose.ringSlot(pair).performTouchInput { longClick() }
        compose.onNodeWithText("Hide Clock badge").assertIsDisplayed()
        compose.onNodeWithText("Show Mail badge").performClick()
        compose.runOnIdle { assertEquals(AppSettings(), appSettings) }
    }

    @Test
    fun anAppsMenuLeavesItOffTheBuiltInCardsAndPutsItBack() {
        collections = CollectionsPage(listOf(CollectionCard(NewApps)))
        show()
        compose.collectionApp(NewApps, mail).assertExists()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        compose.onNodeWithText("Mail").performTouchInput { longClick() }
        compose.onNodeWithText("Hide from New & Most Used").performClick()

        compose.collectionApp(NewApps, mail).assertDoesNotExist()
        compose.collectionApp(NewApps, clock).assertExists()

        compose.onNodeWithText("Mail").performTouchInput { longClick() }
        compose.onNodeWithText("Show in New & Most Used").performClick()

        compose.collectionApp(NewApps, mail).assertExists()
        compose.runOnIdle { assertEquals(AppSettings(), appSettings) }
    }

    @Test
    fun removingAnAppFromTheDockKeepsItOnTheRing() {
        homeApps = HomeApps(ring = ringOf(mail), dock = ringOf(mail))
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
        compose.pickAppsButton().performClick()
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
    fun aDropOnTheDockCountsBeforeTheHomePageHasComeBack() {
        homeApps = HomeApps(dock = ringOf(clock))
        show()
        val dock = centreOf(compose.dock())
        goToCollections()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        val row = centreOf(compose.onNodeWithText(mail.label))

        // One gesture, so the finger lets go over where the dock will be while the home page is still on its way.
        compose.onRoot().performTouchInput {
            liftOut(row)
            moveTo(dock)
            up()
        }

        compose.runOnIdle { assertEquals(HomeApps(dock = ringOf(clock, mail)), homeApps) }
    }

    @Test
    fun anAppDraggedFromTheDrawerOverTheCollectionsPageGoesOntoAHandPickedCard() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(clock.key)))))
        show()
        goToCollections()
        startDraggingFromDrawer(mail)

        dragTo(centreOf(compose.collectionCard(tools)))
        letGo()

        assertSettledOn(LauncherPage.Collections)
        compose.collectionApp(tools, mail).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(Favourites(listOf(clock.key, mail.key)), collections.card(tools)!!.apps)
            assertEquals(HomeApps(), homeApps)
        }
    }

    @Test
    fun anAppRestedAtTheScreensEdgeTurnsThePageAndMovesOntoACardThere() {
        homeApps = HomeApps(ring = ringOf(mail, clock))
        collections = CollectionsPage(listOf(CollectionCard(tools)))
        show()
        val edge = compose.onRoot().fetchSemanticsNode().size.width - 2f

        pickUp(compose.ringSlot(mail))
        dragTo(Offset(edge, centreOf(compose.emblem()).y))
        rest(PAGE_TURN_MILLIS)
        assertSettledOn(LauncherPage.Collections)
        dragTo(centreOf(compose.collectionCard(tools)))
        letGo()

        compose.runOnIdle {
            assertEquals(Favourites(listOf(mail.key)), collections.card(tools)!!.apps)
            assertEquals(HomeApps(ring = ringOf(clock)), homeApps)
        }
    }

    @Test
    fun aCardsAppTakenHomeByTheScreensEdgeMovesOntoTheRing() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key)))))
        show()
        goToCollections()

        liftFromToolsCard(mail)
        dragTo(Offset(2f, centreOf(compose.collectionCard(tools)).y))
        rest(PAGE_TURN_MILLIS)
        assertSettledOn(LauncherPage.Home)
        dragTo(centreOf(compose.emblem()))
        letGo()

        compose.runOnIdle {
            assertEquals(HomeApps(ring = ringOf(mail)), homeApps)
            assertEquals(Favourites(), collections.card(tools)!!.apps)
        }
    }

    @Test
    fun anAppFromABuiltInCardGoesOntoAHandPickedCard() {
        collections = CollectionsPage(listOf(CollectionCard(NewApps), CollectionCard(tools)))
        show()
        goToCollections()

        val icon = centreOf(compose.collectionApp(NewApps, mail))
        compose.onRoot().performTouchInput { liftOut(icon) }
        compose.dragGhost().assertIsDisplayed()
        dragTo(centreOf(compose.collectionCard(tools)))
        letGo()

        compose.collectionApp(tools, mail).assertIsDisplayed()
        compose.runOnIdle { assertEquals(Favourites(listOf(mail.key)), collections.card(tools)!!.apps) }
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
        compose.runOnIdle { assertEquals(HomeApps(dock = ringOf(mail)), homeApps) }
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
    fun theRingMakesWayOnlyWhereTheFingerRestsAndAQuickDropStillTakesThePlaceUnderIt() {
        val four = alphabet.take(4)
        apps = four
        homeApps = HomeApps(ring = ringOf(*four.toTypedArray()))
        show()

        pickUp(compose.ringSlot(four[0]))
        compose.appOptionsMenu().assertDoesNotExist()
        compose.reorderModeButton(ReorderMode.Insert).assertIsSelected()
        // Measured once the drag is under way: the dock comes in for it, and the ring makes room.
        val slots = four.map { centreOf(compose.ringSlot(it)) }

        // A sweep past moves nothing; a rest makes way.
        dragTo(edgeOf(compose.ringSlot(four[2])))
        assertNear(slots[0], centreOf(compose.ringSlot(four[0])))
        assertNear(slots[2], centreOf(compose.ringSlot(four[2])))
        rest(MAKE_WAY_MILLIS)
        assertNear(slots[2], centreOf(compose.ringSlot(four[0])))
        assertNear(slots[0], centreOf(compose.ringSlot(four[1])))
        assertNear(slots[1], centreOf(compose.ringSlot(four[2])))
        assertNear(slots[3], centreOf(compose.ringSlot(four[3])))
        compose.runOnIdle { assertEquals(0, homeAppsChanges) }

        // Resting anywhere but a slot puts everything back, and so does letting go there.
        dragTo(centreOf(compose.emblem()))
        rest(MAKE_WAY_MILLIS)
        assertNear(slots[0], centreOf(compose.ringSlot(four[0])))
        letGo()
        compose.dragGhost().assertDoesNotExist()
        compose.reorderSwitch().assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, homeAppsChanges) }

        // Let go at once on another's middle, the app takes its place rather than folding into it.
        pickUp(compose.ringSlot(four[0]))
        dragTo(centreOf(compose.ringSlot(four[2])))
        letGo()

        compose.reorderSwitch().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(ringOf(four[1], four[2], four[0], four[3]), homeApps.ring)
            assertEquals(emptyList<AppEntry>(), launched)
        }
    }

    @Test
    fun aRingAppRestingOnAnotherLightsItAndFoldsIntoAFolderWithIt() {
        val four = alphabet.take(4)
        apps = four
        homeApps = HomeApps(ring = ringOf(*four.toTypedArray()))
        show()

        pickUp(compose.ringSlot(four[0]))
        dragTo(centreOf(compose.ringSlot(four[2])))
        compose.ringSlot(four[2]).assert(hasStateDescription(FOLD_TARGET).not())
        rest(FOLD_MILLIS)
        compose.ringSlot(four[2]).assert(hasStateDescription(FOLD_TARGET))

        // One the finger leaves must be rested on again, not just swept back to.
        dragTo(centreOf(compose.emblem()))
        dragTo(centreOf(compose.ringSlot(four[2])))
        compose.ringSlot(four[2]).assert(hasStateDescription(FOLD_TARGET).not())
        rest(FOLD_MILLIS)
        letGo()

        compose.folderSlot(1).assertContentDescriptionEquals("Folder, 2 apps")
        compose.runOnIdle {
            assertEquals(Ring(listOf(RingSlot.App(four[1].key), folderOf(four[2], four[0]), RingSlot.App(four[3].key))), homeApps.ring)
        }
    }

    @Test
    fun aDockAppLetGoOnAnEmptyRingGoesOnIt() {
        homeApps = HomeApps(dock = ringOf(mail))
        show()

        pickUp(compose.dockSlot(mail))
        dragTo(centreOf(compose.emblem()))
        letGo()

        compose.ringSlot(mail).assertIsDisplayed()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(mail)), homeApps) }
    }

    @Test
    fun appsMoveBetweenTheRingAndTheDockToWhereTheyAreLetGo() {
        val three = alphabet.take(3)
        apps = three
        homeApps = HomeApps(ring = ringOf(*three.toTypedArray()))
        show()
        compose.dock().assertDoesNotExist()

        // The empty dock comes in for a ring app, as a place to drop it.
        pickUp(compose.ringSlot(three[0]))
        dragTo(centreOf(compose.dock()))
        letGo()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(three[1], three[2]), dock = ringOf(three[0])), homeApps) }

        pickUp(compose.dockSlot(three[0]))
        dragTo(centreOf(compose.ringSlot(three[2])))
        letGo()
        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(three[1], three[0], three[2])), homeApps) }
    }

    // The folder's icons go as it closes, the dragged one's too; its node, and the gesture on it, must stay.
    @Test
    fun anAppHeldOverTheMiddleOfItsFolderClosesItAndLandsOnTheRingTakingTheEmptiedFolderAway() {
        val (first, second) = alphabet.take(2)
        apps = listOf(clock, first, second)
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(first.key), RingSlot.App(second.key), folderOf(clock))))
        show()
        compose.folderSlot(2).performClick()

        pickUp(compose.ringSlot(clock))
        dragTo(centreOf(compose.closeFolder()))
        rest(MAKE_WAY_MILLIS)
        compose.emblem().assertIsDisplayed()
        compose.dragGhost().assertIsDisplayed()
        compose.reorderSwitch().assertIsDisplayed()
        compose.ringSlot(clock).assertDoesNotExist()

        dragTo(centreOf(compose.ringSlot(first)))
        compose.dragGhost().assertIsDisplayed()
        letGo()

        compose.dragGhost().assertDoesNotExist()
        compose.folderSlot(2).assertDoesNotExist()
        compose.runOnIdle { assertEquals(ringOf(clock, first, second), homeApps.ring) }
    }

    @Test
    fun theRingMakesWayBetweenItsAppsForAnAppOutOfItsFolderAndItLandsThere() {
        val three = alphabet.take(3)
        apps = listOf(clock) + three
        homeApps = HomeApps(ring = Ring(three.map { RingSlot.App(it.key) } + folderOf(clock)))
        show()
        compose.folderSlot(3).performClick()

        pickUp(compose.ringSlot(clock))
        dragTo(centreOf(compose.closeFolder()))
        rest(MAKE_WAY_MILLIS)
        // On the ring between the first two apps, nearer the second, and farther than an icon from either.
        val centre = centreOf(compose.emblem())
        val (first, second) = three.take(2).map { centreOf(compose.ringSlot(it)) - centre }
        val towards = first * 0.4f + second * 0.6f
        dragTo(centre + towards * (first.getDistance() / towards.getDistance()))
        compose.ringSlot(clock).assertDoesNotExist()
        rest(MAKE_WAY_MILLIS)
        compose.ringSlot(clock).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, homeAppsChanges) }
        letGo()

        compose.runOnIdle { assertEquals(ringOf(three[0], clock, three[1], three[2]), homeApps.ring) }
    }

    @Test
    fun anAppOutOfItsFolderLandsBeforeItWhereTheRingMadeWay() {
        val (first, second) = alphabet.take(2)
        apps = listOf(clock, mail, first, second)
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(first.key), work, RingSlot.App(second.key))))
        show()
        compose.folderSlot(1).performClick()

        pickUp(compose.ringSlot(clock))
        dragTo(centreOf(compose.closeFolder()))
        rest(MAKE_WAY_MILLIS)
        dragTo(edgeOf(compose.folderSlot(1)))
        rest(MAKE_WAY_MILLIS)
        compose.ringSlot(clock).assertIsDisplayed()
        letGo()

        compose.runOnIdle { assertEquals(Ring(listOf(RingSlot.App(first.key), RingSlot.App(clock.key), folderOf(mail), RingSlot.App(second.key))), homeApps.ring) }
    }

    @Test
    fun anAppDraggedOutOfItsFolderAndLetGoOffTheRingAndTheDockStaysInIt() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()

        pickUp(compose.ringSlot(clock))
        dragTo(centreOf(compose.closeFolder()))
        rest(MAKE_WAY_MILLIS)
        compose.emblem().assertIsDisplayed()
        dragTo(centreOf(compose.clockTime()))
        letGo()

        compose.runOnIdle {
            assertEquals(HomeApps(ring = Ring(listOf(work))), homeApps)
            assertEquals(0, homeAppsChanges)
        }
    }

    @Test
    fun aQuickSweepAcrossAnOpenFoldersMiddleLeavesItOpen() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.folderSlot(0).performClick()

        pickUp(compose.ringSlot(clock))
        // Measured once the drag is under way: the dock comes in for it, and the ring makes room.
        val start = centreOf(compose.ringSlot(clock))
        dragTo(centreOf(compose.closeFolder()))
        dragTo(start)
        rest(MAKE_WAY_MILLIS)
        compose.closeFolder().assertIsDisplayed()
        compose.emblem().assertDoesNotExist()
        letGo()

        compose.closeFolder().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, homeAppsChanges) }
    }

    @Test
    fun anAppOutOfItsFolderLightsAnotherFolderButNotItsOwn() {
        val first = alphabet[0]
        apps = listOf(clock, mail, first)
        homeApps = HomeApps(ring = Ring(listOf(work, folderOf(first))))
        show()
        compose.folderSlot(0).performClick()

        pickUp(compose.ringSlot(clock))
        dragTo(centreOf(compose.closeFolder()))
        rest(MAKE_WAY_MILLIS)
        compose.emblem().assertIsDisplayed()

        dragTo(centreOf(compose.folderSlot(0)))
        rest(FOLD_MILLIS)
        compose.folderSlot(0).assert(hasStateDescription(FOLD_TARGET).not())
        dragTo(centreOf(compose.folderSlot(1)))
        rest(FOLD_MILLIS)
        compose.folderSlot(1).assert(hasStateDescription(FOLD_TARGET))
        letGo()

        compose.runOnIdle { assertEquals(Ring(listOf(folderOf(mail), folderOf(first, clock))), homeApps.ring) }
    }

    @Test
    fun anAppFromTheDrawerRestingOnARingAppMakesAFolderWithIt() {
        homeApps = HomeApps(ring = ringOf(clock))
        show()
        startDraggingFromDrawer(mail)

        dragTo(centreOf(compose.ringSlot(clock)))
        rest(FOLD_MILLIS)
        letGo()

        compose.runOnIdle { assertEquals(HomeApps(ring = Ring(listOf(folderOf(clock, mail)))), homeApps) }
    }

    @Test
    fun aDockAppRestingOnAnotherFoldsIntoADockFolderThatOpensOnTheRing() {
        val other = alphabet[0]
        apps = listOf(clock, mail, other)
        homeApps = HomeApps(ring = ringOf(other), dock = ringOf(clock, mail))
        show()

        pickUp(compose.dockSlot(clock))
        dragTo(centreOf(compose.dockSlot(mail)))
        rest(FOLD_MILLIS)
        compose.dockSlot(mail).assert(hasStateDescription(FOLD_TARGET))
        letGo()

        compose.runOnIdle { assertEquals(HomeApps(ring = ringOf(other), dock = Ring(listOf(folderOf(mail, clock)))), homeApps) }
        compose.dockFolder(0).assertContentDescriptionEquals("Folder, 2 apps").performClick()
        compose.emblem().assertDoesNotExist()
        compose.ringSlot(other).assertDoesNotExist()
        compose.ringSlot(mail).assertIsDisplayed()
        compose.ringSlot(clock).performClick()
        compose.runOnIdle { assertEquals(listOf(clock), launched) }

        compose.closeFolder().performClick()
        compose.ringSlot(other).assertIsDisplayed()
        compose.dockFolder(0).assertIsDisplayed()
    }

    @Test
    fun aDockFolderOpensOutOfItsOwnSlotInTheDock() {
        val (first, second, third) = alphabet.take(3)
        apps = listOf(clock, mail, first, second, third)
        homeApps = HomeApps(dock = Ring(listOf(folderOf(first, second), RingSlot.App(clock.key), RingSlot.App(mail.key), RingSlot.App(third.key))))
        show()
        val folder = centreOf(compose.dockFolder(0))
        val middle = centreOf(compose.dock()).x
        compose.mainClock.autoAdvance = false

        compose.dockFolder(0).performClick()
        compose.mainClock.advanceTimeByFrame()

        val leaving = centreOf(compose.ringSlot(first)).x
        assertTrue("the first app starts at its folder, not the middle", abs(leaving - folder.x) < abs(leaving - middle))
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun aTapOnAnOpenDockFolderClosesIt() {
        apps = listOf(clock, mail)
        homeApps = HomeApps(dock = Ring(listOf(folderOf(clock, mail))))
        show()
        compose.dockFolder(0).performClick()
        compose.ringSlot(clock).assertIsDisplayed()

        compose.dockFolder(0).performClick()

        compose.emblem().assertIsDisplayed()
        compose.ringSlot(clock).assertDoesNotExist()
    }

    // A folder is named by its slot: the dock app ahead of it going would otherwise open the next folder in its stead.
    @Test
    fun removingADockAppAheadOfAnOpenDockFolderClosesIt() {
        val (first, second) = alphabet.take(2)
        apps = listOf(clock, mail, first, second)
        homeApps = HomeApps(dock = Ring(listOf(RingSlot.App(clock.key), folderOf(mail), folderOf(first, second))))
        show()
        compose.dockFolder(1).performClick()
        compose.ringSlot(mail).assertIsDisplayed()

        compose.dockSlot(clock).performTouchInput { longClick() }
        compose.onNodeWithText("Remove from the dock").performClick()

        compose.runOnIdle { assertEquals(Ring(listOf(folderOf(mail), folderOf(first, second))), homeApps.dock) }
        compose.emblem().assertIsDisplayed()
        compose.ringSlot(first).assertDoesNotExist()
    }

    @Test
    fun aDockFolderMovesAlongTheDockWithItsApps() {
        val other = alphabet[0]
        apps = listOf(clock, mail, other)
        homeApps = HomeApps(ring = ringOf(other), dock = Ring(listOf(folderOf(clock, mail), RingSlot.App(other.key))))
        show()

        pickUp(compose.dockFolder(0))
        dragTo(centreOf(compose.dockSlot(other)))
        letGo()

        compose.runOnIdle { assertEquals(Ring(listOf(RingSlot.App(other.key), folderOf(clock, mail))), homeApps.dock) }
    }

    @Test
    fun newFolderFromADockAppStartsOneInItsDockSlot() {
        homeApps = HomeApps(dock = ringOf(clock, mail))
        show()

        compose.dockSlot(mail).performTouchInput { longClick() }
        compose.onNodeWithText("New folder").performClick()

        assertDrawerOpen(true)
        compose.onNodeWithText("Adding to folder").assertIsDisplayed()
        compose.onNodeWithText("Clock").performClick()
        compose.runOnIdle { assertEquals(Ring(listOf(RingSlot.App(clock.key), folderOf(mail, clock))), homeApps.dock) }

        Espresso.pressBack()
        assertDrawerOpen(false)
        compose.dockFolder(1).assertContentDescriptionEquals("Folder, 2 apps").assertIsDisplayed()
    }

    @Test
    fun aSecondFingerFlipsTheSwitchToSwapWhileTheFirstHoldsTheApp() {
        val four = alphabet.take(4)
        apps = four
        homeApps = HomeApps(ring = ringOf(*four.toTypedArray()))
        show()

        pickUp(compose.ringSlot(four[0]))
        val target = centreOf(compose.ringSlot(four[2]))
        val swap = centreOf(compose.reorderModeButton(ReorderMode.Swap))
        compose.onRoot().performTouchInput {
            down(1, swap)
            up(1)
        }
        compose.runOnIdle { assertEquals(ReorderMode.Swap, reorderMode) }
        compose.reorderModeButton(ReorderMode.Swap).assertIsSelected()
        dragTo(target)
        letGo()

        compose.runOnIdle { assertEquals(ringOf(four[2], four[1], four[0], four[3]), homeApps.ring) }
    }

    @Test
    fun theHeldAppRestingOnTheSwitchsOtherHalfFlipsItButPassingOverDoesNot() {
        val four = alphabet.take(4)
        apps = four
        homeApps = HomeApps(ring = ringOf(*four.toTypedArray()))
        show()
        val target = centreOf(compose.ringSlot(four[2]))

        pickUp(compose.ringSlot(four[0]))
        val swap = centreOf(compose.reorderModeButton(ReorderMode.Swap))
        compose.mainClock.autoAdvance = false
        dragTo(swap)
        compose.mainClock.advanceTimeBy(SWITCH_HOVER_MILLIS / 2)
        dragTo(target)
        compose.mainClock.advanceTimeBy(SWITCH_HOVER_MILLIS)
        compose.runOnIdle { assertEquals(ReorderMode.Insert, reorderMode) }

        dragTo(swap)
        compose.mainClock.advanceTimeBy(SWITCH_HOVER_MILLIS + 100)
        compose.runOnIdle { assertEquals(ReorderMode.Swap, reorderMode) }
        compose.mainClock.autoAdvance = true
        dragTo(target)
        letGo()

        compose.runOnIdle { assertEquals(ringOf(four[2], four[1], four[0], four[3]), homeApps.ring) }
    }

    @Test
    fun aDockAppMovesAlongTheDockAndTheRingStaysPut() {
        val three = alphabet.take(3)
        apps = three
        homeApps = HomeApps(ring = ringOf(three[0]), dock = ringOf(*three.toTypedArray()))
        show()

        pickUp(compose.dockSlot(three[0]))
        dragTo(centreOf(compose.dockSlot(three[2])))
        letGo()

        compose.runOnIdle {
            assertEquals(HomeApps(ring = ringOf(three[0]), dock = ringOf(three[1], three[2], three[0])), homeApps)
        }
    }

    // Folders do not nest, so a folder resting on an app's middle folds nothing.
    @Test
    fun aFolderMovesRoundTheRingWithItsAppsAndAnOpenFoldersAppsMoveWithinIt() {
        val (first, second) = alphabet.take(2)
        apps = listOf(clock, mail, first, second)
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(first.key), folderOf(clock, mail, second))))
        show()

        pickUp(compose.folderSlot(1))
        compose.folderOptionsMenu().assertDoesNotExist()
        dragTo(centreOf(compose.ringSlot(first)))
        rest(FOLD_MILLIS)
        compose.ringSlot(first).assert(hasStateDescription(FOLD_TARGET).not())
        letGo()
        compose.runOnIdle { assertEquals(Ring(listOf(folderOf(clock, mail, second), RingSlot.App(first.key))), homeApps.ring) }

        compose.folderSlot(0).performClick()
        pickUp(compose.ringSlot(clock))
        dragTo(centreOf(compose.ringSlot(second)))
        letGo()

        compose.runOnIdle { assertEquals(Ring(listOf(folderOf(mail, second, clock), RingSlot.App(first.key))), homeApps.ring) }
        compose.closeFolder().assertIsDisplayed()
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
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), work)), dock = ringOf(mail))
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

        compose.longPressEmptyHomeSpace()

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
    fun aLongPressOnTheEmblemOpensTheLauncherMenuAtItsCentreRatherThanThePicker() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()

        compose.emblem().performTouchInput { longClick() }

        // On screen, since the menu is a window of its own. It may open above or below the spot, so only its span is checked.
        val emblem = compose.emblem().fetchSemanticsNode()
        val centre = emblem.positionOnScreen + emblem.size.center.toOffset()
        val menu = compose.launcherMenu().fetchSemanticsNode()
        val slack = with(compose.density) { 16.dp.toPx() }
        assertEquals(centre.x, menu.positionOnScreen.x, slack)
        assertTrue(centre.y in menu.positionOnScreen.y - slack..menu.positionOnScreen.y + menu.size.height + slack)
        assertDrawerOpen(false)
    }

    // Flipped twice, so a row that kept the style it first showed would ask for the same one again.
    @Test
    fun theLauncherMenusClockRowShowsTheClocksHourStyleAndATapFlipsIt() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("24-hour clock").assertIsOn()

        compose.onNodeWithText("24-hour clock").performClick()

        compose.launcherMenu().assertDoesNotExist()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("24-hour clock").assertIsOff()
        compose.onNodeWithText("24-hour clock").performClick()
        compose.runOnIdle { assertEquals(listOf(false, true), hourStylesChosen) }
    }

    @Test
    fun theLauncherMenusAmbientMotionRowShowsTheSettingAndATapFlipsIt() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Ambient motion").assertIsOn().performClick()

        compose.runOnIdle { assertFalse(ambientMotion) }
    }

    @Test
    fun theLauncherMenusAppNamesRowNamesTheAppsOnTheRingAndInAnOpenFolder() {
        homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(mail.key), folderOf(clock))))
        fun nameOn(app: AppEntry) = compose.onNode(hasText(app.label) and hasAnyAncestor(hasTestTag(HomeRingTags.slot(app))), useUnmergedTree = true)
        show()
        nameOn(mail).assertDoesNotExist()

        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Names on the ring").assertIsOff().performClick()

        compose.runOnIdle { assertTrue(appNames) }
        nameOn(mail).assertExists()
        compose.onNodeWithTag(HomeRingTags.folder(1)).performClick()
        nameOn(clock).assertExists()
    }

    @Test
    fun theLauncherMenusOpenOnHomeRowFlipsWhetherTheLauncherComesBackOnTheHomePage() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Open on home page").assertIsOff().performClick()

        compose.runOnIdle { assertTrue(homeOnReturn) }
    }

    @Test
    fun theLauncherComesBackOnThePageLeftUnlessSetToOpenOnTheHomePage() {
        show()
        goToCollections()

        leaveAndReturn()
        assertSettledOn(LauncherPage.Collections)

        homeOnReturn = true
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)
        // Before the activity saves its state, which follows the stop at once.
        compose.runOnIdle {
            sight.lifecycle.currentState = Lifecycle.State.CREATED
            assertEquals(layout.homeIndex, pager.currentPage)
        }
        compose.runOnIdle { sight.lifecycle.currentState = Lifecycle.State.RESUMED }
        assertSettledOn(LauncherPage.Home)
        assertDrawerOpen(false)
    }

    @Test
    fun anErrandOfTheLaunchersOwnComesBackToItsPageOnceEvenWhenSetToOpenOnTheHomePage() {
        homeOnReturn = true
        collections = CollectionsPage()
        show()
        goToCollections()

        compose.onNodeWithText("Permission Required").performClick()
        leaveAndReturn()
        assertSettledOn(LauncherPage.Collections)

        leaveAndReturn()
        assertSettledOn(LauncherPage.Home)
    }

    @Test
    fun theLauncherMenusUpdateRowShowsWhetherItChecksAndATapFlipsIt() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Check for updates").assertIsOn()

        compose.onNodeWithText("Check for updates").performClick()

        compose.runOnIdle { assertFalse(checkForUpdates) }
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Check for updates").assertIsOff()
    }

    @Test
    fun theLauncherMenusFolderLookRowShowsTheLookAndItsDialogChangesIt() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Folder look").assert(hasText("Solar system")).performClick()
        compose.onNodeWithTag(LauncherMenuTags.LOOK_DIALOG).assertIsDisplayed()

        compose.onNodeWithText("Moons in orbit").performClick()

        compose.onNodeWithTag(LauncherMenuTags.LOOK_DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertEquals(FolderLook.Orbit, folderLook) }
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Folder look").assert(hasText("Moons in orbit"))
    }

    @Test
    fun theLauncherMenusThemeRowTriesThemesAndTheirFolderLooksOnHomeUntilOneIsUsed() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").assert(hasText("Space")).performClick()

        compose.onNodeWithTag(ThemePreviewTags.BAR).assertIsDisplayed()
        compose.onNodeWithText(face.time).assertDoesNotExist()
        compose.onNodeWithText("Solar system folders").assertIsSelected()
        compose.onNodeWithText("Clockwork").performClick()
        compose.runOnIdle { assertEquals(Theme.Clockwork, previewTheme) }
        compose.onNodeWithText("Solar system folders").assertDoesNotExist()
        compose.onNodeWithText("Gear folders").performClick()
        compose.onNodeWithText("Brass").assertIsSelected()
        compose.onNodeWithText("Steel").performClick()
        compose.onNodeWithText("Steel").assertIsSelected()
        compose.runOnIdle {
            assertEquals(FolderLook.Gear, folderLooks.of(Theme.Clockwork))
            assertEquals(Colourway.Steel, colourways.of(Theme.Clockwork))
            assertEquals(Theme.Space, theme)
        }

        compose.onNodeWithTag(ThemePreviewTags.USE).assert(hasText("Use Clockwork")).performClick()

        compose.onNodeWithTag(ThemePreviewTags.BAR).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(Theme.Clockwork, theme)
            assertEquals(null, previewTheme)
        }
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").assert(hasText("Clockwork"))
    }

    @Test
    fun aSidewaysDragOnTheThemeChipsScrollsThemPastTheirEndsWithoutMovingThePage() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").performClick()
        val chip = centreOf(compose.onNodeWithText("Space"))

        // Right first, from the start of the row, where it has nothing to scroll; then left past its end.
        listOf(1f, -1f, -1f, -1f, -1f).forEach { direction ->
            compose.onRoot().performTouchInput {
                down(chip)
                repeat(10) { moveBy(Offset(direction * width / 20f, 0f)) }
            }
            compose.runOnIdle { assertEquals(0f, pager.currentPageOffsetFraction) }
            compose.onRoot().performTouchInput { up() }
        }

        assertSettledOn(LauncherPage.Home)
        compose.onNodeWithText("Green core").assertIsDisplayed()
    }

    @Test
    fun aThemeWithASceneOffersToSetItAsTheWallpaperOnceADialogHasAsked() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("as wallpaper", substring = true).assertDoesNotExist()

        art = CrystalArt
        compose.onNodeWithText("Use the crystal city as wallpaper").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertTrue(wallpapersSet.isEmpty()) }

        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Use the crystal city as wallpaper").performClick()
        compose.onNodeWithText("Set").performClick()
        compose.runOnIdle {
            val scene = wallpapersSet.single().toPixelMap()
            assertEquals(1f, scene[scene.width / 2, scene.height / 2].alpha)
        }
    }

    @Test
    fun backLeavesATryOfAThemeOnlyOnceTheFolderIsClosedAndCloseAndHomeLeaveItToo() {
        homeApps = HomeApps(ring = Ring(listOf(work)))
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("Clockwork").performClick()
        compose.folderSlot(0).performClick()
        compose.emblem().assertDoesNotExist()

        Espresso.pressBack()

        compose.emblem().assertIsDisplayed()
        compose.onNodeWithTag(ThemePreviewTags.BAR).assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithTag(ThemePreviewTags.BAR).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(null, previewTheme)
            assertEquals(Theme.Space, theme)
        }
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithTag(ThemePreviewTags.CLOSE).performClick()
        compose.onNodeWithTag(ThemePreviewTags.BAR).assertDoesNotExist()

        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").performClick()
        pressHome(launcherInFront = true)
        compose.onNodeWithTag(ThemePreviewTags.BAR).assertDoesNotExist()
        compose.runOnIdle { assertEquals(null, previewTheme) }
    }

    @Test
    fun whileAThemeIsTriedTheMenuShowsItAndOffersItsFolderLooks() {
        folderLooks = FolderLooks().with(Theme.Clockwork, FolderLook.Gear)
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("Clockwork").performClick()

        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Theme").assert(hasText("Clockwork"))
        compose.onNodeWithText("Folder look").assert(hasText("Gear")).performClick()
        compose.onNodeWithText("Sub-dial").performClick()

        compose.runOnIdle {
            assertEquals(FolderLook.SubDial, folderLooks.of(Theme.Clockwork))
            assertEquals(FolderLook.Rim, folderLooks.of(Theme.Space))
            assertEquals(Theme.Clockwork, previewTheme)
        }
    }

    @Test
    fun homeClosesTheDrawersSortMenu() {
        show()
        compose.drawerHandle().performClick()
        compose.onNodeWithContentDescription("Sort apps").performClick()
        compose.onNodeWithTag(AppDrawerTags.SORT_MENU).assertIsDisplayed()
        pressHome(launcherInFront = true)

        compose.onNodeWithTag(AppDrawerTags.SORT_MENU).assertDoesNotExist()
    }

    // A tap while the launcher fades back in is taken by the wallpaper, not by the app fading in under it.
    @Test
    fun theLauncherMenusWallpaperRowHidesTheLauncherAndTheBarsUntilTheNextTap() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()
        val mailAt = centreOf(compose.ringSlot(mail))
        compose.longPressEmptyHomeSpace()

        compose.onNodeWithText("Showcase wallpaper").performClick()

        compose.pager().assertDoesNotExist()
        compose.runOnIdle { assertEquals(false, barsShown.last()) }
        compose.mainClock.autoAdvance = false
        compose.wallpaper().performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onRoot().performTouchInput { click(mailAt) }
        compose.mainClock.autoAdvance = true

        compose.wallpaper().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(emptyList<AppEntry>(), launched)
            assertEquals(true, barsShown.last())
        }
        compose.onRoot().performTouchInput { click(mailAt) }
        compose.runOnIdle { assertEquals(listOf(mail), launched) }
    }

    @Test
    fun backAndHomeBringTheLauncherBackOverTheWallpaper() {
        show()
        for (bringBack in listOf({ Espresso.pressBack() }, { pressHome(launcherInFront = true) })) {
            compose.longPressEmptyHomeSpace()
            compose.onNodeWithText("Showcase wallpaper").performClick()
            compose.pager().assertDoesNotExist()

            bringBack()

            compose.pager().assertIsDisplayed()
            compose.wallpaper().assertDoesNotExist()
            compose.runOnIdle { assertEquals(true, barsShown.last()) }
        }
    }

    @Test
    fun theLauncherMenusRestartRowIsAPlainButtonThatRestartsAtOnce() {
        show()
        compose.longPressEmptyHomeSpace()
        for (action in listOf("Restart launcher", "Reset launcher")) {
            compose.onNodeWithText(action).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        }

        compose.onNodeWithText("Restart launcher").performClick()

        compose.launcherMenu().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, restarts)
            assertEquals(0, resets)
        }
    }

    @Test
    fun theLauncherMenusResetRowAsksFirstAndOnlyTheDialogsResetResets() {
        show()
        openResetDialog()

        compose.onNodeWithText("Cancel").performClick()

        compose.resetDialog().assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, resets) }
        openResetDialog()

        compose.onNodeWithText("Reset").performClick()

        compose.resetDialog().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, resets)
            assertEquals(0, restarts)
        }
    }

    @Test
    fun homeClosesTheResetDialogWithoutResetting() {
        show()
        openResetDialog()

        pressHome(launcherInFront = true)

        compose.resetDialog().assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, resets) }
    }

    @Test
    fun aPinRequestClosesWhatIsOpenOnTheHomePageAndAddPutsTheShortcutAtTheEndOfTheRing() {
        homeApps = HomeApps(ring = ringOf(clock))
        show()
        goToCollections()
        compose.drawerHandle().performClick()
        assertDrawerOpen(true)

        requestPin()

        assertSettledOn(LauncherPage.Home)
        assertDrawerOpen(false)
        compose.pinDialog().assertIsDisplayed()
        compose.onNodeWithText("Squoosh").assertIsDisplayed()
        compose.onNodeWithText("Shortcut from Browser").assertIsDisplayed()

        compose.onNodeWithText("Add").performClick()

        compose.pinDialog().assertDoesNotExist()
        compose.ringSlot(squoosh).assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, accepts)
            assertEquals(HomeApps(ring = ringOf(clock, squoosh)), homeApps)
        }
    }

    @Test
    fun cancelAndARefusedPinLeaveTheRingAlone() {
        homeApps = HomeApps(ring = ringOf(clock))
        show()
        requestPin()

        compose.onNodeWithText("Cancel").performClick()

        compose.pinDialog().assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, accepts) }
        requestPin(refused = true)

        compose.onNodeWithText("Add").performClick()

        compose.pinDialog().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, accepts)
            assertEquals(0, homeAppsChanges)
        }
    }

    @Test
    fun homeClosesThePinDialogWithoutAdding() {
        show()
        requestPin()
        compose.pinDialog().assertIsDisplayed()

        pressHome(launcherInFront = false)

        compose.pinDialog().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, accepts)
            assertEquals(0, homeAppsChanges)
        }
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
        compose.longPressEmptyHomeSpace()
        compose.launcherMenu().assertIsDisplayed()

        Espresso.pressBack()
        compose.launcherMenu().assertDoesNotExist()
        assertSettledOn(LauncherPage.Home)

        compose.longPressEmptyHomeSpace()
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
    fun aCardsGearSetsItsOwnLookOrTheDefaultsOnTabsASwipeMovesBetween() {
        val tools = CollectionKind.Category(AppCategory.Tools)
        collections = CollectionsPage().add(tools)
        show()
        goToCollections()

        compose.collectionSettingsButton(MostUsed).performClick()
        compose.onNodeWithTag(CollectionTags.SETTINGS_DIALOG).assertIsDisplayed()
        compose.onNodeWithText("App limit").assertIsDisplayed()
        compose.onNodeWithTag(CollectionTags.choice(CardSetting.Rows, 3)).performClick()
        compose.runOnIdle { assertEquals(CardLook(rows = 3), collections.look(collections.card(MostUsed)!!)) }
        compose.onNodeWithTag(CollectionTags.useDefault(CardSetting.Rows)).assertIsOff().performClick()
        compose.runOnIdle { assertEquals(CollectionsPage().add(tools), collections) }

        compose.onNodeWithTag(CollectionTags.SETTINGS_PAGES).performTouchInput { swipeLeft() }
        compose.onNodeWithTag(CollectionTags.choice(CardSetting.Rows, 2)).performClick()
        compose.runOnIdle { assertEquals(CollectionsPage().add(tools).setDefault(CardSetting.Rows, 2), collections) }
        compose.onNodeWithTag(CollectionTags.SETTINGS_PAGES).performTouchInput { swipeRight() }
        compose.onNodeWithTag(CollectionTags.useDefault(CardSetting.Rows)).assertIsOn()
        compose.onNodeWithText("Defaults").performClick()
        compose.onNodeWithTag(CollectionTags.useDefault(CardSetting.Rows)).assertIsNotDisplayed()
        assertSettledOn(LauncherPage.Collections)
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithTag(CollectionTags.SETTINGS_DIALOG).assertDoesNotExist()

        // A card the user fills has no limit of its own to set.
        compose.collectionSettingsButton(tools).performClick()
        compose.onNodeWithText("App limit").assertDoesNotExist()
        compose.onNodeWithTag(CollectionTags.useDefault(CardSetting.Names)).assertIsOn()
    }

    @Test
    fun aCardsGearRemovesItOnlyOnceTheQuestionIsAnswered() {
        val work = CollectionKind.Custom("Work")
        collections = CollectionsPage().add(work)
        show()
        goToCollections()

        compose.collectionSettingsButton(work).performClick()
        compose.onNodeWithText("Defaults").performClick()
        compose.onNodeWithTag(CollectionTags.REMOVE).performClick()
        compose.onNodeWithText("Remove Work?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Every collection that uses the default follows these.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(CollectionsPage().add(work), collections) }

        compose.onNodeWithTag(CollectionTags.REMOVE).performClick()
        compose.onNodeWithText("Remove").performClick()
        compose.onNodeWithTag(CollectionTags.REMOVE_DIALOG).assertDoesNotExist()
        compose.onNodeWithTag(CollectionTags.SETTINGS_DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertEquals(CollectionsPage(), collections) }
        assertSettledOn(LauncherPage.Collections)
    }

    @Test
    fun aSwatchGivesOneCardItsOwnBackgroundColour() {
        collections = CollectionsPage()
        show()
        goToCollections()

        compose.collectionSettingsButton(MostUsed).performClick()
        val teal = compose.onNodeWithTag(CollectionTags.choice(CardSetting.Colour, CardColour.Teal))
        teal.performScrollTo().performClick()
        teal.assertIsSelected()
        compose.runOnIdle {
            assertEquals(CardColour.Teal, collections.look(collections.card(MostUsed)!!).colour)
            assertEquals(CardColour.Plain, collections.look(collections.card(NewApps)!!).colour)
        }
    }

    @Test
    fun createYourOwnNamesAnEmptyCardWhoseTileTakesItAwayForGoodOnceThePickerCloses() {
        collections = CollectionsPage()
        val finance = CollectionKind.Custom("Finance")
        show()
        goToCollections()
        compose.addCollectionButton().performClick()

        compose.createCollectionTile().performClick()
        compose.createCollectionName().performTextInput("tools")
        compose.onNodeWithText("Ok").assertIsNotEnabled()
        compose.createCollectionName().performTextReplacement(" Finance ")
        compose.onNodeWithText("Ok").performClick()

        compose.createCollectionDialog().assertDoesNotExist()
        compose.pickerTile(finance).assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(NewApps, MostUsed, finance), collections.cards.map { it.kind }) }

        compose.collectionTile(finance).performClick()
        compose.runOnIdle { assertEquals(CollectionsPage(), collections) }
        Espresso.pressBack()
        compose.addCollectionButton().performScrollTo().performClick()

        // Scrolled to the end of the grid, where the tile would be.
        compose.createCollectionTile()
        compose.collectionTile(finance).assertDoesNotExist()
    }

    @Test
    fun aCustomTileTappedOffStaysInPlaceUnlitAndASecondTapPutsItsCardBackWithItsApps() {
        val bills = CollectionKind.Custom("Bills")
        val trips = CollectionKind.Custom("Trips")
        collections = CollectionsPage(listOf(CollectionCard(bills, Favourites(listOf(mail.key))), CollectionCard(trips, Favourites(listOf(clock.key)))))
        show()
        goToCollections()
        compose.addCollectionButton().performClick()
        val place = compose.pickerTile(bills).getUnclippedBoundsInRoot()

        compose.collectionTile(bills).performClick()

        compose.collectionTile(bills).assertIsNotSelected()
        assertEquals(place, compose.collectionTile(bills).getUnclippedBoundsInRoot())
        compose.collectionTile(trips).assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(trips), collections.cards.map { it.kind }) }

        compose.collectionTile(bills).performClick()

        compose.collectionTile(bills).assertIsSelected()
        compose.runOnIdle {
            assertEquals(listOf(trips, bills), collections.cards.map { it.kind })
            assertEquals(Favourites(listOf(mail.key)), collections.card(bills)!!.apps)
            assertEquals(Favourites(listOf(clock.key)), collections.card(trips)!!.apps)
        }
    }

    @Test
    fun pickingForACustomCardSurvivesTheScreenBeingRecreated() {
        val bills = CollectionKind.Custom("Bills")
        collections = CollectionsPage(listOf(CollectionCard(bills)))
        val restoration = StateRestorationTester(compose)
        show(restoration = restoration)
        goToCollections()
        compose.collectionEditButton(bills).performClick()

        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText("Adding to Bills").assertIsDisplayed()
        compose.drawerRow("Mail").performClick()
        compose.runOnIdle { assertEquals(Favourites(listOf(mail.key)), collections.card(bills)!!.apps) }
    }

    @Test
    fun aPanelFillsTheNavigationBarBelowIt() {
        show()
        goToCollections()

        compose.addCollectionButton().performClick()

        val bottom = compose.collectionPicker().fetchSemanticsNode().boundsInRoot.bottom.toInt()
        val window = compose.onRoot().captureToImage()
        assertTrue("No navigation bar below the panel", bottom < window.height)
        val edge = window.toPixelMap(startX = 1, startY = bottom - 1, width = 1, height = window.height - bottom + 1)
        assertEquals(edge[0, 0], edge[0, edge.height - 1])
    }

    @Test
    fun thePencilPicksForTheCardWithItsAppsCheckedAndATapTogglesOne() {
        collections = CollectionsPage().add(tools).addApp(tools, mail)
        show()
        goToCollections()

        compose.collectionEditButton(tools).performClick()

        compose.onNodeWithText("Adding to Tools").assertIsDisplayed()
        compose.drawerRow("Mail").assertIsOn()
        compose.drawerRow("Clock").assertIsOff()
        compose.drawerRow("Clock").performClick()
        compose.drawerRow("Mail").performClick()
        compose.drawerRow("Clock").assertIsOn()
        compose.drawerRow("Mail").assertIsOff()
        compose.runOnIdle {
            assertEquals(Favourites(listOf(clock.key)), collections.card(tools)!!.apps)
            assertEquals(emptyList<AppEntry>(), launched)
        }

        Espresso.pressBack()

        compose.placePicker().assertDoesNotExist()
        assertSettledOn(LauncherPage.Collections)
        compose.collectionApp(tools, clock).assertIsDisplayed()
        compose.collectionApp(tools, mail).assertDoesNotExist()
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
    fun aLongPressOnACardsAppOpensItsMenuThereAlone() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key))), CollectionCard(NewApps)))
        show()
        goToCollections()

        compose.collectionApp(tools, mail).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(viewConfiguration.touchSlop / 2, 0f))
            up()
        }
        compose.dragGhost().assertDoesNotExist()
        compose.collectionBin().assertDoesNotExist()
        compose.onNodeWithText("Remove from Tools").assertIsDisplayed()
        compose.onAllNodesWithText("Hide from New & Most Used").assertCountEquals(1)
        compose.onNodeWithText("Remove from Tools").performClick()

        compose.appOptionsMenu().assertDoesNotExist()
        compose.collectionApp(tools, mail).assertDoesNotExist()
        compose.runOnIdle { assertEquals(Favourites(), collections.card(tools)!!.apps) }
    }

    @Test
    fun movingOnFromACardAppsOpenMenuLiftsTheAppAndClosesTheMenu() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key, clock.key)))))
        show()
        goToCollections()
        val icon = compose.collectionApp(tools, mail)

        icon.performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, 1f))
        }
        compose.appOptionsMenu().assertIsDisplayed()
        icon.performTouchInput { moveBy(Offset(0f, viewConfiguration.touchSlop * 2)) }

        compose.appOptionsMenu().assertDoesNotExist()
        compose.dragGhost().assertIsDisplayed()
        compose.collectionBin().assertIsDisplayed()
        letGo()
    }

    @Test
    fun anAppLiftedOntoAnotherOnItsCardTakesItsPlace() {
        val other = alphabet[0]
        apps = listOf(clock, mail, other)
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key, clock.key, other.key)))))
        show()
        goToCollections()

        liftFromToolsCard(mail)
        compose.reorderSwitch().assertIsDisplayed()
        dragTo(centreOf(compose.collectionApp(tools, clock)))
        letGo()

        compose.runOnIdle { assertEquals(Favourites(listOf(clock.key, mail.key, other.key)), collections.card(tools)!!.apps) }
    }

    // One layout holds every row, so the app keeps its gesture as the others make way for it from row to row.
    @Test
    fun anAppMovesFromRowToRowOnAnExpandedCard() {
        val seven = alphabet.take(7)
        apps = seven
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(seven.map { it.key }), expanded = true)))
        show()
        goToCollections()
        val target = centreOf(compose.collectionApp(tools, seven[6]))

        liftFromToolsCard(seven[0])
        dragTo(target)
        rest(MAKE_WAY_MILLIS)
        compose.dragGhost().assertIsDisplayed()
        assertNear(target, centreOf(compose.collectionApp(tools, seven[0])))
        letGo()

        compose.runOnIdle { assertEquals(Favourites((seven.drop(1) + seven[0]).map { it.key }), collections.card(tools)!!.apps) }
    }

    @Test
    fun aSecondFingerHoldingAnotherAppWhileOneIsLiftedLeavesTheFirstInCharge() {
        collections = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key, clock.key)))))
        show()
        goToCollections()
        val other = centreOf(compose.collectionApp(tools, clock))

        liftFromToolsCard(mail)
        compose.onRoot().performTouchInput {
            down(1, other)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(1, Offset(0f, viewConfiguration.touchSlop * 2))
            up(1)
        }
        dragTo(centreOf(compose.collectionBin()))
        letGo()

        compose.runOnIdle { assertEquals(Favourites(listOf(clock.key)), collections.card(tools)!!.apps) }
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
    fun homeEndsPickingForACardAndClosesThePicker() {
        collections = CollectionsPage().add(tools)
        show()
        goToCollections()
        compose.collectionEditButton(tools).performClick()
        compose.placePicker().assertIsDisplayed()

        pressHome(launcherInFront = true)

        compose.placePicker().assertDoesNotExist()
        assertSettledOn(LauncherPage.Home)

        goToCollections()
        compose.addCollectionButton().performClick()
        compose.createCollectionTile().performClick()
        compose.createCollectionDialog().assertIsDisplayed()
        pressHome(launcherInFront = false)

        compose.createCollectionDialog().assertDoesNotExist()
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
    fun anywhereOnTheHomePageASwipeDownOpensTheNotificationsAndASwipeUpTheDrawerAndNothingElse() {
        homeApps = HomeApps(ring = ringOf(mail), dock = ringOf(clock))
        isHomeApp = false
        show()

        val onContent = listOf(compose.clockTime(), compose.ringSlot(mail), compose.emblem(), compose.homeAppCard(), compose.dockSlot(clock))
        (listOf(compose.emptyHomeSpace()) + onContent.map(::centreOf)).forEach { start ->
            swipeFrom(start, down = true)
            swipeFrom(start, down = false)
            assertDrawerOpen(true)
            Espresso.pressBack()
            assertDrawerOpen(false)
        }

        compose.runOnIdle {
            assertEquals(6, notificationsOpened)
            assertEquals(emptyList<AppEntry>(), launched)
            assertEquals(emptyList<String>(), opened)
            assertEquals(0, homeRequests)
        }
    }

    @Test
    fun aPageSwipeThatDriftsUpOrDownChangesThePageWithoutOpeningTheNotificationsOrTheDrawer() {
        show()

        compose.swipePager { swipe(center, center + Offset(-width / 2f, height / 10f)) }
        assertSettledOn(LauncherPage.Collections)
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Home)
        compose.swipePager { swipe(center, center + Offset(width / 2f, -height / 10f)) }
        assertSettledOn(LauncherPage.Widgets)

        compose.runOnIdle { assertEquals(0, notificationsOpened) }
        assertDrawerOpen(false)
    }

    @Test
    fun aSwipeDownThatDriftsAcrossKeepsThePage() {
        show()
        val start = compose.emptyHomeSpace()

        compose.onRoot().performTouchInput {
            down(start)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3))
            repeat(10) { moveBy(Offset(width / 20f, height / 40f)) }
            up()
        }

        assertSettledOn(LauncherPage.Home)
        compose.runOnIdle { assertEquals(1, notificationsOpened) }
    }

    @Test
    fun aReorderThatDriftsAcrossKeepsThePage() {
        collections = CollectionsPage().add(tools)
        show()
        goToCollections()
        val handle = centreOf(compose.collectionHandle(tools))

        compose.onRoot().performTouchInput {
            down(handle)
            moveBy(Offset(0f, -viewConfiguration.touchSlop * 2))
            repeat(10) { moveBy(Offset(width / 20f, 0f)) }
            up()
        }

        assertSettledOn(LauncherPage.Collections)
    }

    @Test
    fun theFirstFrameHoldsOnlyTheHomePageAndTheOthersFollow() {
        compose.mainClock.autoAdvance = false
        show()

        compose.page(LauncherPage.Home).assertExists()
        compose.page(LauncherPage.Widgets).assertDoesNotExist()

        compose.mainClock.autoAdvance = true
        compose.page(LauncherPage.Widgets).assertExists()
    }

    @Test
    fun aResizeThatDriftsAcrossKeepsThePage() {
        widgetPage = WidgetPage(listOf(search))
        show()
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)
        val row = compose.widgetHeight(search)
        compose.longPressWidget(search)
        val handle = centreOf(compose.widgetResizeHandle())

        compose.onRoot().performTouchInput {
            down(handle)
            moveBy(Offset(0f, row.toPx() * 1.7f))
            repeat(10) { moveBy(Offset(-width / 20f, 0f)) }
        }
        // Within a pixel, as a stretched row need not be a whole one.
        assertEquals(widgetSpan(3, row).value, compose.widgetHeight(search).value, 1f)
        compose.onRoot().performTouchInput { up() }

        assertSettledOn(LauncherPage.Widgets)
    }

    @Test
    fun aLongPressThatMovesDownPicksTheAppUpRatherThanOpeningTheNotifications() {
        homeApps = HomeApps(ring = ringOf(mail))
        show()
        val icon = centreOf(compose.ringSlot(mail))

        compose.onRoot().performTouchInput {
            down(icon)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            // Past the slop, which makes it a swipe, but short of the dock, where the app would go.
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3))
            up()
        }

        compose.appOptionsMenu().assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, notificationsOpened)
            assertEquals(HomeApps(ring = ringOf(mail)), homeApps)
        }
    }

    @Test
    fun theWidgetPageAddsWidgetsAndBackHomeOrLeavingThePageEndsEditing() {
        widgetPage = WidgetPage(listOf(search))
        show()
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)

        compose.addWidgetButton().performClick()
        compose.runOnIdle { assertEquals(1, widgetsAdded.size) }
        assertTrue("the page holds ${widgetsAdded.single()} rows", widgetsAdded.single() >= 4)

        compose.longPressWidget(search)
        compose.widgetEditFrame().assertIsDisplayed()
        Espresso.pressBack()
        compose.widgetEditFrame().assertDoesNotExist()
        assertSettledOn(LauncherPage.Widgets)

        compose.longPressWidget(search)
        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Home)
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)
        compose.widgetEditFrame().assertDoesNotExist()

        compose.longPressWidget(search)
        pressHome(launcherInFront = false)
        compose.widgetEditFrame().assertDoesNotExist()
        assertSettledOn(LauncherPage.Widgets)
    }

    @Test
    fun backInTheMiddleOfAResizePutsTheWidgetBack() {
        widgetPage = WidgetPage(listOf(search))
        show()
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)
        val row = compose.widgetHeight(search)
        compose.longPressWidget(search)

        compose.widgetResizeHandle().performTouchInput {
            down(center)
            moveBy(Offset(0f, row.toPx() * 1.7f))
        }
        assertEquals(widgetSpan(3, row).value, compose.widgetHeight(search).value, 1f)
        Espresso.pressBack()

        compose.widgetEditFrame().assertDoesNotExist()
        assertEquals(row, compose.widgetHeight(search))
        assertSettledOn(LauncherPage.Widgets)
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

        compose.waitOutWidgetLongPress()
        compose.widgetEditFrame().assertDoesNotExist()
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

    private fun onPage(page: LauncherPage, tag: String) =
        compose.onNode(hasTestTag(tag) and hasAnyAncestor(hasTestTag(LauncherTags.page(page))))

    @Test
    fun anAppTakenToAnotherRingPageByTheScreensEdgeMovesThere() {
        val second = LauncherPage("ring-2", PageKind.Ring)
        layout = PageLayout(listOf(LauncherPage.Home, second))
        homeApps = HomeApps(ring = ringOf(mail, clock))
        show()
        val edge = compose.onRoot().fetchSemanticsNode().size.width - 2f

        pickUp(compose.ringSlot(mail))
        dragTo(Offset(edge, centreOf(onPage(LauncherPage.Home, HomeRingTags.EMBLEM)).y))
        rest(PAGE_TURN_MILLIS)
        assertSettledOn(second)
        dragTo(centreOf(onPage(second, HomeRingTags.EMBLEM)))
        letGo()

        compose.runOnIdle {
            assertEquals(ringOf(clock), ringPages.on(LauncherPage.Home.id).ring)
            assertEquals(ringOf(mail), ringPages.on(second.id).ring)
        }
    }

    @Test
    fun anotherRingPageHasItsOwnRingWithoutTheClockOrTheDockAndAppsPickedThereStayThere() {
        val second = LauncherPage("ring-2", PageKind.Ring)
        layout = PageLayout(listOf(LauncherPage.Home, second))
        homeApps = HomeApps(ring = ringOf(clock), dock = ringOf(clock))
        show()
        compose.swipePager { swipeLeft() }
        assertSettledOn(second)
        onPage(second, HomeClockTags.TIME).assertDoesNotExist()
        onPage(second, DockTags.DOCK).assertDoesNotExist()

        onPage(second, HomeRingTags.EMBLEM).performClick()
        assertDrawerOpen(true)
        compose.drawerRow("Mail").performClick()
        Espresso.pressBack()

        onPage(second, HomeRingTags.slot(mail)).assertIsDisplayed()
        onPage(second, HomeRingTags.slot(clock)).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(ringOf(mail), ringPages.on(second.id).ring)
            assertEquals(HomeApps(ring = ringOf(clock), dock = ringOf(clock)), homeApps)
        }
    }

    @Test
    fun aFolderOpensOnItsOwnRingPageAloneAndBackClosesItBeforeGoingHome() {
        val second = LauncherPage("ring-2", PageKind.Ring)
        layout = PageLayout(listOf(LauncherPage.Home, second))
        ringPages = RingPages(rings = mapOf(LauncherPage.Home.id to Ring(listOf(work)), second.id to Ring(listOf(work))))
        show()
        compose.swipePager { swipeLeft() }
        assertSettledOn(second)

        onPage(second, HomeRingTags.folder(0)).performClick()
        onPage(second, HomeRingTags.EMBLEM).assertDoesNotExist()
        onPage(LauncherPage.Home, HomeRingTags.EMBLEM).assertExists()

        Espresso.pressBack()
        onPage(second, HomeRingTags.EMBLEM).assertIsDisplayed()
        assertSettledOn(second)
    }

    @Test
    fun editPagesInTheLauncherMenuAddsAPageWhereTheEditorSaysAndATapOnItGoesThere() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Edit pages").assert(hasText("3 pages")).performClick()

        compose.onNodeWithTag(PageEditorTags.add(atStart = false)).performScrollTo().performClick()
        compose.addPageOf(PageKind.Ring)
        val added = layout.pages.last()
        assertEquals(PageKind.Ring, added.kind)
        compose.onNodeWithTag(PageEditorTags.page(added)).performScrollTo().performClick()

        compose.onNodeWithTag(PageEditorTags.EDITOR).assertDoesNotExist()
        assertSettledOn(added)
    }

    @Test
    fun backAndHomeCloseThePageEditorAndAPageAddedBeforeTheOneInViewLeavesItInView() {
        show()
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Edit pages").performClick()
        compose.onNodeWithTag(PageEditorTags.add(atStart = true)).performClick()
        compose.addPageOf(PageKind.Widgets)

        Espresso.pressBack()

        compose.onNodeWithTag(PageEditorTags.EDITOR).assertDoesNotExist()
        assertSettledOn(LauncherPage.Home)
        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("Edit pages").performClick()
        pressHome(launcherInFront = true)
        compose.onNodeWithTag(PageEditorTags.EDITOR).assertDoesNotExist()
    }

    @Test
    fun aDragFromTheDrawerAfterAPageIsAddedBeforeHomeStaysOnHome() {
        show()
        layout = layout.add(PageKind.Ring, atStart = true)

        startDraggingFromDrawer(mail)
        dragTo(centreOf(onPage(LauncherPage.Home, HomeRingTags.EMBLEM)))
        letGo()

        assertSettledOn(LauncherPage.Home)
        compose.runOnIdle { assertTrue(mail.key in homeApps.keys) }
    }

    @Test
    fun pageDotsShowWhileSwipingOnlyOnceAPageIsTwoSwipesFromHome() {
        show()
        compose.swipePager { down(center); moveBy(Offset(-width / 4f, 0f)) }
        compose.onNodeWithTag(PAGE_DOTS).assertDoesNotExist()
        compose.swipePager { up() }

        layout = layout.add(PageKind.Ring, atStart = false)
        compose.swipePager { down(center); moveBy(Offset(-width / 4f, 0f)) }
        compose.onNodeWithTag(PAGE_DOTS).assertIsDisplayed()
        compose.swipePager { up() }
    }

    @Test
    fun anAppOnTwoRingPagesOpensOneMenu() {
        val second = LauncherPage("ring-2", PageKind.Ring)
        layout = PageLayout(listOf(LauncherPage.Home, second))
        ringPages = RingPages(rings = mapOf(LauncherPage.Home.id to ringOf(mail), second.id to ringOf(mail)))
        show()

        onPage(LauncherPage.Home, HomeRingTags.slot(mail)).performTouchInput { longClick() }

        compose.appOptionsMenu().assertIsDisplayed()
    }

    @Test
    fun eachCollectionsPageKeepsItsOwnCards() {
        val second = LauncherPage("collections-2", PageKind.Collections)
        layout = PageLayout(listOf(LauncherPage.Home, LauncherPage.Collections, second))
        val page = CollectionsPage(listOf(CollectionCard(tools, Favourites(listOf(mail.key)))))
        collectionPages = CollectionPages(mapOf(LauncherPage.Collections.id to page, second.id to page))
        show()
        compose.swipePager { swipeLeft() }
        compose.swipePager { swipeLeft() }
        assertSettledOn(second)

        onPage(second, CollectionTags.chevron(tools)).performClick()

        compose.runOnIdle {
            assertTrue(collectionPages.on(second.id).card(tools)!!.expanded)
            assertFalse(collections.card(tools)!!.expanded)
        }
    }

    @Test
    fun aWidgetIsAddedToTheWidgetPageInView() {
        val second = LauncherPage("widgets-2", PageKind.Widgets)
        layout = PageLayout(listOf(LauncherPage.Widgets, LauncherPage.Home, second))
        show()
        compose.swipePager { swipeLeft() }
        assertSettledOn(second)

        onPage(second, WidgetTags.ADD).performClick()

        compose.runOnIdle { assertEquals(listOf(second.id), widgetPagesAddedTo) }
    }
}
