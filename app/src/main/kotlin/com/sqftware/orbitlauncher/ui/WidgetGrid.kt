package com.sqftware.orbitlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.RING_ID
import com.sqftware.orbitlauncher.domain.WIDGET_COLUMNS
import com.sqftware.orbitlauncher.domain.WIDGET_GAP_DP
import com.sqftware.orbitlauncher.domain.WIDGET_ROW_HEIGHT_DP
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetSizing
import com.sqftware.orbitlauncher.domain.cellRange
import com.sqftware.orbitlauncher.domain.edgePull
import com.sqftware.orbitlauncher.domain.heldDrag
import com.sqftware.orbitlauncher.domain.nearestCells
import com.sqftware.orbitlauncher.domain.roomFor
import com.sqftware.orbitlauncher.domain.widgetCells
import com.sqftware.orbitlauncher.domain.widgetRowsWithin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.math.abs
import kotlin.math.roundToInt

object WidgetTags {
    const val ADD = "widget_add"
    const val EDIT = "widget_edit"
    const val REMOVE = "widget_remove"
    const val RESIZE = "widget_resize"
    const val LANDING = "widget_landing"
    const val RING = "widget_ring"

    fun widget(id: Int) = "widget_$id"
}

/**
 * What a page of widgets asks of the system: a widget's view, a new one for a page of so many rows of cells so big, a
 * removal, every widget and the ring put where a page has them once one is moved or resized, and how a widget's
 * provider lets it be resized.
 */
data class WidgetActions(
    val view: (Context, Int) -> View,
    val add: (pageRows: Int, columnWidthDp: Float, rowHeightDp: Float) -> Unit,
    val remove: (Int) -> Unit,
    val arrange: (WidgetPage) -> Unit,
    val sizing: (Int) -> WidgetSizing,
)

/** The rows a page of widgets shows, and how a widget moved or stretched on it lands. */
private sealed interface PageRows {
    val count: Int

    /** The rows a widget may be dragged down to. */
    fun reach(page: WidgetPage): Int

    fun move(page: WidgetPage, id: Int, row: Int, column: Int): WidgetPage?

    fun resize(page: WidgetPage, id: Int, rows: Int, columns: Int): WidgetPage?
}

/** The widgets page's rows, which go on as far as its widgets do. */
private data class Lengthening(override val count: Int) : PageRows {
    // A screen's worth past the page's end, or past the button's row when the widgets end above it, so one drag
    // lengthens the page by a screen at most and a slip scrolls no further.
    override fun reach(page: WidgetPage) = maxOf(count, page.rows) + count

    override fun move(page: WidgetPage, id: Int, row: Int, column: Int) = page.move(id, row, column)

    override fun resize(page: WidgetPage, id: Int, rows: Int, columns: Int) = page.resize(id, rows, columns)
}

/** A ring page's rows, which never lengthen, so a widget lands only where it and those in its way fit. */
private data class Fixed(override val count: Int) : PageRows {
    // Past the last row only for a page stored on a taller screen, so a widget held there has rows to be dragged about.
    override fun reach(page: WidgetPage) = maxOf(count, page.rows)

    override fun move(page: WidgetPage, id: Int, row: Int, column: Int) = page.moveWithin(id, row, column, count)

    override fun resize(page: WidgetPage, id: Int, rows: Int, columns: Int) = page.resizeWithin(id, rows, columns, count)
}

private val GAP = WIDGET_GAP_DP.dp
private val PAGE_PADDING = 16.dp
private val CONTROL_SIZE = 40.dp

// How far the edit controls reach past the outline's corners. Any reach keeps the bin and the handle apart on a widget
// one row tall, since each is half a row; half the page's padding keeps them clear of the screen's edge.
private val CONTROL_REACH = PAGE_PADDING / 2

// A finger this near the page's top or bottom edge, dragging a widget or its handle, scrolls the page, and at the edge
// itself this fast, a second.
private val EDGE_ZONE = 72.dp
private val EDGE_SPEED = 900.dp

/** The length of [cells] cells [cell] long and the gaps between them. */
private fun span(cells: Int, cell: Dp) = cell * cells + GAP * (cells - 1)

/**
 * The widget held to be moved, how far the finger has dragged it and how far the page has scrolled under it since, in
 * pixels.
 */
private class HeldWidget {
    var id by mutableStateOf<Int?>(null)
    var offset by mutableStateOf(Offset.Zero)
    var scrolled by mutableFloatStateOf(0f)

    /** Whether [id] was picked up: not while another is held, so a second finger cannot take over. */
    fun pickUp(id: Int): Boolean {
        if (this.id != null) return false
        this.id = id
        offset = Offset.Zero
        scrolled = 0f
        return true
    }

    /**
     * How far [widget] is dragged, and the page scrolled under it, kept within the columns and the first [rows] rows, a
     * cell and its gap being [pitch] across and down.
     */
    fun shift(widget: HostedWidget, pitch: Offset, rows: Int) = Offset(
        offset.x.coerceIn(-widget.column * pitch.x, (WIDGET_COLUMNS - widget.end) * pitch.x),
        (offset.y + scrolled).coerceIn(drop(widget, pitch, rows)),
    )

    /** How much of a scroll of [px] the held [widget] can still follow, once the finger has dragged it at all. */
    fun room(px: Float, widget: HostedWidget, pitch: Offset, rows: Int, slop: Float): Float {
        if (offset.getDistance() <= slop) return 0f
        val drop = drop(widget, pitch, rows)
        return roomFor(px, offset.y + scrolled, drop.start, drop.endInclusive)
    }

    private fun drop(widget: HostedWidget, pitch: Offset, rows: Int) = -widget.row * pitch.y..(rows - widget.bottom) * pitch.y

    /**
     * [page] as it would be with the held widget let go now, in the cells nearest to where it has been dragged, within
     * the rows it may be dragged down to; as it is, if the widget does not fit there.
     */
    fun preview(page: WidgetPage, pitch: Offset, rows: PageRows): WidgetPage {
        val widget = page.widgets.find { it.id == id } ?: return page
        val shift = shift(widget, pitch, rows.reach(page))
        return rows.move(page, widget.id, nearestCells(widget.row, shift.y, pitch.y), nearestCells(widget.column, shift.x, pitch.x)) ?: page
    }
}

/** A drag the page scrolls under: how much of a scroll of so many pixels it can still follow, and following one. */
private class Follower(val room: (px: Float) -> Float, val follow: (px: Float) -> Unit)

/** The resize handle's drag, which follows the page's scroll too, and the row its outline reaches down to meanwhile. */
private class EdgeScroll {
    var stretch by mutableStateOf<Follower?>(null)
    var outlineBottom by mutableIntStateOf(0)
}

/**
 * The widgets on [page], on a grid of [WIDGET_COLUMNS] columns and rows of at least [WIDGET_ROW_HEIGHT_DP], each in its
 * own cells, and below them a button to add another, at the bottom of the screen or, once the widgets reach further,
 * of the page, which then scrolls, so no widget is ever under it; a long press on the page where no widget is asks for
 * one too. An empty page says so in the middle.
 * With a [ring], the page is a ring page's: it shows the ring across the page, as tall as it is wide, in the middle until
 * moved, and around it the widgets in the rows the screen holds, which never scroll; it has no button, and leaves the
 * touches on its empty cells to what is behind it, so [addRequests] asks for a widget instead. Edited as [RING_ID], the
 * ring wears an outline and a finger drags it up or down the page.
 * Cells no widget takes stay empty. A long press on a widget puts it in edit mode, as in Arc: [editing] is its id, and
 * it wears an outline, a bin on the top-right corner that removes it, and, if its provider lets it stretch, a handle on
 * the bottom-right corner that drags its size a whole cell at a time, within what the provider allows, the rows the
 * page shows and the columns right of it. While one is edited the widgets take no taps, and a tap anywhere but on that
 * widget ends the mode through [onEditingChange]. A finger that stays down after the long press, or a later one held
 * on the edited widget, drags it anywhere on the page, empty cells too; the cells it would land in show under it, the
 * widgets it would cover make way below, and it lands there when let go. A finger dragging a widget or its handle near
 * the top or bottom of the screen scrolls the page, so a widget taken below the last row lengthens it.
 */
@Composable
fun WidgetGrid(
    page: WidgetPage,
    actions: WidgetActions,
    editing: Int?,
    onEditingChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    ring: (@Composable (Modifier) -> Unit)? = null,
    addRequests: Flow<Unit> = emptyFlow(),
) {
    val fixed = ring != null
    val density = LocalDensity.current
    val keyboard = WindowInsets.ime
    val bars = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    var area by remember { mutableStateOf(DpSize.Zero) }
    var buttonHeight by remember { mutableStateOf(0.dp) }
    // The rows the page shows fill the screen but for the button below them and a gap, so a page no taller than the
    // screen does not scroll.
    val rows = remember(area.height, buttonHeight, fixed) {
        val button = if (fixed) 0.dp else buttonHeight + GAP
        widgetRowsWithin((area.height - PAGE_PADDING * 2 - button).value)
    }
    val pageRows = rows.count
    val fit = remember(pageRows, fixed) { if (fixed) Fixed(pageRows) else Lengthening(pageRows) }
    val cell = DpSize(maxOf((area.width - PAGE_PADDING * 2 - GAP * (WIDGET_COLUMNS - 1)) / WIDGET_COLUMNS, 0.dp), rows.heightDp.dp)
    // As tall as the page is wide, so the ring is the size it has on a page of its own.
    val ringRows = widgetCells(span(WIDGET_COLUMNS, cell.width).value.roundToInt(), cell.height.value, pageRows)
    // Placed only once the page is measured, and stored as placed, so the widgets the host moves are where they show.
    val measured = area != DpSize.Zero
    val laid = remember(page, fixed, ringRows, pageRows, measured) { if (fixed && measured) page.withRing(ringRows, pageRows) else page }
    val reach = fit.reach(laid)
    LaunchedEffect(laid) { if (laid != page) actions.arrange(laid) }
    val held = remember { HeldWidget() }
    val edge = remember { EdgeScroll() }
    val scroll = rememberScrollState()
    // How far down the page a finger is while one is down; not a number otherwise.
    var finger by remember { mutableFloatStateOf(Float.NaN) }
    val pitch = with(density) { Offset((cell.width + GAP).toPx(), (cell.height + GAP).toPx()) }
    // Only a drag that reaches other cells changes the page shown.
    val shown by remember(laid, pitch, fit) { derivedStateOf(structuralEqualityPolicy()) { held.preview(laid, pitch, fit) } }
    val latestPage by rememberUpdatedState(laid)
    val latestPitch by rememberUpdatedState(pitch)
    val latestFit by rememberUpdatedState(fit)
    val latestActions by rememberUpdatedState(actions)
    // One for the page's life, so the gesture that drags the edited widget is not started over mid-drag.
    // Only the widget held is let go, so a finger on another one lifting moves nothing.
    val release = remember(held) {
        release@{ id: Int, lifted: Boolean ->
            if (held.id != id) return@release
            val landing = held.preview(latestPage, latestPitch, latestFit)
            held.id = null
            if (lifted && landing != latestPage) latestActions.arrange(landing)
        }
    }
    // Whether the page took the widget [id] stretched to [newRows] by [newColumns]. The latest page, since the handle's
    // gesture keeps the lambda it started with, and a move down the page does not restart it.
    val resize = remember(held) {
        { id: Int, newRows: Int, newColumns: Int -> latestFit.resize(latestPage, id, newRows, newColumns)?.also(latestActions.arrange) != null }
    }
    // Whatever ends edit mode, Back and HOME included, puts a held widget back.
    LaunchedEffect(editing == null) { if (editing == null) held.id?.let { release(it, false) } }
    val add = {
        onEditingChange(null)
        actions.add(pageRows, cell.width.value, cell.height.value)
    }
    val latestAdd by rememberUpdatedState(add)
    LaunchedEffect(addRequests) { addRequests.collect { latestAdd() } }
    val latestEditing by rememberUpdatedState(editing)
    val latestOnEditingChange by rememberUpdatedState(onEditingChange)
    val haptics = LocalHapticFeedback.current
    val slop = LocalViewConfiguration.current.touchSlop
    // A widget pressed near an edge and not yet dragged is only being long pressed, so it follows no scroll.
    val heldFollower = remember(held) {
        Follower(
            room = room@{ px ->
                val widget = latestPage.widgets.find { it.id == held.id } ?: return@room 0f
                held.room(px, widget, latestPitch, latestFit.reach(latestPage), slop)
            },
            follow = { held.scrolled += it },
        )
    }
    val dragging = held.id != null || edge.stretch != null
    LaunchedEffect(dragging) {
        if (!dragging || fixed) return@LaunchedEffect
        val follower = edge.stretch ?: heldFollower
        val zone = with(density) { EDGE_ZONE.toPx() }
        val speed = with(density) { EDGE_SPEED.toPx() }
        scroll.scrollAtEdges(speed, pull = { edgePull(finger, scroll.viewportSize.toFloat(), zone) }, room = follower.room, follow = follower.follow)
    }

    Box(
        modifier
            .fillMaxSize()
            .then(
                if (fixed) {
                    Modifier
                } else {
                    Modifier
                        // Watched ahead of the widgets and their views, which take the touches they drag with.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) finger = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed }?.position?.y ?: Float.NaN
                            }
                        }
                        // Keyed on nothing that a long press changes, so ending edit mode does not restart the gesture while
                        // the finger is still down, leaving the rest of the press to scroll or turn the page.
                        .pointerInput(haptics) {
                            detectTapGestures(
                                onTap = { if (latestEditing != null) latestOnEditingChange(null) },
                                onLongPress = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    latestAdd()
                                },
                            )
                        }
                },
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    with(density) {
                        // The keyboard, raised for the drawer's search, squeezes this page too. Measured as if it were
                        // down, the rows keep their height, so no widget is resized and redrawn by its provider meanwhile.
                        val squeeze = (keyboard.getBottom(this) - bars.getBottom(this)).coerceAtLeast(0)
                        area = DpSize(size.width.toDp(), (size.height + squeeze).toDp())
                    }
                }
                .then(if (fixed) Modifier else Modifier.verticalPageScroll(scroll))
                .padding(PAGE_PADDING),
            verticalArrangement = Arrangement.spacedBy(GAP),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val places = shown.widgets.associateBy { it.id }
            Box(
                Modifier
                    .fillMaxWidth()
                    // At least the page, so a widget can be dropped anywhere in sight.
                    .height(span(if (fixed) pageRows else maxOf(shown.rows, pageRows, edge.outlineBottom), cell.height))
                    // A widget dragged past the last row passes over the button on its way down.
                    .zIndex(if (held.id != null) 1f else 0f),
            ) {
                if (!fixed && laid.isEmpty) {
                    Text(
                        "No widgets yet",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center).padding(8.dp),
                    )
                }
                places[held.id]?.let { landing ->
                    Box(
                        Modifier
                            .offset { cellOffset(landing, cell) }
                            .size(span(landing.columns, cell.width), span(landing.rows, cell.height))
                            .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                            .testTag(WidgetTags.LANDING),
                    )
                }
                laid.widgets.forEach { widget ->
                    key(widget.id) {
                        val at = if (held.id == widget.id) widget else places[widget.id] ?: widget
                        if (widget.id == RING_ID) {
                            ring?.let { RingBlock(widget, at, cell, pitch, reach, editing, onEditingChange, held, release, it) }
                        } else {
                            Widget(widget, at, cell, pitch, reach, actions, resize, pageRows, editing, onEditingChange, held, release, edge)
                        }
                    }
                }
            }
            if (!fixed) {
                // Measured around the button, whose touch target is taller than the pill it draws.
                Box(
                    Modifier
                        .onSizeChanged { buttonHeight = with(density) { it.height.toDp() } }
                        .testTag(WidgetTags.ADD),
                ) {
                    TonalButton(onClick = add) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Add widget")
                    }
                }
            }
        }
    }
}

/**
 * The [ring] as stored, shown in the cells of [at], which [held] drags up and down the page's first [reach] rows while
 * it is edited, a cell and its gap being [pitch]; [release] lets it go. While a widget is edited, a tap on it ends that.
 */
@Composable
private fun RingBlock(
    ring: HostedWidget,
    at: HostedWidget,
    cell: DpSize,
    pitch: Offset,
    reach: Int,
    editing: Int?,
    onEditingChange: (Int?) -> Unit,
    held: HeldWidget,
    release: (id: Int, lifted: Boolean) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Box(
        Modifier
            .inCells(ring, at, cell, pitch, reach, held, edited = editing == RING_ID)
            .size(span(ring.columns, cell.width), span(ring.rows, cell.height))
            .testTag(WidgetTags.RING),
    ) {
        content(Modifier.fillMaxSize())
        // Over the ring, so its apps take no touches meanwhile.
        when (editing) {
            null -> Unit
            RING_ID -> Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.onBackground)
                    .dragToMove(RING_ID, held, release)
                    .testTag(WidgetTags.EDIT),
            )
            else -> Box(Modifier.fillMaxSize().takeTaps(onTap = { onEditingChange(null) }))
        }
    }
}

/**
 * [widget] drawn in the cells of [at], or, while [held] drags it, where the finger has taken it within the page's first
 * [reach] rows, a cell and its gap being [pitch]; above the others while it is [edited] or dragged.
 */
@Composable
private fun Modifier.inCells(widget: HostedWidget, at: HostedWidget, cell: DpSize, pitch: Offset, reach: Int, held: HeldWidget, edited: Boolean): Modifier {
    val dragged = held.id == widget.id
    // The others slide aside while one is dragged. Once it lands they are drawn in their cells at once, since even a
    // snap would take a frame, in which the one let go would be back where it started.
    val place = with(LocalDensity.current) { cellOffset(at, cell) }
    val sliding by animateIntOffsetAsState(place, if (held.id == null) snap() else spring(), label = "widget_place")
    return offset { if (held.id == null) place else sliding }
        .zIndex(if (dragged) 2f else if (edited) 1f else 0f)
        .graphicsLayer {
            if (dragged) {
                val shift = held.shift(widget, pitch, reach)
                translationX = shift.x
                translationY = shift.y
            }
        }
}

/** Where the top-left cell of [widget] is, on a grid of [cell]s. */
private fun Density.cellOffset(widget: HostedWidget, cell: DpSize) =
    IntOffset(((cell.width + GAP) * widget.column).roundToPx(), ((cell.height + GAP) * widget.row).roundToPx())

/**
 * The [widget] as stored, shown in the cells of [at], which [held] drags about the page's first [reach] rows,
 * a cell and its gap being [pitch]; [release] lets it go, moving it to the cells it is over if the finger lifted. While
 * it is edited, [edge] scrolls the page under its handle.
 */
@Composable
private fun Widget(
    widget: HostedWidget,
    at: HostedWidget,
    cell: DpSize,
    pitch: Offset,
    reach: Int,
    actions: WidgetActions,
    resize: (id: Int, rows: Int, columns: Int) -> Boolean,
    pageRows: Int,
    editing: Int?,
    onEditingChange: (Int?) -> Unit,
    held: HeldWidget,
    release: (id: Int, lifted: Boolean) -> Unit,
    edge: EdgeScroll,
) {
    val haptics = LocalHapticFeedback.current
    val edited = editing == widget.id
    // A drag that took the page past the screen's edge leaves the widget partly off it; once it lands it shows in full.
    val landed = remember { BringIntoViewRequester() }
    LaunchedEffect(widget.row, widget.rows, edited) { if (edited && held.id == null) landed.bringIntoView() }
    // The cells the outline shows while the handle is dragged. The widget takes them only once they are stored, so its
    // provider redraws once per resize rather than at every cell; until then they hold, so the outline does not jump back.
    var rows by remember(widget.rows, edited) { mutableIntStateOf(widget.rows) }
    var columns by remember(widget.columns, edited) { mutableIntStateOf(widget.columns) }
    val onLongPress = {
        if (held.pickUp(widget.id)) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onEditingChange(widget.id)
        }
    }

    Box(
        Modifier
            .inCells(widget, at, cell, pitch, reach, held, edited)
            .size(span(maxOf(columns, widget.columns), cell.width), span(maxOf(rows, widget.rows), cell.height))
            .semantics { contentDescription = "Widget" }
            .testTag(WidgetTags.widget(widget.id))
            .bringIntoViewRequester(landed),
    ) {
        // Made afresh from the id each time the widget is composed; the system keeps what it shows.
        AndroidView(
            factory = { context -> LongPressFrame(context).apply { addView(actions.view(context, widget.id)) } },
            update = {
                it.onLongPress = onLongPress
                it.onDrag = { moved -> if (held.id == widget.id) held.offset = moved }
                it.onRelease = { lifted -> release(widget.id, lifted) }
            },
            modifier = Modifier
                .size(span(widget.columns, cell.width), span(widget.rows, cell.height))
                .then(
                    when {
                        edited -> Modifier.holdToMove(widget.id, held, haptics, release)
                        editing != null -> Modifier.takeTaps(onTap = { onEditingChange(null) })
                        else -> Modifier
                    },
                ),
        )
        if (edited) {
            val sizing = remember(widget.id) { actions.sizing(widget.id) }
            Box(
                Modifier
                    .size(span(columns, cell.width), span(rows, cell.height))
                    .border(2.dp, MaterialTheme.colorScheme.onBackground)
                    .testTag(WidgetTags.EDIT),
            ) {
                EditButton(
                    Icons.Default.Delete,
                    "Remove widget",
                    Modifier.align(Alignment.TopEnd).offset(CONTROL_REACH, -CONTROL_REACH).testTag(WidgetTags.REMOVE),
                    Modifier.clickable {
                        onEditingChange(null)
                        actions.remove(widget.id)
                    },
                )
                val down = Stretch(widget.rows, sizing.vertical.cellRange(widget.rows, cell.height.value, pageRows), (cell.height + GAP).value)
                val across = Stretch(
                    widget.columns,
                    sizing.horizontal.cellRange(widget.columns, cell.width.value, WIDGET_COLUMNS - widget.column),
                    (cell.width + GAP).value,
                )
                // A handle that could reach no other size would be a dead control.
                if (down.stretches || across.stretches) {
                    EditButton(
                        if (!across.stretches) HeightGlyph else if (!down.stretches) WidthGlyph else ResizeGlyph,
                        "Resize widget",
                        Modifier.align(Alignment.BottomEnd).offset(CONTROL_REACH, CONTROL_REACH).testTag(WidgetTags.RESIZE),
                        // At the screen's edge, where a drag across would otherwise be the system's back gesture.
                        Modifier.systemGestureExclusion().resizeHandle(
                            down,
                            across,
                            edge,
                            onDrag = { newRows, newColumns ->
                                rows = newRows
                                columns = newColumns
                                edge.outlineBottom = widget.row + newRows
                            },
                            onRelease = { newRows, newColumns ->
                                val changed = newRows != widget.rows || newColumns != widget.columns
                                // A size the page cannot take goes back to the stored one.
                                if (changed && !resize(widget.id, newRows, newColumns)) {
                                    rows = widget.rows
                                    columns = widget.columns
                                }
                            },
                        ),
                    )
                }
            }
        }
    }
}

/** A round control on the edit outline, which answers touches through [input], inside its circle. */
@Composable
private fun EditButton(icon: ImageVector, description: String, modifier: Modifier, input: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(CONTROL_SIZE)
            .clip(CircleShape)
            .then(input)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One way a handle stretches a widget: the [cells] it spans, the [range] it may span, and a cell and its gap, in dp. */
private data class Stretch(val cells: Int, val range: IntRange, val pitchDp: Float) {
    val stretches: Boolean get() = range.first < range.last

    /** The pull [pulledDp] taken [movedDp] further, held within [range]. */
    fun pull(pulledDp: Float, movedDp: Float) = heldDrag(cells, pulledDp + movedDp, range, pitchDp)

    fun cellsAt(pulledDp: Float) = nearestCells(cells, pulledDp, pitchDp)

    /** How much of a move of [byDp] the pull [pulledDp] can still take within [range]. */
    fun room(pulledDp: Float, byDp: Float) = pull(pulledDp, byDp) - pulledDp
}

/**
 * Drags a widget to other whole cells, [down] and [across], following the finger from where it went down, touch slop
 * and all, and the page as [edge] scrolls it under the finger, and hands its rows and columns to [onDrag] as they change
 * and to [onRelease] as the finger lifts; a drag the page takes over puts them back. The press is taken at once, so a
 * tap on the handle is not a tap on the page.
 */
private fun Modifier.resizeHandle(
    down: Stretch,
    across: Stretch,
    edge: EdgeScroll,
    onDrag: (rows: Int, columns: Int) -> Unit,
    onRelease: (rows: Int, columns: Int) -> Unit,
) = pointerInput(down, across, edge) {
    awaitEachGesture {
        val press = awaitFirstDown()
        press.consume()
        var pulled = Offset.Zero
        var released = false
        fun newRows() = down.cellsAt(pulled.y)
        fun newColumns() = across.cellsAt(pulled.x)
        fun follow(change: PointerInputChange, moved: Offset) {
            change.consume()
            pulled = Offset(across.pull(pulled.x, moved.x.toDp().value), down.pull(pulled.y, moved.y.toDp().value))
            onDrag(newRows(), newColumns())
        }
        // Whatever ends the gesture short of a release, the handle being rebuilt included, leaves no unstored cells shown.
        try {
            // From the down until the drag starts; after that the handle moves with the cells, so step by step. Every
            // move is taken, or the pager would take the touch back the first time one went across.
            val start = awaitTouchSlopOrCancellation(press.id) { change, _ -> follow(change, change.position - press.position) }
            if (start != null) {
                edge.stretch = Follower(
                    room = { px -> down.room(pulled.y, px.toDp().value).dp.toPx() },
                    follow = { px ->
                        pulled = pulled.copy(y = down.pull(pulled.y, px.toDp().value))
                        onDrag(newRows(), newColumns())
                    },
                )
                if (drag(start.id) { follow(it, it.positionChange()) }) {
                    onRelease(newRows(), newColumns())
                    released = true
                }
            }
        } finally {
            edge.stretch = null
            if (!released) onDrag(down.cells, across.cells)
            edge.outlineBottom = 0
        }
    }
}

// Material's "height" and "open with" symbols, and "height" turned on its side: the arrows point the ways the widget
// stretches.
private val HeightGlyph = materialGlyph("Height", "M13 6.99h3L12 3 8 6.99h3v10.02H8L12 21l4-3.99h-3z")
private val WidthGlyph = materialGlyph("Width", "M6.99 13v3L3 12l3.99-4v3h10.02V8L21 12l-3.99 4v-3z")
private val ResizeGlyph = materialGlyph(
    "OpenWith",
    "M10 9h4V6h3l-5-5-5 5h3v3zm-1 1H6V7l-5 5 5 5v-3h3v-4zm14 2l-5-5v3h-3v4h3v3l5-5zm-9 3h-4v3H7l5 5 5-5h-3v-3z",
)

/**
 * Takes every press on the edited widget [id] before its view sees it, as [takeTaps] does, and moves the widget
 * through [held] with one that is held, until [release]. A finger that moves on before then scrolls the page.
 */
private fun Modifier.holdToMove(id: Int, held: HeldWidget, haptics: HapticFeedback, release: (id: Int, lifted: Boolean) -> Unit) =
    pointerInput(id, held, haptics, release) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            down.consume()
            // The wait for the long press starts on the next event, since it would take this one's consumed down for a
            // gesture another handler had claimed.
            awaitPointerEvent(PointerEventPass.Main)
            val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            if (!held.pickUp(id)) return@awaitEachGesture
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            carry(id, press.id, held, release)
        }
    }

/** Moves [id] through [held] with the finger [pointer] until it lifts or is taken, then lets it go through [release]. */
private suspend fun AwaitPointerEventScope.carry(id: Int, pointer: PointerId, held: HeldWidget, release: (id: Int, lifted: Boolean) -> Unit) {
    var lifted = false
    try {
        // Every move is taken, or the pager would take the touch back.
        lifted = drag(pointer) {
            held.offset += it.positionChange()
            it.consume()
        }
    } finally {
        release(id, lifted)
    }
}

/**
 * Moves [id] through [held] with a finger that drags on it, following it from where it went down, until [release]. The
 * press is taken at once, so neither what is under it nor the page takes it.
 */
private fun Modifier.dragToMove(id: Int, held: HeldWidget, release: (id: Int, lifted: Boolean) -> Unit) =
    pointerInput(id, held, release) {
        awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
            if (!held.pickUp(id)) return@awaitEachGesture
            held.offset = start.position - down.position
            carry(id, start.id, held, release)
        }
    }

/**
 * Takes every press on the widget before its view sees it, so an edited page launches nothing, and calls [onTap] for
 * one that lifts where it went down. Only the press itself is taken, so the page still scrolls and swipes.
 */
private fun Modifier.takeTaps(onTap: () -> Unit) = pointerInput(onTap) {
    awaitEachGesture {
        awaitFirstDown(pass = PointerEventPass.Initial).consume()
        waitForUpOrCancellation(PointerEventPass.Initial)?.let {
            it.consume()
            onTap()
        }
    }
}

/**
 * Calls [onLongPress] when a finger rests on the widget. Compose cannot take a press back from a view whose buttons
 * already hold it, so this watches from the view side, as the platform launcher does: it sees each event ahead of its
 * children and, once the press fires, intercepts the rest, so the widget gets a cancel rather than a tap, and keeps the
 * page and the pager from following the finger. A finger that lifts or moves, a child that starts to scroll, or the
 * pager taking the drag ends the wait. Where no child takes the press, the frame takes it itself, since a view that
 * turns down the down hears nothing more of the gesture and could not cancel the wait. After the press the finger
 * goes to [onDrag], as how far it has gone in pixels, and its end to [onRelease], with whether it lifted.
 */
private class LongPressFrame(context: Context) : FrameLayout(context) {
    var onLongPress: (() -> Unit)? = null
    var onDrag: ((Offset) -> Unit)? = null
    var onRelease: ((lifted: Boolean) -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var pressed = false
    private var downX = 0f
    private var downY = 0f

    // On the screen, since the frame itself moves with the widget as it is dragged.
    private var downRawX = 0f
    private var downRawY = 0f
    private var pointerId = 0
    private val fire = Runnable {
        pressed = true
        parent?.requestDisallowInterceptTouchEvent(true)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        track(event)
        // The event taken here goes to the child as a cancel and never on to the frame, so the frame follows it here: a
        // finger that lifts without having moved since the press would otherwise never be let go.
        val taken = pressed
        if (taken) follow(event)
        return taken
    }

    // The frame never clicks; it holds the gesture to time the press and then to drag, and the widget inside keeps its
    // own clicks.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) track(event)
        if (pressed) follow(event)
        return onLongPress != null
    }

    // Only the finger that pressed drags; another that comes and goes changes nothing.
    private fun follow(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> event.findPointerIndex(pointerId).takeIf { it >= 0 }?.let { onDrag?.invoke(Offset(event.getRawX(it) - downRawX, event.getRawY(it) - downRawY)) }
            MotionEvent.ACTION_POINTER_UP -> if (event.getPointerId(event.actionIndex) == pointerId) release(lifted = true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> release(lifted = event.actionMasked == MotionEvent.ACTION_UP)
        }
    }

    private fun release(lifted: Boolean) {
        pressed = false
        onRelease?.invoke(lifted)
    }

    private fun track(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                removeCallbacks(fire)
                pressed = false
                downX = event.x
                downY = event.y
                downRawX = event.rawX
                downRawY = event.rawY
                pointerId = event.getPointerId(0)
                if (onLongPress != null) postDelayed(fire, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) removeCallbacks(fire)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> removeCallbacks(fire)
        }
    }

    override fun requestDisallowInterceptTouchEvent(disallow: Boolean) {
        if (disallow) removeCallbacks(fire)
        super.requestDisallowInterceptTouchEvent(disallow)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(fire)
        if (pressed) release(lifted = false)
        super.onDetachedFromWindow()
    }
}
