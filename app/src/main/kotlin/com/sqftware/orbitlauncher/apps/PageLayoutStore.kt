package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.PageLayout
import com.sqftware.orbitlauncher.domain.decodePageLayout
import com.sqftware.orbitlauncher.domain.encode

interface PageLayoutStore {
    fun load(): PageLayout

    fun save(layout: PageLayout)
}

class SharedPreferencesPageLayoutStore(context: Context) : PageLayoutStore {
    private val prefs = context.getSharedPreferences("pages", Context.MODE_PRIVATE)

    override fun load(): PageLayout = prefs.getString(LAYOUT_KEY, null)?.let(::decodePageLayout) ?: PageLayout()

    override fun save(layout: PageLayout) = prefs.edit { putString(LAYOUT_KEY, layout.encode()) }

    private companion object {
        const val LAYOUT_KEY = "layout"
    }
}
