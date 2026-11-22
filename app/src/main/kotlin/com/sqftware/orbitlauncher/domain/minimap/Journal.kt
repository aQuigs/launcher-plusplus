package com.sqftware.orbitlauncher.domain.minimap

/**
 * How near the quest the user counts as there, in ground meters: as far as they move between two fixes, so the last fix
 * before they reach it is not left short of it.
 */
const val ARRIVED_M = 50.0

/**
 * What a theme that keeps a journal keeps of the user's travels, as the northern game's does: the spot its [quest]
 * marker points to, if any, and the [Place.key]s of the places [discovered].
 */
data class Journal(val quest: LatLon? = null, val discovered: Set<String> = emptySet()) {
    /** The journal once the user has been to [place], or null if that is no news: no place known, or one discovered before. */
    fun discovering(place: Place?): Journal? = place?.key?.takeIf { it !in discovered }?.let { copy(discovered = discovered + it) }

    /** The journal with its quest cleared once the user is [at] it, or null while they are not, or have no quest. */
    fun arriving(at: LatLon): Journal? = quest?.takeIf { WebMercator.groundDistance(it, at) <= ARRIVED_M }?.let { copy(quest = null) }
}
