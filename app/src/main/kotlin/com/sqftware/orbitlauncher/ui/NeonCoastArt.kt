package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.CityWindows
import com.sqftware.orbitlauncher.ui.theme.PalmShadow
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.Skyline
import com.sqftware.orbitlauncher.ui.theme.StarGlint
import com.sqftware.orbitlauncher.ui.theme.SunGlitter
import com.sqftware.orbitlauncher.ui.theme.SunGold
import com.sqftware.orbitlauncher.ui.theme.SunHalo
import com.sqftware.orbitlauncher.ui.theme.SunPink
import com.sqftware.orbitlauncher.ui.theme.SunsetEdge
import com.sqftware.orbitlauncher.ui.theme.SunsetSea
import com.sqftware.orbitlauncher.ui.theme.SunsetSky
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How many times a neon sign hums brighter and back in the hour the folders' sky takes to turn once. */
private const val HUMS_PER_HOUR = 90

/** How much of a folder's square the disc of previews takes on each look, which shows round it. */
private const val SIGN_DISC = 0.74f
private const val SUNSET_DISC = 0.72f

/** How many bands a sunset sun is cut into below its middle. */
private const val SUN_BANDS = 6

/**
 * A coast city after sundown: the emblem is a striped sun sinking into a glittering sea behind a palm; the ring a pink
 * neon tube round a cyan one; folders are neon signs or striped suns; the drawer is a retro
 * grid running to a palm-lined horizon; and the sunset over the city's skyline is a scene for the wallpaper.
 */
object NeonCoastArt : ThemeArt {
    override val scene = ThemeScene("sunset") { sunset() }

    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val ground = MaterialTheme.colorScheme.surfaceContainerLowest
        Spacer(
            modifier.graphicsLayer().drawWithCache {
                val radius = size.emblemRadius
                val centre = size.center
                val disc = Path().apply { addOval(Rect(centre, radius)) }
                val top = centre.y - radius
                val horizon = centre.y + radius * 0.2f
                val sky = Brush.verticalGradient(SunsetSky, top, horizon)
                val sea = Brush.verticalGradient(SunsetSea, horizon, centre.y + radius)
                val sun = SettingSun(Offset(centre.x, horizon - radius * 0.24f), radius * 0.52f, horizon)
                val palm = palmTree(Offset(centre.x - radius * 0.5f, centre.y + radius * 1.05f), radius * 1.05f, lean = -0.32f)
                val glitter = 1.5.dp.toPx()
                onDrawBehind {
                    if (!marked) {
                        drawCircle(ground, radius)
                        return@onDrawBehind
                    }
                    val turn = fastTurn()
                    clipPath(disc) {
                        drawRect(sky, Offset(centre.x - radius, top), Size(radius * 2, horizon - top))
                        drawCircle(sun.fill, sun.radius, sun.centre)
                        sun.forEachCut(cycles(turn, 4) % 1f) { cutTop, cut ->
                            drawRect(sky, Offset(sun.centre.x - sun.radius, cutTop), Size(sun.radius * 2, cut))
                        }
                        drawRect(sea, Offset(centre.x - radius, horizon), Size(radius * 2, centre.y + radius - horizon))
                        drawGlitter(centre.x, horizon, centre.y + radius, sun.radius, glitter, cycles(turn, 2) * 2 * PI.toFloat())
                        drawPath(palm, PalmShadow)
                    }
                }
            },
        )
    }

    /** A pink neon tube round the slots with its haze, and a thin cyan one inside it, broken round each slot's disc. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val apart = 3.dp.toPx()
        val outer = Path().apply { addOval(Rect(centre, radius + apart)) }
        val inner = Path().apply { addOval(Rect(centre, radius - apart)) }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 4.dp.toPx())) } }
        val haze = Stroke(8.dp.toPx())
        val tube = Stroke(1.75.dp.toPx())
        val thin = Stroke(1.dp.toPx())
        return { glow, alpha ->
            if (alpha > 0f) {
                clipPath(discs, ClipOp.Difference) {
                    drawPath(outer, colours.starLine.copy(alpha = (0.16f + 0.24f * glow) * alpha), style = haze)
                    drawPath(outer, colours.starLine.copy(alpha = (0.85f + 0.15f * glow) * alpha), style = tube)
                    drawPath(inner, colours.mark.copy(alpha = (0.6f + 0.4f * glow) * alpha), style = thin)
                }
            }
        }
    }

    @Composable
    override fun folderWear(folder: RingItem.Folder): FolderWear {
        val style = LocalFolderStyle.current
        return when (style.look) {
            FolderLook.Sunset -> FolderWear(SUNSET_DISC, Modifier.sunsetSun())
            // Neon signs, and a look of another theme's met before the theme's own is picked.
            else -> FolderWear(SIGN_DISC, Modifier.neonSign(MaterialTheme.colorScheme) { style.minutes() })
        }
    }

    private val boulevard = DrawerTileArt { Boulevard(this, it) }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val grid = colour.copy(alpha = colour.alpha * 0.3f)
        val shapes = colour.copy(alpha = colour.alpha * 0.18f)
        forEachDrawerTile(scrolled) { origin, tile ->
            val built = with(boulevard) { of(tile) }
            translate(origin.x, origin.y) {
                clipRect(0f, 0f, tile.width, tile.height) {
                    drawPath(built.grid, grid, style = built.line)
                    drawPath(built.shapes, shapes)
                }
            }
        }
    }
}

/**
 * A sun of [radius] round [centre], setting at a [horizon] below its middle, [fill]ed from gold to pink and cut between
 * its middle and the horizon into bands, the cuts between them growing from nothing at its middle as they drift down.
 */
private class SettingSun(val centre: Offset, val radius: Float, val horizon: Float) {
    val fill = Brush.verticalGradient(listOf(SunGold, SunPink), centre.y - radius, horizon)

    /** Calls [cut] with the top and the height of each cut between the bands, moved down by [drift], a share of the gap between two. */
    inline fun forEachCut(drift: Float, cut: (top: Float, height: Float) -> Unit) {
        val band = (horizon - centre.y) / SUN_BANDS
        for (index in 0..SUN_BANDS) {
            val y = centre.y + (index + drift) * band
            cut(y, band * 0.55f * (y - centre.y) / (horizon - centre.y))
        }
    }

    /** The sun above the horizon with its bands still at [drift], to fill with [fill]: built once, for a sun that does not move. */
    fun shape(drift: Float): Path {
        val disc = Path().apply { addOval(Rect(centre, radius)) }
        val above = Path().apply { addRect(Rect(centre.x - radius, centre.y - radius, centre.x + radius, horizon)) }
        val cuts = Path().apply { forEachCut(drift) { top, height -> addRect(Rect(centre.x - radius, top, centre.x + radius, top + height)) } }
        return Path.combine(PathOperation.Difference, Path.combine(PathOperation.Intersect, disc, above), cuts)
    }
}

/**
 * The sun's glitter on the sea below [horizon] down to [bottom], under a sun [reach] across about [middle]: short dashes
 * [line] thick, shorter as they near, each wavering as [wave] goes round.
 */
private fun DrawScope.drawGlitter(middle: Float, horizon: Float, bottom: Float, reach: Float, line: Float, wave: Float) {
    val rows = 9
    repeat(rows) { row ->
        val share = (row + 0.5f) / rows
        val y = horizon + (bottom - horizon) * share.pow(1.3f)
        val half = reach * (1f - 0.7f * share) * (0.75f + 0.25f * sin(wave + row * 1.7f))
        drawLine(SunGlitter.copy(alpha = 0.85f * (1f - share)), Offset(middle - half, y), Offset(middle + half, y), line)
    }
}

/**
 * A palm standing at [base], [height] tall, its crown leaning [lean] of its height to the right (left if negative): a
 * trunk tapering as it bends, and fronds arching out and drooping from the crown.
 */
private fun palmTree(base: Offset, height: Float, lean: Float): Path = Path().apply {
    val crown = base + Offset(lean * height, -height)
    val bend = Offset(base.x + lean * height * 0.15f, base.y - height * 0.55f)
    val foot = height * 0.035f
    val neck = height * 0.016f
    moveTo(base.x - foot, base.y)
    quadraticTo(bend.x - foot, bend.y, crown.x - neck, crown.y)
    lineTo(crown.x + neck, crown.y)
    quadraticTo(bend.x + foot, bend.y, base.x + foot, base.y)
    close()

    val frond = height * 0.42f
    listOf(-175f, -145f, -112f, -68f, -35f, -5f, 150f, 30f).forEachIndexed { index, degrees ->
        val way = direction(degrees + 90f)
        val across = Offset(-way.y, way.x)
        val length = frond * if (index % 2 == 0) 1f else 0.8f
        val tip = crown + way * length + Offset(0f, length * 0.45f)
        val arch = crown + way * (length * 0.55f) - Offset(0f, length * 0.12f)
        val width = length * 0.13f
        moveTo(crown.x, crown.y)
        quadraticTo(arch.x + across.x * width, arch.y + across.y * width, tip.x, tip.y)
        quadraticTo(arch.x - across.x * width, arch.y - across.y * width, crown.x, crown.y)
        close()
    }
}

/** Two palms at the left of a [width] and one at the right, standing on [ground], the tallest [tall]. */
private fun palmGrove(width: Float, ground: Float, tall: Float): Path = Path().apply {
    addPath(palmTree(Offset(width * 0.1f, ground), tall, lean = 0.26f))
    addPath(palmTree(Offset(width * 0.22f, ground), tall * 0.68f, lean = 0.12f))
    addPath(palmTree(Offset(width * 0.89f, ground), tall * 0.86f, lean = -0.27f))
}

/**
 * A drawer tile's boulevard: a grid running from the bottom of the tile to a vanishing point on the horizon, a striped
 * sun rising over it, and palms along the horizon at either side.
 */
private class Boulevard(density: Density, size: Size) {
    val line = Stroke(with(density) { 0.75.dp.toPx() })
    private val horizon = size.height * 0.4f
    private val vanishing = Offset(size.width / 2, horizon)

    val grid = Path().apply {
        moveTo(0f, horizon)
        lineTo(size.width, horizon)
        val rows = 12
        for (row in 1..rows) {
            val y = horizon + (size.height - horizon) * (row.toFloat() / rows).pow(2)
            moveTo(0f, y)
            lineTo(size.width, y)
        }
        val columns = 16
        for (column in 0..columns) {
            val x = -size.width + 3 * size.width * column / columns
            moveTo(vanishing.x, vanishing.y)
            lineTo(x, size.height)
        }
    }

    /** The sun rising over the horizon and the palms along it, filled alike. */
    val shapes = with(density) {
        val radius = 84.dp.toPx()
        SettingSun(vanishing - Offset(0f, radius * 0.3f), radius, horizon).shape(0.5f).apply {
            addPath(palmGrove(size.width, horizon, 150.dp.toPx()))
        }
    }
}

/**
 * A neon tube bent into a rounded square round the item on its dark backing, in the [colours]' fixed roles, which stay
 * the same on any wallpaper: the lead neon humming brighter and dimmer as the [minutes] go by, round a thin second one.
 */
private fun Modifier.neonSign(colours: ColorScheme, minutes: () -> Float): Modifier = drawWithCache {
    val backing = colours.onPrimaryFixed.copy(alpha = 0.85f)
    val lead = colours.primaryFixed
    val second = colours.tertiaryFixed.copy(alpha = 0.8f)
    val reach = size.minDimension / 2 * 1.08f
    val box = Rect(size.center, reach)
    val corner = CornerRadius(reach * 0.42f)
    val inset = 4.dp.toPx()
    val inner = box.deflate(inset)
    val innerCorner = CornerRadius(corner.x - inset)
    val haze = Stroke(7.dp.toPx())
    val tube = Stroke(2.dp.toPx())
    val thin = Stroke(1.dp.toPx())
    onDrawBehind {
        val hum = 0.82f + 0.18f * sin(minutes() * HUMS_PER_HOUR / 60f * 2 * PI.toFloat())
        drawRoundRect(backing, box.topLeft, box.size, corner)
        drawRoundRect(lead.copy(alpha = 0.3f * hum), box.topLeft, box.size, corner, style = haze)
        drawRoundRect(lead.copy(alpha = hum), box.topLeft, box.size, corner, style = tube)
        drawRoundRect(second, inner.topLeft, inner.size, innerCorner, style = thin)
    }
}

/** A striped sunset sun round the item, its bands still, edged in two lines so it holds on any wallpaper. */
private fun Modifier.sunsetSun(): Modifier = drawWithCache {
    val radius = size.minDimension / 2 * 1.06f
    val centre = size.center
    val sun = SettingSun(centre, radius, centre.y + radius)
    val banded = sun.shape(0.5f)
    val line = Stroke(1.dp.toPx())
    val outer = Stroke(3.dp.toPx())
    onDrawBehind {
        drawPath(banded, sun.fill)
        drawCircle(SunsetEdge.outer, radius, centre, style = outer)
        drawCircle(SunsetEdge.inner, radius, centre, style = line)
    }
}

/**
 * The sunset over the coast: a sky going from night to orange, its first stars, a striped sun sinking behind a city's
 * lit skyline, its glitter on the sea, and palms black on the shore in front.
 */
private fun DrawScope.sunset() {
    val width = size.width
    val height = size.height
    val horizon = height * 0.6f
    drawRect(Brush.verticalGradient(SunsetSky, 0f, horizon), size = Size(width, horizon))

    val random = Random(6)
    repeat(70) {
        val at = Offset(random.nextFloat() * width, random.nextFloat().pow(1.6f) * horizon * 0.5f)
        drawCircle(StarGlint, 0.5.dp.toPx() + random.nextFloat() * 1.2.dp.toPx(), at)
    }

    // About where the home page puts the ring's centre, so the sun sets inside the ring.
    val sun = SettingSun(Offset(width / 2, horizon - size.minDimension * 0.12f), size.minDimension * 0.34f, horizon)
    drawCircle(Brush.radialGradient(listOf(SunHalo, Color.Transparent), sun.centre, sun.radius * 1.9f), sun.radius * 1.9f, sun.centre)
    drawPath(sun.shape(0.3f), sun.fill)
    drawSkyline(random, horizon)

    drawRect(Brush.verticalGradient(SunsetSea, horizon, height), Offset(0f, horizon), Size(width, height - horizon))
    drawGlitter(width / 2, horizon, height * 0.92f, sun.radius, 3.dp.toPx(), 0f)

    val shore = Path().apply {
        moveTo(0f, height)
        lineTo(0f, height * 0.9f)
        quadraticTo(width * 0.3f, height * 0.86f, width * 0.55f, height * 0.93f)
        quadraticTo(width * 0.8f, height * 0.88f, width, height * 0.9f)
        lineTo(width, height)
        close()
    }
    shore.addPath(palmGrove(width, height * 0.9f, height * 0.36f))
    drawPath(shore, PalmShadow)
}

/** A city's towers along the [horizon], their windows lit at random in the city's neon. */
private fun DrawScope.drawSkyline(random: Random, horizon: Float) {
    val window = 2.dp.toPx()
    val step = window * 2.5f
    var x = -random.nextFloat() * 20.dp.toPx()
    while (x < size.width) {
        val towerWidth = size.width * (0.035f + random.nextFloat() * 0.05f)
        val tall = size.height * (0.025f + random.nextFloat().pow(2.5f) * 0.11f)
        drawRect(Skyline, Offset(x, horizon - tall), Size(towerWidth, tall))
        var y = horizon - tall + step
        while (y < horizon - step) {
            var across = x + step * 0.6f
            while (across < x + towerWidth - window) {
                if (random.nextFloat() < 0.3f) drawRect(CityWindows.random(random), Offset(across, y), Size(window, window))
                across += step
            }
            y += step
        }
        x += towerWidth + random.nextFloat() * 4.dp.toPx()
    }
}
