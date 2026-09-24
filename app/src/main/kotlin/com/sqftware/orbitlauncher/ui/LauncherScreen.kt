package com.sqftware.orbitlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppOption
import com.sqftware.orbitlauncher.domain.AppShortcut
import com.sqftware.orbitlauncher.domain.Bounds
import com.sqftware.orbitlauncher.domain.ClockFace
import com.sqftware.orbitlauncher.domain.CollectionCard
import com.sqftware.orbitlauncher.domain.CollectionKind
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.DropZones
import com.sqftware.orbitlauncher.domain.Favourites
import com.sqftware.orbitlauncher.domain.ForegroundTime
import com.sqftware.orbitlauncher.domain.HomeApps
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.Ring
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.RingerMode
import com.sqftware.orbitlauncher.domain.ReorderMode
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.appOptions
import com.sqftware.orbitlauncher.domain.seedCategory
import com.sqftware.orbitlauncher.domain.title
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object LauncherTags {
    const val PAGER = "launcher_pager"

    fun page(page: LauncherPage) = "page_${page.name}"
}

private val DRAWER_PEEK = 48.dp

/** How far above the drawer's strip the shade along the bottom edge starts to darken the wallpaper. */
private val BOTTOM_SHADE_FADE = 32.dp

/** A HOME press. [launcherInFront] is false when the press brought the launcher back from another app. */
data class HomePress(val launcherInFront: Boolean)

/**
 * The whole launcher: a horizontal pager over [layout] and the app drawer peeking below as a chevron. The home page
 * shows the [clock] over the ring from [homeApps], with the dock at its foot: the time and the date open the clock app
 * and the calendar, and the emblem opens the drawer to pick the apps on the ring or in the dock. Under the date, a tap on
 * the [ringerMode] calls [onRingerTap], which steps the ringer on or asks for the access that needs. Until [isHomeApp],
 * a strip over the dock says so and offers [onBecomeHomeApp]. From anywhere on the home page, dock included, a swipe down
 * pulls down the notification shade ([onOpenNotifications]) and a swipe up opens the drawer. Long-pressing an app
 * anywhere opens its menu of shortcuts and
 * options; a ring app's menu can start a folder in its slot. A folder opens in place, as in Arc: its apps take the ring's
 * slots and the emblem makes way for a target that closes it; its own menu fills it from the drawer or removes it. A long
 * press in the drawer that moves on drags the app out: the drawer closes, a ghost of the icon follows the finger over the
 * home page, and letting go over the ring or the dock adds it there. The widget page shows [widgetPage] through
 * [widgets]; a long press puts a widget in edit mode, to resize or remove it, until a tap elsewhere or the page goes
 * out of view. The collections page shows the cards of [collections], the built-in ones filled from the app list and
 * [foregroundTime] (null until usage access is granted, which [onOpenUsageSettings] asks for); a hand-picked card's
 * pencil opens an editor over the screen that adds apps to it, and the button under the cards opens the picker that adds
 * and removes cards, whose last tile opens a dialog naming a new custom collection. An app long-pressed on a hand-picked
 * card lifts off it, and dropping it on the bin takes it off the card. A long press that moves on picks up an app or a
 * folder on the ring, an app in the open folder or the dock, or, at once, an app on a hand-picked card, to move it among
 * its neighbours: they make way as the finger goes, and letting go over another's place puts it there, inserting it or
 * swapping the two as [reorderMode] says, which a switch at the top flips ([onReorderModeChange]) while an item is on
 * the move. Letting go anywhere else leaves the order as it was. Apps everywhere wear their [unread] counts; a long
 * press on the home page's empty space opens the launcher's own menu. Its rows show whether the badges are enabled
 * ([badgesEnabled]) and open the system screen that decides it ([onOpenBadgeSettings]), show whether the clock is in 24
 * hours and flip it ([onTwentyFourHourChange]), restart the launcher ([onRestart]), and reset it ([onReset]) once a
 * dialog has asked. The ring, the dock and folders hold [pinnedShortcuts] as they hold apps; a [PinRequest] closes all
 * that is open, as HOME in front does, and asks on the home page whether to add its shortcut, which goes at the end of
 * the ring once the system has pinned it. [apps] and [pinnedShortcuts] are null until they have loaded. Every
 * [HomePress] cancels a drag, ends widget editing and closes the menu, the dialogs, the drawer, the editor, the picker
 * and the folder; one made while the launcher was in front also scrolls to the home page. Back undoes what is on top:
 * it cancels a drag, else closes the menu or a dialog, then the drawer, then the editor or the picker, then ends widget
 * editing, then returns to the home page, then closes the folder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    layout: PageLayout,
    homePresses: Flow<HomePress>,
    pinRequests: Flow<PinRequest>,
    apps: List<AppEntry>?,
    pinnedShortcuts: List<AppEntry>?,
    homeApps: HomeApps,
    onHomeAppsChange: (HomeApps) -> Unit,
    actions: AppActions,
    reorderMode: ReorderMode,
    onReorderModeChange: (ReorderMode) -> Unit,
    clock: ClockFace,
    onTwentyFourHourChange: (Boolean) -> Unit,
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
    onRestart: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(initialPage = layout.homeIndex) { layout.pages.size },
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val drawerState = rememberStandardBottomSheetState(skipHiddenState = true)
    val drawerOpen = drawerState.targetValue == SheetValue.Expanded
    // The drawer, search and the collections list the installed apps alone; home keeps shortcuts too. It waits for both,
    // so the ring does not space itself out again when the shortcuts join.
    val onHome = remember(apps, pinnedShortcuts) { if (apps != null && pinnedShortcuts != null) apps + pinnedShortcuts else null }
    val ring = remember(homeApps.ring, onHome) { homeApps.ring.resolve(onHome.orEmpty()) }
    val dock = remember(homeApps.dock, onHome) { homeApps.dock.resolve(onHome.orEmpty()) }
    val latestHomeApps by rememberUpdatedState(homeApps)
    val latestApps by rememberUpdatedState(apps)
    val latestRing by rememberUpdatedState(ring)
    val latestDock by rememberUpdatedState(dock)
    val latestReorderMode by rememberUpdatedState(reorderMode)
    val latestOnHomeAppsChange by rememberUpdatedState(onHomeAppsChange)
    val latestCollections by rememberUpdatedState(collections)
    val latestOnCollectionsChange by rememberUpdatedState(onCollectionsChange)
    val latestBadgesEnabled by rememberUpdatedState(badgesEnabled)
    val latestOnOpenBadgeSettings by rememberUpdatedState(onOpenBadgeSettings)
    val latestTwentyFourHour by rememberUpdatedState(clock.twentyFourHour)
    val latestOnTwentyFourHourChange by rememberUpdatedState(onTwentyFourHourChange)
    val latestOnRestart by rememberUpdatedState(onRestart)

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

    // An app on its way out of the drawer, or an item on the move within its place, the finger holding it and where it
    // could land, in root coordinates. The finger moves every frame, so only the ghost reads it; the targets under it are
    // derived, so the ring, the dock, the cards and the bin recompose when the finger changes zone or position, not
    // whenever it moves. The ring and the dock are measured on the home page and judged where it settles, at the pager's
    // origin: a drag from another page goes home under the finger, and a drop must not miss because the page has not
    // arrived yet.
    var dragged by remember { mutableStateOf<Drag?>(null) }
    var finger by remember { mutableStateOf(Offset.Zero) }
    var homePage by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var pagerOrigin by remember { mutableStateOf(Offset.Zero) }
    var zones by remember { mutableStateOf(DropZones()) }
    var binBounds by remember { mutableStateOf<Bounds?>(null) }
    val dropPlace by remember {
        derivedStateOf { if (dragged is Drag.FromDrawer) (finger - pagerOrigin).let { zones.placeAt(it.x, it.y) } else null }
    }

    fun Modifier.dropZone(place: DropZones.(Bounds) -> DropZones) =
        onGloballyPositioned { target -> homePage?.let { zones = zones.place(target.boundsIn(it)) } }
    val binShown by remember { derivedStateOf { (dragged as? Drag.Within)?.remove != null } }
    val overBin by remember { derivedStateOf { binShown && binBounds?.discContains(finger.x, finger.y) == true } }
    val reorderTarget by remember {
        derivedStateOf { (dragged as? Drag.Within)?.takeUnless { overBin }?.place?.at(finger) }
    }
    // A place's positions hold only while it lists what it did at the press: an app updating or going mid-drag would
    // shift them under the finger, so that ends the drag.
    val placeChanged by remember { derivedStateOf { (dragged as? Drag.Within)?.current?.invoke() == false } }
    LaunchedEffect(placeChanged) { if (placeChanged) dragged = null }

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
    // was open, and starts clean each time; dropping focus takes its keyboard down with it. The card being edited is held
    // by its stored name, which survives the activity being recreated.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingCollection by rememberSaveable { mutableStateOf(false) }
    var justPicked by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var editorQuery by rememberSaveable { mutableStateOf("") }
    // Their surfaces are translucent so the wallpaper shows through, as it does through the drawer; the pages and the
    // drawer's chevron must not, so they go while either is up. Not animated: the overlays come and go in a frame,
    // and pages fading back in would be tappable before they could be seen.
    val overlayOpen = editing != null || pickingCollection
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
    // The dialogs are windows of their own too, so like the menu they take Back before this screen's BackHandler.
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    var confirmingPin by remember { mutableStateOf<PinRequest?>(null) }
    // The widget in edit mode counts only while it is on the page and the page is in view: one gone with its provider, or
    // a long press that fired as the page left, must not leave an unseen mode taking taps and Back, nor come back with
    // the page. So whatever does not count is let go, as the mode ends when the page goes out of view.
    var editingWidget by remember { mutableStateOf<Int?>(null) }
    val onWidgetPage = layout.pages[pagerState.currentPage] == LauncherPage.Widgets
    val editedWidget = editingWidget?.takeIf { id -> onWidgetPage && widgetPage.widgets.any { it.id == id } }
    LaunchedEffect(editingWidget, editedWidget) { if (editedWidget == null) editingWidget = null }

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

    // One drag at a time: a second finger reaching for the reorder switch may long-press another item on its way.
    fun beginDrag(drag: Drag, position: Offset): Boolean {
        if (dragged != null) return false
        closeMenu()
        dragged = drag
        finger = position
        return true
    }

    val dragFromDrawer = remember {
        AppDrag(
            onStart = { app, position ->
                beginDrag(Drag.FromDrawer(app), position).also { started ->
                    // The targets are on the home page, wherever the drawer was opened from.
                    if (started) {
                        closeDrawer()
                        goHome()
                    }
                }
            },
            onMove = { finger = it },
            onDrop = {
                val app = (dragged as? Drag.FromDrawer)?.app
                val place = dropPlace
                if (app != null && place != null) changeHomeApps { add(place, app) }
                dragged = null
            },
            onCancel = { dragged = null },
        )
    }

    /**
     * Moving what [items] lists at the press among itself: [move] puts one where another is, as the mode says, and a place
     * with a bin [remove]s one dropped there. [look] is how an item shows under the finger, with its [label] if given.
     */
    fun <T> rearrange(
        items: () -> List<T>,
        look: (T) -> RingItem,
        move: (T, T, ReorderMode) -> Unit,
        remove: ((T) -> Unit)? = null,
        label: ((T) -> String)? = null,
        startOnPress: Boolean = false,
    ): Rearrange = Rearrange(
            onStart = { index, position ->
                val listed = items()
                val item = listed.getOrNull(index)
                val within = item?.let {
                    Drag.Within(
                        place = this,
                        from = index,
                        item = look(it),
                        label = label?.invoke(it),
                        move = { to, mode -> move(it, listed[to], mode) },
                        remove = remove?.let { r -> { r(it) } },
                        current = { items() == listed },
                    )
                }
                val started = within != null && beginDrag(within, position)
                // Lifted at the press, the lift is the answer to the long press, as the menu is elsewhere, so it gets the
                // same nudge.
                if (started && startOnPress) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                started
            },
            onMove = { finger = it },
            onDrop = {
                (dragged as? Drag.Within)?.let { within ->
                    when {
                        overBin -> within.remove?.invoke()
                        !placeChanged -> reorderTarget?.let { within.move(it, latestReorderMode) }
                    }
                }
                dragged = null
            },
            onCancel = { dragged = null },
            startOnPress = startOnPress,
            movingIn = { place ->
                (dragged as? Drag.Within)?.takeIf { it.place === place }?.let { Moving(it.from, reorderTarget, latestReorderMode) }
            },
        )

    val ringRearrange = remember {
        rearrange(items = { latestRing }, look = { it }, move = { item, target, mode ->
            changeRing { move(item, target, mode) }
        })
    }
    val dockRearrange = remember {
        rearrange(items = { latestDock }, look = RingItem::App, move = { app, target, mode ->
            changeHomeApps { move(HomePlace.Dock, app, target, mode) }
        })
    }
    val latestOpen by rememberUpdatedState(open)
    val folderRearrange = remember(open?.index) {
        open?.let { folder ->
            rearrange(items = { latestOpen?.apps.orEmpty() }, look = RingItem::App, move = { app, target, mode ->
                changeHomeApps { move(HomePlace.Folder(folder.index), app, target, mode) }
            })
        }
    }
    val cardRearrange = remember {
        val byKind = mutableMapOf<CollectionKind.HandPicked, Rearrange>()
        val rearrangeFor: (CollectionKind.HandPicked) -> Rearrange = { kind ->
            byKind.getOrPut(kind) {
                rearrange(
                    items = { latestCollections.card(kind)?.apps?.resolve(latestApps.orEmpty()).orEmpty() },
                    look = RingItem::App,
                    move = { app, target, mode -> changeCollections { moveApp(kind, app, target, mode) } },
                    remove = { app -> changeCollections { removeApp(kind, app) } },
                    label = AppEntry::label,
                    startOnPress = true,
                )
            }
        }
        rearrangeFor
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
                            LauncherMenuRow(
                                "24-hour clock",
                                on = latestTwentyFourHour,
                                flips = true,
                                onClick = { latestOnTwentyFourHourChange(!latestTwentyFourHour) },
                            ),
                            LauncherMenuRow("Restart launcher") { latestOnRestart() },
                            LauncherMenuRow("Reset launcher") { confirmingReset = true },
                        ),
                        onDismiss = ::closeMenu,
                    )
                }
            },
        )
    }

    fun closeAll() {
        dragged = null
        closeMenu()
        openFolder = null
        editingWidget = null
        closeDrawer()
        editing = null
        pickingCollection = false
        confirmingReset = false
        confirmingPin = null
    }

    LaunchedEffect(homePresses, pagerState, drawerState, layout) {
        homePresses.collect { press ->
            closeAll()
            // Coming back from an app keeps the page you left, like the stock launcher.
            if (press.launcherInFront) goHome()
        }
    }
    // On the home page, so the shortcut is seen landing on the ring.
    LaunchedEffect(pinRequests, pagerState, drawerState, layout) {
        pinRequests.collect { request ->
            closeAll()
            goHome()
            confirmingPin = request
        }
    }
    // One handler with the order spelled out, instead of one per dismissable relying on composition order. A search is
    // not a rung of its own: the keyboard takes the first Back, and closing the drawer or the editor ends the search.
    // The open folder comes last because the drawer and the other pages both hide it, and a press should undo something
    // in view.
    BackHandler(
        enabled = dragged != null || drawerOpen || overlayOpen || editedWidget != null ||
            pagerState.currentPage != layout.homeIndex || open != null,
    ) {
        when {
            dragged != null -> dragged = null
            drawerOpen -> closeDrawer()
            editing != null -> editing = null
            pickingCollection -> pickingCollection = false
            editedWidget != null -> editingWidget = null
            pagerState.currentPage != layout.homeIndex -> goHome()
            else -> openFolder = null
        }
    }

    // The ghost is drawn over the sheet too, so it is a sibling of the scaffold rather than in its body. It gets the
    // finger in root coordinates, which this box may not start at: the origin is measured after [modifier], so it is
    // the padded content's, where the ghost is placed.
    var origin by remember { mutableStateOf(Offset.Zero) }
    val panel = MaterialTheme.colorScheme.surfaceDim
    val scrim = MaterialTheme.colorScheme.scrim
    // A faint shade from above the chevron down through the navigation bar, in place of the system's darker backing, whose
    // edge lines up with nothing of ours. It is only there to lift the light navigation icons off a bright wallpaper, so
    // it carries on below this box, which stops at the navigation bar. The drawer's panel fills in the navigation bar as
    // the drawer rises, so the open drawer reaches the bottom edge instead of stopping short of it.
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind {
                val top = size.height - (DRAWER_PEEK + BOTTOM_SHADE_FADE).toPx()
                val bottom = size.height + navigationBar.toPx()
                val shade = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to scrim.copy(alpha = 0.06f),
                    1f to scrim.copy(alpha = 0.22f),
                    startY = top,
                    endY = bottom,
                )
                drawRect(shade, Offset(0f, top), Size(size.width, bottom - top))

                val travel = size.height - DRAWER_PEEK.toPx()
                val open = if (travel > 0f) 1f - (drawerState.requireOffset() / travel).coerceIn(0f, 1f) else 1f
                drawRect(panel.copy(alpha = panel.alpha * open), Offset(0f, size.height), Size(size.width, bottom - size.height))
            },
    ) {
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(drawerState),
            sheetPeekHeight = DRAWER_PEEK,
            // The handle is the drawer's own, not the sheet's, so the panel can fill in the strip behind it: clear while the
            // drawer is down, so only the chevron shows over the wallpaper, and solid once it is open, so the open drawer
            // reaches the top as Arc's does. The panel is translucent so the wallpaper still shows through.
            sheetDragHandle = null,
            sheetContainerColor = Color.Transparent,
            sheetShadowElevation = 0.dp,
            containerColor = Color.Transparent,
            sheetContent = {
                Column(
                    Modifier.drawBehind {
                        val strip = DRAWER_PEEK.toPx()
                        // The drawer under the strip is as tall as it travels, like the pages.
                        val travel = size.height - strip
                        val open = if (travel > 0f) 1f - (drawerState.requireOffset() / travel).coerceIn(0f, 1f) else 1f
                        drawRect(panel.copy(alpha = panel.alpha * open), size = Size(size.width, strip))
                        drawRect(panel, Offset(0f, strip))
                    },
                ) {
                    DrawerHandle(
                        open = drawerOpen,
                        onClick = { if (drawerOpen) closeDrawer() else openDrawer() },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    AppDrawer(
                        apps = apps.orEmpty(),
                        icon = actions.icon,
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
                }
            },
            // The collapsed sheet is full height and continues below the scaffold, where the list would show through the
            // navigation-bar inset.
            modifier = Modifier.clipToBounds().alpha(if (overlayOpen) 0f else 1f),
        ) { padding ->
            HorizontalPager(
                state = pagerState,
                key = { layout.pages[it].name },
                // Every page stays composed, so swiping back does not rebuild the ring and reload its icons.
                beyondViewportPageCount = layout.pages.size - 1,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // The pages fade as the drawer rises, so only the wallpaper shows through the translucent drawer. They
                    // are exactly as tall as the drawer travels.
                    .graphicsLayer {
                        if (size.height > 0f) alpha = (drawerState.requireOffset() / size.height).coerceIn(0f, 1f)
                    }
                    .onGloballyPositioned { pagerOrigin = it.positionInRoot() }
                    .testTag(LauncherTags.PAGER),
            ) { index ->
                val page = layout.pages[index]
                Box(Modifier.fillMaxSize().testTag(LauncherTags.page(page))) {
                    when (page) {
                        LauncherPage.Home -> Column(
                            Modifier
                                .fillMaxSize()
                                .onPlaced { homePage = it }
                                .verticalSwipe(onDown = onOpenNotifications, onUp = { openDrawer() }),
                        ) {
                            Box(Modifier.weight(1f)) {
                                // First, so it lies behind the clock, the ring and the card and gets only the touches they leave.
                                // A tap anywhere there closes an open folder, not only one on the ring's centre.
                                EmptySpace(menu = launcherMenu, onTap = { openFolder = null })
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HomeClock(
                                        face = clock,
                                        onTimeClick = onOpenClock,
                                        onDateClick = onOpenCalendar,
                                        modifier = Modifier.padding(top = 24.dp),
                                    )
                                    RingerSwitch(mode = ringerMode, onClick = onRingerTap)
                                    HomeRing(
                                        ring = ring,
                                        // Slots stored for the ring hold the hint back until the apps and shortcuts can say none of
                                        // theirs is there, so neither the hint nor the mark flashes while they load.
                                        showHint = homeApps.ring.isEmpty || (onHome != null && ring.isEmpty()),
                                        icon = actions.icon,
                                        onLaunch = actions.launch,
                                        onOpenFolder = { openFolder = it.index },
                                        onCloseFolder = { openFolder = null },
                                        onEdit = { pickFor(HomePlace.Ring) },
                                        modifier = Modifier.weight(1f).dropZone { copy(ring = it) },
                                        highlighted = dropPlace == HomePlace.Ring,
                                        openFolder = open,
                                        menu = ringMenu,
                                        folderMenu = folderMenu,
                                        folderAppMenu = folderAppMenu,
                                        unread = unread,
                                        rearrange = if (open != null) folderRearrange else ringRearrange,
                                    )
                                    // Nothing dismisses the card: a launcher that is not the home app is not doing its job. Under
                                    // the ring, which sizes itself to the room left, so the two can never overlap.
                                    if (!isHomeApp) {
                                        HomeAppCard(
                                            onBecomeHomeApp = onBecomeHomeApp,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                        )
                                    }
                                }
                            }
                            // Only on the home page, as in Arc, so it slides away with the page and the others reach down to
                            // the drawer handle. Stored dock apps hold its row until the apps and shortcuts load, so the ring
                            // does not move when they arrive. An empty dock shows while an app is dragged, as a place to
                            // drop it, and slides in and out so the ring above moves rather than jumps.
                            val dockShown = dock.isNotEmpty() || (onHome == null && homeApps.dock.keys.isNotEmpty()) || dragged is Drag.FromDrawer
                            AnimatedVisibility(visible = dockShown) {
                                Dock(
                                    apps = dock,
                                    icon = actions.icon,
                                    onLaunch = actions.launch,
                                    modifier = Modifier.dropZone { copy(dock = it) },
                                    highlighted = dropPlace == HomePlace.Dock,
                                    menu = dockMenu,
                                    unread = unread,
                                    rearrange = dockRearrange,
                                )
                            }
                        }
                        LauncherPage.Widgets -> {
                            WidgetColumn(page = widgetPage, actions = widgets, editing = editedWidget, onEditingChange = { editingWidget = it })
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
                                onEdit = { editing = it.name },
                                onAdd = { pickingCollection = true },
                                onOpenUsageSettings = onOpenUsageSettings,
                                rearrange = cardRearrange,
                                bin = if (binShown) BinTarget(overBin, onPositioned = { binBounds = it }) else null,
                                unread = unread,
                            )
                        }
                    }
                }
            }
        }
        // Over the hidden scaffold, drawer strip included: each is a screen of its own until Back or HOME.
        remember(editing) { editing?.let(CollectionKind::named) }?.let { kind ->
            CollectionEditor(
                title = "Add to ${kind.title}",
                apps = apps.orEmpty(),
                icon = actions.icon,
                isPicked = { it.key in justPicked },
                onPick = { app ->
                    // Marked whether or not it was already in the card, so the tap is seen to have counted either way.
                    justPicked = justPicked + app.key
                    changeCollections { addApp(kind, app) }
                },
                query = editorQuery,
                onQueryChange = { editorQuery = it },
            )
        }
        if (pickingCollection) {
            // Both last as long as this visit to the picker. The custom cards it has listed keep a tile tapped off where
            // it was, with its apps, so a second tap cannot land on its neighbour and puts it back whole.
            var listedCustoms by remember { mutableStateOf(emptyList<CollectionCard>()) }
            var creatingCollection by rememberSaveable { mutableStateOf(false) }
            val customs = collections.customTiles(listedCustoms)
            CollectionPicker(
                page = collections,
                customs = customs.map { it.kind },
                onToggle = { kind ->
                    listedCustoms = customs
                    changeCollections {
                        when {
                            kind in this -> remove(kind)
                            kind is CollectionKind.Category -> add(kind, seedCategory(kind.category, apps.orEmpty()))
                            else -> add(kind, customs.find { it.kind == kind }?.apps ?: Favourites())
                        }
                    }
                },
                onCreate = { creatingCollection = true },
            )
            if (creatingCollection) {
                CreateCollectionDialog(
                    page = collections,
                    onCreate = { kind ->
                        creatingCollection = false
                        changeCollections { add(kind) }
                    },
                    onDismiss = { creatingCollection = false },
                )
            }
        }
        if (confirmingReset) {
            ResetDialog(
                onReset = {
                    confirmingReset = false
                    onReset()
                },
                onDismiss = { confirmingReset = false },
            )
        }
        confirmingPin?.let { request ->
            PinDialog(
                request = request,
                onAdd = {
                    confirmingPin = null
                    // Only once it is pinned, and in the same handler: whatever runs next sees it pinned and on the ring.
                    if (request.accept()) changeHomeApps { add(HomePlace.Ring, request.shortcut) }
                },
                onDismiss = { confirmingPin = null },
            )
        }
        // Over the pages, and reachable by a second finger while the first holds the item.
        if (dragged is Drag.Within) {
            ReorderModeSwitch(reorderMode, onReorderModeChange, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        }
        dragged?.let { drag ->
            DragGhost(position = { finger - origin }, label = drag.label) {
                when (val item = drag.item) {
                    is RingItem.App -> AppImage(item.app, actions.icon, Modifier.fillMaxSize())
                    is RingItem.Folder -> FolderPreviews(item, actions.icon)
                }
            }
        }
    }
}

/** What is being dragged, and where from: how it shows under the finger, with its [label] if it has one. */
private sealed interface Drag {
    val item: RingItem
    val label: String? get() = null

    /** Out of the drawer, to the ring or the dock. */
    data class FromDrawer(val app: AppEntry) : Drag {
        override val item = RingItem.App(app)
    }

    /**
     * The item at [from] in [place]: to another of its positions, which [move] takes it to, or, for a place with a bin,
     * onto the bin, which [remove]s it. [current] says whether the place still lists what it did at the press.
     */
    class Within(
        val place: Rearrange,
        val from: Int,
        override val item: RingItem,
        override val label: String?,
        val move: (to: Int, ReorderMode) -> Unit,
        val remove: (() -> Unit)?,
        val current: () -> Boolean,
    ) : Drag
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

    /** The launcher's own menu, from the home page's empty space. */
    data class Launcher(override val expanded: Boolean = true) : OpenMenu {
        override fun closed() = copy(expanded = false)
    }
}

internal fun LayoutCoordinates.rootBounds(): Bounds = boundsIn(findRootCoordinates())

// Unclipped, so a target scrolled off the page keeps its true bounds instead of collapsing onto the edge it left by.
private fun LayoutCoordinates.boundsIn(ancestor: LayoutCoordinates): Bounds {
    val box = ancestor.localBoundingBoxOf(this, clipBounds = false)
    return Bounds(box.left, box.top, box.right, box.bottom)
}
