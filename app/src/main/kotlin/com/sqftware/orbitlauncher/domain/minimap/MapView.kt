package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

/** How far the world map's view comes to the edge of its painting before the next is painted, in map pixels. */
private const val REPAINT_MARGIN = 96.0

/** The ground the world map paints round its view on every side, so the view pans a while before the next painting. */
private const val PAN_ROOM = 256

/**
 * The ground painted round a view that has run past its painting: little, so the gap is soon covered, and less than the
 * repaint margin, so the roomier painting follows at once.
 */
private const val COVER_ROOM = 32

/** A painting of the world map: where it is centred, and its [width] and [height] in map pixels. */
data class PaintedSpan(val centre: LatLon, val width: Int, val height: Int)

/** The world map as the screen shows it: [centre] in the middle of a [width] by [height] px screen, [scale] screen pixels a map pixel. */
data class MapView(val centre: LatLon, val scale: Double, val width: Int, val height: Int) {
    /** The view with the ground dragged [dx] and [dy] screen pixels, right and down. */
    fun draggedBy(dx: Double, dy: Double) = copy(centre = centre.shifted(MapShift(-dx / scale, -dy / scale)))

    /**
     * The view zoomed by [factor], as far as [steps] allow, about a point [anchorX] and [anchorY] screen pixels right of
     * and below the middle, so the ground under that point stays under it.
     */
    fun zoomedBy(factor: Double, anchorX: Double, anchorY: Double, steps: ZoomSteps): MapView {
        val to = steps.clamp(scale * factor)
        val by = 1 / scale - 1 / to
        return copy(centre = centre.shifted(MapShift(anchorX * by, anchorY * by)), scale = to)
    }

    /** Where [at] shows, in screen pixels right of and below the middle. */
    fun onScreen(at: LatLon) = centre.shiftTo(at).let { MapShift(it.x * scale, it.y * scale) }

    /**
     * Whether [painted] leaves too little ground past the view's edge, as the view pans or zooms out towards it, and the
     * next painting should be started round the view before the view reaches past it; with nothing painted, it should.
     */
    fun needsRepaint(painted: PaintedSpan?, steps: ZoomSteps) = painted == null || roomIn(painted, steps) < REPAINT_MARGIN

    /**
     * The stretch to paint round the view next: the view with room to pan, or, while the view reaches past [painted],
     * just the view, which paints soonest.
     */
    fun nextPainting(painted: PaintedSpan?, steps: ZoomSteps): MapFrame {
        val room = if (painted == null || roomIn(painted, steps) < 0) COVER_ROOM else PAN_ROOM
        val (spanX, spanY) = span(steps)
        return MapFrame.worldMap(ceil(spanX).toInt() + 2 * room, ceil(spanY).toInt() + 2 * room)
    }

    // The view in map pixels at the step it is painted for.
    private fun span(steps: ZoomSteps): Pair<Double, Double> {
        val at = steps.paintedAt(scale)
        return width.toDouble() / at to height.toDouble() / at
    }

    // The least ground [painted] holds past the view's edge, in map pixels: below nothing where the view reaches past it.
    private fun roomIn(painted: PaintedSpan, steps: ZoomSteps): Double {
        val shift = painted.centre.shiftTo(centre)
        val (spanX, spanY) = span(steps)
        return min((painted.width - spanX) / 2 - abs(shift.x), (painted.height - spanY) / 2 - abs(shift.y))
    }
}
