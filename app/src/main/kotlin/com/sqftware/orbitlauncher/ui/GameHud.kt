package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sqftware.orbitlauncher.domain.minimap.LatLon
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.domain.minimap.WebMercator
import com.sqftware.orbitlauncher.domain.minimap.bearingTo
import com.sqftware.orbitlauncher.domain.minimap.shiftTo
import com.sqftware.orbitlauncher.domain.turnBetween
import com.sqftware.orbitlauncher.ui.theme.HudColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** The map's icons, as a share of the map disc's diameter, and the player's arrow, as a share of its radius. */
private const val MAP_ICON_SHARE = 0.09f
private const val ARROW_SHARE = 0.13f

/** Where the local map darkens into its edge, as a share of its radius. */
private const val VIGNETTE_FROM = 0.6f

/** How many degrees of the horizon the compass shows, from end to end: a little past the quarters either side, as the games' do. */
private const val COMPASS_FIELD = 200f

/** How tall the compass is, its bar as a share of that, how wide its marks are drawn, and how much of the page's width it spans. */
private val COMPASS_HEIGHT = 52.dp
private const val COMPASS_BAND = 0.5f
private val COMPASS_MARK = 18.dp
internal const val COMPASS_SPAN = 0.9f

/** The quest marker, as a share of the compass bar's height, and on the map as a share of its radius. */
private const val QUEST_ON_COMPASS = 0.45f
private const val QUEST_ON_MAP = 0.11f

/** How far out the map marks a quest beyond it, at its edge, as a share of its radius, so the whole marker shows. */
private const val QUEST_REACH = 0.62f

/** Closer than this, in meters, the user stands at their quest, which the compass then has no way to point. */
private const val QUEST_HERE_M = 15.0

/** How long a place's name takes to show when it is discovered, and to fade. */
private const val DISCOVERY_IN_MILLIS = 800
private const val DISCOVERY_OUT_MILLIS = 1_200

/**
 * How a theme draws a game's view of where the user is, its heads-up display: the local map that is its emblem, the
 * compass along the top of the home page with the user's quest marked on it, the banner naming a place discovered, and
 * the world map. The games lay these out alike, so this file lays them out once; a look gives their colors and the few
 * shapes its game draws its own way.
 */
internal interface HudLook {
    val colors: HudColors

    /** What the local map shows through in the emblem's cell: by default the cell's disc. */
    val window: MapWindow get() = MapWindow.Disc

    /** How many degrees apart the compass's ticks are: the quarters are lettered, every 45° tick is long, the rest short. */
    val compassTick: Float

    /**
     * How far below the compass bar's middle its letters, ticks and places sit, as a share of its height: by default on
     * it, as the northern games set them; a game that runs its bar as a line hangs them under it.
     */
    val compassDrop: Float get() = 0f

    /** The quest marker's outline round its middle, [half] from it to its tip, which points down at the spot. */
    fun questShape(half: Float): Path

    /** Builds the marks on the local map's rim, on a circle of [radius] round [center], north first: turned with the map. By default none. */
    fun CacheDrawScope.rimMarks(center: Offset, radius: Float): DrawScope.() -> Unit = {}

    /** Builds what lies over the local map and the world map, above the ground and its icons: by default nothing. */
    fun CacheDrawScope.screenOver(): DrawScope.() -> Unit = {}

    /** Builds the compass's bar, [band] high round [mid] and as wide as the compass, with its ends and its pointer. */
    fun CacheDrawScope.compassBar(mid: Float, band: Float): CompassBar

    /** What the banner says of the place the user has just discovered, [name]d, set out down its middle. */
    @Composable
    fun DiscoveryCard(name: String)
}

/**
 * A theme drawn as a game's heads-up display in [hud], with [drift] for its weather over the pages and the world map,
 * if it has any: its emblem the local map, its header the compass, and its corners square, as the games' are.
 */
abstract class HudArt internal constructor(private val hud: HudLook, private val drift: Drift?) : ThemeArt {
    override val worldMap: WorldMapArt = HudWorldMap(hud, drift)

    override val hintInk: Color
        @Composable get() = hud.colors.ink

    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) =
        LocalMapEmblem(hud, marked, modifier)

    /** The map fills the emblem's window and draws its own rim, its marks reaching out to the cell's edge. */
    @Composable
    override fun emblemBox(edge: Color): Modifier = Modifier.clip(hud.window.cell)

    @Composable
    override fun HomeHeader(discovery: String?, modifier: Modifier) = HudHeader(hud, discovery, modifier)

    @Composable
    override fun Weather(turn: () -> Float, modifier: Modifier) {
        drift?.let { DriftingWeather(it, turn, modifier) }
    }

    override val shapes: Shapes
        get() = SquareShapes
}

/**
 * What the local map shows through in the emblem's cell, as wide as the cell's disc: [aspect] times as wide as it is
 * tall, its corners rounded by a [corner] share of its height. The map under it is a disc reaching its farthest corner,
 * so it covers the window however the map turns.
 */
internal class MapWindow(private val aspect: Float, private val corner: Float) : Shape {
    /** The window in a cell of [size], drawn [inset] inside its edge. */
    fun outline(size: Size, inset: Float = 0f): RoundRect {
        val bounds = bounds(size)
        return RoundRect(bounds.deflate(inset), CornerRadius((round(bounds) - inset).coerceAtLeast(0f)))
    }

    fun bounds(size: Size): Rect {
        val halfWide = size.emblemRadius
        val halfTall = halfWide / aspect
        return Rect(size.center.x - halfWide, size.center.y - halfTall, size.center.x + halfWide, size.center.y + halfTall)
    }

    /** How far from the middle the window's farthest point lies: the radius of the map disc that covers it. */
    fun reach(size: Size): Float {
        val bounds = bounds(size)
        val round = round(bounds)
        return hypot(bounds.width / 2 - round, bounds.height / 2 - round) + round
    }

    private fun round(bounds: Rect) = bounds.height * corner

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Rounded(outline(size))

    /** The window grown out to the cell's edge, room for what a look draws on its rim: for the disc, the cell's own circle. */
    val cell = GenericShape { size, _ -> addRoundRect(outline(size, inset = size.emblemRadius - size.minDimension / 2)) }

    companion object {
        /** The cell's own disc, as the northern games frame their maps. */
        val Disc = MapWindow(aspect = 1f, corner = 0.5f)
    }
}

/** A compass's bar, drawn by [draw], with its marks running [reach] either way from its middle. */
internal class CompassBar(val reach: Float, val draw: DrawScope.() -> Unit)

/** The quest marker in [look], [half] from its middle to its tip, built once to be drawn anywhere. */
internal class QuestMarker(look: HudLook, val half: Float) {
    private val shape = look.questShape(half)
    private val fill = look.colors.quest
    private val edge = look.colors.questEdge
    private val stroke = Stroke(half * 0.14f)

    fun DrawScope.draw(center: Offset, alpha: Float = 1f) = translate(center.x, center.y) {
        drawPath(shape, fill, alpha = alpha)
        drawPath(shape, edge, alpha = alpha, style = stroke)
    }
}

/**
 * The local map of where the user is in [look], else of a made-up place, or fog until there is one, turned with the
 * phone, its icons kept upright, darkening into a rimmed edge, with the player's arrow at the middle unless [marked] is
 * false because a hint lies over it, all inside the look's window, the marks on its rim reaching past it. What turns is
 * a layer of its own, so a turn moves it without drawing the rest again.
 */
@Composable
internal fun LocalMapEmblem(look: HudLook, marked: Boolean, modifier: Modifier) {
    val map = LocalMinimap.current
    val madeUp = LocalMadeUpMap.current
    val shown = { map() ?: madeUp() }
    val heading = LocalHeading.current
    val quest = LocalQuest.current
    val mapShown by remember { derivedStateOf { map() != null } }
    val creditAlpha = mapCreditAlpha(mapShown)
    val measurer = rememberTextMeasurer()
    val creditStyle = MaterialTheme.typography.labelSmall.copy(color = look.colors.ink, textAlign = TextAlign.Center)
    val window = look.window
    Box(modifier) {
        Spacer(
            Modifier
                .matchParentSize()
                .clip(window)
                .graphicsLayer { rotationZ = -(heading() ?: 0f) }
                .drawBehind {
                    val radius = window.reach(size)
                    shown()?.let { drawIcon(it.ground, center, (2 * radius).roundToInt()) } ?: drawCircle(look.colors.fog, radius)
                },
        )
        Spacer(
            Modifier.matchParentSize().clip(window).drawWithCache {
                val radius = window.reach(size)
                val scale = 2 * radius / MINIMAP_SIZE
                val iconSide = (2 * radius * MAP_ICON_SHARE).roundToInt()
                val marker = QuestMarker(look, radius * QUEST_ON_MAP)
                // A marker at the edge stays whole inside the window and clear of its rim, its tip on the spot.
                val markerRoom = window.bounds(size).let {
                    Rect(it.left + marker.half, it.top + 2 * marker.half, it.right - marker.half, it.bottom - marker.half / 2)
                }
                // Where the quest lies on a map, worked out once for each map rather than on each turn of the phone.
                var spotOf: PaintedMap<ImageBitmap>? = null
                var spot: Offset? = null
                onDrawBehind {
                    val painted = shown() ?: return@onDrawBehind
                    val facing = heading() ?: 0f
                    forEachUprightIcon(painted, center, scale, facing) { image, at -> drawIcon(image, at, iconSide) }
                    if (painted !== spotOf) {
                        spotOf = painted
                        spot = quest?.takeIf { painted === map() }?.let { questSpot(painted, it) }
                    }
                    // Its tip on the spot, as a pin's.
                    spot?.let { with(marker) { draw(markerRoom.towards(center, center + (it * scale).turnedWith(facing)) - Offset(0f, marker.half)) } }
                }
            },
        )
        Spacer(
            Modifier.matchParentSize().clip(window).drawWithCache {
                val radius = window.reach(size)
                val bounds = window.bounds(size)
                val over = with(look) { screenOver() }
                val vignette = Brush.radialGradient(
                    VIGNETTE_FROM to Color.Transparent,
                    1f to look.colors.rim.outer,
                    center = size.center,
                    radius = radius,
                )
                val arrow = playerArrow(size.center, bounds.width / 2 * ARROW_SHARE)
                val arrowEdge = Stroke(1.5.dp.toPx())
                val outer = Stroke(3.dp.toPx())
                val inner = Stroke(1.dp.toPx())
                val outerRim = Path().apply { addRoundRect(window.outline(size, outer.width / 2)) }
                val innerRim = Path().apply { addRoundRect(window.outline(size, outer.width)) }
                val credit = measurer.measure(
                    MAP_CREDIT.replace(" ©", "\n©"),
                    creditStyle,
                    // Narrow enough that its lines fit inside the window where they sit.
                    constraints = Constraints(maxWidth = (bounds.width * 0.6f).roundToInt()),
                )
                onDrawBehind {
                    over()
                    drawRect(vignette)
                    if (marked) {
                        drawPath(arrow, look.colors.ink)
                        drawPath(arrow, look.colors.arrowEdge, style = arrowEdge)
                    }
                    if (map() != null && creditAlpha.value > 0f) {
                        drawText(credit, topLeft = Offset(center.x - credit.size.width / 2f, bounds.bottom - bounds.height * 0.1f - credit.size.height), alpha = creditAlpha.value)
                    }
                    drawPath(outerRim, look.colors.rim.outer, style = outer)
                    drawPath(innerRim, look.colors.rim.inner, style = inner)
                }
            },
        )
        Spacer(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = -(heading() ?: 0f) }
                .drawWithCache {
                    val marks = with(look) { rimMarks(size.center, size.emblemRadius - 2.dp.toPx()) }
                    onDrawBehind { marks() }
                },
        )
    }
}

/** The compass along the top of the home page in [look], and over what lies below it, the name of the place the user has just discovered. */
@Composable
internal fun HudHeader(look: HudLook, discovery: String?, modifier: Modifier) {
    Column(modifier.zIndex(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Compass(look, opensWorldMap())
        DiscoveryBanner(look.colors.band, discovery) { look.DiscoveryCard(it) }
    }
}

/**
 * The game's compass in [look]: its bar, the quarters lettered on it and the map's places marked at their bearings, with
 * the user's quest, sliding along it as the phone turns. It stays north up without a heading: with no compass in the
 * phone, or while "Show where I am" is off, as headings are read only for the map of where the user is.
 */
@Composable
private fun Compass(look: HudLook, modifier: Modifier = Modifier) {
    val map = LocalMinimap.current
    val madeUp = LocalMadeUpMap.current
    val heading = LocalHeading.current
    val quest = LocalQuest.current
    val measurer = rememberTextMeasurer()
    val letterStyle = MaterialTheme.typography.titleMedium.copy(color = look.colors.ink, fontWeight = FontWeight.Medium)
    Spacer(
        modifier
            .fillMaxWidth(COMPASS_SPAN)
            .height(COMPASS_HEIGHT)
            // Read only as the way to the world map: the places it marks have no action, and the map's credit is in the launcher's menu.
            .clearAndSetSemantics {}
            .drawWithCache {
                val letters = listOf("N", "E", "S", "W").map { measurer.measure(it, letterStyle) }
                val northLetter = measurer.measure("N", letterStyle.copy(fontWeight = FontWeight.Bold))
                val mid = size.height / 2
                val band = size.height * COMPASS_BAND
                val bar = with(look) { compassBar(mid, band) }
                val marks = mid + band * look.compassDrop
                val line = Stroke(1.dp.toPx())
                val markSide = COMPASS_MARK.roundToPx()
                val tick = 3.dp.toPx()
                val ticks = (360f / look.compassTick).roundToInt()
                val marker = QuestMarker(look, band * QUEST_ON_COMPASS)
                // A map's places' bearings, and the quest's, worked out once for each map rather than on each turn of the phone.
                var bearingsOf: PaintedMap<ImageBitmap>? = null
                var bearings = FloatArray(0)
                var questBearing: Float? = null

                onDrawBehind {
                    bar.draw(this)

                    val facing = heading() ?: 0f
                    // How far along the bar from its middle a bearing lies, from -1 to 1, or past that off the bar.
                    fun share(bearing: Float) = turnBetween(facing, bearing) / (COMPASS_FIELD / 2)
                    fun x(share: Float) = size.width / 2 + share * bar.reach
                    fun fading(share: Float) = 1f - abs(share).pow(4)

                    for (step in 0 until ticks) {
                        val bearing = step * look.compassTick
                        val share = share(bearing)
                        if (abs(share) > 1f) continue
                        val degrees = bearing.roundToInt()
                        if (degrees % 90 == 0) {
                            val text = if (degrees == 0) northLetter else letters[degrees / 90]
                            drawText(text, topLeft = Offset(x(share) - text.size.width / 2f, marks - text.size.height / 2f), alpha = fading(share))
                        } else {
                            val half = if (degrees % 45 == 0) tick else tick / 2
                            val ink = look.colors.line.copy(alpha = look.colors.line.alpha * fading(share))
                            drawLine(ink, Offset(x(share), marks - half), Offset(x(share), marks + half), line.width)
                        }
                    }

                    val painted = map() ?: madeUp() ?: return@onDrawBehind
                    if (painted !== bearingsOf) {
                        bearingsOf = painted
                        bearings = bearingsOf(painted)
                        // Only a map of where the user is knows where they stand.
                        questBearing = quest
                            ?.takeIf { painted === map() && WebMercator.groundDistance(painted.centre, it) >= QUEST_HERE_M }
                            ?.let { painted.centre.bearingTo(it).toFloat() }
                    }
                    painted.icons.forEachIndexed { i, icon ->
                        val share = share(bearings[i])
                        if (abs(share) > 1f) return@forEachIndexed
                        val at = Offset(x(share), marks)
                        drawCircle(look.colors.shade.copy(alpha = look.colors.shade.alpha * fading(share) * 0.5f), markSide * 0.55f, at)
                        drawIcon(icon.image, at, markSide, fading(share))
                    }
                    questBearing?.let { bearing ->
                        val share = share(bearing)
                        if (abs(share) <= 1f) with(marker) { draw(Offset(x(share), marks), fading(share)) }
                    }
                }
            },
    )
}

/**
 * The name of the place the user has just discovered, its [discovery], told by [card] under the compass on a [band] that
 * fades out at its ends, as the games name a place discovered, fading in, and out once the screen lets it go.
 */
@Composable
private fun DiscoveryBanner(band: Color, discovery: String?, card: @Composable (String) -> Unit) {
    // The name stays while it fades out, after the screen has let it go.
    val last = remember { mutableStateOf("") }
    SideEffect { if (discovery != null) last.value = discovery }
    AnimatedVisibility(
        visible = discovery != null,
        modifier = Modifier
            .fillMaxWidth(COMPASS_SPAN)
            // Over what lies below, rather than pushing it down for the moment the name shows.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                layout(placeable.width, 0) { placeable.place(0, 0) }
            },
        enter = fadeIn(tween(DISCOVERY_IN_MILLIS)),
        exit = fadeOut(tween(DISCOVERY_OUT_MILLIS)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val ground = fadedAtEnds(band, 0.1f)
                    onDrawBehind { drawRect(ground) }
                }
                .padding(vertical = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { card(discovery ?: last.value) }
    }
}

/** A tap that opens the world map round where the user is, once there is a map of it to open round. */
@Composable
private fun opensWorldMap(): Modifier {
    val map = LocalMinimap.current
    val open = LocalOpenWorldMap.current
    val mapShown by remember { derivedStateOf { map() != null } }
    return if (mapShown) {
        Modifier.clickable(onClickLabel = "Open the world map", role = Role.Button, onClick = open).semantics { contentDescription = "World map" }
    } else {
        Modifier
    }
}

/** [point] moved the least way that puts it inside the rect. */
/** [point], or where the line to it from [middle], which lies inside this, leaves this: so it keeps its bearing. */
private fun Rect.towards(middle: Offset, point: Offset): Offset {
    val way = point - middle
    fun share(to: Float, from: Float, along: Float) = if (along == 0f) 1f else (to - from) / along
    val reach = minOf(
        1f,
        share(if (way.x < 0) left else right, middle.x, way.x),
        share(if (way.y < 0) top else bottom, middle.y, way.y),
    )
    return middle + way * reach
}

/** [color] across the middle, fading out over the [fade] share of the width at each end. */
internal fun fadedAtEnds(color: Color, fade: Float) = Brush.horizontalGradient(0f to Color.Transparent, fade to color, 1 - fade to color, 1f to Color.Transparent)

/**
 * Where [quest] lies from the middle of the map [painted], in its pixels east and south: where it is if the map reaches
 * it, else as far out as the map marks a quest, along the way the compass points to it.
 */
private fun questSpot(painted: PaintedMap<ImageBitmap>, quest: LatLon): Offset {
    val far = MINIMAP_SIZE / 2 * QUEST_REACH
    val shift = painted.centre.shiftTo(quest)
    val spot = Offset(shift.x.toFloat(), shift.y.toFloat())
    if (spot.getDistance() <= far) return spot
    val bearing = Math.toRadians(painted.centre.bearingTo(quest))
    return Offset(sin(bearing).toFloat(), -cos(bearing).toFloat()) * far
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
internal fun DrawScope.drawIcon(image: ImageBitmap, center: Offset, side: Int, alpha: Float = 1f) = drawImage(
    image,
    dstOffset = IntOffset((center.x - side / 2f).roundToInt(), (center.y - side / 2f).roundToInt()),
    dstSize = IntSize(side, side),
    alpha = alpha,
    filterQuality = FilterQuality.Medium,
)

/** The player's arrow on the local map, pointing up the way they face, [half] from its middle to its tip, notched at its tail. */
internal fun playerArrow(center: Offset, half: Float) = Path().apply {
    moveTo(center.x, center.y - half)
    lineTo(center.x + half * 0.7f, center.y + half * 0.8f)
    lineTo(center.x, center.y + half * 0.35f)
    lineTo(center.x - half * 0.7f, center.y + half * 0.8f)
    close()
}
