package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import com.sqftware.orbitlauncher.domain.ReorderMode
import com.sqftware.orbitlauncher.domain.reordered

object ReorderTags {
    const val SWITCH = "reorder_switch"

    fun mode(mode: ReorderMode) = "reorder_${mode.name}"
}

/** How faint the item on the move shows where it would land, under the ghost that follows the finger. */
private const val LANDING_ALPHA = 0.35f

/**
 * Moving the items of one place, the ring, an open folder, the dock or a card, among themselves: a long press that moves
 * on, or the press itself with [startOnPress], picks up the item at a position, and the finger then goes as in an
 * [ItemDrag]. The place marks each position with [reorderSlot], so the finger can be told which one it is over, and
 * shows its items as [moving] says while one of them is on the move.
 */
class Rearrange(
    private val onStart: Rearrange.(index: Int, Offset) -> Boolean,
    private val onMove: (Offset) -> Unit,
    private val onDrop: () -> Unit,
    private val onCancel: () -> Unit,
    private val startOnPress: Boolean = false,
    private val movingIn: (Rearrange) -> Moving?,
) {
    // The positions' coordinates rather than their bounds: a page scrolling past would otherwise write them every frame,
    // while they are only read as the finger moves.
    private val placed = mutableMapOf<Int, LayoutCoordinates>()
    private val drags = mutableMapOf<Int, ItemDrag<Any?>>()

    /** The item of this place on the move, if it is one of this place's; read in composition, it follows the finger. */
    val moving: Moving? get() = movingIn(this)

    /** What picks up the item at [index]. */
    fun drag(index: Int): ItemDrag<Any?> =
        drags.getOrPut(index) { ItemDrag({ _, at -> onStart(index, at) }, onMove, onDrop, onCancel, startOnPress) }

    private fun place(index: Int, coordinates: LayoutCoordinates) {
        placed[index] = coordinates
    }

    // Only the entry still naming [coordinates]: another item may have taken the position since.
    private fun forget(index: Int, coordinates: LayoutCoordinates) {
        if (placed[index] === coordinates) placed.remove(index)
    }

    /**
     * The position nearest [finger], in root coordinates, if the finger is within that position's size of its centre.
     * Asked on every move of the finger, so it makes nothing on the way.
     */
    fun at(finger: Offset): Int? {
        var nearest: Int? = null
        var nearestDistance = 1f
        placed.forEach { (index, coordinates) ->
            val centre = coordinates.localToRoot(coordinates.size.center.toOffset())
            val distance = (centre - finger).getDistance() / maxOf(coordinates.size.width, coordinates.size.height)
            if (distance <= nearestDistance) {
                nearest = index
                nearestDistance = distance
            }
        }
        return nearest
    }

    /** Names the item at [index] as that position of its place, however its node was last placed. */
    internal class SlotNode(private var rearrange: Rearrange, private var index: Int) : Modifier.Node(), LayoutAwareModifierNode {
        private var coordinates: LayoutCoordinates? = null

        // A node handed another position may not be placed again, as when only its parent moves, so it names its new
        // position at once.
        fun update(rearrange: Rearrange, index: Int) {
            coordinates?.let { this.rearrange.forget(this.index, it) }
            this.rearrange = rearrange
            this.index = index
            coordinates?.let { rearrange.place(index, it) }
        }

        override fun onPlaced(coordinates: LayoutCoordinates) {
            this.coordinates = coordinates
            rearrange.place(index, coordinates)
        }

        override fun onDetach() {
            coordinates?.let { rearrange.forget(index, it) }
            coordinates = null
        }
    }
}

/**
 * Marks the item at [index] of a place as the position [rearrange] knows by that number, faded while it [lands] there as
 * the item on the move.
 */
fun Modifier.reorderSlot(rearrange: Rearrange?, index: Int, lands: Boolean): Modifier = when {
    rearrange == null -> this
    lands -> this then ReorderSlotElement(rearrange, index) then Modifier.alpha(LANDING_ALPHA)
    else -> this then ReorderSlotElement(rearrange, index)
}

private data class ReorderSlotElement(val rearrange: Rearrange, val index: Int) : ModifierNodeElement<Rearrange.SlotNode>() {
    override fun create() = Rearrange.SlotNode(rearrange, index)

    override fun update(node: Rearrange.SlotNode) = node.update(rearrange, index)
}

/** [items] in the order they show: as they would be if this item were dropped now, or as they are with none on the move. */
fun <T> Moving?.shown(items: List<T>): List<T> = this?.preview(items) ?: items

/** The item at position [from] of a place on the move: to [to] as [mode] says if dropped now, or back to [from] while [to] is null. */
data class Moving(val from: Int, val to: Int?, val mode: ReorderMode) {
    /** Where the item would show if dropped now. */
    val at: Int get() = to ?: from

    /** [items] as they would be if the item were dropped now, so the place can show it before the finger lets go. */
    fun <T> preview(items: List<T>): List<T> = if (to == null) items else items.reordered(from, to, mode)
}

/**
 * Insert or Swap: how an item dropped on another's place gets there. It shows while an item is on the move, so a second
 * finger can flip it before the first lets go.
 */
@Composable
fun ReorderModeSwitch(mode: ReorderMode, onModeChange: (ReorderMode) -> Unit, modifier: Modifier = Modifier) {
    // Solid, as what floats over other content is.
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier.testTag(ReorderTags.SWITCH)) {
        SingleChoiceSegmentedButtonRow(Modifier.padding(6.dp)) {
            ReorderMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == mode,
                    onClick = { onModeChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, ReorderMode.entries.size),
                    modifier = Modifier.testTag(ReorderTags.mode(option)),
                ) {
                    Text(option.name)
                }
            }
        }
    }
}
