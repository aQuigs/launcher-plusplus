package com.aquigs.launcherplusplus.domain

/**
 * Where every label that does not start with a Latin letter (digits, symbols, other scripts) is filed. It sorts before
 * 'A', so its section comes first.
 */
const val OTHER_INITIAL = '#'

/** Apps filed under one rail letter, in the order they were given. */
data class AppSection(val initial: Char, val apps: List<AppEntry>)

/** The rail letter for this app: its first visible letter without accents (É under E), or [OTHER_INITIAL]. */
val AppEntry.initial: Char
    get() {
        val first = label.firstOrNull { !it.isWhitespace() && Character.getType(it) != Character.FORMAT.toInt() }
            ?: return OTHER_INITIAL
        val base = first.toString().unaccented().first().uppercaseChar()
        return if (base in 'A'..'Z') base else OTHER_INITIAL
    }

/** Files the apps under their initials: the '#' section first, then A to Z; apps keep their relative order. */
fun List<AppEntry>.sectionsByInitial(): List<AppSection> =
    groupBy { it.initial }
        .map { (initial, apps) -> AppSection(initial, apps) }
        .sortedBy { it.initial }
