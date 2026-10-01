package com.sqftware.orbitlauncher.domain

/**
 * What the home clock shows: the time in the user's hour style and today's date, both spelled for their locale, and the
 * [minuteOfDay] for a theme that tells the time with hands.
 */
data class ClockFace(val time: String, val date: String, val twentyFourHour: Boolean, val minuteOfDay: Int)

/** How far round from twelve a watch's hour hand is at [minuteOfDay], in degrees: once round every twelve hours. */
fun hourHandDegrees(minuteOfDay: Int) = minuteOfDay % 720 / 2f

/** How far round from twelve its minute hand is, in degrees: once round every hour. */
fun minuteHandDegrees(minuteOfDay: Int) = minuteOfDay % 60 * 6f
