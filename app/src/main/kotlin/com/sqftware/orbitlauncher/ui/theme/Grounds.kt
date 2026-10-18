package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sqftware.orbitlauncher.domain.Colourway

/**
 * What a piece of the launcher is composed on: the [Wallpaper] of a page, the veil of a full-screen [Panel] (the drawer's
 * too), or the solid ground of a [Popup]. Boxes and buttons take their look from it rather than each screen picking one, so
 * a ground's look changes in one place.
 */
enum class Ground { Wallpaper, Panel, Popup }

/** The [Ground] whatever is composed here sits on; panels, pop-ups and floating boxes provide their own. */
val LocalGround = staticCompositionLocalOf { Ground.Wallpaper }

/**
 * The fill of a box on each [Ground], a step off that ground so the box shows on it; of a mark that [floating] over any
 * content must hide it; and the [lit] fill, with [onLit] content, of a chosen box or choice. Night's tiers grow lighter
 * going up and day's darker, so the step is a different role in each scheme. A box on a pop-up has no fill: the pop-up is
 * already the box, and one inside it only groups what it holds.
 */
class BoxColors internal constructor(
    private val wallpaper: Color,
    private val panel: Color,
    val floating: Color,
    val lit: Color,
    val onLit: Color,
) {
    // Every ground named, so a new one does not compile until its boxes have a fill.
    fun on(ground: Ground): Color = when (ground) {
        Ground.Wallpaper -> wallpaper
        Ground.Panel -> panel
        Ground.Popup -> Color.Transparent
    }
}

val LocalBoxColors = staticCompositionLocalOf { lookOf(Colourway.Midnight, lightWallpaper = false).boxes }
