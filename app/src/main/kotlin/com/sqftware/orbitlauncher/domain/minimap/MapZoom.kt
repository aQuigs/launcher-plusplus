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

/** How far past the farthest steps out and in a pinch may stretch before it springs back. */
private const val OVERZOOM_OUT = 0.8
private const val OVERZOOM_IN = 1.25

/**
 * The world map's zoom steps, in screen pixels to the map pixel, where the minimap's scale is [gamePixel] screen pixels:
 * whole ones, so the map is as crisp as the game's on every step.
 */
class ZoomSteps(gamePixel: Int) {
    val all: List<Int> = ZOOM_LEVELS.map { maxOf(LEAST_STEP, (gamePixel * it).roundToInt()) }.distinct()

    /** The step nearest [scale], judged by ratio, which a pinch let go at [scale] settles on. */
    fun nearest(scale: Double): Int = all.minBy { abs(ln(it / scale)) }

    /** [scale] as far as a pinch may take it, a little past the farthest steps. */
    fun clamp(scale: Double) = scale.coerceIn(all.first() * OVERZOOM_OUT, all.last() * OVERZOOM_IN)

    /**
     * The step a view at [scale] is painted for: the nearest at or out from it, so a painting holds as much ground as any
     * step the view settles on, and one past the farthest step out paints no ground it springs back from.
     */
    fun paintedAt(scale: Double): Int = all.lastOrNull { it <= scale } ?: all.first()
}
