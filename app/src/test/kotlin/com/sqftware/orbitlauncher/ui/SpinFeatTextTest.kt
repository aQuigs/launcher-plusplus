package com.sqftware.orbitlauncher.ui

import com.sqftware.orbitlauncher.domain.Milestone
import com.sqftware.orbitlauncher.domain.RecordBeaten
import com.sqftware.orbitlauncher.domain.SpinFeats
import com.sqftware.orbitlauncher.domain.SpinRecord
import com.sqftware.orbitlauncher.domain.YardstickMark
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SpinFeatTextTest {
    private fun texts(vararg records: RecordBeaten, milestones: List<Milestone> = emptyList()) =
        spinFeatTexts(SpinFeats(records.toList(), milestones), Locale.UK)

    @Test
    fun `each record gives its new best and each milestone its own notice`() {
        assertEquals(
            listOf("New record: most in one spin, 4.0 turns", "New record: top speed, 312 rpm", "Milestone: 1,000 spins"),
            texts(
                RecordBeaten(SpinRecord.MostTurns, 3.0, 4.0),
                RecordBeaten(SpinRecord.TopRpm, 300.0, 312.4),
                milestones = listOf(Milestone.Spins(1_000)),
            ),
        )
    }

    @Test
    fun `a record that still shows as the last best is no news`() {
        assertEquals(emptyList<String>(), texts(RecordBeaten(SpinRecord.TopRpm, 120.2, 120.4), RecordBeaten(SpinRecord.MostTurns, 3.46, 3.52)))
    }

    @Test
    fun `a distance reads as the length of so many of a yardstick`() {
        assertEquals(
            listOf("You've spun the length of an Eiffel Tower", "You've spun the length of 2 giraffes"),
            texts(
                milestones = listOf(
                    Milestone.Distance(YardstickMark(1, "Eiffel Tower", "Eiffel Towers")),
                    Milestone.Distance(YardstickMark(2, "giraffe", "giraffes")),
                ),
            ),
        )
    }
}
