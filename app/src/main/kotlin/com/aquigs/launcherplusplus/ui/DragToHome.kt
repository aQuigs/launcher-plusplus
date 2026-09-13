package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.testTag
import com.aquigs.launcherplusplus.domain.AppEntry
import kotlinx.coroutines.CancellationException

object DragTags {
    const val GHOST = "drag_ghost"
}

/**
 * Dragging an app out of the drawer onto the home screen: [onStart] once the finger holding an app moves off its row,
 * [onMove] as it goes, then [onDrop] where it lets go, or [onCancel] when the gesture is taken away or the row goes.
 * Positions are in the root's coordinates.
 */
class DragToHome(
    val onStart: (AppEntry, Offset) -> Unit,
    val onMove: (Offset) -> Unit,
    val onDrop: () -> Unit,
    val onCancel: () -> Unit,
)

/**
 * Lets a long press on [app] turn into a [drag] once the finger moves past touch slop. It reads the same touches as the
 * row's own long press, so the menu that opens stays until the finger moves. From then on every move is consumed, so
 * neither the list nor the sheet it sits in reads the drag as a scroll.
 *
 * It must follow the row's click handling in the modifier chain. Being inner, it sees each touch first in the main pass;
 * the click handling consumes every touch after a long press, and seen the other way round that would read as another
 * gesture taking over and end the long-press wait here before it began.
 */
fun Modifier.dragToHome(app: AppEntry, drag: DragToHome?): Modifier = if (drag == null) this else this then DragToHomeElement(app, drag)

private data class DragToHomeElement(val app: AppEntry, val drag: DragToHome) : ModifierNodeElement<DragToHomeNode>() {
    override fun create() = DragToHomeNode(app, drag)

    override fun update(node: DragToHomeNode) {
        node.app = app
        node.drag = drag
    }
}

private class DragToHomeNode(var app: AppEntry, var drag: DragToHome) : DelegatingNode() {
    init {
        delegate(
            SuspendingPointerInputModifierNode {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Each touch is placed through the row's coordinates as they are at that moment: the row can move
                    // under a still finger, while the list re-lays out during the hold or the drawer closes during the
                    // drag, and the start must be measured from where the finger came down, not from where the row went.
                    val coordinates = requireLayoutCoordinates()
                    val origin = coordinates.localToRoot(down.position)
                    val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    var started = false
                    try {
                        val dropped = drag(press.id) { change ->
                            change.consume()
                            val position = coordinates.localToRoot(change.position)
                            if (started) {
                                drag.onMove(position)
                            } else if ((position - origin).getDistance() > viewConfiguration.touchSlop) {
                                started = true
                                drag.onStart(app, position)
                            }
                        }
                        if (started) if (dropped) drag.onDrop() else drag.onCancel()
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
 * The icon of the [app] being dragged, as big as a ring icon, centred on [position] (relative to this ghost's parent)
 * and translucent so what it is over stays visible. Only the position is read while it moves, so nothing recomposes.
 */
@Composable
fun DragGhost(app: AppEntry, icon: suspend (AppEntry) -> ImageBitmap?, position: () -> Offset, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(RING_ICON_SIZE)
            .graphicsLayer {
                val centre = position()
                translationX = centre.x - size.width / 2
                translationY = centre.y - size.height / 2
                alpha = 0.85f
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag(DragTags.GHOST),
    ) {
        AppImage(app, icon, Modifier.fillMaxSize())
    }
}
