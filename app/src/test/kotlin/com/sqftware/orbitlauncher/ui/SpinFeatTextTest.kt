package com.sqftware.orbitlauncher.ui

import com.sqftware.orbitlauncher.domain.Milestone
import com.sqftware.orbitlauncher.domain.RecordBeaten
import com.sqftware.orbitlauncher.domain.SpinFeats
import com.sqftware.orbitlauncher.domain.SpinRecord
import com.sqftware.orbitlauncher.domain.YardstickMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class SpinFeatTextTest {
    private fun text(vararg records: RecordBeaten, milestones: List<Milestone> = emptyList()) =
        spinFeatsText(SpinFeats(records.toList(), milestones), Locale.UK)

    @Test
    fun `one record gives its new best, several are only named, and each milestone has a line`() {
        assertNull(text())
        assertEquals("New record: top speed, 312 rpm", text(RecordBeaten(SpinRecord.TopRpm, 300.0, 312.4)))
        assertEquals(
            "New records: most in one spin, top speed and longest coast\nMilestone: 1,000 spins",
            text(
                RecordBeaten(SpinRecord.MostTurns, 3.0, 4.0),
                RecordBeaten(SpinRecord.TopRpm, 300.0, 312.0),
                RecordBeaten(SpinRecord.LongestCoast, 6.0, 8.0),
                milestones = listOf(Milestone.Spins(1_000)),
            ),
        )
    }

    @Test
    fun `a record that still shows as the last best is no news`() {
        assertNull(text(RecordBeaten(SpinRecord.TopRpm, 120.2, 120.4), RecordBeaten(SpinRecord.MostTurns, 3.46, 3.52)))
    }

    @Test
    fun `a distance reads as the length of so many of a yardstick`() {
        assertEquals(
            "You've spun the length of an Eiffel Tower",
            text(milestones = listOf(Milestone.Distance(YardstickMark(1, "Eiffel Tower", "Eiffel Towers")))),
        )
        assertEquals("You've spun the length of 2 giraffes", text(milestones = listOf(Milestone.Distance(YardstickMark(2, "giraffe", "giraffes")))))
    }
}
