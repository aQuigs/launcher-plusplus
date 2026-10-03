package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.DrawerLayout
import com.sqftware.orbitlauncher.domain.DrawerOrder
import com.sqftware.orbitlauncher.domain.DrawerStyle
import com.sqftware.orbitlauncher.domain.ForegroundTime
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.inOrder
import com.sqftware.orbitlauncher.domain.keysWithSharedLabels
import com.sqftware.orbitlauncher.domain.matching
import com.sqftware.orbitlauncher.domain.mostUsed
import com.sqftware.orbitlauncher.domain.sectionsByInitial
import kotlinx.coroutines.launch

object AppDrawerTags {
    const val HANDLE = "drawer_handle"
    const val LIST = "app_list"
    const val SEARCH = "app_search"
    const val MOST_USED = "drawer_most_used"
    const val USAGE_ACCESS = "drawer_usage_access"
    const val SORT_MENU = "drawer_sort_menu"

    fun section(initial: Char) = "section_$initial"

    fun letter(initial: Char) = "rail_$initial"
}

/**
 * The chevron that peeks above the pages, pointing the way the drawer will move, on a soft halo in the scrim so it reads on
 * any wallpaper. A tap calls [onClick].
 */
@Composable
fun DrawerHandle(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    val scrim = MaterialTheme.colorScheme.scrim

    Icon(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = if (open) "Close the app drawer" else "Open the app drawer",
        modifier = modifier
            .clickable(onClick = onClick)
            .drawWithCache {
                val radius = size.height / 2
                val shadow = Brush.radialGradient(listOf(scrim.copy(alpha = 0.4f), Color.Transparent), radius = radius)
                onDrawBehind { drawCircle(shadow, radius) }
            }
            .padding(vertical = 8.dp)
            .size(32.dp)
            .graphicsLayer { rotationZ = rotation }
            .testTag(AppDrawerTags.HANDLE),
    )
}

/** How a picked row is marked: a check on a row a tap takes off again, or a dot on one a tap cannot change. */
enum class PickMark { Check, Dot }

/**
 * Picking apps instead of launching them: the drawer shows [header] above the list, puts a [mark] on the rows [isPicked]
 * says, and a tap on a row [isPickable] says calls [onToggle].
 */
class Picking(
    val header: @Composable () -> Unit,
    val isPicked: (AppEntry) -> Boolean,
    val onToggle: (AppEntry) -> Unit,
    val mark: PickMark = PickMark.Check,
    val isPickable: (AppEntry) -> Boolean = { true },
)

/**
 * The drawer's own controls beside its search field: one button that switches the [style] to the next [DrawerLayout],
 * and a button opening the sort menu, shown while [sorting], of the orders and the row of the most used
 * apps. A choice goes to [onStyleChange]; opening and closing the menu to [onSortingChange].
 */
data class DrawerControls(
    val style: DrawerStyle,
    val onStyleChange: (DrawerStyle) -> Unit,
    val sorting: Boolean,
    val onSortingChange: (Boolean) -> Unit,
)

/**
 * Every app, as its [icon] and its name, laid out and ordered as the [controls]' style says: a row per app or a grid of
 * them, and by name with a rail of initials down the end edge to jump by, the list in sections headed by those initials,
 * or else in one run, the most used ([foregroundTime]) or the newest first. The style can head the list with a row of the
 * most used apps; what needs usage access asks for it ([onOpenUsageSettings]) until it is granted. An app whose name an app from
 * another package shares also shows its package name, to tell them apart. A tap launches the app and a long press opens
 * its [menu], or in the most used row the [mostUsedMenu], unless the drawer is [picking]; a long press that moves on
 * becomes a [drag]. A change of style starts the list again at its top. A search field heads the
 * list: with a [query] the list holds only the matching apps, without sections or rail, and the keyboard's search key
 * acts on the first of them as a tap would. An app with [unread] notifications shows their number, in full at the end of
 * its row, since a row has the room a badge lacks, or as a badge on its icon in the grid.
 */
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    picking: Picking? = null,
    gridState: LazyGridState = rememberLazyGridState(),
    menu: AppMenu? = null,
    mostUsedMenu: AppMenu? = null,
    drag: AppDrag? = null,
    query: String,
    onQueryChange: (String) -> Unit,
    unread: UnreadCounts = UnreadCounts(),
    onClearBadge: ((AppEntry) -> Unit)? = null,
    controls: DrawerControls? = null,
    foregroundTime: ForegroundTime? = null,
    onOpenUsageSettings: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val style = controls?.style ?: DrawerStyle()
    val searching = query.isNotBlank()
    val matches = remember(apps, query) { if (searching) apps.matching(query) else emptyList() }
    // The matches scroll on their own, so the sections come back where they were and a match does not become the top of
    // the sections' list because it was the first thing on screen when the search ended. Each new query starts at its top.
    val matchesState = rememberLazyGridState()
    LaunchedEffect(matches) { matchesState.scrollToItem(0) }
    val alphabetical = style.order == DrawerOrder.Alphabetical
    val usageForOrder = foregroundTime.takeIf { style.order == DrawerOrder.MostUsed }
    val ordered = remember(apps, style.order, usageForOrder) { apps.inOrder(style.order, usageForOrder) }
    val sections = remember(ordered, alphabetical) { if (alphabetical) ordered.sectionsByInitial() else emptyList() }
    val askForUsage = picking == null && style.readsUsage && foregroundTime == null
    val showsMostUsed = picking == null && style.mostUsedRow
    val mostUsedApps = remember(apps, showsMostUsed, foregroundTime) {
        if (showsMostUsed && foregroundTime != null) mostUsed(apps, foregroundTime, GRID_COLUMNS) else emptyList()
    }
    val sharingALabel = remember(apps) { apps.keysWithSharedLabels() }
    val detail = { app: AppEntry -> app.packageName.takeIf { app.key in sharingALabel } }
    val initials = remember(sections) { sections.map { it.initial } }
    // At most one row heads the list: the prompt needs usage access missing, the row needs it granted.
    val leading = if (askForUsage || mostUsedApps.isNotEmpty()) 1 else 0
    // The grid has no headers, as Arc's has none, so there the rail jumps to a section's first app.
    val headed = style.layout == DrawerLayout.List
    // The rail's targets are the running item counts: each section is its header, if it has one, then its apps.
    val headerIndices = remember(sections, leading, headed) {
        val header = if (headed) 1 else 0
        sections.runningFold(leading) { index, section -> index + header + section.apps.size }.dropLast(1)
    }
    var lastSelected by remember { mutableStateOf<Int?>(null) }
    // The chosen letter stays lit while its header is on screen: near the end of the list the scroll stops short, and
    // the section at the top is then an earlier one.
    val highlighted by remember(headerIndices, gridState) {
        derivedStateOf {
            val chosenHeader = lastSelected?.let(headerIndices::getOrNull)
            lastSelected.takeIf { chosenHeader != null && gridState.layoutInfo.visibleItemsInfo.any { it.index == chosenHeader } }
                ?: headerIndices.indexOfLast { it <= gridState.firstVisibleItemIndex }.coerceAtLeast(0)
        }
    }
    // The list keeps its place by the first item on screen, so a new order or a new row at the top would open it midway,
    // or the row out of sight above.
    LaunchedEffect(style.order, style.layout, leading) { gridState.scrollToItem(0) }
    val showsRail = !searching && alphabetical
    val entry = DrawerEntry(
        layout = style.layout,
        icon = icon,
        onLaunch = onLaunch,
        picking = picking,
        menu = menu.takeIf { picking == null },
        drag = drag.takeIf { picking == null },
        unread = unread,
        onClearBadge = onClearBadge,
    )

    Column(modifier.fillMaxSize()) {
        picking?.header?.invoke()
        Row(verticalAlignment = Alignment.CenterVertically) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    if (picking == null) matches.firstOrNull()?.let(onLaunch) else matches.firstOrNull(picking.isPickable)?.let(picking.onToggle)
                },
                modifier = Modifier.weight(1f).padding(start = 24.dp, top = 8.dp, end = if (controls == null) 24.dp else 4.dp, bottom = 8.dp),
            )
            controls?.let { StyleButtons(it) }
        }
        Box(Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (style.layout == DrawerLayout.Grid) GRID_COLUMNS else 1),
                state = if (searching) matchesState else gridState,
                contentPadding = PaddingValues(end = if (showsRail) RAIL_WIDTH else 0.dp),
                modifier = Modifier.fillMaxSize().testTag(AppDrawerTags.LIST),
            ) {
                when {
                    searching && matches.isEmpty() -> fullWidth(contentType = "empty") { NoMatches() }
                    searching -> apps(matches, entry, detail)
                    else -> {
                        if (askForUsage) {
                            fullWidth(key = "usage", contentType = "usage") {
                                PermissionRequired(onOpenUsageSettings, Modifier.padding(horizontal = 24.dp).testTag(AppDrawerTags.USAGE_ACCESS))
                            }
                        }
                        if (mostUsedApps.isNotEmpty()) {
                            fullWidth(key = "most_used", contentType = "most_used") {
                                // A letter's header follows it in the A to Z list; elsewhere the rest needs a heading of its own.
                                MostUsedRow(mostUsedApps, entry.copy(menu = mostUsedMenu), detail, headsTheRest = !(alphabetical && headed))
                            }
                        }
                        if (alphabetical) {
                            sections.forEach { section ->
                                if (headed) fullWidth(key = section.initial, contentType = "header") { SectionHeader(section.initial) }
                                apps(section.apps, entry, detail)
                            }
                        } else {
                            apps(ordered, entry, detail)
                        }
                    }
                }
            }
            if (showsRail) {
                LetterRail(
                    initials = initials,
                    highlighted = highlighted,
                    onSelect = { section ->
                        lastSelected = section
                        scope.launch { gridState.scrollToItem(headerIndices[section]) }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search apps") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, contentDescription = "Clear the search") }
            }
        },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            // The placeholder goes once there is text, and a screen reader should still say what the field is for.
            .semantics { contentDescription = "Search apps" }
            .testTag(AppDrawerTags.SEARCH),
    )
}

@Composable
private fun StyleButtons(controls: DrawerControls) {
    val style = controls.style

    // The button shows the layout a tap switches to, as the view toggle in Files does.
    val next = DrawerLayout.entries.let { it[(style.layout.ordinal + 1) % it.size] }
    IconButton(onClick = { controls.onStyleChange(style.copy(layout = next)) }) {
        Icon(
            imageVector = when (next) {
                DrawerLayout.List -> Icons.AutoMirrored.Filled.List
                DrawerLayout.Grid -> GridGlyph
            },
            contentDescription = "Switch to ${next.name.lowercase()} view",
        )
    }
    Box(Modifier.padding(end = 8.dp)) {
        IconButton(onClick = { controls.onSortingChange(true) }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sort apps")
        }
        SortMenu(controls)
    }
}

/** The orders the drawer can list its apps in, the one chosen checked, and the switch for the most used apps' row. */
@Composable
private fun SortMenu(controls: DrawerControls) {
    val style = controls.style
    val close = { controls.onSortingChange(false) }
    fun choose(changed: DrawerStyle) = chooseFrom(controls.sorting, close) { controls.onStyleChange(changed) }

    GroundMenu(expanded = controls.sorting, onDismissRequest = close, modifier = Modifier.testTag(AppDrawerTags.SORT_MENU)) {
        DrawerOrder.entries.forEach { order ->
            val chosen = order == style.order
            DropdownMenuItem(
                text = { Text(order.label) },
                trailingIcon = if (chosen) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else {
                    null
                },
                onClick = { choose(style.copy(order = order)) },
                modifier = Modifier.semantics { selected = chosen },
            )
        }
        HorizontalDivider()
        LauncherMenuItem(
            LauncherMenuRow("Most used row", on = style.mostUsedRow, flips = true) {
                controls.onStyleChange(style.copy(mostUsedRow = !style.mostUsedRow))
            },
            controls.sorting,
            close,
        )
    }
}

@Composable
private fun NoMatches() {
    Text(
        text = "No apps match",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

private val RAIL_WIDTH = 28.dp
private val ROW_ICON_SIZE = 40.dp
private val GRID_ICON_SIZE = 52.dp
private const val GRID_COLUMNS = 4

private fun LazyGridScope.fullWidth(key: Any? = null, contentType: Any? = null, content: @Composable () -> Unit) =
    item(key = key, span = { GridItemSpan(maxLineSpan) }, contentType = contentType) { content() }

private fun LazyGridScope.apps(apps: List<AppEntry>, entry: DrawerEntry, detail: (AppEntry) -> String?) =
    items(apps, key = { it.key }, contentType = { "app" }) { app -> entry.Show(app, detail(app)) }

/** What every app in the drawer is shown with, in its [layout]: a row, or a cell of the grid. */
private data class DrawerEntry(
    val layout: DrawerLayout,
    val icon: suspend (AppEntry) -> ImageBitmap?,
    val onLaunch: (AppEntry) -> Unit,
    val picking: Picking?,
    val menu: AppMenu?,
    val drag: AppDrag?,
    val unread: UnreadCounts,
    val onClearBadge: ((AppEntry) -> Unit)?,
) {
    @Composable
    fun Show(app: AppEntry, detail: String?) = when (layout) {
        DrawerLayout.List -> AppRow(app, detail, this)
        DrawerLayout.Grid -> AppCell(app, detail, this)
    }

    fun action(app: AppEntry): Modifier = when {
        // The drag comes after the click handling, so it reads each touch first and can keep the moves to itself.
        picking == null -> Modifier.launchable(app, onLaunch, menu, onClearBadge = unread.clearing(app, onClearBadge)).itemDrag(app, drag)
        picking.mark == PickMark.Check -> {
            Modifier.toggleable(
                value = picking.isPicked(app),
                enabled = picking.isPickable(app),
                role = Role.Checkbox,
                onValueChange = { picking.onToggle(app) },
            )
        }
        else -> Modifier.clickable(enabled = picking.isPickable(app)) { picking.onToggle(app) }
    }
}

/**
 * The [apps] used most, a grid row of them under their own heading, whatever the drawer's layout, and if it [headsTheRest]
 * a heading for all the apps after it.
 */
@Composable
private fun MostUsedRow(apps: List<AppEntry>, entry: DrawerEntry, detail: (AppEntry) -> String?, headsTheRest: Boolean) {
    Column {
        Column(Modifier.testTag(AppDrawerTags.MOST_USED)) {
            Heading("Most used")
            Row {
                apps.forEach { app -> AppCell(app, detail(app), entry, Modifier.weight(1f)) }
                repeat(GRID_COLUMNS - apps.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (headsTheRest) Heading("All apps")
    }
}

@Composable
private fun SectionHeader(initial: Char) = Heading(initial.toString(), Modifier.testTag(AppDrawerTags.section(initial)))

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
    )
}

@Composable
private fun AppRow(app: AppEntry, detail: String?, entry: DrawerEntry) {
    val picking = entry.picking
    val picked = picking?.isPicked(app) == true
    val unread = entry.unread[app]

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(entry.action(app))
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        IconDisc(modifier = Modifier.size(ROW_ICON_SIZE)) { AppImage(app, entry.icon, Modifier.fillMaxSize()) }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(text = app.label, style = MaterialTheme.typography.titleMedium)
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    // Packages that share a name usually part at the end, so the start gives way.
                    overflow = TextOverflow.StartEllipsis,
                )
            }
        }
        if (unread > 0) {
            // Read as part of the row: a description here would speak for the whole row and silence its name.
            Text(
                text = unread.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp).clearAndSetSemantics { text = AnnotatedString("$unread unread") },
            )
        }
        if (picked) {
            when (picking?.mark) {
                PickMark.Dot -> Dot(Modifier.padding(start = 12.dp))
                else -> Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        entry.menu?.content?.invoke(app)
    }
}

/**
 * An app as a cell of the grid: its icon, wearing its unread badge and the mark of a pick, over its name. The whole
 * cell is the target.
 */
@Composable
private fun AppCell(app: AppEntry, detail: String?, entry: DrawerEntry, modifier: Modifier = Modifier) {
    val picking = entry.picking
    val unread = entry.unread[app]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(entry.action(app))
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Box(Modifier.size(GRID_ICON_SIZE)) {
            IconDisc(modifier = Modifier.fillMaxSize()) { AppImage(app, entry.icon, Modifier.fillMaxSize()) }
            UnreadBadge(unread, Modifier.align(Alignment.TopEnd).clearAndSetSemantics { text = AnnotatedString("$unread unread") })
            if (picking?.isPicked(app) == true) {
                when (picking.mark) {
                    PickMark.Dot -> Dot(Modifier.align(Alignment.BottomEnd))
                    PickMark.Check -> Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .size(18.dp)
                            .padding(2.dp),
                    )
                }
            }
            entry.menu?.content?.invoke(app)
        }
        AppLabel(app.label)
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.StartEllipsis,
            )
        }
    }
}

/** Marks a row whose app was just added: read as part of the row, like the unread count. */
@Composable
private fun Dot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .clearAndSetSemantics { text = AnnotatedString("added") },
    )
}

/**
 * One touch target for the whole rail rather than a button per letter: a finger sliding down it should keep jumping,
 * and a tap between two letters should still land on one of them.
 */
@Composable
private fun LetterRail(initials: List<Char>, highlighted: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    // The gesture loop outlives recompositions, so it reads the latest letters and targets through these.
    val latestInitials by rememberUpdatedState(initials)
    val latestOnSelect by rememberUpdatedState(onSelect)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(RAIL_WIDTH)
            .pointerInput(Unit) {
                awaitEachGesture {
                    var selected = -1
                    fun select(y: Float) {
                        val count = latestInitials.size
                        if (count == 0) return
                        val index = (y / size.height * count).toInt().coerceIn(0, count - 1)
                        if (index != selected) {
                            selected = index
                            latestOnSelect(index)
                        }
                    }

                    val down = awaitFirstDown()
                    select(down.position.y)
                    // Consumed so the sheet underneath does not read a slide down the rail as a drag to close.
                    drag(down.id) { change ->
                        change.consume()
                        select(change.position.y)
                    }
                }
            },
    ) {
        initials.forEachIndexed { index, initial ->
            val isHighlighted = index == highlighted
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight()
                    .semantics { selected = isHighlighted }
                    .testTag(AppDrawerTags.letter(initial)),
            )
        }
    }
}
