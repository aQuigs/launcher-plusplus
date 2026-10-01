package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFaceTest {
    @Test
    fun `the hands point where a watch's would`() {
        assertEquals(0f, hourHandDegrees(0))
        assertEquals(0f, minuteHandDegrees(0))
        // 10:19: the hour hand a little past ten, the minute hand just short of four.
        assertEquals(309.5f, hourHandDegrees(10 * 60 + 19))
        assertEquals(114f, minuteHandDegrees(10 * 60 + 19))
        // Afternoon hours go round the same face as the morning's.
        assertEquals(hourHandDegrees(3 * 60 + 30), hourHandDegrees(15 * 60 + 30))
    }
}
