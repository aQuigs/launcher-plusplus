package com.sqftware.orbitlauncher.apps

import androidx.compose.ui.graphics.ImageBitmap
import com.sqftware.orbitlauncher.domain.minimap.METRES_PER_PIXEL
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.MadeUpTour
import com.sqftware.orbitlauncher.domain.minimap.MapFrame
import com.sqftware.orbitlauncher.domain.minimap.MinimapPainter
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration.Companion.milliseconds

/** How long the player takes to walk a minimap pixel's metres: a stroll, a little slower than the game's walk. */
private val STEP = 200.milliseconds

/**
 * The minimap of a [tour] of places only on the map, for when it does not show where the user is, drawn with the
 * theme's [art]. It lives as long as the activity, so a return to the launcher picks the walk up where it left off.
 */
class WanderingMinimap(private val tour: MadeUpTour, art: MinimapArt) {
    private val painter by lazy { MinimapPainter(art.sprites()) }
    // A collection that ends only stops on its next suspension, so the one after it may already be running.
    @Volatile
    private var walked = 0.0

    /**
     * A new [MINIMAP_SIZE] px disc each step while [walking], or the one where everyone stands otherwise. Collect it only
     * while someone can see it move.
     */
    fun maps(walking: Boolean): Flow<PaintedMap<ImageBitmap>> = flow {
        while (true) {
            emit(painter.paint(tour.scene(walked), MapFrame.MINIMAP).map { it.toImageBitmap() })
            if (!walking) awaitCancellation()
            delay(STEP)
            walked += METRES_PER_PIXEL
        }
    }.flowOn(Dispatchers.Default)
}
