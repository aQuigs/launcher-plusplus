package com.sqftware.orbitlauncher.ui

import com.sqftware.orbitlauncher.domain.Colourway
import com.sqftware.orbitlauncher.domain.DOCK_ICON_FILL
import com.sqftware.orbitlauncher.domain.ITEM_HEIGHT
import com.sqftware.orbitlauncher.domain.ITEM_WIDTH
import androidx.compose.ui.unit.toOffset
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import com.sqftware.orbitlauncher.ui.theme.HitsplatCount
import com.sqftware.orbitlauncher.ui.theme.legible
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.vector.ImageVector
import com.sqftware.orbitlauncher.domain.CardColour
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ColorScheme
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
import com.sqftware.orbitlauncher.ui.theme.MenuVerb
import com.sqftware.orbitlauncher.ui.theme.MenuLine
import com.sqftware.orbitlauncher.ui.theme.MenuRoles
import com.sqftware.orbitlauncher.ui.theme.MenuGround
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.IconPlace
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.domain.Unread
import com.sqftware.orbitlauncher.domain.badgeText
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.ui.theme.GameShadow
import com.sqftware.orbitlauncher.ui.theme.gameTypography
import com.sqftware.orbitlauncher.ui.theme.MinimapGrass
import com.sqftware.orbitlauncher.ui.theme.MinimapSelf
import com.sqftware.orbitlauncher.ui.theme.MinimapSelfEdge
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.StackCount
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** How many of the game's pixels wide the screen is: the mobile client's portrait width, which sets how big one is. */
private const val GAME_WIDTH = 270

/** One of the game's pixels in screen pixels, whole so every sprite stays crisp: four on a 1080 pixel wide phone. */
internal val gamePixel: Int
    @Composable get() = max(2, LocalWindowInfo.current.containerSize.width / GAME_WIDTH)

/**
 * The minimap's frame and its compass, as the game draws them: where the frame's map hole is centred, the radius of
 * the map and of the frame's outer edge; where the compass socket is centred in the frame, the radius the compass is
 * drawn to, inside the socket's rim, which covers its edge, and the radius of the socket's outer edge.
 */
private val FRAME_HOLE_CENTRE = IntOffset(100, 84)
private const val MAP_RADIUS = MINIMAP_SIZE / 2
private const val FRAME_RADIUS = 82
private val COMPASS_CENTRE = IntOffset(22, 22)
private const val COMPASS_RADIUS = 20
private const val SOCKET_RADIUS = 23

/**
 * How far past its cell the emblem may reach, as a share of the cell: the frame fills the room inside the ring's apps
 * rather than leaving a wide gap to them.
 */
private const val EMBLEM_REACH = 1.15f

/**
 * The credit the map data's licences ask to show with the map, along the bottom of the disc: how wide it may run and
 * how far below the centre its foot is, in game pixels.
 */
internal const val MAP_CREDIT = "© OpenMapTiles © OpenStreetMap"
private const val CREDIT_WIDTH = 104
private const val CREDIT_DROP = 70
private const val CREDIT_MILLIS = 5_000L
private const val CREDIT_FADE_MILLIS = 1_000

/** The whole game pixels the minimap is drawn at in an emblem of [size]: as large as reaches its room, and no larger than the rest of the game. */
private fun mapScale(size: Size, px: Int) = fitScale(size.minDimension * EMBLEM_REACH, 2f * FRAME_RADIUS, px)

/**
 * Where the compass is centred in an emblem of [size] with its minimap at [scale]: in its socket off the frame's upper
 * left, as the game has it, reaching out among the ring's apps.
 */
private fun compassAt(size: Size, scale: Int) = size.center + ((COMPASS_CENTRE - FRAME_HOLE_CENTRE) * scale).toOffset()

/**
 * The world map's orb, as the game hangs it off the minimap's frame: where it is centred from the map's centre, low on
 * the right where the game has it and inside the emblem's cell, so it takes touches, and its radius.
 */
private val WORLD_MAP_ORB = IntOffset(54, 54)
private const val WORLD_MAP_ORB_RADIUS = 15

private fun worldMapOrbAt(size: Size, scale: Int) = size.center + (WORLD_MAP_ORB * scale).toOffset()

/** The frame's ring, its compass socket and, [withOrb], the world map's orb: all the emblem draws or takes touches in. */
private fun frameOutline(size: Size, scale: Int, withOrb: Boolean): Path {
    val frame = Path.combine(PathOperation.Union, circle(size.center, FRAME_RADIUS * scale.toFloat()), circle(compassAt(size, scale), SOCKET_RADIUS * scale.toFloat()))
    return if (withOrb) Path.combine(PathOperation.Union, frame, circle(worldMapOrbAt(size, scale), WORLD_MAP_ORB_RADIUS * scale.toFloat())) else frame
}

private fun circle(centre: Offset, radius: Float) = Path().apply { addOval(Rect(centre, radius)) }

/** The game's round orb, clipped from its frame: where its circle is centred in the sprite, and its radius. */
private val ORB_CENTRE = IntOffset(40, 17)
private const val ORB_RADIUS = 17

/** The game's row of tab stones: the pitch its stones repeat at, and how wide each of its rounded end stones is. */
private const val STONE_PITCH = 33
private const val END_STONE = 38

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
 * folder shows its previews in an orb and opens round the minimap, badges are hitsplats, and text is in its fonts.
 */
object OldSchoolArt : ThemeArt {
    override val hintInk: Color
        @Composable get() = StackCount

    /** The game never hides its minimap. */
    override val foldersOpenRoundEmblem: Boolean
        get() = true

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
        // OpenStreetMap's attribution rules let the credit fade five seconds after a map first shows; the launcher's
        // menu keeps it a tap away.
        val creditAlpha = remember { Animatable(1f) }
        val mapShown by remember { derivedStateOf { map() != null } }
        LaunchedEffect(mapShown) {
            if (mapShown) {
                creditAlpha.snapTo(1f)
                delay(CREDIT_MILLIS)
                creditAlpha.animateTo(0f, tween(CREDIT_FADE_MILLIS))
            }
        }
        Box(modifier) {
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = -(heading() ?: 0f) }
                    .drawBehind {
                        val scale = mapScale(size, px)
                        map()?.let { drawSpriteCentred(it.ground, center, scale) } ?: drawCircle(MinimapGrass, MAP_RADIUS * scale.toFloat())
                    },
            )
            // Each icon goes round with the map but stays upright, as the game keeps them: placed by hand rather than
            // turned back on a turned canvas, so its pixels land whole on the screen's, in a layer of its own, so a turn
            // draws only the icons again.
            Spacer(
                Modifier.matchParentSize().graphicsLayer().drawBehind {
                    val minimap = map()?.takeIf { it.icons.isNotEmpty() } ?: return@drawBehind
                    val scale = mapScale(size, px)
                    val turn = Math.toRadians(-(heading() ?: 0f).toDouble())
                    val (cos, sin) = cos(turn).toFloat() to sin(turn).toFloat()
                    minimap.icons.forEach { icon ->
                        val dx = (icon.x - MAP_RADIUS) * scale
                        val dy = (icon.y - MAP_RADIUS) * scale
                        drawSpriteCentred(icon.image, center + Offset(dx * cos - dy * sin, dx * sin + dy * cos), scale)
                    }
                },
            )
            // Under the frame, whose socket's own pixels edge it.
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
                        val face = circle(at, COMPASS_RADIUS * scale.toFloat())
                        onDrawBehind { clipPath(face) { drawSpriteCentred(compass, at, scale) } }
                    },
            )
            Spacer(
                Modifier.matchParentSize().drawWithCache {
                    val scale = mapScale(size, px)
                    val outline = frameOutline(size, scale, withOrb = false)
                    val credit = measurer.measure(MAP_CREDIT.replace(" ©", "\n©"), creditStyle, constraints = Constraints(maxWidth = CREDIT_WIDTH * scale))
                    onDrawBehind {
                        // Upright over the turned map, as the game keeps the player's square.
                        if (marked) selfMarker(center, scale)
                        if (map() != null && creditAlpha.value > 0f) {
                            drawText(credit, topLeft = Offset(center.x - credit.size.width / 2f, center.y + CREDIT_DROP * scale - credit.size.height), alpha = creditAlpha.value)
                        }
                        // The frame's ring and socket alone, without the orbs the game hangs off its side.
                        clipPath(outline) { drawSprite(frame, center - (FRAME_HOLE_CENTRE * scale).toOffset(), scale) }
                    }
                },
            )
            // Only once there is a map, as the world map opens round where the minimap last placed the user.
            if (mapShown) WorldMapOrb(px)
        }
    }

    /** The globe the game opens its world map with, over the frame's lower right. */
    @Composable
    private fun BoxScope.WorldMapOrb(px: Int) {
        val orb = sprite(R.drawable.osrs_1438_0)
        val globe = sprite(R.drawable.osrs_1439_0)
        val open = LocalOpenWorldMap.current
        Spacer(
            Modifier
                .matchParentSize()
                .layout { measurable, constraints ->
                    val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                    val scale = mapScale(size, px)
                    val side = 2 * WORLD_MAP_ORB_RADIUS * scale
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    val at = worldMapOrbAt(size, scale)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place((at.x - side / 2f).roundToInt(), (at.y - side / 2f).roundToInt())
                    }
                }
                .drawBehind {
                    val scale = (size.width / (2 * WORLD_MAP_ORB_RADIUS)).roundToInt()
                    drawSpriteCentred(orb, center, scale)
                    drawSpriteCentred(globe, center, scale)
                }
                // Its own tap, taken before the emblem's, which would search or, held, edit the ring.
                .pointerInput(open) { detectTapGestures(onLongPress = {}) { open() } }
                // A node of its own, or it would merge into the emblem's and lose its tap to the emblem's search.
                .semantics(mergeDescendants = true) {
                    contentDescription = "World map"
                    role = Role.Button
                    onClick { open(); true }
                },
        )
    }

    /**
     * The emblem clips to the frame and its compass socket, which reach past its cell, so both show whole, and to the
     * world map's orb while there is a map, so without one the emblem takes no touch beside its frame.
     */
    @Composable
    override fun emblemBox(edge: Color): Modifier {
        val px = gamePixel
        val map = LocalMinimap.current
        val mapShown by remember { derivedStateOf { map() != null } }
        val shape = remember(px, mapShown) { GenericShape { size, _ -> addPath(frameOutline(size, mapScale(size, px), mapShown)) } }
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

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) = Unit

    /**
     * By night the stone of the game's side panel, as the inventory has it, and by day the parchment of its chatbox,
     * which the day's dark text is read on.
     */
    @Composable
    override fun panelVeil(veil: Color): DrawScope.(alpha: Float, topLeft: Offset, size: Size) -> Unit {
        val night = onStone
        val ground = sprite(if (night) R.drawable.osrs_1031_0 else R.drawable.osrs_1017_0)
        val part = if (night) STONE else PARCHMENT
        val px = gamePixel
        return remember(veil, ground, part, px) {
            { alpha, topLeft, size -> drawTiled(ground, part, px, veil.alpha * alpha, topLeft, size) }
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
                            Text(SEARCH_PLACEHOLDER, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    ClearSearchButton(query, onQueryChange) { SpriteImage(clear, px) }
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
        Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(px.asDp())) {
            choices.forEach { choice ->
                Box(
                    Modifier
                        .selectable(choice.selected, role = Role.RadioButton, onClick = choice.onSelect)
                        .then(choice.modifier)
                        .spriteSized(if (choice.selected) sunk else stone, px),
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

    /** On the ring, the game's orb, the icon filling its well and the frame reaching past it; elsewhere nothing. */
    @Composable
    override fun iconGround(): Modifier = if (LocalIconPlace.current == IconPlace.Ring) Modifier.orb() else Modifier

    /** An item keeps its own outline, so neither its pixels nor its ripple are cut to a disc. */
    override val iconShape: Shape?
        get() = null

    /** An item in hard pixels, as the game draws it; an image larger than an item's canvas is no item, such as a shortcut's icon. */
    override fun iconFilter(bitmap: ImageBitmap): FilterQuality =
        if (bitmap.width > ITEM_WIDTH || bitmap.height > ITEM_HEIGHT) super.iconFilter(bitmap) else FilterQuality.None

    /**
     * A hitsplat with the count on it, as the game marks a hit: blue, the game's splat for a hit that did no harm, while
     * all its notifications are in the shade, and red, its splat for damage, once some are dismissed unread. Half the
     * game's size, so the item stays in sight.
     */
    @Composable
    override fun BadgeFace(unread: Unread, modifier: Modifier) {
        val splat = sprite(if (unread.dismissed) R.drawable.osrs_1359_0 else R.drawable.osrs_1358_0)
        Box(modifier, contentAlignment = Alignment.Center) {
            SpriteImage(splat, max(1, gamePixel / 2))
            Text(badgeText(unread.count), color = HitsplatCount, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
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
            if (rest.isNotEmpty()) Text(" $rest", color = StackCount, style = MaterialTheme.typography.titleLarge.legible(StackCount))
        }
    }

    /**
     * The game's row of tab stones under the dock's slots, one stone to a slot and as wide, so they meet as the game's
     * row does: the row's rounded end stones at its ends and its middle stone between, all lit red, as a selected tab is,
     * while the dock is a drop target.
     */
    @Composable
    override fun dockGround(glowColour: Color): DrawScope.(DockSlots) -> Unit {
        val row = sprite(R.drawable.osrs_1174_0)
        val stone = sprite(R.drawable.osrs_1180_0)
        val lit = sprite(R.drawable.osrs_1181_0)
        return { slots ->
            val count = slots.starts.size
            val scale = slots.iconSize / DOCK_ICON_FILL / STONE_PITCH
            val middle = size.height / 2
            val top = middle - stone.height * scale / 2f
            // The end stones first, their rounded ends at the row's ends and the rest under the stones beside them, so a
            // row as wide as the screen keeps them on it.
            slots.starts.indices.sortedBy { it in 1 until count - 1 }.forEach { index ->
                val centre = slots.starts[index] + slots.iconSize / 2
                val half = STONE_PITCH * scale / 2f
                when {
                    count == 1 || index in 1 until count - 1 -> inGamePixels(Offset(centre - stone.width * scale / 2f, top), scale) { drawSprite(stone, Offset.Zero, 1) }
                    index == 0 -> inGamePixels(Offset(centre - half, top), scale) {
                        drawSpritePart(row, IntRect(0, 0, END_STONE, row.height), Offset.Zero, 1)
                    }
                    else -> inGamePixels(Offset(centre + half - END_STONE * scale, top), scale) {
                        drawSpritePart(row, IntRect(row.width - END_STONE, 0, row.width, row.height), Offset.Zero, 1)
                    }
                }
                if (slots.glow > 0f) {
                    inGamePixels(Offset(centre - lit.width * scale / 2f, middle - lit.height * scale / 2f), scale) {
                        drawSprite(lit, Offset.Zero, 1, alpha = slots.glow * 0.7f)
                    }
                }
            }
        }
    }

    /** The home page keeps only its dock, as the game's bottom bar does; a swipe up opens the drawer. */
    override val drawerHandleOnHome: Boolean
        get() = false

    /** The game's right-click menu: a brown box, its edge a game pixel of the same brown outside the black inside it. */
    @Composable
    override fun menuBox(ground: PanelGround): Modifier = Modifier.background(MenuGround)

    override val menuRim: BorderStroke
        @Composable get() = BorderStroke(gamePixel.asDp(), MenuGround)

    /** The "Choose Option" bar tops the box, as the game's does. */
    override val menuPadding: Dp
        get() = 0.dp

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

    /** The game's stone-framed interface box, rimmed in black outside the menu's brown, a game pixel each. */
    @Composable
    override fun dialogBox(ground: PanelGround): Modifier = Modifier.gameRim(gamePixel).then(super.dialogBox(ground))

    /**
     * "Choose Option" in the box's brown on a black bar, over the items in a black line, which are in white with any
     * secondary text in the target's orange. The game's menu is the same by day, so the items keep the night's look.
     */
    @Composable
    override fun MenuBody(content: @Composable () -> Unit) {
        Column(Modifier.padding(gamePixel.asDp())) {
            Text(
                "Choose Option",
                color = MenuGround,
                style = MaterialTheme.typography.labelLarge.copy(shadow = null),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MenuLine)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .semantics { hideFromAccessibility() },
            )
            Column(Modifier.border(gamePixel.asDp(), MenuLine)) {
                GameNight(MenuRoles) { content() }
            }
        }
    }

    @Composable
    override fun typography(day: Boolean): Typography {
        val px = gamePixel
        val density = LocalDensity.current
        return remember(px, density, day) { gameTypography(px, density, shadowed = !day) }
    }

    /** Every corner square, as the game's interface has them. */
    override val shapes: Shapes
        get() = SquareShapes
}

private val SquareShapes = RoundedCornerShape(0).let { Shapes(it, it, it, it, it) }

/** Each sprite decoded once for the whole launcher, not once for every badge, orb and frame that shows it. */
private val sprites = mutableMapOf<Int, ImageBitmap>()

@Composable
internal fun sprite(@DrawableRes id: Int): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember(id) { sprites.getOrPut(id) { ImageBitmap.imageResource(resources, id) } }
}

/** [image] at [scale], taking the room it is drawn in. */
@Composable
private fun SpriteImage(image: ImageBitmap, scale: Int, modifier: Modifier = Modifier) = Spacer(modifier.spriteSized(image, scale))

/** The size of [image] at [scale], with the image drawn behind from the top left. */
@Composable
internal fun Modifier.spriteSized(image: ImageBitmap, scale: Int): Modifier =
    size((image.width * scale).asDp(), (image.height * scale).asDp()).drawBehind { drawSprite(image, Offset.Zero, scale) }

/** The night scheme, whose light text reads on the game's dark stone; by day the panels are its chatbox's parchment. */
private val onStone: Boolean
    @Composable get() = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f

/** These screen pixels in dp. */
@Composable
internal fun Int.asDp() = with(LocalDensity.current) { toDp() }

/** The rim of the game's interface boxes: black outside the menu's brown, [px] screen pixels each. */
@Composable
private fun Modifier.gameRim(px: Int): Modifier {
    val rim = px.asDp()
    return border(rim, GameShadow).padding(rim).border(rim, MenuGround)
}

/** The stone in the side panel's sprite, less the shadow its frame casts along the top and left and the marks at its corners. */
private val STONE = IntRect(left = 10, top = 8, right = 185, bottom = 255)

/** The parchment in the chatbox's sprite, inside its frame and past the shadow it casts along the top and left. */
private val PARCHMENT = IntRect(left = 20, top = 17, right = 513, bottom = 136)

/**
 * The [part] of [image] over [size] from [topLeft] at [alpha], each of its pixels [px] screen pixels, in tiles from
 * the drawing's origin so the parts of one panel line up, each mirroring the one before so they meet without a seam
 * unless the image tiles by itself and not [mirror].
 */
internal fun DrawScope.drawTiled(
    image: ImageBitmap,
    part: IntRect = IntRect(0, 0, image.width, image.height),
    px: Int,
    alpha: Float = 1f,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
    mirror: Boolean = true,
) {
    val tile = IntSize(part.width * px, part.height * px)
    val bottomRight = topLeft + Offset(size.width, size.height)
    clipRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y) {
        for (row in floor(topLeft.y / tile.height).toInt()..floor(bottomRight.y / tile.height).toInt()) {
            for (column in floor(topLeft.x / tile.width).toInt()..floor(bottomRight.x / tile.width).toInt()) {
                val at = IntOffset(column * tile.width, row * tile.height)
                val centre = Offset(at.x + tile.width / 2f, at.y + tile.height / 2f)
                withTransform({ scale(if (!mirror || column.mod(2) == 0) 1f else -1f, if (!mirror || row.mod(2) == 0) 1f else -1f, centre) }) {
                    drawSpritePart(image, part, at.toOffset(), px, alpha = alpha)
                }
            }
        }
    }
}

/**
 * [content] in Old School's night look whatever the wallpaper, as the game's menu and bank are dark by day too, with
 * its [roles] changed.
 */
@Composable
internal fun GameNight(roles: ColorScheme.() -> ColorScheme = { this }, content: @Composable () -> Unit) =
    Themed(Theme.OldSchool, Colourway.Classic, lightWallpaper = false) {
        val night = MaterialTheme.colorScheme
        MaterialTheme(colorScheme = remember(night, roles) { night.roles() }, content = content)
    }

/** The largest whole scale, from one to [most], at which [units] of the game's pixels fit in [room] screen pixels. */
internal fun fitScale(room: Float, units: Float, most: Int) = max(1, min(most, floor(room / units).toInt()))

/** [image] with its top left at [topLeft], each of its pixels [scale] screen pixels square and as sharp as the game's. */
internal fun DrawScope.drawSprite(image: ImageBitmap, topLeft: Offset, scale: Int, tint: Color? = null, alpha: Float = 1f) =
    drawSpritePart(image, IntRect(0, 0, image.width, image.height), topLeft, scale, tint, alpha)

/** [image] as [drawSprite] draws it, centred on [centre]. */
internal fun DrawScope.drawSpriteCentred(image: ImageBitmap, centre: Offset, scale: Int, tint: Color? = null, alpha: Float = 1f) =
    drawSprite(image, centre - Offset(image.width.toFloat(), image.height.toFloat()) * (scale / 2f), scale, tint, alpha)

/** The part [from] of [image], drawn as [drawSprite] draws the whole, or stretched to [dstSize]. */
internal fun DrawScope.drawSpritePart(
    image: ImageBitmap,
    from: IntRect,
    topLeft: Offset,
    scale: Int,
    tint: Color? = null,
    alpha: Float = 1f,
    dstSize: IntSize = IntSize(from.width * scale, from.height * scale),
) = drawImage(
    image,
    srcOffset = from.topLeft,
    srcSize = from.size,
    dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
    dstSize = dstSize,
    alpha = alpha,
    colorFilter = tint?.let { ColorFilter.tint(it) },
    filterQuality = FilterQuality.None,
)

/**
 * The game's round orb behind what is drawn here, filled with the brown of its right-click menu rather than the orb's
 * own black, which hid dark items.
 */
@Composable
private fun Modifier.orb(): Modifier {
    val frame = sprite(R.drawable.osrs_1071_0)
    val fill = sprite(R.drawable.osrs_1059_0)
    return drawWithCache {
        // The well, the fill inside the frame's rim, fills the item as an icon's disc does, so an item in it is the size
        // every icon is, and the rim reaches past.
        val scale = size.minDimension / fill.width
        val orb = circle(size.center, ORB_RADIUS * scale)
        onDrawBehind {
            clipPath(orb) { inGamePixels(center - ORB_CENTRE.toOffset() * scale, scale) { drawSprite(frame, Offset.Zero, 1) } }
            inGamePixels(center - Offset(fill.width.toFloat(), fill.height.toFloat()) * (scale / 2), scale) {
                drawSprite(fill, Offset.Zero, 1, tint = MenuGround)
            }
        }
    }
}

/**
 * Draws [draw] in the game's own pixels from [origin], each [scale] screen pixels square, however many that makes, for
 * art sized by the shared layout rather than in whole game pixels.
 */
private inline fun DrawScope.inGamePixels(origin: Offset, scale: Float, draw: DrawScope.() -> Unit) =
    withTransform({ translate(origin.x, origin.y); scale(scale, scale, Offset.Zero) }, draw)

/** The player's own square in the minimap's middle, three game pixels wide in a ring of black so it shows on a white wall. */
private fun DrawScope.selfMarker(centre: Offset, scale: Int) {
    val edge = Size(5f * scale, 5f * scale)
    val self = Size(3f * scale, 3f * scale)
    drawRect(MinimapSelfEdge, centre - Offset(edge.width, edge.height) / 2f, edge)
    drawRect(MinimapSelf, centre - Offset(self.width, self.height) / 2f, self)
}

/** [text]'s digits and colons in the quill [digits], each pixel [px] screen pixels, with the game's drop shadow under them. */
@Composable
private fun QuillLine(text: String, digits: List<ImageBitmap>, px: Int) {
    val inks = remember(digits) { digits.map(::inkColumns) }
    val glyphs = text.mapNotNull { c -> (c - '0').takeIf { it in digits.indices } }
    val width = glyphs.sumOf { inks[it].last - inks[it].first + QUILL_GAP } * px
    val height = (digits.first().height + 1) * px
    val shadow = px / CLOCK_SCALE
    Spacer(
        Modifier.size(width.asDp(), height.asDp()).drawBehind {
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

/** The pixels between two quill glyphs, as the game sets them. */
private const val QUILL_GAP = 2

/** The columns a glyph's cell has ink in, from its first to its last. */
private fun inkColumns(glyph: ImageBitmap): IntRange {
    val argb = IntArray(glyph.width * glyph.height)
    glyph.readPixels(argb)
    val inked = (0 until glyph.width).filter { x -> (0 until glyph.height).any { y -> argb[y * glyph.width + x] ushr 24 != 0 } }
    return if (inked.isEmpty()) 0..2 else inked.first()..inked.last()
}

/** Each whole game pixel of a sprite offset as [scale] screen pixels. */
private operator fun IntOffset.times(scale: Int) = IntOffset(x * scale, y * scale)
