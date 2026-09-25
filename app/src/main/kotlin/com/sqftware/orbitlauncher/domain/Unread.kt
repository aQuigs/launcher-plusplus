package com.sqftware.orbitlauncher.domain

/** One notification as the badges see it. [number] is the count the app put on it, or 0. */
data class PostedNotification(
    val packageName: String,
    val isGroupSummary: Boolean,
    val isOngoing: Boolean,
    val isMedia: Boolean,
    val number: Int,
)

/**
 * How many unread notifications each package has. A package with none is absent. A pinned shortcut counts none: its own
 * notifications are not told apart from the rest of its app's, which would overstate them.
 */
data class UnreadCounts(val byPackage: Map<String, Int> = emptyMap()) {
    operator fun get(app: AppEntry): Int = sum(listOf(app))

    /** The count for a folder of [apps]: each package counted once, however many of its activities are in there. */
    fun sum(apps: List<AppEntry>): Int = apps.filter { it.shortcutId == null }.map { it.packageName }.distinct().sumOf { byPackage[it] ?: 0 }

    operator fun plus(other: UnreadCounts) = UnreadCounts(
        (byPackage.keys + other.byPackage.keys).associateWith { (byPackage[it] ?: 0) + (other.byPackage[it] ?: 0) },
    )
}

/**
 * How many unread [this] notification stands for. A group's summary stands for members that are counted themselves, and
 * an ongoing notification (a call, a download) is not something to read, so both count none. Nor does a media player: it
 * shows as the player in Quick Settings rather than in the list, and stays once paused, when it is no longer ongoing. A
 * notification carrying a number, as a mail app's does for its unread threads, counts that many; any other counts one.
 */
private val PostedNotification.unread: Int get() = if (isGroupSummary || isOngoing || isMedia) 0 else maxOf(number, 1)

fun unreadCounts(notifications: List<PostedNotification>): UnreadCounts = UnreadCounts(
    notifications
        .filter { it.unread > 0 }
        .groupingBy { it.packageName }
        .fold(0) { total, notification -> total + notification.unread },
)

/** How the system says a notification left the shade. */
enum class RemovalCause {
    /** The user swiped it away or cleared the shade. */
    UserDismissed,

    /** Its group's summary went, whoever removed it. */
    SummaryRemoved,

    /** The user tapped it, which opens its app. */
    Tapped,

    /** Its app or the system took it back, or the user snoozed it. */
    Other,
}

/** Whether a notification that left the shade went unread, was opened, or was taken back. */
enum class Removal { Dismissed, Opened, Withdrawn }

/**
 * Tells what each removal means for the badges. A group's members leave as [RemovalCause.SummaryRemoved] whoever removed
 * the summary, an app that read the conversation included, and the summary's own removal comes first, so a member counts
 * as dismissed only once the user has dismissed its summary. A group posted to again starts afresh.
 */
class Removals {
    private val dismissedGroups = mutableSetOf<String>()

    fun posted(groupKey: String) {
        dismissedGroups -= groupKey
    }

    fun removed(groupKey: String, isGroupSummary: Boolean, cause: RemovalCause): Removal = when (cause) {
        RemovalCause.UserDismissed -> {
            if (isGroupSummary) dismissedGroups += groupKey
            Removal.Dismissed
        }
        RemovalCause.SummaryRemoved -> if (groupKey in dismissedGroups) Removal.Dismissed else Removal.Withdrawn
        RemovalCause.Tapped -> Removal.Opened
        RemovalCause.Other -> Removal.Withdrawn
    }
}

/** A notification the user dismissed unread: its app and how many unread it stood for. */
data class KeptNotification(val packageName: String, val count: Int)

/**
 * The notifications the user dismissed unread, by the system's key for each, which stay on their app's badge until the
 * app is opened, as Microsoft Launcher's do. Held by key, so a notification posted again replaces its kept count rather
 * than adding to it: a mail app's running total, or a conversation that comes back with the same messages in it.
 */
data class Kept(val byKey: Map<String, KeptNotification> = emptyMap()) {
    val counts: UnreadCounts
        get() = UnreadCounts(byKey.values.groupingBy { it.packageName }.fold(0) { total, kept -> total + kept.count })

    fun afterRemoval(key: String, notification: PostedNotification, removal: Removal): Kept = when {
        removal == Removal.Opened -> opened(notification.packageName)
        removal == Removal.Dismissed && notification.unread > 0 ->
            Kept(byKey + (key to KeptNotification(notification.packageName, notification.unread)))
        else -> this
    }

    fun posted(key: String) = Kept(byKey - key)

    fun opened(packageName: String) = Kept(byKey.filterValues { it.packageName != packageName })
}

/** What a badge says for [count]: the number, until it would need three digits. */
fun badgeText(count: Int): String = if (count > 99) "99+" else count.toString()
