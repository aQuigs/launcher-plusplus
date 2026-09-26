package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A shape on a globe: an outline of longitude and latitude pairs in [degrees], east and north positive. Its edges are cut
 * into short steps so they bend round the globe.
 */
internal class GlobeShape(degrees: IntArray) {
    private val longitudeCos: FloatArray
    private val longitudeSin: FloatArray
    private val latitudeCos: FloatArray
    private val latitudeSin: FloatArray

    init {
        val corners = degrees.size / 2
        val lon = ArrayList<Float>()
        val lat = ArrayList<Float>()
        for (corner in 0 until corners) {
            val next = (corner + 1) % corners
            val x = degrees[2 * corner].toFloat()
            val y = degrees[2 * corner + 1].toFloat()
            val dx = degrees[2 * next] - x
            val dy = degrees[2 * next + 1] - y
            val steps = ceil(max(abs(dx), abs(dy)) / STEP_DEGREES).toInt()
            for (step in 0 until steps) {
                lon += Math.toRadians((x + dx * step / steps).toDouble()).toFloat()
                lat += Math.toRadians((y + dy * step / steps).toDouble()).toFloat()
            }
        }
        longitudeCos = FloatArray(lon.size) { cos(lon[it]) }
        longitudeSin = FloatArray(lon.size) { sin(lon[it]) }
        latitudeCos = FloatArray(lat.size) { cos(lat[it]) }
        latitudeSin = FloatArray(lat.size) { sin(lat[it]) }
    }

    /**
     * Adds the shape to [path] on a globe of [radius] round [centre], turned [spin] radians east and tipped [tilt] radians
     * to show its north, unless none of it faces us. What lies round the far side is pressed onto the globe's edge, so the
     * edge cuts a shape that reaches past it.
     */
    fun addTo(path: Path, centre: Offset, radius: Float, spin: Float, tilt: Float) {
        val spinCos = cos(spin)
        val spinSin = sin(spin)
        val tiltCos = cos(tilt)
        val tiltSin = sin(tilt)
        fun turnCos(i: Int) = longitudeCos[i] * spinCos - longitudeSin[i] * spinSin
        fun depth(i: Int) = tiltSin * latitudeSin[i] + tiltCos * latitudeCos[i] * turnCos(i)
        if (latitudeSin.indices.none { depth(it) >= 0f }) return

        for (i in latitudeSin.indices) {
            var x = latitudeCos[i] * (longitudeSin[i] * spinCos + longitudeCos[i] * spinSin)
            var y = tiltCos * latitudeSin[i] - tiltSin * latitudeCos[i] * turnCos(i)
            if (depth(i) < 0f) {
                val out = sqrt(x * x + y * y).coerceAtLeast(1e-4f)
                x /= out
                y /= out
            }
            val at = centre + Offset(x * radius, -y * radius)
            if (i == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
        }
        path.close()
    }
}

private const val STEP_DEGREES = 4f

/** The Earth's land, drawn simply: the continents and the islands big enough to show on a folder. */
internal val EarthLand = listOf(
    // North America
    GlobeShape(
        intArrayOf(
            -166, 68, -156, 71, -140, 70, -128, 70, -115, 68, -95, 69, -85, 66, -80, 62, -94, 59, -92, 57, -82, 55,
            -79, 52, -78, 58, -72, 61, -64, 60, -56, 52, -60, 47, -66, 44, -70, 42, -74, 40, -76, 35, -81, 31, -80, 26,
            -81, 25, -83, 29, -89, 30, -94, 29, -97, 26, -97, 21, -95, 19, -91, 19, -90, 21, -87, 21, -88, 16, -84, 15,
            -83, 10, -79, 9, -77, 8, -80, 7, -84, 9, -86, 12, -92, 14, -96, 16, -105, 20, -110, 24, -114, 30, -117, 33,
            -121, 35, -124, 40, -124, 46, -127, 50, -133, 55, -140, 59, -148, 61, -153, 59, -158, 57, -163, 55, -158, 59,
            -162, 60, -165, 62, -163, 64,
        ),
    ),
    // Greenland
    GlobeShape(intArrayOf(-73, 78, -60, 82, -35, 83, -20, 80, -20, 72, -30, 68, -42, 60, -50, 64, -54, 68, -58, 76)),
    // South America
    GlobeShape(
        intArrayOf(
            -80, 9, -75, 11, -71, 12, -62, 10, -60, 8, -52, 5, -50, 0, -44, -2, -35, -5, -35, -9, -39, -13, -39, -18,
            -41, -22, -48, -26, -53, -34, -58, -38, -62, -39, -65, -42, -67, -46, -69, -51, -68, -55, -72, -54, -74, -50,
            -75, -45, -73, -38, -71, -30, -70, -18, -76, -14, -80, -6, -81, -3, -80, 1, -78, 4, -77, 8,
        ),
    ),
    // Africa
    GlobeShape(
        intArrayOf(
            -17, 21, -13, 28, -9, 32, -6, 36, 3, 37, 10, 37, 11, 33, 20, 31, 25, 32, 32, 31, 34, 28, 38, 22, 43, 12,
            51, 12, 51, 10, 47, 4, 40, -3, 39, -10, 40, -15, 35, -24, 33, -27, 27, -34, 20, -35, 18, -32, 15, -26, 12, -17,
            13, -12, 12, -5, 9, -1, 10, 4, 6, 5, 0, 6, -5, 5, -8, 4, -13, 8, -15, 11, -17, 15,
        ),
    ),
    GlobeShape(intArrayOf(44, -25, 47, -25, 50, -16, 49, -12, 44, -17)),
    // Eurasia, from Iberia along the north coast and back along the south
    GlobeShape(
        intArrayOf(
            -10, 36, -9, 43, -2, 44, -5, 48, 0, 50, 5, 53, 9, 54, 8, 57, 11, 58, 6, 58, 5, 62, 14, 68, 20, 70, 28, 71,
            40, 67, 44, 68, 55, 69, 70, 73, 80, 73, 100, 77, 113, 74, 130, 71, 140, 72, 160, 70, 180, 67, 172, 62,
            163, 58, 160, 53, 156, 51, 155, 58, 143, 59, 137, 54, 141, 49, 135, 43, 130, 42, 129, 35, 126, 35, 126, 38,
            122, 40, 121, 37, 120, 34, 122, 30, 120, 26, 114, 22, 108, 21, 106, 18, 109, 12, 105, 9, 100, 13, 102, 6,
            104, 1, 101, 3, 98, 8, 98, 16, 94, 17, 92, 22, 87, 21, 80, 15, 77, 8, 73, 17, 72, 21, 67, 25, 57, 26, 56, 24,
            59, 22, 52, 17, 45, 13, 43, 15, 39, 21, 35, 28, 34, 31, 35, 36, 30, 36, 27, 37, 26, 40, 23, 38, 20, 40,
            19, 42, 13, 45, 14, 42, 18, 40, 16, 38, 12, 42, 9, 44, 3, 43, 0, 39, -2, 37, -6, 36,
        ),
    ),
    GlobeShape(intArrayOf(-5, 50, 1, 51, 2, 53, -2, 56, -2, 58, -5, 59, -6, 56, -3, 54, -5, 52)),
    GlobeShape(intArrayOf(130, 31, 132, 34, 136, 34, 140, 35, 141, 38, 142, 40, 141, 42, 144, 44, 142, 45, 140, 42, 140, 39, 137, 37, 133, 35)),
    GlobeShape(intArrayOf(95, 5, 98, 4, 106, -6, 104, -6, 100, -1)),
    GlobeShape(intArrayOf(109, 1, 111, -3, 116, -4, 119, 1, 117, 7, 113, 3)),
    GlobeShape(intArrayOf(131, -1, 141, -3, 150, -10, 143, -9, 138, -8, 133, -4)),
    // Australia
    GlobeShape(
        intArrayOf(
            114, -22, 114, -34, 118, -35, 124, -34, 131, -31, 136, -35, 138, -34, 141, -38, 147, -38, 150, -37, 153, -31,
            153, -25, 146, -19, 145, -15, 142, -11, 141, -17, 136, -15, 137, -12, 131, -11, 126, -14, 122, -18,
        ),
    ),
    GlobeShape(intArrayOf(172, -34, 178, -38, 175, -41, 171, -44, 167, -46, 171, -41)),
)

/** The Arctic's ice, closed through the pole so it fills round it. The Earth leans too far north to show the Antarctic. */
internal val EarthIce = GlobeShape(intArrayOf(-180, 80, 180, 80, 180, 90, -180, 90))

/** A few streaks of cloud, which drift over the land at a pace of their own. */
internal val EarthClouds = listOf(
    cloud(-40, 45, 22, 5), cloud(-150, 20, 16, 4), cloud(60, -40, 26, 5), cloud(100, 10, 14, 4), cloud(160, -15, 18, 4),
)

/** An oval of cloud [width] by [height] degrees across round [longitude] and [latitude]. */
private fun cloud(longitude: Int, latitude: Int, width: Int, height: Int) = GlobeShape(
    IntArray(2 * CLOUD_CORNERS) {
        val angle = 2 * Math.PI * (it / 2) / CLOUD_CORNERS
        if (it % 2 == 0) longitude + (width * cos(angle)).toInt() else latitude + (height * sin(angle)).toInt()
    },
)

private const val CLOUD_CORNERS = 12
