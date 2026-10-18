package com.sqftware.orbitlauncher.domain

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

private const val RADIANS_PER_TURN = 2 * PI

/**
 * One spin of the ring or an open folder, from the drag that starts it till it comes to rest or is caught: how far it
 * turned [clockwise] and [anticlockwise], in radians, how fast it was let go, in radians a second either way, and how
 * long it coasted after, in seconds.
 */
data class Spin(val clockwise: Double, val anticlockwise: Double, val letGo: Double, val coast: Double)

/**
 * Every spin so far: how many there were and how many whole turns they made each way, with the records: the most turns
 * in one spin, the fastest let go, in turns a minute, and the longest coast, in seconds.
 */
data class SpinStats(
    val spins: Int = 0,
    val clockwise: Double = 0.0,
    val anticlockwise: Double = 0.0,
    val mostTurns: Double = 0.0,
    val topRpm: Double = 0.0,
    val longestCoast: Double = 0.0,
) {
    operator fun plus(spin: Spin): SpinStats {
        val clockwise = spin.clockwise / RADIANS_PER_TURN
        val anticlockwise = spin.anticlockwise / RADIANS_PER_TURN
        return SpinStats(
            spins = spins + 1,
            clockwise = this.clockwise + clockwise,
            anticlockwise = this.anticlockwise + anticlockwise,
            mostTurns = max(mostTurns, clockwise + anticlockwise),
            topRpm = max(topRpm, abs(spin.letGo) / RADIANS_PER_TURN * 60),
            longestCoast = max(longestCoast, spin.coast),
        )
    }
}
