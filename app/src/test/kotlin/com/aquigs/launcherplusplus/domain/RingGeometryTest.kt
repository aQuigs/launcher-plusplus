package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class RingGeometryTest {
    private val fullSize = 64f
    private val roomySide = 400f
    private val sides = listOf(120f, 180f, 229f, 400f, 1000f)

    private fun assertOffset(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals("x", expected.first, actual.first, 1e-6f)
        assertEquals("y", expected.second, actual.second, 1e-6f)
    }

    @Test
    fun `the first slot is at the top and the rest go clockwise`() {
        assertOffset(0f to -1f, ringSlotOffset(0, 4))
        assertOffset(1f to 0f, ringSlotOffset(1, 4))
        assertOffset(0f to 1f, ringSlotOffset(2, 4))
        assertOffset(-1f to 0f, ringSlotOffset(3, 4))
    }

    @Test
    fun `on a roomy page up to six icons keep full size`() {
        (0..FULL_SIZE_RING_SLOTS).forEach { assertEquals(fullSize, ringIconSize(fullSize, roomySide, it)) }
    }

    @Test
    fun `past six every extra icon shrinks them all`() {
        val sizes = (FULL_SIZE_RING_SLOTS..24).map { ringIconSize(fullSize, roomySide, it) }

        assertTrue(sizes.toString(), sizes.zipWithNext().all { (bigger, smaller) -> smaller < bigger })
    }

    @Test
    fun `icons stay clear of the emblem and the page edge on any page`() {
        sides.forEach { side ->
            val radius = side * RING_RADIUS_FRACTION
            (1..24).forEach { count ->
                val half = ringIconSize(fullSize, side, count) / 2
                assertTrue("$count icons on $side reach the emblem", radius - half >= side * EMBLEM_FRACTION / 2 - 1e-3f)
                assertTrue("$count icons on $side cross the edge", radius + half <= side / 2 + 1e-3f)
            }
        }
    }

    @Test
    fun `neighbouring icons never overlap on any page`() {
        sides.forEach { side ->
            (2..24).forEach { count ->
                val distance = 2 * side * RING_RADIUS_FRACTION * sin(PI / count).toFloat()
                assertTrue("$count icons on $side overlap", ringIconSize(fullSize, side, count) <= distance)
            }
        }
    }
}
