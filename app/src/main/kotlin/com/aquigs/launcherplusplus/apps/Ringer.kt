package com.aquigs.launcherplusplus.apps

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.aquigs.launcherplusplus.domain.RingerMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

interface Ringer {
    /** The ringer's mode now. */
    fun mode(): RingerMode

    /** [mode] now, then again whenever the ringer changes, from here or elsewhere. Collect while the launcher is visible. */
    fun modes(): Flow<RingerMode>

    /**
     * Switches the ringer from the mode it is in now to the next; Silent turns Do Not Disturb on and leaving it turns it
     * off. Without the user's Do Not Disturb access it changes nothing and opens the screen that grants it.
     */
    fun cycle()
}

private const val TAG = "SystemRinger"

// Settings' screen for one app's access, which the SDK keeps to system apps, so a build may lack it or guard it; the
// list of every app has the launcher too.
private const val ACTION_ACCESS_DETAIL_SETTINGS = "android.settings.NOTIFICATION_POLICY_ACCESS_DETAIL_SETTINGS"

/** The system ringer, which the system links to Do Not Disturb. */
class SystemRinger(private val context: Context) : Ringer {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override fun mode(): RingerMode = when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingerMode.Silent
        AudioManager.RINGER_MODE_VIBRATE -> RingerMode.Vibrate
        else -> RingerMode.Normal
    }

    override fun modes(): Flow<RingerMode> =
        callbackFlow {
            val changes = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    trySend(Unit)
                }
            }
            val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
            ContextCompat.registerReceiver(context, changes, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            send(Unit)
            awaitClose { context.unregisterReceiver(changes) }
        }
            .conflate()
            .map { mode() }

    // Read at the tap, not from the label, which lags the system by a broadcast. Arc asks for the access before any
    // switch, even Normal to Vibrate, which the system would allow without it.
    override fun cycle() {
        if (!notificationManager.isNotificationPolicyAccessGranted) return openAccessSettings()
        try {
            audioManager.ringerMode = when (mode().next()) {
                RingerMode.Normal -> AudioManager.RINGER_MODE_NORMAL
                RingerMode.Vibrate -> AudioManager.RINGER_MODE_VIBRATE
                RingerMode.Silent -> AudioManager.RINGER_MODE_SILENT
            }
        } catch (e: SecurityException) {
            // The access went between the check and the switch.
            Log.w(TAG, "Cannot switch the ringer", e)
            openAccessSettings()
        }
    }

    private fun openAccessSettings() {
        var opened = false
        startOrLog(TAG, "the launcher's Do Not Disturb access") {
            context.startActivity(
                Intent(ACTION_ACCESS_DETAIL_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            opened = true
        }
        if (!opened) {
            startOrLog(TAG, "the Do Not Disturb access settings") {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
