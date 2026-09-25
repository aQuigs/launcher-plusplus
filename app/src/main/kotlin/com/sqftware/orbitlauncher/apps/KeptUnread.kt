package com.sqftware.orbitlauncher.apps

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.sqftware.orbitlauncher.domain.Kept
import com.sqftware.orbitlauncher.domain.KeptNotification
import com.sqftware.orbitlauncher.domain.UnreadCounts
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The notifications the user dismissed unread, one preferences entry per notification key, so they outlive the process.
 * [UnreadListener] and the launcher both change them, always on the main thread, which keeps [update] from racing.
 */
class KeptUnread(context: Context) {
    private val prefs = context.getSharedPreferences("kept_unread", Context.MODE_PRIVATE)

    fun load() = Kept(prefs.all.mapNotNull { (key, value) -> (value as? String)?.decode()?.let { key to it } }.toMap())

    fun update(change: (Kept) -> Kept) {
        val old = load()
        val new = change(old)
        if (new == old) return
        prefs.edit {
            clear()
            new.byKey.forEach { (key, kept) -> putString(key, "${kept.count} ${kept.packageName}") }
        }
    }

    /** The kept counts now and on each change, whichever side made it. */
    fun counts(): Flow<UnreadCounts> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(load().counts) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send(load().counts)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    private fun String.decode(): KeptNotification? {
        val count = substringBefore(' ').toIntOrNull() ?: return null
        return KeptNotification(packageName = substringAfter(' '), count = count)
    }
}
