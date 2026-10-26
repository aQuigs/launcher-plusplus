package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class WebMercatorTest {
    private val bryantPark = LatLon(40.7536, -73.9832)
    private val livonia = LatLon(42.3961, -83.3707)

    @Test
    fun projectsToMercatorMetresAndBack() {
        assertEquals(20_037_508.34, WebMercator.toMercator(LatLon(0.0, 180.0)).x, 0.01)
        assertEquals(0.0, WebMercator.toMercator(LatLon(0.0, 0.0)).y, 1e-9)
        val back = WebMercator.toLatLon(WebMercator.toMercator(bryantPark))
        assertEquals(bryantPark.lat, back.lat, 1e-9)
        assertEquals(bryantPark.lon, back.lon, 1e-9)
    }

    @Test
    fun pointsTheShortestWayRoundTheEarth() {
        assertEquals(0.0, WebMercator.bearing(bryantPark, LatLon(bryantPark.lat + 0.01, bryantPark.lon)), 1e-6)
        assertEquals(90.0, WebMercator.bearing(LatLon(0.0, 0.0), LatLon(0.0, 10.0)), 1e-6)
        assertEquals(270.0, WebMercator.bearing(LatLon(0.0, 0.0), LatLon(0.0, -10.0)), 1e-6)
        // New York to London sets out northeast, not east along the map's straight line.
        assertEquals(51.2, WebMercator.bearing(LatLon(40.71, -74.01), LatLon(51.51, -0.13)), 0.5)
    }

    @Test
    fun measuresTheGroundOffsetBetweenNearbyPlaces() {
        val north = WebMercator.groundOffset(bryantPark, LatLon(bryantPark.lat + 0.001, bryantPark.lon))
        assertEquals(0.0, north.east, 1e-6)
        assertEquals(111.0, north.north, 1.0)
        val west = WebMercator.groundOffset(bryantPark, LatLon(bryantPark.lat, bryantPark.lon - 0.001))
        assertEquals(-84.4, west.east, 1.0)
    }

    @Test
    fun findsTheTileAPlaceIsIn() {
        assertEquals(TileId(14, 4824, 6157), WebMercator.tileAt(WebMercator.toMercator(bryantPark), 14))
        assertEquals(TileId(14, 4397, 6057), WebMercator.tileAt(WebMercator.toMercator(livonia), 14))
        assertEquals(TileId(0, 0, 0), WebMercator.tileAt(WebMercator.toMercator(livonia), 0))
    }

    @Test
    fun placesTileCoordinatesOnTheTilesBounds() {
        val tile = TileId(14, 4824, 6157)
        val box = WebMercator.bounds(tile)
        assertEquals(Mercator(box.minX, box.maxY), WebMercator.fromTile(tile, 4096, TilePoint(0, 0)))
        val corner = WebMercator.fromTile(tile, 4096, TilePoint(4096, 4096))
        assertEquals(box.maxX, corner.x, 1e-6)
        assertEquals(box.minY, corner.y, 1e-6)
        assertEquals(WebMercator.tileSize(14), box.maxX - box.minX, 1e-6)
    }

    @Test
    fun gathersTheNeighbourWhenTheViewCrossesATileEdge() {
        // Bryant Park lies 94% of the way across its tile, under the map's reach of the eastern edge.
        assertEquals(listOf(TileId(14, 4824, 6157), TileId(14, 4825, 6157)), OpenMapTiles.tilesFor(bryantPark, MapFrame.MINIMAP))
        assertEquals(listOf(TileId(14, 4397, 6057)), OpenMapTiles.tilesFor(livonia, MapFrame.MINIMAP))
        assertTrue(OpenMapTiles.tilesFor(LatLon(-90.0, 0.0), MapFrame.MINIMAP).size <= 4)
    }

    @Test
    fun measuresGroundMetresBetweenNearbyPlaces() {
        // A thousandth of a degree of latitude is about 111 m anywhere; of longitude, that times cos(lat).
        assertEquals(111.3, WebMercator.groundDistance(bryantPark, LatLon(bryantPark.lat + 0.001, bryantPark.lon)), 0.5)
        assertEquals(84.3, WebMercator.groundDistance(bryantPark, LatLon(bryantPark.lat, bryantPark.lon + 0.001)), 0.5)
        assertEquals(0.0, WebMercator.groundDistance(livonia, livonia), 0.0)
    }

    @Test
    fun shrinksMercatorToGroundMetresAwayFromTheEquator() {
        assertEquals(1.0, WebMercator.groundScale(0.0), 1e-12)
        assertEquals(0.5, WebMercator.groundScale(60.0), 1e-12)
    }
}
