package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapFrame
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.MinimapMaker
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Paints maps in [frame] from map [tiles] with the theme's [art], as bitmaps, off the main thread. */
class BitmapMapMaker(tiles: MapTiles, frame: MapFrame, art: MinimapArt) {
    private val maker = MinimapMaker(tiles, frame, art::sprites)

    // A paint runs on past a cancelled caller until it next suspends, so the next caller would share the maker with it.
    private val painting = Mutex()

    /** The map round [at], or null while a tile it needs cannot be had. */
    suspend fun paint(at: LatLon): PaintedMap<ImageBitmap>? = withContext(Dispatchers.Default) {
        painting.withLock { maker.paint(at)?.map { it.toImageBitmap() } }
    }
}
