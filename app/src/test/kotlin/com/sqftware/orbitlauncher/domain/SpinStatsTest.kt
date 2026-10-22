package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import kotlin.math.PI

class SpinStatsTest {
    private val turn = 2 * PI
    private val day = LocalDate.of(2026, 10, 21)

    private fun spin(clockwise: Double = turn, anticlockwise: Double = 0.0, letGo: Double = 0.0, coast: Double = 0.0, rim: Double = 160.0) =
        Spin(clockwise, anticlockwise, letGo, coast, rim)

    private fun SpinStats.on(vararg days: LocalDate) = days.fold(this) { stats, day -> stats.add(spin(), day) }

    @Test
    fun `spins add up their turns each way and count one each`() {
        val stats = SpinStats().add(spin(clockwise = 3 * turn, anticlockwise = 0.5 * turn), day)
            .add(spin(clockwise = turn, anticlockwise = 2 * turn), day)

        assertEquals(2, stats.spins)
        assertEquals(4.0, stats.clockwise, 1e-9)
        assertEquals(2.5, stats.anticlockwise, 1e-9)
    }

    @Test
    fun `records keep the best single spin, whichever way it went`() {
        val stats = SpinStats().add(spin(clockwise = 3 * turn, anticlockwise = 0.5 * turn, letGo = turn, coast = 4.0), day)
            .add(spin(clockwise = 0.0, anticlockwise = turn, letGo = -2 * turn, coast = 1.0), day)

        assertEquals(3.5, stats.mostTurns, 1e-9)
        assertEquals(120.0, stats.topRpm, 1e-9)
        assertEquals(4.0, stats.longestCoast, 1e-9)
        assertEquals(5.0, stats.coasting, 1e-9)
    }

    @Test
    fun `the distance is how far the rim went both ways, a 160 dp rim being an inch from the centre`() {
        val stats = SpinStats().add(spin(clockwise = turn, anticlockwise = turn, rim = 160.0), day)

        assertEquals(2 * turn * 0.0254, stats.distance, 1e-9)
    }

    @Test
    fun `a streak runs while each day has a spin and starts over after a day without`() {
        val stats = SpinStats().on(day, day, day.plusDays(1), day.plusDays(2), day.plusDays(4))

        assertEquals(3, stats.longestStreak)
        assertEquals(1, stats.streak)
        assertEquals(4, stats.daysSpun)
        assertEquals(2, stats.busiestDay)
    }

    @Test
    fun `a day before the last, as after a clock set ahead is put right, starts a new day and streak`() {
        val ahead = SpinStats().on(day.plusDays(365))
        val stats = ahead.on(day, day, day.plusDays(1))

        assertEquals(2, stats.streak)
        assertEquals(3, stats.daysSpun)
        assertEquals(2, stats.busiestDay)
    }
}
