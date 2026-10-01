package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.FolderLooks
import com.sqftware.orbitlauncher.domain.Theme

interface FolderLookStore {
    /** The folder look picked in each theme, none until the user picks. */
    fun load(): FolderLooks

    fun save(looks: FolderLooks)
}

class SharedPreferencesFolderLookStore(context: Context) : FolderLookStore {
    private val prefs = context.getSharedPreferences("folders", Context.MODE_PRIVATE)

    override fun load() = Theme.entries.fold(FolderLooks()) { looks, theme ->
        val look = FolderLook.entries.find { it.name == prefs.getString(keyOf(theme), null) }
        if (look == null) looks else looks.with(theme, look)
    }

    override fun save(looks: FolderLooks) = prefs.edit { Theme.entries.forEach { putString(keyOf(it), looks.of(it).name) } }

    private companion object {
        // Space's pick keeps the key it had before there were themes, so it survives the update.
        fun keyOf(theme: Theme) = if (theme == Theme.Space) "look" else "look_${theme.name}"
    }
}
