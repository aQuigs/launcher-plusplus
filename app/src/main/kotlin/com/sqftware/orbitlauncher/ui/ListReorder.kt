package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * An item of a column, a collection card or a widget, dragged up or down it: which, how far the finger has moved it,
 * and where every item rests, so the others can make way. Positions are in the column's own coordinates, which its
 * scroll offset does not touch, and [gap] is the space between items, in pixels.
 */
class ListReorder(private val gap: Float) {
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
