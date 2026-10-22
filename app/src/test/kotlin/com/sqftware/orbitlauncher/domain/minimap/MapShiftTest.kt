package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapShiftTest {
    private val bryantPark = LatLon(40.7536, -73.9832)

    @Test
    fun shiftsByGroundMetresAndBack() {
        val east = bryantPark.shifted(MapShift(100.0, 0.0))
        assertEquals(100 * METRES_PER_PIXEL, WebMercator.groundDistance(bryantPark, east), 1.0)
        assertTrue(east.lon > bryantPark.lon)
        assertTrue(bryantPark.shifted(MapShift(0.0, 100.0)).lat < bryantPark.lat)

        val back = bryantPark.shiftTo(bryantPark.shifted(MapShift(-37.0, 212.0)))
        assertEquals(-37.0, back.x, 1e-6)
        assertEquals(212.0, back.y, 1e-6)
    }

    @Test
    fun repaintsOnlyAsTheViewNearsThePaintingsEdge() {
        // A phone's view, 270 by 600 map pixels, leaves 212 px of painting above and below it.
        fun after(shift: MapShift) = needsRepaint(bryantPark, bryantPark.shifted(shift), 270.0, 600.0)
        assertFalse(after(MapShift(0.0, 0.0)))
        assertFalse(after(MapShift(0.0, 100.0)))
        assertTrue(after(MapShift(0.0, -130.0)))
        assertFalse(after(MapShift(200.0, 0.0)))
        assertTrue(after(MapShift(-300.0, 0.0)))
    }

    @Test
    fun aViewTooBigToLeaveRoomStillWaitsForAMove() {
        assertFalse(needsRepaint(bryantPark, bryantPark.shifted(MapShift(20.0, 0.0)), 1000.0, 1000.0))
        assertTrue(needsRepaint(bryantPark, bryantPark.shifted(MapShift(40.0, 0.0)), 1000.0, 1000.0))
    }
}
