package com.sqftware.orbitlauncher.domain

/**
 * [raw] as a name the user gave something: every run of whitespace (a tab or line break, which would split the stored
 * text, or a no-break space) made one space, invisible formatting and control characters dropped, trimmed, and cut to
 * [max] characters.
 */
fun cleanName(raw: String, max: Int): String {
    val spaced = buildString {
        raw.forEach { c ->
            when {
                c.isWhitespace() -> append(' ')
                c.category != CharCategory.FORMAT && c.category != CharCategory.CONTROL -> append(c)
            }
        }
    }
    val cut = spaced.split(' ').filter(String::isNotEmpty).joinToString(" ").take(max)
    return (if (cut.lastOrNull()?.isHighSurrogate() == true) cut.dropLast(1) else cut).trimEnd()
}
