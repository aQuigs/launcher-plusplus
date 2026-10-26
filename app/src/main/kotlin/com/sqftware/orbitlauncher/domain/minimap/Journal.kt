package com.sqftware.orbitlauncher.domain.minimap

/**
 * What a theme that keeps a journal keeps of the user's travels, as the northern game's does: the spot its [quest]
 * marker points to, if any, and the [Place.key]s of the places [discovered].
 */
data class Journal(val quest: LatLon? = null, val discovered: Set<String> = emptySet()) {
    /** The journal once the user has been to [place], or null if that is no news: no place known, or one discovered before. */
    fun discovering(place: Place?): Journal? = place?.key?.takeIf { it !in discovered }?.let { copy(discovered = discovered + it) }
}
