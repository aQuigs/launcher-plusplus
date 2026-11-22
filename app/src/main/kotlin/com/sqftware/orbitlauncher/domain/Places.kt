package com.sqftware.orbitlauncher.domain

/** How many places the prompt offers as recent. */
const val RECENT_PLACES = 8

/** These recent places with [place] first, once, and the oldest past [RECENT_PLACES] dropped. */
fun List<PlaceValue>.withRecent(place: PlaceValue): List<PlaceValue> = (listOf(place) + filterNot { it.address == place.address }).take(RECENT_PLACES)

/**
 * What to search for to find the place in [text] shared from another app: Google Maps shares a place's name and address
 * on lines of their own, then a link, which no geocoder reads. Null when nothing but links is left.
 */
fun placeQueryFromShare(text: String): String? =
    text.lines()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.contains("://") }
        .joinToString(", ")
        .takeIf { it.isNotEmpty() }
