package com.aquigs.launcherplusplus.domain

import java.text.Normalizer

/**
 * The apps whose label contains [query], ignoring case and accents: those with a word starting with it first, then the
 * rest, each group in the order given. A blank query matches every app.
 */
fun List<AppEntry>.matching(query: String): List<AppEntry> {
    val wanted = query.trim().searchable()
    if (wanted.isEmpty()) return this
    val hits = mapNotNull { app -> app.label.searchable().takeIf { wanted in it }?.let { app to it } }
    val (wordStarts, elsewhere) = hits.partition { (_, label) -> label.hasWordStarting(wanted) }
    return (wordStarts + elsewhere).map { (app, _) -> app }
}

// Lower-cased with the accents split off and dropped, so "eclair" finds Éclair.
private fun String.searchable(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        .lowercase()

private fun String.hasWordStarting(prefix: String): Boolean {
    var at = indexOf(prefix)
    while (at >= 0) {
        if (at == 0 || !this[at - 1].isLetterOrDigit()) return true
        at = indexOf(prefix, at + 1)
    }
    return false
}
