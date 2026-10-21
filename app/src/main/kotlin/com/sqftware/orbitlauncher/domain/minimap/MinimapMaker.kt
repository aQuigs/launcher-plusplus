package com.sqftware.orbitlauncher.domain.minimap

/** Where the minimap gets its OpenMapTiles vector tiles. */
interface MapTiles {
    /** The tile [id]'s bytes, or null when it cannot be had now (offline, say). */
    suspend fun tile(id: TileId): ByteArray?

    /** Forgets the tile [id], which turned out unreadable, so the next ask fetches it afresh. */
    suspend fun discard(id: TileId)
}

/**
 * Paints the minimap as the user moves: it picks the tiles each fix needs and keeps those it has decoded, since a fix
 * 50 m on usually needs the very same ones, and keeps the last disc for a fix that has not moved. One caller at a time.
 * The [sprites] are read once, on the first paint.
 */
class MinimapMaker(private val tiles: MapTiles, sprites: () -> MinimapSprites) {
    private val painter by lazy { MinimapPainter(sprites()) }
    private val decoded = mutableMapOf<TileId, VectorTile>()
    private var last: Pair<LatLon, IntArray>? = null

    /**
     * The disc around [centre], the very array of the last one if [centre] has not moved, or null while a tile it needs
     * cannot be had: a map with a tile missing would show made-up grass where that tile belongs.
     */
    suspend fun paint(centre: LatLon): IntArray? {
        last?.let { (at, disc) -> if (at == centre) return disc }
        val needed = OpenMapTiles.tilesFor(centre)
        decoded.keys.retainAll(needed.toSet())
        for (id in needed - decoded.keys) {
            val bytes = tiles.tile(id) ?: continue
            try {
                decoded[id] = OpenMapTiles.decode(bytes)
            } catch (e: RuntimeException) {
                // Whatever is wrong with the bytes, the home screen must not go down over them, nor keep them.
                tiles.discard(id)
            }
        }
        if (decoded.size < needed.size) return null
        return painter.paint(OpenMapTiles.scene(centre, decoded)).also { last = centre to it }
    }
}
