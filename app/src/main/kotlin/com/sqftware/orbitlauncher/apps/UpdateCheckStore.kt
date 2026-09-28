package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit

interface UpdateCheckStore {
    /** Whether the launcher looks for its own updates, on until the user turns it off. */
    fun load(): Boolean

    fun save(checkForUpdates: Boolean)
}

class SharedPreferencesUpdateCheckStore(context: Context) : UpdateCheckStore {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, true)

    override fun save(checkForUpdates: Boolean) = prefs.edit { putBoolean(KEY, checkForUpdates) }

    private companion object {
        const val KEY = "check"
    }
}
