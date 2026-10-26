package com.sqftware.orbitlauncher.domain.minimap

import java.util.IdentityHashMap

/**
 * A painted map of the ground round [centre]: the [ground] of walls, trees and people, which the minimap turns with the
 * phone, and the map [icons] on it, kept apart so whoever shows the map keeps them upright as the game does, and the
 * [place] it shows, if known. [Image] is what its art is held as.
 */
class PaintedMap<Image>(val centre: LatLon, val ground: Image, val icons: List<MapMark<Image>>, val place: Place? = null) {
    /** The same map with its art held as [convert] turns it, each image once however many icons share it. */
    fun <Other> map(convert: (Image) -> Other): PaintedMap<Other> {
        val converted = IdentityHashMap<Image, Other>()
        return PaintedMap(centre, convert(ground), icons.map { MapMark(converted.getOrPut(it.image) { convert(it.image) }, it.x, it.y) }, place)
    }
}

/** A map icon's [image], its middle at ([x], [y]) on the ground, in pixels from its top left. */
class MapMark<Image>(val image: Image, val x: Float, val y: Float)
