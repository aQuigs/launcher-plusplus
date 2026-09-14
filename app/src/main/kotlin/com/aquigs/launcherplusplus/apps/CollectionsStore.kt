package com.aquigs.launcherplusplus.apps

import android.content.Context
import androidx.core.content.edit
import com.aquigs.launcherplusplus.domain.CollectionsPage
import com.aquigs.launcherplusplus.domain.decodeCollectionsPage
import com.aquigs.launcherplusplus.domain.encode

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
