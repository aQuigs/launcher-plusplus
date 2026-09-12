package com.aquigs.launcherplusplus.domain

import kotlin.math.PI
import kotlin.math.sin

/** How many icons the ring holds at full size. Past that the icons shrink rather than the ring growing. */
const val FULL_SIZE_RING_SLOTS = 6

/** Angle of slot [index] out of [count], in radians clockwise from twelve o'clock. */
fun ringSlotAngle(index: Int, count: Int): Double = 2 * PI * index / count

/**
 * Icon size as a fraction of full size: 1 up to [FULL_SIZE_RING_SLOTS] icons, then in step with the distance between
 * neighbouring slots, so the gap between icons keeps its proportion and they never overlap.
 */
fun ringIconScale(count: Int): Float =
    if (count <= FULL_SIZE_RING_SLOTS) 1f else (sin(PI / count) / sin(PI / FULL_SIZE_RING_SLOTS)).toFloat()
