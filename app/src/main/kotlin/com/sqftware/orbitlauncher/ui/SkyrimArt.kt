package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.METRES_PER_PIXEL
import com.sqftware.orbitlauncher.domain.minimap.WebMercator
import com.sqftware.orbitlauncher.ui.theme.QuestGold
import com.sqftware.orbitlauncher.ui.theme.QuestGoldEdge
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.turnBetween
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.ui.theme.CompassLetter
import com.sqftware.orbitlauncher.ui.theme.CompassLine
import com.sqftware.orbitlauncher.ui.theme.CompassShade
import com.sqftware.orbitlauncher.ui.theme.CompassSmoke
import com.sqftware.orbitlauncher.ui.theme.DiscoverySmoke
import com.sqftware.orbitlauncher.ui.theme.LocalMapFog
import com.sqftware.orbitlauncher.ui.theme.LocalMapInk
import com.sqftware.orbitlauncher.ui.theme.LocalMapRim
import com.sqftware.orbitlauncher.ui.theme.LocalMapVignette
import com.sqftware.orbitlauncher.ui.theme.PlayerArrow
import com.sqftware.orbitlauncher.ui.theme.PlayerArrowEdge
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.ShieldBoardDark
import com.sqftware.orbitlauncher.ui.theme.ShieldBoardLight
import com.sqftware.orbitlauncher.ui.theme.ShieldIron
import com.sqftware.orbitlauncher.ui.theme.ShieldRivet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** The map's icons, as a share of the map disc's diameter, and the player's arrow, as a share of its radius. */
private const val MAP_ICON_SHARE = 0.09f
private const val ARROW_SHARE = 0.13f

/** Where the local map darkens into its edge, as a share of its radius. */
private const val VIGNETTE_FROM = 0.6f

/** How many degrees of the horizon the compass shows, from end to end: a little past the quarters either side, as the game's does. */
private const val COMPASS_FIELD = 200f

/**
 * How tall the compass is, its bar as a share of that, how wide its marks are drawn, how much of the page's width it
 * spans, and how wide each carved end is, as a share of the bar's height.
 */
private val COMPASS_HEIGHT = 52.dp
private const val COMPASS_BAND = 0.5f
private val COMPASS_MARK = 18.dp
private const val COMPASS_SPAN = 0.9f
private const val COMPASS_END = 1.2f

/** The quest marker, as a share of the compass bar's height, and on the map as a share of its radius. */
private const val QUEST_ON_COMPASS = 0.45f
private const val QUEST_ON_MAP = 0.11f

/** How far out the map marks a quest beyond it, at its edge, as a share of its radius. */
private const val QUEST_REACH = 0.8f

/** How long a place's name takes to show when it is discovered, stays, and fades. */
private const val DISCOVERY_IN_MILLIS = 800
private const val DISCOVERY_HOLD_MILLIS = 3_500L
private const val DISCOVERY_OUT_MILLIS = 1_200

/** A shield's previews take its boss, its boards show around them, and so many rivets hold its rim. */
private const val SHIELD_DISC = 0.64f
private const val SHIELD_BOARDS = 5
private const val SHIELD_RIVETS = 10

/** Skyrim's lettering: a plain condensed sans, spaced out, as the game sets its menus and its compass. */
private val Condensed = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Light),
    Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Normal),
    Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Medium),
    Font(DeviceFontFamilyName("sans-serif-condensed"), FontWeight.Bold),
)

/**
 * Skyrim: an homage to the northern game's interface, drawn by hand rather than from its files. The emblem is its local
 * map of where the user is, turned with the phone, with the player's arrow at its middle; the compass runs along the top
 * of the home page between carved ends, with the map's places and the user's quest marked on it, and names each place the
 * user discovers below it; folders are round Nord shields; menus are smoke between bone hairlines, in a condensed type.
 */
object SkyrimArt : ThemeArt {
    override val hintInk: Color
        @Composable get() = LocalMapInk

    /**
     * The local map of where the user is, else of a made-up place, or fog until there is one, turned with the phone, its
     * icons kept upright, darkening into a rimmed edge, with the player's arrow at the middle. What turns is a layer of
     * its own, so a turn moves it without drawing the rest again.
     */
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val map = LocalMinimap.current
        val madeUp = LocalMadeUpMap.current
        val shown = { map() ?: madeUp() }
        val heading = LocalHeading.current
        val quest = LocalJournal.current.journal.quest
        val mapShown by remember { derivedStateOf { map() != null } }
        val creditAlpha = mapCreditAlpha(mapShown)
        val measurer = rememberTextMeasurer()
        val creditStyle = MaterialTheme.typography.labelSmall.copy(color = LocalMapInk, textAlign = TextAlign.Center)
        Box(modifier) {
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = -(heading() ?: 0f) }
                    .drawBehind {
                        val radius = size.emblemRadius
                        shown()?.let { drawIcon(it.ground, center, (2 * radius).roundToInt()) } ?: drawCircle(LocalMapFog, radius)
                    },
            )
            Spacer(
                Modifier.matchParentSize().graphicsLayer().drawBehind {
                    val painted = shown() ?: return@drawBehind
                    val radius = size.emblemRadius
                    val iconSide = (2 * radius * MAP_ICON_SHARE).roundToInt()
                    forEachUprightIcon(painted, center, 2 * radius / MINIMAP_SIZE, heading() ?: 0f) { image, at -> drawIcon(image, at, iconSide) }
                    if (map() != null) quest?.let { drawQuestOnMap(painted, it, radius, heading() ?: 0f) }
                },
            )
            Spacer(
                Modifier.matchParentSize().drawWithCache {
                    val radius = size.emblemRadius
                    val vignette = Brush.radialGradient(
                        VIGNETTE_FROM to Color.Transparent,
                        1f to LocalMapVignette,
                        center = size.center,
                        radius = radius,
                    )
                    val arrow = playerArrow(size.center, radius * ARROW_SHARE)
                    val arrowEdge = Stroke(1.5.dp.toPx())
                    val outer = Stroke(3.dp.toPx())
                    val inner = Stroke(1.dp.toPx())
                    val credit = measurer.measure(
                        MAP_CREDIT.replace(" ©", "\n©"),
                        creditStyle,
                        // Narrow enough that its lines fit inside the disc where they sit.
                        constraints = Constraints(maxWidth = (radius * 1.2f).roundToInt()),
                    )
                    onDrawBehind {
                        drawCircle(vignette, radius)
                        if (marked) {
                            drawPath(arrow, PlayerArrow)
                            drawPath(arrow, PlayerArrowEdge, style = arrowEdge)
                        }
                        if (map() != null && creditAlpha.value > 0f) {
                            drawText(credit, topLeft = Offset(center.x - credit.size.width / 2f, center.y + radius * 0.8f - credit.size.height), alpha = creditAlpha.value)
                        }
                        drawCircle(LocalMapRim.outer, radius - outer.width / 2, style = outer)
                        drawCircle(LocalMapRim.inner, radius - outer.width, style = inner)
                    }
                },
            )
            // The studs on the rim mark north and the quarters, turned with the map.
            Spacer(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationZ = -(heading() ?: 0f) }
                    .drawWithCache {
                        val radius = size.emblemRadius - 2.dp.toPx()
                        val stud = 4.dp.toPx()
                        val studs = Path().apply {
                            for (quarter in 0 until 4) {
                                val half = if (quarter == 0) stud * 1.6f else stud
                                val out = direction(quarter * 90f)
                                addPath(diamond(size.center + out * radius, half, half * 0.6f, atan2(out.y, out.x)))
                            }
                        }
                        onDrawBehind { drawPath(studs, LocalMapRim.inner) }
                    },
            )
        }
    }

    /** The map fills the emblem's circle and draws its own rim. */
    @Composable
    override fun emblemBox(edge: Color): Modifier = Modifier.clip(CircleShape)

    /** A hairline track, with a small diamond midway between each pair of slots, as the game studs its lines. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val track = Stroke(1.dp.toPx())
        val half = 3.dp.toPx()
        val studs = Path().apply {
            if (slots.size < 2) return@apply
            slots.indices.forEach { i ->
                val a = slots[i] - centre
                val b = slots[(i + 1) % slots.size] - centre
                // Half the turn from one slot to the next; two slots face each other, so each gap gets half a turn the same way.
                val turn = if (slots.size == 2) PI.toFloat() else turnBetween(a.x, a.y, b.x, b.y)
                val angle = atan2(a.y, a.x) + turn / 2
                val at = centre + Offset(cos(angle) * radius, sin(angle) * radius)
                addPath(diamond(at, half, half * 0.6f, angle + PI.toFloat() / 2))
            }
        }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2)) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                val ink = colours.mark.copy(alpha = (0.35f + 0.45f * glow) * alpha)
                clipPath(discs, ClipOp.Difference) {
                    drawCircle(ink, radius, centre, style = track)
                }
                drawPath(studs, ink)
            }
        }
    }

    @Composable
    override fun folderWear(folder: RingItem.Folder): FolderWear = FolderWear(SHIELD_DISC, Modifier.shield())

    /** Skyrim's panels are plain smoke. */
    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) = Unit

    /** The compass, and over what lies below it, the name of each place the user discovers. */
    @Composable
    override fun HomeHeader(modifier: Modifier) {
        Column(modifier.zIndex(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Compass()
            Discovery()
        }
    }

    @Composable
    override fun menuBox(ground: PanelGround): Modifier = hairlinedBox(ground)

    override val menuRim: BorderStroke?
        @Composable get() = null

    @Composable
    override fun dialogBox(ground: PanelGround): Modifier = hairlinedBox(ground)

    @Composable
    override fun typography(day: Boolean): Typography = SkyrimTypography

    override val shapes: Shapes
        get() = SquareShapes
}

private val SkyrimTypography = Typography().let { type ->
    fun TextStyle.condensed(spacing: Float = 0.06f) = copy(fontFamily = Condensed, letterSpacing = spacing.em)
    type.copy(
        displayLarge = type.displayLarge.condensed(0.02f),
        displayMedium = type.displayMedium.condensed(0.02f),
        displaySmall = type.displaySmall.condensed(0.02f),
        headlineLarge = type.headlineLarge.condensed(),
        headlineMedium = type.headlineMedium.condensed(),
        headlineSmall = type.headlineSmall.condensed(),
        titleLarge = type.titleLarge.condensed(),
        titleMedium = type.titleMedium.condensed(),
        titleSmall = type.titleSmall.condensed(),
        bodyLarge = type.bodyLarge.condensed(0.02f),
        bodyMedium = type.bodyMedium.condensed(0.02f),
        bodySmall = type.bodySmall.condensed(0.02f),
        labelLarge = type.labelLarge.condensed(0.1f),
        labelMedium = type.labelMedium.condensed(0.1f),
        labelSmall = type.labelSmall.condensed(0.1f),
    )
}

/**
 * The game's compass: a bar of smoke between bone hairlines and carved ends, the quarters lettered on it and the map's
 * places marked at their bearings, with the user's quest in gold, sliding along it as the phone turns. It stays north up
 * without a heading: with no compass in the phone, or while "Show where I am" is off, as headings are read only for the
 * map of where the user is.
 */
@Composable
private fun Compass(modifier: Modifier = Modifier) {
    val map = LocalMinimap.current
    val madeUp = LocalMadeUpMap.current
    val heading = LocalHeading.current
    val quest = LocalJournal.current.journal.quest
    val measurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.typography.titleMedium.copy(color = CompassLetter, fontWeight = FontWeight.Medium)
    Spacer(
        modifier
            .fillMaxWidth(COMPASS_SPAN)
            .height(COMPASS_HEIGHT)
            // Only decoration: the places it marks have no action, and the map's credit is in the launcher's menu.
            .clearAndSetSemantics {}
            .drawWithCache {
                val letters = listOf("N", "E", "S", "W").map { measurer.measure(it, letterStyle) }
                val northLetter = measurer.measure("N", letterStyle.copy(fontWeight = FontWeight.Bold))
                val mid = size.height / 2
                val band = size.height * COMPASS_BAND
                val top = mid - band / 2
                val bottom = mid + band / 2
                val endWidth = band * COMPASS_END
                // The marks run between the carved ends, fading as they reach them.
                val reach = size.width / 2 - endWidth
                val line = Stroke(1.dp.toPx())
                val markSide = COMPASS_MARK.roundToPx()
                val tick = 3.dp.toPx()
                val end = compassEnd(endWidth, mid, band)
                val pointer = diamond(Offset(size.width / 2, top), tick * 1.4f, tick)
                // A map's places' bearings, and the quest's, worked out once for each map rather than on each turn of the phone.
                var bearingsOf: PaintedMap<ImageBitmap>? = null
                var bearings = FloatArray(0)
                var questBearing: Float? = null

                onDrawBehind {
                    val barStart = endWidth * 0.9f
                    drawRect(CompassSmoke, topLeft = Offset(barStart, top), size = Size(size.width - 2 * barStart, band))
                    drawLine(CompassLine, Offset(barStart, top), Offset(size.width - barStart, top), line.width)
                    drawLine(CompassLine, Offset(barStart, bottom), Offset(size.width - barStart, bottom), line.width)
                    for (side in listOf(1f, -1f)) {
                        scale(side, 1f) {
                            drawPath(end, CompassShade)
                            drawPath(end, CompassLine, style = line)
                        }
                    }
                    drawPath(pointer, CompassLine)

                    val facing = heading() ?: 0f
                    // How far along the bar from its middle a bearing lies, from -1 to 1, or past that off the bar.
                    fun share(bearing: Float) = turnBetween(facing, bearing) / (COMPASS_FIELD / 2)
                    fun x(share: Float) = size.width / 2 + share * reach
                    fun fading(share: Float) = 1f - abs(share).pow(4)

                    for (step in 0 until 8) {
                        val share = share(step * 45f)
                        if (abs(share) > 1f) continue
                        if (step % 2 == 0) {
                            val text = if (step == 0) northLetter else letters[step / 2]
                            drawText(text, topLeft = Offset(x(share) - text.size.width / 2f, mid - text.size.height / 2f), alpha = fading(share))
                        } else {
                            drawLine(CompassLine.copy(alpha = CompassLine.alpha * fading(share)), Offset(x(share), mid - tick), Offset(x(share), mid + tick), line.width)
                        }
                    }

                    val painted = map() ?: madeUp() ?: return@onDrawBehind
                    if (painted !== bearingsOf) {
                        bearingsOf = painted
                        bearings = bearingsOf(painted)
                        // Only a map of where the user is knows where they stand.
                        questBearing = quest?.takeIf { painted === map() }?.let { WebMercator.bearing(painted.centre, it).toFloat() }
                    }
                    painted.icons.forEachIndexed { i, icon ->
                        val share = share(bearings[i])
                        if (abs(share) > 1f) return@forEachIndexed
                        val at = Offset(x(share), mid)
                        drawCircle(CompassShade.copy(alpha = CompassShade.alpha * fading(share) * 0.5f), markSide * 0.55f, at)
                        drawIcon(icon.image, at, markSide, fading(share))
                    }
                    questBearing?.let { bearing ->
                        val share = share(bearing)
                        if (abs(share) <= 1f) drawQuestMarker(Offset(x(share), mid), band * QUEST_ON_COMPASS, fading(share))
                    }
                }
            },
    )
}

/**
 * The left of the compass's carved ends, [width] wide, round a bar [band] high at [mid]: a pointed jewel the bar ends in,
 * held by a claw curling out above and below it, as the game's compass is. The right end is its mirror image.
 */
private fun compassEnd(width: Float, mid: Float, band: Float): Path {
    val half = band / 2
    return Path().apply {
        moveTo(width * 0.02f, mid)
        lineTo(width * 0.55f, mid - half * 0.8f)
        lineTo(width, mid)
        lineTo(width * 0.55f, mid + half * 0.8f)
        close()
        for (side in listOf(-1f, 1f)) {
            val edge = mid + side * half
            moveTo(width * 1.1f, edge)
            cubicTo(width * 1.0f, edge + side * half * 0.9f, width * 0.55f, edge + side * half * 1.2f, width * 0.3f, edge + side * half * 0.9f)
            cubicTo(width * 0.6f, edge + side * half * 0.6f, width * 0.75f, edge + side * half * 0.2f, width * 0.78f, edge)
            close()
        }
    }
}

/**
 * The name of each place the user comes to that the journal has not got, written large under the compass as the game
 * names a place discovered, then fading; the journal keeps it, so the name shows only the first time.
 */
@Composable
private fun Discovery() {
    val map = LocalMinimap.current
    val kept by rememberUpdatedState(LocalJournal.current)
    val place by remember { derivedStateOf { map()?.place } }
    var shown by remember { mutableStateOf<String?>(null) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(place) {
        val journal = kept.journal.discovering(place) ?: return@LaunchedEffect
        kept.onChange(journal)
        shown = place
        try {
            alpha.snapTo(0f)
            alpha.animateTo(1f, tween(DISCOVERY_IN_MILLIS))
            delay(DISCOVERY_HOLD_MILLIS)
            alpha.animateTo(0f, tween(DISCOVERY_OUT_MILLIS))
        } finally {
            shown = null
        }
    }
    val name = shown ?: return
    val type = MaterialTheme.typography
    Column(
        Modifier
            .fillMaxWidth(COMPASS_SPAN)
            // Over what lies below, rather than pushing it down for the moment the name shows.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                layout(placeable.width, 0) { placeable.place(0, 0) }
            }
            .graphicsLayer { this.alpha = alpha.value }
            .drawBehind {
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.2f to DiscoverySmoke, 0.8f to DiscoverySmoke, 1f to Color.Transparent))
            }
            .padding(vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            name.uppercase(),
            style = type.headlineMedium.copy(color = CompassLetter, letterSpacing = 0.12.em),
            textAlign = TextAlign.Center,
        )
        Spacer(
            Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(0.6f)
                .height(7.dp)
                .drawWithCache {
                    val hairline = Brush.horizontalGradient(0f to Color.Transparent, 0.2f to CompassLine, 0.8f to CompassLine, 1f to Color.Transparent)
                    val jewel = diamond(size.center, size.height * 0.9f, size.height / 2, 0f)
                    onDrawBehind {
                        drawLine(hairline, Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx())
                        drawPath(jewel, CompassLine)
                    }
                },
        )
        Text("DISCOVERED", style = type.titleSmall.copy(color = CompassLetter, letterSpacing = 0.3.em))
    }
}

/** The quest marker at [quest] on the map [painted], [radius] round, turned with the phone's [heading]; at its edge if beyond it. */
private fun DrawScope.drawQuestOnMap(painted: PaintedMap<ImageBitmap>, quest: LatLon, radius: Float, heading: Float) {
    val offset = WebMercator.groundOffset(painted.centre, quest)
    val scale = 2 * radius / MINIMAP_SIZE / METRES_PER_PIXEL.toFloat()
    val turn = Math.toRadians(-heading.toDouble())
    val cos = cos(turn).toFloat()
    val sin = sin(turn).toFloat()
    val dx = offset.east.toFloat() * scale
    val dy = -offset.north.toFloat() * scale
    var at = Offset(dx * cos - dy * sin, dx * sin + dy * cos)
    val far = radius * QUEST_REACH
    if (at.getDistance() > far) at *= far / at.getDistance()
    val half = radius * QUEST_ON_MAP
    // Its tip on the spot, as a pin's.
    drawQuestMarker(center + at - Offset(0f, half), half)
}

/** The game's gold quest marker, [half] from its middle at [center] to its tip: the player's arrow, pointing down. */
private fun DrawScope.drawQuestMarker(center: Offset, half: Float, alpha: Float = 1f) {
    val marker = playerArrow(center, -half)
    drawPath(marker, QuestGold, alpha = alpha)
    drawPath(marker, QuestGoldEdge, alpha = alpha, style = Stroke(half * 0.14f))
}

/** The bearing of each of [painted]'s places from its middle, which the map is centered on, in degrees clockwise from north. */
private fun bearingsOf(painted: PaintedMap<ImageBitmap>): FloatArray {
    val middle = painted.ground.width / 2
    return FloatArray(painted.icons.size) { i ->
        val icon = painted.icons[i]
        Math.toDegrees(atan2((icon.x - middle).toDouble(), (middle - icon.y).toDouble())).toFloat()
    }
}

/** [image] drawn [side] pixels square around [center], scaled smoothly. */
private fun DrawScope.drawIcon(image: ImageBitmap, center: Offset, side: Int, alpha: Float = 1f) = drawImage(
    image,
    dstOffset = IntOffset((center.x - side / 2f).roundToInt(), (center.y - side / 2f).roundToInt()),
    dstSize = IntSize(side, side),
    alpha = alpha,
    filterQuality = FilterQuality.Medium,
)

/** The player's arrow on the local map, pointing up the way they face, [half] from its middle to its tip, notched at its tail. */
private fun playerArrow(center: Offset, half: Float) = Path().apply {
    moveTo(center.x, center.y - half)
    lineTo(center.x + half * 0.7f, center.y + half * 0.8f)
    lineTo(center.x, center.y + half * 0.35f)
    lineTo(center.x - half * 0.7f, center.y + half * 0.8f)
    close()
}

/** A diamond around [center], [long] from it to each point along [angle] and [short] across it. */
private fun diamond(center: Offset, long: Float, short: Float, angle: Float = -PI.toFloat() / 2): Path {
    val along = Offset(cos(angle), sin(angle))
    val across = Offset(-along.y, along.x)
    return Path().apply {
        (center + along * long).let { moveTo(it.x, it.y) }
        (center + across * short).let { lineTo(it.x, it.y) }
        (center - along * long).let { lineTo(it.x, it.y) }
        (center - across * short).let { lineTo(it.x, it.y) }
        close()
    }
}

/**
 * A menu's or dialog's box, as the game's are: the solid [ground] squared off, between hairlines along its top and bottom
 * edges, each with a diamond at its middle.
 */
@Composable
private fun hairlinedBox(ground: PanelGround): Modifier {
    val ink = MaterialTheme.colorScheme.outline
    return Modifier.solidGround(ground, RectangleShape).drawWithCache {
        val width = 1.dp.toPx()
        val half = 3.dp.toPx()
        val edges = listOf(width / 2, size.height - width / 2)
        val diamonds = Path().apply { edges.forEach { y -> addPath(diamond(Offset(size.width / 2, y), half * 1.6f, half, 0f)) } }
        onDrawWithContent {
            drawContent()
            edges.forEach { y -> drawLine(ink, Offset(0f, y), Offset(size.width, y), width) }
            drawPath(diamonds, ink)
        }
    }
}

/** A Nord's round shield: boards of two woods inside an iron rim set with rivets. */
private fun Modifier.shield() = drawWithCache {
    val radius = size.minDimension / 2
    val face = Path().apply { addOval(Rect(size.center, radius)) }
    val rim = Stroke(radius * 0.12f)
    val board = size.width / SHIELD_BOARDS
    onDrawBehind {
        clipPath(face) {
            drawCircle(ShieldBoardDark, radius)
            for (i in 0 until SHIELD_BOARDS step 2) {
                drawRect(ShieldBoardLight, topLeft = Offset(i * board, 0f), size = Size(board, size.height))
            }
        }
        drawCircle(ShieldIron, radius - rim.width / 2, style = rim)
        for (i in 0 until SHIELD_RIVETS) {
            drawCircle(ShieldRivet, rim.width * 0.22f, center + direction(360f * i / SHIELD_RIVETS) * (radius - rim.width / 2))
        }
    }
}
