package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class RingGeometryTest {
    private val fullSize = 64f
    private val margin = 8f
    private val phoneSide = 411f
    private val sides = (120..600 step 10).map(Int::toFloat)
    private val counts = 0..30

    private fun assertOffset(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals("x", expected.first, actual.first, 1e-6f)
        assertEquals("y", expected.second, actual.second, 1e-6f)
    }

    /** How large ring icons were before the ring could grow: a fixed radius, and past six icons all shrinking in step. */
    private fun fixedRingIconSize(side: Float, count: Int): Float {
        val clearance = min(RING_RADIUS_FRACTION - EMBLEM_FRACTION / 2, 0.5f - RING_RADIUS_FRACTION)
        val largest = min(fullSize, 2 * clearance * side)
        if (count <= 6) return largest
        return largest * (sin(PI / count) / sin(PI / 6)).toFloat()
    }

    @Test
    fun `the first slot is at the top and the rest go clockwise`() {
        assertOffset(0f to -1f, ringSlotOffset(0, 4))
        assertOffset(1f to 0f, ringSlotOffset(1, 4))
        assertOffset(0f to 1f, ringSlotOffset(2, 4))
        assertOffset(-1f to 0f, ringSlotOffset(3, 4))
    }

    @Test
    fun `a few icons keep full size on the usual ring`() {
        (0..7).forEach {
            assertEquals(RingLayout(phoneSide * RING_RADIUS_FRACTION, fullSize), ringLayout(fullSize, phoneSide, it, margin))
        }
    }

    @Test
    fun `a crowded ring grows toward the edges before its icons shrink`() {
        val eight = ringLayout(fullSize, phoneSide, 8, margin)
        assertEquals(fullSize, eight.iconSize, 1e-3f)
        assertTrue("$eight", eight.radius > phoneSide * RING_RADIUS_FRACTION)

        val eleven = ringLayout(fullSize, phoneSide, 11, margin)
        assertEquals(phoneSide * MAX_RING_RADIUS_FRACTION, eleven.radius)
        assertEquals(48f, eleven.iconSize, 1f)
    }

    @Test
    fun `moving the ring never makes icons smaller than on the usual ring`() {
        sides.forEach { side ->
            counts.forEach { count ->
                val usual = ringIconSize(fullSize, side, count, side * RING_RADIUS_FRACTION, margin)
                assertTrue("$count icons on $side", ringLayout(fullSize, side, count, margin).iconSize >= usual - 1e-3f)
            }
        }
    }

    @Test
    fun `icons are never smaller than on the fixed ring, bar the margin it did not keep`() {
        sides.forEach { side ->
            counts.forEach { count ->
                val fixed = fixedRingIconSize(side, count)
                val size = ringLayout(fullSize, side, count, margin).iconSize
                assertTrue("$count icons on $side: $size, fixed $fixed", size >= fixed - 2 * margin - 1e-3f)
                // Where the fixed ring's icons kept the margin anyway, these are at least as large.
                if (fixed <= side * (1 - 2 * RING_RADIUS_FRACTION) - 2 * margin) {
                    assertTrue("$count icons on $side: $size, fixed $fixed", size >= fixed - 1e-3f)
                }
            }
        }
    }

    @Test
    fun `adding icons never grows them or pulls the ring in`() {
        sides.forEach { side ->
            counts.map { ringLayout(fullSize, side, it, margin) }.zipWithNext().forEach { (fewer, more) ->
                assertTrue("$fewer then $more on $side", more.iconSize <= fewer.iconSize + 1e-3f && more.radius >= fewer.radius - 1e-3f)
            }
        }
    }

    @Test
    fun `icons stay clear of the emblem, the page edge and each other on any page`() {
        sides.forEach { side ->
            counts.forEach { count ->
                val (radius, size) = ringLayout(fullSize, side, count, margin)
                assertTrue("$count icons on $side reach the emblem", radius - size / 2 >= side * EMBLEM_FRACTION / 2 - 1e-3f)
                assertTrue("$count icons on $side come within the margin", radius + size / 2 <= side / 2 - margin + 1e-3f)
                if (count > 1) {
                    assertTrue("$count icons on $side overlap", size <= 2 * radius * sin(PI / count).toFloat())
                }
            }
        }
    }
}
