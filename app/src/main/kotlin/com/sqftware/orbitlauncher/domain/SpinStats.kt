package com.sqftware.orbitlauncher.domain

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

private const val RADIANS_PER_TURN = 2 * PI

/** A dp is a 160th of an inch on the screen. */
private const val METRES_PER_DP = 0.0254 / 160

/**
 * One spin of the ring or an open folder, from the drag that starts it till it comes to rest or is caught: how far it
 * turned [clockwise] and [anticlockwise], in radians, how fast it was let go, in radians a second either way, how
 * long it coasted after, in seconds, and how far its icons sat from its centre, its [rim], in dp.
 */
data class Spin(val clockwise: Double, val anticlockwise: Double, val letGo: Double, val coast: Double, val rim: Double)

/**
 * Every spin so far: how many there were, how many whole turns they made each way, how far the icons on the rim
 * travelled, in metres, and how long they coasted, in seconds, with the records: the most turns in one spin, the
 * fastest let go, in turns a minute, the longest coast, in seconds, the most days running with a spin, and the most
 * spins in a day. [lastDay], the epoch day of the latest spin or 0 before any, with its [lastDaySpins] and the [streak] of days
 * running up to it, are what the next day's spin carries on from.
 */
data class SpinStats(
    val spins: Int = 0,
    val clockwise: Double = 0.0,
    val anticlockwise: Double = 0.0,
    val distance: Double = 0.0,
    val coasting: Double = 0.0,
    val mostTurns: Double = 0.0,
    val topRpm: Double = 0.0,
    val longestCoast: Double = 0.0,
    val daysSpun: Int = 0,
    val longestStreak: Int = 0,
    val busiestDay: Int = 0,
    val lastDay: Long = 0,
    val lastDaySpins: Int = 0,
    val streak: Int = 0,
) {
    /** These stats with [spin], made [on] that day, added. */
    fun add(spin: Spin, on: LocalDate): SpinStats {
        val clockwise = spin.clockwise / RADIANS_PER_TURN
        val anticlockwise = spin.anticlockwise / RADIANS_PER_TURN
        // Day 0 is in 1970, so stats with no day yet start a streak like any day after a gap.
        val day = on.toEpochDay()
        // Any other day, even one before the last, as after a clock put right or a flight west, is a new one: a day kept
        // from a clock once set ahead would hold every spin after it in that one day.
        val sameDay = day == lastDay
        val streak = when {
            sameDay -> streak
            day == lastDay + 1 -> streak + 1
            else -> 1
        }
        val daySpins = if (sameDay) lastDaySpins + 1 else 1
        return SpinStats(
            spins = spins + 1,
            clockwise = this.clockwise + clockwise,
            anticlockwise = this.anticlockwise + anticlockwise,
            distance = distance + (spin.clockwise + spin.anticlockwise) * spin.rim * METRES_PER_DP,
            coasting = coasting + spin.coast,
            mostTurns = max(mostTurns, clockwise + anticlockwise),
            topRpm = max(topRpm, abs(spin.letGo) / RADIANS_PER_TURN * 60),
            longestCoast = max(longestCoast, spin.coast),
            daysSpun = if (sameDay) daysSpun else daysSpun + 1,
            longestStreak = max(longestStreak, streak),
            busiestDay = max(busiestDay, daySpins),
            lastDay = day,
            lastDaySpins = daySpins,
            streak = streak,
        )
    }
}
