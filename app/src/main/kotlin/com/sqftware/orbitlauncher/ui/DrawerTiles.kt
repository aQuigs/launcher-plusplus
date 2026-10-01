package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

// A drawer's backdrop is a tile a phone's screen fits in, repeated beyond it. What is on it sits a fixed distance from
// the top left, not at a share of the area drawn, so when the keyboard shrinks the drawer for a search it covers some
// rather than squeezing them up.
internal val DRAWER_TILE_WIDTH = 480.dp
internal val DRAWER_TILE_HEIGHT = 960.dp

// A far backdrop drifts behind what scrolls over it at a fraction of its pace.
private const val PARALLAX = 0.25f

/**
 * Calls [tile] with the top left and the size of each tile the draw area shows, the tiles repeated across and down it
 * and drifting up by a share of how far the content over them has [scrolled].
 */
internal fun DrawScope.forEachDrawerTile(scrolled: Float, tile: DrawScope.(origin: Offset, size: Size) -> Unit) {
    val tileSize = Size(DRAWER_TILE_WIDTH.toPx(), DRAWER_TILE_HEIGHT.toPx())
    val top = -(scrolled * PARALLAX).mod(tileSize.height)
    repeat(ceil(size.width / tileSize.width).toInt()) { column ->
        repeat(ceil((size.height - top) / tileSize.height).toInt()) { row ->
            tile(Offset(column * tileSize.width, top + row * tileSize.height), tileSize)
        }
    }
}

/**
 * What a theme draws on every drawer tile, [build] once for the tile's size and again only when that changes: the
 * drawer redraws its backdrop on every frame it moves, so a drawing with many parts is not remade each time.
 */
internal class DrawerTileArt<T>(private val build: Density.(Size) -> T) {
    private var built: Pair<Size, T>? = null

    fun DrawScope.of(tile: Size): T = built?.takeIf { it.first == tile }?.second ?: build(tile).also { built = tile to it }
}
