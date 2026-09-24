package com.sqftware.orbitlauncher.apps

import android.app.StatusBarManager
import android.content.Context
import android.util.Log

interface NotificationShade {
    /** Pulls down the system's notification shade, as a swipe down from the status bar would. */
    fun open()
}

private const val TAG = "StatusBarNotificationShade"

/**
 * The shade pulled down by the status bar's own expandNotificationsPanel, as launchers have long done. The SDK hides
 * it, so a build may lack or block it, but the system lets apps holding the normal EXPAND_STATUS_BAR permission call it.
 */
class StatusBarNotificationShade(private val context: Context) : NotificationShade {
    override fun open() {
        val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
        try {
            StatusBarManager::class.java.getMethod("expandNotificationsPanel").invoke(statusBar)
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "Cannot open the notification shade", e)
        }
    }
}
