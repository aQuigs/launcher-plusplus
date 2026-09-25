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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.WIDGET_COLUMNS
import com.sqftware.orbitlauncher.domain.WIDGET_GAP_DP
import com.sqftware.orbitlauncher.domain.WIDGET_ROW_HEIGHT_DP
import com.sqftware.orbitlauncher.domain.WIDGET_ROW_PITCH_DP
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetSizing
import com.sqftware.orbitlauncher.domain.cellRange
import com.sqftware.orbitlauncher.domain.heldDrag
import com.sqftware.orbitlauncher.domain.resizedCells
import kotlin.math.abs
import kotlin.math.roundToInt

object WidgetTags {
    const val ADD = "widget_add"
    const val EDIT = "widget_edit"
    const val REMOVE = "widget_remove"
    const val RESIZE = "widget_resize"
    const val LANDING = "widget_landing"

    fun widget(id: Int) = "widget_$id"
}

/**
 * What the widget page asks of the system: a widget's view, a new one for a page of so many rows and columns so wide, a
 * removal, a resize, a move to other cells, and how a widget's provider lets it be resized.
 */
class WidgetActions(
    val view: (Context, Int) -> View,
    val add: (pageRows: Int, columnWidthDp: Int) -> Unit,
    val remove: (Int) -> Unit,
    val resize: (id: Int, rows: Int, columns: Int) -> Unit,
    val move: (id: Int, row: Int, column: Int) -> Unit,
    val sizing: (Int) -> WidgetSizing,
)

private val ROW_HEIGHT = WIDGET_ROW_HEIGHT_DP.dp
private val GAP = WIDGET_GAP_DP.dp
private val PAGE_PADDING = 16.dp
private val CONTROL_SIZE = 40.dp

// How far the edit controls reach past the outline's corners. Any reach keeps the bin and the handle apart on a widget
// one row tall, since each is half a row; half the page's padding keeps them clear of the screen's edge.
private val CONTROL_REACH = PAGE_PADDING / 2

/** The length of [cells] cells [cell] long and the gaps between them. */
private fun span(cells: Int, cell: Dp) = cell * cells + GAP * (cells - 1)

/**
 * The widget held to be moved and how far it has been dragged, in pixels; once it is let go, the page it landed on,
 * shown until the stored page comes back, so it does not flash back to where it was.
 */
private class HeldWidget {
    var id by mutableStateOf<Int?>(null)
    var offset by mutableStateOf(Offset.Zero)
    var landed by mutableStateOf<WidgetPage?>(null)

    // One cell and its gap, across and down, in pixels.
    var pitch = Offset(1f, 1f)

    fun pickUp(id: Int) {
        if (this.id != null) return
        this.id = id
        offset = Offset.Zero
    }

    /** [page] as it would be with the held widget let go now, in the cells nearest to where it has been dragged. */
    fun preview(page: WidgetPage): WidgetPage {
        val widget = page.widgets.find { it.id == id } ?: return landed ?: page
        return page.move(widget.id, widget.row + (offset.y / pitch.y).roundToInt(), widget.column + (offset.x / pitch.x).roundToInt())
    }
}

/**
 * The widgets on [page], on a grid of [WIDGET_COLUMNS] columns and rows of [WIDGET_ROW_HEIGHT_DP], each in its own
 * cells, scrolling above a button pinned to the bottom of the page to add another; an empty page says so in the middle.
 * Cells no widget takes stay empty. A long press on a widget puts it in edit mode, as in Arc: [editing] is its id, and
 * it wears an outline, a bin on the top-right corner that removes it, and, if its provider lets it stretch, a handle on
 * the bottom-right corner that drags its size a whole cell at a time, within what the provider allows, the rows the
 * page shows and the columns right of it. While one is edited the widgets take no taps, and a tap anywhere but on that
 * widget ends the mode through [onEditingChange]. A finger that stays down after the long press, or a later one held
 * on the edited widget, drags it anywhere on the page, empty cells too; the cells it would land in show under it, the
 * widgets it would cover make way below, and it lands there when let go.
 */
@Composable
fun WidgetGrid(
    page: WidgetPage,
    actions: WidgetActions,
    editing: Int?,
    onEditingChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // The rows the page shows are those of the scrolling part, above the button.
    var pageRows by remember { mutableIntStateOf(1) }
    var columnWidth by remember { mutableStateOf(0.dp) }
    val held = remember { HeldWidget() }
    SideEffect { held.pitch = with(density) { Offset((columnWidth + GAP).toPx(), (ROW_HEIGHT + GAP).toPx()) } }
    val latestPage by rememberUpdatedState(page)
    val latestMove by rememberUpdatedState(actions.move)
    // One for the page's life, so the gesture that drags the edited widget is not started over mid-drag.
    val release = remember(held) {
        { lifted: Boolean ->
            val id = held.id
            val landing = held.preview(latestPage)
            held.id = null
            if (lifted && id != null && landing != latestPage) {
                held.landed = landing
                landing.widgets.find { it.id == id }?.let { latestMove(id, it.row, it.column) }
            }
        }
    }
    LaunchedEffect(page) { held.landed = null }
    // Whatever ends edit mode, Back and HOME included, puts a held widget back.
    LaunchedEffect(editing == null) { if (editing == null) release(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .then(if (editing != null) Modifier.pointerInput(onEditingChange) { detectTapGestures { onEditingChange(null) } } else Modifier),
    ) {
        val area = Modifier
            .weight(1f)
            .fillMaxWidth()
            .onSizeChanged { size ->
                with(density) {
                    pageRows = ((size.height.toDp() - PAGE_PADDING * 2 + GAP) / (ROW_HEIGHT + GAP)).toInt().coerceAtLeast(1)
                    columnWidth = (size.width.toDp() - PAGE_PADDING * 2 - GAP * (WIDGET_COLUMNS - 1)) / WIDGET_COLUMNS
                }
            }
        if (page.isEmpty) {
            Box(area, contentAlignment = Alignment.Center) {
                Text("No widgets yet", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))
            }
        } else {
            val shown = held.preview(page)
            Box(
                area
                    .verticalPageScroll()
                    .padding(PAGE_PADDING)
                    .fillMaxWidth()
                    // At least the page, so a widget can be dropped anywhere in sight.
                    .height(span(maxOf(shown.rows, pageRows), ROW_HEIGHT)),
            ) {
                shown.widgets.find { it.id == held.id }?.let { landing ->
                    Box(
                        Modifier
                            .offset { cellOffset(landing, columnWidth) }
                            .size(span(landing.columns, columnWidth), span(landing.rows, ROW_HEIGHT))
                            .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                            .testTag(WidgetTags.LANDING),
                    )
                }
                page.widgets.forEach { widget ->
                    key(widget.id) {
                        val at = if (held.id == widget.id) widget else shown.widgets.find { it.id == widget.id } ?: widget
                        Widget(widget, at, columnWidth, actions, pageRows, editing, onEditingChange, held, release)
                    }
                }
            }
        }
        FilledTonalButton(
            onClick = {
                onEditingChange(null)
                actions.add(pageRows, columnWidth.value.toInt())
            },
            modifier = Modifier.padding(bottom = PAGE_PADDING).testTag(WidgetTags.ADD),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Add widget")
        }
    }
}

/** Where the top-left cell of [widget] is, on a grid of columns [columnWidth] wide. */
private fun Density.cellOffset(widget: HostedWidget, columnWidth: Dp) =
    IntOffset(((columnWidth + GAP) * widget.column).roundToPx(), ((ROW_HEIGHT + GAP) * widget.row).roundToPx())

/**
 * The [widget] as stored, shown in the cells of [at], which [held] drags about the page; [release] lets it go, moving it
 * to the cells it is over if the finger lifted.
 */
@Composable
private fun Widget(
    widget: HostedWidget,
    at: HostedWidget,
    columnWidth: Dp,
    actions: WidgetActions,
    pageRows: Int,
    editing: Int?,
    onEditingChange: (Int?) -> Unit,
    held: HeldWidget,
    release: (lifted: Boolean) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val edited = editing == widget.id
    val dragged = held.id == widget.id
    // The others slide aside while one is dragged, and snap the moment it lands.
    val place by animateIntOffsetAsState(
        targetValue = with(density) { cellOffset(at, columnWidth) },
        animationSpec = if (held.id == null) snap() else spring(),
        label = "widget_place",
    )
    // The cells the outline shows while the handle is dragged. The widget takes them only once they are stored, so its
    // provider redraws once per resize rather than at every cell; until then they hold, so the outline does not jump back.
    var rows by remember(widget.rows, edited) { mutableIntStateOf(widget.rows) }
    var columns by remember(widget.columns, edited) { mutableIntStateOf(widget.columns) }
    val onLongPress = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onEditingChange(widget.id)
        held.pickUp(widget.id)
    }

    Box(
        Modifier
            .offset { place }
            .zIndex(if (dragged) 2f else if (edited) 1f else 0f)
            .graphicsLayer {
                if (dragged) {
                    translationX = held.offset.x
                    translationY = held.offset.y
                }
            }
            .size(span(maxOf(columns, widget.columns), columnWidth), span(maxOf(rows, widget.rows), ROW_HEIGHT))
            .semantics { contentDescription = "Widget" }
            .testTag(WidgetTags.widget(widget.id)),
    ) {
        // Made afresh from the id each time the widget is composed; the system keeps what it shows.
        AndroidView(
            factory = { context -> LongPressFrame(context).apply { addView(actions.view(context, widget.id)) } },
            update = {
                it.onLongPress = onLongPress
                it.onDrag = { moved -> if (held.id == widget.id) held.offset = moved }
                it.onRelease = release
            },
            modifier = Modifier
                .size(span(widget.columns, columnWidth), span(widget.rows, ROW_HEIGHT))
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
                    .size(span(columns, columnWidth), span(rows, ROW_HEIGHT))
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
                val rowRange = sizing.vertical.cellRange(widget.rows, WIDGET_ROW_HEIGHT_DP.toFloat(), pageRows)
                val columnRange = sizing.horizontal.cellRange(widget.columns, columnWidth.value, WIDGET_COLUMNS - widget.column)
                val down = rowRange.first < rowRange.last
                val across = columnRange.first < columnRange.last
                // A handle that could reach no other size would be a dead control.
                if (down || across) {
                    EditButton(
                        if (!across) HeightGlyph else if (!down) WidthGlyph else ResizeGlyph,
                        "Resize widget",
                        Modifier.align(Alignment.BottomEnd).offset(CONTROL_REACH, CONTROL_REACH).testTag(WidgetTags.RESIZE),
                        // At the screen's edge, where a drag across would otherwise be the system's back gesture.
                        Modifier.systemGestureExclusion().resizeHandle(
                            rows = widget.rows,
                            columns = widget.columns,
                            rowRange = rowRange,
                            columnRange = columnRange,
                            columnPitchDp = (columnWidth + GAP).value,
                            onDrag = { newRows, newColumns ->
                                rows = newRows
                                columns = newColumns
                            },
                            onRelease = { newRows, newColumns ->
                                if (newRows != widget.rows || newColumns != widget.columns) actions.resize(widget.id, newRows, newColumns)
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

/**
 * Drags a widget [rows] by [columns] to other whole cells within [rowRange] and [columnRange], a column and its gap
 * being [columnPitchDp], following the finger from where it went down, touch slop and all, and hands them to [onDrag]
 * as they change and to [onRelease] as the finger lifts; a drag the page takes over puts them back. The press is taken
 * at once, so a tap on the handle is not a tap on the page.
 */
private fun Modifier.resizeHandle(
    rows: Int,
    columns: Int,
    rowRange: IntRange,
    columnRange: IntRange,
    columnPitchDp: Float,
    onDrag: (rows: Int, columns: Int) -> Unit,
    onRelease: (rows: Int, columns: Int) -> Unit,
) = pointerInput(rows, columns, rowRange, columnRange, columnPitchDp) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        var pulledX = 0f
        var pulledY = 0f
        var released = false
        fun newRows() = resizedCells(rows, pulledY, WIDGET_ROW_PITCH_DP.toFloat())
        fun newColumns() = resizedCells(columns, pulledX, columnPitchDp)
        fun follow(change: PointerInputChange, moved: Offset) {
            change.consume()
            pulledX = heldDrag(columns, pulledX + moved.x.toDp().value, columnRange, columnPitchDp)
            pulledY = heldDrag(rows, pulledY + moved.y.toDp().value, rowRange, WIDGET_ROW_PITCH_DP.toFloat())
            onDrag(newRows(), newColumns())
        }
        // Whatever ends the gesture short of a release, the handle being rebuilt included, leaves no unstored cells shown.
        try {
            // From the down until the drag starts; after that the handle moves with the cells, so step by step. Every
            // move is taken, or the pager would take the touch back the first time one went across.
            val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> follow(change, change.position - down.position) }
            if (start != null && drag(start.id) { follow(it, it.positionChange()) }) {
                onRelease(newRows(), newColumns())
                released = true
            }
        } finally {
            if (!released) onDrag(rows, columns)
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
private fun Modifier.holdToMove(id: Int, held: HeldWidget, haptics: HapticFeedback, release: (lifted: Boolean) -> Unit) =
    pointerInput(id, held, haptics, release) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            down.consume()
            // The wait for the long press starts on the next event, since it would take this one's consumed down for a
            // gesture another handler had claimed.
            awaitPointerEvent(PointerEventPass.Main)
            val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            held.pickUp(id)
            var lifted = false
            try {
                // Every move is taken, or the pager would take the touch back.
                lifted = drag(press.id) {
                    held.offset += it.positionChange()
                    it.consume()
                }
            } finally {
                release(lifted)
            }
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
