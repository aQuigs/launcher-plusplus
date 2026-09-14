package com.aquigs.launcherplusplus.domain

/** One notification as the badges see it. [number] is the count the app put on it, or 0. */
data class PostedNotification(val packageName: String, val isGroupSummary: Boolean, val isOngoing: Boolean, val number: Int)

/** How many unread notifications each package has. A package with none is absent. */
data class UnreadCounts(val byPackage: Map<String, Int> = emptyMap()) {
    operator fun get(app: AppEntry): Int = byPackage[app.packageName] ?: 0

    /** The count for a folder of [apps]: each package counted once, however many of its activities are in there. */
    fun sum(apps: List<AppEntry>): Int = apps.map { it.packageName }.distinct().sumOf { byPackage[it] ?: 0 }
}

/**
 * The unread counts in [notifications]. A group's summary stands for members that are counted themselves, and an
 * ongoing notification (a call, a download, a playing track) is not something to read, so both are skipped. A
 * notification carrying a number, as a mail app's does for its unread threads, counts that many; any other counts one.
 */
fun unreadCounts(notifications: List<PostedNotification>): UnreadCounts = UnreadCounts(
    notifications
        .filterNot { it.isGroupSummary || it.isOngoing }
        .groupingBy { it.packageName }
        .fold(0) { total, notification -> total + maxOf(notification.number, 1) },
)

/** What a badge says for [count]: the number, until it would need three digits. */
fun badgeText(count: Int): String = if (count > 99) "99+" else count.toString()
