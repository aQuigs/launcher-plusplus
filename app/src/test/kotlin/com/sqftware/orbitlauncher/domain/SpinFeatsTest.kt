package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SpinFeatsTest {
    private fun SpinStats.feats(after: SpinStats.() -> SpinStats) = after().feats(before = this)

    @Test
    fun `a spin says which records it beat, from what to what, and the milestones it passed`() {
        val before = SpinStats(spins = 19, mostTurns = 3.0, topRpm = 100.0, longestCoast = 4.0)

        val feats = before.feats { copy(spins = 20, mostTurns = 3.5, topRpm = 120.0) }

        assertEquals(
            SpinFeats(
                listOf(RecordBeaten(SpinRecord.MostTurns, 3.0, 3.5), RecordBeaten(SpinRecord.TopRpm, 100.0, 120.0)),
                listOf(Milestone.Spins(20)),
            ),
            feats,
        )
    }

    @Test
    fun `records start counting only after the first ten spins, when nearly every spin sets one`() {
        assertEquals(SpinFeats(), SpinStats(spins = 8, topRpm = 100.0).feats { copy(spins = 9, topRpm = 120.0) })
        assertEquals(
            SpinFeats(listOf(RecordBeaten(SpinRecord.TopRpm, 100.0, 120.0))),
            SpinStats(spins = 10, topRpm = 100.0).feats { copy(spins = 11, topRpm = 120.0) },
        )
    }

    @Test
    fun `spin counts are milestones at round numbers from ten`() {
        val milestones = (1..1_000).filter { SpinStats(spins = it - 1).feats { copy(spins = it) }.milestones.isNotEmpty() }

        assertEquals(listOf(10, 20, 50, 100, 200, 500, 1_000), milestones)
    }

    @Test
    fun `the distance is a milestone each time it reaches a new round count of a yardstick`() {
        assertEquals(listOf(Milestone.Distance(yardstickMark(5.5)!!)), SpinStats(distance = 5.0).feats { copy(spins = 1, distance = 6.0) }.milestones)
        assertEquals(SpinFeats(), SpinStats(distance = 6.0).feats { copy(spins = 1, distance = 10.0) })
    }

    @Test
    fun `the busiest day is news once, from the spin that takes it from the days before`() {
        val day = LocalDate.of(2026, 10, 21)
        val spin = Spin(clockwise = 1.0, anticlockwise = 0.0, letGo = 0.0, coast = 0.0, rim = 0.0)
        val days = List(20) { day } + List(23) { day.plusDays(1) }
        val news = days.runningFold(SpinStats()) { stats, on -> stats.add(spin, on) }
            .zipWithNext { before, after -> after.feats(before) }
            .withIndex().filter { (_, feats) -> feats.records.any { it.record == SpinRecord.BusiestDay } }.map { it.index + 1 }

        assertEquals(listOf(41), news)
    }

    @Test
    fun `stats that are not one spin on, as after a reset, say nothing`() {
        assertEquals(SpinFeats(), SpinStats(spins = 99, topRpm = 100.0).feats { SpinStats() })
    }
}
