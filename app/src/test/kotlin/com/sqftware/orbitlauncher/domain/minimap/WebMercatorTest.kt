package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
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
        assertEquals(listOf(TileId(14, 4824, 6157), TileId(14, 4825, 6157)), OpenMapTiles.tilesFor(bryantPark))
        assertEquals(listOf(TileId(14, 4397, 6057)), OpenMapTiles.tilesFor(livonia))
    }

    @Test
    fun shrinksMercatorToGroundMetresAwayFromTheEquator() {
        assertEquals(1.0, WebMercator.groundScale(0.0), 1e-12)
        assertEquals(0.5, WebMercator.groundScale(60.0), 1e-12)
    }
}
