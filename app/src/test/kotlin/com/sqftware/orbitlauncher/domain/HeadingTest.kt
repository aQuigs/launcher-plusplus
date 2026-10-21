package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingTest {
    @Test
    fun aHeadingMovesItsShareOfTheWayToTheReading() {
        assertEquals(50f, easeHeading(40f, 60f, 0.5f), 0.001f)
    }

    @Test
    fun aHeadingTurnsTheShortWayRoundPastNorth() {
        assertEquals(0f, easeHeading(350f, 10f, 0.5f), 0.001f)
        assertEquals(355f, easeHeading(5f, 345f, 0.5f), 0.001f)
    }

    @Test
    fun aHeadingStaysWithinACircle() {
        assertEquals(10f, wrapDegrees(370f), 0.001f)
        assertEquals(350f, wrapDegrees(-10f), 0.001f)
    }
}
