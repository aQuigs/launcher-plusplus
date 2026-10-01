package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.ChartEdge
import com.sqftware.orbitlauncher.ui.theme.Contour
import com.sqftware.orbitlauncher.ui.theme.Graticule
import com.sqftware.orbitlauncher.ui.theme.Land
import com.sqftware.orbitlauncher.ui.theme.MapFold
import com.sqftware.orbitlauncher.ui.theme.MapPaper
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.Route
import com.sqftware.orbitlauncher.ui.theme.Sea
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** How far the compass rose sways either way while the ring turns, and how many times in a turn: whole, so it never jumps. */
private const val SWAY_DEGREES = 4f
private const val SWAYS_PER_TURN = 4

/** How many times a globe turns in the hour the folders' sky takes to turn once. */
private const val GLOBE_TURNS_PER_HOUR = 2

/** How much of a folder's square the disc of previews takes on an island and on a folded map, which show round it. */
private const val ISLAND_DISC = 0.8f
private const val MAP_DISC = 0.72f

/** A hill's contour lines, as shares of its reach, from its foot to its top. */
private val HillSteps = listOf(1f, 0.8f, 0.6f, 0.42f, 0.26f)

/** A hill on the drawer's map: where it stands in its tile, as shares of it, how far its foot reaches, and its lines at unit reach. */
private class Hill(val x: Float, val y: Float, val reach: Dp, val contours: Path)

private val DrawerHills = listOf(
    Hill(0.24f, 0.28f, 170.dp, coastline(0.6f, 1.9f, steps = HillSteps)),
    Hill(0.8f, 0.72f, 210.dp, coastline(2.4f, 0.3f, steps = HillSteps)),
)

/**
 * An explorer's chart: the emblem is a compass whose rose sways gently while the ring turns, inside a bezel marked
 * with the four points; the ring is a scale of degrees; folders are islands, globes or folded maps; and the drawer is a
 * topographic map.
 */
object AtlasArt : ThemeArt {
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val scheme = MaterialTheme.colorScheme
        val letters = rememberTextMeasurer()
        val lettering = MaterialTheme.typography.labelSmall
        // Each part on a layer of its own, so the ring's glow and the rose swaying each redraw only it.
        Box(modifier) {
            Spacer(Modifier.fillMaxSize().graphicsLayer().drawWithCache { bezel(scheme, marked, letters, lettering) })
            if (marked) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = sin(slowTurn() * SWAYS_PER_TURN / 180f * PI.toFloat()) * SWAY_DEGREES }
                        .drawWithCache { rose(scheme) },
                )
            }
        }
    }

    /** A track through the slots, with a mark every five degrees inside it and a longer one at each point of the compass. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit = tickedTrack(
        centre,
        slots,
        radius,
        iconSize,
        colours,
        minor = ticks(centre, 72, radius - 3.dp.toPx(), radius) { it % 9 != 0 },
        major = ticks(centre, 8, radius - 7.dp.toPx(), radius + 2.dp.toPx()) { true },
        majorWidth = 1.5.dp,
    )

    @Composable
    override fun FolderFace(
        folder: RingItem.Folder,
        icon: suspend (AppEntry) -> ImageBitmap?,
        modifier: Modifier,
        presses: InteractionSource?,
        inner: () -> Float,
    ) {
        val style = LocalFolderStyle.current
        val (worn, disc) = when (style.look) {
            FolderLook.Globe -> Modifier.globe { style.minutes() } to Modifier.fillMaxSize().edge(ChartEdge)
            FolderLook.FoldedMap -> Modifier.foldedMap() to Modifier.fillMaxSize(MAP_DISC)
            // Islands, and a look of another theme's met before the theme's own is picked.
            else -> Modifier.island() to Modifier.fillMaxSize(ISLAND_DISC)
        }
        Box(modifier.then(worn), contentAlignment = Alignment.Center) { FolderDisc(folder, icon, presses, inner, disc) }
    }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val faint = colour.copy(alpha = colour.alpha * 0.3f)
        val line = 0.5.dp.toPx()
        forEachDrawerTile(scrolled) { origin, tile ->
            DrawerHills.forEach { hill ->
                val reach = hill.reach.toPx()
                withTransform({
                    translate(origin.x + hill.x * tile.width, origin.y + hill.y * tile.height)
                    scale(reach, reach, Offset.Zero)
                }) { drawPath(hill.contours, faint, style = Stroke(line / reach)) }
            }
        }
    }
}

/**
 * A closed wavering line round [centre], shaped by two ripples set at [first] and [second], at [reach] times each of
 * [steps], so one path holds a coast or all of a hill's contours.
 */
private fun coastline(first: Float, second: Float, centre: Offset = Offset.Zero, reach: Float = 1f, steps: List<Float> = listOf(1f)) =
    Path().apply {
        val points = 64
        steps.forEach { step ->
            repeat(points) { index ->
                val angle = index * 2 * PI.toFloat() / points
                val wave = 0.9f + 0.07f * sin(3 * angle + first) + 0.04f * sin(5 * angle + second)
                val at = centre + Offset(sin(angle), -cos(angle)) * (reach * step * wave)
                if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
            }
            close()
        }
    }

/** An island reaching a little past the item: land inside a two-lined coast, and contour lines inland, round the disc. */
private fun Modifier.island(): Modifier = drawWithCache {
    val reach = size.minDimension / 2 * 1.06f
    val land = coastline(0.6f, 1.9f, size.center, reach)
    val contours = coastline(0.6f, 1.9f, size.center, reach, steps = listOf(0.9f, 0.8f))
    val line = 1.dp.toPx()
    onDrawBehind {
        drawPath(land, Land)
        drawPath(land, ChartEdge.outer, style = Stroke(line * 3))
        drawPath(land, ChartEdge.inner, style = Stroke(line))
        drawPath(contours, Contour, style = Stroke(line))
    }
}

/**
 * A globe filling the item, turning as the [minutes] go by: the Earth's lands over its seas, and the graticule, whose
 * meridians are drawn only on the side facing out, so they turn with the lands.
 */
private fun Modifier.globe(minutes: () -> Float): Modifier = drawWithCache {
    val half = size.minDimension / 2
    val centre = size.center
    val land = Path()
    val line = Stroke(0.75.dp.toPx())
    val parallels = listOf(-0.5f, 0f, 0.5f).map { latitude ->
        val y = centre.y + latitude * half
        val reach = half * sqrt(1 - latitude * latitude)
        Offset(centre.x - reach, y) to Offset(centre.x + reach, y)
    }
    onDrawBehind {
        val spin = minutes() * GLOBE_TURNS_PER_HOUR / 60f * 2 * PI.toFloat()
        drawCircle(Sea, half, centre)
        land.rewind()
        EarthLand.forEach { it.addTo(land, centre, half, spin, 0f) }
        drawPath(land, Land)
        for (meridian in 0 until 12) {
            val longitude = spin + meridian * PI.toFloat() / 6
            if (cos(longitude) <= 0f) continue
            val width = abs(sin(longitude)) * half
            val start = if (sin(longitude) >= 0f) -90f else 90f
            drawArc(Graticule, start, 180f, false, Offset(centre.x - width, centre.y - half), Size(width * 2, half * 2), style = line)
        }
        parallels.forEach { (from, to) -> drawLine(Graticule, from, to, line.width) }
    }
}

/**
 * A map folded in three and tilted, reaching a little past the item, with a dashed route round its edge to an X in a
 * corner, where the disc of previews leaves them in sight.
 */
private fun Modifier.foldedMap(): Modifier = drawWithCache {
    val side = size.minDimension
    val corner = size.center - Offset(side / 2, side / 2)
    fun at(x: Float, y: Float) = corner + Offset(x * side, y * side)
    val inset = 1.dp.toPx()
    val route = Path().apply {
        at(0.1f, 0.88f).let { moveTo(it.x, it.y) }
        at(0.88f, 0.88f).let { lineTo(it.x, it.y) }
        at(0.88f, 0.26f).let { lineTo(it.x, it.y) }
    }
    val dashes = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.5.dp.toPx())))
    val x = at(0.86f, 0.14f)
    val arm = side * 0.06f
    val cross = 2.dp.toPx()
    onDrawBehind {
        rotate(-8f) {
            drawRect(MapPaper, corner, Size(side, side))
            drawRect(MapFold, at(1 / 3f, 0f), Size(side / 3, side))
            drawRect(ChartEdge.outer, corner, Size(side, side), style = Stroke(inset))
            drawRect(ChartEdge.inner, corner + Offset(inset, inset), Size(side - inset * 2, side - inset * 2), style = Stroke(inset))
            drawPath(route, Route, style = dashes)
            drawLine(Route, x - Offset(arm, arm), x + Offset(arm, arm), cross, StrokeCap.Round)
            drawLine(Route, x - Offset(arm, -arm), x + Offset(arm, -arm), cross, StrokeCap.Round)
        }
    }
}

/**
 * The compass's face and bezel, in the scheme's roles so it turns over with the wallpaper: a face of the see-through
 * ground, a ring marked every five degrees, and, while [marked], the four points lettered inside it, north in the
 * theme's red.
 */
private fun CacheDrawScope.bezel(scheme: ColorScheme, marked: Boolean, letters: TextMeasurer, lettering: TextStyle): DrawResult {
    val radius = size.emblemRadius
    val centre = size.center
    val cut = scheme.primary.copy(alpha = 0.5f)
    val rule = Stroke(0.75.dp.toPx())
    val pointMark = Stroke(1.5.dp.toPx())
    val degrees = ticks(centre, 72, radius * 0.84f, radius * 0.9f) { it % 9 != 0 }
    val points = ticks(centre, 8, radius * 0.78f, radius * 0.9f) { true }
    val style = lettering.copy(fontSize = (radius * 0.13f).toSp(), fontWeight = FontWeight.Bold)
    val lettered = if (marked) {
        listOf("N", "E", "S", "W").mapIndexed { index, letter ->
            Triple(letters.measure(letter, style), centre + direction(index * 90f) * (radius * 0.66f), index == 0)
        }
    } else {
        emptyList()
    }
    return onDrawBehind {
        drawCircle(scheme.surfaceContainerLowest, radius, centre)
        drawCircle(cut, radius * 0.9f, centre, style = rule)
        drawPath(degrees, cut, style = rule)
        drawPath(points, scheme.primary, style = pointMark)
        lettered.forEach { (layout, at, north) ->
            val box = layout.size
            drawText(layout, if (north) scheme.tertiary else scheme.primary, at - Offset(box.width / 2f, box.height / 2f))
        }
    }
}

/**
 * The compass rose: four long points and four short ones between, each lit on one side and shaded on the other. North's
 * is in the theme's red, shaded in its light container, so it stands out by its lightness too and not by hue alone.
 */
private fun CacheDrawScope.rose(scheme: ColorScheme): DrawResult {
    val radius = size.emblemRadius
    val centre = size.center
    // The short points first, so the long ones lie over them.
    val halves = (0 until 8).sortedBy { it % 2 == 0 }.flatMap { point ->
        val long = point % 2 == 0
        val along = direction(point * 45f)
        val across = Offset(-along.y, along.x) * (radius * if (long) 0.085f else 0.06f)
        val tip = centre + along * (radius * if (long) 0.52f else 0.3f)
        fun half(side: Float) = Path().apply {
            moveTo(centre.x, centre.y)
            lineTo(tip.x, tip.y)
            (centre + across * side).let { lineTo(it.x, it.y) }
            close()
        }
        val north = point == 0
        listOf(
            half(-1f) to if (north) scheme.tertiary else scheme.primary,
            half(1f) to if (north) scheme.tertiaryContainer else scheme.primary.copy(alpha = 0.55f),
        )
    }
    val hub = radius * 0.05f
    val hubRim = Stroke(1.dp.toPx())
    return onDrawBehind {
        halves.forEach { (path, colour) -> drawPath(path, colour) }
        drawCircle(scheme.inverseOnSurface, hub, centre)
        drawCircle(scheme.primary, hub, centre, style = hubRim)
    }
}
