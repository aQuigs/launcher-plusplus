package com.sqftware.orbitlauncher.domain

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

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
 * slot at the top, turned [turn] radians clockwise, the rest clockwise from it.
 */
fun ringSlotOffset(index: Int, count: Int, turn: Double = 0.0): Pair<Float, Float> {
    val angle = 2 * PI * index / count + turn
    return sin(angle).toFloat() to -cos(angle).toFloat()
}

/**
 * Where slot [index] of a folder of [count] apps sits, as [ringSlotOffset] gives it turned back half a slot, plus [turn]:
 * the first two either side of the top, or a lone app on top, so two sit side by side, three make a triangle on its point and four a square.
 * The closed folder's previews and the open folder's ring both place apps here, so each app opens where it was shown.
 */
fun folderSlotOffset(index: Int, count: Int, turn: Double = 0.0): Pair<Float, Float> =
    ringSlotOffset(index, count, if (count > 1) turn - PI / count else turn)

/** Names hanging under ring icons: each reaches [below] its icon, gap included, and keeps [air] from its neighbours and their names. */
data class HangingNames(val below: Float, val air: Float)

/**
 * Lays [count] icons out on a page whose shorter side is [side], keeping them [margin] inside its edges, all in the unit
 * of [fullSize]. Icons keep [fullSize] and the ring its usual radius while neighbours have room; a more crowded ring
 * first grows toward the edges, and only once it can grow no further do its icons shrink. On a page too small for
 * [fullSize] at the usual radius, the ring moves to wherever the icons can be largest between the emblem and the edge.
 * With [names], each icon's name is kept clear of its neighbours and their names, and the top and bottom ones' of the
 * emblem and the edge; a slot's corner between them can still reach over the emblem's rim on a small page.
 */
fun ringLayout(fullSize: Float, side: Float, count: Int, margin: Float, names: HangingNames? = null): RingLayout {
    val usual = side * RING_RADIUS_FRACTION
    val below = names?.below ?: 0f
    // What hangs under the top icon reaches toward the emblem as if the emblem were that much larger.
    val emblem = side * EMBLEM_FRACTION + 2 * below
    // And under the bottom icon toward the edge; the ring stays centred, so it keeps that clear at every edge.
    val room = side - 2 * (margin + below)
    val grip = grip(fullSize, side, count)
    val spacing = spacing(count)
    // What keeps icons apart and off the emblem lets them grow with the radius; the edge closes in as it grows.
    fun apart(radius: Float) = minOf(grip * radius, names?.fit(spacing * radius) ?: Float.MAX_VALUE, 2 * radius - emblem)

    val toFullSize = maxOf(fullSize / grip, (names?.apartFor(fullSize) ?: 0f) / spacing, (fullSize + emblem) / 2)
    val fullSizeFits = (room - fullSize) / 2
    val best = if (toFullSize <= fullSizeFits) usual.coerceIn(toFullSize, fullSizeFits) else meeting(0f, side) { apart(it) >= room - 2 * it }
    val radius = minOf(best, side * MAX_RING_RADIUS_FRACTION)
    return RingLayout(radius, ringIconSize(fullSize, side, count, radius, margin, names))
}

/**
 * The largest of [count] icons, up to [fullSize], that fit on a ring of [radius] on a page whose shorter side is [side]:
 * apart from each other, clear of the emblem, and [margin] inside the page's edges, all with their [names] if they have them.
 */
internal fun ringIconSize(fullSize: Float, side: Float, count: Int, radius: Float, margin: Float, names: HangingNames? = null): Float {
    val below = names?.below ?: 0f
    return minOf(
        fullSize,
        grip(fullSize, side, count) * radius,
        names?.fit(spacing(count) * radius) ?: fullSize,
        side - 2 * (margin + below) - 2 * radius,
        2 * radius - side * EMBLEM_FRACTION - 2 * below,
    ).coerceAtLeast(0f)
}

/** How wide the name under an icon [iconSize] across may be: the icon's share of the spacing, as neighbours keep it. */
fun ringNameWidth(iconSize: Float): Float = iconSize / RING_ICON_FILL

/**
 * How far apart neighbours' centres must be for an icon [size] across, at whatever angle round the ring they sit: far
 * enough that the neighbour clears the far corner of the name, and that two names clear each other.
 */
private fun HangingNames.apartFor(size: Float): Float {
    val name = ringNameWidth(size)
    return maxOf(hypot(name / 2, size / 2 + below) + size / 2, hypot(name, below)) + air
}

/** The largest icon whose name keeps clear of a neighbour [apart] away: [apartFor] turned round. */
private fun HangingNames.fit(apart: Float): Float {
    val room = apart - air
    if (room <= below) return 0f
    val fill2 = RING_ICON_FILL * RING_ICON_FILL
    val beside = RING_ICON_FILL * sqrt(room * room - below * below)
    // The corner's distance plus the icon's radius is the room; squared, that is a quadratic in the icon's size.
    val reach = below + room
    val corner = 2 * fill2 * (sqrt(reach * reach - (below * below - room * room) / fill2) - reach)
    return minOf(beside, corner)
}

/** The least of [from] to [to] where [reached], which holds from some point on, holds. */
private fun meeting(from: Float, to: Float, reached: (Float) -> Boolean): Float {
    var low = from
    var high = to
    repeat(40) {
        val middle = (low + high) / 2
        if (reached(middle)) high = middle else low = middle
    }
    return high
}

/** How far apart neighbours' centres are per unit of the ring's radius, which for a lone icon is as far as for two. */
private fun spacing(count: Int): Float = 2 * sin(PI / maxOf(count, 2)).toFloat()

/** How large each of [count] icons may be per unit of the ring's radius and still keep apart. */
private fun grip(fullSize: Float, side: Float, count: Int): Float {
    // A small page lets icons crowd as close as six of the largest that fit on the usual ring, as they always could.
    val usual = side * RING_RADIUS_FRACTION
    return spacing(count) * maxOf(RING_ICON_FILL, minOf(fullSize, side - 2 * usual) / usual)
}
