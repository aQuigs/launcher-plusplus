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

/**
 * Lays [count] icons out on a page whose shorter side is [side], keeping them [margin] inside its edges, all in the unit
 * of [fullSize]. Icons keep [fullSize] and the ring its usual radius while neighbours have room; a more crowded ring
 * first grows toward the edges, and only once it can grow no further do its icons shrink. On a page too small for
 * [fullSize] at the usual radius, the ring moves to wherever the icons can be largest between the emblem and the edge.
 * What hangs [below] each icon, its name, is kept clear of the edge and of the emblem too.
 */
fun ringLayout(fullSize: Float, side: Float, count: Int, margin: Float, below: Float = 0f): RingLayout {
    val usual = side * RING_RADIUS_FRACTION
    // What hangs under the top icon reaches toward the emblem as if the emblem were that much larger.
    val emblem = side * EMBLEM_FRACTION + 2 * below
    // And under the bottom icon toward the edge; the ring stays centred, so it keeps that clear at every edge.
    val room = side - 2 * (margin + below)
    val grip = grip(fullSize, side, count)

    // Icons grow with the radius until they are full size, and until they meet the edge, which closes in as it grows.
    val toFullSize = maxOf((fullSize + below) / grip, (fullSize + emblem) / 2)
    val fullSizeFits = (room - fullSize) / 2
    val toEdge = maxOf((room + below) / (2 + grip), (room + emblem) / 4)
    val best = if (toFullSize <= fullSizeFits) usual.coerceIn(toFullSize, fullSizeFits) else toEdge
    val radius = minOf(best, side * MAX_RING_RADIUS_FRACTION)
    return RingLayout(radius, ringIconSize(fullSize, side, count, radius, margin, below))
}

/**
 * The largest of [count] icons, up to [fullSize], that fit on a ring of [radius] on a page whose shorter side is [side]:
 * apart from each other, and with what hangs [below] them clear of the next icon, the emblem and [margin] inside the
 * page's edges.
 */
internal fun ringIconSize(fullSize: Float, side: Float, count: Int, radius: Float, margin: Float, below: Float = 0f): Float = minOf(
    fullSize,
    // Beside the ring the next icon is under this one, where its name hangs.
    grip(fullSize, side, count) * radius - below,
    side - 2 * (margin + below) - 2 * radius,
    2 * radius - side * EMBLEM_FRACTION - 2 * below,
).coerceAtLeast(0f)

/**
 * How wide the name hanging [below] an icon [iconSize] across may be: as far as neighbours' centres are apart once the
 * ring is crowded enough to space its icons by their names, so neighbours' names never meet side by side. Pages too small
 * to keep [RING_ICON_FILL] crowd closer, as the icons do.
 */
fun ringNameWidth(iconSize: Float, below: Float): Float = (iconSize + below) / RING_ICON_FILL

/** How large each of [count] icons may be per unit of the ring's radius and still keep apart. */
private fun grip(fullSize: Float, side: Float, count: Int): Float {
    // How far apart neighbours' centres are per unit of radius, which for a lone icon is as far as for two.
    val spacing = 2 * sin(PI / maxOf(count, 2)).toFloat()
    // A small page lets icons crowd as close as six of the largest that fit on the usual ring, as they always could.
    val usual = side * RING_RADIUS_FRACTION
    return spacing * maxOf(RING_ICON_FILL, minOf(fullSize, side - 2 * usual) / usual)
}
