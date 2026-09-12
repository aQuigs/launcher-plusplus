package com.aquigs.launcherplusplus.domain

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
)

/** Identifies one launchable activity whatever its label, so it survives relabelling and reloads of the app list. */
val AppEntry.key: String
    get() = "$packageName/$activityName"

fun List<AppEntry>.sortedByLabel(): List<AppEntry> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppEntry::label).thenBy(AppEntry::packageName))
