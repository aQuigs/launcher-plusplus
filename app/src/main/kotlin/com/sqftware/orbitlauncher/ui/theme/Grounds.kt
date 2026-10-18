package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.Colourway

/**
 * What a piece of the launcher is composed on: the [Wallpaper] of a page, the veil of a full-screen [Panel] (the drawer's
 * too), or the solid ground of a [Popup]. Boxes and buttons take their look from it rather than each screen picking one, so
 * a box looks the same on every ground of a kind, and a ground's look changes in one place.
 */
enum class Ground { Wallpaper, Panel, Popup }

/** The [Ground] whatever is composed here sits on; a panel, a pop-up and a floating box each provide their own. */
val LocalGround = staticCompositionLocalOf { Ground.Wallpaper }

/**
 * The fill of a box on each [Ground], a step off that ground so the box shows on it, and of what floats over any content
 * and must hide it. Night's tiers grow lighter going up and day's darker, so the step is a different role in each scheme;
 * it is picked here once rather than by each box.
 */
class BoxColors internal constructor(val glass: Color, private val panel: Color, private val popup: Color, val floating: Color) {
    // Every ground named, so a new one does not compile until its boxes have a fill.
    fun on(ground: Ground): Color = when (ground) {
        Ground.Wallpaper -> glass
        Ground.Panel -> panel
        Ground.Popup -> popup
    }
}

val LocalBoxColors = staticCompositionLocalOf { lookOf(Colourway.Midnight, lightWallpaper = false).boxes }

/**
 * The launcher's corners, from a small target's to a strip's; a box takes one of these by size rather than a radius of its
 * own, so boxes of a kind round alike.
 */
internal val LauncherShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
