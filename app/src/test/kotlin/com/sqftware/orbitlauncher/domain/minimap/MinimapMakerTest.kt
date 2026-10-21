package com.sqftware.orbitlauncher.domain.minimap

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class MinimapMakerTest {
    private val bryantPark = LatLon(40.7536, -73.9832)

    private class FakeTiles(val bytes: MutableMap<TileId, ByteArray>) : MapTiles {
        val asked = mutableListOf<TileId>()
        val discarded = mutableListOf<TileId>()

        override suspend fun tile(id: TileId): ByteArray? = bytes[id].also { asked += id }

        override suspend fun discard(id: TileId) {
            discarded += id
            bytes.remove(id)
        }
    }

    private fun fixtures() = OpenMapTiles.tilesFor(bryantPark).associateWith { fixture("${it.z}-${it.x}-${it.y}.mvt") }.toMutableMap()

    @Test
    fun decodesEachTileOnceAsTheUserMovesAndKeepsTheDiscOfAFixThatHasNotMoved() = runBlocking {
        val tiles = FakeTiles(fixtures())
        val maker = MinimapMaker(tiles) { MinimapSprites() }

        val first = maker.paint(bryantPark)
        assertNotNull(first)
        assertSame(first, maker.paint(bryantPark))
        assertNotNull(maker.paint(LatLon(bryantPark.lat + 0.0002, bryantPark.lon)))

        assertEquals(OpenMapTiles.tilesFor(bryantPark), tiles.asked)
    }

    @Test
    fun drawsNothingWhileATileIsMissing() = runBlocking {
        val bytes = fixtures()
        bytes.remove(bytes.keys.last())
        assertNull(MinimapMaker(FakeTiles(bytes)) { MinimapSprites() }.paint(bryantPark))
    }

    @Test
    fun discardsAnUnreadableTile() = runBlocking {
        val bytes = fixtures()
        val bad = bytes.keys.first()
        bytes[bad] = "<html>captive portal</html>".toByteArray()
        val tiles = FakeTiles(bytes)

        assertNull(MinimapMaker(tiles) { MinimapSprites() }.paint(bryantPark))
        assertEquals(listOf(bad), tiles.discarded)
    }
}
