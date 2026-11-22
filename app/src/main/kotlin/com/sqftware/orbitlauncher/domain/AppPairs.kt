package com.sqftware.orbitlauncher.domain

/** Whether [first] and [second] make a pair, as [pairOf] says. */
fun canPair(first: AppEntry, second: AppEntry): Boolean = first.isPairable && second.isPairable && first.packageName != second.packageName

/**
 * Whether it can be one of a pair: an app of the user's own, plain or opened to an intent, as the launcher opens a work
 * app only through the system.
 */
val AppEntry.isPairable: Boolean get() = ownApp != null && !isWork

/**
 * The pair of [first] and [second], opened in split screen with [first] on top and named after both, as Pixel names its
 * pairs; null unless they are [pairable][isPairable] apps of two packages. Two activities of one package share its
 * task, so the second would take the first one's place rather than open beside it.
 */
fun pairOf(first: AppEntry, second: AppEntry): AppEntry? {
    if (!canPair(first, second)) return null
    return AppEntry(
        label = "${first.label} | ${second.label}",
        packageName = "",
        activityName = "",
        canUninstall = false,
        kind = EntryKind.AppPair(first, second),
    )
}

/**
 * [this] with its app [member] opened as [replacement] instead: a pair of the replacement and the other app, else the
 * replacement itself; null for a pair that would not make one.
 */
fun AppEntry.withMember(member: AppEntry, replacement: AppEntry): AppEntry? = when (val kind = kind) {
    is EntryKind.AppPair -> when (member) {
        kind.first -> pairOf(replacement, kind.second)
        kind.second -> pairOf(kind.first, replacement)
        else -> null
    }
    else -> replacement
}

/** This pair the other way round, so the app below opens on top, or null if it is not a pair. */
fun AppEntry.flipped(): AppEntry? = (kind as? EntryKind.AppPair)?.let { pairOf(it.second, it.first) }
