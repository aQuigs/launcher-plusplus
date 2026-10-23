package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class VitalsTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val tuesday = monday.plusDays(1)
    private val boot = 7

    private fun mondayTally() = StepTally.first(5_000, monday, boot).read(6_000, monday, boot)

    @Test
    fun `steps count up from the first reading`() {
        assertEquals(1_000, mondayTally().today)
    }

    @Test
    fun `a new day counts on from the last reading of the one before`() {
        val tally = mondayTally().read(6_030, tuesday, boot)
        assertEquals(30, tally.today)
        assertEquals(400, tally.read(6_400, tuesday, boot).today)
    }

    @Test
    fun `a new day that starts with a jump counts from there rather than claim steps it did not hear`() {
        assertEquals(0, mondayTally().read(13_000, tuesday, boot).today)
    }

    @Test
    fun `a restart keeps the day's steps and counts on from zero`() {
        val tally = mondayTally().read(200, monday, boot + 1)
        assertEquals(1_200, tally.today)
        assertEquals(1_500, tally.read(500, monday, boot + 1).today)
    }

    @Test
    fun `a restart shows in the boot count even once the counter has passed its last reading`() {
        assertEquals(1_000 + 7_000, mondayTally().read(7_000, monday, boot + 1).today)
    }

    @Test
    fun `a restart overnight starts the new day from zero`() {
        assertEquals(150, mondayTally().read(150, tuesday, boot + 1).today)
    }

    @Test
    fun `steps toward the goal stop at full`() {
        assertEquals(42, Steps.Today(4_200).percent)
        assertEquals(100, Steps.Today(STEP_GOAL).percent)
        assertEquals(100, Steps.Today(25_000).percent)
    }
}
