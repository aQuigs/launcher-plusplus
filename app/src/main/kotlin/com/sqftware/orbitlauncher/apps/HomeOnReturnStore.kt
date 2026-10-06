package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit

interface HomeOnReturnStore {
    /** Whether the launcher comes back on the home page after the screen goes off or an app is opened, rather than on the page left. */
    fun load(): Boolean

    fun save(on: Boolean)
}

class SharedPreferencesHomeOnReturnStore(context: Context) : HomeOnReturnStore {
    private val prefs = context.getSharedPreferences("return", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, false)

    override fun save(on: Boolean) = prefs.edit { putBoolean(KEY, on) }

    private companion object {
        const val KEY = "home"
    }
}
