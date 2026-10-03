package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.RingPages
import com.sqftware.orbitlauncher.domain.decodeRing
import com.sqftware.orbitlauncher.domain.encode

interface RingPagesStore {
    fun load(): RingPages

    fun save(pages: RingPages)
}

class SharedPreferencesRingPagesStore(context: Context) : RingPagesStore {
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    // One string per ring rather than a string set: a set does not keep the order.
    override fun load(): RingPages {
        val rings = prefs.all.keys.mapNotNull { key -> pageOf(key)?.let { it to decodeRing(read(key)) } }.toMap()
        return RingPages(rings = rings, dock = decodeRing(read(DOCK_KEY)))
    }

    override fun save(pages: RingPages) {
        prefs.edit {
            prefs.all.keys.filter { key -> pageOf(key)?.let { it !in pages.rings } == true }.forEach { remove(it) }
            pages.rings.forEach { (page, ring) -> putString(keyOf(page), ring.encode()) }
            putString(DOCK_KEY, pages.dock.encode())
        }
    }

    private fun read(key: String) = prefs.getString(key, null).orEmpty()

    private companion object {
        // The home ring's key predates the dock and the other pages; keeping its name keeps the rings already stored.
        const val HOME_KEY = "favourites"
        const val DOCK_KEY = "dock"
        const val PAGE_PREFIX = "ring:"

        fun keyOf(page: String) = if (page == LauncherPage.Home.id) HOME_KEY else PAGE_PREFIX + page

        fun pageOf(key: String) = if (key == HOME_KEY) LauncherPage.Home.id else key.removePrefix(PAGE_PREFIX).takeIf { key.startsWith(PAGE_PREFIX) }
    }
}
