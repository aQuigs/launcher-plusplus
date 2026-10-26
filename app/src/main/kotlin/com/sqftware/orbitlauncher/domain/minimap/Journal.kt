package com.sqftware.orbitlauncher.domain.minimap

/**
 * What the Skyrim theme keeps of the user's travels, as the game's journal does: the spot its [quest] marker points to,
 * if any, and the names of the places [discovered].
 */
data class Journal(val quest: LatLon? = null, val discovered: Set<String> = emptySet()) {
    /** The journal once the user is in [place], or null if that is no news: no place known, or one discovered before. */
    fun discovering(place: String?): Journal? = place?.takeIf { it !in discovered }?.let { copy(discovered = discovered + it) }
}
