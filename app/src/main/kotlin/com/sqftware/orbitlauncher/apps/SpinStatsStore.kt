package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.SpinStats

interface SpinStatsStore {
    fun load(): SpinStats

    fun save(stats: SpinStats)
}

class SharedPreferencesSpinStatsStore(context: Context) : SpinStatsStore {
    private val prefs = context.getSharedPreferences("spin_stats", Context.MODE_PRIVATE)

    override fun load() = SpinStats(
        spins = prefs.getInt(SPINS, 0),
        clockwise = prefs.getDouble(CLOCKWISE),
        anticlockwise = prefs.getDouble(ANTICLOCKWISE),
        distance = prefs.getDouble(DISTANCE),
        coasting = prefs.getDouble(COASTING),
        mostTurns = prefs.getDouble(MOST_TURNS),
        topRpm = prefs.getDouble(TOP_RPM),
        longestCoast = prefs.getDouble(LONGEST_COAST),
        daysSpun = prefs.getInt(DAYS_SPUN, 0),
        longestStreak = prefs.getInt(LONGEST_STREAK, 0),
        busiestDay = prefs.getInt(BUSIEST_DAY, 0),
        lastDay = prefs.getLong(LAST_DAY, 0),
        lastDaySpins = prefs.getInt(LAST_DAY_SPINS, 0),
        streak = prefs.getInt(STREAK, 0),
    )

    override fun save(stats: SpinStats) = prefs.edit {
        putInt(SPINS, stats.spins)
        putDouble(CLOCKWISE, stats.clockwise)
        putDouble(ANTICLOCKWISE, stats.anticlockwise)
        putDouble(DISTANCE, stats.distance)
        putDouble(COASTING, stats.coasting)
        putDouble(MOST_TURNS, stats.mostTurns)
        putDouble(TOP_RPM, stats.topRpm)
        putDouble(LONGEST_COAST, stats.longestCoast)
        putInt(DAYS_SPUN, stats.daysSpun)
        putInt(LONGEST_STREAK, stats.longestStreak)
        putInt(BUSIEST_DAY, stats.busiestDay)
        putLong(LAST_DAY, stats.lastDay)
        putInt(LAST_DAY_SPINS, stats.lastDaySpins)
        putInt(STREAK, stats.streak)
    }

    // Preferences hold no doubles, and a float total would start rounding off each spin's fraction of a turn as it grows.
    private fun SharedPreferences.getDouble(key: String) = Double.fromBits(getLong(key, 0.0.toRawBits()))

    private fun SharedPreferences.Editor.putDouble(key: String, value: Double) = putLong(key, value.toRawBits())

    private companion object {
        const val SPINS = "spins"
        const val CLOCKWISE = "clockwise"
        const val ANTICLOCKWISE = "anticlockwise"
        const val MOST_TURNS = "most_turns"
        const val TOP_RPM = "top_rpm"
        const val LONGEST_COAST = "longest_coast"
        const val DISTANCE = "distance"
        const val COASTING = "coasting"
        const val DAYS_SPUN = "days_spun"
        const val LONGEST_STREAK = "longest_streak"
        const val BUSIEST_DAY = "busiest_day"
        const val LAST_DAY = "last_day"
        const val LAST_DAY_SPINS = "last_day_spins"
        const val STREAK = "streak"
    }
}
