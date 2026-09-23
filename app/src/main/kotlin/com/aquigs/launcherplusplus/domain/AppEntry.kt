package com.aquigs.launcherplusplus.domain

/**
 * One launchable activity, or a shortcut an app pinned to the home screen, which has a [shortcutId] and belongs to its
 * app's [activityName]. [canUninstall] is false for an app built into the system. [installedAt] is when its package was
 * first installed, in epoch milliseconds, and [category] the kind of app its package says it is, if it says.
 */
data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val canUninstall: Boolean = true,
    val installedAt: Long = 0L,
    val category: AppCategory? = null,
    val shortcutId: String? = null,
) {
    /** Identifies one activity or shortcut whatever its label, so it survives relabelling and reloads of the app list. */
    val key: String = if (shortcutId == null) "$packageName/$activityName" else shortcutKey(packageName, shortcutId)

    /** The label as a search sees it: lower case, without accents. Made once here rather than on every keystroke. */
    val searchableLabel: String = label.unaccented().lowercase()
}

/**
 * The key of [packageName]'s shortcut [id]. Keys are stored, so it must never be an activity's: a package name holds
 * neither '/' nor '#', so the first of them in a key ends the package name, and says which kind of key it is.
 */
fun shortcutKey(packageName: String, id: String) = "$packageName#$id"

fun List<AppEntry>.sortedByLabel(): List<AppEntry> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppEntry::label).thenBy(AppEntry::packageName))
