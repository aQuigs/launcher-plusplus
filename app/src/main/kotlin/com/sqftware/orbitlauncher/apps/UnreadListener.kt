package com.sqftware.orbitlauncher.apps

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sqftware.orbitlauncher.domain.PostedNotification
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.unreadCounts
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Reads the notifications the user lets the launcher see and publishes their unread counts. The system binds it in the
 * launcher's process while access is granted, so [counts] is process-wide state the adapter hands on; it is empty
 * whenever the listener is not connected.
 */
class UnreadListener : NotificationListenerService() {
    override fun onListenerConnected() = publish()

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    override fun onListenerDisconnected() {
        counts.value = UnreadCounts()
    }

    private fun publish() {
        // Null, or a refusal, when the listener has lost its connection with a callback still on its way.
        val active = try {
            activeNotifications
        } catch (_: SecurityException) {
            null
        }
        counts.value = unreadCounts(active.orEmpty().map { it.asPosted() })
    }

    private fun StatusBarNotification.asPosted() = PostedNotification(
        packageName = packageName,
        isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        isOngoing = isOngoing,
        number = notification.number,
    )

    companion object {
        val counts = MutableStateFlow(UnreadCounts())
    }
}
