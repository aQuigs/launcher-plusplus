package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewTest {
    private val bryantPark = LatLon(40.7536, -73.9832)
    private val steps = ZoomSteps(4)

    // A 1080 by 2400 screen on the step of the minimap's scale shows 270 by 600 map pixels.
    private val view = MapView(bryantPark, 4.0, 1080, 2400)

    @Test
    fun aDragMovesTheGroundWithTheFinger() {
        val dragged = view.draggedBy(400.0, -200.0)
        val moved = bryantPark.shiftTo(dragged.centre)
        assertEquals(-100.0, moved.x, 1e-6)
        assertEquals(50.0, moved.y, 1e-6)
    }

    @Test
    fun aZoomKeepsTheGroundUnderTheFingers() {
        // The fingers 300 px right of and 200 px above the middle, as the map goes from 4 to 6 screen pixels a map pixel.
        val under = view.centre.shifted(MapShift(300.0 / 4, -200.0 / 4))
        val zoomed = view.zoomedBy(1.5, 300.0, -200.0, steps)
        assertEquals(6.0, zoomed.scale, 1e-9)
        val spot = zoomed.onScreen(under)
        assertEquals(300.0, spot.x, 1e-6)
        assertEquals(-200.0, spot.y, 1e-6)
    }

    @Test
    fun paintsRoomToPanRoundTheViewAndJustTheViewWhileItReachesPastThePainting() {
        val roomy = view.nextPainting(PaintedSpan(bryantPark, 782, 1112), steps)
        assertEquals(782, roomy.width)
        assertEquals(1112, roomy.height)

        val first = view.nextPainting(null, steps)
        assertEquals(334, first.width)
        assertEquals(664, first.height)

        // Zoomed out towards 2, the 1112 px painting no longer reaches the 1200 px view's ends.
        val tight = view.copy(scale = 2.4).nextPainting(PaintedSpan(bryantPark, 782, 1112), steps)
        assertEquals(604, tight.width)
        assertEquals(1264, tight.height)
    }

    @Test
    fun repaintsAsTheViewNearsThePaintingsEdgeOrZoomsOutPastIt() {
        // The 782 by 1112 painting round the 270 by 600 view leaves 160 px to pan each way before the repaint margin.
        val painted = PaintedSpan(bryantPark, 782, 1112)
        fun after(dx: Double, dy: Double, scale: Double = 4.0) =
            view.copy(centre = bryantPark.shifted(MapShift(dx, dy)), scale = scale).needsRepaint(painted, steps)
        assertFalse(after(0.0, 0.0))
        assertFalse(after(150.0, -150.0))
        assertTrue(after(0.0, -170.0))
        assertTrue(after(170.0, 0.0))

        assertFalse(after(0.0, 0.0, scale = 8.0))
        assertFalse(after(0.0, 0.0, scale = 3.5))
        assertTrue(after(0.0, 0.0, scale = 2.0))
        assertTrue(view.needsRepaint(null, steps))
    }
}
