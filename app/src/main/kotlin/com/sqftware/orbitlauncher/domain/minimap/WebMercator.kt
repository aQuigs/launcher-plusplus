package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
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
