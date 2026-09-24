package com.sqftware.orbitlauncher.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The ring's radius while its icons have room, as a fraction of the page's shorter side. */
const val RING_RADIUS_FRACTION = 0.36f

/** The furthest a crowded ring grows toward the page's edges before its icons shrink, as a fraction of the shorter side. */
const val MAX_RING_RADIUS_FRACTION = 0.4f

/** The emblem's diameter, as a fraction of the page's shorter side. */
const val EMBLEM_FRACTION = 0.42f

/** The most of the distance between neighbours' centres an icon takes; the rest keeps them apart. Closer than Arc's, whose crowded rings shrink icons too far. */
private const val RING_ICON_FILL = 0.7f

/** A ring laid out: its slots' centres [radius] from the middle, each icon [iconSize] square. */
data class RingLayout(val radius: Float, val iconSize: Float)

/**
 * Where slot [index] out of [count] sits, as a unit offset from the centre in screen axes (x right, y down): the first
 * slot at the top, the rest clockwise.
 */
fun ringSlotOffset(index: Int, count: Int): Pair<Float, Float> {
    val angle = 2 * PI * index / count
    return sin(angle).toFloat() to -cos(angle).toFloat()
}

/**
 * Lays [count] icons out on a page whose shorter side is [side], keeping them [margin] inside its edges, all in the unit
 * of [fullSize]. Icons keep [fullSize] and the ring its usual radius while neighbours have room; a more crowded ring
 * first grows toward the edges, and only once it can grow no further do its icons shrink. On a page too small for
 * [fullSize] at the usual radius, the ring moves to wherever the icons can be largest between the emblem and the edge.
 */
fun ringLayout(fullSize: Float, side: Float, count: Int, margin: Float): RingLayout {
    val usual = side * RING_RADIUS_FRACTION
    val emblem = side * EMBLEM_FRACTION
    val room = side - 2 * margin
    val grip = grip(fullSize, side, count)

    // Icons grow with the radius until they are full size, and until they meet the edge, which closes in as it grows.
    val toFullSize = maxOf(fullSize / grip, (fullSize + emblem) / 2)
    val fullSizeFits = (room - fullSize) / 2
    val toEdge = maxOf(room / (2 + grip), (room + emblem) / 4)
    val best = if (toFullSize <= fullSizeFits) usual.coerceIn(toFullSize, fullSizeFits) else toEdge
    val radius = minOf(best, side * MAX_RING_RADIUS_FRACTION)
    return RingLayout(radius, ringIconSize(fullSize, side, count, radius, margin))
}

/**
 * The largest of [count] icons, up to [fullSize], that fit on a ring of [radius] on a page whose shorter side is [side]:
 * apart from each other, clear of the emblem, and [margin] inside the page's edges.
 */
internal fun ringIconSize(fullSize: Float, side: Float, count: Int, radius: Float, margin: Float): Float = minOf(
    fullSize,
    grip(fullSize, side, count) * radius,
    side - 2 * margin - 2 * radius,
    2 * radius - side * EMBLEM_FRACTION,
).coerceAtLeast(0f)

/** How large each of [count] icons may be per unit of the ring's radius and still keep apart. */
private fun grip(fullSize: Float, side: Float, count: Int): Float {
    // How far apart neighbours' centres are per unit of radius, which for a lone icon is as far as for two.
    val spacing = 2 * sin(PI / maxOf(count, 2)).toFloat()
    // A small page lets icons crowd as close as six of the largest that fit on the usual ring, as they always could.
    val usual = side * RING_RADIUS_FRACTION
    return spacing * maxOf(RING_ICON_FILL, minOf(fullSize, side - 2 * usual) / usual)
}
