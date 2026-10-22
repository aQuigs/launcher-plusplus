package com.sqftware.orbitlauncher.domain

import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

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
    val count = (metres / stick.metres * 10).roundToLong() / 10.0
    return "That's ${String.format(locale, "%,.1f", count)} ${if (count == 1.0) stick.one else stick.many}"
}

private const val METRES_PER_FOOT = 0.3048

/** The countries that measure in feet and miles, in the regions [Locale.getCountry] names. */
private val IMPERIAL = setOf("US", "LR", "MM")

/** [n] rounded to [places] decimals, as it shows, so the unit follows what shows: 0.996 m is a metre, not 100 cm. */
private fun shown(n: Double, places: Int): Double = 10.0.pow(places).let { (n * it).roundToLong() / it }

/** [metres] in the units [locale]'s country goes by, in centimetres or inches below a metre or a foot. */
fun distanceText(metres: Double, locale: Locale): String {
    fun format(pattern: String, n: Double) = String.format(locale, pattern, n)

    if (locale.country in IMPERIAL) {
        val feet = metres / METRES_PER_FOOT
        return when {
            shown(feet * 12, 0) < 12 -> format("%.0f in", feet * 12)
            shown(feet, 0) < 528 -> format("%,.0f ft", feet)
            else -> format("%,.2f mi", feet / 5_280)
        }
    }
    return when {
        shown(metres * 100, 0) < 100 -> format("%.0f cm", metres * 100)
        shown(metres, 1) < 1_000 -> format("%.1f m", metres)
        else -> format("%,.2f km", metres / 1_000)
    }
}

/** [seconds] as seconds under a minute, minutes and seconds under an hour, and hours and minutes beyond. */
fun durationText(seconds: Double, locale: Locale): String {
    if (shown(seconds, 1) < 60) return String.format(locale, "%.1f s", seconds)
    val whole = seconds.roundToLong()
    return if (whole < 3_600) "${whole / 60} min ${whole % 60} s" else String.format(locale, "%,d h %d min", whole / 3_600, whole % 3_600 / 60)
}
