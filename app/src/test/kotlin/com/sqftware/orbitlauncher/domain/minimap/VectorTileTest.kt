package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class VectorTileTest {
    @Test
    fun decodesLayersFeaturesTagsAndGeometry() {
        val tile = VectorTile.decode(
            tile(
                layer(
                    name = "poi",
                    keys = listOf("class", "rank", "height", "open", "level"),
                    values = listOf(string("bank"), uint(3), double(12.5), bool(true), sint(-1)),
                    feature(id = 42, type = 1, tags = listOf(0, 0, 1, 1, 2, 2, 3, 3, 4, 4), geometry = listOf(cmd(1, 2), zz(10), zz(20), zz(-5), zz(5))),
                    extent = 512,
                ),
                layer(
                    name = "building",
                    keys = emptyList(),
                    values = emptyList(),
                    // A square, clockwise on screen, with a square hole inside it wound the other way.
                    feature(
                        id = 7,
                        type = 3,
                        tags = emptyList(),
                        geometry = listOf(
                            cmd(1, 1), zz(0), zz(0), cmd(2, 3), zz(10), zz(0), zz(0), zz(10), zz(-10), zz(0), cmd(7, 1),
                            cmd(1, 1), zz(2), zz(-8), cmd(2, 3), zz(0), zz(6), zz(6), zz(0), zz(0), zz(-6), cmd(7, 1),
                        ),
                    ),
                ),
                layer("housenumber", emptyList(), emptyList(), feature(1, 1, emptyList(), listOf(cmd(1, 1), zz(1), zz(1)))),
            ),
        )

        val poi = tile["poi"]!!
        assertEquals(512, poi.extent)
        val bank = poi.features.single()
        assertEquals(42L, bank.id)
        assertEquals(GeometryType.Point, bank.type)
        assertEquals("bank", bank.string("class"))
        assertEquals(3.0, bank.number("rank")!!, 0.0)
        assertEquals(12.5, bank.number("height")!!, 0.0)
        assertEquals(true, bank.tags["open"])
        assertEquals(-1.0, bank.number("level")!!, 0.0)
        // A multipoint's points are each their own part, the second relative to the first.
        assertEquals(listOf(listOf(TilePoint(10, 20)), listOf(TilePoint(5, 25))), bank.parts)

        val building = tile["building"]!!.features.single()
        assertEquals(GeometryType.Polygon, building.type)
        val polygons = building.polygons()
        assertEquals(1, polygons.size)
        assertEquals(listOf(TilePoint(0, 0), TilePoint(10, 0), TilePoint(10, 10), TilePoint(0, 10)), polygons[0][0])
        assertEquals(TilePoint(2, 2), polygons[0][1][0])
        assertEquals(4096, tile["housenumber"]!!.extent)
    }

    @Test
    fun decodesOnlyTheLayersAskedFor() {
        val bytes = tile(
            layer("water", emptyList(), emptyList()),
            layer("housenumber", emptyList(), emptyList()),
        )
        val tile = VectorTile.decode(bytes, only = setOf("water"))
        assertTrue("water" in tile.layers)
        assertNull(tile["housenumber"])
    }

    @Test
    fun rejectsCorruptBytesAsIllegal() {
        val corrupt = listOf(
            byteArrayOf(0x1A, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x07, 0x00),
            byteArrayOf(0x1A, 0x05, 0x0A),
            byteArrayOf(0x1A, 0x80.toByte()),
            "<html>captive portal</html>".toByteArray(),
        )
        for (bytes in corrupt) {
            assertThrows(IllegalArgumentException::class.java) { VectorTile.decode(bytes) }
        }
    }

    @Test
    fun decodesARealTile() {
        val tile = VectorTile.decode(fixture("14-4824-6157.mvt"))
        assertEquals(OpenMapTiles.LAYERS, tile.layers.keys)
        val buildings = tile["building"]!!.features
        assertTrue(buildings.isNotEmpty())
        assertTrue(buildings.all { it.type == GeometryType.Polygon && it.polygons().isNotEmpty() })
        val roads = tile["transportation"]!!.features.filter { it.string("class") == "secondary" }
        assertTrue(roads.all { it.type == GeometryType.Line && it.parts.all { line -> line.size >= 2 } })
        assertTrue(tile["poi"]!!.features.any { it.string("subclass") == "subway_entrance" })
    }

    private fun tile(vararg layers: ByteArray) = message { layers.forEach { bytes(3, it) } }

    private fun layer(name: String, keys: List<String>, values: List<ByteArray>, vararg features: ByteArray, extent: Int? = null) = message {
        varint(15, 2)
        bytes(1, name.toByteArray())
        features.forEach { bytes(2, it) }
        keys.forEach { bytes(3, it.toByteArray()) }
        values.forEach { bytes(4, it) }
        extent?.let { varint(5, it.toLong()) }
    }

    private fun feature(id: Long, type: Int, tags: List<Int>, geometry: List<Int>) = message {
        varint(1, id)
        bytes(2, packed(tags))
        varint(3, type.toLong())
        bytes(4, packed(geometry))
    }

    private fun string(s: String) = message { bytes(1, s.toByteArray()) }

    private fun uint(n: Long) = message { varint(5, n) }

    private fun sint(n: Long) = message { varint(6, (n shl 1) xor (n shr 63)) }

    private fun bool(b: Boolean) = message { varint(7, if (b) 1 else 0) }

    private fun double(d: Double) = message {
        key(3, 1)
        val bits = d.toRawBits()
        for (i in 0 until 8) out.write((bits ushr (8 * i)).toInt() and 0xFF)
    }

    private fun cmd(id: Int, count: Int) = id or (count shl 3)

    private fun zz(n: Int) = (n shl 1) xor (n shr 31)

    private fun packed(ints: List<Int>) = message { ints.forEach { writeVarint(it.toLong() and 0xFFFFFFFFL) } }

    private class Writer {
        val out = ByteArrayOutputStream()

        fun writeVarint(value: Long) {
            var v = value
            while (true) {
                if (v and 0x7FL.inv() == 0L) {
                    out.write(v.toInt())
                    return
                }
                out.write((v.toInt() and 0x7F) or 0x80)
                v = v ushr 7
            }
        }

        fun key(field: Int, wire: Int) = writeVarint(((field shl 3) or wire).toLong())

        fun varint(field: Int, value: Long) {
            key(field, 0)
            writeVarint(value)
        }

        fun bytes(field: Int, value: ByteArray) {
            key(field, 2)
            writeVarint(value.size.toLong())
            out.write(value)
        }
    }

    private fun message(build: Writer.() -> Unit) = Writer().apply(build).out.toByteArray()
}

// The fixtures are OpenFreeMap z14 tiles around Bryant Park and Livonia City Hall, cut down to the minimap's layers and
// the features near each: OpenMapTiles schema, data © OpenStreetMap contributors (ODbL).
internal fun fixture(name: String): ByteArray =
    VectorTileTest::class.java.getResourceAsStream("/minimap/$name")!!.use { it.readBytes() }
