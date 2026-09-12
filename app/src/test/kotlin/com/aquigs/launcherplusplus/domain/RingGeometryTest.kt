package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class RingGeometryTest {
    @Test
    fun `slots start at the top and go round evenly`() {
        assertEquals(0.0, ringSlotAngle(0, 4), 1e-9)
        assertEquals(PI / 2, ringSlotAngle(1, 4), 1e-9)
        assertEquals(3 * PI / 2, ringSlotAngle(3, 4), 1e-9)
    }

    @Test
    fun `up to six icons keep full size`() {
        (0..FULL_SIZE_RING_SLOTS).forEach { assertEquals(1f, ringIconScale(it)) }
    }

    @Test
    fun `past six every extra icon shrinks them all`() {
        val scales = (FULL_SIZE_RING_SLOTS..24).map(::ringIconScale)

        assertTrue(scales.toString(), scales.zipWithNext().all { (bigger, smaller) -> smaller < bigger })
    }

    @Test
    fun `shrunk icons keep the spacing of a full ring of six`() {
        fun neighbourDistance(count: Int) = 2 * sin(PI / count)

        (FULL_SIZE_RING_SLOTS + 1..24).forEach { count ->
            assertEquals(neighbourDistance(FULL_SIZE_RING_SLOTS), neighbourDistance(count) / ringIconScale(count), 1e-6)
        }
    }
}
