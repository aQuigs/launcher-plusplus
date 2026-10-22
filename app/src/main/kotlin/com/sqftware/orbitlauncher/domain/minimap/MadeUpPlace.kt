package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.floor
import kotlin.math.hypot

/** A millionth of a metre: far below a pixel, far above a double's rounding at a place's size. */
private const val HAIR_M = 1e-6

/** A closed loop through [points]; two points make a walk there and back. */
class Route(private val points: List<Local>) {
    private val legs = points.zip(points.drop(1) + points.first())

    /** Metres once round. */
    val length = legs.sumOf { (from, to) -> distance(from, to) }

    /** Where someone is after walking [walked] metres round from the first point. */
    fun at(walked: Double): Local {
        var left = walked.mod(length)
        for ((from, to) in legs) {
            val leg = distance(from, to)
            if (left < leg) {
                val t = left / leg
                return Local(from.east + (to.east - from.east) * t, from.north + (to.north - from.north) * t)
            }
            left -= leg
        }
        return points.first()
    }

    private fun distance(from: Local, to: Local) = hypot(to.east - from.east, to.north - from.north)
}

/** Someone walking a [route] round a made-up place, drawn as their [dot]: [pace] times as fast as the player, [start] metres round. */
class Walker(val dot: Dot, val route: Route, val pace: Double, val start: Double = 0.0)

/**
 * A place that is only on the minimap, for a map that does not show where the user is: its [ground] in metres from its
 * own origin, the [route] the player walks round it, and the [walkers] about it. One clock, the metres the player has
 * walked, moves everyone, so all stand still together.
 */
class MadeUpPlace(private val ground: MapScene, private val route: Route, private val walkers: List<Walker> = emptyList()) {
    /** Metres once round the player's route. */
    val length: Double
        get() = route.length

    /**
     * Where the player is after walking [walked] metres round the route, on the middle of a minimap pixel, so the map
     * moves a whole pixel at a time and stays crisp.
     */
    fun at(walked: Double): Local = route.at(walked).let { Local(onPixel(it.east), onPixel(it.north)) }

    /**
     * The minimap's scene once the player has walked [walked] metres. Its centre lies as many metres from where the
     * equator meets the prime meridian, where mercator metres are ground metres, so the painter's seeded trees and grass
     * belong to the place's own pixels and stay put as the player walks.
     */
    fun scene(walked: Double): MapScene {
        val at = at(walked)
        // The hair keeps a straight edge off the very middle of a pixel, where rounding would decide the pixel afresh
        // with each step, so a bank or wall could flicker as the map moves.
        fun Local.moved() = Local(east - at.east + HAIR_M, north - at.north + HAIR_M)
        fun Polygon.moved() = map { ring -> ring.map { it.moved() } }
        return ground.copy(
            centre = WebMercator.toLatLon(Mercator(at.east, at.north)),
            areas = ground.areas.map { it.copy(polygons = it.polygons.map { polygon -> polygon.moved() }) },
            ways = ground.ways.map { it.copy(points = it.points.map { point -> point.moved() }) },
            buildings = ground.buildings.map { it.copy(polygon = it.polygon.moved()) },
            pois = ground.pois.map { it.copy(at = it.at.moved()) },
            figures = walkers.map { Figure(it.route.at(it.start + walked * it.pace).moved(), it.dot) },
        )
    }

    private fun onPixel(metres: Double) = (floor(metres / METRES_PER_PIXEL) + 0.5) * METRES_PER_PIXEL
}

/**
 * Made-up [places] the player walks in turn, once round each before moving on to the next as if by teleport, so the
 * map changes its scenery rather than repeat one loop. Everyone in a place starts afresh with each visit.
 */
class MadeUpTour(val places: List<MadeUpPlace>) {
    private val length = places.sumOf { it.length }

    /** The minimap's scene once the player has walked [walked] metres of the tour. */
    fun scene(walked: Double): MapScene {
        var left = walked.mod(length)
        for (place in places) {
            if (left < place.length) return place.scene(left)
            left -= place.length
        }
        return places.first().scene(0.0)
    }
}
