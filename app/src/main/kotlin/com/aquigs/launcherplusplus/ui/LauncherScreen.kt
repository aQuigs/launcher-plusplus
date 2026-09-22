package com.aquigs.launcherplusplus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppCategory
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.AppOption
import com.aquigs.launcherplusplus.domain.AppShortcut
import com.aquigs.launcherplusplus.domain.Bounds
import com.aquigs.launcherplusplus.domain.ClockFace
import com.aquigs.launcherplusplus.domain.CollectionKind
import com.aquigs.launcherplusplus.domain.CollectionsPage
import com.aquigs.launcherplusplus.domain.DropZones
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.ForegroundTime
import com.aquigs.launcherplusplus.domain.HomeApps
import com.aquigs.launcherplusplus.domain.HomePlace
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import com.aquigs.launcherplusplus.domain.Ring
import com.aquigs.launcherplusplus.domain.RingItem
import com.aquigs.launcherplusplus.domain.RingerMode
import com.aquigs.launcherplusplus.domain.UnreadCounts
import com.aquigs.launcherplusplus.domain.WidgetPage
import com.aquigs.launcherplusplus.domain.appOptions
import com.aquigs.launcherplusplus.domain.seedCategory
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
 * the calendar, and the emblem opens the drawer to pick the apps on the ring or in the dock. Under the date, a tap on
 * the [ringerMode] calls [onRingerTap], which steps the ringer on or asks for the access that needs. Until [isHomeApp],
 * a card over the ring says so and offers [onBecomeHomeApp]. A swipe down from anywhere on the home page above the dock
 * pulls down the notification shade ([onOpenNotifications]). Long-pressing an app anywhere opens its menu of shortcuts and
 * options; a ring app's menu can start a folder in its slot. A folder opens in place, as in Arc: its apps take the ring's
 * slots and the emblem makes way for a target that closes it; its own menu fills it from the drawer or removes it. A long
 * press in the drawer that moves on drags the app out: the drawer closes, a ghost of the icon follows the finger over the
 * home page, and letting go over the ring or the dock adds it there. The widget page shows [widgetPage] through
 * [widgets], and a widget's long-press menu removes it. The collections page shows the cards of [collections], the
 * built-in ones filled from the app list and [foregroundTime] (null until usage access is granted, which
 * [onOpenUsageSettings] asks for); a category card's pencil opens an editor over the screen that adds apps to it, and
 * the button under the cards opens the picker that adds and removes cards. An app long-pressed on a category card lifts
 * off it, and dropping it on the bin takes it off the card. Apps everywhere wear their [unread] counts; a long press on
 * the home page's empty space opens the launcher's own menu, whose one row shows whether the badges are enabled
 * ([badgesEnabled]) and opens the system screen that decides it ([onOpenBadgeSettings]). [apps] is null until the
 * installed apps have loaded. Every [HomePress] cancels a drag and closes the menu, the drawer, the editor, the picker
 * and the folder; one made while the launcher was in front also scrolls to the home page. Back undoes what is on top:
 * it cancels a drag, else closes the menu, then the drawer, then the editor or the picker, then returns to the home
 * page, then closes the folder.
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
    ringerMode: RingerMode,
    onRingerTap: () -> Unit,
    isHomeApp: Boolean,
    onBecomeHomeApp: () -> Unit,
    widgetPage: WidgetPage,
    widgets: WidgetActions,
    collections: CollectionsPage,
    onCollectionsChange: (CollectionsPage) -> Unit,
    foregroundTime: ForegroundTime?,
    onOpenUsageSettings: () -> Unit,
    unread: UnreadCounts,
    badgesEnabled: Boolean,
    onOpenBadgeSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = layout.homeIndex) { layout.pages.size },
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val drawerState = rememberStandardBottomSheetState(skipHiddenState = true)
    val drawerOpen = drawerState.targetValue == SheetValue.Expanded
    val ring = remember(homeApps.ring, apps) { homeApps.ring.resolve(apps.orEmpty()) }
    val dock = remember(homeApps.dock, apps) { homeApps.dock.resolve(apps.orEmpty()) }
    val latestHomeApps by rememberUpdatedState(homeApps)
    val latestOnHomeAppsChange by rememberUpdatedState(onHomeAppsChange)
    val latestCollections by rememberUpdatedState(collections)
    val latestOnCollectionsChange by rememberUpdatedState(onCollectionsChange)
    val latestBadgesEnabled by rememberUpdatedState(badgesEnabled)
    val latestOnOpenBadgeSettings by rememberUpdatedState(onOpenBadgeSettings)

    // Callbacks built once read the home apps through the latest state, so what they change is always the current ring.
    fun changeHomeApps(change: HomeApps.() -> HomeApps) {
        val changed = latestHomeApps.change()
        if (changed != latestHomeApps) latestOnHomeAppsChange(changed)
    }

    fun changeCollections(change: CollectionsPage.() -> CollectionsPage) {
        val changed = latestCollections.change()
        if (changed != latestCollections) latestOnCollectionsChange(changed)
    }

    // Explicit receiver: the resolved ring above shadows the stored one inside the lambda.
    fun changeRing(change: Ring.() -> Ring) = changeHomeApps { copy(ring = this.ring.change()) }

    // An app on its way out of the drawer or off a collection card, the finger holding it and where it could land, all
    // in root coordinates. The finger moves every frame, so only the ghost reads it; the targets under it are derived,
    // so the ring, the dock and the bin recompose when the finger changes zone, not whenever it moves.
    var dragged by remember { mutableStateOf<Drag?>(null) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var zones by remember { mutableStateOf(DropZones()) }
    var binBounds by remember { mutableStateOf<Bounds?>(null) }
    val dropPlace by remember { derivedStateOf { if (dragged is Drag.FromDrawer) zones.placeAt(finger.x, finger.y) else null } }
    val overBin by remember { derivedStateOf { dragged is Drag.FromCard && binBounds?.discContains(finger.x, finger.y) == true } }

    // Picking and searching are modes of the drawer, so they end however the drawer closes: chevron, drag, Back or HOME.
    // They wait for the drawer to settle closed: a drag moves the target back and forth, and the drawer may still end up
    // open. Dropping focus takes the keyboard down with the drawer. An app dragged out of a search waits too: ending the
    // search would take away the row it left, and the gesture with it, while the finger is still down.
    var picking by rememberSaveable { mutableStateOf<HomePlace?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val drawerSettledClosed = drawerState.currentValue == SheetValue.PartiallyExpanded && !drawerOpen && dragged == null
    LaunchedEffect(drawerSettledClosed) {
        if (drawerSettledClosed) {
            picking = null
            query = ""
            focusManager.clearFocus()
        }
    }
    // The editor and the picker each cover the whole screen until Back or HOME. The editor marks the apps tapped while it
    // was open, and starts clean each time; dropping focus takes its keyboard down with it.
    var editing by rememberSaveable { mutableStateOf<AppCategory?>(null) }
    var pickingCollection by rememberSaveable { mutableStateOf(false) }
    var justPicked by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var editorQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(editing) {
        if (editing == null) {
            justPicked = emptySet()
            editorQuery = ""
            focusManager.clearFocus()
        }
    }
    // The menu is a focusable popup window, so Back reaches its onDismissRequest before this screen's BackHandler. The
    // open folder is part of the ring, named by its slot, so Back reaches it through that handler.
    var openMenu by remember { mutableStateOf<OpenMenu?>(null) }
    var openingMenu by remember { mutableStateOf<Job?>(null) }
    var openFolder by remember { mutableStateOf<Int?>(null) }
    val open = ring.filterIsInstance<RingItem.Folder>().find { it.index == openFolder }
    // A slot number outliving its folder would open a folder made later in that slot by itself.
    LaunchedEffect(open == null) { if (open == null) openFolder = null }

    // Each animation gets its own job: a drag in progress cancels it, and that must not stop the collector.
    fun openDrawer() = scope.launch { drawerState.expand() }
    fun closeDrawer() = scope.launch { drawerState.partialExpand() }
    fun goHome() = scope.launch { pagerState.animateScrollToPage(layout.homeIndex) }

    fun pickFor(place: HomePlace) {
        picking = place
        openDrawer()
    }

    // A menu still loading its shortcuts is cancelled too, or HOME pressed meanwhile would not stop it opening afterwards.
    fun closeMenu() {
        openingMenu?.cancel()
        openMenu = openMenu?.closed()
    }

    fun appMenu(place: HomePlace?) = AppMenu(
        onOpen = { app ->
            openingMenu?.cancel()
            openingMenu = scope.launch { openMenu = OpenMenu.App(app, place, actions.shortcuts(app)) }
        },
        content = { app ->
            (openMenu as? OpenMenu.App)?.takeIf { it.isFor(app, place) }?.let { shown ->
                // An app that leaves the screen takes its popup away undismissed; without closing here the menu would reopen
                // by itself when the app returns, as after an update.
                DisposableEffect(Unit) {
                    onDispose { if ((openMenu as? OpenMenu.App)?.isFor(app, place) == true) closeMenu() }
                }
                AppOptionsMenu(
                    expanded = shown.expanded,
                    shortcuts = shown.shortcuts,
                    options = appOptions(app, place),
                    shortcutIcon = actions.shortcutIcon,
                    onShortcut = actions.startShortcut,
                    onOption = { option ->
                        when (option) {
                            is AppOption.Remove -> changeHomeApps { toggle(option.place, app) }
                            AppOption.NewFolder -> {
                                val slot = latestHomeApps.ring.indexOf(app)
                                if (slot >= 0) {
                                    changeRing { newFolder(app) }
                                    pickFor(HomePlace.Folder(slot))
                                }
                            }
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
    val folderAppMenu = remember(actions, open?.index) { open?.let { appMenu(HomePlace.Folder(it.index)) } }
    val folderMenu = remember {
        FolderMenu(
            onOpen = { folder ->
                openingMenu?.cancel()
                openMenu = OpenMenu.Folder(folder.index)
            },
            content = { folder ->
                (openMenu as? OpenMenu.Folder)?.takeIf { it.index == folder.index }?.let { shown ->
                    DisposableEffect(Unit) {
                        onDispose { if ((openMenu as? OpenMenu.Folder)?.index == folder.index) closeMenu() }
                    }
                    FolderOptionsMenu(
                        expanded = shown.expanded,
                        onOption = { option ->
                            when (option) {
                                FolderOption.AddApps -> pickFor(HomePlace.Folder(folder.index))
                                FolderOption.Remove -> {
                                    // The next folder along inherits this slot's number, and would inherit the fading menu too.
                                    openMenu = null
                                    changeRing { remove(folder.index) }
                                }
                            }
                        },
                        onDismiss = ::closeMenu,
                    )
                }
            },
        )
    }

    val dragFromDrawer = remember {
        AppDrag(
            onStart = { app, position ->
                closeMenu()
                dragged = Drag.FromDrawer(app)
                finger = position
                // The targets are on the home page, wherever the drawer was opened from.
                closeDrawer()
                goHome()
            },
            onMove = { finger = it },
            onDrop = {
                val app = dragged?.app
                val place = dropPlace
                if (app != null && place != null) changeHomeApps { add(place, app) }
                dragged = null
            },
            onCancel = { dragged = null },
        )
    }

    val cardLift = remember {
        CardLift(
            onStart = { category, app, position ->
                closeMenu()
                // The lift is the answer to the long press, as the menu is elsewhere, so it gets the same nudge.
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                dragged = Drag.FromCard(app, category)
                finger = position
            },
            onMove = { finger = it },
            onDrop = {
                val lifted = dragged as? Drag.FromCard
                if (lifted != null && overBin) changeCollections { removeApp(CollectionKind.Category(lifted.category), lifted.app) }
                dragged = null
            },
            onCancel = { dragged = null },
        )
    }

    val widgetMenu = remember(widgets) {
        WidgetMenu(
            onOpen = { widget ->
                openingMenu?.cancel()
                openMenu = OpenMenu.Widget(widget.id)
            },
            content = { widget ->
                (openMenu as? OpenMenu.Widget)?.takeIf { it.id == widget.id }?.let { shown ->
                    DisposableEffect(Unit) {
                        onDispose { if ((openMenu as? OpenMenu.Widget)?.id == widget.id) closeMenu() }
                    }
                    WidgetOptionsMenu(expanded = shown.expanded, onRemove = { widgets.remove(widget.id) }, onDismiss = ::closeMenu)
                }
            },
        )
    }

    val launcherMenu = remember {
        LauncherMenu(
            onOpen = {
                openingMenu?.cancel()
                openMenu = OpenMenu.Launcher()
            },
            content = {
                (openMenu as? OpenMenu.Launcher)?.let { shown ->
                    LauncherOptionsMenu(
                        expanded = shown.expanded,
                        rows = listOf(
                            LauncherMenuRow("Unread badges", on = latestBadgesEnabled, onClick = { latestOnOpenBadgeSettings() }),
                        ),
                        onDismiss = ::closeMenu,
                    )
                }
            },
        )
    }

    LaunchedEffect(homePresses, pagerState, drawerState, layout) {
        homePresses.collect { press ->
            dragged = null
            closeMenu()
            openFolder = null
            closeDrawer()
            editing = null
            pickingCollection = false
            // Coming back from an app keeps the page you left, like the stock launcher.
            if (press.launcherInFront) goHome()
        }
    }
    // One handler with the order spelled out, instead of one per dismissable relying on composition order. A search is
    // not a rung of its own: the keyboard takes the first Back, and closing the drawer or the editor ends the search.
    // The open folder comes last because the drawer and the other pages both hide it, and a press should undo something
    // in view.
    val overlayOpen = editing != null || pickingCollection
    BackHandler(enabled = dragged != null || drawerOpen || overlayOpen || pagerState.currentPage != layout.homeIndex || open != null) {
        when {
            dragged != null -> dragged = null
            drawerOpen -> closeDrawer()
            editing != null -> editing = null
            pickingCollection -> pickingCollection = false
            pagerState.currentPage != layout.homeIndex -> goHome()
            else -> openFolder = null
        }
    }

    // The ghost is drawn over the sheet too, so it is a sibling of the scaffold rather than in its body. It gets the
    // finger in root coordinates, which this box may not start at: the origin is measured after [modifier], so it is
    // the padded content's, where the ghost is placed.
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
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
                    drag = dragFromDrawer,
                    query = query,
                    onQueryChange = { query = it },
                    unread = unread,
                    picking = picking?.let { place ->
                        val picked = homeApps[place]
                        Picking(
                            header = {
                                when (place) {
                                    is HomePlace.Folder -> FolderPicker()
                                    HomePlace.Ring, HomePlace.Dock -> PlacePicker(place = place, onPlaceChange = { picking = it })
                                }
                            },
                            isPicked = { it in picked },
                            onToggle = { onHomeAppsChange(homeApps.toggle(place, it)) },
                        )
                    },
                )
            },
            // The collapsed sheet is full height and continues below the scaffold, where the list would show through the
            // navigation-bar inset.
            modifier = Modifier.clipToBounds(),
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
                            LauncherPage.Home -> Box(Modifier.fillMaxSize().swipeDown(onOpenNotifications)) {
                                // First, so it lies behind the clock, the card and the ring and gets only the touches they leave.
                                EmptySpace(menu = launcherMenu)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HomeClock(
                                        face = clock,
                                        onTimeClick = onOpenClock,
                                        onDateClick = onOpenCalendar,
                                        modifier = Modifier.padding(top = 24.dp),
                                    )
                                    RingerSwitch(mode = ringerMode, onClick = onRingerTap)
                                    // Nothing dismisses the card: a launcher that is not the home app is not doing its job.
                                    if (!isHomeApp) {
                                        HomeAppCard(
                                            onBecomeHomeApp = onBecomeHomeApp,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                        )
                                    }
                                    HomeRing(
                                        ring = ring,
                                        // Slots stored for the ring hold the hint back until the app list can say none of their
                                        // apps is installed, so neither the hint nor the mark flashes while apps load.
                                        showHint = homeApps.ring.isEmpty || (apps != null && ring.isEmpty()),
                                        icon = actions.icon,
                                        onLaunch = actions.launch,
                                        onOpenFolder = { openFolder = it.index },
                                        onCloseFolder = { openFolder = null },
                                        onEdit = { pickFor(HomePlace.Ring) },
                                        modifier = Modifier.weight(1f).onGloballyPositioned { zones = zones.copy(ring = it.rootBounds()) },
                                        highlighted = dropPlace == HomePlace.Ring,
                                        openFolder = open,
                                        menu = ringMenu,
                                        folderMenu = folderMenu,
                                        folderAppMenu = folderAppMenu,
                                        unread = unread,
                                    )
                                }
                            }
                            LauncherPage.Widgets -> {
                                WidgetColumn(page = widgetPage, view = widgets.view, onAdd = widgets.add, menu = widgetMenu)
                            }
                            LauncherPage.Collections -> {
                                CollectionsColumn(
                                    page = collections,
                                    apps = apps.orEmpty(),
                                    foregroundTime = foregroundTime,
                                    icon = actions.icon,
                                    onLaunch = actions.launch,
                                    onToggleExpanded = { kind -> changeCollections { toggleExpanded(kind) } },
                                    onMove = { from, to -> changeCollections { move(from, to) } },
                                    onEdit = { editing = it },
                                    onAdd = { pickingCollection = true },
                                    onOpenUsageSettings = onOpenUsageSettings,
                                    lift = cardLift,
                                    bin = if (dragged is Drag.FromCard) BinTarget(overBin, onPositioned = { binBounds = it }) else null,
                                    unread = unread,
                                )
                            }
                        }
                    }
                }
                // Outside the pager, so it stays put while the pages swipe. Stored dock apps hold its row until the app list
                // loads, so the pages do not move when it arrives. An empty dock shows while an app is dragged, as a place to
                // drop it, and slides in and out so the ring above moves rather than jumps.
                val dockShown = dock.isNotEmpty() || (apps == null && homeApps.dock.keys.isNotEmpty()) || dragged is Drag.FromDrawer
                AnimatedVisibility(visible = dockShown) {
                    Dock(
                        apps = dock,
                        icon = actions.icon,
                        onLaunch = actions.launch,
                        modifier = Modifier.onGloballyPositioned { zones = zones.copy(dock = it.rootBounds()) },
                        highlighted = dropPlace == HomePlace.Dock,
                        menu = dockMenu,
                        unread = unread,
                    )
                }
            }
        }
        // Over the scaffold, drawer strip included: each is a screen of its own until Back or HOME.
        editing?.let { category ->
            CollectionEditor(
                title = "Add to ${category.label}",
                apps = apps.orEmpty(),
                isPicked = { it.key in justPicked },
                onPick = { app ->
                    // Marked whether or not it was already in the card, so the tap is seen to have counted either way.
                    justPicked = justPicked + app.key
                    changeCollections { addApp(CollectionKind.Category(category), app) }
                },
                query = editorQuery,
                onQueryChange = { editorQuery = it },
            )
        }
        if (pickingCollection) {
            CollectionPicker(
                page = collections,
                onToggle = { kind ->
                    changeCollections {
                        if (kind in this) {
                            remove(kind)
                        } else {
                            add(kind, (kind as? CollectionKind.Category)?.let { seedCategory(it.category, apps.orEmpty()) } ?: Favourites())
                        }
                    }
                },
            )
        }
        dragged?.let { drag ->
            DragGhost(drag.app, actions.icon, position = { finger - origin }, label = (drag as? Drag.FromCard)?.app?.label)
        }
    }
}

/** An app being dragged, and where from. */
private sealed interface Drag {
    val app: AppEntry

    /** Out of the drawer, to the ring or the dock. */
    data class FromDrawer(override val app: AppEntry) : Drag

    /** Off the card of [category], to the bin. */
    data class FromCard(override val app: AppEntry, val category: AppCategory) : Drag
}

/** The long-press menu that is showing. It keeps what it shows while [expanded] turns false, so it animates away whole. */
private sealed interface OpenMenu {
    val expanded: Boolean

    fun closed(): OpenMenu

    /** [app]'s menu, where it was pressed (null for the drawer), and its shortcuts. */
    data class App(
        val app: AppEntry,
        val place: HomePlace?,
        val shortcuts: List<AppShortcut>,
        override val expanded: Boolean = true,
    ) : OpenMenu {
        // By key, so a reload that relabels the app keeps its menu.
        fun isFor(app: AppEntry, place: HomePlace?) = app.key == this.app.key && place == this.place

        override fun closed() = copy(expanded = false)
    }

    /** The menu of the folder in the ring's slot [index]. */
    data class Folder(val index: Int, override val expanded: Boolean = true) : OpenMenu {
        override fun closed() = copy(expanded = false)
    }

    /** The menu of the widget [id]. */
    data class Widget(val id: Int, override val expanded: Boolean = true) : OpenMenu {
        override fun closed() = copy(expanded = false)
    }

    /** The launcher's own menu, from the home page's empty space. */
    data class Launcher(override val expanded: Boolean = true) : OpenMenu {
        override fun closed() = copy(expanded = false)
    }
}

// Unclipped, so a ring scrolled off the page keeps its true bounds instead of collapsing onto the edge it left by.
internal fun LayoutCoordinates.rootBounds(): Bounds {
    val topLeft = positionInRoot()
    return Bounds(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height)
}
