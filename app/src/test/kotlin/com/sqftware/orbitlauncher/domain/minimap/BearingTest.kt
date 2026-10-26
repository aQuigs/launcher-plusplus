package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Test

class BearingTest {
    @Test
    fun pointsTheShortestWayRoundTheEarth() {
        val bryantPark = LatLon(40.7536, -73.9832)
        assertEquals(0.0, bryantPark.bearingTo(LatLon(bryantPark.lat + 0.01, bryantPark.lon)), 1e-6)
        assertEquals(90.0, LatLon(0.0, 0.0).bearingTo(LatLon(0.0, 10.0)), 1e-6)
        assertEquals(270.0, LatLon(0.0, 0.0).bearingTo(LatLon(0.0, -10.0)), 1e-6)
        // New York to London sets out northeast, not east along the map's straight line.
        assertEquals(51.2, LatLon(40.71, -74.01).bearingTo(LatLon(51.51, -0.13)), 0.5)
        // Honolulu to Tokyo goes west, across the date line, not east round the world.
        assertEquals(298.0, LatLon(21.31, -157.86).bearingTo(LatLon(35.68, 139.69)), 2.0)
    }
}
