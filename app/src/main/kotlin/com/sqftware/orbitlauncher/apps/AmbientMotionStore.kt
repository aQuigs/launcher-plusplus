package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit

interface AmbientMotionStore {
    /** Whether the planets and the emblem's sky turn on their own, as they do until the user stops them. */
    fun load(): Boolean

    fun save(on: Boolean)
}

class SharedPreferencesAmbientMotionStore(context: Context) : AmbientMotionStore {
    private val prefs = context.getSharedPreferences("motion", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, true)

    override fun save(on: Boolean) = prefs.edit { putBoolean(KEY, on) }

    private companion object {
        const val KEY = "ambient"
    }
}
