package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Which way to set out from here to reach [to] by the shortest way round the Earth, in degrees clockwise from north, so
 * a place across an ocean is pointed to as a compass would, not along the map's straight line.
 */
fun LatLon.bearingTo(to: LatLon): Double {
    val lat1 = Math.toRadians(lat)
    val lat2 = Math.toRadians(to.lat)
    val dLon = Math.toRadians(to.lon - lon)
    val east = sin(dLon) * cos(lat2)
    val north = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return Math.toDegrees(atan2(east, north)).mod(360.0)
}
