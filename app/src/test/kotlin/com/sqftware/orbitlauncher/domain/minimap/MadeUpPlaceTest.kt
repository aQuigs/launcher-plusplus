package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.Pixels
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class MadeUpPlaceTest {
    private val m = METRES_PER_PIXEL
    private val route = Route(listOf(Local(0.0, 0.0), Local(100 * m, 0.0), Local(100 * m, 50 * m)))
    private val ground = MapScene(
        centre = LatLon(0.0, 0.0),
        areas = listOf(Area(Ground.Grass, listOf(listOf(listOf(Local(-200.0, -200.0), Local(200.0, -200.0), Local(200.0, 0.0), Local(-200.0, 0.0)))), order = 0, woodland = true)),
        ways = listOf(Way(WayKind.Road, 8.0, listOf(Local(-200.0, 20.0), Local(200.0, 20.0)))),
        buildings = listOf(Building(listOf(listOf(Local(10.0, 30.0), Local(40.0, 30.0), Local(40.0, 50.0), Local(10.0, 50.0))), seed = 1)),
    )
    private val pacer = Walker(Dot.Npc, Route(listOf(Local(0.0, 10.0), Local(20.0, 10.0))), pace = 0.5, start = 4.0)
    private val place = MadeUpPlace(ground, route, listOf(pacer))

    @Test
    fun walksRoundARouteAndBackToItsStart() {
        assertEquals(150 * m + Math.hypot(100 * m, 50 * m), route.length, 1e-9)
        assertNear(Local(40 * m, 0.0), route.at(40 * m))
        assertNear(Local(100 * m, 20 * m), route.at(120 * m))
        assertNear(route.at(10.0), route.at(route.length + 10.0))
    }

    @Test
    fun walksATwoPointRouteThereAndBack() {
        val there = Route(listOf(Local(0.0, 0.0), Local(10.0, 0.0)))
        assertEquals(20.0, there.length, 1e-9)
        assertNear(Local(4.0, 0.0), there.at(16.0))
    }

    @Test
    fun putsThePlayerOnThePixelTheyStandIn() {
        assertEquals(Local(0.5 * m, 0.5 * m), place.at(0.0))
        assertEquals(Local(40.5 * m, 0.5 * m), place.at(40.2 * m))
    }

    @Test
    fun movesThePlaceAndItsWalkersRoundThePlayer() {
        val walked = 40.0 * m
        val at = place.at(walked)
        val scene = place.scene(walked)
        assertNear(Local(10.0 - at.east, 30.0 - at.north), scene.buildings.single().polygon[0][0])
        assertNear(Local(-200.0 - at.east, 20.0 - at.north), scene.ways.single().points[0])
        val pacing = pacer.route.at(4.0 + walked * 0.5)
        assertEquals(Dot.Npc, scene.figures.single().dot)
        assertNear(Local(pacing.east - at.east, pacing.north - at.north), scene.figures.single().at)
    }

    @Test
    fun aStepMovesTheWholeMapOnePixelTheOtherWay() {
        val painter = MinimapPainter(MinimapSprites(trees = listOf(Pixels(1, 1, intArrayOf(TREE)))))
        // Along the route's first leg east, then up its second leg north.
        for (walked in listOf(40.0 * m, 110.0 * m)) {
            val here = place.at(walked)
            val there = place.at(walked + m)
            val dx = ((there.east - here.east) / m).roundToInt()
            val dy = -((there.north - here.north) / m).roundToInt()
            val before = painter.paint(place.scene(walked).copy(figures = emptyList()), MapFrame.MINIMAP).ground
            val after = painter.paint(place.scene(walked + m).copy(figures = emptyList()), MapFrame.MINIMAP).ground
            for (y in 40 until 112) for (x in 40 until 112) assertEquals("$walked: ($x, $y)", before[x + dx, y + dy], after[x, y])
        }
    }

    private fun assertNear(expected: Local, actual: Local) {
        assertEquals(expected.east, actual.east, 1e-5)
        assertEquals(expected.north, actual.north, 1e-5)
    }

    private companion object {
        const val TREE = 0xFF00FF00.toInt()
    }
}
