package com.sqftware.orbitlauncher.ui

import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.sqftware.orbitlauncher.domain.CardColour
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import com.sqftware.orbitlauncher.domain.Unread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import com.sqftware.orbitlauncher.domain.Colourway
import com.sqftware.orbitlauncher.domain.DOCK_ICON_FILL
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.IconPlace
import com.sqftware.orbitlauncher.domain.Planet
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.ui.theme.LauncherTheme
import com.sqftware.orbitlauncher.ui.theme.LocalBoxColors
import com.sqftware.orbitlauncher.ui.theme.LocalGround
import com.sqftware.orbitlauncher.ui.theme.RingColors

/**
 * The shapes a theme draws its own way, where its colours alone would not make it: the ring's centre, the marks round the
 * ring, a folder, and what lies on a full-screen panel's veil. Their colours still come from `ui/theme`.
 */
interface ThemeArt {
    /**
     * What the emblem's hint is written in over the emblem's own face: by default the content colour, for a face drawn in
     * the scheme's roles.
     */
    val hintInk: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface

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
     * What [folder] wears in the look of [LocalFolderStyle] round the previews [FolderFace] lays out, which are the same
     * in every look: by default nothing, on an app's ground.
     */
    @Composable
    fun folderWear(folder: RingItem.Folder): FolderWear = FolderWear()

    /** The faint pattern over a panel's veil, in [colour] at its brightest, drifting by a share of how far the list has [scrolled]. */
    fun DrawScope.drawBackdrop(colour: Color, scrolled: Float)

    /**
     * Builds what lays a panel's [veil] over a rect at a share of its own alpha: by default the flat colour. Every part
     * of one panel is drawn in the same coordinates, so a texture lines up across them.
     */
    @Composable
    fun panelVeil(veil: Color): DrawScope.(alpha: Float, topLeft: Offset, size: Size) -> Unit = remember(veil) { flatVeil(veil) }

    /** The theme's world as a scene the launcher's menu offers to set as the wallpaper, if it has one. */
    val scene: ThemeScene?
        get() = null

    /**
     * What an app's icon, or a folder's previews, sit on, drawn under them on a layer the size of the disc, which it
     * may reach past: by default the disc. Only drawn, never round the icon, so it cannot move or size it.
     * [LocalIconPlace] says whether the icon is on the home ring or elsewhere, where a theme may frame it differently.
     */
    @Composable
    fun iconGround(): Modifier = Modifier.background(LocalBoxColors.current.on(LocalGround.current), CircleShape)

    /** The shape an icon is clipped to and its press ripples within, or none for an icon that keeps its own outline. */
    val iconShape: Shape?
        get() = CircleShape

    /** An icon's [bitmap] in its square: by default filling it, scaled smoothly. */
    @Composable
    fun IconImage(bitmap: ImageBitmap, modifier: Modifier) = Image(bitmap, contentDescription = null, modifier = modifier)

    /** The badge for [unread] notifications, placed over an icon's corner by [modifier]: by default a bubble. */
    @Composable
    fun BadgeFace(unread: Unread, modifier: Modifier) = BubbleBadge(unread, modifier)

    /**
     * What the theme shows along the top of the home page, over the clock, with the name of the place the user has just
     * discovered while one is to be shown, its [discovery]: by default nothing.
     */
    @Composable
    fun HomeHeader(discovery: String?, modifier: Modifier) = Unit

    /** The home clock's [time], one line, tapped through [modifier]: by default in the scheme's largest light type. */
    @Composable
    fun ClockTime(time: String, modifier: Modifier) = PlainClockTime(time, modifier)

    /** How large a dock's icons are while they all fit, and what share of its slot each fills. */
    val dockIconSize: Dp
        @Composable get() = 56.dp

    val dockIconFill: Float
        get() = DOCK_ICON_FILL

    /**
     * Builds what draws under the dock's [slots][DockSlots], glowing by how much the dock is lit as a drop target: by
     * default nothing but that glow, in [glowColour].
     */
    @Composable
    fun dockGround(glowColour: Color): DrawScope.(DockSlots) -> Unit = { slots ->
        if (slots.glow > 0f) drawRoundRect(glowColour.copy(alpha = 0.12f * slots.glow), cornerRadius = CornerRadius(size.height / 2))
    }

    /** What opens and closes the drawer above the pages, pointing the way it will move: by default a chevron. */
    @Composable
    fun DrawerHandle(open: Boolean, onClick: () -> Unit, modifier: Modifier) = ChevronHandle(open, onClick, modifier)

    /** A menu's box on [ground]: by default the solid ground in Material's menu shape, rimmed so it holds its edge. */
    @Composable
    fun menuBox(ground: PanelGround): Modifier = Modifier.solidGround(ground, MenuDefaults.shape)

    val menuRim: BorderStroke?
        @Composable get() = popupRim()

    /** The room above and below a menu's items: by default Material's. */
    val menuPadding: Dp
        get() = MATERIAL_MENU_PADDING

    /**
     * A collection card round its [header] and its [body], on the ground of the [colour] the user chose, solid while
     * [dragged] and marked while it is the [dropTarget]: by default a rounded card of the colour's tint.
     */
    @Composable
    fun CollectionCardBox(
        colour: CardColour,
        dragged: Boolean,
        dropTarget: Boolean,
        modifier: Modifier,
        header: @Composable () -> Unit,
        body: @Composable () -> Unit,
    ) = PlainCardBox(colour, dragged, dropTarget, modifier, header, body)

    /** A collection's [glyph] and [title] at the start of its card's header: by default the glyph beside the title. */
    @Composable
    fun CardTitle(glyph: ImageVector, title: String, modifier: Modifier) = PlainCardTitle(glyph, title, modifier)

    /** The face of a card header's button, its [glyph] in [tint] and turned by [glyphModifier]: by default the glyph alone. */
    @Composable
    fun CardButtonFace(glyph: ImageVector, contentDescription: String?, tint: Color, glyphModifier: Modifier) =
        Icon(glyph, contentDescription, glyphModifier, tint)

    /** The bin an app is dropped on to take it out of a collection, filling its target, lit while [highlighted]: by default a disc. */
    @Composable
    fun BinFace(highlighted: Boolean, modifier: Modifier) = PlainBinFace(highlighted, modifier)

    /** A dialog's box on [ground]: by default the solid ground in Material's dialog shape. */
    @Composable
    fun dialogBox(ground: PanelGround): Modifier = Modifier.solidGround(ground, AlertDialogDefaults.shape)

    /** The drawer's search field, editing [query] and calling [onSearch] from the keyboard's search key: by default Material's outlined field. */
    @Composable
    fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier) =
        PlainSearchField(query, onQueryChange, onSearch, modifier)

    /** A row of buttons, one to each of the [choices], one of them chosen: by default Material's segmented buttons. */
    @Composable
    fun ChoiceButtons(choices: List<Choice>, modifier: Modifier) = SegmentedChoices(choices, modifier)

    /** A menu's items, [content], as the theme sets them out: by default as they are. */
    @Composable
    fun MenuBody(content: @Composable () -> Unit) = content()

    /**
     * What the emblem's box clips its face and its touch to, and draws round the face: by default the circle of its
     * cell, rimmed faintly in [edge].
     */
    @Composable
    fun emblemBox(edge: Color): Modifier =
        Modifier.clip(CircleShape).drawBehind { drawCircle(edge.copy(alpha = 0.35f), radius = size.emblemRadius, style = Stroke(1.dp.toPx())) }

    /** Whether an open folder's apps go round the emblem, which a tap then closes, rather than round the folder's own face. */
    val foldersOpenRoundEmblem: Boolean
        get() = false

    /** The type every text is set in, by [day] when the day scheme is in use. */
    @Composable
    fun typography(day: Boolean): Typography = MaterialTypography

    /**
     * The corners of every surface: by default Material's. The fully round ones, a choice's segments and a colour's
     * swatch, are drawn in the largest, so a theme of square corners squares them too.
     */
    val shapes: Shapes
        get() = MaterialShapes
}

/** A flat [veil], at a share of its own alpha. */
fun flatVeil(veil: Color): DrawScope.(alpha: Float, topLeft: Offset, size: Size) -> Unit =
    { alpha, topLeft, size -> drawRect(veil.copy(alpha = veil.alpha * alpha), topLeft, size) }

/** The dock's slots as laid out on its row: each icon [iconSize] across, the nth starting [starts] along, and how much it [glow]s. */
class DockSlots(val iconSize: Float, val starts: List<Float>, val glow: Float)

val LocalIconPlace = staticCompositionLocalOf { IconPlace.Elsewhere }

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
    Theme.OldSchool -> OldSchoolArt
    Theme.Skyrim -> SkyrimArt
}

val LocalThemeArt = staticCompositionLocalOf<ThemeArt> { SpaceArt }

/** [content] in [theme]: its [colourway] for a light or a dark wallpaper, and its art, chosen together. */
@Composable
fun Themed(theme: Theme, colourway: Colourway, lightWallpaper: Boolean, content: @Composable () -> Unit) {
    val art = artOf(theme)
    LauncherTheme(colourway, lightWallpaper, art.typography(lightWallpaper), art.shapes) {
        CompositionLocalProvider(LocalThemeArt provides art, content = content)
    }
}

private val MaterialTypography = Typography()
private val MaterialShapes = Shapes()

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
