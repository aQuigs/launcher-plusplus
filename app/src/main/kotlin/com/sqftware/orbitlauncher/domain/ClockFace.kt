package com.sqftware.orbitlauncher.domain

/**
 * What the home clock shows: the time in the user's hour style and today's date, both spelled for their locale, and the
 * [minuteOfDay] for a theme that tells the time with hands.
 */
data class ClockFace(val time: String, val date: String, val twentyFourHour: Boolean, val minuteOfDay: Int)
