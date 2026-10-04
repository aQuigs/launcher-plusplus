package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

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
    fun `a folder's first slots sit either side of the top`() {
        val half = sqrt(0.5f)
        assertOffset(0f to -1f, folderSlotOffset(0, 1))
        assertOffset(-1f to 0f, folderSlotOffset(0, 2))
        assertOffset(1f to 0f, folderSlotOffset(1, 2))
        assertOffset(0f to 1f, folderSlotOffset(2, 3))
        listOf(-half to -half, half to -half, half to half, -half to half).forEachIndexed { index, corner ->
            assertOffset(corner, folderSlotOffset(index, 4))
        }
    }

    @Test
    fun `a few icons keep full size on the usual ring`() {
        (0..9).forEach {
            assertEquals(RingLayout(phoneSide * RING_RADIUS_FRACTION, fullSize), ringLayout(fullSize, phoneSide, it, margin))
        }
    }

    @Test
    fun `a crowded ring grows toward the edges before its icons shrink`() {
        val eleven = ringLayout(fullSize, phoneSide, 11, margin)
        assertEquals(fullSize, eleven.iconSize, 1e-3f)
        assertTrue("$eleven", eleven.radius > phoneSide * RING_RADIUS_FRACTION)

        val thirteen = ringLayout(fullSize, phoneSide, 13, margin)
        assertEquals(phoneSide * MAX_RING_RADIUS_FRACTION, thirteen.radius)
        assertEquals(55f, thirteen.iconSize, 1f)
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

    private val names = HangingNames(below = 20f, air = 4f)

    /** How far the box of [a]'s name, under its icon [size] across, keeps from [b]'s icon and from [b]'s name; negative where they overlap. */
    private fun nameClearance(a: Pair<Float, Float>, b: Pair<Float, Float>, size: Float): Float {
        val half = ringNameWidth(size) / 2
        fun nameBox(c: Pair<Float, Float>) = floatArrayOf(c.first - half, c.first + half, c.second + size / 2, c.second + size / 2 + names.below)
        val box = nameBox(a)
        val x = b.first.coerceIn(box[0], box[1])
        val y = b.second.coerceIn(box[2], box[3])
        val fromIcon = hypot(b.first - x, b.second - y) - size / 2
        val other = nameBox(b)
        val dx = maxOf(other[0] - box[1], box[0] - other[1])
        val dy = maxOf(other[2] - box[3], box[2] - other[3])
        val fromName = if (dx < 0 && dy < 0) maxOf(dx, dy) else hypot(maxOf(dx, 0f), maxOf(dy, 0f))
        return minOf(fromIcon, fromName)
    }

    @Test
    fun `names stay clear of the emblem, the page edge, other icons and other names on any page, however the ring turns`() {
        sides.forEach { side ->
            counts.forEach { count ->
                val (radius, size) = ringLayout(fullSize, side, count, margin, names)
                // A page too small for the names has no room for icons either.
                if (size == 0f) return@forEach
                assertTrue("$count named icons on $side reach the emblem", radius - size / 2 - names.below >= side * EMBLEM_FRACTION / 2 - 1e-3f)
                assertTrue("$count named icons on $side come within the margin", radius + size / 2 + names.below <= side / 2 - margin + 1e-3f)
                listOf(0.0, 0.3, PI / maxOf(count, 1)).forEach { turn ->
                    val slots = List(count) { ringSlotOffset(it, count, turn).let { (x, y) -> x * radius to y * radius } }
                    slots.forEachIndexed { i, a ->
                        slots.forEachIndexed { j, b ->
                            if (i != j) assertTrue("$count named icons on $side, $i by $j", nameClearance(a, b, size) >= names.air - 1e-2f)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `names cost a crowded ring only the room they need`() {
        // Taking a name's whole line out of the gap neighbours keep left 12 icons under two thirds of their size.
        (10..16).forEach { count ->
            val plain = ringLayout(fullSize, phoneSide, count, margin).iconSize
            val named = ringLayout(fullSize, phoneSide, count, margin, names).iconSize
            assertTrue("$count icons: $named named, $plain without", named >= 0.7f * plain)
        }
    }
}
