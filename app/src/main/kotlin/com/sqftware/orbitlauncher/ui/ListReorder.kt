package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.first

/**
 * An item of a column, such as a collection card, dragged up or down it, or of a row, such as a page in the page
 * editor, dragged along it: which, how far the finger has moved it, and where every item rests, so the others can make
 * way. Positions are along the list's axis in its own coordinates, which its scroll offset does not touch, and [gap] is
 * the space between items, in pixels. A [horizontal] list runs from its start, which is its right when [mirrored], as in a
 * right-to-left layout.
 */
class ListReorder(private val gap: Float, val horizontal: Boolean = false, val mirrored: Boolean = false) {
    var count by mutableIntStateOf(0)
    var dragging by mutableStateOf<Int?>(null)
    var offset by mutableFloatStateOf(0f)
    private val tops = mutableStateMapOf<Int, Float>()
    private val heights = mutableStateMapOf<Int, Float>()

    /**
     * Where the dragged item would land: past every item whose middle its leading edge has crossed, so a tall item moves
     * as soon as it covers half a short one.
     */
    val target: Int? by derivedStateOf {
        val from = dragging
        val top = tops[from]
        val height = heights[from]
        if (from == null || top == null || height == null) {
            null
        } else {
            val movedTop = top + offset
            val below = (from + 1 until count).count { middleOf(it).let { m -> m != null && m < movedTop + height } }
            val above = (0 until from).count { middleOf(it).let { m -> m != null && m > movedTop } }
            from + below - above
        }
    }

    // Only a change is written: a state map tells its readers about every put, and every layout pass places every item.
    fun place(index: Int, top: Float, height: Float) {
        if (tops[index] != top) tops[index] = top
        if (heights[index] != height) heights[index] = height
    }

    /** How far the item at [index] moves aside, in pixels, to leave the dragged item its place. */
    fun shift(index: Int): Float {
        val from = dragging ?: return 0f
        val to = target ?: return 0f
        val room = (heights[from] ?: return 0f) + gap
        return when (index) {
            in (from + 1)..to -> -room
            in to until from -> room
            else -> 0f
        }
    }

    /** Moves the dragged item by a finger's move of [px] on screen, across or down it as the list runs. */
    fun moveBy(px: Float) {
        offset += if (mirrored) -px else px
    }

    /** Ends the drag, saying where the item came from and where it landed. */
    fun end(): Pair<Int, Int>? {
        val move = dragging?.let { from -> target?.let { from to it } }
        dragging = null
        offset = 0f
        return move
    }

    private fun middleOf(index: Int): Float? {
        val top = tops[index] ?: return null
        val height = heights[index] ?: return null
        return top + height / 2
    }
}

/**
 * Item [index] of [reorder]'s list: where it rests is recorded, and it is lifted over the others and follows the finger
 * while dragged, or slides aside to make way for the one that is.
 */
@Composable
fun Modifier.reorderItem(reorder: ListReorder, index: Int): Modifier {
    val dragged = reorder.dragging == index
    // The others slide aside while an item is dragged, and snap back the moment it lands: they are then laid out where they
    // slid to, and animating from there would take them somewhere else first.
    val shift by animateFloatAsState(
        targetValue = if (dragged) 0f else reorder.shift(index),
        animationSpec = if (reorder.dragging == null) snap() else spring(),
        label = "reorder_shift",
    )
    val onScreen = if (reorder.mirrored) -1f else 1f

    return this
        // Measured outside the layer below, so the item's resting place is recorded, not where it has been dragged to.
        .onGloballyPositioned {
            val position = it.positionInParent().let { at -> if (reorder.horizontal) at.x else at.y }
            val length = (if (reorder.horizontal) it.size.width else it.size.height).toFloat()
            val list = it.parentLayoutCoordinates?.size?.width?.toFloat() ?: 0f
            reorder.place(index, if (reorder.mirrored) list - position - length else position, length)
        }
        .zIndex(if (dragged) 1f else 0f)
        .graphicsLayer {
            val moved = onScreen * if (dragged) reorder.offset else shift
            if (reorder.horizontal) translationX = moved else translationY = moved
        }
}

/**
 * Scrolls while a held finger is near an edge, as [pull] says, from -1 at the start to 1 at the end, faster the nearer it is,
 * up to [speed] pixels a second, as far as [room] lets each step go; [follow] gets each step, to move what is held with it.
 * Frames run only while there is somewhere to scroll to, so a thing held still leaves the screen idle.
 */
suspend fun ScrollState.scrollAtEdges(speed: Float, pull: () -> Float, room: (px: Float) -> Float = { it }, follow: (px: Float) -> Unit) {
    fun canScroll() = pull().let { it != 0f && room(it) != 0f && if (it > 0f) canScrollForward else canScrollBackward }
    while (true) {
        snapshotFlow { canScroll() }.first { it }
        var last = withFrameNanos { it }
        while (canScroll()) {
            val now = withFrameNanos { it }
            follow(dispatchRawDelta(room(pull() * speed * (now - last) / 1_000_000_000f)))
            last = now
        }
    }
}
