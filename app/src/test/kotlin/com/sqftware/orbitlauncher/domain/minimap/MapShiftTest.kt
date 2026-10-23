package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
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
}
