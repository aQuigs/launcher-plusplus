package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.InputValue
import com.sqftware.orbitlauncher.domain.KeptInputs
import com.sqftware.orbitlauncher.domain.PlaceValue

interface KeptInputsStore<T : InputValue> {
    fun load(): KeptInputs<T>

    fun save(kept: KeptInputs<T>)
}

/** The places kept for the prompt that asks for one. */
fun placesStore(context: Context): KeptInputsStore<PlaceValue> =
    SharedPreferencesKeptInputsStore(context, "places") { InputKind.Place.decode(it) as? PlaceValue }

/** [KeptInputsStore] in the preferences file [name], reading each value back with [decode]. */
private class SharedPreferencesKeptInputsStore<T : InputValue>(
    context: Context,
    name: String,
    private val decode: (String) -> T?,
) : KeptInputsStore<T> {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun load() = KeptInputs(favorites = read(FAVORITES), recent = read(RECENT))

    override fun save(kept: KeptInputs<T>) = prefs.edit {
        putString(FAVORITES, kept.favorites.text())
        putString(RECENT, kept.recent.text())
    }

    // One value to a line, as an encoded value is one line.
    private fun read(key: String) = prefs.getString(key, null).orEmpty().lines().filter(String::isNotEmpty).mapNotNull(decode)

    private fun List<T>.text() = joinToString("\n") { it.encode() }

    private companion object {
        const val FAVORITES = "favorites"
        const val RECENT = "recent"
    }
}
