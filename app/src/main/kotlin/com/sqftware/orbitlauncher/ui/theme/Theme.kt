package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sqftware.orbitlauncher.domain.Colourway

// Only this package names a colour, and the rest of the app reaches them through the scheme's roles or the tokens here.

/**
 * The few colours a theme is made of, from which both its schemes are drawn: the [deep] ground of what floats, ink by
 * day; its [accent] and [warm] second accent; the [pale] of text by night; the [spark] of what is lit on the ring and
 * the [ringMark] of its track by night; the [alarm] of errors; and the [frost] that frosts the day's glass.
 */
internal class Palette(
    val deep: Color,
    val accent: Color,
    val pale: Color,
    val spark: Color,
    val warm: Color,
    val alarm: Color,
    val frost: Color = Color.White,
    val ringMark: Color = frost,
) {
    val glass = frost.copy(alpha = 0.1f)
    val dayGlass = frost.copy(alpha = 0.35f)
    val dayWarm = lerp(warm, deep, 0.5f)
    val mist = lerp(pale, deep, 0.28f)
    val dayAccent = lerp(deep, accent, 0.4f)
}

/**
 * The home ring's marks that lie on the wallpaper (its track and the lines between its slots, the emblem's edge, the ring
 * round a fold target), inked for the wallpaper like the scheme.
 */
class RingColors(val mark: Color, val starLine: Color, val lit: Color)

/**
 * The edge of a folder's glass disc: a dark line round a light one, the same in both schemes. The scheme follows the
 * wallpaper as a whole, but a disc sits on one patch of it, which may be light under the night scheme or dark under the
 * day's, so one of the two lines has to stand out on whatever is there.
 */
class DiscEdge(val outer: Color, val inner: Color)

/** Everything a theme sets for one kind of wallpaper, a dark or a light one. */
internal class LauncherLook(val colors: ColorScheme, val ring: RingColors, val panelMark: Color, val tonalEdge: Color)

internal fun lookOf(colourway: Colourway, lightWallpaper: Boolean): LauncherLook {
    val palette = paletteOf(colourway)
    return if (lightWallpaper) palette.dayLook() else palette.nightLook()
}

// Every colourway named here, so a new one does not compile until it has its colours.
private fun paletteOf(colourway: Colourway): Palette = when (colourway) {
    Colourway.Midnight -> SpacePalette
    Colourway.Nebula -> SpaceNebula
    Colourway.Aurora -> SpaceAurora
    Colourway.Mars -> SpaceMars
    Colourway.Brass -> ClockworkPalette
    Colourway.Steel -> ClockworkSteel
    Colourway.RoseGold -> ClockworkRoseGold
    Colourway.Emerald -> ClockworkEmerald
    Colourway.Parchment -> AtlasPalette
    Colourway.Nautical -> AtlasNautical
    Colourway.Desert -> AtlasDesert
    Colourway.Forest -> AtlasForest
    Colourway.Sand -> ZenPalette
    Colourway.Fern -> ZenFern
    Colourway.Slate -> ZenSlate
    Colourway.Sakura -> ZenSakura
    Colourway.Sumi -> InkPalette
    Colourway.Indigo -> InkIndigo
    Colourway.Sepia -> InkSepia
    Colourway.Lime -> GermPalette
    Colourway.Coral -> PaperPalette
    Colourway.Ice -> CrystalPalette
    Colourway.Green -> GreenCorePalette
}

val LocalRingColors = staticCompositionLocalOf { lookOf(Colourway.Midnight, lightWallpaper = false).ring }

/**
 * The faint marks the theme scatters over a full-screen panel's veil, at their brightest; each is dimmer by its own
 * share. Pale on the night's dark veil, and on the day's frosted one the accent inked towards the deep, so they show
 * without turning a panel busy.
 */
val LocalPanelMark = staticCompositionLocalOf { lookOf(Colourway.Midnight, lightWallpaper = false).panelMark }

/** The rim of a tonal button, whose tint is too faint to hold its edge on a light wallpaper. */
val LocalTonalEdge = staticCompositionLocalOf { lookOf(Colourway.Midnight, lightWallpaper = false).tonalEdge }

/** What a glyph is drawn in before `Icon` tints it, as Material's own icons are. */
val GlyphFill = Color.Black

/**
 * Built with the full constructor, so no role is left on Material's stock greys. The surfaces come in two tiers of the
 * deep. What lies behind a page's content lets the wallpaper through: pages are clear, `surface` (every default
 * container) and cards are faint glass, and full-screen panels (the drawer, and every `Panel`) take the veil of
 * `surfaceDim` from their one `PanelGround`, so a container on one never stacks a second veil. Menus and dialogs float
 * on the same ground made opaque, so they read as the drawer does. Anything else that floats over other content (sheets,
 * a bin, edit handles, a box within a dialog) takes `surfaceContainerLow` and up, which are opaque lit deep, or the icons
 * beneath would show through. A tonal button's `secondaryContainer` is a faint tint, of the deep by night
 * and frost by day, rimmed by the tonal edge, so it shows on a light wallpaper without drawing the eye. Content colours
 * are all opaque, so their contrast does not hang on the wallpaper.
 */
private fun Palette.nightLook() = LauncherLook(
    colors = ColorScheme(
        primary = accent,
        onPrimary = deep,
        primaryContainer = lerp(deep, accent, 0.5f),
        onPrimaryContainer = pale,
        inversePrimary = lerp(accent, deep, 0.5f),
        secondary = mist,
        onSecondary = deep,
        secondaryContainer = deep.copy(alpha = 0.25f),
        onSecondaryContainer = pale,
        tertiary = warm,
        onTertiary = deep,
        tertiaryContainer = lerp(deep, warm, 0.5f),
        onTertiaryContainer = pale,
        background = Color.Transparent,
        onBackground = pale,
        surface = glass,
        onSurface = pale,
        surfaceVariant = glass,
        onSurfaceVariant = mist,
        surfaceTint = accent,
        inverseSurface = pale,
        inverseOnSurface = deep,
        error = alarm,
        onError = deep,
        errorContainer = lerp(deep, alarm, 0.5f),
        onErrorContainer = pale,
        outline = lerp(pale, deep, 0.4f),
        outlineVariant = frost.copy(alpha = 0.16f),
        scrim = deep,
        surfaceBright = lerp(deep, frost, 0.22f),
        // Dense enough that text on the drawer still reads over a white wallpaper.
        surfaceDim = deep.copy(alpha = 0.85f),
        surfaceContainerLowest = deep.copy(alpha = 0.6f),
        surfaceContainerLow = lerp(deep, frost, 0.1f),
        surfaceContainer = lerp(deep, frost, 0.14f),
        surfaceContainerHigh = lerp(deep, frost, 0.18f),
        surfaceContainerHighest = lerp(deep, frost, 0.22f),
        primaryFixed = accent,
        primaryFixedDim = lerp(accent, deep, 0.2f),
        onPrimaryFixed = deep,
        onPrimaryFixedVariant = lerp(deep, accent, 0.3f),
        secondaryFixed = pale,
        secondaryFixedDim = lerp(pale, deep, 0.2f),
        onSecondaryFixed = deep,
        onSecondaryFixedVariant = lerp(deep, pale, 0.3f),
        tertiaryFixed = warm,
        tertiaryFixedDim = lerp(warm, deep, 0.2f),
        onTertiaryFixed = deep,
        onTertiaryFixedVariant = lerp(deep, warm, 0.3f),
    ),
    ring = RingColors(mark = ringMark, starLine = accent.copy(alpha = 0.7f), lit = spark),
    panelMark = pale.copy(alpha = 0.5f),
    tonalEdge = deep.copy(alpha = 0.5f),
)

/**
 * The night's look turned over for a light wallpaper, which the system says wants dark text: the same two tiers, with the
 * glass and the veil frosted instead of deep, what floats opaque lit frost, and deep ink for content. The scrim turns to
 * frost too, so the shades that lift the chevron and the navigation icons off the wallpaper stay light behind dark marks.
 * The fixed roles, the clear background and the tint stay the night's.
 */
private fun Palette.dayLook() = nightLook().let { night ->
    LauncherLook(
        colors = night.colors.copy(
            primary = lerp(deep, accent, 0.3f),
            onPrimary = pale,
            primaryContainer = lerp(frost, accent, 0.5f),
            onPrimaryContainer = deep,
            inversePrimary = accent,
            secondary = lerp(deep, pale, 0.3f),
            onSecondary = pale,
            secondaryContainer = dayGlass,
            onSecondaryContainer = deep,
            tertiary = dayWarm,
            onTertiary = pale,
            tertiaryContainer = lerp(frost, warm, 0.5f),
            onTertiaryContainer = deep,
            onBackground = deep,
            surface = dayGlass,
            onSurface = deep,
            surfaceVariant = dayGlass,
            onSurfaceVariant = lerp(deep, pale, 0.25f),
            inverseSurface = deep,
            inverseOnSurface = pale,
            error = lerp(alarm, deep, 0.5f),
            onError = pale,
            errorContainer = lerp(frost, alarm, 0.4f),
            onErrorContainer = deep,
            outline = lerp(deep, pale, 0.5f),
            outlineVariant = deep.copy(alpha = 0.16f),
            scrim = frost,
            surfaceBright = frost,
            // Dense enough that text on the drawer still reads over a dark patch of the wallpaper.
            surfaceDim = pale.copy(alpha = 0.92f),
            surfaceContainerLowest = pale.copy(alpha = 0.6f),
            surfaceContainerLow = lerp(frost, pale, 0.4f),
            surfaceContainer = lerp(frost, pale, 0.6f),
            surfaceContainerHigh = lerp(frost, pale, 0.8f),
            surfaceContainerHighest = pale,
        ),
        ring = RingColors(mark = deep, starLine = dayAccent.copy(alpha = 0.7f), lit = dayWarm),
        panelMark = dayAccent.copy(alpha = 0.4f),
        tonalEdge = night.tonalEdge,
    )
}

@Composable
fun LauncherTheme(colourway: Colourway, lightWallpaper: Boolean, content: @Composable () -> Unit) {
    val look = remember(colourway, lightWallpaper) { lookOf(colourway, lightWallpaper) }
    MaterialTheme(colorScheme = look.colors) {
        CompositionLocalProvider(
            // Pages sit straight on the wallpaper, so text defaults to the on-background colour; surfaces set their own.
            LocalContentColor provides look.colors.onBackground,
            LocalRingColors provides look.ring,
            LocalPanelMark provides look.panelMark,
            LocalTonalEdge provides look.tonalEdge,
            // Elevation would tint a pane on top of the container ladder, which already sets how lit each tier is.
            LocalTonalElevationEnabled provides false,
            content = content,
        )
    }
}
