package com.aquigs.launcherplusplus.domain

/** One launchable activity. [canUninstall] is false for an app built into the system. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val canUninstall: Boolean = true,
) {
    /** Identifies one launchable activity whatever its label, so it survives relabelling and reloads of the app list. */
    val key: String = "$packageName/$activityName"
}

fun List<AppEntry>.sortedByLabel(): List<AppEntry> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppEntry::label).thenBy(AppEntry::packageName))
