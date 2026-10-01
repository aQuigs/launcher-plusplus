package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.GardenEdge
import com.sqftware.orbitlauncher.ui.theme.Koi
import com.sqftware.orbitlauncher.ui.theme.LilyPad
import com.sqftware.orbitlauncher.ui.theme.Moss
import com.sqftware.orbitlauncher.ui.theme.MossDark
import com.sqftware.orbitlauncher.ui.theme.MossLight
import com.sqftware.orbitlauncher.ui.theme.Pebble
import com.sqftware.orbitlauncher.ui.theme.Pond
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.Stone
import com.sqftware.orbitlauncher.ui.theme.StoneLight
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/** How many ripples spread out from the emblem's stone at once, and how many set out in a fast turn: whole, so none jumps. */
private const val RIPPLES = 4
private const val RIPPLES_PER_TURN = 6

/** How many times the koi swim round their pond in the hour the folders' sky takes to turn once. */
private const val KOI_LAPS_PER_HOUR = 20

/** How much of a folder's square the disc of previews takes on a stone or on moss, which show round it. */
private const val PATCH_DISC = 0.84f

/** Where each of the drawer's stones lies in its tile, as shares of it, and how far it reaches; the rake goes round it. */
private class GardenStone(val x: Float, val y: Float, val reach: Dp)

private val DrawerStones = listOf(GardenStone(0.78f, 0.22f, 34.dp), GardenStone(0.22f, 0.7f, 26.dp))

/** Where the moss's tufts grow, as shares of the folder's half-width and of its reach, light ones then dark. */
private val Tufts = listOf(
    Triple(Offset(-0.62f, -0.55f), 0.1f, true),
    Triple(Offset(0.7f, 0.4f), 0.12f, true),
    Triple(Offset(0.1f, 0.74f), 0.08f, false),
    Triple(Offset(-0.8f, 0.3f), 0.07f, false),
    Triple(Offset(0.48f, -0.62f), 0.07f, false),
)

/**
 * A raked garden: the emblem is a stone that ripples slowly spread out from while the ring turns; the ring is a raked path,
 * every slot a stone with ripples raked round it; folders are stones, koi ponds or moss; and the drawer is raked sand
 * round stones.
 */
object ZenArt : ThemeArt {
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val scheme = MaterialTheme.colorScheme
        // Each part on a layer of its own, so the ring's glow and the ripples spreading each redraw only it.
        Box(modifier) {
            Spacer(
                Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                    onDrawBehind { drawCircle(scheme.surfaceContainerLowest, size.emblemRadius) }
                },
            )
            Spacer(
                Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                    val radius = size.emblemRadius
                    val line = Stroke(0.75.dp.toPx())
                    onDrawBehind {
                        val spread = fastTurn() / 360f * RIPPLES_PER_TURN % 1f
                        repeat(RIPPLES) { ripple ->
                            // From the stone's edge out to the rim, fading in and out so none pops.
                            val out = (ripple + spread) / RIPPLES
                            val faded = scheme.primary.copy(alpha = 0.45f * sin(out * PI.toFloat()))
                            drawCircle(faded, radius * (0.5f + 0.42f * out), style = line)
                        }
                    }
                },
            )
            if (marked) Spacer(Modifier.fillMaxSize().stone { size.emblemRadius * 0.42f })
        }
    }

    /**
     * A raked path of four lines through the slots, parted round each slot by two ripples raked round it. The ripples
     * and the path's parting narrow as the ring crowds, so some path still shows between the closest slots.
     */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val line = Stroke(0.75.dp.toPx())
        val rake = listOf(-6, -2, 2, 6).map { radius + it.dp.toPx() }
        val gap = if (slots.size < 2) Float.MAX_VALUE else (slots[0] - slots[1]).getDistance() - iconSize
        val ripples = listOf(4.dp.toPx() to 0.12f, 8.dp.toPx() to 0.24f).map { (most, share) -> iconSize / 2 + minOf(most, gap * share) }
        val parting = iconSize / 2 + minOf(11.dp.toPx(), gap * 0.34f)
        val clear = Path().apply { slots.forEach { addOval(Rect(it, parting)) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                val path = colours.mark.copy(alpha = (0.25f + 0.4f * glow) * alpha)
                clipPath(clear, ClipOp.Difference) { rake.forEach { drawCircle(path, it, centre, style = line) } }
                val ripple = colours.starLine.copy(alpha = (colours.starLine.alpha * 0.7f + 0.3f * glow) * alpha)
                slots.forEach { slot ->
                    drawCircle(ripple, ripples[0], slot, style = line)
                    drawCircle(ripple.copy(alpha = ripple.alpha * 0.6f), ripples[1], slot, style = line)
                }
            }
        }
    }

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
            FolderLook.KoiPond -> Modifier.pond { style.minutes() } to Modifier.fillMaxSize().edge(GardenEdge)
            FolderLook.Moss -> Modifier.moss() to Modifier.fillMaxSize(PATCH_DISC)
            // Stones, and a look of another theme's met before the theme's own is picked.
            else -> Modifier.stone { size.minDimension / 2 * 1.08f } to Modifier.fillMaxSize(PATCH_DISC)
        }
        Box(modifier.then(worn), contentAlignment = Alignment.Center) { FolderDisc(folder, icon, presses, inner, disc) }
    }

    private val garden = DrawerTileArt { GardenTile(this, it) }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val faint = colour.copy(alpha = colour.alpha * 0.3f)
        forEachDrawerTile(scrolled) { origin, tile ->
            val built = with(garden) { of(tile) }
            translate(origin.x, origin.y) {
                drawPath(built.sand, faint, style = built.line)
                drawPath(built.stones, faint)
            }
        }
    }
}

/** A drawer tile of raked sand, the lines parted round its stones where their ripples reach, and the stones. */
private class GardenTile(density: Density, size: Size) {
    val line = Stroke(with(density) { 0.5.dp.toPx() })
    val sand = Path()
    val stones = Path()

    init {
        val placed = DrawerStones.map { Offset(it.x * size.width, it.y * size.height) to with(density) { it.reach.toPx() } }
        val gap = with(density) { 12.dp.toPx() }
        var y = gap / 2
        while (y < size.height) {
            var from = 0f
            placed
                .mapNotNull { (at, reach) ->
                    val clear = reach * 2.1f
                    val dy = y - at.y
                    if (dy * dy >= clear * clear) null else sqrt(clear * clear - dy * dy).let { at.x - it to at.x + it }
                }
                .sortedBy { it.first }
                .forEach { (start, end) ->
                    if (start > from) rakeLine(from, start, y)
                    from = maxOf(from, end)
                }
            if (size.width > from) rakeLine(from, size.width, y)
            y += gap
        }
        placed.forEach { (at, reach) ->
            listOf(1.3f, 1.6f, 1.9f).forEach { sand.addOval(Rect(at, reach * it)) }
            stones.addPath(wavering(0.4f, 2.2f, at, reach, lobes = 2))
        }
    }

    private fun rakeLine(from: Float, to: Float, y: Float) {
        sand.moveTo(from, y)
        sand.lineTo(to, y)
    }
}

/** A smooth grey stone round the item's centre, reaching [reach] from it, lit on top, with a pebble at its foot. */
private fun Modifier.stone(reach: CacheDrawScope.() -> Float): Modifier = drawWithCache {
    val radius = reach()
    val centre = size.center
    val body = wavering(0.4f, 2.2f, centre, radius, lobes = 2)
    val light = wavering(1.3f, 0.5f, centre + Offset(-0.22f, -0.25f) * radius, radius * 0.42f, lobes = 2)
    val pebble = centre + Offset(0.6f, 0.5f) * radius
    val line = 1.dp.toPx()
    onDrawBehind {
        drawPath(body, Stone)
        drawPath(light, StoneLight)
        drawCircle(Pebble, radius * 0.14f, pebble)
        drawPath(body, GardenEdge.outer, style = Stroke(line * 3))
        drawPath(body, GardenEdge.inner, style = Stroke(line))
    }
}

/** A pond filling the item, a lily pad on it and three koi swimming round as the [minutes] go by. */
private fun Modifier.pond(minutes: () -> Float): Modifier = drawWithCache {
    val half = size.minDimension / 2
    val centre = size.center
    val orbit = half * 0.6f
    val length = half * 0.18f
    val width = half * 0.065f
    // Swimming clockwise from the top, so facing right.
    val top = centre - Offset(0f, orbit)
    val fish = Path().apply {
        addOval(Rect(top.x - length, top.y - width, top.x + length, top.y + width))
        moveTo(top.x - length * 0.8f, top.y)
        lineTo(top.x - length * 1.5f, top.y - width * 1.4f)
        lineTo(top.x - length * 1.5f, top.y + width * 1.4f)
        close()
    }
    val pad = centre + Offset(0.3f, 0.35f) * half
    onDrawBehind {
        drawCircle(Pond, half, centre)
        drawCircle(LilyPad, half * 0.2f, pad)
        val swum = minutes() * 6f * KOI_LAPS_PER_HOUR
        Koi.forEachIndexed { index, colour ->
            rotate(swum + index * 130f, centre) { drawPath(fish, colour) }
        }
    }
}

/** A patch of moss reaching a little past the item, with tufts of lighter and darker moss round its edge. */
private fun Modifier.moss(): Modifier = drawWithCache {
    val half = size.minDimension / 2
    val centre = size.center
    val patch = wavering(2.1f, 0.9f, centre, half * 1.08f)
    val line = 1.dp.toPx()
    onDrawBehind {
        drawPath(patch, Moss)
        Tufts.forEach { (at, reach, light) -> drawCircle(if (light) MossLight else MossDark, half * reach, centre + at * half) }
        drawPath(patch, GardenEdge.outer, style = Stroke(line * 3))
        drawPath(patch, GardenEdge.inner, style = Stroke(line))
    }
}
