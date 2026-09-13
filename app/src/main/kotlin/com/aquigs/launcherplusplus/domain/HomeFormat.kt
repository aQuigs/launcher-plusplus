package com.aquigs.launcherplusplus.domain

private const val LINE = "\n"
private const val FIELD = "\t"

/** Favourites as text, one key per line, in order. */
fun Favourites.encode(): String = keys.joinToString(LINE)

fun decodeFavourites(text: String): Favourites = Favourites(text.nonEmptyLines())

/**
 * The ring as text, one line per slot: an app is its key, and a folder is its name followed by its keys, tab-separated.
 * No key holds a tab, so a folder is any line with one, and a ring stored before folders is all app lines and still reads.
 * A name is written as [Ring.name] reads it, so it cannot break its line.
 */
fun Ring.encode(): String = slots.joinToString(LINE) { slot ->
    when (slot) {
        is RingSlot.App -> slot.key
        is RingSlot.Folder -> Ring.name(slot.name).orEmpty() + FIELD + slot.keys.joinToString(FIELD)
    }
}

fun decodeRing(text: String): Ring = Ring(
    text.nonEmptyLines().map { line ->
        val fields = line.split(FIELD)
        if (fields.size == 1) RingSlot.App(line) else RingSlot.Folder(fields.first(), fields.drop(1).filter(String::isNotEmpty))
    },
)

private fun String.nonEmptyLines() = split(LINE).filter(String::isNotEmpty)
