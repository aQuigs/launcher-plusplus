package com.sqftware.orbitlauncher.domain

/** What the home clock shows: the time in the user's hour style and today's date, both spelled for their locale. */
data class ClockFace(val time: String, val date: String, val twentyFourHour: Boolean)
