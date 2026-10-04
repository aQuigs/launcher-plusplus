package com.sqftware.orbitlauncher.domain

internal const val LINE = "\n"
internal const val FIELD = "\t"

/** Parts a folder's pick from its name: a control character, which [cleanName] keeps out of every name. */
private const val NAME = "\u001F"

/**
 * The ring or the dock as text, one line per slot: an app is its key, and a folder is a tab followed by its keys,
 * tab-separated. No key holds a tab, so a folder is any line with one, even a folder of one app or none, and a place
 * stored before it had folders is all app lines and still reads. A folder whose planet was picked has the pick before
 * its first tab, and a named one its name after the pick and a [NAME] mark, which no cleaned name holds.
 */
fun Ring.encode(): String = slots.joinToString(LINE) { slot ->
    when (slot) {
        is RingSlot.App -> slot.key
        is RingSlot.Folder -> {
            val head = slot.pick.encode() + if (slot.name.isEmpty()) "" else NAME + slot.name
            slot.keys.joinToString(FIELD, prefix = head + FIELD)
        }
    }
}

fun decodeRing(text: String): Ring = Ring(
    text.nonEmptyLines().map { line ->
        val fields = line.split(FIELD)
        // Folders once had a name with no mark before their first tab, which reads as no pick unless it names one.
        if (fields.size == 1) {
            RingSlot.App(line)
        } else {
            val head = fields[0].split(NAME, limit = 2)
            RingSlot.Folder(fields.drop(1).filter(String::isNotEmpty), decodePick(head[0]), head.getOrElse(1) { "" })
        }
    },
)

private const val PLAIN = "Plain"

private fun PlanetPick.encode() = when (this) {
    PlanetPick.Auto -> ""
    PlanetPick.Plain -> PLAIN
    is PlanetPick.Of -> planet.name
}

private fun decodePick(text: String): PlanetPick =
    if (text == PLAIN) PlanetPick.Plain else Planet.entries.find { it.name == text }?.let(PlanetPick::Of) ?: PlanetPick.Auto

/** Whether [key] can be stored: an activity's key never holds a line break or a tab, but a shortcut's id may. */
fun isStorable(key: String) = LINE !in key && FIELD !in key

internal fun String.nonEmptyLines() = split(LINE).filter(String::isNotEmpty)
