package com.sqftware.orbitlauncher.apps

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.MinimapPainter
import com.sqftware.orbitlauncher.domain.minimap.OpenMapTiles
import com.sqftware.orbitlauncher.domain.minimap.TileId
import com.sqftware.orbitlauncher.domain.minimap.VectorTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

private const val TAG = "LiveMinimap"

/** The minimap of where the user is: their [location], drawn from map [tiles] with the theme's [art]. */
class LiveMinimap(private val location: LocationSource, private val tiles: MapTiles, private val art: MinimapArt) {
    /**
     * A new [MINIMAP_SIZE] px disc each time the user moves far enough for a new fix, once the tiles it needs are to
     * be had. Collect it only while the launcher is visible: it keeps location updates running while collected.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun images(): Flow<ImageBitmap> {
        // The tiles of the last map, decoded, since a fix 50 m on usually needs the very same ones.
        val decoded = mutableMapOf<TileId, VectorTile>()
        val painter by lazy { MinimapPainter(art.sprites()) }
        return location.locations()
            .mapLatest { centre ->
                val needed = OpenMapTiles.tilesFor(centre)
                decoded.keys.retainAll(needed.toSet())
                for (id in needed - decoded.keys) {
                    val bytes = tiles.tile(id) ?: continue
                    try {
                        decoded[id] = VectorTile.decode(bytes, OpenMapTiles.LAYERS)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "Unreadable tile $id", e)
                    }
                }
                // A map with a tile missing would show made-up grass where it belongs, so the last map stays instead.
                if (decoded.size < needed.size) return@mapLatest null
                val disc = painter.paint(OpenMapTiles.scene(centre, decoded))
                Bitmap.createBitmap(disc, MINIMAP_SIZE, MINIMAP_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
            }
            .filterNotNull()
            .flowOn(Dispatchers.Default)
    }
}
