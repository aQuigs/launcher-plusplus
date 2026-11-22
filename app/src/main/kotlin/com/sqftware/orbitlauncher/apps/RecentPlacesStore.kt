package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.PlaceValue

interface RecentPlacesStore {
    /** The places last picked in the prompt that asks for one, latest first. */
    fun load(): List<PlaceValue>

    fun save(places: List<PlaceValue>)
}

class SharedPreferencesRecentPlacesStore(context: Context) : RecentPlacesStore {
    private val prefs = context.getSharedPreferences("places", Context.MODE_PRIVATE)

    // One place to a line, as an encoded place is one line.
    override fun load(): List<PlaceValue> =
        prefs.getString(KEY, null).orEmpty().lines()
            .filter(String::isNotEmpty)
            .mapNotNull { InputKind.Place.decode(it) as? PlaceValue }

    override fun save(places: List<PlaceValue>) = prefs.edit { putString(KEY, places.joinToString("\n") { it.encode() }) }

    private companion object {
        const val KEY = "recent"
    }
}
