package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * The game's world map zoom levels against its minimap's scale, but for its 37%, which would paint seven times the ground
 * of the minimap's scale for each stretch.
 */
private val ZOOM_LEVELS = listOf(0.5, 0.75, 1.0, 2.0)

/**
 * The fewest screen pixels a world map pixel takes: a smaller pixel would show, and so paint, the whole of a small
 * phone's screen in map pixels, more than its memory comfortably holds.
 */
private const val LEAST_STEP = 2

/** How far past a step a view may stray out before it is painted coarser, so a pinch that only wobbles there repaints nothing. */
private const val COARSEN_PAST = 1.15

/** How far past the farthest steps out and in a pinch may stretch before it springs back. */
private const val OVERZOOM_OUT = 0.8
private const val OVERZOOM_IN = 1.25

/**
 * The world map's zoom steps, in screen pixels to the map pixel, where the minimap's scale is [gamePixel] screen pixels:
 * whole ones, so the map is as crisp as the game's on every step, then [farSteps] more out from them, each half the last,
 * for a game whose map pulls back over its whole land. On those a painting's pixels span more map pixels.
 */
class ZoomSteps(gamePixel: Int, farSteps: Int = 0) {
    private val whole = ZOOM_LEVELS.map { maxOf(LEAST_STEP, (gamePixel * it).roundToInt()).toDouble() }.distinct()

    val all: List<Double> = (farSteps downTo 1).map { whole.first() / (1 shl it) } + whole

    /** The step nearest [scale], judged by ratio, which a pinch let go at [scale] settles on. */
    fun nearest(scale: Double): Double = all.minBy { abs(ln(it / scale)) }

    /** [scale] as far as a pinch may take it, a little past the farthest steps. */
    fun clamp(scale: Double) = scale.coerceIn(all.first() * OVERZOOM_OUT, all.last() * OVERZOOM_IN)

    /**
     * The step a view at [scale] is painted for: the nearest at or out from it, so a painting holds as much ground as any
     * step the view settles on, and one past the farthest step out paints no ground it springs back from. Past the whole
     * steps, where the next is coarser, a view still takes a step it is barely out from.
     */
    fun paintedAt(scale: Double): Double = all.lastOrNull { it <= if (it <= whole.first()) scale * COARSEN_PAST else scale } ?: all.first()

    /** How many map pixels across each pixel is of the painting for a view at [scale]. */
    fun spanAt(scale: Double): Int = maxOf(1, (whole.first() / paintedAt(scale)).roundToInt())
}
