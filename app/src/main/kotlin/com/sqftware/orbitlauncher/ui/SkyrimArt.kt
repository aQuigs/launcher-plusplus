package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.sqftware.orbitlauncher.domain.minimap.MINIMAP_SIZE
import com.sqftware.orbitlauncher.domain.minimap.PaintedMap
import com.sqftware.orbitlauncher.ui.theme.CompassLetter
import com.sqftware.orbitlauncher.ui.theme.CompassLine
import com.sqftware.orbitlauncher.ui.theme.CompassShade
import com.sqftware.orbitlauncher.ui.theme.CompassSmoke
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

/** How tall the compass is, how wide its marks are drawn, and how much of the page's width it spans. */
private val COMPASS_HEIGHT = 44.dp
private val COMPASS_MARK = 18.dp
private const val COMPASS_SPAN = 0.86f

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
 * of the home page with the map's places marked on it; folders are round Nord shields; menus are smoke between bone
 * hairlines, in a condensed type.
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
                        val ground = shown()?.ground
                        if (ground == null) {
                            drawCircle(LocalMapFog, radius)
                        } else {
                            val side = (2 * radius).roundToInt()
                            drawImage(
                                ground,
                                dstOffset = IntOffset((center.x - side / 2f).roundToInt(), (center.y - side / 2f).roundToInt()),
                                dstSize = IntSize(side, side),
                                filterQuality = FilterQuality.Medium,
                            )
                        }
                    },
            )
            Spacer(
                Modifier.matchParentSize().graphicsLayer().drawBehind {
                    val painted = shown()?.takeIf { it.icons.isNotEmpty() } ?: return@drawBehind
                    val radius = size.emblemRadius
                    val scale = 2 * radius / MINIMAP_SIZE
                    val iconSide = (2 * radius * MAP_ICON_SHARE).roundToInt()
                    val turn = Math.toRadians(-(heading() ?: 0f).toDouble())
                    val (cos, sin) = cos(turn).toFloat() to sin(turn).toFloat()
                    painted.icons.forEach { icon ->
                        val dx = (icon.x - MINIMAP_SIZE / 2f) * scale
                        val dy = (icon.y - MINIMAP_SIZE / 2f) * scale
                        drawIcon(icon.image, center + Offset(dx * cos - dy * sin, dx * sin + dy * cos), iconSide)
                    }
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
                        constraints = Constraints(maxWidth = (radius * 0.9f).roundToInt()),
                    )
                    onDrawBehind {
                        drawCircle(vignette, radius)
                        if (marked) {
                            drawPath(arrow, PlayerArrow)
                            drawPath(arrow, PlayerArrowEdge, style = arrowEdge)
                        }
                        if (map() != null && creditAlpha.value > 0f) {
                            drawText(credit, topLeft = Offset(center.x - credit.size.width / 2f, center.y + radius * 0.88f - credit.size.height), alpha = creditAlpha.value)
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
                        onDrawBehind {
                            for (quarter in 0 until 4) {
                                rotate(quarter * 90f, center) {
                                    val half = if (quarter == 0) stud * 1.6f else stud
                                    drawPath(diamond(Offset(center.x, center.y - radius), half, half * 0.6f), LocalMapRim.inner)
                                }
                            }
                        }
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
                // Half the turn from one slot to the next, the short way, so two slots facing each other get a stud each side.
                val from = atan2(a.y, a.x)
                val turn = (atan2(b.y, b.x) - from + 3 * PI.toFloat()) % (2 * PI.toFloat()) - PI.toFloat()
                val angle = from + turn / 2
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

    @Composable
    override fun HomeHeader(modifier: Modifier) = Compass(modifier)

    /** A menu is the ground squared off between hairlines, a diamond at the middle of each, as the game's are. */
    @Composable
    override fun menuBox(ground: PanelGround): Modifier = Modifier.solidGround(ground, RectangleShape).hairlined(MaterialTheme.colorScheme.outline)

    override val menuRim: BorderStroke?
        @Composable get() = null

    @Composable
    override fun dialogBox(ground: PanelGround): Modifier = Modifier.solidGround(ground, RectangleShape).hairlined(MaterialTheme.colorScheme.outline)

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
 * The game's compass: a bar of smoke between bone hairlines that fades out at both ends, the quarters lettered on it and
 * the map's places marked at their bearings, sliding along it as the phone turns. It stays north up without a heading:
 * with no compass in the phone, or while "Show where I am" is off, as headings are read only for the map of where the
 * user is.
 */
@Composable
private fun Compass(modifier: Modifier) {
    val map = LocalMinimap.current
    val madeUp = LocalMadeUpMap.current
    val heading = LocalHeading.current
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
                val mid = size.height * 0.5f
                val band = size.height * 0.62f
                val reach = size.width / 2
                val line = Stroke(1.dp.toPx())
                val markSide = COMPASS_MARK.roundToPx()
                val tick = 3.dp.toPx()
                val fade = Brush.horizontalGradient(0f to Color.Transparent, 0.18f to CompassSmoke, 0.82f to CompassSmoke, 1f to Color.Transparent)
                val hairline = Brush.horizontalGradient(0f to Color.Transparent, 0.12f to CompassLine, 0.88f to CompassLine, 1f to Color.Transparent)

                // Where a bearing lies along the bar, and how much it fades toward the ends; null off the bar.
                fun place(bearing: Float, facing: Float): Pair<Float, Float>? {
                    val off = ((bearing - facing) % 360f + 540f) % 360f - 180f
                    val share = off / (COMPASS_FIELD / 2)
                    if (abs(share) > 1f) return null
                    return size.width / 2 + share * reach * 0.92f to (1f - abs(share).pow(4))
                }

                onDrawBehind {
                    drawRect(fade, topLeft = Offset(0f, mid - band / 2), size = Size(size.width, band))
                    drawLine(hairline, Offset(0f, mid - band / 2), Offset(size.width, mid - band / 2), line.width)
                    drawLine(hairline, Offset(0f, mid + band / 2), Offset(size.width, mid + band / 2), line.width)
                    drawPath(diamond(Offset(size.width / 2, mid - band / 2), tick * 1.4f, tick), CompassLine)

                    val facing = heading() ?: 0f
                    for (step in 0 until 8) {
                        val (x, alpha) = place(step * 45f, facing) ?: continue
                        if (step % 2 == 0) {
                            val text = if (step == 0) northLetter else letters[step / 2]
                            drawText(text, topLeft = Offset(x - text.size.width / 2f, mid - text.size.height / 2f), alpha = alpha)
                        } else {
                            drawLine(CompassLine.copy(alpha = CompassLine.alpha * alpha), Offset(x, mid - tick), Offset(x, mid + tick), line.width)
                        }
                    }
                    (map() ?: madeUp())?.let { painted -> drawPlaces(painted, facing, markSide, mid, ::place) }
                }
            },
    )
}

/** The places on [painted] at their bearings from its middle, which the map is centerd on. */
private fun DrawScope.drawPlaces(
    painted: PaintedMap<ImageBitmap>,
    facing: Float,
    side: Int,
    mid: Float,
    place: (bearing: Float, facing: Float) -> Pair<Float, Float>?,
) {
    painted.icons.forEach { icon ->
        val dx = icon.x - MINIMAP_SIZE / 2f
        val dy = icon.y - MINIMAP_SIZE / 2f
        val bearing = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
        val (x, alpha) = place(bearing, facing) ?: return@forEach
        drawCircle(CompassShade.copy(alpha = CompassShade.alpha * alpha * 0.5f), side * 0.55f, Offset(x, mid))
        drawIcon(icon.image, Offset(x, mid), side, alpha)
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

/** Hairlines in [ink] along the top and bottom edges, each with a diamond at its middle. */
private fun Modifier.hairlined(ink: Color) = drawWithContent {
    drawContent()
    val width = 1.dp.toPx()
    val half = 3.dp.toPx()
    for (y in listOf(width / 2, size.height - width / 2)) {
        drawLine(ink, Offset(0f, y), Offset(size.width, y), width)
        drawPath(diamond(Offset(size.width / 2, y), half * 1.6f, half, 0f), ink)
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
            val angle = 2 * PI.toFloat() * i / SHIELD_RIVETS
            val at = center + Offset(cos(angle), sin(angle)) * (radius - rim.width / 2)
            drawCircle(ShieldRivet, rim.width * 0.22f, at)
        }
    }
}
