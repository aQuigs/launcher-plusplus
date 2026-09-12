package com.aquigs.launcherplusplus.domain

/** Where every label that does not start with a Latin letter (digits, symbols, other scripts) is filed. */
const val OTHER_INITIAL = '#'

/** Apps filed under one rail letter, in the order they were given. */
data class AppSection(val initial: Char, val apps: List<AppEntry>)

val AppEntry.initial: Char
    get() = label.trimStart().firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: OTHER_INITIAL

/** Files the apps under their initials: the '#' section first, then A to Z; apps keep their relative order. */
fun List<AppEntry>.sectionsByInitial(): List<AppSection> =
    groupBy { it.initial }
        .map { (initial, apps) -> AppSection(initial, apps) }
        .sortedBy { if (it.initial == OTHER_INITIAL) ' ' else it.initial }
