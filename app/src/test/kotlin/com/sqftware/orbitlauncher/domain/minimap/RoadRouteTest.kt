package com.sqftware.orbitlauncher.domain.minimap

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadRouteTest {
    private class FixtureTiles : MapTiles {
        val asked = mutableListOf<TileId>()

        override suspend fun tile(id: TileId): ByteArray? {
            asked += id
            return runCatching { fixture("${id.z}-${id.x}-${id.y}.mvt") }.getOrNull()
        }

        override suspend fun discard(id: TileId) = Unit
    }

    // The fixture tiles keep only a few streets round Bryant Park, among them Sixth Avenue crossing 42nd Street.
    private val bryantPark = LatLon(40.7536, -73.9832)
    private val upSixth = LatLon(40.7560, -73.9834)
    private val along42nd = LatLon(40.7542, -73.9823)

    private fun length(route: List<LatLon>) = route.zipWithNext { a, b -> WebMercator.groundDistance(a, b) }.sum()

    @Test
    fun routesByRoadFromEndToEnd() = runBlocking {
        val route = RoadRouter(FixtureTiles()).route(upSixth, along42nd)!!

        assertEquals(upSixth, route.first())
        assertEquals(along42nd, route.last())
        val straight = WebMercator.groundDistance(upSixth, along42nd)
        assertTrue(length(route) in straight..straight * 2)
        assertTrue(route.size > 3)
    }

    @Test
    fun findsNoRouteToAQuestTooFarWithoutFetchingTiles() = runBlocking {
        val tiles = FixtureTiles()

        assertNull(RoadRouter(tiles).route(bryantPark, LatLon(bryantPark.lat + 0.05, bryantPark.lon)))
        assertEquals(emptyList<TileId>(), tiles.asked)
    }

    @Test
    fun findsNoRouteWhileATileIsMissing() = runBlocking {
        assertNull(RoadRouter(FixtureTiles()).route(bryantPark, LatLon(bryantPark.lat - 0.012, bryantPark.lon)))
    }

    private val tile = TileId(14, 4824, 6157)

    private fun at(x: Int, y: Int) = WebMercator.toLatLon(WebMercator.fromTile(tile, EXTENT, TilePoint(x, y)))

    private fun road(vararg points: TilePoint, brunnel: String? = null) =
        VectorFeature(GeometryType.Line, listOfNotNull("class" to "minor", brunnel?.let { "brunnel" to it }).toMap(), listOf(points.toList()))

    private fun path(from: TilePoint, to: TilePoint, vararg roads: VectorFeature): List<Int>? {
        val grid = RoadGrid.around(at(from.x, from.y), at(to.x, to.y))
        grid.lay(mapOf(tile to VectorLayer("transportation", EXTENT, roads.toList())))
        return grid.path(grid.cellOf(at(from.x, from.y)), grid.cellOf(at(to.x, to.y)))
    }

    // A street running south from the user, and a road crossing it east to the quest.
    private val user = TilePoint(2048, 1500)
    private val quest = TilePoint(2600, 2048)
    private val street = road(user, TilePoint(2048, 2600))

    @Test
    fun turnsWhereRoadsMeet() {
        assertNotNull(path(user, quest, street, road(TilePoint(1500, 2048), quest)))
    }

    @Test
    fun neverTurnsOntoABridgeOverTheRoad() {
        assertNull(path(user, quest, street, road(TilePoint(1500, 2048), quest, brunnel = "bridge")))
    }

    @Test
    fun neverLeavesABridgeWhereItCarriesOnAsAnotherWay() {
        val west = road(TilePoint(1500, 2048), TilePoint(2048, 2048), brunnel = "bridge")
        val east = road(TilePoint(2048, 2048), quest, brunnel = "bridge")

        assertNull(path(user, quest, street, west, east))
    }

    @Test
    fun goesOntoABridgeFromTheRoadItCarriesOn() {
        val approach = road(TilePoint(1000, 2048), TilePoint(1500, 2048))

        assertNotNull(path(TilePoint(1000, 2048), quest, approach, road(TilePoint(1500, 2048), quest, brunnel = "bridge")))
    }

    @Test
    fun dropsPointsThatLieOnTheLineBetweenTheirNeighbors() {
        val points = listOf(Mercator(0.0, 0.0), Mercator(1.0, 0.1), Mercator(2.0, 0.0), Mercator(2.0, 5.0))

        assertEquals(listOf(points[0], points[2], points[3]), simplified(points, tolerance = 0.5))
    }

    private companion object {
        const val EXTENT = 4096
    }
}
