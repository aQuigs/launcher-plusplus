package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit

interface WhereIAmStore {
    /** Whether the minimap shows where the user is, off until they turn it on. */
    fun load(): Boolean

    fun save(on: Boolean)
}

class SharedPreferencesWhereIAmStore(context: Context) : WhereIAmStore {
    private val prefs = context.getSharedPreferences("where_i_am", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, false)

    override fun save(on: Boolean) = prefs.edit { putBoolean(KEY, on) }

    private companion object {
        const val KEY = "show"
    }
}
