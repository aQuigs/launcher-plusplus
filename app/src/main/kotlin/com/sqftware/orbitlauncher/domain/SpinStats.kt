package com.sqftware.orbitlauncher.domain

import java.time.LocalDate
import java.util.Locale
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
 * spins in a day. [lastDay], the epoch day of the latest spin, with its [lastDaySpins] and the [streak] of days
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
    val lastDay: Long? = null,
    val lastDaySpins: Int = 0,
    val streak: Int = 0,
) {
    /** These stats with [spin], made [on] that day, added. */
    fun add(spin: Spin, on: LocalDate): SpinStats {
        val clockwise = spin.clockwise / RADIANS_PER_TURN
        val anticlockwise = spin.anticlockwise / RADIANS_PER_TURN
        val day = on.toEpochDay()
        // A clock set back counts as the same day, so it never breaks a streak or starts one over.
        val sameDay = lastDay != null && day <= lastDay
        val streak = when {
            sameDay -> streak
            lastDay != null && day == lastDay + 1 -> streak + 1
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
            lastDay = if (sameDay) lastDay else day,
            lastDaySpins = daySpins,
            streak = streak,
        )
    }
}

/** Something [metres] long that a distance spun is put next to, named [one] and, for more than one, [many]. */
private class Yardstick(val metres: Double, val one: String, val many: String)

private val YARDSTICKS = listOf(
    Yardstick(0.18, "banana", "bananas"),
    Yardstick(5.5, "giraffe", "giraffes"),
    Yardstick(30.0, "blue whale", "blue whales"),
    Yardstick(105.0, "football pitch", "football pitches"),
    Yardstick(330.0, "Eiffel Tower", "Eiffel Towers"),
    Yardstick(8_849.0, "Everest", "Everests"),
    Yardstick(42_195.0, "marathon", "marathons"),
    Yardstick(40_075_000.0, "trip round the Earth", "trips round the Earth"),
)

/** [metres] as so many of the longest yardstick it reaches, such as "That's 7.6 bananas", or null short of the first. */
fun yardstickText(metres: Double, locale: Locale): String? {
    val stick = YARDSTICKS.lastOrNull { metres >= it.metres } ?: return null
    val count = String.format(locale, "%,.1f", metres / stick.metres)
    return "That's $count ${if (count == String.format(locale, "%.1f", 1.0)) stick.one else stick.many}"
}

private const val METRES_PER_FOOT = 0.3048
private const val METRES_PER_MILE = 1_609.344

/** The countries that measure in feet and miles, in the regions [Locale.getCountry] names. */
private val IMPERIAL = setOf("US", "LR", "MM")

/** [metres] in the units [locale]'s country goes by, in centimetres or inches below a metre or a foot. */
fun distanceText(metres: Double, locale: Locale): String {
    fun format(pattern: String, n: Double) = String.format(locale, pattern, n)

    if (locale.country in IMPERIAL) {
        val feet = metres / METRES_PER_FOOT
        return when {
            feet < 1 -> format("%.0f in", feet * 12)
            metres < METRES_PER_MILE / 10 -> format("%,.0f ft", feet)
            else -> format("%,.2f mi", metres / METRES_PER_MILE)
        }
    }
    return when {
        metres < 1 -> format("%.0f cm", metres * 100)
        metres < 1_000 -> format("%.1f m", metres)
        else -> format("%,.2f km", metres / 1_000)
    }
}

/** [seconds] as seconds under a minute, minutes and seconds under an hour, and hours and minutes beyond. */
fun durationText(seconds: Double, locale: Locale): String {
    val whole = seconds.toLong()
    return when {
        seconds < 60 -> String.format(locale, "%.1f s", seconds)
        seconds < 3_600 -> "${whole / 60} min ${whole % 60} s"
        else -> String.format(locale, "%,d h %d min", whole / 3_600, whole % 3_600 / 60)
    }
}
