package com.sqftware.orbitlauncher.domain.minimap

/** A shift across the world map in its pixels, [x] east and [y] south, as a screen counts them. */
data class MapShift(val x: Double, val y: Double)

/** The place [shift] from here. */
fun LatLon.shifted(shift: MapShift): LatLon {
    val at = WebMercator.toMercator(this)
    val perPixel = mercatorPerPixel(lat)
    return WebMercator.toLatLon(Mercator(at.x + shift.x * perPixel, at.y - shift.y * perPixel))
}

/** The shift from here to [to]. */
fun LatLon.shiftTo(to: LatLon): MapShift {
    val from = WebMercator.toMercator(this)
    val at = WebMercator.toMercator(to)
    val perPixel = mercatorPerPixel(lat)
    return MapShift((at.x - from.x) / perPixel, (from.y - at.y) / perPixel)
}

private fun mercatorPerPixel(lat: Double) = 1 / gridPixelsPerMetre(lat)
