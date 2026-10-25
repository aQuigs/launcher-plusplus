package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.MapStyle
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
    private val maker = BitmapMapMaker(tiles, art)
    private var shown: Pair<MapStyle, PaintedMap<ImageBitmap>>? = null

    /**
     * A new map in [style], its [MINIMAP_SIZE] px disc and its icons, each time the user moves far enough for a new fix,
     * once the tiles it needs are to be had. Collect it only while the launcher is visible: it keeps location updates
     * running while collected.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun maps(style: MapStyle): Flow<PaintedMap<ImageBitmap>> = location.locations()
        .mapLatest { centre ->
            // Nothing else comes until the user moves on, so a tile not to be had yet is asked for again; a new fix
            // cancels the wait.
            var retry = 0
            var map = paint(centre, style)
            while (map == null) {
                delay(paintRetryDelay(retry++))
                map = paint(centre, style)
            }
            map
        }
        .flowOn(Dispatchers.Default)

    // A fix within the update distance of the last map's centre, such as the first fix on a return home, keeps that map,
    // if it was painted in the same style.
    private suspend fun paint(centre: LatLon, style: MapStyle): PaintedMap<ImageBitmap>? {
        shown?.let { (shownStyle, map) -> if (shownStyle == style && WebMercator.groundDistance(map.centre, centre) < UPDATE_METRES) return map }
        return maker.paint(centre, MapFrame.MINIMAP, style)?.also { shown = style to it }
    }
}
