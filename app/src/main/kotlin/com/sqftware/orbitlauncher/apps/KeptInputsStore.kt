package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.InputValue
import com.sqftware.orbitlauncher.domain.KeptInputs
import com.sqftware.orbitlauncher.domain.LinkApp
import com.sqftware.orbitlauncher.domain.LinkValue
import com.sqftware.orbitlauncher.domain.PlaceValue

interface KeptInputsStore<T : InputValue> {
    fun load(): KeptInputs<T>

    fun save(kept: KeptInputs<T>)
}

/** The places kept for the prompt that asks for one. */
fun placesStore(context: Context): KeptInputsStore<PlaceValue> =
    SharedPreferencesKeptInputsStore(context, "places", InputKind.Place::decode)

/** The chats or notes kept for the prompt that asks for one of [app]'s. */
fun linksStore(context: Context, app: LinkApp): KeptInputsStore<LinkValue> =
    SharedPreferencesKeptInputsStore(context, "chats", app::decode, keyPrefix = "${app.name}_")

/** [KeptInputsStore] in the preferences file [name], under keys that start with [keyPrefix], reading each value back with [decode]. */
private class SharedPreferencesKeptInputsStore<T : InputValue>(
    context: Context,
    name: String,
    private val decode: (String) -> T?,
    private val keyPrefix: String = "",
) : KeptInputsStore<T> {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun load() = KeptInputs(favorites = read(FAVORITES), recent = read(RECENT))

    override fun save(kept: KeptInputs<T>) = prefs.edit {
        putString(keyPrefix + FAVORITES, kept.favorites.text())
        putString(keyPrefix + RECENT, kept.recent.text())
    }

    // One value to a line, as an encoded value is one line.
    private fun read(key: String) = prefs.getString(keyPrefix + key, null).orEmpty().lines().filter(String::isNotEmpty).mapNotNull(decode)

    private fun List<T>.text() = joinToString("\n") { it.encode() }

    private companion object {
        const val FAVORITES = "favorites"
        const val RECENT = "recent"
    }
}
