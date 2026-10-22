package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMapTilesTest {
    private val id = TileId(14, 4824, 6157)
    private val centre = toLatLon(WebMercator.fromTile(id, EXTENT, TilePoint(2048, 2048)))

    private fun scene(vararg layers: Pair<String, List<VectorFeature>>) =
        OpenMapTiles.scene(centre, mapOf(id to VectorTile(layers.associate { (name, features) -> name to VectorLayer(name, EXTENT, features) })))

    private fun square(tags: Map<String, Any>, size: Int = 20) = VectorFeature(
        GeometryType.Polygon,
        tags,
        listOf(listOf(TilePoint(2040, 2040), TilePoint(2040 + size, 2040), TilePoint(2040 + size, 2040 + size), TilePoint(2040, 2040 + size))),
    )

    private fun line(tags: Map<String, Any>) =
        VectorFeature(GeometryType.Line, tags, listOf(listOf(TilePoint(2000, 2048), TilePoint(2100, 2048))))

    private fun point(tags: Map<String, Any>, at: TilePoint = TilePoint(2050, 2050)) =
        VectorFeature(GeometryType.Point, tags, listOf(listOf(at)))

    @Test
    fun scalesAPolarCentreAsTheEdgeOfTheMapItIsDrawnAt() {
        val top = TileId(14, 8192, 0)
        val tile = VectorTile(mapOf("landcover" to VectorLayer("landcover", EXTENT, listOf(square(mapOf("class" to "grass"))))))
        fun polygons(lat: Double) = OpenMapTiles.scene(LatLon(lat, 0.0), mapOf(top to tile)).areas.single().polygons

        assertEquals(polygons(85.05112878), polygons(90.0))
    }

    @Test
    fun mapsGroundLayersAndClasses() {
        val areas = scene(
            "landcover" to listOf(square(mapOf("class" to "wood")), square(mapOf("class" to "grass", "subclass" to "garden")), square(mapOf("class" to "sand")), square(mapOf("class" to "rock"))),
            "landuse" to listOf(square(mapOf("class" to "pitch")), square(mapOf("class" to "retail")), square(mapOf("class" to "residential"))),
            "water" to listOf(square(mapOf("class" to "lake")), square(mapOf("class" to "river", "brunnel" to "tunnel"))),
        ).areas

        assertEquals(
            listOf(Ground.Grass, Ground.Grass, Ground.Sand, Ground.Pitch, Ground.Plaza, Ground.Water).sorted(),
            areas.map { it.ground }.sorted(),
        )
        assertTrue(areas.single { it.woodland }.ground == Ground.Grass)
        assertTrue(areas.single { it.ground == Ground.Plaza }.builtUpOnly)
    }

    @Test
    fun mapsTransportationToWaysOfTheirWidth() {
        val ways = scene(
            "transportation" to listOf(
                line(mapOf("class" to "secondary")),
                line(mapOf("class" to "primary", "ramp" to 1L)),
                line(mapOf("class" to "service", "service" to "parking_aisle")),
                line(mapOf("class" to "path", "subclass" to "pedestrian")),
                line(mapOf("class" to "path", "subclass" to "footway")),
                line(mapOf("class" to "rail", "subclass" to "rail")),
                line(mapOf("class" to "transit", "subclass" to "subway")),
                line(mapOf("class" to "minor", "brunnel" to "tunnel")),
                line(mapOf("class" to "minor", "layer" to -1L)),
                line(mapOf("class" to "ferry")),
            ),
        ).ways

        assertEquals(
            listOf(WayKind.Road to 12.0, WayKind.Road to 8.0, WayKind.Parking to 5.5, WayKind.Walk to 6.0, WayKind.Path to 3.0, WayKind.Rail to 0.0),
            ways.map { it.kind to it.widthM },
        )
    }

    @Test
    fun mapsPedestrianAreasAndPiers() {
        val areas = scene(
            "transportation" to listOf(square(mapOf("class" to "path", "subclass" to "pedestrian")), square(mapOf("class" to "pier"))),
        ).areas
        assertEquals(listOf(Ground.Plaza, Ground.Pier), areas.map { it.ground })
    }

    @Test
    fun makesEachBuildingPolygonABuildingAndSkipsLiftedParts() {
        val two = VectorFeature(
            GeometryType.Polygon,
            emptyMap(),
            listOf(
                listOf(TilePoint(2000, 2000), TilePoint(2010, 2000), TilePoint(2010, 2010), TilePoint(2000, 2010)),
                listOf(TilePoint(2050, 2050), TilePoint(2060, 2050), TilePoint(2060, 2060), TilePoint(2050, 2060)),
            ),
        )
        val buildings = scene("building" to listOf(two, square(mapOf("render_min_height" to 6.0)))).buildings
        assertEquals(2, buildings.size)
        assertTrue(buildings[0].seed != buildings[1].seed)
    }

    @Test
    fun leavesOutWhatIsFarFromTheCentre() {
        val far = VectorFeature(GeometryType.Polygon, mapOf("class" to "lake"), listOf(listOf(TilePoint(0, 0), TilePoint(10, 0), TilePoint(10, 10))))
        assertTrue(scene("water" to listOf(far)).areas.isEmpty())
        assertTrue(scene("poi" to listOf(point(mapOf("class" to "bank", "subclass" to "bank"), TilePoint(100, 100)))).pois.isEmpty())
    }

    @Test
    fun givesPlacesTheirIconsPeopleAndDoors() {
        val pois = scene(
            "poi" to listOf(
                point(mapOf("class" to "bank", "subclass" to "bank")),
                point(mapOf("class" to "place_of_worship", "subclass" to "christian")),
                point(mapOf("class" to "entrance", "subclass" to "subway_entrance")),
                point(mapOf("class" to "fast_food", "subclass" to "fast_food")),
                point(mapOf("class" to "shop", "subclass" to "convenience")),
                point(mapOf("class" to "clothing_store", "subclass" to "clothes")),
                point(mapOf("class" to "parking", "subclass" to "parking")),
                point(mapOf("class" to "office", "subclass" to "government", "name" to "Livonia City Hall")),
                point(mapOf("class" to "bench", "subclass" to "bench")),
                point(mapOf("class" to "bus", "subclass" to "bus_stop", "name" to "Church St & 5th Ave")),
            ),
        ).pois

        assertEquals(
            listOf(MapIcon.Bank, MapIcon.Altar, MapIcon.Transportation, MapIcon.KebabShop, MapIcon.GeneralStore, MapIcon.ClothesShop, null, MapIcon.QuestStart),
            pois.map { it.icon },
        )
        assertEquals(listOf(true, false, true, true, true, true, true, false), pois.map { it.npc })
        val parking = pois[6]
        assertFalse(parking.door)
        assertTrue(pois[0].door)
    }

    @Test
    fun takesAPointInATilesBufferFromItsOwnTileOnly() {
        assertTrue(scene("poi" to listOf(point(mapOf("class" to "bank", "subclass" to "bank"), TilePoint(-10, 2048)))).pois.isEmpty())
    }

    @Test
    fun buildsBryantParkFromRealTiles() {
        val bryantPark = LatLon(40.7536, -73.9832)
        val tiles = OpenMapTiles.tilesFor(bryantPark).associateWith { OpenMapTiles.decode(fixture("${it.z}-${it.x}-${it.y}.mvt")) }
        val scene = OpenMapTiles.scene(bryantPark, tiles)

        assertTrue(scene.buildings.isNotEmpty())
        assertTrue(scene.areas.any { it.ground == Ground.Grass })
        assertTrue(scene.ways.any { it.kind == WayKind.Road })
        assertTrue(scene.pois.any { it.icon == MapIcon.Transportation })
        assertNull(scene.pois.firstOrNull { it.icon == null && !it.npc })
    }

    private companion object {
        const val EXTENT = 4096
    }
}
