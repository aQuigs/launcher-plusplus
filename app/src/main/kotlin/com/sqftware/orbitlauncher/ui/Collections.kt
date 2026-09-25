package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.materialIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sqftware.orbitlauncher.domain.AppCategory
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.Bounds
import com.sqftware.orbitlauncher.domain.CREATE_YOUR_OWN
import com.sqftware.orbitlauncher.domain.CollectionCard
import com.sqftware.orbitlauncher.domain.CollectionKind
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.ForegroundTime
import com.sqftware.orbitlauncher.domain.MAX_COLLECTION_NAME
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.mostUsed
import com.sqftware.orbitlauncher.domain.newApps
import com.sqftware.orbitlauncher.domain.title
import com.sqftware.orbitlauncher.ui.theme.GlyphFill
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

object CollectionTags {
    const val ADD = "collection_add"
    const val BIN = "collection_bin"
    const val PICKER = "collection_picker"
    const val EDITOR = "collection_editor"
    const val NOTICE = "collection_notice"
    const val CREATE = "collection_create"
    const val CREATE_DIALOG = "collection_create_dialog"
    const val CREATE_NAME = "collection_create_name"

    fun card(kind: CollectionKind) = "collection_${kind.name}"

    fun handle(kind: CollectionKind) = "collection_handle_${kind.name}"

    fun chevron(kind: CollectionKind) = "collection_chevron_${kind.name}"

    fun edit(kind: CollectionKind) = "collection_edit_${kind.name}"

    fun tile(kind: CollectionKind) = "collection_tile_${kind.name}"

    fun app(kind: CollectionKind, app: AppEntry) = "collection_${kind.name}_${app.key}"
}

/** The bin a lifted app can be dropped on: whether the finger is over it, and where it lies, in root coordinates. */
class BinTarget(val highlighted: Boolean, val onPositioned: (Bounds) -> Unit)

/** What a card says instead of apps when it has none: some are filled by hand, the built-in ones by the system. */
private val CollectionKind.emptyText: String
    get() = when (this) {
        CollectionKind.NewApps -> "No apps yet"
        CollectionKind.MostUsed -> "Nothing used this week"
        is CollectionKind.HandPicked -> "Tap the pencil to add apps"
    }

private const val APPS_PER_ROW = 5
private val PAGE_PADDING = 16.dp
private val CARD_GAP = 12.dp
private val CARD_ICON_SIZE = 48.dp
private val ROW_GAP = 12.dp
private val BIN_SIZE = 72.dp
private const val NOTICE_MILLIS = 2_000L

/**
 * The collection cards on [page], top to bottom, and a button under them to add one. A card's header names it and
 * carries a handle to drag it above or below the others, a pencil on a hand-picked card that calls [onEdit], and a
 * chevron that calls [onToggleExpanded]: a compact card shows one row of its first apps, an expanded one every app with
 * its label. The built-in cards work their apps out from [apps] and [foregroundTime], and Most Used asks for the usage
 * access it lacks with a body that calls [onOpenUsageSettings]. A tap launches an app; a long press on a hand-picked
 * card's app lifts it through that card's [rearrange], to move it among the card's apps or, while the [bin] sits at the
 * bottom of the page, to drop it there. While one of its apps is on the move, a card shows where they would be if it
 * were dropped. Apps wear their [unread] counts.
 */
@Composable
fun CollectionsColumn(
    page: CollectionsPage,
    apps: List<AppEntry>,
    foregroundTime: ForegroundTime?,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onToggleExpanded: (CollectionKind) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onEdit: (CollectionKind) -> Unit,
    onAdd: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    modifier: Modifier = Modifier,
    rearrange: (CollectionKind.HandPicked) -> Rearrange? = { null },
    bin: BinTarget? = null,
    unread: UnreadCounts = UnreadCounts(),
) {
    val newApps = remember(apps) { newApps(apps) }
    val mostUsed = remember(apps, foregroundTime) { foregroundTime?.let { mostUsed(apps, it) } }
    val gap = with(LocalDensity.current) { CARD_GAP.toPx() }
    val reorder = remember(gap) { ListReorder(gap) }
    SideEffect { reorder.count = page.cards.size }

    Box(modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(CARD_GAP),
            modifier = Modifier.fillMaxSize().verticalPageScroll().padding(PAGE_PADDING),
        ) {
            page.cards.forEachIndexed { index, card ->
                key(card.kind.name) {
                    val handPicked = card.kind as? CollectionKind.HandPicked
                    val cardApps = when (card.kind) {
                        CollectionKind.NewApps -> newApps
                        CollectionKind.MostUsed -> mostUsed
                        is CollectionKind.HandPicked -> remember(card.apps, apps) { card.apps.resolve(apps) }
                    }
                    CollectionCardView(
                        card = card,
                        apps = cardApps,
                        index = index,
                        reorder = reorder,
                        icon = icon,
                        onLaunch = onLaunch,
                        onToggleExpanded = { onToggleExpanded(card.kind) },
                        onMove = onMove,
                        onEdit = if (handPicked != null) ({ onEdit(card.kind) }) else null,
                        rearrange = handPicked?.let(rearrange),
                        onOpenUsageSettings = onOpenUsageSettings,
                        unread = unread,
                    )
                }
            }
            FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().testTag(CollectionTags.ADD)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Add Collection")
            }
        }
        if (bin != null) Bin(bin, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

@Composable
private fun CollectionCardView(
    card: CollectionCard,
    apps: List<AppEntry>?,
    index: Int,
    reorder: ListReorder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onToggleExpanded: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onEdit: (() -> Unit)?,
    rearrange: Rearrange?,
    onOpenUsageSettings: () -> Unit,
    unread: UnreadCounts,
) {
    val dragged = reorder.dragging == index
    val colours = MaterialTheme.colorScheme
    // The others slide aside while a card is dragged, and snap back the moment it lands: they are then laid out where they
    // slid to, and animating from there would take them somewhere else first.
    val shift by animateFloatAsState(
        targetValue = if (dragged) 0f else reorder.shift(index),
        animationSpec = if (reorder.dragging == null) snap() else spring(),
        label = "card_shift",
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        // Solid while dragged, so the card it passes over does not show through it.
        colors = CardDefaults.cardColors(containerColor = if (dragged) colours.surfaceContainerHigh else colours.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            // Measured outside the layer below, so the card's resting place is recorded, not where it has been dragged to.
            .onGloballyPositioned { reorder.place(index, it.positionInParent().y, it.size.height.toFloat()) }
            .zIndex(if (dragged) 1f else 0f)
            .graphicsLayer {
                translationY = if (dragged) reorder.offset else shift
                alpha = if (dragged) 0.7f else 1f
            }
            .testTag(CollectionTags.card(card.kind)),
    ) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, bottom = 12.dp)) {
            CardHeader(card.kind, card.expanded, index, reorder, onMove, onToggleExpanded, onEdit)
            if (apps == null) {
                PermissionRequired(onOpenUsageSettings)
            } else {
                AppGrid(card.kind, apps, card.expanded, icon, onLaunch, rearrange, unread)
            }
        }
    }
}

@Composable
private fun CardHeader(
    kind: CollectionKind,
    expanded: Boolean,
    index: Int,
    reorder: ListReorder,
    onMove: (from: Int, to: Int) -> Unit,
    onToggleExpanded: () -> Unit,
    onEdit: (() -> Unit)?,
) {
    val latestOnMove by rememberUpdatedState(onMove)
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Box(Modifier.fillMaxWidth().height(48.dp)) {
        // Kept clear of the handle in the middle, which a long title on a narrow screen would otherwise run under.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.5f).padding(end = 28.dp),
        ) {
            Icon(kind.glyph, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                text = kind.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        // A drag straight from the touch, with no long press: a vertical one, so a swipe across the handle still pages.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .pointerInput(index) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var overSlop = 0f
                        val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                            change.consume()
                            overSlop = over
                        } ?: return@awaitEachGesture
                        reorder.dragging = index
                        reorder.offset = overSlop
                        // Not verticalDrag, which leaves a move straight across to the pager and so loses the card to it.
                        try {
                            val dragged = drag(start.id) {
                                reorder.offset += it.positionChange().y
                                it.consume()
                            }
                            if (dragged) {
                                reorder.end()?.let { (from, to) -> if (from != to) latestOnMove(from, to) }
                            }
                        } finally {
                            reorder.end()
                        }
                    }
                }
                .semantics { contentDescription = "Reorder ${kind.title}" }
                .testTag(CollectionTags.handle(kind)),
        ) {
            Icon(DragHandleGlyph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Row(modifier = Modifier.align(Alignment.CenterEnd)) {
            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.testTag(CollectionTags.edit(kind))) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${kind.title}")
                }
            }
            IconButton(onClick = onToggleExpanded, modifier = Modifier.testTag(CollectionTags.chevron(kind))) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Show fewer" else "Show all",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                )
            }
        }
    }
}

/**
 * The card's apps in rows of five: the first row alone when not [expanded], every row with labels when it is. One layout
 * holds every row, so an app on the move keeps its node, and the gesture, as it goes from row to row.
 */
@Composable
private fun AppGrid(
    kind: CollectionKind,
    apps: List<AppEntry>,
    expanded: Boolean,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    rearrange: Rearrange?,
    unread: UnreadCounts,
) {
    val moving = rearrange?.moving
    val ordered = moving.shown(apps)
    val shown = if (expanded) ordered else ordered.take(APPS_PER_ROW)
    val modifier = Modifier.fillMaxWidth().padding(end = 8.dp).defaultMinSize(minHeight = CARD_ICON_SIZE)

    if (shown.isEmpty()) {
        Text(
            text = kind.emptyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 12.dp),
        )
        return
    }
    Layout(
        content = {
            shown.forEachIndexed { index, app ->
                key(app.key) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppIcon(
                            app = app,
                            icon = icon,
                            onLaunch = onLaunch,
                            modifier = Modifier
                                .size(CARD_ICON_SIZE)
                                .testTag(CollectionTags.app(kind, app))
                                .reorderSlot(rearrange, index, moving?.at == index),
                            drag = rearrange?.drag(index),
                            unread = unread[app],
                        )
                        if (expanded) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = constraints.maxWidth / APPS_PER_ROW
        val rows = measurables.map { it.measure(Constraints.fixedWidth(width)) }.chunked(APPS_PER_ROW)
        val heights = rows.map { row -> row.maxOf { it.height } }
        val gap = ROW_GAP.roundToPx()
        val height = maxOf(heights.sum() + gap * (rows.size - 1), constraints.minHeight)

        layout(constraints.maxWidth, height) {
            var top = 0
            rows.forEachIndexed { row, cells ->
                cells.forEachIndexed { column, cell -> cell.placeRelative(column * width, top) }
                top += heights[row] + gap
            }
        }
    }
}

// The whole body opens the settings, as Arc's does; the button is there so it is plain what to do.
@Composable
private fun PermissionRequired(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Open the usage access settings", onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text("Permission Required", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Lets Orbit see which apps you use most.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        FilledTonalButton(onClick = onClick) { Text("Allow usage access") }
    }
}

@Composable
private fun Bin(bin: BinTarget, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (bin.highlighted) 1.2f else 1f, label = "bin")
    val colours = MaterialTheme.colorScheme

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(BIN_SIZE)
            .onGloballyPositioned { bin.onPositioned(it.rootBounds()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(if (bin.highlighted) colours.error else colours.surfaceContainerHigh)
            .semantics { contentDescription = "Remove from the collection" }
            .testTag(CollectionTags.BIN),
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, tint = if (bin.highlighted) colours.onError else colours.onSurfaceVariant)
    }
}

/**
 * Select Collection: every built-in kind as a tile, then the [customs], then Create Your Own, which calls [onCreate]; a
 * tile is lit when its card is on [page]. A tap on any other tile calls [onToggle] and says what it did in a label that
 * fades after a moment; the grid stays until Back.
 */
@Composable
fun CollectionPicker(
    page: CollectionsPage,
    customs: List<CollectionKind>,
    onToggle: (CollectionKind) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var notice by remember { mutableStateOf<Notice?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            notice = null
        }
    }

    Panel(modifier.testTag(CollectionTags.PICKER)) {
        Box {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(PAGE_PADDING),
                horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
                verticalArrangement = Arrangement.spacedBy(CARD_GAP),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Select Collection", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(CollectionKind.all + customs, key = { it.name }) { kind ->
                    val onPage = kind in page
                    Tile(
                        // As in Arc Pro: a person on a custom collection's tile, the grid on its card.
                        glyph = if (kind is CollectionKind.Custom) Icons.Default.Person else kind.glyph,
                        title = kind.title,
                        selected = onPage,
                        onClick = {
                            // Numbered so a second tap restarts the fade instead of sharing the first one's.
                            notice = Notice(if (onPage) "Collection Removed" else "Collection Added", (notice?.serial ?: 0) + 1)
                            onToggle(kind)
                        },
                        modifier = Modifier.testTag(CollectionTags.tile(kind)),
                    )
                }
                item(key = CollectionTags.CREATE) {
                    Tile(
                        glyph = GridGlyph,
                        title = CREATE_YOUR_OWN,
                        selected = false,
                        onClick = onCreate,
                        modifier = Modifier.testTag(CollectionTags.CREATE),
                    )
                }
            }
            notice?.let { NoticeLabel(it.text, Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)) }
        }
    }
}

private data class Notice(val text: String, val serial: Int)

/**
 * Create Your Own: names a custom collection. Ok waits for a name [page] would take (see [CollectionsPage.custom]) and
 * calls [onCreate] with it; Back and a tap outside call [onDismiss]. Like Arc's, it has no Cancel.
 */
@Composable
fun CreateCollectionDialog(page: CollectionsPage, onCreate: (CollectionKind.Custom) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val kind = page.custom(name)
    val taken = kind == null && name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { kind?.let(onCreate) }, enabled = kind != null) { Text("Ok") } },
        title = { Text(CREATE_YOUR_OWN) },
        text = {
            // In the dialog's own window, and only once that window has focus: a text field focused before then gets no
            // keyboard.
            val focus = remember { FocusRequester() }
            val window = LocalWindowInfo.current
            LaunchedEffect(window) {
                snapshotFlow { window.isWindowFocused }.first { it }
                focus.requestFocus()
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_COLLECTION_NAME) },
                label = { Text("Collection Name") },
                singleLine = true,
                isError = taken,
                supportingText = if (taken) ({ Text("That name is taken") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { kind?.let(onCreate) }),
                modifier = Modifier.focusRequester(focus).testTag(CollectionTags.CREATE_NAME),
            )
        },
        modifier = Modifier.testTag(CollectionTags.CREATE_DIALOG),
    )
}

@Composable
private fun Tile(glyph: ImageVector, title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colours = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colours.primaryContainer else colours.surfaceVariant,
        modifier = modifier
            .aspectRatio(1f)
            .semantics { this.selected = selected },
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(glyph, contentDescription = null, modifier = Modifier.size(36.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

// A plain box rather than a surface: it must not take the taps meant for the tiles under it.
@Composable
private fun NoticeLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag(CollectionTags.NOTICE),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.inverseOnSurface)
    }
}

/**
 * Adding apps to a hand-picked card, named by [title]: the drawer's list of every app, with its search and rail, where a
 * tap calls [onPick] and the rows [isPicked] says wear a dot, without check marks, since a tap only ever adds.
 */
@Composable
fun CollectionEditor(
    title: String,
    apps: List<AppEntry>,
    icon: suspend (AppEntry) -> ImageBitmap?,
    isPicked: (AppEntry) -> Boolean,
    onPick: (AppEntry) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Panel(modifier.testTag(CollectionTags.EDITOR)) {
        AppDrawer(
            apps = apps,
            icon = icon,
            onLaunch = {},
            picking = Picking(
                header = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    )
                },
                isPicked = isPicked,
                onToggle = onPick,
                mark = PickMark.Dot,
            ),
            query = query,
            onQueryChange = onQueryChange,
        )
    }
}

val CollectionKind.glyph: ImageVector
    get() = when (this) {
        CollectionKind.NewApps -> SparkleGlyph
        CollectionKind.MostUsed -> BarChartGlyph
        is CollectionKind.Category -> category.glyph
        is CollectionKind.Custom -> GridGlyph
    }

private val AppCategory.glyph: ImageVector
    get() = when (this) {
        AppCategory.Business -> WorkGlyph
        AppCategory.Communication -> ChatGlyph
        AppCategory.Entertainment -> Icons.Default.Star
        AppCategory.Games -> GamepadGlyph
        AppCategory.Kids -> Icons.Default.Face
        AppCategory.LifeStyle -> Icons.Default.Favorite
        AppCategory.Media -> MovieGlyph
        AppCategory.Music -> MusicGlyph
        AppCategory.Personalisation -> BrushGlyph
        AppCategory.Photos -> ImageGlyph
        AppCategory.Productivity -> TrendingUpGlyph
        AppCategory.Shopping -> Icons.Default.ShoppingCart
        AppCategory.Social -> Icons.Default.Person
        AppCategory.Tools -> Icons.Default.Build
        AppCategory.Transport -> Icons.Default.LocationOn
        AppCategory.Video -> Icons.Default.PlayArrow
    }

// Material glyphs the core icon set leaves out, from their published path data.
internal fun materialGlyph(name: String, pathData: String): ImageVector =
    materialIcon(name) { addPath(addPathNodes(pathData), fill = SolidColor(GlyphFill)) }

private val DragHandleGlyph = materialGlyph("DragHandle", "M20 9H4v2h16V9zM4 15h16v-2H4v2z")
private val BarChartGlyph = materialGlyph("BarChart", "M4 9h4v11H4zM10 4h4v16h-4zM16 13h4v7h-4z")
private val SparkleGlyph = materialGlyph(
    "AutoAwesome",
    "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12" +
        "l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z",
)
private val GridGlyph = materialGlyph(
    "Apps",
    "M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4z" +
        "m0 6h4v-4h-4v4z",
)
private val WorkGlyph = materialGlyph(
    "Work",
    "M20 6h-4V4c0-1.11-.89-2-2-2h-4c-1.11 0-2 .89-2 2v2H4c-1.11 0-1.99.89-1.99 2L2 19c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V8" +
        "c0-1.11-.89-2-2-2zm-6 0h-4V4h4v2z",
)
private val ChatGlyph = materialGlyph("ChatBubble", "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z")
private val GamepadGlyph = materialGlyph(
    "VideogameAsset",
    "M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-10 7H8v3H6v-3H3v-2h3V8h2v3h3v2zm4.5 2" +
        "c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm4-3c-.83 0-1.5-.67-1.5-1.5S18.67 9 19.5 9" +
        "s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z",
)
private val MovieGlyph = materialGlyph(
    "Movie",
    "M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4z",
)
private val MusicGlyph = materialGlyph("MusicNote", "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z")
private val BrushGlyph = materialGlyph(
    "Brush",
    "M7 14c-1.66 0-3 1.34-3 3 0 1.31-1.16 2-2 2 .92 1.22 2.49 2 4 2 2.21 0 4-1.79 4-4 0-1.66-1.34-3-3-3zm13.71-9.37" +
        "l-1.34-1.34c-.39-.39-1.02-.39-1.41 0L9 12.25 11.75 15l8.96-8.96c.39-.39.39-1.02 0-1.41z",
)
private val ImageGlyph = materialGlyph(
    "Image",
    "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
)
private val TrendingUpGlyph = materialGlyph("TrendingUp", "M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6z")
