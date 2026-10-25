package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.METRES_PER_PIXEL
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.MadeUpTour
import com.sqftware.orbitlauncher.domain.minimap.MapFrame
import com.sqftware.orbitlauncher.domain.minimap.MapStyle
import com.sqftware.orbitlauncher.domain.minimap.MinimapPainter
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

/** How long the player takes to walk a minimap pixel's metres: a stroll, a little slower than the game's walk. */
private val STEP = 200.milliseconds

/**
 * The minimap of a [tour] of places only on the map, for when it does not show where the user is, drawn with the
 * theme's [art]. It lives as long as the activity, so a return to the launcher picks the walk up where it left off.
 */
class WanderingMinimap(private val tour: MadeUpTour, private val art: MinimapArt) {
    private val painters = ConcurrentHashMap<MapStyle, MinimapPainter>()
    // A collection that ends only stops on its next suspension, so the one after it may already be running.
    @Volatile
    private var walked = 0.0

    /**
     * A new [MINIMAP_SIZE] px disc in [style] each step while [walking], or the one where everyone stands otherwise.
     * Collect it only while someone can see it move.
     */
    fun maps(walking: Boolean, style: MapStyle): Flow<PaintedMap<ImageBitmap>> = flow {
        val painter = painters.getOrPut(style) { MinimapPainter(art.sprites(style), style) }
        while (true) {
            emit(painter.paint(tour.scene(walked), MapFrame.MINIMAP).map { it.toImageBitmap() })
            if (!walking) awaitCancellation()
            delay(STEP)
            walked += METRES_PER_PIXEL
        }
    }.flowOn(Dispatchers.Default)
}
