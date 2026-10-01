package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.Theme

interface ThemeStore {
    /** The launcher's theme, Space until the user picks another. */
    fun load(): Theme

    fun save(theme: Theme)
}

class SharedPreferencesThemeStore(context: Context) : ThemeStore {
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    override fun load() = Theme.entries.find { it.name == prefs.getString(KEY, null) } ?: Theme.Space

    override fun save(theme: Theme) = prefs.edit { putString(KEY, theme.name) }

    private companion object {
        const val KEY = "theme"
    }
}
