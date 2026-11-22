package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.PlaceValue
import java.net.URLDecoder
import java.net.URLEncoder

interface RecentPlacesStore {
    /** The places last picked in the prompt that asks for one, latest first. */
    fun load(): List<PlaceValue>

    fun save(places: List<PlaceValue>)
}

class SharedPreferencesRecentPlacesStore(context: Context) : RecentPlacesStore {
    private val prefs = context.getSharedPreferences("places", Context.MODE_PRIVATE)

    // Each place is URL-encoded, so the line breaks between them are not in one.
    override fun load(): List<PlaceValue> =
        prefs.getString(KEY, null).orEmpty().lines()
            .filter(String::isNotEmpty)
            .mapNotNull { InputKind.Place.decode(URLDecoder.decode(it, "UTF-8")) as? PlaceValue }

    override fun save(places: List<PlaceValue>) = prefs.edit {
        putString(KEY, places.joinToString("\n") { URLEncoder.encode(it.encode(), "UTF-8") })
    }

    private companion object {
        const val KEY = "recent"
    }
}
