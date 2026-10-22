package com.sqftware.orbitlauncher.ui

import com.sqftware.orbitlauncher.domain.ITEM_HEIGHT
import com.sqftware.orbitlauncher.domain.ITEM_WIDTH
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.toOffset
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import com.sqftware.orbitlauncher.ui.theme.StackCountDismissed
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.material3.ripple
import androidx.compose.foundation.indication
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.vector.ImageVector
import com.sqftware.orbitlauncher.domain.CardColour
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntRect
import com.sqftware.orbitlauncher.ui.theme.ChatTypedDay
import com.sqftware.orbitlauncher.ui.theme.ChatTypedNight
import com.sqftware.orbitlauncher.ui.theme.GameOrange
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

/**
 * The minimap's frame and its compass, as the game draws them: where the frame's map hole is centred, the radius of
 * the map and of the frame's outer edge; where the compass socket is centred in the frame, the radius of the compass's
 * face and of its socket's outer edge; and the way from the map's centre to the socket.
 */
private val FRAME_HOLE_CENTRE = IntOffset(100, 84)
private const val MAP_RADIUS = 76
private const val FRAME_RADIUS = 82
private val COMPASS_CENTRE = IntOffset(22, 22)
private const val COMPASS_RADIUS = 17
private const val SOCKET_RADIUS = 23
private val COMPASS_WAY = (COMPASS_CENTRE - FRAME_HOLE_CENTRE).toOffset().let { it / it.getDistance() }

/**
 * How far past its cell the emblem may reach, as a share of the cell: the frame fills the room inside the ring's apps
 * rather than leaving a wide gap to them.
 */
private const val EMBLEM_REACH = 1.15f

/**
 * The credit the map data's licences ask to show with the map, along the bottom of the disc: how wide it may run and
 * how far below the centre its foot is, in game pixels.
 */
private const val MAP_CREDIT = "© OpenMapTiles\n© OpenStreetMap"
private const val CREDIT_WIDTH = 104
private const val CREDIT_DROP = 70

/** The whole game pixels the minimap is drawn at in an emblem of [size]: as large as reaches its room, and no larger than the rest of the game. */
private fun mapScale(size: Size, px: Int) = fitScale(size.minDimension * EMBLEM_REACH, 2f * FRAME_RADIUS, px)

/** Where the compass is centred in an emblem of [size] with its minimap at [scale]. */
private fun compassAt(size: Size, scale: Int) = size.center + COMPASS_WAY * ((FRAME_RADIUS - SOCKET_RADIUS) * scale.toFloat())

private fun circle(centre: Offset, radius: Int) = Path().apply { addOval(Rect(centre, radius.toFloat())) }

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

    /**
     * The minimap in its frame: the map of where the user is, or the game's grass until there is one, turned with the
     * phone as the game turns it with the camera, and the compass, turned with it, on the frame's edge. What turns is a
     * layer of its own, so a turn moves it without drawing the frame again.
     */
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val frame = sprite(R.drawable.osrs_1177_0)
        val compass = sprite(R.drawable.osrs_169_0)
        val px = gamePixel
        val map = LocalMinimap.current
        val heading = LocalHeading.current
        val measurer = rememberTextMeasurer()
        val creditStyle = MaterialTheme.typography.labelSmall.copy(color = MinimapSelf, textAlign = TextAlign.Center)
        Box(modifier) {
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = -(heading() ?: 0f) }
                    .drawBehind {
                        val scale = mapScale(size, px)
                        map()?.let { drawSpriteCentred(it, center, scale) } ?: drawCircle(MinimapGrass, MAP_RADIUS * scale.toFloat())
                    },
            )
            Spacer(
                Modifier.matchParentSize().drawWithCache {
                    val scale = mapScale(size, px)
                    val ring = circle(size.center, FRAME_RADIUS * scale)
                    val at = compassAt(size, scale)
                    val socket = Path.combine(PathOperation.Difference, circle(at, SOCKET_RADIUS * scale), circle(at, COMPASS_RADIUS * scale))
                    val credit = measurer.measure(MAP_CREDIT, creditStyle, constraints = Constraints(maxWidth = CREDIT_WIDTH * scale))
                    onDrawBehind {
                        // Upright over the turned map, as the game keeps the player's square.
                        if (marked) selfMarker(center, scale)
                        if (map() != null) drawText(credit, topLeft = Offset(center.x - credit.size.width / 2f, center.y + CREDIT_DROP * scale - credit.size.height))
                        // The frame's own ring alone: its compass socket would reach out among the ring's apps, so the
                        // compass sits just inside the frame's edge, over the map, as in the game's fixed-size view.
                        clipPath(ring) { drawSprite(frame, center - (FRAME_HOLE_CENTRE * scale).toOffset(), scale) }
                        clipPath(socket) { drawSprite(frame, at - (COMPASS_CENTRE * scale).toOffset(), scale) }
                    }
                },
            )
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val at = compassAt(size, mapScale(size, px))
                        transformOrigin = TransformOrigin(at.x / size.width, at.y / size.height)
                        rotationZ = -(heading() ?: 0f)
                    }
                    .drawWithCache {
                        val scale = mapScale(size, px)
                        val at = compassAt(size, scale)
                        val face = circle(at, COMPASS_RADIUS * scale)
                        onDrawBehind { clipPath(face) { drawSpriteCentred(compass, at, scale) } }
                    },
            )
        }
    }

    /** The emblem clips to the frame's outer edge, which reaches past its cell, so the map and its compass show whole. */
    @Composable
    override fun emblemBox(edge: Color): Modifier {
        val px = gamePixel
        val shape = remember(px) { GenericShape { size, _ -> addOval(Rect(Offset(size.width / 2, size.height / 2), FRAME_RADIUS * mapScale(size, px).toFloat())) } }
        return Modifier.clip(shape)
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
            folder.apps.getOrNull(1)?.let { PileItem(it, icon, PILE_BEHIND, px, inner) }
            folder.apps.firstOrNull()?.let { PileItem(it, icon, PILE_FRONT, px, inner) }
        }
    }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) = Unit

    /**
     * By night the stone of the game's side panel, as the inventory has it, and by day the flat parchment of its
     * chatbox, which the day's dark text is read on.
     */
    @Composable
    override fun panelVeil(veil: Color): DrawScope.(alpha: Float, topLeft: Offset, size: Size) -> Unit {
        val stone = sprite(R.drawable.osrs_1031_0)
        val px = gamePixel
        val night = onStone
        return remember(veil, stone, px, night) {
            if (night) {
                { alpha, topLeft, size -> drawStone(stone, px, veil.alpha * alpha, topLeft, size) }
            } else {
                flatVeil(veil)
            }
        }
    }

    /**
     * The chatbox's input line: the typed text in the game's blue, with its asterisk for a caret, in a square box
     * rimmed as its interface boxes are, behind its magnifying glass.
     */
    @Composable
    override fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, modifier: Modifier) {
        val glass = sprite(R.drawable.osrs_535_0)
        val clear = sprite(R.drawable.osrs_773_0)
        val px = gamePixel
        val typed = if (onStone) ChatTypedNight else ChatTypedDay
        val style = MaterialTheme.typography.bodyLarge
        var focused by remember { mutableStateOf(false) }

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = modifier.onFocusChanged { focused = it.isFocused },
            textStyle = style.copy(color = typed),
            singleLine = true,
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { field ->
                Row(
                    Modifier.gameRim(px).heightIn(min = 56.dp).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpriteImage(glass, max(1, px / 2))
                    Row(Modifier.weight(1f).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f, fill = false)) { field() }
                        if (focused) Text("*", style = style, color = typed)
                        if (query.isEmpty()) {
                            Text("Search apps", style = style, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            SpriteImage(clear, px, Modifier.semantics { contentDescription = "Clear the search" })
                        }
                    }
                }
            },
        )
    }

    /** The chatbox's filter buttons: a stone each, the chosen one sunk, its label in white, or the game's orange once chosen. */
    @Composable
    override fun ChoiceButtons(choices: List<Choice>, modifier: Modifier) {
        val stone = sprite(R.drawable.osrs_3051_0)
        val sunk = sprite(R.drawable.osrs_3053_0)
        val px = gamePixel
        with(LocalDensity.current) {
            Row(modifier, horizontalArrangement = Arrangement.spacedBy(px.toDp())) {
                choices.forEach { choice ->
                    val face = if (choice.selected) sunk else stone
                    Box(
                        Modifier
                            .selectable(choice.selected, role = Role.Tab, onClick = choice.onSelect)
                            .testTag(choice.tag)
                            .size((face.width * px).toDp(), (face.height * px).toDp())
                            .drawBehind { drawSprite(face, Offset.Zero, px) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            choice.label,
                            color = if (choice.selected) GameOrange else MenuVerb,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

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
                modifier.drawWithCache {
                    val orb = circle(size.center, ORB_RADIUS * px)
                    onDrawBehind {
                        clipPath(orb) { drawSprite(frame, center - (ORB_CENTRE * px).toOffset(), px) }
                        drawSpriteCentred(fill, center, px)
                    }
                }.pressed(presses),
                contentAlignment = contentAlignment,
                content = content,
            )
        } else {
            Box(modifier.pressed(presses), contentAlignment = contentAlignment, content = content)
        }
    }

    /**
     * An item drawn as the game draws it, at whole game pixels, as large as its square takes and no larger than the
     * game's. An image larger than an item's canvas is no item, such as a pinned shortcut's icon, and is fitted whole.
     */
    @Composable
    override fun IconImage(bitmap: ImageBitmap, modifier: Modifier) {
        if (bitmap.width > ITEM_WIDTH || bitmap.height > ITEM_HEIGHT) return Image(bitmap, contentDescription = null, modifier = modifier)
        val px = gamePixel
        Spacer(modifier.drawBehind { drawSpriteCentred(bitmap, center, fitScale(size.minDimension, ITEM_FIT, px)) })
    }

    override val badgeCorner: Alignment
        get() = Alignment.TopStart

    /**
     * An item's stack count, in the game's small font at the item's top left: yellow while all its notifications are in
     * the shade, white once some are dismissed, as the game's white counts mark a different stack.
     */
    @Composable
    override fun BadgeFace(unread: Unread, modifier: Modifier) {
        Text(
            badgeText(unread.count),
            modifier.testTag(BadgeTags.BUBBLE),
            color = if (unread.dismissed) StackCountDismissed else StackCount,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }

    /** The time in the quill digits the game draws its largest headings in, anything else in its bold type. */
    @Composable
    override fun ClockTime(time: String, modifier: Modifier) {
        val digits = QUILL.map { sprite(it) }
        val px = gamePixel * CLOCK_SCALE
        val quill = time.takeWhile { it in '0'..':' }
        val rest = time.drop(quill.length).trim()
        Row(modifier.clearAndSetSemantics { text = AnnotatedString(time) }, verticalAlignment = Alignment.CenterVertically) {
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
            val scale = fitScale(slots.iconSize, STONE_FACE.toFloat(), px)
            val top = size.height / 2 - STONE_HEIGHT * scale / 2f
            slots.starts.forEachIndexed { index, start ->
                val centre = start + slots.iconSize / 2
                val half = STONE_PITCH * scale / 2f
                when {
                    count == 1 || index in 1 until count - 1 -> drawSprite(stone, Offset(centre - stone.width * scale / 2f, top), scale)
                    index == 0 -> drawSpritePart(row, IntRect(0, 0, END_STONE, row.height), Offset(centre + half - END_STONE * scale, top), scale)
                    else -> drawSpritePart(row, IntRect(row.width - END_STONE, 0, row.width, row.height), Offset(centre - half, top), scale)
                }
                if (slots.glow > 0f) drawSprite(lit, Offset(centre - lit.width * scale / 2f, top), scale, alpha = slots.glow * 0.7f)
            }
        }
    }

    /** The inventory tab's backpack, on the dock's last stone, which opens the drawer as the game's tab opens its inventory. */
    override val dockDrawerTab: @Composable (onClick: () -> Unit, modifier: Modifier) -> Unit = { onClick, modifier -> InventoryTab(onClick, modifier) }

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

    /** A collection is the game's bank, its title on the bank's tab and its header's buttons the game's stone ones. */
    @Composable
    override fun CollectionCardBox(
        colour: CardColour,
        dragged: Boolean,
        dropTarget: Boolean,
        modifier: Modifier,
        header: @Composable () -> Unit,
        body: @Composable () -> Unit,
    ) = BankCard(colour, dropTarget, modifier, header, body)

    @Composable
    override fun CardTitle(glyph: ImageVector, title: String, modifier: Modifier) = BankTab(glyph, title, modifier)

    @Composable
    override fun CardButtonFace(glyph: ImageVector, contentDescription: String?, tint: Color, glyphModifier: Modifier) =
        StoneButtonFace(glyph, contentDescription, glyphModifier)

    @Composable
    override fun BinFace(highlighted: Boolean, modifier: Modifier) = StoneBin(highlighted, modifier)

    /** A card's settings are boxed in the menu's brown rim. */
    @Composable
    override fun settingBox(): Modifier =
        Modifier.border(with(LocalDensity.current) { gamePixel.toDp() }, MenuRim).background(MaterialTheme.colorScheme.surfaceContainerHighest)

    /** The game's stone-framed interface box, rimmed in black outside the menu's brown, a game pixel each. */
    @Composable
    override fun dialogBox(ground: PanelGround): Modifier = Modifier.gameRim(gamePixel).solidGround(ground, RectangleShape)

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

    /** Every corner square, as the game's interface has them. */
    override val shapes: Shapes
        get() = SquareShapes
}

private val SquareShapes = RoundedCornerShape(0).let { Shapes(it, it, it, it, it) }

/** The inventory tab's backpack, filling its slot of the dock at whole game pixels, which opens the drawer. */
@Composable
private fun InventoryTab(onClick: () -> Unit, modifier: Modifier) {
    val bag = sprite(R.drawable.osrs_777_0)
    val px = gamePixel
    Spacer(
        modifier
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open the app drawer" }
            .testTag(AppDrawerTags.DOCK_TAB)
            .drawBehind { drawSpriteCentred(bag, center, fitScale(size.minDimension, STONE_FACE.toFloat(), px)) },
    )
}

/** An item fits a square this many game pixels across: its canvas less the clear rows round the mark. */
private const val ITEM_FIT = 28f

@Composable
internal fun sprite(@DrawableRes id: Int): ImageBitmap = ImageBitmap.imageResource(id)

/** [image] at [scale], taking the room it is drawn in. */
@Composable
private fun SpriteImage(image: ImageBitmap, scale: Int, modifier: Modifier = Modifier) {
    with(LocalDensity.current) {
        Spacer(modifier.size((image.width * scale).toDp(), (image.height * scale).toDp()).drawBehind { drawSprite(image, Offset.Zero, scale) })
    }
}

/** The night scheme, whose light text reads on the game's dark stone; by day the panels are its chatbox's parchment. */
private val onStone: Boolean
    @Composable get() = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f

/** The rim of the game's interface boxes: black outside the menu's brown, [px] screen pixels each. */
@Composable
private fun Modifier.gameRim(px: Int): Modifier {
    val rim = with(LocalDensity.current) { px.toDp() }
    return border(rim, GameShadow).padding(rim).border(rim, MenuRim)
}

/** The stone in the side panel's sprite, less the shadow its frame casts along the top and left and the marks at its corners. */
private val STONE = IntRect(left = 10, top = 8, right = 185, bottom = 255)

/**
 * The side panel's [stone] over [size] from [topLeft] at [alpha], each of its pixels [px] screen pixels, in tiles from
 * the drawing's origin so the parts of one panel line up, each mirroring the one before so they meet without a seam.
 */
private fun DrawScope.drawStone(stone: ImageBitmap, px: Int, alpha: Float, topLeft: Offset, size: Size) {
    val tile = IntSize(STONE.width * px, STONE.height * px)
    val bottomRight = topLeft + Offset(size.width, size.height)
    clipRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y) {
        for (row in floor(topLeft.y / tile.height).toInt()..floor(bottomRight.y / tile.height).toInt()) {
            for (column in floor(topLeft.x / tile.width).toInt()..floor(bottomRight.x / tile.width).toInt()) {
                val at = IntOffset(column * tile.width, row * tile.height)
                val centre = Offset(at.x + tile.width / 2f, at.y + tile.height / 2f)
                withTransform({ scale(if (column.mod(2) == 0) 1f else -1f, if (row.mod(2) == 0) 1f else -1f, centre) }) {
                    drawImage(stone, STONE.topLeft, STONE.size, at, tile, alpha = alpha, filterQuality = FilterQuality.None)
                }
            }
        }
    }
}

/** The ripple of a press on an icon, unbounded, so it never clips the item's pixels. */
private fun Modifier.pressed(presses: InteractionSource?) = if (presses == null) this else indication(presses, ripple(bounded = false))

/** The largest whole scale, from one to [most], at which [units] of the game's pixels fit in [room] screen pixels. */
internal fun fitScale(room: Float, units: Float, most: Int) = max(1, min(most, floor(room / units).toInt()))

/** [image] with its top left at [topLeft], each of its pixels [scale] screen pixels square and as sharp as the game's. */
internal fun DrawScope.drawSprite(image: ImageBitmap, topLeft: Offset, scale: Int, tint: Color? = null, alpha: Float = 1f) =
    drawSpritePart(image, IntRect(0, 0, image.width, image.height), topLeft, scale, tint, alpha)

/** [image] as [drawSprite] draws it, centred on [centre]. */
internal fun DrawScope.drawSpriteCentred(image: ImageBitmap, centre: Offset, scale: Int) =
    drawSprite(image, centre - Offset(image.width.toFloat(), image.height.toFloat()) * (scale / 2f), scale)

/** The part [from] of [image], drawn as [drawSprite] draws the whole. */
internal fun DrawScope.drawSpritePart(
    image: ImageBitmap,
    from: IntRect,
    topLeft: Offset,
    scale: Int,
    tint: Color? = null,
    alpha: Float = 1f,
) = drawImage(
    image,
    srcOffset = from.topLeft,
    srcSize = from.size,
    dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
    dstSize = IntSize(from.width * scale, from.height * scale),
    alpha = alpha,
    colorFilter = tint?.let { ColorFilter.tint(it) },
    filterQuality = FilterQuality.None,
)

private operator fun IntOffset.times(scale: Int) = IntOffset(x * scale, y * scale)

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
