package com.aquigs.launcherplusplus.domain

internal const val LINE = "\n"
internal const val FIELD = "\t"

/** Favourites as text, one key per line, in order. */
fun Favourites.encode(): String = keys.joinToString(LINE)

fun decodeFavourites(text: String): Favourites = Favourites(text.nonEmptyLines())

/**
 * The ring as text, one line per slot: an app is its key, and a folder is a tab followed by its keys, tab-separated. No
 * key holds a tab, so a folder is any line with one, even a folder of one app or none, and a ring stored before folders
 * is all app lines and still reads.
 */
fun Ring.encode(): String = slots.joinToString(LINE) { slot ->
    when (slot) {
        is RingSlot.App -> slot.key
        is RingSlot.Folder -> slot.keys.joinToString(FIELD, prefix = FIELD)
    }
}

fun decodeRing(text: String): Ring = Ring(
    text.nonEmptyLines().map { line ->
        val fields = line.split(FIELD)
        // Whatever stands before a folder's first tab is skipped: folders once had a name there.
        if (fields.size == 1) RingSlot.App(line) else RingSlot.Folder(fields.drop(1).filter(String::isNotEmpty))
    },
)

/** Whether [key] can be stored: an activity's key never holds a line break or a tab, but a shortcut's id may. */
fun isStorable(key: String) = LINE !in key && FIELD !in key

internal fun String.nonEmptyLines() = split(LINE).filter(String::isNotEmpty)
