package com.aquigs.launcherplusplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppOption
import com.aquigs.launcherplusplus.domain.AppShortcut
import com.aquigs.launcherplusplus.domain.ClockFace
import com.aquigs.launcherplusplus.domain.HomeApps
import com.aquigs.launcherplusplus.domain.HomePlace
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.domain.appOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object LauncherTags {
    const val PAGER = "launcher_pager"

    fun page(page: LauncherPage) = "page_${page.name}"
}

/** A HOME press. [launcherInFront] is false when the press brought the launcher back from another app. */
data class HomePress(val launcherInFront: Boolean)

/**
 * The whole launcher: a horizontal pager over [layout] with the dock under it and the app drawer peeking below as a
 * chevron. The home page shows the [clock] over the ring from [homeApps]: the time and the date open the clock app and
 * the calendar, and the emblem opens the drawer to pick the apps on the ring or in the dock. Long-pressing an app
 * anywhere opens its menu of shortcuts and options. [apps] is null until the installed apps have loaded. Every
 * [HomePress] closes the menu and the drawer; one made while the launcher was in front also scrolls to the home page.
 * Back closes the menu, then the drawer, then returns to the home page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    layout: PageLayout,
    homePresses: Flow<HomePress>,
    apps: List<AppEntry>?,
    homeApps: HomeApps,
    onHomeAppsChange: (HomeApps) -> Unit,
    actions: AppActions,
    clock: ClockFace,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = layout.homeIndex) { layout.pages.size },
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberStandardBottomSheetState(skipHiddenState = true)
    val drawerOpen = drawerState.targetValue == SheetValue.Expanded
    val ring = remember(homeApps.ring, apps) { homeApps.ring.resolve(apps.orEmpty()) }
    val dock = remember(homeApps.dock, apps) { homeApps.dock.resolve(apps.orEmpty()) }
    // Picking is a mode of the drawer, so it ends however the drawer closes: chevron, drag, Back or HOME. It waits for the
    // drawer to settle closed: a drag moves the target back and forth, and the drawer may still end up open.
    var picking by rememberSaveable { mutableStateOf<HomePlace?>(null) }
    val drawerSettledClosed = drawerState.currentValue == SheetValue.PartiallyExpanded && !drawerOpen
    LaunchedEffect(drawerSettledClosed) { if (drawerSettledClosed) picking = null }
    // The menu is a focusable popup window, so Back reaches its onDismissRequest before this screen's BackHandler.
    var openMenu by remember { mutableStateOf<OpenMenu?>(null) }
    var openingMenu by remember { mutableStateOf<Job?>(null) }
    val latestHomeApps by rememberUpdatedState(homeApps)
    val latestOnHomeAppsChange by rememberUpdatedState(onHomeAppsChange)

    // Each animation gets its own job: a drag in progress cancels it, and that must not stop the collector.
    fun openDrawer() = scope.launch { drawerState.expand() }
    fun closeDrawer() = scope.launch { drawerState.partialExpand() }
    fun goHome() = scope.launch { pagerState.animateScrollToPage(layout.homeIndex) }

    // A menu still loading its shortcuts is cancelled too, or HOME pressed meanwhile would not stop it opening afterwards.
    fun closeMenu() {
        openingMenu?.cancel()
        openMenu = openMenu?.copy(expanded = false)
    }

    fun appMenu(place: HomePlace?) = AppMenu(
        onOpen = { app ->
            openingMenu?.cancel()
            openingMenu = scope.launch { openMenu = OpenMenu(app, place, actions.shortcuts(app)) }
        },
        content = { app ->
            openMenu?.takeIf { it.isFor(app, place) }?.let { shown ->
                // An app that leaves the screen takes its popup away undismissed; without closing here the menu would reopen
                // by itself when the app returns, as after an update.
                DisposableEffect(Unit) {
                    onDispose { if (openMenu?.isFor(app, place) == true) closeMenu() }
                }
                AppOptionsMenu(
                    expanded = shown.expanded,
                    shortcuts = shown.shortcuts,
                    options = appOptions(app, place),
                    shortcutIcon = actions.shortcutIcon,
                    onShortcut = actions.startShortcut,
                    onOption = { option ->
                        when (option) {
                            is AppOption.Remove -> latestOnHomeAppsChange(latestHomeApps.toggle(option.place, app))
                            AppOption.AppInfo -> actions.openAppInfo(app)
                            AppOption.Uninstall -> actions.uninstall(app)
                        }
                    },
                    onDismiss = ::closeMenu,
                )
            }
        },
    )
    // Built once, so the ring, the dock and the drawer can skip recomposing while only the page or the drawer moves.
    val drawerMenu = remember(actions) { appMenu(place = null) }
    val ringMenu = remember(actions) { appMenu(HomePlace.Ring) }
    val dockMenu = remember(actions) { appMenu(HomePlace.Dock) }

    LaunchedEffect(homePresses, pagerState, drawerState, layout) {
        homePresses.collect { press ->
            closeMenu()
            closeDrawer()
            // Coming back from an app keeps the page you left, like the stock launcher.
            if (press.launcherInFront) goHome()
        }
    }
    // One handler with the order spelled out, instead of one per dismissable relying on composition order.
    BackHandler(enabled = drawerOpen || pagerState.currentPage != layout.homeIndex) {
        if (drawerOpen) closeDrawer() else goHome()
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(drawerState),
        sheetPeekHeight = 48.dp,
        sheetDragHandle = { DrawerHandle(open = drawerOpen) },
        // Translucent so the wallpaper still shows through the open drawer.
        sheetContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        containerColor = Color.Transparent,
        sheetContent = {
            AppDrawer(
                apps = apps.orEmpty(),
                onLaunch = actions.launch,
                menu = drawerMenu,
                picking = picking?.let { place ->
                    Picking(
                        header = { PlacePicker(place = place, onPlaceChange = { picking = it }) },
                        isPicked = { it in homeApps[place] },
                        onToggle = { onHomeAppsChange(homeApps.toggle(place, it)) },
                    )
                },
            )
        },
        // The collapsed sheet is full height and continues below the scaffold, where the list would show through the
        // navigation-bar inset.
        modifier = modifier.clipToBounds(),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // The pages and the dock fade as the drawer rises, so only the wallpaper shows through the translucent
                // drawer. Together they are exactly as tall as the drawer travels.
                .graphicsLayer {
                    if (size.height > 0f) alpha = (drawerState.requireOffset() / size.height).coerceIn(0f, 1f)
                },
        ) {
            HorizontalPager(
                state = pagerState,
                key = { layout.pages[it].name },
                // Every page stays composed, so swiping back does not rebuild the ring and reload its icons.
                beyondViewportPageCount = layout.pages.size - 1,
                modifier = Modifier.weight(1f).testTag(LauncherTags.PAGER),
            ) { index ->
                val page = layout.pages[index]
                Box(Modifier.fillMaxSize().testTag(LauncherTags.page(page))) {
                    when (page) {
                        LauncherPage.Home -> Column {
                            HomeClock(
                                face = clock,
                                onTimeClick = onOpenClock,
                                onDateClick = onOpenCalendar,
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp),
                            )
                            HomeRing(
                                ring = ring,
                                // Favourites stored for the ring hold the hint back until the app list can say none of them
                                // is installed, so neither the hint nor the mark flashes while apps load.
                                showHint = homeApps.ring.keys.isEmpty() || (apps != null && ring.isEmpty()),
                                icon = actions.icon,
                                onLaunch = actions.launch,
                                onEdit = {
                                    picking = HomePlace.Ring
                                    openDrawer()
                                },
                                modifier = Modifier.weight(1f),
                                menu = ringMenu,
                            )
                        }
                        LauncherPage.Widgets, LauncherPage.Collections -> PlaceholderPage(page)
                    }
                }
            }
            // Outside the pager, so it stays put while the pages swipe. Stored dock apps hold its row until the app list
            // loads, so the pages do not move when it arrives.
            if (dock.isNotEmpty() || (apps == null && homeApps.dock.keys.isNotEmpty())) {
                Dock(apps = dock, icon = actions.icon, onLaunch = actions.launch, menu = dockMenu)
            }
        }
    }
}

/** The app whose menu is showing, where it was pressed (null for the drawer), and its shortcuts. */
private data class OpenMenu(val app: AppEntry, val place: HomePlace?, val shortcuts: List<AppShortcut>, val expanded: Boolean = true) {
    // By key, so a reload that relabels the app keeps its menu.
    fun isFor(app: AppEntry, place: HomePlace?) = app.key == this.app.key && place == this.place
}

@Composable
private fun PlaceholderPage(page: LauncherPage) {
    Text(
        text = page.name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.fillMaxSize().wrapContentSize(),
    )
}
