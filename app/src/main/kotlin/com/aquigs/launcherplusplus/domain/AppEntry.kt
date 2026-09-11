package com.aquigs.launcherplusplus.domain

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
)

fun List<AppEntry>.sortedByLabel(): List<AppEntry> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppEntry::label).thenBy(AppEntry::packageName))
