package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.CityWindows
import com.sqftware.orbitlauncher.ui.theme.PalmShadow
import com.sqftware.orbitlauncher.ui.theme.Skyline
import com.sqftware.orbitlauncher.ui.theme.StarGlint
import com.sqftware.orbitlauncher.ui.theme.SunGlitter
import com.sqftware.orbitlauncher.ui.theme.SunGold
import com.sqftware.orbitlauncher.ui.theme.SunHalo
import com.sqftware.orbitlauncher.ui.theme.SunPink
import com.sqftware.orbitlauncher.ui.theme.SunsetSea
import com.sqftware.orbitlauncher.ui.theme.SunsetSky
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How many bands the sun is cut into below its middle. */
private const val SUN_BANDS = 6

/**
 * The sunset over a coast city, which the launcher's menu can set as the wallpaper: a sky going from night to orange,
 * its first stars, a striped sun sinking behind the city's lit skyline, its glitter on the sea, and palms black on the
 * shore in front.
 */
internal val Sunset = ThemeScene("sunset") { sunset() }

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
    val centre = Offset(width / 2, horizon - size.minDimension * 0.12f)
    val radius = size.minDimension * 0.34f
    drawCircle(Brush.radialGradient(listOf(SunHalo, Color.Transparent), centre, radius * 1.9f), radius * 1.9f, centre)
    drawPath(settingSun(centre, radius, horizon), Brush.verticalGradient(listOf(SunGold, SunPink), centre.y - radius, horizon))
    drawSkyline(random, horizon)

    drawRect(Brush.verticalGradient(SunsetSea, horizon, height), Offset(0f, horizon), Size(width, height - horizon))
    drawGlitter(width / 2, horizon, height * 0.92f, radius, 3.dp.toPx())

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

/**
 * A sun of [radius] round [centre], setting at a [horizon] below its middle and cut between its middle and the horizon
 * into bands, the cuts between them growing from nothing at its middle.
 */
private fun settingSun(centre: Offset, radius: Float, horizon: Float): Path {
    val disc = Path().apply { addOval(Rect(centre, radius)) }
    val above = Path().apply { addRect(Rect(centre.x - radius, centre.y - radius, centre.x + radius, horizon)) }
    val band = (horizon - centre.y) / SUN_BANDS
    val cuts = Path().apply {
        for (index in 0..SUN_BANDS) {
            val top = centre.y + (index + 0.3f) * band
            addRect(Rect(centre.x - radius, top, centre.x + radius, top + band * 0.55f * (top - centre.y) / (horizon - centre.y)))
        }
    }
    return Path.combine(PathOperation.Difference, Path.combine(PathOperation.Intersect, disc, above), cuts)
}

/**
 * The sun's glitter on the sea below [horizon] down to [bottom], under a sun [reach] across about [middle]: short dashes
 * [line] thick, shorter as they near.
 */
private fun DrawScope.drawGlitter(middle: Float, horizon: Float, bottom: Float, reach: Float, line: Float) {
    val rows = 9
    repeat(rows) { row ->
        val share = (row + 0.5f) / rows
        val y = horizon + (bottom - horizon) * share.pow(1.3f)
        val half = reach * (1f - 0.7f * share) * (0.75f + 0.25f * sin(row * 1.7f))
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
