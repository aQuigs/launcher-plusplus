package com.aquigs.launcherplusplus.domain

import kotlin.math.min

/** Share of each dock slot an icon fills; the rest is the gap to its neighbours. */
private const val DOCK_ICON_FILL = 0.8f

/** The icons of a dock laid out: each [iconSize] square, the nth starting [starts] along the row. */
class DockRow(val iconSize: Float, val starts: List<Float>)

/**
 * Lays [count] icons out along a dock [width] wide, in the unit of [fullSize] and [width]. Each has an equal slot, a
 * [fullSize] icon and its gap, and the row of slots sits in the middle of the width, so a few icons keep together
 * rather than spread across the dock; only once that many no longer fit do the slots narrow, and the icons with them.
 */
fun dockRow(fullSize: Float, width: Float, count: Int): DockRow {
    val slot = min(fullSize / DOCK_ICON_FILL, width / count)
    val iconSize = slot * DOCK_ICON_FILL
    val first = (width - slot * count) / 2 + (slot - iconSize) / 2
    return DockRow(iconSize, List(count) { first + slot * it })
}
