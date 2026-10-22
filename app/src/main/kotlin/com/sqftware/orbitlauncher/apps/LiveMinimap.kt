package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.domain.minimap.MapFrame
import com.sqftware.orbitlauncher.domain.minimap.WebMercator
import com.sqftware.orbitlauncher.domain.minimap.paintRetryDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

/**
 * The minimap of where the user is: their [location], drawn from map [tiles] with the theme's [art]. It lives as long as
 * the activity, so the tiles it has decoded and the last map outlast each time the launcher goes out of sight.
 */
class LiveMinimap(private val location: LocationSource, tiles: MapTiles, art: MinimapArt) {
    private val maker = BitmapMapMaker(tiles, MapFrame.MINIMAP, art)
    private var shown: PaintedMap<ImageBitmap>? = null

    /**
     * A new map, its [MINIMAP_SIZE] px disc and its icons, each time the user moves far enough for a new fix, once the
     * tiles it needs are to be had. Collect it only while the launcher is visible: it keeps location updates running while collected.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun maps(): Flow<PaintedMap<ImageBitmap>> = location.locations()
        .mapLatest { centre ->
            // Nothing else comes until the user moves on, so a tile not to be had yet is asked for again; a new fix
            // cancels the wait.
            var retry = 0
            var map = paint(centre)
            while (map == null) {
                delay(paintRetryDelay(retry++))
                map = paint(centre)
            }
            map
        }
        .flowOn(Dispatchers.Default)

    // A fix within the update distance of the last map's centre, such as the first fix on a return home, keeps that map.
    private suspend fun paint(centre: LatLon): PaintedMap<ImageBitmap>? {
        shown?.let { if (WebMercator.groundDistance(it.centre, centre) < UPDATE_METRES) return it }
        return maker.paint(centre)?.also { shown = it }
    }
}
