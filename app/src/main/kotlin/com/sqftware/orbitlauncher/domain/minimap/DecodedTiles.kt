package com.sqftware.orbitlauncher.domain.minimap

/**
 * The map [tiles] a map maker has decoded with [decode], kept while it needs them, since a user who moves on a little at
 * a time usually needs the very same ones. One caller at a time.
 */
internal class DecodedTiles(private val tiles: MapTiles, private val decode: (ByteArray) -> VectorTile) {
    private val decoded = mutableMapOf<TileId, VectorTile>()

    /**
     * The tiles [needed], decoded, or null while one cannot be had. Each missing tile is asked for, so the next call
     * finds as many as could be had; one that cannot be read is discarded, so the next ask fetches it afresh.
     */
    suspend fun get(needed: List<TileId>): Map<TileId, VectorTile>? {
        decoded.keys.retainAll(needed.toSet())
        for (id in needed - decoded.keys) {
            val bytes = tiles.tile(id) ?: continue
            try {
                decoded[id] = decode(bytes)
            } catch (e: RuntimeException) {
                // Whatever is wrong with the bytes, the home screen must not go down over them, nor keep them.
                tiles.discard(id)
            }
        }
        return decoded.takeIf { it.size == needed.size }
    }
}
