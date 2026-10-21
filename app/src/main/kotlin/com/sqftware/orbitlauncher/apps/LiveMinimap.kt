package com.sqftware.orbitlauncher.apps

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.MinimapMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

/**
 * The minimap of where the user is: their [location], drawn from map [tiles] with the theme's [art]. It lives as long as
 * the activity, so the tiles it has decoded and the last map outlast each time the launcher goes out of sight.
 */
class LiveMinimap(private val location: LocationSource, tiles: MapTiles, art: MinimapArt) {
    private val maker = MinimapMaker(tiles, art::sprites)
    private var shown: Pair<IntArray, ImageBitmap>? = null

    /**
     * A new [MINIMAP_SIZE] px disc each time the user moves far enough for a new fix, once the tiles it needs are to
     * be had. Collect it only while the launcher is visible, and one collector at a time: it keeps location updates
     * running while collected.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun images(): Flow<ImageBitmap> = location.locations()
        .distinctUntilChanged()
        .mapLatest { centre ->
            val disc = maker.paint(centre) ?: return@mapLatest null
            shown?.takeIf { it.first === disc }?.second
                ?: Bitmap.createBitmap(disc, MINIMAP_SIZE, MINIMAP_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap().also { shown = disc to it }
        }
        .filterNotNull()
        .flowOn(Dispatchers.Default)
}
