package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.abs

/** How far the world map's view comes to the edge of its painting before the next is painted, in map pixels. */
private const val REPAINT_MARGIN = 96.0

/** How far the view must move before the next painting, however little room its painting leaves round it. */
private const val LEAST_REPAINT_SHIFT = 32.0

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

/**
 * Whether a view [width] by [height] map pixels round [view] has come close enough to the edge of the world map painted
 * round [painted] that the next should be painted round the view, before it reaches past the painting's edge.
 */
fun needsRepaint(painted: LatLon, view: LatLon, width: Double, height: Double): Boolean {
    val shift = painted.shiftTo(view)
    fun past(span: Double, by: Double) = abs(by) > maxOf(LEAST_REPAINT_SHIFT, (WORLD_MAP_SIZE - span) / 2 - REPAINT_MARGIN)
    return past(width, shift.x) || past(height, shift.y)
}

private fun mercatorPerPixel(lat: Double) = 1 / gridPixelsPerMetre(lat)
