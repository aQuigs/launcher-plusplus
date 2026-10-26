package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.Pixels
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val FIRST_RETRY = 5.seconds
private val LAST_RETRY = 2.minutes

/**
 * How long to wait before the [retry]th try again (from 0) at a map whose tiles could not be had: 5 s, doubling up to
 * 2 min, so a map left blank by a dropped connection fills in soon after it returns without asking the server often.
 */
fun paintRetryDelay(retry: Int): Duration = (FIRST_RETRY * (1 shl retry.coerceAtMost(5))).coerceAtMost(LAST_RETRY)

/** Where the minimap gets its OpenMapTiles vector tiles. */
interface MapTiles {
    /** The tile [id]'s bytes, or null when it cannot be had now (offline, say). */
    suspend fun tile(id: TileId): ByteArray?

    /** Forgets the tile [id], which turned out unreadable, so the next ask fetches it afresh. */
    suspend fun discard(id: TileId)
}

/**
 * Paints maps as the user moves or pans: it picks the tiles each centre needs and keeps those it has decoded, since a
 * centre 50 m on usually needs the very same ones. One caller at a time. A style's [sprites] are read on each paint in it.
 */
class MinimapMaker(private val tiles: MapTiles, private val sprites: (MapStyle) -> MinimapSprites) {
    private val decoded = mutableMapOf<TileId, VectorTile>()

    /**
     * The map in [frame] round [at] in [style], centred as the frame centres it, or null while a tile it needs cannot be
     * had: a map with a tile missing would show made-up grass where that tile belongs.
     */
    suspend fun paint(at: LatLon, frame: MapFrame, style: MapStyle): PaintedMap<Pixels>? {
        val centre = frame.centreFor(at)
        val needed = OpenMapTiles.tilesFor(centre, frame)
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
        return MinimapPainter(sprites(style), style).paint(OpenMapTiles.scene(centre, decoded, frame), frame)
    }
}
