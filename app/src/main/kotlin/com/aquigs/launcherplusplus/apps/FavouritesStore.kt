package com.aquigs.launcherplusplus.apps

import android.content.Context
import com.aquigs.launcherplusplus.domain.Favourites

interface FavouritesStore {
    fun load(): Favourites

    fun save(favourites: Favourites)
}

class SharedPreferencesFavouritesStore(context: Context) : FavouritesStore {
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    // One joined string rather than a string set: a set does not keep the ring order.
    override fun load() = Favourites(prefs.getString(KEY, null)?.split(SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty())

    override fun save(favourites: Favourites) {
        prefs.edit().putString(KEY, favourites.keys.joinToString(SEPARATOR)).apply()
    }

    private companion object {
        const val KEY = "favourites"
        const val SEPARATOR = "\n"
    }
}
