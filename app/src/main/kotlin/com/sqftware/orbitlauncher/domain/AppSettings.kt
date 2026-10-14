package com.sqftware.orbitlauncher.domain

/**
 * What the user set for single apps, by package name: the apps whose unread badge is off, and those left off the New Apps
 * and Most Used cards. By package, as badges and those cards are: every activity of an app goes with it.
 */
data class AppSettings(val badgeOff: Set<String> = emptySet(), val offBuiltInCards: Set<String> = emptySet()) {
    fun isBadgeOff(app: AppEntry): Boolean = app.packageKey in badgeOff

    fun isOffBuiltInCards(app: AppEntry): Boolean = app.packageKey in offBuiltInCards

    fun toggleBadge(app: AppEntry): AppSettings = copy(badgeOff = badgeOff.toggle(app.packageKey))

    fun toggleBuiltInCards(app: AppEntry): AppSettings = copy(offBuiltInCards = offBuiltInCards.toggle(app.packageKey))

    /** [unread] without the counts of the apps whose badge is off. */
    fun badges(unread: UnreadCounts): UnreadCounts = unread - badgeOff
}

private fun Set<String>.toggle(item: String) = if (item in this) this - item else this + item
