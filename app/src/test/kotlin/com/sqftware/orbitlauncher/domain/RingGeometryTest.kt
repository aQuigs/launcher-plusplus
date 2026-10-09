package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val gap = 4f
    private val line = 16f
    private val air = 6f

    /** Names as wide as phone labels run: short ones, and a few long enough to need two lines on a crowded ring. */
    private val labels = listOf(
        NameWidths(48f),
        NameWidths(44f),
        NameWidths(55f),
        NameWidths(64f, 34f),
        NameWidths(34f),
        NameWidths(30f),
        NameWidths(66f),
        NameWidths(46f),
        NameWidths(57f, 26f),
        NameWidths(60f),
        NameWidths(59f),
        NameWidths(35f),
        NameWidths(43f),
        null,
        NameWidths(130f, 60f),
    )

    private fun namesOf(count: Int, at: (Int, Int) -> Pair<Float, Float> = ::ringSlotOffset) =
        HangingNames(gap, line, air, List(count) { labels[it % labels.size] }, at)

    /** The box each name shows in, under its icon: as wide as it is if it fits, on the lines it takes, else cut to its room. */
    private fun shownBoxes(layout: RingLayout, names: HangingNames, count: Int): List<FloatArray?> = List(count) { i ->
        val name = names.widths[i] ?: return@List null
        val room = layout.names[i]
        val width = when {
            !room.fits -> room.width
            room.lines == 1 -> name.oneLine
            else -> name.twoLines
        }
        val (x, y) = names.at(i, count).let { (dx, dy) -> dx * layout.radius to dy * layout.radius }
        val top = y + layout.iconSize / 2 + gap
        floatArrayOf(x - width / 2, x + width / 2, top, top + room.lines * line)
    }

    private fun clearOfDisc(box: FloatArray, cx: Float, cy: Float, radius: Float): Float =
        hypot(cx - cx.coerceIn(box[0], box[1]), cy - cy.coerceIn(box[2], box[3])) - radius

    private fun clearOfBox(a: FloatArray, b: FloatArray): Float {
        val dx = maxOf(b[0] - a[1], a[0] - b[1])
        val dy = maxOf(b[2] - a[3], a[2] - b[3])
        return if (dx < 0 && dy < 0) maxOf(dx, dy) else hypot(maxOf(dx, 0f), maxOf(dy, 0f))
    }

    @Test
    fun `names show clear of the icons, each other, the emblem and the page, on any page and in a folder`() {
        sides.filter { it >= 200f }.forEach { side ->
            (1..24).forEach { count ->
                listOf(::ringSlotOffset, ::folderSlotOffset).forEach { at ->
                    val names = namesOf(count) { i, c -> at(i, c, 0.0) }
                    val layout = ringLayout(fullSize, side, count, margin, names)
                    val boxes = shownBoxes(layout, names, count)
                    val slots = List(count) { names.at(it, count).let { (x, y) -> x * layout.radius to y * layout.radius } }
                    boxes.forEachIndexed { i, box ->
                        if (box == null || box[1] - box[0] < 1e-3f) return@forEachIndexed
                        val what = "$count named icons on $side, name $i"
                        assertTrue("$what reaches the emblem", clearOfDisc(box, 0f, 0f, side * EMBLEM_FRACTION / 2) >= air - 1e-2f)
                        assertTrue("$what leaves the page", box[0] >= -side / 2 - 1e-2f && box[1] <= side / 2 + 1e-2f && box[3] <= side / 2 + 1e-2f)
                        slots.forEachIndexed { j, (x, y) ->
                            if (j == i) return@forEachIndexed
                            assertTrue("$what meets icon $j", clearOfDisc(box, x, y, layout.iconSize / 2) >= air - 1e-2f)
                            boxes[j]?.let { assertTrue("$what meets name $j", clearOfBox(box, it) >= air - 1e-2f) }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `names leave icons at least 85 percent of their size without them, and never more`() {
        sides.forEach { side ->
            counts.forEach { count ->
                val plain = ringLayout(fullSize, side, count, margin).iconSize
                val named = ringLayout(fullSize, side, count, margin, namesOf(count)).iconSize
                assertTrue("$count icons on $side: $named named, $plain without", named >= 0.85f * plain - 1e-2f && named <= plain + 1e-3f)
            }
        }
    }

    @Test
    fun `a ring whose names fit keeps its icons' size, drawn in at most as far as the bottom name needs`() {
        (1..6).forEach { count ->
            val plain = ringLayout(fullSize, phoneSide, count, margin)
            val named = ringLayout(fullSize, phoneSide, count, margin, namesOf(count))
            assertEquals("$count icons", plain.iconSize, named.iconSize)
            assertTrue("$count icons: $named", named.radius <= plain.radius && named.radius >= plain.radius - gap - line)
            assertTrue("$count icons", named.names.all { it.fits && it.lines == 1 })
        }
    }

    @Test
    fun `a long name at the ring's side that fits where the ring is keeps the icons their size`() {
        // A larger ring would give it less room, between the slot and the page's edge.
        val names = HangingNames(gap, line, air, listOf(NameWidths(40f), NameWidths(100f), NameWidths(40f), NameWidths(40f)))
        val plain = ringLayout(fullSize, phoneSide, 4, margin)
        val named = ringLayout(fullSize, phoneSide, 4, margin, names)
        assertEquals(plain.iconSize, named.iconSize)
        assertTrue("${named.names}", named.names.all(NameRoom::fits))
    }

    @Test
    fun `a name that fits nowhere does not cost the others theirs`() {
        val widths = listOf(NameWidths(300f)) + List(7) { if (it == 1) NameWidths(100f) else NameWidths(40f) }
        val named = ringLayout(fullSize, phoneSide, 8, margin, HangingNames(gap, line, air, widths))
        assertTrue("${named.names}", !named.names[0].fits && named.names.drop(1).all(NameRoom::fits))
    }

    @Test
    fun `a name too long for one line wraps onto two before it is cut`() {
        // At the foot of a ring of nine, with room below it but not across.
        val names = namesOf(9).let { it.copy(widths = it.widths.mapIndexed { i, name -> if (i == 4) labels.last() else name }) }
        val longest = ringLayout(fullSize, phoneSide, 9, margin, names).names[4]
        assertTrue("$longest", longest.fits && longest.lines == 2)
    }

    @Test
    fun `a move along the ring goes round it, one toward or away from the centre does not`() {
        // At the top of the ring, across is round it and up or down is in or out; at its side, the other way about.
        assertTrue(goesRound(0f, -100f, 10f, 2f))
        assertFalse(goesRound(0f, -100f, 2f, 10f))
        assertTrue(goesRound(100f, 0f, -2f, -10f))
        assertFalse(goesRound(100f, 0f, -10f, 2f))
    }

    @Test
    fun `the turn between two points is clockwise on screen and the short way round`() {
        assertEquals(PI.toFloat() / 2, turnBetween(0f, -1f, 1f, 0f), 1e-6f)
        assertEquals(-PI.toFloat() / 2, turnBetween(1f, 0f, 0f, -1f), 1e-6f)
        assertEquals(PI.toFloat() / 2, turnBetween(-1f, 0f, 0f, -5f), 1e-6f)
        assertEquals(0.2f, turnBetween(-1f, 0.1f, -1f, -0.1f), 0.01f)
    }

    @Test
    fun `only the finger's speed round the centre turns the ring`() {
        assertEquals(2f, turnRate(0f, -100f, 200f, 0f), 1e-6f)
        assertEquals(-2f, turnRate(0f, 100f, 200f, 0f), 1e-6f)
        assertEquals(0f, turnRate(0f, -100f, 0f, 500f), 1e-6f)
    }

    @Test
    fun `a spun ring rests on the whole turn nearest where it coasts to`() {
        val fullTurn = (2 * PI).toFloat()
        assertEquals(0f, restingTurn(0.4f), 0f)
        assertEquals(fullTurn, restingTurn(0.6f * fullTurn), 1e-5f)
        assertEquals(-2 * fullTurn, restingTurn(-2.3f * fullTurn), 1e-5f)
    }
}
