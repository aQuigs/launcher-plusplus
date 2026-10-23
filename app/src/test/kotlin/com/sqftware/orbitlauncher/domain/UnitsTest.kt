package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class UnitsTest {
    @Test
    fun `a distance goes in metric units or, where the country uses them, imperial ones`() {
        assertEquals("45 cm", distanceText(0.45, Locale.UK))
        assertEquals("12.3 m", distanceText(12.34, Locale.UK))
        assertEquals("1,234.57 km", distanceText(1_234_567.0, Locale.UK))
        assertEquals("6 in", distanceText(0.1524, Locale.US))
        assertEquals("41 ft", distanceText(12.5, Locale.US))
        assertEquals("1.00 mi", distanceText(1_609.344, Locale.US))
    }

    @Test
    fun `a distance that rounds up to the next unit shows in it`() {
        assertEquals("1.0 m", distanceText(0.996, Locale.UK))
        assertEquals("1.00 km", distanceText(999.96, Locale.UK))
        assertEquals("1 ft", distanceText(0.99 * 0.3048, Locale.US))
    }

    @Test
    fun `a distance is put next to the longest yardstick it reaches`() {
        assertNull(yardstickText(0.1, Locale.UK))
        assertEquals("That's 1.0 banana", yardstickText(0.18, Locale.UK))
        assertEquals("That's 2.0 giraffes", yardstickText(11.0, Locale.UK))
    }

    @Test
    fun `an American reader gets football fields and trips around the Earth`() {
        assertEquals("That's 2.0 football pitches", yardstickText(210.0, Locale.UK))
        assertEquals("That's 2.0 football fields", yardstickText(182.88, Locale.US))
        assertEquals("That's 1.0 trip around the Earth", yardstickText(40_075_000.0, Locale.US))
    }

    @Test
    fun `a duration reads in seconds, then minutes, then hours`() {
        assertEquals("12.5 s", durationText(12.5, Locale.UK))
        assertEquals("1 min 0 s", durationText(59.96, Locale.UK))
        assertEquals("3 min 20 s", durationText(200.0, Locale.UK))
        assertEquals("2 h 5 min", durationText(7_500.0, Locale.UK))
    }
}
