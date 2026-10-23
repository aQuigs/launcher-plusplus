package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class VitalsTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val tuesday = monday.plusDays(1)

    @Test
    fun `steps count up from the first reading`() {
        val tally = StepTally.first(5_000, monday).read(5_400, monday).read(6_000, monday)
        assertEquals(1_000, tally.today)
    }

    @Test
    fun `a new day counts from the last reading of the one before`() {
        val tally = StepTally.first(5_000, monday).read(6_000, monday).read(6_300, tuesday)
        assertEquals(300, tally.today)
        assertEquals(400, tally.read(6_400, tuesday).today)
    }

    @Test
    fun `a restart keeps the day's steps and counts on from zero`() {
        val tally = StepTally.first(5_000, monday).read(6_000, monday).read(200, monday)
        assertEquals(1_200, tally.today)
        assertEquals(1_500, tally.read(500, monday).today)
    }

    @Test
    fun `a restart overnight starts the new day from zero`() {
        assertEquals(150, StepTally.first(5_000, monday).read(6_000, monday).read(150, tuesday).today)
    }

    @Test
    fun `steps toward the goal stop at full`() {
        assertEquals(42, Steps.Today(4_200).percent)
        assertEquals(100, Steps.Today(STEP_GOAL).percent)
        assertEquals(100, Steps.Today(25_000).percent)
    }
}
