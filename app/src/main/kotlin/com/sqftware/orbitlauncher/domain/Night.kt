package com.sqftware.orbitlauncher.domain

/** When night falls and when it ends, in minutes from midnight: about when a game's sky goes dark and grows light again. */
private const val DUSK = 19 * 60
private const val DAWN = 6 * 60

/** Whether [minuteOfDay] is after dark. */
fun isNight(minuteOfDay: Int): Boolean = minuteOfDay >= DUSK || minuteOfDay < DAWN
