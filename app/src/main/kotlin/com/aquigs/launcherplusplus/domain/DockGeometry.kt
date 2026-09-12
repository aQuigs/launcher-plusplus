package com.aquigs.launcherplusplus.domain

import kotlin.math.min

/** Share of each dock slot an icon may fill; the rest is the gap to its neighbours. */
private const val DOCK_ICON_FILL = 0.8f

/**
 * The size of each of [count] icons in a dock [width] wide, in the unit of [fullSize] and [width]. The row is split into
 * equal slots. Icons keep [fullSize] while it fits a slot with a gap, and shrink together once the row gets crowded.
 */
fun dockIconSize(fullSize: Float, width: Float, count: Int): Float =
    if (count == 0) fullSize else min(fullSize, width / count * DOCK_ICON_FILL)
