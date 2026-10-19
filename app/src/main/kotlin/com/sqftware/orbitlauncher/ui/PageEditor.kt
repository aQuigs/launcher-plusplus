package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.PageContents
import com.sqftware.orbitlauncher.domain.PageKind
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.WIDGET_COLUMNS
import com.sqftware.orbitlauncher.domain.edgePull
import com.sqftware.orbitlauncher.ui.theme.LocalBoxColors
import com.sqftware.orbitlauncher.ui.theme.Spacing
import com.sqftware.orbitlauncher.ui.theme.sidewaysScroll

object PageEditorTags {
    const val EDITOR = "page_editor"
    const val DELETE_DIALOG = "page_delete_dialog"

    fun page(page: LauncherPage) = "page_card_${page.id}"

    fun delete(page: LauncherPage) = "page_delete_${page.id}"

    fun home(page: LauncherPage) = "page_home_${page.id}"

    fun add(atStart: Boolean) = if (atStart) "page_add_start" else "page_add_end"
}

private val CARD_WIDTH = 88.dp
private val CARD_HEIGHT = 176.dp
// As far as a lifted card rises and tilts out of the strip.
private val LIFT_ROOM = 8.dp

// A finger dragging a page this near either end of the strip scrolls it, and at the edge itself this fast, a second.
private val EDGE_ZONE = 48.dp
private val EDGE_SPEED = 600.dp

/**
 * The pages of [layout] as a strip of cards in swipe order, each drawn from what [contentsOf] says it holds. A tap on a
 * card hands its page to [onOpen]; a long press lifts it, to drag it along the strip to another place. A + at either end
 * adds a page of the kind picked in a dialog there, until the layout is full. Each card's house makes its page home, the
 * home page's filled in. Every page but
 * the clock's ring has an × that deletes it, at once if it is empty, else once a dialog naming what it holds has asked;
 * deleting home makes the clock's ring home again. Each change is handed to [onLayoutChange].
 */
@Composable
fun PageEditor(
    layout: PageLayout,
    contentsOf: (LauncherPage) -> PageContents,
    onLayoutChange: (PageLayout) -> Unit,
    onOpen: (LauncherPage) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var addingAtStart by remember { mutableStateOf<Boolean?>(null) }
    var deleting by remember { mutableStateOf<LauncherPage?>(null) }
    val contents = remember(layout, contentsOf) { layout.pages.associateWith(contentsOf) }
    val latestLayout by rememberUpdatedState(layout)
    val latestOnLayoutChange by rememberUpdatedState(onLayoutChange)
    val density = LocalDensity.current
    val rightToLeft = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reorder = remember(density, rightToLeft) { ListReorder(with(density) { Spacing.Gap.toPx() }, horizontal = true, mirrored = rightToLeft) }
    SideEffect { reorder.count = layout.pages.size }
    val scroll = rememberScrollState()
    var finger by remember { mutableFloatStateOf(Float.NaN) }

    // The strip scrolls under a finger holding a page near either end, and the page moves with it, so it stays held.
    LaunchedEffect(reorder.dragging != null) {
        if (reorder.dragging == null) return@LaunchedEffect
        val zone = with(density) { EDGE_ZONE.toPx() }
        val speed = with(density) { EDGE_SPEED.toPx() }
        val towardsEnd = if (rightToLeft) -1f else 1f
        scroll.scrollAtEdges(speed, pull = { towardsEnd * edgePull(finger, scroll.viewportSize.toFloat(), zone) }) { reorder.offset += it }
    }

    Panel(
        "Pages",
        onClose,
        modifier.testTag(PageEditorTags.EDITOR),
        subtitle = "In the order you swipe through them. Hold a page and drag to move it; tap one to go to it, or its house to make it home.",
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Gap),
            modifier = Modifier
                .padding(top = Spacing.Top)
                // Watched ahead of the cards, which take the touches they drag with, and before the scroll, so in
                // the strip's own place on screen.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) finger = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed }?.position?.x ?: Float.NaN
                    }
                }
                .sidewaysScroll(scroll, bleed = LIFT_ROOM),
        ) {
            if (!layout.isFull) AddTile(atStart = true, onClick = { addingAtStart = true })
            layout.pages.forEachIndexed { index, page ->
                key(page.id) {
                    val held = contents.getValue(page)
                    PageCard(
                        page = page,
                        label = layout.label(page),
                        home = page == layout.home,
                        contents = held,
                        lifted = reorder.dragging == index,
                        // A long press let go without moving reaches the card's tap first, and opens nothing.
                        onOpen = { if (reorder.dragging == null) onOpen(page) },
                        onDelete = { if (held.isEmpty) onLayoutChange(layout.remove(page)) else deleting = page },
                        onMakeHome = { onLayoutChange(layout.withHome(page)) },
                        modifier = Modifier
                            .reorderItem(reorder, index)
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { reorder.dragging = index },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        reorder.moveBy(amount.x)
                                    },
                                    onDragEnd = {
                                        reorder.end()?.let { (from, to) ->
                                            if (from != to) latestOnLayoutChange(latestLayout.move(from, to))
                                        }
                                    },
                                    onDragCancel = { reorder.end() },
                                )
                            },
                    )
                }
            }
            if (!layout.isFull) AddTile(atStart = false, onClick = { addingAtStart = false })
        }
    }

    addingAtStart?.let { atStart ->
        val side = if (atStart != rightToLeft) "left" else "right"
        ChoiceDialog(
            title = "Add a page on the $side",
            choices = PageKind.entries,
            chosen = null,
            label = { it.title },
            description = { it.blurb },
            onChoose = { kind ->
                addingAtStart = null
                onLayoutChange(layout.add(kind, atStart))
            },
            onDismiss = { addingAtStart = null },
        )
    }
    deleting?.let { page ->
        DeletePageDialog(
            label = layout.label(page),
            contents = contents.getValue(page),
            onDelete = {
                deleting = null
                onLayoutChange(layout.remove(page))
            },
            onDismiss = { deleting = null },
        )
    }
}

private val PageKind.blurb: String
    get() = when (this) {
        PageKind.Ring -> "Its own ring of apps and folders"
        PageKind.Collections -> "Cards of apps: new, most used, by category"
        PageKind.Widgets -> "A grid of widgets"
        PageKind.Grid -> "A plain grid of apps, folders and widgets"
    }

@Composable
private fun PageCard(
    page: LauncherPage,
    label: String,
    home: Boolean,
    contents: PageContents,
    lifted: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onMakeHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MaterialTheme.colorScheme
    val isClockRing = page == LauncherPage.Home
    val marked = home || lifted

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.testTag(PageEditorTags.page(page))) {
        Pane(
            Modifier
                .size(CARD_WIDTH, CARD_HEIGHT)
                .graphicsLayer {
                    if (lifted) {
                        translationY = -8.dp.toPx()
                        rotationZ = -3f
                    }
                },
            shape = MaterialTheme.shapes.medium,
            marked = marked,
            onClick = onOpen,
        ) {
            Box(Modifier.fillMaxSize()) {
                PagePreview(contents, isClockRing, Modifier.matchParentSize())
                IconButton(
                    onClick = onMakeHome,
                    enabled = !home,
                    modifier = Modifier.size(32.dp).testTag(PageEditorTags.home(page)),
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = if (home) "Home page" else "Make $label the home page",
                        tint = if (home) colours.onPrimary else colours.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .background(if (home) colours.primary else LocalBoxColors.current.floating, CircleShape)
                            .padding(3.dp),
                    )
                }
                if (!isClockRing) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopEnd).size(32.dp).testTag(PageEditorTags.delete(page)),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete $label",
                            modifier = Modifier.size(20.dp).background(LocalBoxColors.current.floating, CircleShape).padding(3.dp),
                        )
                    }
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        Text(contents.summary() ?: "Empty", style = MaterialTheme.typography.labelSmall, color = colours.onSurfaceVariant)
    }
}

/** What [this] holds, in a few words, or null when it holds nothing. */
private fun PageContents.summary(): String? {
    if (isEmpty) return null
    return when (this) {
        is PageContents.Ring -> itemsSummary(apps, folders, widgets)
        is PageContents.Cards -> counted(cards, "collection")
        is PageContents.Widgets -> counted(widgets.size, "widget")
        is PageContents.Grid -> itemsSummary(apps, folders, widgets).ifEmpty { "the clock" }
    }
}

private fun itemsSummary(apps: Int, folders: Int, widgets: Int): String = listOfNotNull(
    counted(apps, "app").takeIf { apps > 0 },
    counted(folders, "folder").takeIf { folders > 0 },
    counted(widgets, "widget").takeIf { widgets > 0 },
).joinToString(" and ")

/** A sketch of a page: the ring and its slots, with home's clock and dock, the cards, or the widgets where they sit. */
@Composable
private fun PagePreview(contents: PageContents, isClockRing: Boolean, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.outline
    val mark = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        when (contents) {
            is PageContents.Ring -> {
                if (isClockRing) drawRoundRect(line, Offset(size.width * 0.3f, size.height * 0.1f), Size(size.width * 0.4f, 6.dp.toPx()), CornerRadius(3.dp.toPx()))
                val centre = Offset(size.width / 2, size.height * if (isClockRing) 0.48f else 0.45f)
                val radius = size.width * 0.34f
                drawCircle(line, radius, centre, style = Stroke(1.dp.toPx()))
                drawCircle(line, radius * 0.4f, centre, style = Stroke(1.dp.toPx()))
                repeat(contents.slots) { slot ->
                    val angle = Math.toRadians(-90.0 + 360.0 * slot / contents.slots)
                    drawCircle(mark, 4.dp.toPx(), centre + Offset((radius * Math.cos(angle)).toFloat(), (radius * Math.sin(angle)).toFloat()))
                }
                if (contents.dock > 0) {
                    val step = size.width / (contents.dock + 1)
                    repeat(contents.dock) { drawCircle(mark, 4.dp.toPx(), Offset(step * (it + 1), size.height * 0.88f)) }
                }
            }
            is PageContents.Cards -> repeat(minOf(contents.cards, 6)) { card ->
                val top = 10.dp.toPx() + card * 26.dp.toPx()
                drawRoundRect(line, Offset(7.dp.toPx(), top), Size(size.width - 14.dp.toPx(), 20.dp.toPx()), CornerRadius(4.dp.toPx()), style = Stroke(1.dp.toPx()))
                repeat(4) { drawCircle(mark, 3.dp.toPx(), Offset(16.dp.toPx() + it * 14.dp.toPx(), top + 10.dp.toPx())) }
            }
            is PageContents.Widgets, is PageContents.Grid -> {
                val inset = 6.dp.toPx()
                val cell = (size.width - inset * 2) / WIDGET_COLUMNS
                val blocks = if (contents is PageContents.Grid) contents.blocks else (contents as PageContents.Widgets).widgets
                blocks.forEach { widget ->
                    drawRoundRect(
                        mark,
                        Offset(inset + widget.column * cell + 1.dp.toPx(), inset + widget.row * cell + 1.dp.toPx()),
                        Size(widget.columns * cell - 2.dp.toPx(), widget.rows * cell - 2.dp.toPx()),
                        CornerRadius(3.dp.toPx()),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTile(atStart: Boolean, onClick: () -> Unit) {
    val edge = MaterialTheme.colorScheme.outline
    // The page cards' corners, so it reads as a page yet to be.
    val shape = MaterialTheme.shapes.medium

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp, CARD_HEIGHT)
            .drawBehind {
                val stroke = 1.dp.toPx()
                inset(stroke / 2) {
                    drawOutline(
                        shape.createOutline(size, layoutDirection, this),
                        edge,
                        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                    )
                }
            }
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(PageEditorTags.add(atStart)),
    ) {
        Icon(Icons.Default.Add, contentDescription = if (atStart) "Add a page at the start" else "Add a page at the end", tint = MaterialTheme.colorScheme.primary)
    }
}

/** Asks before deleting the page [label] names, saying what goes with it. Back and a tap outside call [onDismiss]. */
@Composable
private fun DeletePageDialog(label: String, contents: PageContents, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val what = contents.summary().orEmpty()

    ConfirmDialog(
        title = "Delete $label?",
        text = when (contents) {
            is PageContents.Ring -> "This takes $what off the home screen. The apps stay installed and in the drawer."
            is PageContents.Cards -> "This deletes $what. The apps stay installed."
            is PageContents.Widgets -> "This removes $what."
            is PageContents.Grid -> "This takes $what off the home screen. The apps stay installed and in the drawer."
        },
        verb = "Delete",
        onConfirm = onDelete,
        onDismiss = onDismiss,
        destructive = true,
        modifier = Modifier.testTag(PageEditorTags.DELETE_DIALOG),
    )
}
