package com.sqftware.orbitlauncher.domain

/**
 * The pair of [first] and [second], opened in split screen with [first] on top and named after both, as Pixel names its
 * pairs; null unless they are plain apps of two packages. Two activities of one package share its task, so the second
 * would take the first one's place rather than open beside it.
 */
fun pairOf(first: AppEntry, second: AppEntry): AppEntry? {
    if (!first.isApp || !second.isApp || first.packageName == second.packageName) return null
    return AppEntry(
        label = "${first.label} | ${second.label}",
        packageName = "",
        activityName = "",
        canUninstall = false,
        kind = EntryKind.AppPair(first, second),
    )
}
