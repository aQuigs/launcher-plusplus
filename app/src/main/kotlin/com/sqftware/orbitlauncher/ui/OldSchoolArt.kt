package com.sqftware.orbitlauncher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.border
import com.sqftware.orbitlauncher.ui.theme.MenuTarget
import com.sqftware.orbitlauncher.ui.theme.MenuVerb
import com.sqftware.orbitlauncher.ui.theme.MenuRim
import com.sqftware.orbitlauncher.ui.theme.MenuGround
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.Unread
import com.sqftware.orbitlauncher.domain.badgeText
import com.sqftware.orbitlauncher.ui.theme.GameShadow
import com.sqftware.orbitlauncher.ui.theme.MinimapGrass
import com.sqftware.orbitlauncher.ui.theme.MinimapSelf
import com.sqftware.orbitlauncher.ui.theme.MinimapSelfEdge
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.StackCount
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** How many of the game's pixels wide the screen is: the mobile client's portrait width, which sets how big one is. */
private const val GAME_WIDTH = 270

/** One of the game's pixels in screen pixels, whole so every sprite stays crisp: four on a 1080 pixel wide phone. */
internal val gamePixel: Int
    @Composable get() = max(2, LocalWindowInfo.current.containerSize.width / GAME_WIDTH)

/** The minimap's frame and its compass, as the game draws them: where the frame's map hole is centred, and its radius. */
private val FRAME_HOLE_CENTRE = IntOffset(100, 84)
private const val MAP_RADIUS = 76
private const val COMPASS_RADIUS = 17
private val COMPASS_CENTRE = IntOffset(22, 22)

/** The game's round orb, clipped from its frame: where its circle is centred in the sprite, and its radius. */
private val ORB_CENTRE = IntOffset(40, 17)
private const val ORB_RADIUS = 17

/** How far each of a pile's two items sits from the middle, in the game's pixels: the one behind up and over, the one in front down and back. */
private val PILE_BEHIND = IntOffset(4, -3)
private val PILE_FRONT = IntOffset(-3, 4)

/**
 * The game's row of tab stones: a stone's face, as wide as a dock icon, and the pitch they repeat at; how tall a stone
 * is; and how wide each of the row's rounded end stones is.
 */
private const val STONE_FACE = 31
private const val STONE_PITCH = 33
private const val STONE_HEIGHT = 36
private const val END_STONE = 38

/** The game's own type: its plain, bold and small fonts, drawn whole game pixels high. */
private val GameFont = FontFamily(Font(R.font.osrs_plain), Font(R.font.osrs_bold, FontWeight.Bold))
private val GameSmallFont = FontFamily(Font(R.font.osrs_small))

/** The size the game's fonts are drawn at, in its pixels. */
private const val GAME_FONT_SIZE = 16

/** The quill digits of the game's clock, '0' to '9' and ':', each a sprite of the cache's quill font. */
private val QUILL = listOf(
    R.drawable.osrs_497_48, R.drawable.osrs_497_49, R.drawable.osrs_497_50, R.drawable.osrs_497_51, R.drawable.osrs_497_52,
    R.drawable.osrs_497_53, R.drawable.osrs_497_54, R.drawable.osrs_497_55, R.drawable.osrs_497_56, R.drawable.osrs_497_57,
    R.drawable.osrs_497_58,
)

/** How many times a game pixel the clock's quill digits are drawn, the size the mobile client gives its largest type. */
private const val CLOCK_SCALE = 2

/**
 * Old School: an homage to the old game's interface, drawn from its own sprites at whole game pixels. The emblem is the
 * minimap in its frame with the compass, the ring's apps sit in its round orbs, every app is one of its items, a
 * folder is a pile of two, badges are an item's yellow stack count, and text is in its fonts.
 */
object OldSchoolArt : ThemeArt {
    override val hintInk: Color
        @Composable get() = StackCount

    override val hintShade: Color
        @Composable get() = GameShadow

    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val frame = sprite(R.drawable.osrs_1177_0)
        val compass = sprite(R.drawable.osrs_169_0)
        val px = gamePixel
        Spacer(
            modifier.drawWithCache {
                // As large as fits the emblem, and never larger than the rest of the game's pixels.
                val scale = max(1, min(px, floor(size.minDimension / (2f * (MAP_RADIUS + 6))).toInt()))
                val centre = Offset(size.width / 2, size.height / 2)
                val origin = centre - (FRAME_HOLE_CENTRE * scale).toOffset()
                val compassAt = origin + (COMPASS_CENTRE * scale).toOffset()
                val compassHole = Path().apply { addOval(Rect(compassAt, COMPASS_RADIUS * scale.toFloat())) }
                onDrawBehind {
                    drawCircle(MinimapGrass, MAP_RADIUS * scale.toFloat(), centre)
                    if (marked) selfMarker(centre, scale)
                    clipPath(compassHole) { drawSprite(compass, compassAt - Offset(compass.width.toFloat(), compass.height.toFloat()) * (scale / 2f), scale) }
                    drawSprite(frame, origin, scale)
                }
            },
        )
    }

    /** The minimap is the whole of the game's ring; nothing more is drawn round the slots. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit = { _, _ -> }

    @Composable
    override fun FolderFace(
        folder: RingItem.Folder,
        icon: suspend (AppEntry) -> ImageBitmap?,
        modifier: Modifier,
        presses: InteractionSource?,
        inner: () -> Float,
    ) {
        val px = gamePixel
        IconGround(LocalIconPlace.current, presses, modifier, Alignment.Center) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                folder.apps.getOrNull(1)?.let { PileItem(it, icon, PILE_BEHIND, px, inner) }
                folder.apps.firstOrNull()?.let { PileItem(it, icon, PILE_FRONT, px, inner) }
            }
        }
    }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) = Unit

    @Composable
    override fun IconGround(
        place: IconPlace,
        presses: InteractionSource?,
        modifier: Modifier,
        contentAlignment: Alignment,
        content: @Composable BoxScope.() -> Unit,
    ) {
        if (place == IconPlace.Ring) {
            val frame = sprite(R.drawable.osrs_1071_0)
            val fill = sprite(R.drawable.osrs_1059_0)
            val px = gamePixel
            Box(
                modifier.drawBehind {
                    val centre = Offset(size.width / 2, size.height / 2)
                    val orb = Path().apply { addOval(Rect(centre, ORB_RADIUS * px.toFloat())) }
                    clipPath(orb) { drawSprite(frame, centre - (ORB_CENTRE * px).toOffset(), px) }
                    drawSprite(fill, centre - Offset(fill.width.toFloat(), fill.height.toFloat()) * (px / 2f), px)
                },
                contentAlignment = contentAlignment,
                content = content,
            )
        } else {
            Box(modifier, contentAlignment = contentAlignment, content = content)
        }
    }

    /** An item drawn as the game draws it, at whole game pixels, as large as its square takes and no larger than the game's. */
    @Composable
    override fun IconImage(bitmap: ImageBitmap, modifier: Modifier) {
        val px = gamePixel
        Spacer(
            modifier.drawBehind {
                val scale = max(1, min(px, floor(size.minDimension / ITEM_FIT).toInt()))
                drawSprite(bitmap, Offset(size.width, size.height) / 2f - Offset(bitmap.width.toFloat(), bitmap.height.toFloat()) * (scale / 2f), scale)
            },
        )
    }

    override val badgeCorner: Alignment
        get() = Alignment.TopStart

    /** An item's stack count: yellow, in the game's small font, at the item's top left. */
    @Composable
    override fun BadgeFace(unread: Unread, modifier: Modifier) {
        Text(badgeText(unread.count), modifier, color = StackCount, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }

    /** The time in the quill digits the game draws its largest headings in, anything else in its bold type. */
    @Composable
    override fun ClockTime(time: String, modifier: Modifier) {
        val digits = QUILL.map { sprite(it) }
        val px = gamePixel * CLOCK_SCALE
        val quill = time.takeWhile { it in '0'..':' }
        val rest = time.drop(quill.length).trim()
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            QuillLine(quill, digits, px)
            if (rest.isNotEmpty()) Text(" $rest", color = StackCount, style = MaterialTheme.typography.titleLarge)
        }
    }

    /** A dock icon is as wide as a tab stone's face, so the stones meet as the game's row does. */
    override val dockIconSize: Dp
        @Composable get() = with(LocalDensity.current) { (STONE_FACE * gamePixel).toDp() }

    override val dockIconFill: Float
        get() = STONE_FACE.toFloat() / STONE_PITCH

    /**
     * The game's row of tab stones under the dock's slots, one stone to a slot: the row's rounded end stones at its ends
     * and its middle stone between, all lit red, as a selected tab is, while the dock is a drop target.
     */
    @Composable
    override fun dockGround(glowColour: Color): DrawScope.(DockSlots) -> Unit {
        val row = sprite(R.drawable.osrs_1174_0)
        val stone = sprite(R.drawable.osrs_1180_0)
        val lit = sprite(R.drawable.osrs_1181_0)
        val px = gamePixel
        return { slots ->
            val count = slots.starts.size
            val scale = max(1, min(px, floor(slots.iconSize / STONE_FACE).toInt()))
            val top = size.height / 2 - STONE_HEIGHT * scale / 2f
            slots.starts.forEachIndexed { index, start ->
                val centre = start + slots.iconSize / 2
                val half = STONE_PITCH * scale / 2f
                when {
                    count == 1 || index in 1 until count - 1 -> drawSprite(stone, Offset(centre - stone.width * scale / 2f, top), scale)
                    index == 0 -> drawSpritePart(row, 0, END_STONE, Offset(centre + half - END_STONE * scale, top), scale)
                    else -> drawSpritePart(row, row.width - END_STONE, END_STONE, Offset(centre - half, top), scale)
                }
                if (slots.glow > 0f) drawSprite(lit, Offset(centre - lit.width * scale / 2f, top), scale, alpha = slots.glow * 0.7f)
            }
        }
    }

    /** The inventory tab: a stone with the game's backpack, lit red while the drawer is open as a selected tab is. */
    @Composable
    override fun DrawerHandle(open: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val stone = sprite(if (open) R.drawable.osrs_1181_0 else R.drawable.osrs_1180_0)
        val bag = sprite(R.drawable.osrs_777_0)
        val px = gamePixel
        with(LocalDensity.current) {
            Spacer(
                modifier
                    .clickable(onClickLabel = null, onClick = onClick)
                    .semantics { contentDescription = if (open) "Close the app drawer" else "Open the app drawer" }
                    .testTag(AppDrawerTags.HANDLE)
                    .size((stone.width * px).toDp(), (stone.height * px).toDp())
                    .drawBehind {
                        drawSprite(stone, Offset.Zero, px)
                        drawSprite(bag, Offset((stone.width - bag.width) * px / 2f, (stone.height - bag.height) * px / 2f), px)
                    },
            )
        }
    }

    /** The game's right-click menu: a black box in a one pixel brown rim. */
    @Composable
    override fun menuBox(ground: PanelGround): Modifier = Modifier.background(MenuGround)

    override val menuRim: BorderStroke
        @Composable get() = BorderStroke(with(LocalDensity.current) { gamePixel.toDp() }, MenuRim)

    /** A collection is a bank tab's square box, in the menu's brown rim. */
    override val cardShape: Shape
        get() = RectangleShape

    override val cardRim: BorderStroke
        @Composable get() = menuRim

    /** The game's stone-framed interface box: square, rimmed in black outside the menu's brown, a game pixel each. */
    @Composable
    override fun dialogBox(ground: PanelGround): Modifier {
        val rim = with(LocalDensity.current) { gamePixel.toDp() }
        return Modifier.border(rim, GameShadow).padding(rim).border(rim, MenuRim).solidGround(ground, RectangleShape)
    }

    /** "Choose Option" in the rim's brown over the items, which are in white with any secondary text in the target's orange. */
    @Composable
    override fun MenuBody(content: @Composable () -> Unit) {
        Text(
            "Choose Option",
            color = MenuRim,
            style = MaterialTheme.typography.labelLarge.copy(shadow = null),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).semantics { hideFromAccessibility() },
        )
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(onSurface = MenuVerb, onSurfaceVariant = MenuTarget)) { content() }
    }

    override val typography: Typography
        @Composable get() {
            val px = gamePixel
            val density = LocalDensity.current
            return remember(px, density) { gameTypography(px, density) }
        }
}

/** An item fits a square this many game pixels across: its canvas less the clear rows round the mark. */
private const val ITEM_FIT = 28f

@Composable
private fun sprite(@DrawableRes id: Int): ImageBitmap = ImageBitmap.imageResource(id)

/** [image] with its top left at [topLeft], each of its pixels [scale] screen pixels square and as sharp as the game's. */
private fun DrawScope.drawSprite(image: ImageBitmap, topLeft: Offset, scale: Int, tint: Color? = null, alpha: Float = 1f) =
    drawSpritePart(image, 0, image.width, topLeft, scale, tint, alpha)

/** The columns of [image] from [from], [width] of them, drawn as [drawSprite] draws the whole. */
private fun DrawScope.drawSpritePart(
    image: ImageBitmap,
    from: Int,
    width: Int,
    topLeft: Offset,
    scale: Int,
    tint: Color? = null,
    alpha: Float = 1f,
) = drawImage(
    image,
    srcOffset = IntOffset(from, 0),
    srcSize = IntSize(width, image.height),
    dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
    dstSize = IntSize(width * scale, image.height * scale),
    alpha = alpha,
    colorFilter = tint?.let { ColorFilter.tint(it) },
    filterQuality = FilterQuality.None,
)

private operator fun IntOffset.times(scale: Int) = IntOffset(x * scale, y * scale)

private fun IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())

/** The player's own square in the minimap's middle, three game pixels wide in a ring of black so it shows on a white wall. */
private fun DrawScope.selfMarker(centre: Offset, scale: Int) {
    val edge = Size(5f * scale, 5f * scale)
    val self = Size(3f * scale, 3f * scale)
    drawRect(MinimapSelfEdge, centre - Offset(edge.width, edge.height) / 2f, edge)
    drawRect(MinimapSelf, centre - Offset(self.width, self.height) / 2f, self)
}

/** One of a pile's items, [offset] game pixels from the middle, fading by [inner] as the open folder's previews do. */
@Composable
private fun PileItem(app: AppEntry, icon: suspend (AppEntry) -> ImageBitmap?, offset: IntOffset, px: Int, inner: () -> Float) {
    val density = LocalDensity.current
    with(density) {
        AppImage(
            app,
            icon,
            Modifier
                .size((ITEM_FIT * px).toDp())
                .graphicsLayer {
                    translationX = offset.x * px.toFloat()
                    translationY = offset.y * px.toFloat()
                    alpha = inner()
                },
        )
    }
}

/** [text]'s digits and colons in the quill [digits], each pixel [px] screen pixels, with the game's drop shadow under them. */
@Composable
private fun QuillLine(text: String, digits: List<ImageBitmap>, px: Int) {
    val inks = remember(digits) { digits.map(::inkColumns) }
    val glyphs = text.mapNotNull { c -> (c - '0').takeIf { it in digits.indices } }
    val width = glyphs.sumOf { inks[it].last - inks[it].first + QUILL_GAP } * px
    val height = (digits.first().height + 1) * px
    val shadow = px / CLOCK_SCALE
    with(LocalDensity.current) {
        Spacer(
            Modifier.size(width.toDp(), height.toDp()).drawBehind {
                var x = 0
                glyphs.forEach { index ->
                    val ink = inks[index]
                    val at = Offset(((x - ink.first) * px).toFloat(), 0f)
                    drawSprite(digits[index], at + Offset(shadow.toFloat(), shadow.toFloat()), px, GameShadow)
                    drawSprite(digits[index], at, px, StackCount)
                    x += ink.last - ink.first + QUILL_GAP
                }
            },
        )
    }
}

/** The pixels between two quill glyphs, as the game sets them. */
private const val QUILL_GAP = 2

/** The columns a glyph's cell has ink in, from its first to its last. */
private fun inkColumns(glyph: ImageBitmap): IntRange {
    val argb = IntArray(glyph.width * glyph.height)
    glyph.readPixels(argb)
    val inked = (0 until glyph.width).filter { x -> (0 until glyph.height).any { y -> argb[y * glyph.width + x] ushr 24 != 0 } }
    return if (inked.isEmpty()) 0..2 else inked.first()..inked.last()
}

/**
 * Every style in the game's fonts, each a whole number of screen pixels to one of the font's, with the font's one pixel
 * shadow. Half the game's scale for most text, as a phone's type is smaller than the game's, and up to twice it for
 * display type.
 */
private fun gameTypography(px: Int, density: Density): Typography {
    fun style(fontPixel: Int, family: FontFamily = GameFont, weight: FontWeight = FontWeight.Normal): TextStyle {
        val size = with(density) { (GAME_FONT_SIZE * fontPixel).toSp() }
        val shadow = Shadow(GameShadow, Offset(fontPixel.toFloat(), fontPixel.toFloat()), 0f)
        return TextStyle(fontFamily = family, fontWeight = weight, fontSize = size, lineHeight = size * 1.15f, shadow = shadow)
    }
    val small = max(1, px / 2)
    val mid = max(small + 1, px * 3 / 4)
    return Typography(
        displayLarge = style(px * 2, weight = FontWeight.Bold),
        displayMedium = style(px * 2, weight = FontWeight.Bold),
        displaySmall = style(px * 3 / 2, weight = FontWeight.Bold),
        headlineLarge = style(px * 3 / 2, weight = FontWeight.Bold),
        headlineMedium = style(px, weight = FontWeight.Bold),
        headlineSmall = style(px, weight = FontWeight.Bold),
        titleLarge = style(px, weight = FontWeight.Bold),
        titleMedium = style(mid, weight = FontWeight.Bold),
        titleSmall = style(mid),
        bodyLarge = style(mid),
        bodyMedium = style(mid),
        bodySmall = style(small),
        labelLarge = style(mid, weight = FontWeight.Bold),
        labelMedium = style(small),
        labelSmall = style(small, GameSmallFont),
    )
}
