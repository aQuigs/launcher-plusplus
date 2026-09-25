package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.ReorderMode

interface ReorderModeStore {
    /** How a moved app last landed, Insert until the user picks. */
    fun load(): ReorderMode

    fun save(mode: ReorderMode)
}

class SharedPreferencesReorderModeStore(context: Context) : ReorderModeStore {
    private val prefs = context.getSharedPreferences("reorder", Context.MODE_PRIVATE)

    override fun load() = ReorderMode.entries.find { it.name == prefs.getString(KEY, null) } ?: ReorderMode.Insert

    override fun save(mode: ReorderMode) = prefs.edit { putString(KEY, mode.name) }

    private companion object {
        const val KEY = "mode"
    }
}
