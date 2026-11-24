package com.sqftware.orbitlauncher.domain

import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.WebMercator

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

/** A place OpenStreetMap knows, as its search returns one: a business or landmark by its [name], or an address. */
data class OsmPlace(
    val name: String? = null,
    val houseNumber: String? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

/** It as a place to pick: named by its name, else its street address, with its address written out as a postal one is. */
fun OsmPlace.toPlace(): PlaceValue? {
    val streetLine = listOfNotNull(houseNumber, street).joinToString(" ").ifEmpty { null }
    val region = listOfNotNull(state, postcode).joinToString(" ").ifEmpty { null }
    val label = name ?: streetLine ?: city ?: return null
    val address = listOfNotNull(streetLine, city, region, country).joinToString(", ")
    return PlaceValue(label, address.ifEmpty { label }, latitude, longitude)
}

/**
 * The places each search found, in the searches' order, leaving out one an earlier search found too. Places one search
 * found are all kept: two shops of a chain can be next door to each other.
 */
fun combinedPlaces(found: List<List<PlaceValue>>): List<PlaceValue> = buildList {
    for (places in found) {
        val earlier = toList()
        addAll(places.filter { place -> earlier.none { it.isSamePlace(place) } })
    }
}

/**
 * Whether [query] is an address written out, such as a maps app shares or a user types: one with a comma, or starting
 * with a house number. A search by name matches one loosely, so a geocoder's answer is the better one.
 */
fun isWrittenAddress(query: String): Boolean = ',' in query || query.trimStart().firstOrNull()?.isDigit() == true

// Two searches name a place alike but put it a few meters apart, so the same name close by is the same place.
private fun PlaceValue.isSamePlace(other: PlaceValue): Boolean {
    if (!label.equals(other.label, ignoreCase = true)) return false
    val here = latLon ?: return address.equals(other.address, ignoreCase = true)
    val there = other.latLon ?: return address.equals(other.address, ignoreCase = true)
    return WebMercator.groundDistance(here, there) < SAME_PLACE_M
}

internal val PlaceValue.latLon get() = latitude?.let { lat -> longitude?.let { LatLon(lat, it) } }

private const val SAME_PLACE_M = 150.0
