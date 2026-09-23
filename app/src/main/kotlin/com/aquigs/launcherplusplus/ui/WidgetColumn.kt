package com.aquigs.launcherplusplus.ui

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aquigs.launcherplusplus.domain.HostedWidget
import com.aquigs.launcherplusplus.domain.WIDGET_ROW_HEIGHT_DP
import com.aquigs.launcherplusplus.domain.WidgetPage
import kotlin.math.abs

object WidgetTags {
    const val ADD = "widget_add"
    const val MENU = "widget_options"

    fun widget(id: Int) = "widget_$id"
}

/** What the widget page asks of the system: a widget's view, a new widget for a page of so many rows, and a removal. */
class WidgetActions(val view: (Context, Int) -> View, val add: (pageRows: Int) -> Unit, val remove: (Int) -> Unit)

/** A widget's long-press menu, opened and shown like an [AppMenu]. */
typealias WidgetMenu = LongPressMenu<HostedWidget>

private val ROW_HEIGHT = WIDGET_ROW_HEIGHT_DP.dp
private val PAGE_PADDING = 16.dp

/**
 * The widgets on [page], top to bottom, each the page's width and its own rows tall, with a button under them to add
 * another; an empty page says so over the button. A long press on a widget opens its [menu].
 */
@Composable
fun WidgetColumn(
    page: WidgetPage,
    view: (Context, Int) -> View,
    onAdd: (pageRows: Int) -> Unit,
    modifier: Modifier = Modifier,
    menu: WidgetMenu? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val pageRows = ((maxHeight - PAGE_PADDING * 2) / ROW_HEIGHT).toInt().coerceAtLeast(1)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (page.isEmpty) Arrangement.Center else Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PAGE_PADDING),
        ) {
            if (page.isEmpty) Text("No widgets yet", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(8.dp))
            page.widgets.forEach { widget ->
                key(widget.id) { Widget(widget, view, menu, Modifier.fillMaxWidth().height(ROW_HEIGHT * widget.rows)) }
            }
            FilledTonalButton(onClick = { onAdd(pageRows) }, modifier = Modifier.padding(top = 8.dp).testTag(WidgetTags.ADD)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Add widget")
            }
        }
    }
}

@Composable
private fun Widget(widget: HostedWidget, view: (Context, Int) -> View, menu: WidgetMenu?, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val onLongPress = menu?.let { m ->
        {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            m.onOpen(widget)
        }
    }

    Box(modifier.semantics { contentDescription = "Widget" }.testTag(WidgetTags.widget(widget.id))) {
        // Made afresh from the id each time the widget is composed; the system keeps what it shows.
        AndroidView(
            factory = { context -> LongPressFrame(context).apply { addView(view(context, widget.id)) } },
            update = { it.onLongPress = onLongPress },
            modifier = Modifier.fillMaxSize(),
        )
        menu?.content?.invoke(widget)
    }
}

/**
 * Calls [onLongPress] when a finger rests on the widget. Compose cannot take a press back from a view whose buttons
 * already hold it, so this watches from the view side, as the platform launcher does: it sees each event ahead of its
 * children and, once the press fires, intercepts the rest, so the widget gets a cancel rather than a tap. A finger that
 * moves, a child that starts to scroll, or the pager taking the drag ends the wait.
 */
private class LongPressFrame(context: Context) : FrameLayout(context) {
    var onLongPress: (() -> Unit)? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var pressed = false
    private var downX = 0f
    private var downY = 0f
    private val fire = Runnable {
        pressed = true
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = false
                downX = event.x
                downY = event.y
                if (onLongPress != null) postDelayed(fire, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) removeCallbacks(fire)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> removeCallbacks(fire)
        }
        return pressed
    }

    override fun onTouchEvent(event: MotionEvent) = pressed

    override fun requestDisallowInterceptTouchEvent(disallow: Boolean) {
        if (disallow) removeCallbacks(fire)
        super.requestDisallowInterceptTouchEvent(disallow)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(fire)
        super.onDetachedFromWindow()
    }
}

/** A widget's long-press menu. Choosing an item dismisses the menu, then hands on the choice. */
@Composable
fun WidgetOptionsMenu(expanded: Boolean, onRemove: () -> Unit, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(WidgetTags.MENU)) {
        DropdownMenuItem(
            text = { Text("Remove") },
            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
            onClick = { chooseFrom(expanded, onDismiss, onRemove) },
        )
    }
}
