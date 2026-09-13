package com.aquigs.launcherplusplus.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The ring's radius, as a fraction of the page's shorter side. */
const val RING_RADIUS_FRACTION = 0.36f

/** The emblem's diameter, as a fraction of the page's shorter side. */
const val EMBLEM_FRACTION = 0.42f

/** How many icons the ring holds at full size. Past that the icons shrink rather than the ring growing. */
const val FULL_SIZE_RING_SLOTS = 6

/**
 * Where slot [index] out of [count] sits, as a unit offset from the centre in screen axes (x right, y down): the first
 * slot at the top, the rest clockwise.
 */
fun ringSlotOffset(index: Int, count: Int): Pair<Float, Float> {
    val angle = 2 * PI * index / count
    return sin(angle).toFloat() to -cos(angle).toFloat()
}

/**
 * The size of each of [count] icons on a page whose shorter side is [side], in the unit of [fullSize] and [side]. Up to
 * [FULL_SIZE_RING_SLOTS] icons keep [fullSize] unless the page is too small to fit it between the emblem and the edge.
 * Past that every icon shrinks in step with the distance between neighbours, so they never overlap.
 */
fun ringIconSize(fullSize: Float, side: Float, count: Int): Float {
    val clearance = min(RING_RADIUS_FRACTION - EMBLEM_FRACTION / 2, 0.5f - RING_RADIUS_FRACTION)
    val largest = min(fullSize, 2 * clearance * side)
    if (count <= FULL_SIZE_RING_SLOTS) return largest
    return largest * (sin(PI / count) / sin(PI / FULL_SIZE_RING_SLOTS)).toFloat()
}
