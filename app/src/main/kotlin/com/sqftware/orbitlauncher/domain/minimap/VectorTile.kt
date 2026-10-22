package com.sqftware.orbitlauncher.domain.minimap

/** A point in a layer's tile coordinates: 0 to the layer's extent, y down. */
data class TilePoint(val x: Int, val y: Int)

enum class GeometryType { Unknown, Point, Line, Polygon }

/**
 * One feature of a Mapbox Vector Tile layer. [parts] are its points for a point feature (one per part), its line strings
 * for a line, and its rings for a polygon, each ring open (the closing point not repeated).
 */
class VectorFeature(val type: GeometryType, val tags: Map<String, Any>, val parts: List<List<TilePoint>>) {
    fun string(key: String) = tags[key] as? String

    fun number(key: String) = (tags[key] as? Number)?.toDouble()

    /**
     * The rings of a polygon feature grouped into polygons, each its exterior ring followed by its holes. The format
     * winds exterior rings clockwise on screen, a positive area with y down, and holes the other way.
     */
    fun polygons(): List<List<List<TilePoint>>> {
        val polygons = mutableListOf<MutableList<List<TilePoint>>>()
        for (ring in parts) {
            val area = signedArea(ring)
            when {
                area > 0 -> polygons += mutableListOf(ring)
                area < 0 -> polygons.lastOrNull()?.add(ring)
            }
        }
        return polygons
    }
}

class VectorLayer(val name: String, val extent: Int, val features: List<VectorFeature>)

/** A decoded Mapbox Vector Tile (spec 2.1): its layers by name. */
class VectorTile(val layers: Map<String, VectorLayer>) {
    operator fun get(layer: String): VectorLayer? = layers[layer]

    companion object {
        /**
         * Decodes [bytes], keeping only the layers named in [only] and the tags named in [keys] if given, since a city
         * tile carries far more than a minimap draws, its places' names in dozens of languages among it.
         */
        fun decode(bytes: ByteArray, only: Set<String>? = null, keys: Set<String>? = null): VectorTile {
            val layers = mutableMapOf<String, VectorLayer>()
            val tile = Protobuf(bytes, 0, bytes.size)
            while (tile.hasMore()) {
                val (field, wire) = tile.key()
                if (field == 3 && wire == WIRE_BYTES) {
                    tile.message().let { decodeLayer(it, only, keys) }?.let { layers[it.name] = it }
                } else {
                    tile.skip(wire)
                }
            }
            return VectorTile(layers)
        }

        private fun decodeLayer(layer: Protobuf, only: Set<String>?, wanted: Set<String>?): VectorLayer? {
            var name = ""
            var extent = 4096
            val keys = mutableListOf<String>()
            val values = mutableListOf<Any>()
            val features = mutableListOf<Protobuf>()
            while (layer.hasMore()) {
                val (field, wire) = layer.key()
                when {
                    field == 1 && wire == WIRE_BYTES -> {
                        name = layer.string()
                        if (only != null && name !in only) return null
                    }
                    field == 2 && wire == WIRE_BYTES -> features += layer.message()
                    field == 3 && wire == WIRE_BYTES -> keys += layer.string()
                    field == 4 && wire == WIRE_BYTES -> values += decodeValue(layer.message())
                    field == 5 && wire == WIRE_VARINT -> extent = layer.varint().toInt()
                    else -> layer.skip(wire)
                }
            }
            val kept = keys.map { key -> key.takeIf { wanted == null || it in wanted } }
            return VectorLayer(name, extent, features.map { decodeFeature(it, kept, values) })
        }

        private fun decodeValue(value: Protobuf): Any {
            var decoded: Any = ""
            while (value.hasMore()) {
                val (field, wire) = value.key()
                decoded = when (field) {
                    1 -> value.string()
                    2 -> Float.fromBits(value.fixed32())
                    3 -> Double.fromBits(value.fixed64())
                    4, 5 -> value.varint()
                    6 -> value.varint().let { (it ushr 1) xor -(it and 1) }
                    7 -> value.varint() != 0L
                    else -> {
                        value.skip(wire)
                        decoded
                    }
                }
            }
            return decoded
        }

        private fun decodeFeature(feature: Protobuf, keys: List<String?>, values: List<Any>): VectorFeature {
            var type = GeometryType.Unknown
            val tags = mutableMapOf<String, Any>()
            var geometry = IntArray(0)
            while (feature.hasMore()) {
                val (field, wire) = feature.key()
                when {
                    field == 2 && wire == WIRE_BYTES -> feature.packed().let { tagIndices ->
                        for (i in 0 until tagIndices.size - 1 step 2) {
                            val key = keys.getOrNull(tagIndices[i])
                            val value = values.getOrNull(tagIndices[i + 1])
                            if (key != null && value != null) tags[key] = value
                        }
                    }
                    field == 3 && wire == WIRE_VARINT -> type = GeometryType.entries.getOrElse(feature.varint().toInt()) { GeometryType.Unknown }
                    field == 4 && wire == WIRE_BYTES -> geometry = feature.packed()
                    else -> feature.skip(wire)
                }
            }
            return VectorFeature(type, tags, decodeGeometry(geometry))
        }

        // Each MoveTo starts a part, so each point of a multipoint is a part of its own.
        private fun decodeGeometry(commands: IntArray): List<List<TilePoint>> {
            val parts = mutableListOf<List<TilePoint>>()
            var current = mutableListOf<TilePoint>()
            var x = 0
            var y = 0
            var i = 0
            while (i < commands.size) {
                val command = commands[i] and 0x7
                val count = commands[i] ushr 3
                i++
                when (command) {
                    MOVE_TO, LINE_TO -> repeat(count) {
                        if (i + 1 >= commands.size) return parts
                        x += zigzag(commands[i])
                        y += zigzag(commands[i + 1])
                        i += 2
                        if (command == MOVE_TO) {
                            if (current.isNotEmpty()) parts += current
                            current = mutableListOf()
                        }
                        current += TilePoint(x, y)
                    }
                    CLOSE_PATH -> {
                        parts += current
                        current = mutableListOf()
                    }
                    else -> return parts
                }
            }
            if (current.isNotEmpty()) parts += current
            return parts
        }

        private fun zigzag(n: Int) = (n ushr 1) xor -(n and 1)

        private const val MOVE_TO = 1
        private const val LINE_TO = 2
        private const val CLOSE_PATH = 7
        private const val WIRE_VARINT = 0
        private const val WIRE_BYTES = 2
    }
}

/** Twice the signed area of [ring] with y down: positive when it winds clockwise on screen. */
private fun signedArea(ring: List<TilePoint>): Long {
    var sum = 0L
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[(i + 1) % ring.size]
        sum += a.x.toLong() * b.y - b.x.toLong() * a.y
    }
    return sum
}

/** A reader over a protobuf message in [bytes] from [start] to [end]: just what the vector tile format needs. */
private class Protobuf(private val bytes: ByteArray, start: Int, private val end: Int) {
    private var at = start

    fun hasMore() = at < end

    fun key(): Pair<Int, Int> = varint().let { (it ushr 3).toInt() to (it and 0x7).toInt() }

    fun varint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            if (at >= end) throw IllegalArgumentException("Truncated varint")
            val b = bytes[at++].toInt()
            result = result or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) return result
            shift += 7
            if (shift > 63) throw IllegalArgumentException("Varint too long")
        }
    }

    fun fixed32() = fixed(4).toInt()

    fun fixed64() = fixed(8)

    private fun fixed(size: Int): Long {
        if (at + size > end) throw IllegalArgumentException("Truncated fixed value")
        var v = 0L
        for (i in 0 until size) v = v or ((bytes[at + i].toLong() and 0xFF) shl (8 * i))
        at += size
        return v
    }

    private fun length(): Int {
        val n = varint().toInt()
        // Compared without adding, so a corrupt length cannot overflow past the check.
        if (n < 0 || n > end - at) throw IllegalArgumentException("Length past the end")
        return n
    }

    fun message(): Protobuf {
        val n = length()
        return Protobuf(bytes, at, at + n).also { at += n }
    }

    fun string(): String {
        val n = length()
        return String(bytes, at, n, Charsets.UTF_8).also { at += n }
    }

    fun packed(): IntArray {
        val inner = message()
        var out = IntArray(16)
        var n = 0
        while (inner.hasMore()) {
            if (n == out.size) out = out.copyOf(n * 2)
            out[n++] = inner.varint().toInt()
        }
        return out.copyOf(n)
    }

    fun skip(wire: Int) {
        when (wire) {
            0 -> varint()
            1 -> at += 8
            2 -> at += length()
            5 -> at += 4
            else -> throw IllegalArgumentException("Unsupported wire type $wire")
        }
    }
}
