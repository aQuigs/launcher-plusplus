package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit

interface PickTipStore {
    /** Whether a tap on the ring's centre still tells the user that a long press there picks the ring's apps. */
    fun load(): Boolean

    fun save(on: Boolean)
}

class SharedPreferencesPickTipStore(context: Context) : PickTipStore {
    private val prefs = context.getSharedPreferences("tips", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, true)

    override fun save(on: Boolean) = prefs.edit { putBoolean(KEY, on) }

    private companion object {
        const val KEY = "pick"
    }
}
