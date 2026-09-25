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
import androidx.compose.runtime.snapshotFlow
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
import com.sqftware.orbitlauncher.domain.EMBLEM_FRACTION
import com.sqftware.orbitlauncher.domain.Favourites
import com.sqftware.orbitlauncher.domain.ForegroundTime
import com.sqftware.orbitlauncher.domain.HomeApps
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.Landing
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.RingerMode
import com.sqftware.orbitlauncher.domain.ReorderMode
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.appOptions
import com.sqftware.orbitlauncher.domain.seedCategory
import com.sqftware.orbitlauncher.domain.title
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object LauncherTags {
    const val PAGER = "launcher_pager"

    fun page(page: LauncherPage) = "page_${page.name}"
}

private val DRAWER_PEEK = 48.dp

/** How far above the drawer's strip the shade along the bottom edge starts. */
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
 * options; the menu of an app on the ring or in the dock can start a folder in its slot. A folder, on the ring or in the
 * dock, opens in place on the ring, as in Arc: its apps take the ring's slots and the emblem makes way for a target that
 * closes it; its own menu fills it from the drawer or removes it. A long
 * press in the drawer that moves on drags the app out: the drawer closes, a ghost of the icon follows the finger over the
 * home page, and letting go over the ring or the dock adds it there. The widget page shows [widgetPage] through
 * [widgets]; a long press puts a widget in edit mode, to move, resize or remove it, until a tap elsewhere or the page goes
 * out of view. The collections page shows the cards of [collections], the built-in ones filled from the app list and
 * [foregroundTime] (null until usage access is granted, which [onOpenUsageSettings] asks for); a hand-picked card's
 * pencil opens an editor over the screen that adds apps to it, and the button under the cards opens the picker that adds
 * and removes cards, whose last tile opens a dialog naming a new custom collection. An app long-pressed on a hand-picked
 * card lifts off it, and dropping it on the bin takes it off the card. A long press that moves on picks up an app or a
 * folder on the ring, an app in the open folder or the dock, or, at once, an app on a hand-picked card, to move it among
 * its neighbours: they make way where the finger rests, and letting go over another's place puts it there, inserting it
 * or swapping the two as [reorderMode] says, which a switch at the top flips ([onReorderModeChange]) while an item is on
 * the move, at a second finger's tap or when the item rests on its other half. Letting go anywhere else leaves the order
 * as it was. An app goes between the ring and the dock the same way, and one held over the middle of its open folder
 * closes it and goes on to either. An app, from the drawer too, resting on the middle of an app or folder on the ring or
 * in the dock lights it, and letting go there folds it in; a folder whose last app leaves goes. Apps everywhere wear their [unread] counts; a long
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
    val ring = remember(homeApps.ring, onHome) { homeApps.ring.resolve(onHome.orEmpty(), HomePlace.Ring) }
    val dock = remember(homeApps.dock, onHome) { homeApps.dock.resolve(onHome.orEmpty(), HomePlace.Dock) }
    val shown = remember(onHome) { onHome?.mapTo(HashSet()) { it.key } }
    val latestHomeApps by rememberUpdatedState(homeApps)
    val latestApps by rememberUpdatedState(apps)
    val latestShown by rememberUpdatedState(shown)
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

    // The open folder takes the ring over wherever it is kept, and is named by its place and slot, so Back reaches it
    // through this screen's BackHandler.
    var openFolder by remember { mutableStateOf<HomePlace.Folder?>(null) }

    // Callbacks built once read the home apps through the latest state, so what they change is always the current ring.
    // The open folder is named by its slot, so it closes once another folder may show there: a dock folder's neighbours
    // stay live while it is open, and one of them going or moving shifts the slots.
    fun changeHomeApps(change: HomeApps.() -> HomeApps) {
        val changed = latestHomeApps.change()
        if (changed == latestHomeApps) return
        if (openFolder?.let { latestHomeApps.keeps(it, changed) } == false) openFolder = null
        latestOnHomeAppsChange(changed)
    }

    fun changeCollections(change: CollectionsPage.() -> CollectionsPage) {
        val changed = latestCollections.change()
        if (changed != latestCollections) latestOnCollectionsChange(changed)
    }

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
    // The halves of the reorder switch, and the one the item on the move is over: a rest there flips the mode. The
    // switch overlaps what is under it, the first card's apps among them, so while the item is over it nothing moves.
    // Only an app from the drawer, which only adds, has no switch.
    val switchShown by remember { derivedStateOf { dragged.let { it != null && it !is Drag.FromDrawer } } }
    var switchHalves by remember { mutableStateOf(emptyMap<ReorderMode, Bounds>()) }
    val overSwitch by remember {
        derivedStateOf {
            if (switchShown) switchHalves.entries.find { it.value.contains(finger.x, finger.y) }?.key else null
        }
    }
    val latestOnReorderModeChange by rememberUpdatedState(onReorderModeChange)
    LaunchedEffect(overSwitch) {
        val mode = overSwitch ?: return@LaunchedEffect
        if (mode != latestReorderMode) {
            delay(SWITCH_HOVER_MILLIS)
            latestOnReorderModeChange(mode)
        }
    }
    // A place's positions hold only while it lists what it did at the press: an app updating or going mid-drag would
    // shift them under the finger, so that ends the drag.
    val placeChanged by remember { derivedStateOf { dragged?.current?.invoke() == false } }
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
            // A folder left empty goes once its pick is over, not while toggling apps off and on again. Not before the apps
            // load, as until then it shows none of them.
            (picking as? HomePlace.Folder)?.let { folder ->
                latestShown?.let { shown -> changeHomeApps { change(folder.holder) { removeIfEmpty(folder.index, shown) } } }
            }
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
    // Every page stays composed, so the home page must be told when nobody can see it.
    val homeSettled by remember(pagerState, layout) { derivedStateOf { pagerState.settledPage == layout.homeIndex } }
    val homeInSight = homeSettled && !drawerOpen && !overlayOpen
    LaunchedEffect(editing) {
        if (editing == null) {
            justPicked = emptySet()
            editorQuery = ""
            focusManager.clearFocus()
        }
    }
    // The menu is a focusable popup window, so Back reaches its onDismissRequest before this screen's BackHandler.
    var openMenu by remember { mutableStateOf<OpenMenu?>(null) }
    var openingMenu by remember { mutableStateOf<Job?>(null) }
    val open = openFolder?.let { at ->
        (if (at.holder == HomePlace.Ring) ring else dock).filterIsInstance<RingItem.Folder>().find { it.at == at }
    }
    // A slot number outliving its folder would open a folder made later in that slot by itself.
    LaunchedEffect(open == null) { if (open == null) openFolder = null }

    // The ring's and the dock's positions, so an app can be dragged from one home place to another. Each is registered as
    // it is built, below, with the drag handling that needs these.
    val homePlaces = remember { mutableMapOf<HomePlace.Slots, Rearrange>() }

    fun itemsOf(holder: HomePlace.Slots) = if (holder == HomePlace.Ring) latestRing else latestDock
    // What resting the finger has done. A pause off the middle of anything the dragged app could fold into makes its own
    // place make way for it there, and over the middle of an open folder closes the folder; a longer one on such a middle
    // lights that item, and letting go folds the app into it. A quick sweep changes nothing.
    var makingWay by remember { mutableStateOf<Int?>(null) }
    var lit by remember { mutableStateOf<Over.Slot?>(null) }

    // Folding an app dragged onto the middle of position [hit] of [holder], the ring or the dock, into the item that shows
    // there as the place makes way, if the app lands there; nothing for a folder dragged, as folders do not nest.
    fun foldInto(drag: Drag, holder: HomePlace.Slots, place: Rearrange, hit: Rearrange.Hit): Landing.Into? {
        if (!hit.onMiddle) return null
        val app = drag.app ?: return null
        val items = itemsOf(holder)
        val item = items.getOrNull(place.moving?.sourceOf(items, hit.index) ?: hit.index) ?: return null
        return Landing.Into(holder, item).takeIf { latestHomeApps.lands(app, drag.home, it) }
    }

    fun overFor(drag: Drag): Over? {
        val own = (drag as? Drag.Within)?.place
        if (overBin || overSwitch != null) return null
        // A card's apps move only on their card.
        if (drag.home == null && own != null) return own.at(finger)?.let { Over.Slot(own, it) }
        val onHome = finger - pagerOrigin
        if (drag.home is HomePlace.Folder && own != null && zones.ring?.discContains(onHome.x, onHome.y, EMBLEM_FRACTION) == true) {
            return Over.FolderCentre
        }
        val place = zones.placeAt(onHome.x, onHome.y)
        // The dock's row first, for an app or one of the dock's own folders: the ring's lowest slot reaches down towards it.
        val dockPlace = homePlaces[HomePlace.Dock]
        if (dockPlace != null && place == HomePlace.Dock && (drag.app != null || own === dockPlace)) {
            return dockPlace.hit(finger)?.let { Over.Slot(dockPlace, it.index, foldInto(drag, HomePlace.Dock, dockPlace, it)) }
                ?: Over.DockEnd.takeIf { drag.app != null }
        }
        val ringPlace = homePlaces[HomePlace.Ring]
        ringPlace?.hit(finger)?.let { return Over.Slot(ringPlace, it.index, foldInto(drag, HomePlace.Ring, ringPlace, it)) }
        if (own !== ringPlace) own?.at(finger)?.let { return Over.Slot(own, it) }
        // Off every slot, as all of an empty ring is.
        return Over.RingEnd.takeIf { drag.app != null && place == HomePlace.Ring }
    }

    val over by remember { derivedStateOf { dragged?.let(::overFor) } }

    // The folder closes, so the ring comes back under the finger, and the app goes on as one dragged out of it. The folder
    // is named by its slot, so the drag holds only while the ring and the dock do.
    fun leaveFolder() {
        val within = dragged as? Drag.Within ?: return
        val app = within.app ?: return
        val folder = within.home as? HomePlace.Folder ?: return
        val left = latestRing to latestDock
        dragged = Drag.OutOfFolder(app, folder, current = { (latestRing to latestDock) == left })
        makingWay = null
        openFolder = null
    }

    // Collected rather than read here, so the screen does not recompose each time the finger crosses into another slot.
    LaunchedEffect(Unit) {
        snapshotFlow { over }.collectLatest { now ->
            // One the finger leaves must be rested on again, not just swept back to.
            lit = null
            if (now is Over.Slot && now.foldInto != null) {
                delay(FOLD_MILLIS)
                lit = now
            } else {
                delay(MAKE_WAY_MILLIS)
                if (now == Over.FolderCentre) {
                    leaveFolder()
                } else {
                    makingWay = (now as? Over.Slot)?.takeIf { it.place === (dragged as? Drag.Within)?.place }?.index
                }
            }
        }
    }
    // Lit only while the finger is still where it rested, as it may have left before the collector hears of it.
    val litSlot by remember { derivedStateOf { lit?.takeIf { it == over } } }

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
                            is AppOption.Remove -> latestShown?.let { shown -> changeHomeApps { remove(option.place, app, shown) } }
                            AppOption.NewFolder -> (place as? HomePlace.Slots)?.let { holder ->
                                val slot = latestHomeApps.slots(holder).indexOf(app)
                                if (slot >= 0) {
                                    changeHomeApps { change(holder) { newFolder(app) } }
                                    pickFor(HomePlace.Folder(holder, slot))
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
    val folderAppMenu = remember(actions, open?.at) { open?.let { appMenu(it.at) } }
    val folderMenu = remember {
        FolderMenu(
            onOpen = { folder ->
                openingMenu?.cancel()
                openMenu = OpenMenu.Folder(folder.at)
            },
            content = { folder ->
                (openMenu as? OpenMenu.Folder)?.takeIf { it.at == folder.at }?.let { shown ->
                    DisposableEffect(Unit) {
                        onDispose { if ((openMenu as? OpenMenu.Folder)?.at == folder.at) closeMenu() }
                    }
                    FolderOptionsMenu(
                        expanded = shown.expanded,
                        onOption = { option ->
                            when (option) {
                                FolderOption.AddApps -> pickFor(folder.at)
                                FolderOption.Remove -> {
                                    // The next folder along inherits this slot's number, and would inherit the fading menu too.
                                    openMenu = null
                                    changeHomeApps { change(folder.at.holder) { remove(folder.at.index) } }
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
        makingWay = null
        lit = null
        return true
    }

    // Where an app let go over [target] in another home place lands, if it [lands][HomeApps.lands] there.
    fun landing(target: Over?): Landing? {
        val mode = latestReorderMode
        return when (target) {
            Over.DockEnd -> Landing.At(HomePlace.Dock, null, mode)
            Over.RingEnd -> Landing.At(HomePlace.Ring, null, mode)
            is Over.Slot -> homePlaces.entries.find { it.value === target.place }?.key?.let { holder ->
                itemsOf(holder).getOrNull(target.index)?.let { Landing.At(holder, it, mode) }
            }
            else -> null
        }
    }

    fun drop() {
        val drag = dragged ?: return
        val target = over
        val app = drag.app
        val foldInto = litSlot?.foldInto
        val shown = latestShown
        when {
            drag is Drag.Within && overBin -> drag.remove?.invoke()
            placeChanged -> Unit
            app != null && foldInto != null -> shown?.let { changeHomeApps { move(app, drag.home, foldInto, it) } }
            drag is Drag.FromDrawer -> dropPlace?.let { place -> changeHomeApps { add(place, drag.app) } }
            drag is Drag.Within && target is Over.Slot && target.place === drag.place -> drag.move(target.index, latestReorderMode)
            app != null && shown != null -> landing(target)?.let { to -> changeHomeApps { move(app, drag.home, to, shown) } }
        }
        dragged = null
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
            onDrop = ::drop,
            onCancel = { dragged = null },
        )
    }

    /**
     * Moving what [items] lists at the press among itself: [move] puts one where another is, as the mode says, and a place
     * with a bin [remove]s one dropped there. [look] is how an item shows under the finger, with its [label] if given. A
     * [home] place's apps can also be dragged to the others.
     */
    fun <T> rearrange(
        home: HomePlace?,
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
                        home = home,
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
            onDrop = ::drop,
            onCancel = { dragged = null },
            startOnPress = startOnPress,
            movingIn = { place ->
                (dragged as? Drag.Within)?.takeIf { it.place === place }?.let { Moving(it.from, makingWay, latestReorderMode) }
            },
        )

    fun slotsRearrange(holder: HomePlace.Slots) =
        rearrange(holder, items = { itemsOf(holder) }, look = { it }, move = { item, target, mode ->
            changeHomeApps { change(holder) { move(item, target, mode) } }
        }).also { homePlaces[holder] = it }

    val ringRearrange = remember { slotsRearrange(HomePlace.Ring) }
    val dockRearrange = remember { slotsRearrange(HomePlace.Dock) }
    val latestOpen by rememberUpdatedState(open)
    val folderRearrange = remember(open?.at) {
        open?.let { folder ->
            val place = folder.at
            rearrange(place, items = { latestOpen?.apps.orEmpty() }, look = RingItem::App, move = { app, target, mode ->
                changeHomeApps { change(place.holder) { move(place.index, app, target, mode) } }
            })
        }
    }
    val cardRearrange = remember {
        val byKind = mutableMapOf<CollectionKind.HandPicked, Rearrange>()
        val rearrangeFor: (CollectionKind.HandPicked) -> Rearrange = { kind ->
            byKind.getOrPut(kind) {
                rearrange(
                    home = null,
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
    // edge lines up with nothing of ours. It is in the scrim, the opposite of the navigation icons' ink, only to lift them
    // off the wallpaper, so it carries on below this box, which stops at the navigation bar. The drawer's panel fills in the navigation bar as
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
                                        onOpenFolder = { openFolder = it.at },
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
                                        foldTarget = litSlot?.takeIf { it.foldInto?.holder == HomePlace.Ring }?.index,
                                        held = (dragged as? Drag.OutOfFolder)?.app,
                                        inSight = homeInSight,
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
                            // does not move when they arrive. An empty dock shows while an app is dragged from the drawer or
                            // another home place, as a place to drop it, and slides in and out so the ring above moves rather
                            // than jumps.
                            val dockShown = dock.isNotEmpty() || (onHome == null && !homeApps.dock.isEmpty) ||
                                dragged?.let { it.app != null && (it is Drag.FromDrawer || it.home != null) } == true
                            AnimatedVisibility(visible = dockShown) {
                                Dock(
                                    items = dock,
                                    icon = actions.icon,
                                    onLaunch = actions.launch,
                                    onOpenFolder = { openFolder = it.at },
                                    modifier = Modifier.dropZone { copy(dock = it) },
                                    highlighted = dropPlace == HomePlace.Dock,
                                    menu = dockMenu,
                                    folderMenu = folderMenu,
                                    unread = unread,
                                    rearrange = dockRearrange,
                                    foldTarget = litSlot?.takeIf { it.foldInto?.holder == HomePlace.Dock }?.index,
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
        if (switchShown) {
            ReorderModeSwitch(
                mode = reorderMode,
                onModeChange = onReorderModeChange,
                onPlaced = { mode, bounds -> if (switchHalves[mode] != bounds) switchHalves = switchHalves + (mode to bounds) },
                hovered = overSwitch != null,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )
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

    /** The home place it leaves if it lands in another; none for one from the drawer or a card. */
    val home: HomePlace? get() = null

    /** The app dragged, unless it is a folder. */
    val app: AppEntry? get() = (item as? RingItem.App)?.app

    /** Whether the place it came from still lists what it did, so the positions found there hold; none from the drawer. */
    val current: (() -> Boolean)? get() = null

    /** Out of the drawer, to the ring or the dock. */
    data class FromDrawer(override val app: AppEntry) : Drag {
        override val item = RingItem.App(app)
    }

    /**
     * The item at [from] in [place], one of the [home] places or a card: to another of its positions, which [move] takes
     * it to, or, for a place with a bin, onto the bin, which [remove]s it.
     */
    class Within(
        val place: Rearrange,
        val from: Int,
        override val item: RingItem,
        override val label: String?,
        override val home: HomePlace?,
        val move: (to: Int, ReorderMode) -> Unit,
        val remove: (() -> Unit)?,
        override val current: () -> Boolean,
    ) : Drag

    /** An app dragged out of the open folder at [home] over its middle, which closed it, to the ring or the dock. */
    class OutOfFolder(override val app: AppEntry, override val home: HomePlace.Folder, override val current: () -> Boolean) : Drag {
        override val item = RingItem.App(app)
    }
}

/** What the finger holding a dragged item is over, as far as letting it go there goes. */
private sealed interface Over {
    /** Position [index] of [place], on the middle of the item a dragged app would fold into, as [foldInto] says if given. */
    data class Slot(val place: Rearrange, val index: Int, val foldInto: Landing.Into? = null) : Over

    /** The dock's row away from its apps, whose end an app goes to. */
    data object DockEnd : Over

    /** The ring away from its slots, whose end an app goes to. */
    data object RingEnd : Over

    /** The middle of the ring while a folder is open there, which is the way out of the folder. */
    data object FolderCentre : Over
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

    /** The menu of the folder [at] its place and slot. */
    data class Folder(val at: HomePlace.Folder, override val expanded: Boolean = true) : OpenMenu {
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
