package com.sqftware.orbitlauncher.domain.minimap

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

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
    fun decodesEachTileOnceAsTheUserMoves() = runBlocking {
        val tiles = FakeTiles(fixtures())
        val maker = MinimapMaker(tiles) { MinimapSprites() }

        assertNotNull(maker.paint(bryantPark))
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
    fun drawsAroundAnEmptyTile() = runBlocking {
        val bytes = fixtures()
        bytes[bytes.keys.first()] = ByteArray(0)
        val tiles = FakeTiles(bytes)

        assertNotNull(MinimapMaker(tiles) { MinimapSprites() }.paint(bryantPark))
        assertEquals(emptyList<TileId>(), tiles.discarded)
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

    @Test
    fun triesAgainSoonAtFirstThenBacksOffToTwoMinutes() {
        assertEquals(listOf(5, 10, 20, 40, 80, 120, 120), List(7) { paintRetryDelay(it).inWholeSeconds.toInt() })
        assertEquals(2.minutes, paintRetryDelay(100))
    }
}
