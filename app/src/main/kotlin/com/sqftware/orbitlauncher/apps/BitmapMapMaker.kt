package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapFrame
import com.sqftware.orbitlauncher.domain.minimap.MapStyle
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.MinimapMaker
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Paints maps from map [tiles] with the theme's [art], as bitmaps, off the main thread. */
class BitmapMapMaker(tiles: MapTiles, art: MinimapArt) {
    private val maker = MinimapMaker(tiles, art::sprites)

    // A paint runs on past a cancelled caller until it next suspends, so the next caller would share the maker with it.
    private val painting = Mutex()

    /** The map in [frame] round [at] in [style], or null while a tile it needs cannot be had. */
    suspend fun paint(at: LatLon, frame: MapFrame, style: MapStyle): PaintedMap<ImageBitmap>? = withContext(Dispatchers.Default) {
        painting.withLock { maker.paint(at, frame, style)?.map { it.toImageBitmap() } }
    }
}
