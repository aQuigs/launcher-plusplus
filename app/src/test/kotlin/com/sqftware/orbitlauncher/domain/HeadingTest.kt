package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
    fun aTurnPastNorthCountsTheShortWay() {
        assertTrue(turnedPast(359f, 1f, 1f))
        assertFalse(turnedPast(359.8f, 0.2f, 1f))
    }
}
