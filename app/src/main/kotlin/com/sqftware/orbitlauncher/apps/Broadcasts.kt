package com.sqftware.orbitlauncher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * A value once the receiver is registered, then one for each broadcast [filter] matches, for as long as it is collected.
 * The receiver is the application's: the system drops an activity's own as the activity is destroyed, which can come
 * before the collection is cancelled, and unregistering it then throws.
 */
internal fun Context.broadcasts(filter: IntentFilter): Flow<Unit> = callbackFlow {
    val app = applicationContext
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(Unit)
        }
    }
    ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    trySend(Unit)
    awaitClose { app.unregisterReceiver(receiver) }
}
