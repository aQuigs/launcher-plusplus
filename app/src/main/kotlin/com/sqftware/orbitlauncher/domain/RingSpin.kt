package com.sqftware.orbitlauncher.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.round

/** A whole turn of the ring, in radians: turned by any number of them, it shows as it always does. */
const val FULL_TURN = (2 * PI).toFloat()

/**
 * Whether a move of ([dx], [dy]) from ([x], [y]), both from the ring's centre in screen axes, goes round the ring more
 * than in or out of it.
 */
fun goesRound(x: Float, y: Float, dx: Float, dy: Float): Boolean = abs(x * dy - y * dx) > abs(x * dx + y * dy)

/**
 * How far round the centre ([x1], [y1]) is from ([x0], [y0]), in radians the short way, clockwise on screen as the
 * ring's slots count.
 */
fun turnBetween(x0: Float, y0: Float, x1: Float, y1: Float): Float = atan2(x0 * y1 - y0 * x1, x0 * x1 + y0 * y1)

/** How fast a finger at ([x], [y]) from the centre, moving at ([vx], [vy]), turns round it, clockwise, in radians per unit of time. */
fun turnRate(x: Float, y: Float, vx: Float, vy: Float): Float = (x * vy - y * vx) / (x * x + y * y)

/** Where a ring spun on toward [coastsTo] comes to rest: the nearest whole turn, so it ends as it always shows. */
fun restingTurn(coastsTo: Float): Float = round(coastsTo / FULL_TURN) * FULL_TURN
