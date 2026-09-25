package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import kotlinx.coroutines.CancellationException

object DragTags {
    const val GHOST = "drag_ghost"
}

/**
 * Dragging an item, an app or a folder, away from the row or icon holding it: [onStart] once the finger holding it has
 * moved off it, or at the hold itself with [startOnPress]; [onMove] as it goes; then [onDrop] where it lets go, or
 * [onCancel] when the gesture is taken away or the item's node goes. Positions are in the root's coordinates. [onStart]
 * says whether the drag may begin: one refused, as while another finger drags something, hears nothing more.
 */
class ItemDrag<in T>(
    val onStart: (T, Offset) -> Boolean,
    val onMove: (Offset) -> Unit,
    val onDrop: () -> Unit,
    val onCancel: () -> Unit,
    val startOnPress: Boolean = false,
)

typealias AppDrag = ItemDrag<AppEntry>

/**
 * Lets a long press on [item] turn into a [drag]: once the finger moves past touch slop, or at the press itself when the
 * drag starts on press. It reads the same touches as the node's own press handling, so a menu that opens on the long
 * press stays until the finger moves. From the start on, every move is consumed, so neither a list nor a pager under
 * the item reads the drag as a scroll, and so is the release, so a tap handler does not act on it too.
 *
 * It must follow the click handling in the modifier chain. Being inner, it sees each touch first in the main pass; the
 * click handling consumes every touch after a long press, and seen the other way round that would read as another
 * gesture taking over and end the long-press wait here before it began.
 */
fun <T> Modifier.itemDrag(item: T, drag: ItemDrag<T>?): Modifier = if (drag == null) this else this then ItemDragElement(item, drag)

private data class ItemDragElement<T>(val item: T, val drag: ItemDrag<T>) : ModifierNodeElement<ItemDragNode<T>>() {
    override fun create() = ItemDragNode(item, drag)

    override fun update(node: ItemDragNode<T>) {
        node.item = item
        node.drag = drag
    }
}

private class ItemDragNode<T>(var item: T, var drag: ItemDrag<T>) : DelegatingNode() {
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Each touch is placed through the node's coordinates as they are at that moment: the node can move
                    // under a still finger, while a list re-lays out during the hold, the drawer closes during the drag
                    // or the item's place shows where it would land, and the start must be measured from where the
                    // finger came down, not from where the node went.
                    val coordinates = requireLayoutCoordinates()
                    val origin = coordinates.localToRoot(down.position)
                    val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    // A refused drag still takes the finger's moves to their end, so no pager under it pages instead.
                    var started = false
                    var refused = false
                    fun start(position: Offset) {
                        if (drag.onStart(item, position)) started = true else refused = true
                    }
                    if (drag.startOnPress) start(coordinates.localToRoot(press.position))
                    try {
                        while (true) {
                            // The finger is gone, or another gesture has taken the touch: a change already consumed, which
                            // is also how a cancelled touch arrives, so it is checked before the release is.
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == press.id }
                            if (change == null || change.isConsumed) break
                            if (change.changedToUp()) {
                                if (started) {
                                    change.consume()
                                    drag.onDrop()
                                }
                                return@awaitEachGesture
                            }
                            if (!change.positionChanged()) continue
                            change.consume()
                            val position = coordinates.localToRoot(change.position)
                            if (started) {
                                drag.onMove(position)
                            } else if (!refused && (position - origin).getDistance() > viewConfiguration.touchSlop) {
                                start(position)
                            }
                        }
                        if (started) drag.onCancel()
                    } catch (e: CancellationException) {
                        if (started) drag.onCancel()
                        throw e
                    }
                }
            },
        )
    }
}

/**
 * The item being dragged, its [content] on a disc as big as a ring icon, centred on [position] (relative to this ghost's
 * parent) and translucent so what it is over stays visible, with its [label] under it if given. Only the position is
 * read while it moves, so nothing recomposes.
 */
@Composable
fun DragGhost(
    position: () -> Offset,
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.graphicsLayer {
            val centre = position()
            translationX = centre.x - size.width / 2
            translationY = centre.y - RING_ICON_SIZE.toPx() / 2
            alpha = 0.85f
        },
    ) {
        // Solid, not the icons' glass: it floats over whatever it is dragged across.
        Box(
            Modifier
                .size(RING_ICON_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .testTag(DragTags.GHOST),
            contentAlignment = Alignment.Center,
            content = content,
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp).widthIn(max = 120.dp),
            )
        }
    }
}
