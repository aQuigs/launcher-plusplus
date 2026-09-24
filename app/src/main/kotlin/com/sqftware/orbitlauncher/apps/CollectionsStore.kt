package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.CollectionsPage
import com.sqftware.orbitlauncher.domain.decodeCollectionsPage
import com.sqftware.orbitlauncher.domain.encode

interface CollectionsStore {
    fun load(): CollectionsPage

    fun save(page: CollectionsPage)
}

class SharedPreferencesCollectionsStore(context: Context) : CollectionsStore {
    private val prefs = context.getSharedPreferences("collections", Context.MODE_PRIVATE)

    // Nothing stored yet is the default page; an empty string is a page the user emptied, which must stay empty.
    override fun load() = prefs.getString(PAGE_KEY, null)?.let(::decodeCollectionsPage) ?: CollectionsPage()

    override fun save(page: CollectionsPage) = prefs.edit { putString(PAGE_KEY, page.encode()) }

    private companion object {
        const val PAGE_KEY = "cards"
    }
}
