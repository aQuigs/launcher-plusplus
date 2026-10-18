package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class SpinStatsTest {
    private val turn = 2 * PI

    @Test
    fun `spins add up their turns each way and count one each`() {
        val stats = SpinStats() + Spin(clockwise = 3 * turn, anticlockwise = 0.5 * turn, letGo = 0.0, coast = 0.0) +
            Spin(clockwise = turn, anticlockwise = 2 * turn, letGo = 0.0, coast = 0.0)

        assertEquals(2, stats.spins)
        assertEquals(4.0, stats.clockwise, 1e-9)
        assertEquals(2.5, stats.anticlockwise, 1e-9)
    }

    @Test
    fun `records keep the best single spin, whichever way it went`() {
        val stats = SpinStats() + Spin(clockwise = 3 * turn, anticlockwise = 0.5 * turn, letGo = turn, coast = 4.0) +
            Spin(clockwise = 0.0, anticlockwise = turn, letGo = -2 * turn, coast = 1.0)

        assertEquals(3.5, stats.mostTurns, 1e-9)
        assertEquals(120.0, stats.topRpm, 1e-9)
        assertEquals(4.0, stats.longestCoast, 1e-9)
    }
}
