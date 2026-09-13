package com.aquigs.launcherplusplus.apps

import android.content.Context
import androidx.core.content.edit
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.HomeApps

interface HomeAppsStore {
    fun load(): HomeApps

    fun save(homeApps: HomeApps)
}

class SharedPreferencesHomeAppsStore(context: Context) : HomeAppsStore {
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    override fun load() = HomeApps(ring = read(RING_KEY), dock = read(DOCK_KEY))

    override fun save(homeApps: HomeApps) {
        prefs.edit {
            putString(RING_KEY, homeApps.ring.keys.joinToString(SEPARATOR))
            putString(DOCK_KEY, homeApps.dock.keys.joinToString(SEPARATOR))
        }
    }

    // One joined string per place rather than a string set: a set does not keep the order.
    private fun read(key: String) = Favourites(prefs.getString(key, null).orEmpty().split(SEPARATOR).filter(String::isNotEmpty))

    private companion object {
        // The ring's key predates the dock; keeping its name keeps the rings already stored.
        const val RING_KEY = "favourites"
        const val DOCK_KEY = "dock"
        const val SEPARATOR = "\n"
    }
}
