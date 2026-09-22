package com.aquigs.launcherplusplus.apps

import android.content.Context
import androidx.core.content.edit

interface HourStyleStore {
    /** Whether the user chose 24 hours for the home clock, or null while they have not chosen and it follows the system. */
    fun load(): Boolean?

    fun save(twentyFourHour: Boolean)
}

class SharedPreferencesHourStyleStore(context: Context) : HourStyleStore {
    private val prefs = context.getSharedPreferences("clock", Context.MODE_PRIVATE)

    override fun load() = if (prefs.contains(KEY)) prefs.getBoolean(KEY, false) else null

    override fun save(twentyFourHour: Boolean) = prefs.edit { putBoolean(KEY, twentyFourHour) }

    private companion object {
        const val KEY = "twenty_four_hour"
    }
}
