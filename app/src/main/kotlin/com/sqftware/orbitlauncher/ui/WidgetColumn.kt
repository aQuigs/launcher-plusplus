package com.sqftware.orbitlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.WIDGET_ROW_HEIGHT_DP
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetSizing
import com.sqftware.orbitlauncher.domain.heldDrag
import com.sqftware.orbitlauncher.domain.resizedRows
import com.sqftware.orbitlauncher.domain.rowRange
import kotlin.math.abs

object WidgetTags {
    const val ADD = "widget_add"
    const val EDIT = "widget_edit"
    const val REMOVE = "widget_remove"
    const val RESIZE = "widget_resize"

    fun widget(id: Int) = "widget_$id"
}

/**
 * What the widget page asks of the system: a widget's view, a new one for a page of so many rows, a removal, a resize,
 * and how a widget's provider lets it be resized.
 */
class WidgetActions(
    val view: (Context, Int) -> View,
    val add: (pageRows: Int) -> Unit,
    val remove: (Int) -> Unit,
    val resize: (id: Int, rows: Int) -> Unit,
    val sizing: (Int) -> WidgetSizing,
)

private val ROW_HEIGHT = WIDGET_ROW_HEIGHT_DP.dp
private val PAGE_PADDING = 16.dp
private val CONTROL_SIZE = 40.dp

// How far the edit controls reach past the outline's corners. Any reach keeps the bin and the handle apart on a widget
// one row tall, since each is half a row; half the page's padding keeps them clear of the screen's edge.
private val CONTROL_REACH = PAGE_PADDING / 2

/**
 * The widgets on [page], top to bottom, each the page's width and its own rows tall, with a button under them to add
 * another; an empty page says so over the button. A long press on a widget puts it in edit mode, as in Arc: [editing]
 * is its id, and it wears an outline, a bin on the top-right corner that removes it, and, if its provider lets it
 * stretch up and down, a handle on the bottom-right corner that drags its height a whole row at a time, within the rows
 * the provider allows and the page shows. While one is edited the widgets take no taps, and a tap anywhere but on that
 * widget ends the mode through [onEditingChange].
 */
@Composable
fun WidgetColumn(
    page: WidgetPage,
    actions: WidgetActions,
    editing: Int?,
    onEditingChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val pageRows = ((maxHeight - PAGE_PADDING * 2) / ROW_HEIGHT).toInt().coerceAtLeast(1)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (page.isEmpty) Arrangement.Center else Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .then(if (editing != null) Modifier.pointerInput(onEditingChange) { detectTapGestures { onEditingChange(null) } } else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(PAGE_PADDING),
        ) {
            if (page.isEmpty) Text("No widgets yet", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))
            page.widgets.forEach { widget ->
                key(widget.id) { Widget(widget, actions, pageRows, editing, onEditingChange) }
            }
            FilledTonalButton(
                onClick = {
                    onEditingChange(null)
                    actions.add(pageRows)
                },
                modifier = Modifier.padding(top = 8.dp).testTag(WidgetTags.ADD),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Add widget")
            }
        }
    }
}

@Composable
private fun Widget(widget: HostedWidget, actions: WidgetActions, pageRows: Int, editing: Int?, onEditingChange: (Int?) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val edited = editing == widget.id
    // The rows the outline shows while the handle is dragged. The widget takes them only once they are stored, so its
    // provider redraws once per resize rather than at every row; until then they hold, so the outline does not jump back.
    var rows by remember(widget.rows, edited) { mutableIntStateOf(widget.rows) }
    val onLongPress = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onEditingChange(widget.id)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT * maxOf(rows, widget.rows))
            .semantics { contentDescription = "Widget" }
            .testTag(WidgetTags.widget(widget.id)),
    ) {
        // Made afresh from the id each time the widget is composed; the system keeps what it shows.
        AndroidView(
            factory = { context -> LongPressFrame(context).apply { addView(actions.view(context, widget.id)) } },
            update = { it.onLongPress = onLongPress },
            modifier = Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT * widget.rows)
                .then(if (editing != null) Modifier.takeTaps(onTap = { if (!edited) onEditingChange(null) }) else Modifier),
        )
        if (edited) {
            val sizing = remember(widget.id) { actions.sizing(widget.id) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT * rows)
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
                val range = sizing.rowRange(widget.rows, pageRows)
                // A handle that could reach no other height would be a dead control.
                if (sizing.vertical && range.first < range.last) {
                    EditButton(
                        ResizeIcon,
                        "Resize widget",
                        Modifier.align(Alignment.BottomEnd).offset(CONTROL_REACH, CONTROL_REACH).testTag(WidgetTags.RESIZE),
                        Modifier.resizeHandle(
                            rows = widget.rows,
                            range = range,
                            onDrag = { rows = it },
                            onRelease = { if (it != widget.rows) actions.resize(widget.id, it) },
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
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Drags a widget [rows] tall to other whole rows within [range], following the finger from where it went down, touch
 * slop and all, and hands them to [onDrag] as they change and to [onRelease] as the finger lifts; a drag the page takes
 * over puts them back. The press is taken at once, so a tap on the handle is not a tap on the page.
 */
private fun Modifier.resizeHandle(rows: Int, range: IntRange, onDrag: (Int) -> Unit, onRelease: (Int) -> Unit) =
    pointerInput(rows, range) {
        awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            var pulled = 0f
            var released = false
            fun follow(change: PointerInputChange, dy: Float) {
                change.consume()
                pulled = heldDrag(rows, pulled + dy.toDp().value, range)
                onDrag(resizedRows(rows, pulled))
            }
            // Whatever ends the gesture short of a release, the handle being rebuilt included, leaves no unstored rows shown.
            try {
                // From the down until the drag starts; after that the handle moves with the rows, so step by step. Every
                // move is taken, sideways ones too, or the pager would take the touch back the first time one went across.
                val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ -> follow(change, change.position.y - down.position.y) }
                if (start != null && drag(start.id) { follow(it, it.positionChange().y) }) {
                    onRelease(resizedRows(rows, pulled))
                    released = true
                }
            } finally {
                if (!released) onDrag(rows)
            }
        }
    }

// Material's "height" symbol: the widget stretches up and down only.
private val ResizeIcon = materialGlyph("Height", "M13 6.99h3L12 3 8 6.99h3v10.02H8L12 21l4-3.99h-3z")

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
 * turns down the down hears nothing more of the gesture and could not cancel the wait.
 */
private class LongPressFrame(context: Context) : FrameLayout(context) {
    var onLongPress: (() -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var pressed = false
    private var downX = 0f
    private var downY = 0f
    private val fire = Runnable {
        pressed = true
        parent?.requestDisallowInterceptTouchEvent(true)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        track(event)
        return pressed
    }

    // The frame never clicks; it holds the gesture only to time the press, and the widget inside keeps its own clicks.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) track(event)
        return onLongPress != null
    }

    private fun track(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                removeCallbacks(fire)
                pressed = false
                downX = event.x
                downY = event.y
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
        super.onDetachedFromWindow()
    }
}
