package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.Pixels
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.hypot
import kotlin.math.roundToInt

class MadeUpPlacesTest {
    private val m = METRES_PER_PIXEL
    private val painter = MinimapPainter(
        MinimapSprites(trees = listOf(Pixels(1, 1, intArrayOf(TREE))), icons = MapIcon.entries.associateWith { Pixels(1, 1, intArrayOf(TREE)) }),
        MapStyle.OldSchool,
    )

    @Test
    fun walksEachPlaceInTurn() {
        val (first, second) = MADE_UP_TOUR.places
        assertEquals(first.scene(5.0), MADE_UP_TOUR.scene(5.0))
        assertEquals(second.scene(5.0), MADE_UP_TOUR.scene(first.length + 5.0))
    }

    // The painter decides some things per view, such as which icons to keep, so a place laid out carelessly would
    // change in the middle of the disc as the player walks: only its rim may.
    @Test
    fun aStepChangesNothingInTheMiddleOfAPlaceButWhereItIs() {
        for ((p, place) in MADE_UP_TOUR.places.withIndex()) {
            var before = paint(place, 0.0)
            for (step in 1..(place.length / m).toInt()) {
                val here = place.at((step - 1) * m)
                val there = place.at(step * m)
                val dx = ((there.east - here.east) / m).roundToInt()
                val dy = -((there.north - here.north) / m).roundToInt()
                val after = paint(place, step * m)
                for (y in 0 until MINIMAP_SIZE) for (x in 0 until MINIMAP_SIZE) {
                    if (inMiddle(x, y)) assertEquals("place $p, step $step at ($x, $y)", before.ground[x + dx, y + dy], after.ground[x, y])
                }
                val shifted = before.icons.map { it.x.roundToInt() - dx to it.y.roundToInt() - dy }.filter { (x, y) -> inMiddle(x, y) }.toSet()
                assertEquals("place $p, step $step icons", shifted, after.icons.map { it.x.roundToInt() to it.y.roundToInt() }.filter { (x, y) -> inMiddle(x, y) }.toSet())
                before = after
            }
        }
    }

    // Without the walkers, whose moving is the point.
    private fun paint(place: MadeUpPlace, walked: Double) = painter.paint(place.scene(walked).copy(figures = emptyList()), MapFrame.MINIMAP)

    private fun inMiddle(x: Int, y: Int) = hypot(x - MINIMAP_SIZE / 2.0, y - MINIMAP_SIZE / 2.0) < MIDDLE_PX

    private companion object {
        const val TREE = 0xFF00FF00.toInt()

        /** How far from the centre the disc must stay put: the rim beyond is where icons come in and go. */
        const val MIDDLE_PX = 56
    }
}
