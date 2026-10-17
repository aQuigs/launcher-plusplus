package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.GridPages
import com.sqftware.orbitlauncher.domain.decodeGridPage
import com.sqftware.orbitlauncher.domain.encode

interface GridPagesStore {
    fun load(): GridPages

    fun save(pages: GridPages)
}

/** One string per page, keyed by its id; where each app sits is the widget host's to keep, with the page's widgets. */
class SharedPreferencesGridPagesStore(context: Context) : GridPagesStore {
    private val prefs = context.getSharedPreferences("grid", Context.MODE_PRIVATE)

    override fun load() = GridPages(prefs.all.keys.associateWith { decodeGridPage(prefs.getString(it, null).orEmpty()) })

    override fun save(pages: GridPages) = prefs.edit {
        prefs.all.keys.filter { it !in pages.pages }.forEach { remove(it) }
        pages.pages.forEach { (page, grid) -> putString(page, grid.encode()) }
    }
}
