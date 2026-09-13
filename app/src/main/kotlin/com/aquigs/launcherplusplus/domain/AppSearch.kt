package com.aquigs.launcherplusplus.domain

import java.text.Normalizer

/**
 * The apps whose label contains [query], ignoring case and accents: those with a word starting with it first, the
 * earlier the word the better, then the rest in the order given. A blank query matches every app.
 */
fun List<AppEntry>.matching(query: String): List<AppEntry> {
    val wanted = query.trim().unaccented().lowercase()
    if (wanted.isEmpty()) return this
    val hits = filter { wanted in it.searchableLabel }
    val (wordStarts, elsewhere) = hits.partition { it.searchableLabel.hasWordStarting(wanted) }
    return wordStarts.sortedBy { it.searchableLabel.indexOf(wanted) } + elsewhere
}

/** Without accents: decomposition splits each accent into its own mark, which is then dropped, so É becomes E. */
internal fun String.unaccented(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }

private fun String.hasWordStarting(prefix: String): Boolean {
    var at = indexOf(prefix)
    while (at >= 0) {
        if (at == 0 || !this[at - 1].isLetterOrDigit()) return true
        at = indexOf(prefix, at + 1)
    }
    return false
}
