package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.WhereIAm

interface WhereIAmStore {
    /** Whether the minimap shows where the user is, shown until they turn it off. */
    fun load(): WhereIAm

    fun save(whereIAm: WhereIAm)
}

class SharedPreferencesWhereIAmStore(context: Context) : WhereIAmStore {
    private val prefs = context.getSharedPreferences("where_i_am", Context.MODE_PRIVATE)

    // Before a refusal was kept apart from turning it off, the choice was a flag.
    override fun load(): WhereIAm = prefs.getString(KEY, null)?.let { saved -> WhereIAm.entries.find { it.name == saved } }
        ?: if (prefs.getBoolean(FLAG_KEY, true)) WhereIAm.Shown else WhereIAm.Hidden

    override fun save(whereIAm: WhereIAm) = prefs.edit { putString(KEY, whereIAm.name) }

    private companion object {
        const val KEY = "where"
        const val FLAG_KEY = "show"
    }
}
