package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.Colourway
import com.sqftware.orbitlauncher.domain.Colourways
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.FolderLooks
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.domain.ThemePicks

interface ThemePicksStore<T> {
    /** What was picked in each theme, none until the user picks. */
    fun load(): ThemePicks<T>

    fun save(picks: ThemePicks<T>)
}

/** Each theme's pick of [entries] in the preferences [file], under the key [keyOf] gives the theme. */
class SharedPreferencesThemePicksStore<T : Enum<T>>(
    context: Context,
    file: String,
    private val none: ThemePicks<T>,
    private val entries: List<T>,
    private val keyOf: (Theme) -> String,
) : ThemePicksStore<T> {
    private val prefs = context.getSharedPreferences(file, Context.MODE_PRIVATE)

    override fun load() = Theme.entries.fold(none) { picks, theme ->
        val pick = entries.find { it.name == prefs.getString(keyOf(theme), null) }
        if (pick == null) picks else picks.with(theme, pick)
    }

    // Only what was picked, so a theme still on its first follows that if a later release puts another first.
    override fun save(picks: ThemePicks<T>) = prefs.edit {
        Theme.entries.forEach { theme ->
            val pick = picks.picked(theme)
            if (pick == null) remove(keyOf(theme)) else putString(keyOf(theme), pick.name)
        }
    }
}

// Space's pick keeps the key it had before there were themes, so it survives the update.
fun folderLookStore(context: Context): ThemePicksStore<FolderLook> = SharedPreferencesThemePicksStore(context, "folders", FolderLooks(), FolderLook.entries) {
    if (it == Theme.Space) "look" else "look_${it.name}"
}

fun colourwayStore(context: Context): ThemePicksStore<Colourway> = SharedPreferencesThemePicksStore(context, "colourways", Colourways(), Colourway.entries) { it.name }
