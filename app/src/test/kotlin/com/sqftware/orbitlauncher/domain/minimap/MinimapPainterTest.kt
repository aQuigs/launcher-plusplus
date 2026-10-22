package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.Pixels
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class MinimapPainterTest {
    private val centre = LatLon(40.7536, -73.9832)
    private val tree = solid(1, 1, TREE)
    private val npc = solid(3, 3, NPC)
    private val icons = MapIcon.entries.associateWith { solid(3, 3, ICON or it.ordinal) }
    private val painter = MinimapPainter(MinimapSprites(trees = listOf(tree), npc = npc, icons = icons))

    @Test
    fun paintsAnOpaqueDiscOnAClearSquare() {
        val disc = painter.paintDisc(MapScene(centre))
        assertEquals(MINIMAP_SIZE, disc.width)
        assertEquals(MINIMAP_SIZE, disc.height)
        assertEquals(0, disc[0, 0])
        assertEquals(0, disc[MINIMAP_SIZE - 1, MINIMAP_SIZE - 1])
        assertEquals(0, disc[10, 10])
        for ((x, y) in listOf(0 to 76, 76 to 0, 151 to 76, 76 to 151, 76 to 76, 30 to 30)) {
            assertEquals("($x, $y)", 0xFF, disc[x, y] ushr 24)
        }
    }

    @Test
    fun paintsBareGroundAsGrassWithNoWallsAndNoSelfMarker() {
        val disc = MinimapPainter(MinimapSprites()).paintDisc(MapScene(centre)).argb
        val ground = disc.filter { it ushr 24 == 0xFF }.toSet()
        assertTrue(ground.all { it shr 8 and 0xFF == 0x8F && it and 0xFF == 0x24 })
        assertTrue(WALL !in ground)
    }

    @Test
    fun wallsOnlyTheOutsideOfTouchingBuildings() {
        // Two footprints sharing an edge at x = 70 make one structure: the edge between them is floor, not wall.
        val disc = MinimapPainter(MinimapSprites()).paintDisc(
            MapScene(centre, buildings = listOf(Building(rect(40, 40, 70, 60), seed = 1), Building(rect(70, 40, 100, 60), seed = 2))),
        )
        assertEquals(WALL, disc[40, 50])
        assertEquals(WALL, disc[99, 50])
        assertEquals(WALL, disc[55, 40])
        assertEquals(WALL, disc[85, 59])
        for (y in 41..58) {
            assertTrue("seam at y $y", disc[69, y] != WALL && disc[70, y] != WALL)
        }
        assertEquals(disc[60, 50], disc[80, 50])
    }

    @Test
    fun givesAStructureAtMostTwoDoorsOfOneTileEach() {
        val building = Building(rect(40, 40, 110, 60), seed = 3)
        val entered = (45..105 step 10).map { Poi(local(it, 41), door = true) }
        val disc = MinimapPainter(MinimapSprites()).paintDisc(MapScene(centre, buildings = listOf(building), pois = entered)).argb
        assertEquals(MAX_DOORS * DOOR_PX, disc.count { it == DOOR })

        val one = MinimapPainter(MinimapSprites()).paintDisc(MapScene(centre, buildings = listOf(building), pois = entered.take(1))).argb
        assertEquals(DOOR_PX, one.count { it == DOOR })
    }

    @Test
    fun spacesWoodlandTreesApart() {
        val wood = Area(Ground.Grass, listOf(rect(0, 0, 152, 152)), order = 0, woodland = true)
        val trees = MinimapPainter(MinimapSprites(trees = listOf(tree))).paintDisc(MapScene(centre, areas = listOf(wood))).spots(TREE)
        assertTrue("${trees.size} trees", trees.size > 20)
        assertMinimumSpacing(trees, TREE_SPACING_PX)
    }

    @Test
    fun decoratesTreelessGrassWithWiderSpacedTrees() {
        val trees = MinimapPainter(MinimapSprites(trees = listOf(tree))).paintDisc(MapScene(centre)).spots(TREE)
        assertTrue("${trees.size} trees", trees.size > 5)
        assertMinimumSpacing(trees, DECOR_SPACING_PX)
    }

    @Test
    fun keepsTreesOffRoadsAndFloors() {
        val road = Way(WayKind.Road, 200.0, listOf(Local(-200.0, 0.0), Local(200.0, 0.0)))
        val disc = MinimapPainter(MinimapSprites(trees = listOf(tree))).paintDisc(MapScene(centre, ways = listOf(road)))
        assertTrue(disc.spots(TREE).isEmpty())
    }

    @Test
    fun capsIconsSpacesThemAndLimitsEachKind() {
        val spread = MapIcon.entries.take(8).mapIndexed { i, icon -> Poi(local(30 + (i % 4) * 30, 40 + (i / 4) * 60), icon) }
        val shown = painter.iconsOn(MapScene(centre, pois = spread))
        assertEquals(MapIcon.entries.take(MAX_ICONS), MapIcon.entries.filter { it in shown })

        val banks = (0 until 4).map { Poi(local(30 + it * 30, 40), MapIcon.Bank) }
        assertEquals(List(MAX_PER_ICON) { MapIcon.Bank }, painter.iconsOn(MapScene(centre, pois = banks)))

        val crowded = listOf(Poi(local(40, 40), MapIcon.Bank), Poi(local(50, 40), MapIcon.Altar))
        assertEquals(listOf(MapIcon.Bank), painter.iconsOn(MapScene(centre, pois = crowded)))
    }

    @Test
    fun leavesIconsOffTheDiscAndCentresEachOnItsPlace() {
        val map = painter.paint(MapScene(centre, pois = listOf(Poi(local(40, 60), MapIcon.Bank))))
        assertTrue(map.disc.argb.none { it == ICON or MapIcon.Bank.ordinal })
        val bank = map.icons.single()
        assertEquals(40.0, bank.x.toDouble(), 1.0)
        assertEquals(60.0, bank.y.toDouble(), 1.0)
    }

    @Test
    fun standsPeopleOnlyWhereSomeoneIsMapped() {
        assertTrue(painter.paintDisc(MapScene(centre)).argb.none { it == NPC })
        val people = listOf(Poi(local(40, 40), npc = true), Poi(local(110, 110), npc = true), Poi(local(40, 110)))
        assertEquals(2 * 9, painter.paintDisc(MapScene(centre, pois = people)).argb.count { it == NPC })
        val many = (0 until 30).map { Poi(local(30 + (it % 6) * 15, 40 + (it / 6) * 15), npc = true) }
        assertEquals(MAX_NPCS * 9, painter.paintDisc(MapScene(centre, pois = many)).argb.count { it == NPC })
    }

    @Test
    fun pavesBuiltUpPlaces() {
        val blocks = (0 until 4).flatMap { i -> (0 until 4).map { j -> Building(rect(20 + i * 30, 20 + j * 30, 40 + i * 30, 40 + j * 30), seed = (i * 4 + j).toLong()) } }
        val disc = MinimapPainter(MinimapSprites()).paintDisc(MapScene(centre, buildings = blocks))
        assertEquals(PLAZA, disc[17, 76])
    }

    @Test
    fun paintsTheSameSceneTheSameWay() {
        val tiles = OpenMapTiles.tilesFor(centre).associateWith { OpenMapTiles.decode(fixture("${it.z}-${it.x}-${it.y}.mvt")) }
        val scene = OpenMapTiles.scene(centre, tiles)
        val disc = painter.paintDisc(scene).argb
        assertArrayEquals(disc, painter.paintDisc(scene).argb)
        assertTrue(disc.any { it == WALL })
        assertTrue(disc.any { it == ROAD })
    }

    private fun MinimapPainter.paintDisc(scene: MapScene) = paint(scene).disc

    private fun MinimapPainter.iconsOn(scene: MapScene) = paint(scene).icons.map { mark -> icons.entries.first { it.value === mark.image }.key }

    private fun Pixels.spots(colour: Int) = argb.indices.filter { argb[it] == colour }.map { (it % MINIMAP_SIZE).toDouble() to (it / MINIMAP_SIZE).toDouble() }

    // Spots are whole pixels of positions that were not, so two may sit up to a pixel's diagonal closer than placed.
    private fun assertMinimumSpacing(spots: List<Pair<Double, Double>>, spacing: Double) {
        for (a in spots.indices) for (b in a + 1 until spots.size) {
            val d = hypot(spots[a].first - spots[b].first, spots[a].second - spots[b].second)
            assertTrue("$d apart", d >= spacing - 1.5)
        }
    }

    // A pixel position on the disc as ground metres around its centre.
    private fun local(x: Int, y: Int) = Local((x + 0.5 - 76.5) * METRES_PER_PIXEL, (76.5 - y - 0.5) * METRES_PER_PIXEL)

    private fun rect(x0: Int, y0: Int, x1: Int, y1: Int): Polygon {
        fun corner(x: Int, y: Int) = Local((x - 76.5) * METRES_PER_PIXEL, (76.5 - y) * METRES_PER_PIXEL)
        return listOf(listOf(corner(x0, y0), corner(x1, y0), corner(x1, y1), corner(x0, y1)))
    }

    private fun solid(w: Int, h: Int, colour: Int) = Pixels(w, h, IntArray(w * h) { colour })

    private companion object {
        const val TREE = 0xFFFF00FF.toInt()
        const val NPC = 0xFF00FFFF.toInt()
        const val ICON = 0xFF0000E0.toInt()
        const val WALL = 0xFFEEEEEE.toInt()
        const val DOOR = 0xFFEE0000.toInt()
        const val PLAZA = 0xFF6F6A5E.toInt()
        const val ROAD = 0xFF85755A.toInt()
    }
}
