package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit

interface RingSpinStore {
    /** Whether a quick drag round the ring spins it, as it does until the user turns it off. */
    fun load(): Boolean

    fun save(on: Boolean)
}

class SharedPreferencesRingSpinStore(context: Context) : RingSpinStore {
    private val prefs = context.getSharedPreferences("spin", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, true)

    override fun save(on: Boolean) = prefs.edit { putBoolean(KEY, on) }

    private companion object {
        const val KEY = "ring"
    }
}
