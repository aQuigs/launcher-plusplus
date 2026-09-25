package com.sqftware.orbitlauncher.apps

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.sqftware.orbitlauncher.domain.UnreadCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

interface Badges {
    /** Whether the user has granted the launcher notification access, which the badges need. */
    fun isEnabled(): Boolean

    /** [isEnabled] when collected: the access is granted in Settings, so collect afresh on each return to the front. */
    fun enabled(): Flow<Boolean>

    /** The unread counts now and as they change, empty while not [isEnabled]. Collect while the launcher is visible. */
    fun counts(): Flow<UnreadCounts>

    /** Clears the counts kept for [packageName]'s dismissed notifications, now that the user is opening it. */
    fun opened(packageName: String)

    /** Opens the system screen where the user grants or revokes the launcher's notification access. */
    fun openSettings()
}

private const val TAG = "NotificationBadges"

/**
 * Badges fed by [UnreadListener], which the system runs once the user allows it notification access, with the counts it
 * kept for the notifications the user dismissed unread.
 */
class NotificationBadges(private val context: Context) : Badges {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val listener = ComponentName(context, UnreadListener::class.java)
    private val kept = KeptUnread(context)

    override fun isEnabled(): Boolean = notificationManager.isNotificationListenerAccessGranted(listener)

    override fun enabled(): Flow<Boolean> = flow { emit(isEnabled()) }

    // Gated on the same read as [enabled], so a value the listener published before losing its access never shows, and
    // the badges and the menu's switch agree on each return to the front.
    override fun counts(): Flow<UnreadCounts> =
        combine(enabled(), UnreadListener.counts, kept.counts()) { on, live, dismissed -> if (on) live + dismissed else UnreadCounts() }

    override fun opened(packageName: String) = kept.update { it.opened(packageName) }

    override fun openSettings() = startOrLog(TAG, "the notification access settings") {
        val forThisListener = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(forThisListener)
        } catch (_: ActivityNotFoundException) {
            // Some builds only have the list of every listener, where the user finds the launcher by name.
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
