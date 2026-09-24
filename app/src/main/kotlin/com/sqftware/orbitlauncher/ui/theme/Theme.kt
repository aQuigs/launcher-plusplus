package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// The launcher icon's palette (res/drawable/ic_launcher_*.xml). Only this file names a colour, and the rest of the app
// reaches these through the scheme's roles or the ring's tokens below.
private val Sky = Color(0xFF0F1630)
private val Star = Color(0xFF9FB2E6)
private val Starlight = Color(0xFFE8ECFF)
private val Spark = Color(0xFFF4EFE6)
private val Gold = Color(0xFFFFD27A)
private val Ember = Color(0xFFFF8A80)
private val Frost = Color.White

private val Glass = Frost.copy(alpha = 0.1f)
private val Mist = lerp(Starlight, Sky, 0.28f)

/** The home ring's drawing, in the icon's colours so it reads as the icon writ large; its marks take no hue of their own. */
val RingMark = Frost
val RingStarLine = Star.copy(alpha = 0.7f)
val RingSpark = Spark

/**
 * Built with the full constructor, so no role is left on Material's stock greys. The surfaces come in two tiers of the
 * icon's sky. What lies behind a page's content lets the wallpaper through: pages are clear, `surface` (every default
 * container) and cards are faint glass, and full-screen panels (the drawer, the collection picker) ask for the veil of
 * `surfaceDim` by name, so a container on one never stacks a second veil. What floats over other content (menus, dialogs,
 * sheets, a bin, edit handles) takes `surfaceContainerLow` and up, which are opaque lit sky, or the icons beneath would
 * show through. Content colours are all opaque, so their contrast does not hang on the wallpaper.
 */
val LauncherColors = ColorScheme(
    primary = Star,
    onPrimary = Sky,
    primaryContainer = lerp(Sky, Star, 0.5f),
    onPrimaryContainer = Starlight,
    inversePrimary = lerp(Star, Sky, 0.5f),
    secondary = Mist,
    onSecondary = Sky,
    secondaryContainer = Glass,
    onSecondaryContainer = Starlight,
    tertiary = Gold,
    onTertiary = Sky,
    tertiaryContainer = lerp(Sky, Gold, 0.5f),
    onTertiaryContainer = Starlight,
    background = Color.Transparent,
    onBackground = Starlight,
    surface = Glass,
    onSurface = Starlight,
    surfaceVariant = Glass,
    onSurfaceVariant = Mist,
    surfaceTint = Star,
    inverseSurface = Starlight,
    inverseOnSurface = Sky,
    error = Ember,
    onError = Sky,
    errorContainer = lerp(Sky, Ember, 0.5f),
    onErrorContainer = Starlight,
    outline = lerp(Starlight, Sky, 0.4f),
    outlineVariant = Frost.copy(alpha = 0.16f),
    scrim = Sky,
    surfaceBright = lerp(Sky, Frost, 0.22f),
    // Dense enough that text on the drawer still reads over a white wallpaper.
    surfaceDim = Sky.copy(alpha = 0.85f),
    surfaceContainerLowest = Sky.copy(alpha = 0.6f),
    surfaceContainerLow = lerp(Sky, Frost, 0.1f),
    surfaceContainer = lerp(Sky, Frost, 0.14f),
    surfaceContainerHigh = lerp(Sky, Frost, 0.18f),
    surfaceContainerHighest = lerp(Sky, Frost, 0.22f),
    primaryFixed = Star,
    primaryFixedDim = lerp(Star, Sky, 0.2f),
    onPrimaryFixed = Sky,
    onPrimaryFixedVariant = lerp(Sky, Star, 0.3f),
    secondaryFixed = Starlight,
    secondaryFixedDim = lerp(Starlight, Sky, 0.2f),
    onSecondaryFixed = Sky,
    onSecondaryFixedVariant = lerp(Sky, Starlight, 0.3f),
    tertiaryFixed = Gold,
    tertiaryFixedDim = lerp(Gold, Sky, 0.2f),
    onTertiaryFixed = Sky,
    onTertiaryFixedVariant = lerp(Sky, Gold, 0.3f),
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LauncherColors) {
        CompositionLocalProvider(
            // Pages sit straight on the wallpaper, so text defaults to the on-background colour; surfaces set their own.
            LocalContentColor provides LauncherColors.onBackground,
            // Elevation would tint a pane on top of the container ladder, which already sets how lit each tier is.
            LocalTonalElevationEnabled provides false,
            content = content,
        )
    }
}
