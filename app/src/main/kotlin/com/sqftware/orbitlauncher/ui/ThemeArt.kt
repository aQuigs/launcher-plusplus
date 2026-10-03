package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.Colourway
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.Planet
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.ui.theme.LauncherTheme
import com.sqftware.orbitlauncher.ui.theme.RingColors

/**
 * The shapes a theme draws its own way, where its colours alone would not make it: the ring's centre, the marks round the
 * ring, a folder, and what lies on a full-screen panel's veil. Their colours still come from `ui/theme`.
 */
interface ThemeArt {
    /**
     * What the emblem's hint is written in, and the shadow that keeps it legible, over the emblem's own face: by default
     * the content colour on its inverse, for a face drawn in the scheme's roles.
     */
    val hintInk: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface

    val hintShade: Color
        @Composable get() = MaterialTheme.colorScheme.inverseOnSurface

    /**
     * The emblem's face, filling the item: its ground, and its mark unless [marked] is false because a hint is shown
     * over it. [slowTurn] and [fastTurn] are angles in degrees that move while the ring turns, once in ten minutes and
     * once a minute, and start over at each turn; [fastTurn] is also the clock what moves on the face keeps time by, in
     * [cycles]. [minuteOfDay] is the time, read where it is drawn so the minute ticking over redraws only what shows it.
     */
    @Composable
    fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier)

    /**
     * Builds, once for a size, what draws the ring's marks round [centre]: [slots] are the centres of the items on a
     * circle of [radius], each a disc [iconSize] across, which the marks keep clear of. The drawing takes how much the
     * ring glows as a drop target, from 0 to 1, and the alpha it is drawn at.
     */
    fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit

    /**
     * [folder] in the look of [LocalFolderStyle], filling the item: a disc of its previews the size of an app's, with
     * whatever the look wears reaching past it. [inner] fades the previews, as when the folder takes the ring's centre and
     * its apps are round it instead; [presses] ripple its disc. It is given a layer of its own, so what turns with the
     * sky redraws only the folder, not the page round it.
     */
    @Composable
    fun FolderFace(
        folder: RingItem.Folder,
        icon: suspend (AppEntry) -> ImageBitmap?,
        modifier: Modifier,
        presses: InteractionSource?,
        inner: () -> Float,
    )

    /** The faint pattern over a panel's veil, in [colour] at its brightest, drifting by a share of how far the list has [scrolled]. */
    fun DrawScope.drawBackdrop(colour: Color, scrolled: Float)

    /** The theme's world as a scene the launcher's menu offers to set as the wallpaper, if it has one. */
    val scene: ThemeScene?
        get() = null
}

/**
 * How many times something done [perTurn] times a turn has been done when an angle that turns has come [turn] degrees
 * round: a whole number a turn, so it never jumps as the angle starts over.
 */
internal fun cycles(turn: Float, perTurn: Int) = turn / 360f * perTurn

/** A scene of a theme's world, [name]d in the launcher's menu, which the launcher can set as the wallpaper. */
class ThemeScene(val name: String, val draw: DrawScope.() -> Unit)

/** The scene drawn on a new image [size] pixels across, as the wallpaper it becomes. It draws in software, so it may run off the main thread. */
fun ThemeScene.image(size: IntSize, density: Density): ImageBitmap =
    ImageBitmap(size.width, size.height).also { image ->
        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), size.toSize(), draw)
    }

private fun artOf(theme: Theme): ThemeArt = when (theme) {
    Theme.Space -> SpaceArt
    Theme.Clockwork -> ClockworkArt
    Theme.Atlas -> AtlasArt
    Theme.ZenGarden -> ZenArt
    Theme.Ink -> InkArt
    Theme.Germ -> GermArt
    Theme.Paper -> PaperArt
    Theme.CrystalCity -> CrystalArt
    Theme.GreenCore -> GreenCoreArt
}

val LocalThemeArt = staticCompositionLocalOf<ThemeArt> { SpaceArt }

/** [content] in [theme]: its [colourway] for a light or a dark wallpaper, and its art, chosen together. */
@Composable
fun Themed(theme: Theme, colourway: Colourway, lightWallpaper: Boolean, content: @Composable () -> Unit) {
    LauncherTheme(colourway, lightWallpaper) {
        CompositionLocalProvider(LocalThemeArt provides artOf(theme), content = content)
    }
}

/**
 * How every folder on the screen draws itself: the [look] the user chose, the [planets] the folders are in the Solar
 * system, and the [minutes] the sky has turned, which move what moves.
 */
class FolderStyle(
    val look: FolderLook,
    val planets: Map<HomePlace.Folder, Planet> = emptyMap(),
    val minutes: () -> Float = { 0f },
)

val LocalFolderStyle = compositionLocalOf { FolderStyle(Theme.Space.folderLooks.first()) }
