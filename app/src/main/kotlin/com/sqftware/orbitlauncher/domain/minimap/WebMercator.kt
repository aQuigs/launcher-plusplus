package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

data class LatLon(val lat: Double, val lon: Double)

/** A point in spherical web mercator metres (EPSG:3857), x east and y north. */
data class Mercator(val x: Double, val y: Double)

/** A slippy-map tile: zoom [z], column [x] from the antimeridian eastward, row [y] from the north. */
data class TileId(val z: Int, val x: Int, val y: Int)

/** A box in web mercator metres. */
data class MercatorBox(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

object WebMercator {
    private const val EARTH_RADIUS_M = 6_378_137.0
    const val HALF_WORLD_M = PI * EARTH_RADIUS_M
    private const val MAX_LAT = 85.05112878

    fun toMercator(at: LatLon): Mercator {
        val lat = at.lat.coerceIn(-MAX_LAT, MAX_LAT)
        return Mercator(
            x = Math.toRadians(at.lon) * EARTH_RADIUS_M,
            y = ln(tan(PI / 4 + Math.toRadians(lat) / 2)) * EARTH_RADIUS_M,
        )
    }

    fun toLatLon(at: Mercator) = LatLon(
        lat = Math.toDegrees(2 * atan(exp(at.y / EARTH_RADIUS_M)) - PI / 2),
        lon = Math.toDegrees(at.x / EARTH_RADIUS_M),
    )

    /**
     * Ground metres per mercator metre at [lat]: mercator stretches everything away from the equator by 1 / cos(lat).
     * Taken at the latitude [toMercator] clamps to, since the map stops short of the poles, where the scale would reach
     * across the whole world.
     */
    fun groundScale(lat: Double) = cos(Math.toRadians(lat.coerceIn(-MAX_LAT, MAX_LAT)))

    /**
     * [groundScale] at the whole degree of latitude nearest [lat]: the scale of the world-wide pixel grid that seeded
     * features and the world map keep to, which changes only a degree away, so the grid holds still as the view moves.
     */
    fun gridScale(lat: Double) = groundScale(round(lat))

    /** Ground metres from [a] to [b], close enough for places a few kilometres apart. */
    fun groundDistance(a: LatLon, b: LatLon): Double {
        val from = toMercator(a)
        val to = toMercator(b)
        return hypot(to.x - from.x, to.y - from.y) * groundScale((a.lat + b.lat) / 2)
    }

    /** Ground meters east and north from [from] to [to], at the scale at [from], close enough for places a few kilometers apart. */
    fun groundOffset(from: LatLon, to: LatLon): Local {
        val a = toMercator(from)
        val b = toMercator(to)
        val scale = groundScale(from.lat)
        return Local((b.x - a.x) * scale, (b.y - a.y) * scale)
    }

    /**
     * Which way to set out from [from] to reach [to] by the shortest way round the Earth, in degrees clockwise from
     * north, so a place across an ocean is pointed to as a compass would, not along the map's straight line.
     */
    fun bearing(from: LatLon, to: LatLon): Double {
        val lat1 = Math.toRadians(from.lat)
        val lat2 = Math.toRadians(to.lat)
        val dLon = Math.toRadians(to.lon - from.lon)
        val east = sin(dLon) * cos(lat2)
        val north = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return Math.toDegrees(atan2(east, north)).mod(360.0)
    }

    fun tileSize(z: Int) = 2 * HALF_WORLD_M / (1 shl z)

    fun tileAt(at: Mercator, z: Int): TileId {
        val n = 1 shl z
        val size = tileSize(z)
        return TileId(
            z = z,
            x = floor((at.x + HALF_WORLD_M) / size).toInt().coerceIn(0, n - 1),
            y = floor((HALF_WORLD_M - at.y) / size).toInt().coerceIn(0, n - 1),
        )
    }

    fun bounds(tile: TileId): MercatorBox {
        val size = tileSize(tile.z)
        val minX = -HALF_WORLD_M + tile.x * size
        val maxY = HALF_WORLD_M - tile.y * size
        return MercatorBox(minX, maxY - size, minX + size, maxY)
    }

    /** The mercator position of [point] in [tile], whose layer spans [extent] units a side. */
    fun fromTile(tile: TileId, extent: Int, point: TilePoint): Mercator {
        val box = bounds(tile)
        val scale = (box.maxX - box.minX) / extent
        return Mercator(box.minX + point.x * scale, box.maxY - point.y * scale)
    }

    /** Every tile at zoom [z] that holds ground within [radiusM] metres of [centre]. */
    fun tilesAround(centre: LatLon, radiusM: Double, z: Int): List<TileId> {
        val c = toMercator(centre)
        val reach = radiusM / groundScale(centre.lat)
        val low = tileAt(Mercator(c.x - reach, c.y - reach), z)
        val high = tileAt(Mercator(c.x + reach, c.y + reach), z)
        return (high.y..low.y).flatMap { y -> (low.x..high.x).map { x -> TileId(z, x, y) } }
    }
}
