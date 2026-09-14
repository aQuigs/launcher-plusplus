package com.aquigs.launcherplusplus.domain

/**
 * One launchable activity. [canUninstall] is false for an app built into the system. [installedAt] is when its package
 * was first installed, in epoch milliseconds, and [category] the kind of app its package says it is, if it says.
 */
data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val canUninstall: Boolean = true,
    val installedAt: Long = 0L,
    val category: AppCategory? = null,
) {
    /** Identifies one launchable activity whatever its label, so it survives relabelling and reloads of the app list. */
    val key: String = "$packageName/$activityName"

    /** The label as a search sees it: lower case, without accents. Made once here rather than on every keystroke. */
    val searchableLabel: String = label.unaccented().lowercase()
}

fun List<AppEntry>.sortedByLabel(): List<AppEntry> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppEntry::label).thenBy(AppEntry::packageName))
