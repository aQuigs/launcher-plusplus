package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.HomeApps
import com.sqftware.orbitlauncher.domain.decodeFavourites
import com.sqftware.orbitlauncher.domain.decodeRing
import com.sqftware.orbitlauncher.domain.encode

interface HomeAppsStore {
    fun load(): HomeApps

    fun save(homeApps: HomeApps)
}

class SharedPreferencesHomeAppsStore(context: Context) : HomeAppsStore {
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    // One string per place rather than a string set: a set does not keep the order.
    override fun load() = HomeApps(ring = decodeRing(read(RING_KEY)), dock = decodeFavourites(read(DOCK_KEY)))

    override fun save(homeApps: HomeApps) {
        prefs.edit {
            putString(RING_KEY, homeApps.ring.encode())
            putString(DOCK_KEY, homeApps.dock.encode())
        }
    }

    private fun read(key: String) = prefs.getString(key, null).orEmpty()

    private companion object {
        // The ring's key predates the dock; keeping its name keeps the rings already stored.
        const val RING_KEY = "favourites"
        const val DOCK_KEY = "dock"
    }
}
