package com.sqftware.orbitlauncher.domain

/** How an item dropped on another's place gets there. */
enum class ReorderMode {
    /** It takes that place, and the items from there to where it was shift one along to close the gap. */
    Insert,

    /** It and the item in that place trade places; the rest stay put. */
    Swap,
}

/** This list with the item at [from] moved to [to] as [mode] says. An index off the list changes nothing. */
fun <T> List<T>.reordered(from: Int, to: Int, mode: ReorderMode): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply {
        when (mode) {
            ReorderMode.Insert -> add(to, removeAt(from))
            ReorderMode.Swap -> this[to] = set(from, this[to])
        }
    }
}

/** The position in this list of the item that [reordered], given the same [from], [to] and [mode], puts at [position]. */
fun <T> List<T>.reorderedFrom(position: Int, from: Int, to: Int, mode: ReorderMode): Int = when {
    from !in indices || to !in indices -> position
    position == to -> from
    mode == ReorderMode.Swap -> if (position == from) to else position
    position in from..<to -> position + 1
    position in to + 1..from -> position - 1
    else -> position
}
