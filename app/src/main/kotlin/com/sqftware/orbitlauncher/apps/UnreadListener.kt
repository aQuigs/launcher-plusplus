package com.sqftware.orbitlauncher.apps

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sqftware.orbitlauncher.domain.PostedNotification
import com.sqftware.orbitlauncher.domain.RemovalCause
import com.sqftware.orbitlauncher.domain.Removals
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.unreadCounts
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Reads the notifications the user lets the launcher see and publishes their unread counts. The system binds it in the
 * launcher's process while access is granted, so [counts] is process-wide state the adapter hands on; it is empty
 * whenever the listener is not connected. The ones the user dismisses unread go to [KeptUnread].
 */
class UnreadListener : NotificationListenerService() {
    private val kept by lazy { KeptUnread(this) }
    private val removals = Removals()

    override fun onListenerConnected() = publish()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            removals.posted(sbn.groupKey)
            kept.update { it.posted(sbn.key) }
        }
        publish()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        if (sbn != null) {
            val posted = sbn.asPosted()
            val removal = removals.removed(sbn.groupKey, posted.isGroupSummary, cause(reason))
            kept.update { it.afterRemoval(sbn.key, posted, removal) }
        }
        publish()
    }

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

    private fun cause(reason: Int) = when (reason) {
        REASON_CANCEL, REASON_CANCEL_ALL -> RemovalCause.UserDismissed
        REASON_GROUP_SUMMARY_CANCELED -> RemovalCause.SummaryRemoved
        REASON_CLICK -> RemovalCause.Tapped
        else -> RemovalCause.Other
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
