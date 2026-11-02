package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.turnBetween
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.ShieldBoardDark
import com.sqftware.orbitlauncher.ui.theme.ShieldBoardLight
import com.sqftware.orbitlauncher.ui.theme.ShieldIron
import com.sqftware.orbitlauncher.ui.theme.ShieldRivet
import com.sqftware.orbitlauncher.ui.theme.SkillNebula
import com.sqftware.orbitlauncher.ui.theme.SkillNebulaDeep
import com.sqftware.orbitlauncher.ui.theme.SkyrimHudColors
import com.sqftware.orbitlauncher.ui.theme.legible
import com.sqftware.orbitlauncher.ui.theme.typographyIn
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** How wide each of the compass's carved ends is, as a share of the bar's height. */
private const val COMPASS_END = 1.2f

/** The fewest slots that make a constellation: a triangle's lines would run under the emblem, so fewer are a hairline track. */
private const val MIN_CONSTELLATION = 4

/**
 * How far a slot's branch swings off the line out from the ring's middle, in radians, and how far out it reaches, and the
 * field of faint stars, as shares of the ring's radius.
 */
private const val BRANCH_SWING = 0.5f
private const val BRANCH_SEED = 17
private const val FIELD_SEED = 29
private const val BRANCH_REACH = 1.22f
private const val FIELD_STARS = 40
private const val FIELD_REACH = 1.3f

/** How far a slot's glow and its rays reach from its middle, as shares of its disc's diameter. */
private const val STAR_GLOW = 0.95f
private const val STAR_RAYS = 0.78f

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
 * user discovers below it; snow and mist drift over the launcher; folders are round Nord shields; menus are smoke between
 * bone hairlines, in a condensed type.
 */
object SkyrimArt : HudArt(SkyrimHud, Snowfall) {
    /**
     * The skills menu's constellation: the slots joined star to star by pale lines, each sending a branch out to a small
     * star of its own, over faint stars and a cool nebula. A ring of fewer slots is a hairline track instead.
     */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        if (slots.size < MIN_CONSTELLATION) return hairlineTrack(centre, slots, radius, iconSize, colours)
        val line = Stroke(1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2)) } }
        val lines = Path().apply {
            slots.forEachIndexed { i, star -> if (i == 0) moveTo(star.x, star.y) else lineTo(star.x, star.y) }
            close()
        }
        val starSide = 2.5.dp.toPx()
        // Kept on the page, which a crowded ring reaches nearly to the edge of.
        fun reach(share: Float) = min(radius * share, size.minDimension / 2 - 3 * starSide)
        val swing = Random(BRANCH_SEED)
        val branchEnds = slots.map { slot ->
            val angle = slot.angleFrom(centre) + (swing.nextFloat() - 0.5f) * BRANCH_SWING
            centre + Offset(cos(angle), sin(angle)) * reach(BRANCH_REACH)
        }
        val branches = Path().apply {
            slots.zip(branchEnds).forEach { (slot, end) ->
                moveTo(slot.x, slot.y)
                lineTo(end.x, end.y)
            }
        }
        val sparkles = Path().apply {
            branchEnds.forEach { addPath(sparkle(it, starSide * 1.6f)) }
            slots.indices.forEach { i -> addPath(sparkle((slots[i] + slots[(i + 1) % slots.size]) / 2f, starSide * 1.6f)) }
        }
        val sky = Random(FIELD_SEED)
        val field = List(FIELD_STARS) {
            val angle = sky.nextFloat() * 2 * PI.toFloat()
            val distance = reach(FIELD_REACH) * sqrt(sky.nextFloat())
            centre + Offset(cos(angle), sin(angle)) * distance to sky.nextFloat()
        }
        // Each slot a star, the app or folder on it at its heart: a glow round it and four thin rays, one pointing out.
        val glows = slots.map { Brush.radialGradient(0f to colours.mark, 1f to Color.Transparent, center = it, radius = iconSize * STAR_GLOW) }
        val rays = Path().apply { slots.forEach { addPath(sparkle(it, iconSize * STAR_RAYS, waist = 0.06f, angle = it.angleFrom(centre))) } }
        val nebulae = listOf(Offset(-0.35f, -0.25f) to SkillNebula, Offset(0.3f, 0.35f) to SkillNebulaDeep).map { (at, colour) ->
            val middle = centre + at * radius
            middle to Brush.radialGradient(0f to colour, 1f to Color.Transparent, center = middle, radius = radius * 0.95f)
        }
        return { glow, alpha ->
            if (alpha > 0f) {
                nebulae.forEach { (middle, brush) -> drawCircle(brush, radius * 0.95f, middle, alpha = alpha) }
                slots.zip(glows).forEach { (slot, brush) -> drawCircle(brush, iconSize * STAR_GLOW, slot, alpha = (0.3f + 0.2f * glow) * alpha) }
                val ink = colours.starLine.copy(alpha = (colours.starLine.alpha + 0.3f * glow) * alpha)
                clipPath(discs, ClipOp.Difference) {
                    field.forEach { (at, bright) -> drawCircle(colours.mark, starSide * (0.25f + 0.35f * bright), at, alpha = (0.2f + 0.4f * bright) * alpha) }
                    drawPath(lines, ink, style = line)
                    drawPath(branches, ink.copy(alpha = ink.alpha * 0.7f), style = line)
                    drawPath(rays, colours.mark, alpha = 0.6f * alpha)
                }
                drawPath(sparkles, colours.mark, alpha = (0.7f + 0.3f * glow) * alpha)
            }
        }
    }

    @Composable
    override fun folderWear(folder: RingItem.Folder): FolderWear = FolderWear(SHIELD_DISC, Modifier.shield())

    /** Skyrim's panels are plain smoke. */
    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) = Unit

    @Composable
    override fun menuBox(ground: PanelGround): Modifier = hairlinedBox(ground)

    override val menuRim: BorderStroke?
        @Composable get() = null

    @Composable
    override fun dialogBox(ground: PanelGround): Modifier = hairlinedBox(ground)

    @Composable
    override fun typography(day: Boolean): Typography = SkyrimTypography
}

private val SkyrimTypography = typographyIn(Condensed, display = 0.02f, heading = 0.06f, body = 0.02f, label = 0.1f)

/**
 * The northern game's view of where the user is: a gray local map in a bone-rimmed disc studded at the quarters, the
 * compass a bar of smoke between bone hairlines and carved ends, the quest marked in gold, and a place discovered named
 * large between hairlines.
 */
private object SkyrimHud : HudLook {
    override val colors = SkyrimHudColors
    override val compassTick = 45f

    // The game's gold marker: the player's arrow, pointing down.
    override fun questShape(half: Float) = playerArrow(Offset.Zero, -half)

    // Studs mark north and the quarters.
    override fun CacheDrawScope.rimMarks(center: Offset, radius: Float): DrawScope.() -> Unit {
        val stud = 4.dp.toPx()
        val studs = Path().apply {
            for (quarter in 0 until 4) {
                val half = if (quarter == 0) stud * 1.6f else stud
                val out = direction(quarter * 90f)
                addPath(diamond(center + out * radius, half, half * 0.6f, atan2(out.y, out.x)))
            }
        }
        return { drawPath(studs, colors.rim.inner) }
    }

    // A bar of smoke between hairlines, ending in carved ends, with a diamond pointing to where the user faces.
    override fun CacheDrawScope.compassBar(mid: Float, band: Float): CompassBar {
        val top = mid - band / 2
        val bottom = mid + band / 2
        val endWidth = band * COMPASS_END
        val line = Stroke(1.dp.toPx())
        val tick = 3.dp.toPx()
        val end = compassEnd(endWidth, mid, band)
        val pointer = diamond(Offset(size.width / 2, top), tick * 1.4f, tick)
        val barStart = endWidth * 0.9f
        // The marks run between the carved ends, fading as they reach them.
        return CompassBar(size.width / 2 - endWidth) {
            drawRect(colors.smoke, topLeft = Offset(barStart, top), size = Size(size.width - 2 * barStart, band))
            drawLine(colors.line, Offset(barStart, top), Offset(size.width - barStart, top), line.width)
            drawLine(colors.line, Offset(barStart, bottom), Offset(size.width - barStart, bottom), line.width)
            drawPath(end, colors.shade)
            drawPath(end, colors.line, style = line)
            scale(-1f, 1f) {
                drawPath(end, colors.shade)
                drawPath(end, colors.line, style = line)
            }
            drawPath(pointer, colors.line)
        }
    }

    // Written large, over a hairline jewelled at its middle and the word the game puts under it.
    @Composable
    override fun DiscoveryCard(name: String) {
        val type = MaterialTheme.typography
        Text(
            name.uppercase(),
            Modifier.fillMaxWidth(0.8f),
            style = type.headlineMedium.copy(color = colors.ink, letterSpacing = 0.12.em).legible(colors.ink),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(
            Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth(0.6f)
                .height(7.dp)
                .drawWithCache {
                    val hairline = fadedAtEnds(colors.line, 0.2f)
                    val jewel = diamond(size.center, size.height * 0.9f, size.height / 2, 0f)
                    onDrawBehind {
                        drawLine(hairline, Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx())
                        drawPath(jewel, colors.line)
                    }
                },
        )
        Text("DISCOVERED", style = type.titleSmall.copy(color = colors.ink, letterSpacing = 0.3.em).legible(colors.ink))
    }
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

/** A hairline track, with a small diamond midway between each pair of slots, as the game studs its lines. */
private fun CacheDrawScope.hairlineTrack(
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

/**
 * A star of the skills menu at [center]: four rays, [half] from it to each tip, the first at [angle], pinched in toward it
 * to [waist] of that between them.
 */
private fun sparkle(center: Offset, half: Float, waist: Float = 0.18f, angle: Float = 0f): Path {
    val along = Offset(cos(angle), sin(angle)) * half
    val corners = listOf(along, Offset(-along.y, along.x), -along, Offset(along.y, -along.x))
    return Path().apply {
        corners.forEachIndexed { i, tip ->
            val pinch = center + (tip + corners[(i + 1) % 4]) * waist
            if (i == 0) moveTo(center.x + tip.x, center.y + tip.y) else lineTo(center.x + tip.x, center.y + tip.y)
            lineTo(pinch.x, pinch.y)
        }
        close()
    }
}

private fun Offset.angleFrom(centre: Offset) = atan2(y - centre.y, x - centre.x)

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
