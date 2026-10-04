package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit

interface AppNamesStore {
    /** Whether the ring names its apps under their icons, as it does not until the user asks. */
    fun load(): Boolean

    fun save(on: Boolean)
}

class SharedPreferencesAppNamesStore(context: Context) : AppNamesStore {
    private val prefs = context.getSharedPreferences("names", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, false)

    override fun save(on: Boolean) = prefs.edit { putBoolean(KEY, on) }

    private companion object {
        const val KEY = "on_ring"
    }
}
