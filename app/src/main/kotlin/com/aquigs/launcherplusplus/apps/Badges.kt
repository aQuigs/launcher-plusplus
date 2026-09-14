package com.aquigs.launcherplusplus.apps

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.aquigs.launcherplusplus.domain.UnreadCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface Badges {
    /** Whether the user has granted the launcher notification access, which the badges need. */
    fun isEnabled(): Boolean

    /** [isEnabled] when collected: the access is granted in Settings, so collect afresh on each return to the front. */
    fun enabled(): Flow<Boolean>

    /** The unread counts now and as they change, empty while not [isEnabled]. Collect while the launcher is visible. */
    fun counts(): Flow<UnreadCounts>

    /** Opens the system screen where the user grants or revokes the launcher's notification access. */
    fun openSettings()
}

private const val TAG = "NotificationBadges"

/** Badges fed by [UnreadListener], which the system runs once the user allows it notification access. */
class NotificationBadges(private val context: Context) : Badges {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val listener = ComponentName(context, UnreadListener::class.java)

    override fun isEnabled(): Boolean = notificationManager.isNotificationListenerAccessGranted(listener)

    override fun enabled(): Flow<Boolean> = flow { emit(isEnabled()) }

    // Checked on each value rather than once: what the listener last published may outlive its access when the system
    // takes the access away without telling it.
    override fun counts(): Flow<UnreadCounts> = UnreadListener.counts.map { if (isEnabled()) it else UnreadCounts() }

    override fun openSettings() {
        val forThisListener = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(forThisListener)
        } catch (e: ActivityNotFoundException) {
            // Some builds only have the list of every listener, where the user finds the launcher by name.
            startOrLog(TAG, "the notification access settings") {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
