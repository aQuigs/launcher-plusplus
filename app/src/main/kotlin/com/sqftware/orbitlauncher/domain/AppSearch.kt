package com.sqftware.orbitlauncher.domain

import java.text.Normalizer

/**
 * The apps whose label contains [query], ignoring case and accents: those with a word starting with it first, the
 * earlier the word the better, then the rest in the order given. A blank query matches every app.
 */
fun List<AppEntry>.matching(query: String): List<AppEntry> = matching(query, AppEntry::searchableLabel)

/** As [matching] for apps, for anything with a [label], as [searchable] makes it. */
fun <T> List<T>.matching(query: String, label: (T) -> String): List<T> {
    val wanted = query.trim().searchable()
    if (wanted.isEmpty()) return this
    val hits = filter { wanted in label(it) }
    val (wordStarts, elsewhere) = hits.partition { label(it).hasWordStarting(wanted) }
    return wordStarts.sortedBy { label(it).indexOf(wanted) } + elsewhere
}

/** As a search sees it: lower case, without accents. */
fun String.searchable(): String = unaccented().lowercase()

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
