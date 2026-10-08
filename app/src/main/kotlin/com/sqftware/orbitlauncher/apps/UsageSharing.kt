package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics

interface UsageSharing {
    /** Whether the launcher sends usage data, such as sessions and the device model, on until the user turns it off. */
    fun load(): Boolean

    fun save(share: Boolean)
}

/**
 * Firebase's automatic events only; the launcher logs none of its own. Firebase keeps the choice too, from the next
 * process start, before any activity reads it. Debug builds never send any (debug manifest).
 */
class FirebaseUsageSharing(context: Context) : UsageSharing {
    private val prefs = context.getSharedPreferences("analytics", Context.MODE_PRIVATE)

    override fun load() = prefs.getBoolean(KEY, true)

    override fun save(share: Boolean) {
        prefs.edit { putBoolean(KEY, share) }
        Firebase.analytics.setAnalyticsCollectionEnabled(share)
    }

    private companion object {
        const val KEY = "share"
    }
}
