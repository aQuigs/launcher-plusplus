package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

private val HALO_BLUR = 2.dp
private const val HALO_ALPHA = 0.6f

// Pure, so the halo is the opposite of the ink whatever the wallpaper's colours.
private val DarkHalo = Color.Black
private val LightHalo = Color.White

/**
 * This style made readable where it sits: on the wallpaper, which can be light in one place and dark in the next, text
 * keeps a faint halo the opposite of its [ink], so it reads over any patch; on a panel or a pop-up, a ground of one
 * colour, it is left as it is.
 */
@Composable
fun TextStyle.legible(ink: Color = LocalContentColor.current): TextStyle {
    if (LocalGround.current != Ground.Wallpaper) return this
    val halo = if (ink.luminance() > 0.5f) DarkHalo else LightHalo
    return copy(shadow = Shadow(halo.copy(alpha = HALO_ALPHA), blurRadius = with(LocalDensity.current) { HALO_BLUR.toPx() }))
}
