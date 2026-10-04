package com.sqftware.orbitlauncher.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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

/** A ring laid out: its slots' centres [radius] from the middle, each icon [iconSize] square, and the room each slot gives its [names], if it has them. */
data class RingLayout(val radius: Float, val iconSize: Float, val names: List<NameRoom> = emptyList())

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

/** How wide a name is on one line, and broken onto two at a word, infinite for a name of one word. */
data class NameWidths(val oneLine: Float, val twoLines: Float = Float.POSITIVE_INFINITY)

/** The room a slot gives its name: up to [width] across on up to [lines] lines, and whether the name [fits] there or is cut. */
data class NameRoom(val width: Float, val lines: Int, val fits: Boolean = true)

/**
 * Names hanging under ring icons, one per slot in [widths], null for none: [gap] under the icon, in lines [line] tall,
 * kept [air] from the other icons and names and from the emblem, and inside the page, which may reach [spare] past the
 * foot of the square the ring is laid out in. The slots sit where [at] puts them.
 */
data class HangingNames(
    val gap: Float,
    val line: Float,
    val air: Float,
    val widths: List<NameWidths?>,
    val at: (index: Int, count: Int) -> Pair<Float, Float> = ::ringSlotOffset,
    val spare: Float = 0f,
)

/** The least of the icons' size without names that names leave them: less than all the names need, so long ones wrap or are cut. */
private const val NAMED_ICON_FLOOR = 0.85f

/**
 * Lays [count] icons out on a page whose shorter side is [side], keeping them [margin] inside its edges, all in the unit
 * of [fullSize]. Icons keep [fullSize] and the ring its usual radius while neighbours have room; a more crowded ring
 * first grows toward the edges, and only once it can grow no further do its icons shrink. On a page too small for
 * [fullSize] at the usual radius, the ring moves to wherever the icons can be largest between the emblem and the edge.
 *
 * With [names], icons are as large as leaves every name room to show in full, on one line or two, but never smaller than
 * [NAMED_ICON_FLOOR] of their size without names; past that, a name that does not fit is cut. The ring keeps its radius
 * without names if the names fit there, and otherwise grows only as far as they need.
 */
fun ringLayout(fullSize: Float, side: Float, count: Int, margin: Float, names: HangingNames? = null): RingLayout {
    val plain = plainLayout(fullSize, side, count, margin)
    if (names == null || count == 0) return plain

    val grip = grip(fullSize, side, count)
    val emblem = side * EMBLEM_FRACTION
    // Where icons [size] across may sit: apart, off the emblem and inside the page, with room under the bottom one for its name.
    fun least(size: Float) = maxOf(size / grip, (size + emblem) / 2)
    fun most(size: Float) = maxOf(
        least(size),
        minOf(side * MAX_RING_RADIUS_FRACTION, side / 2 - margin - size / 2, side / 2 + names.spare - size / 2 - names.gap - names.line),
    )
    fun fit(size: Float, radius: Float) = names.rooms(count, size, radius, side).allFit()

    val size = if (fit(plain.iconSize, most(plain.iconSize))) plain.iconSize else meeting(plain.iconSize, plain.iconSize * NAMED_ICON_FLOOR) { fit(it, most(it)) }
    val start = if (size == plain.iconSize) plain.radius else RING_RADIUS_FRACTION * side
    val from = start.coerceIn(least(size), most(size))
    val radius = if (fit(size, from)) from else meeting(from, most(size)) { fit(size, it) }
    return RingLayout(radius, size, names.rooms(count, size, radius, side))
}

/** The ring of [ringLayout] without names. */
private fun plainLayout(fullSize: Float, side: Float, count: Int, margin: Float): RingLayout {
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

/** Whether every name has room to show in full. */
private fun List<NameRoom>.allFit() = all { it.fits }

/**
 * The room each of [count] slots on a ring of [radius] gives its name under an icon [size] across, on a page whose
 * shorter side is [side]: on one line if it fits, else two, else one where it is cut.
 */
private fun HangingNames.rooms(count: Int, size: Float, radius: Float, side: Float): List<NameRoom> = List(count) { index ->
    val name = widths[index] ?: return@List NameRoom(0f, 1)
    val one = room(index, count, size, radius, side, 1)
    when {
        name.oneLine <= one -> NameRoom(one, 1)
        name.twoLines <= room(index, count, size, radius, side, 2) -> NameRoom(room(index, count, size, radius, side, 2), 2)
        else -> NameRoom(one, 1, fits = false)
    }
}

/**
 * How wide the name of slot [index] may be on [lines] lines: clear of every icon, of the emblem and of the page's edges,
 * and of each name beside it, which takes half the gap between them. Names beside are taken to be two lines tall, as any
 * may be.
 */
private fun HangingNames.room(index: Int, count: Int, size: Float, radius: Float, side: Float, lines: Int): Float {
    fun place(i: Int) = at(i, count).let { (x, y) -> x * radius to y * radius }
    val (x, y) = place(index)
    val top = y + size / 2 + gap
    val bottom = top + lines * line
    if (bottom > side / 2 + spare) return 0f
    var half = side / 2 - abs(x)

    // A disc keeps the name off as far across as its edge reaches at the name's nearest line.
    fun clear(cx: Float, cy: Float, discRadius: Float) {
        val reach = discRadius + air
        val off = maxOf(0f, top - cy, cy - bottom)
        if (off < reach) half = minOf(half, abs(cx - x) - sqrt(reach * reach - off * off))
    }

    clear(0f, 0f, side * EMBLEM_FRACTION / 2)
    for (other in 0 until count) {
        if (other == index) continue
        val (ox, oy) = place(other)
        clear(ox, oy, size / 2)
        val otherTop = oy + size / 2 + gap
        if (widths[other] != null && otherTop < bottom + air && otherTop + 2 * line + air > top) half = minOf(half, (abs(ox - x) - air) / 2)
    }
    return (2 * half).coerceAtLeast(0f)
}

/** The first point from [from] toward [to] where [reached] holds, as it does from there on, or [to] if it never does. */
private fun meeting(from: Float, to: Float, reached: (Float) -> Boolean): Float {
    var before = from
    var after = to
    repeat(40) {
        val middle = (before + after) / 2
        if (reached(middle)) after = middle else before = middle
    }
    return after
}

/** How large each of [count] icons may be per unit of the ring's radius and still keep apart. */
private fun grip(fullSize: Float, side: Float, count: Int): Float {
    // How far apart neighbours' centres are per unit of radius, which for a lone icon is as far as for two.
    val spacing = 2 * sin(PI / maxOf(count, 2)).toFloat()
    // A small page lets icons crowd as close as six of the largest that fit on the usual ring, as they always could.
    val usual = side * RING_RADIUS_FRACTION
    return spacing * maxOf(RING_ICON_FILL, minOf(fullSize, side - 2 * usual) / usual)
}
