package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.CardLook
import com.sqftware.orbitlauncher.domain.CollectionPages
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.decodeCardLook
import com.sqftware.orbitlauncher.domain.decodeCollectionsPage
import com.sqftware.orbitlauncher.domain.encode

interface CollectionsStore {
    fun load(): CollectionPages

    fun save(pages: CollectionPages)
}

class SharedPreferencesCollectionsStore(context: Context) : CollectionsStore {
    private val prefs = context.getSharedPreferences("collections", Context.MODE_PRIVATE)

    // A page with nothing stored yet is the default page; an empty string is a page the user emptied, which must stay empty.
    override fun load(): CollectionPages {
        val defaults = prefs.getString(DEFAULTS_KEY, null)?.let(::decodeCardLook) ?: CardLook()
        val pages = prefs.all.keys.mapNotNull { key -> pageOf(key)?.let { it to decodeCollectionsPage(prefs.getString(key, null).orEmpty()) } }
        return CollectionPages(pages.toMap(), defaults)
    }

    override fun save(pages: CollectionPages) = prefs.edit {
        prefs.all.keys.filter { key -> pageOf(key)?.let { it !in pages.pages } == true }.forEach { remove(it) }
        pages.pages.forEach { (page, cards) -> putString(keyOf(page), cards.encode()) }
        putString(DEFAULTS_KEY, pages.defaults.encode())
    }

    private companion object {
        // The first page's key predates the others; keeping its name keeps the cards already stored.
        const val PAGE_KEY = "cards"
        const val PAGE_PREFIX = "cards:"
        const val DEFAULTS_KEY = "defaults"

        fun keyOf(page: String) = if (page == LauncherPage.Collections.id) PAGE_KEY else PAGE_PREFIX + page

        fun pageOf(key: String) =
            if (key == PAGE_KEY) LauncherPage.Collections.id else key.removePrefix(PAGE_PREFIX).takeIf { key.startsWith(PAGE_PREFIX) }
    }
}
