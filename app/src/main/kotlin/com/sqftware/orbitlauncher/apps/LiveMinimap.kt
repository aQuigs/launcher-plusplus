package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.MinimapMaker
import com.sqftware.orbitlauncher.domain.minimap.WebMercator
import com.sqftware.orbitlauncher.domain.minimap.paintRetryDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The minimap of where the user is: their [location], drawn from map [tiles] with the theme's [art]. It lives as long as
 * the activity, so the tiles it has decoded and the last map outlast each time the launcher goes out of sight.
 */
class LiveMinimap(private val location: LocationSource, tiles: MapTiles, art: MinimapArt) {
    private val maker = MinimapMaker(tiles, art::sprites)

    // A paint runs on past its collection's end until it next suspends, so a collection restarted at once would share
    // the maker with it.
    private val painting = Mutex()
    private var shown: Pair<LatLon, ImageBitmap>? = null

    /**
     * A new [MINIMAP_SIZE] px disc each time the user moves far enough for a new fix, once the tiles it needs are to
     * be had. Collect it only while the launcher is visible: it keeps location updates running while collected.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun images(): Flow<ImageBitmap> = location.locations()
        .mapLatest { centre ->
            // Nothing else comes until the user moves on, so a tile not to be had yet is asked for again; a new fix
            // cancels the wait.
            var retry = 0
            var image = paint(centre)
            while (image == null) {
                delay(paintRetryDelay(retry++))
                image = paint(centre)
            }
            image
        }
        .flowOn(Dispatchers.Default)

    // A fix within the update distance of the last map's centre, such as the first fix on a return home, keeps that map.
    private suspend fun paint(centre: LatLon): ImageBitmap? = painting.withLock {
        shown?.let { (at, image) -> if (WebMercator.groundDistance(at, centre) < UPDATE_METRES) return@withLock image }
        val disc = maker.paint(centre) ?: return@withLock null
        disc.toImageBitmap().also { shown = centre to it }
    }
}
