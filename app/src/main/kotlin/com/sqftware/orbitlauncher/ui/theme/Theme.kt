package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
private val DayGlass = Frost.copy(alpha = 0.35f)
private val DayGold = lerp(Gold, Sky, 0.5f)
private val Mist = lerp(Starlight, Sky, 0.28f)

/**
 * The home ring's marks that lie on the wallpaper (its track, the constellation's lines, the emblem's edge, the ring round
 * a fold target), in the icon's colours so the ring reads as the icon writ large, inked for the wallpaper like the scheme.
 */
class RingColors(val mark: Color, val starLine: Color, val lit: Color)

private val NightRing = RingColors(mark = Frost, starLine = Star.copy(alpha = 0.7f), lit = Spark)
private val DayRing = RingColors(mark = Sky, starLine = lerp(Sky, Star, 0.4f).copy(alpha = 0.7f), lit = DayGold)

val LocalRingColors = staticCompositionLocalOf { NightRing }

// The emblem is a disc of the icon's own sky whatever the wallpaper, so what is drawn inside it keeps the night's colours.
val RingSpark = Spark
val RingInk = Frost.copy(alpha = 0.9f)
val RingShade = Sky

/**
 * The edge of a folder's glass disc: a dark line round a light one, the same in both schemes. The scheme follows the
 * wallpaper as a whole, but a disc sits on one patch of it, which may be light under the night scheme or dark under the
 * day's, so one of the two lines has to stand out on whatever is there.
 */
class DiscEdge(val outer: Color, val inner: Color)

val FolderEdge = DiscEdge(outer = Sky.copy(alpha = 0.5f), inner = Frost.copy(alpha = 0.5f))

/** What a glyph is drawn in before `Icon` tints it, as Material's own icons are. */
val GlyphFill = Color.Black

/**
 * Built with the full constructor, so no role is left on Material's stock greys. The surfaces come in two tiers of the
 * icon's sky. What lies behind a page's content lets the wallpaper through: pages are clear, `surface` (every default
 * container) and cards are faint glass, and full-screen panels (the drawer, and every `Panel`) ask for the veil of
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

/**
 * [LauncherColors] turned over for a light wallpaper, which the system says wants dark text: the same two tiers, with the
 * glass and the veil frosted instead of sky, what floats opaque lit frost, and sky ink for content. The scrim turns to
 * frost too, so the shades that lift the chevron and the navigation icons off the wallpaper stay light behind dark marks.
 * The fixed roles, the clear background and the tint stay the night's.
 */
val LauncherDayColors = LauncherColors.copy(
    primary = lerp(Sky, Star, 0.3f),
    onPrimary = Starlight,
    primaryContainer = lerp(Frost, Star, 0.5f),
    onPrimaryContainer = Sky,
    inversePrimary = Star,
    secondary = lerp(Sky, Starlight, 0.3f),
    onSecondary = Starlight,
    secondaryContainer = lerp(Frost, Star, 0.35f),
    onSecondaryContainer = Sky,
    tertiary = DayGold,
    onTertiary = Starlight,
    tertiaryContainer = lerp(Frost, Gold, 0.5f),
    onTertiaryContainer = Sky,
    onBackground = Sky,
    surface = DayGlass,
    onSurface = Sky,
    surfaceVariant = DayGlass,
    onSurfaceVariant = lerp(Sky, Starlight, 0.25f),
    inverseSurface = Sky,
    inverseOnSurface = Starlight,
    error = lerp(Ember, Sky, 0.5f),
    onError = Starlight,
    errorContainer = lerp(Frost, Ember, 0.4f),
    onErrorContainer = Sky,
    outline = lerp(Sky, Starlight, 0.5f),
    outlineVariant = Sky.copy(alpha = 0.16f),
    scrim = Frost,
    surfaceBright = Frost,
    // Dense enough that text on the drawer still reads over a dark patch of the wallpaper.
    surfaceDim = Starlight.copy(alpha = 0.92f),
    surfaceContainerLowest = Starlight.copy(alpha = 0.6f),
    surfaceContainerLow = lerp(Frost, Starlight, 0.4f),
    surfaceContainer = lerp(Frost, Starlight, 0.6f),
    surfaceContainerHigh = lerp(Frost, Starlight, 0.8f),
    surfaceContainerHighest = Starlight,
)

@Composable
fun LauncherTheme(lightWallpaper: Boolean = false, content: @Composable () -> Unit) {
    val (colors, ring) = if (lightWallpaper) LauncherDayColors to DayRing else LauncherColors to NightRing
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(
            // Pages sit straight on the wallpaper, so text defaults to the on-background colour; surfaces set their own.
            LocalContentColor provides colors.onBackground,
            LocalRingColors provides ring,
            // Elevation would tint a pane on top of the container ladder, which already sets how lit each tier is.
            LocalTonalElevationEnabled provides false,
            content = content,
        )
    }
}
