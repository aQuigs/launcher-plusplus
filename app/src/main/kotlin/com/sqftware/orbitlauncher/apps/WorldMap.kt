package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MapFrame
import com.sqftware.orbitlauncher.domain.minimap.MapTiles
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap

/**
 * One visit to the world map, which paints the ground the user pans across. Open one for each visit, so the tiles it
 * decoded go when the map closes rather than staying with the launcher.
 */
interface WorldMap {
    /** The world map's square of ground round [centre], its icons in it, or null while a tile it needs cannot be had. */
    suspend fun paint(centre: LatLon): PaintedMap<ImageBitmap>?
}

/** The world map drawn from map [tiles] with the theme's [art], as the minimap is. */
class TiledWorldMap(tiles: MapTiles, art: MinimapArt) : WorldMap {
    private val maker = BitmapMapMaker(tiles, MapFrame.WORLD_MAP, art)

    override suspend fun paint(centre: LatLon) = maker.paint(centre)
}
