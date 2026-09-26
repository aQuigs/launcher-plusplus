package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.FolderLook

interface FolderLookStore {
    /** How folders are drawn, the Solar system until the user picks. */
    fun load(): FolderLook

    fun save(look: FolderLook)
}

class SharedPreferencesFolderLookStore(context: Context) : FolderLookStore {
    private val prefs = context.getSharedPreferences("folders", Context.MODE_PRIVATE)

    override fun load() = FolderLook.entries.find { it.name == prefs.getString(KEY, null) } ?: FolderLook.SolarSystem

    override fun save(look: FolderLook) = prefs.edit { putString(KEY, look.name) }

    private companion object {
        const val KEY = "look"
    }
}
