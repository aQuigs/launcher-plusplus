package com.aquigs.launcherplusplus.apps

import android.content.Context
import androidx.core.content.edit
import com.aquigs.launcherplusplus.domain.Favourites

interface FavouritesStore {
    fun load(): Favourites

    fun save(favourites: Favourites)
}

class SharedPreferencesFavouritesStore(context: Context) : FavouritesStore {
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)

    // One joined string rather than a string set: a set does not keep the ring order.
    override fun load() = Favourites(prefs.getString(KEY, null).orEmpty().split(SEPARATOR).filter(String::isNotEmpty))

    override fun save(favourites: Favourites) {
        prefs.edit { putString(KEY, favourites.keys.joinToString(SEPARATOR)) }
    }

    private companion object {
        const val KEY = "favourites"
        const val SEPARATOR = "\n"
    }
}
