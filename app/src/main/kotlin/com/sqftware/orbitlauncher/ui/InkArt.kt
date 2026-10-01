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
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.InkEdge
import com.sqftware.orbitlauncher.ui.theme.InkHeart
import com.sqftware.orbitlauncher.ui.theme.InkWash
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.SealPaper
import com.sqftware.orbitlauncher.ui.theme.SealRed
import com.sqftware.orbitlauncher.ui.theme.SumiInk
import kotlin.math.PI
import kotlin.math.sin

/** How many times an ink drop bleeds out in the hour the folders' sky takes to turn once. */
private const val BLEEDS_PER_HOUR = 30

/** How much of a folder's square the disc of previews takes on an ensō and on a seal, which show round it. */
private const val ENSO_DISC = 0.8f
private const val SEAL_DISC = 0.74f

/** How far a seal is turned, as a hand presses it a little askew. */
private const val SEAL_TILT = -6f

/**
 * Sumi-e brushwork: the emblem is a red seal, the ring an ensō brushed in one stroke, folders are ensō, seals or ink
 * drops bleeding out, and the drawer is an ink-wash landscape.
 */
object InkArt : ThemeArt {
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val ground = MaterialTheme.colorScheme.surfaceContainerLowest
        val glyphs = rememberTextMeasurer()
        val lettering = MaterialTheme.typography.titleLarge
        Spacer(
            modifier.drawWithCache {
                val side = size.emblemRadius * 0.8f
                val inset = side * 0.08f
                val style = lettering.copy(fontSize = (side * 0.42f).toSp(), fontWeight = FontWeight.Black)
                val glyph = glyphs.measure("++", style)
                val at = size.center - Offset(glyph.size.width / 2f, glyph.size.height / 2f)
                onDrawBehind {
                    drawCircle(ground, size.emblemRadius)
                    if (marked) {
                        rotate(SEAL_TILT) {
                            seal(size.center, side, inset)
                            drawText(glyph, SealPaper, at)
                        }
                    }
                }
            },
        )
    }

    /** An ensō through the slots: one brush stroke pressed in at the top right, thinning as it goes round to a dry tail. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val stroke = brush(centre, radius, from = 30f, sweep = 318f, width = 4.dp.toPx())
        val dry = Stroke(1.dp.toPx())
        val dryAt = radius + 5.dp.toPx()
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                clipPath(discs, ClipOp.Difference) {
                    drawPath(stroke, colours.mark.copy(alpha = (0.35f + 0.3f * glow) * alpha))
                    val streak = colours.starLine.copy(alpha = (colours.starLine.alpha * 0.5f + 0.2f * glow) * alpha)
                    drawArc(streak, 30f + 40f - 90f, 150f, false, centre - Offset(dryAt, dryAt), Size(dryAt * 2, dryAt * 2), style = dry)
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
            FolderLook.Seal -> Modifier.sealed() to Modifier.fillMaxSize(SEAL_DISC)
            FolderLook.InkDrop -> Modifier.inkDrop { style.minutes() } to Modifier.fillMaxSize(ENSO_DISC).edge(InkEdge)
            // Ensō, and a look of another theme's met before the theme's own is picked.
            else -> Modifier.enso() to Modifier.fillMaxSize(ENSO_DISC)
        }
        Box(modifier.then(worn), contentAlignment = Alignment.Center) { FolderDisc(folder, icon, presses, inner, disc) }
    }

    private val landscape = DrawerTileArt { Landscape(this, it) }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        forEachDrawerTile(scrolled) { origin, tile ->
            val built = with(landscape) { of(tile) }
            translate(origin.x, origin.y) {
                drawCircle(colour.copy(alpha = colour.alpha * 0.12f), built.moonRadius, built.moon)
                built.ridges.forEachIndexed { far, ridge -> drawPath(ridge, colour.copy(alpha = colour.alpha * (0.2f - 0.05f * far))) }
            }
        }
    }
}

/**
 * A drawer tile's ink-wash landscape: a pale moon, and ranges of mountains from near to far, each a band of wash whose
 * foot wavers off into the paper, so the tile below begins in open sky with no seam.
 */
private class Landscape(density: Density, size: Size) {
    val moon = Offset(size.width * 0.76f, size.height * 0.2f)
    val moonRadius = with(density) { 46.dp.toPx() }
    val ridges = listOf(0.84f to 0.06f, 0.72f to 0.09f, 0.6f to 0.12f).mapIndexed { index, (base, height) ->
        ridge(size, base * size.height, height * size.height, depth = size.height * 0.1f, phase = index * 1.7f)
    }

    /** A range whose crests rise [height] above [base] and whose wash reaches [depth] below it into a wavering mist. */
    private fun ridge(size: Size, base: Float, height: Float, depth: Float, phase: Float) = Path().apply {
        val steps = 48
        // Whole waves across the tile, so a range meets itself where tiles sit side by side.
        fun crest(x: Float): Float {
            val across = x / size.width * 2 * PI.toFloat()
            return base - height * (0.55f * sin(across + phase) + 0.3f * sin(3 * across + phase * 2) + 0.15f * sin(7 * across + phase * 3) + 0.5f)
        }
        moveTo(0f, crest(0f))
        for (step in 1..steps) (size.width * step / steps).let { lineTo(it, crest(it)) }
        for (step in steps downTo 0) {
            val x = size.width * step / steps
            lineTo(x, base + depth * (0.8f + 0.2f * sin(x / size.width * 4 * PI.toFloat() + phase)))
        }
        close()
    }
}

/**
 * One brush stroke round [centre] at [radius], from [from] degrees clockwise from the top through [sweep], [width] wide
 * where it presses hardest: the brush presses in quickly, wavers a little, and lifts off slowly to a thin tail.
 */
private fun brush(centre: Offset, radius: Float, from: Float, sweep: Float, width: Float) = Path().apply {
    val steps = 120
    fun half(step: Int): Float {
        val along = step / steps.toFloat()
        val pressed = minOf(1f, along / 0.06f) * (1f - 0.8f * along)
        return width / 2 * pressed * (1f + 0.1f * sin(along * 23f))
    }
    fun at(step: Int, side: Float) = centre + direction(from + sweep * step / steps) * (radius + side * half(step))
    at(0, 1f).let { moveTo(it.x, it.y) }
    for (step in 1..steps) at(step, 1f).let { lineTo(it.x, it.y) }
    for (step in steps downTo 0) at(step, -1f).let { lineTo(it.x, it.y) }
    close()
}

/** A seal [side] across round [centre]: vermilion, edged to show on any wallpaper, with a carved paper border [inset] inside. */
private fun DrawScope.seal(centre: Offset, side: Float, inset: Float) {
    val corner = CornerRadius(side * 0.1f)
    val outer = Rect(centre, side / 2)
    val line = 1.dp.toPx()
    drawRoundRect(SealRed, outer.topLeft, outer.size, corner)
    drawRoundRect(InkEdge.outer, outer.topLeft, outer.size, corner, style = Stroke(line * 3))
    drawRoundRect(InkEdge.inner, outer.topLeft, outer.size, corner, style = Stroke(line))
    val carved = outer.deflate(inset)
    drawRoundRect(SealPaper, carved.topLeft, carved.size, CornerRadius(side * 0.06f), style = Stroke(line * 1.5f))
}

/**
 * An ensō round the item: a grey wash brushed round in one ink stroke, the wash edged in two lines and the stroke in
 * paper, so they show on any wallpaper.
 */
private fun Modifier.enso(): Modifier = drawWithCache {
    val half = size.minDimension / 2
    val centre = size.center
    val stroke = brush(centre, half * 0.94f, from = 200f, sweep = 330f, width = half * 0.16f)
    val line = Stroke(1.dp.toPx())
    val edge = Stroke(3.dp.toPx())
    val outline = Stroke(1.5.dp.toPx())
    onDrawBehind {
        drawCircle(InkWash, half * 0.9f, centre)
        drawCircle(InkEdge.outer, half * 0.9f, centre, style = edge)
        drawCircle(InkEdge.inner, half * 0.9f, centre, style = line)
        drawPath(stroke, InkEdge.inner, style = outline)
        drawPath(stroke, SumiInk)
    }
}

/** A seal filling the item, pressed a little askew, its previews in the carved square. */
private fun Modifier.sealed(): Modifier = drawWithCache {
    val side = size.minDimension * 0.96f
    val inset = side * 0.06f
    onDrawBehind { rotate(SEAL_TILT) { seal(size.center, side, inset) } }
}

/**
 * An ink drop in the paper round the item, bleeding out as the [minutes] go by: a dark heart, and rings of paler ink
 * spreading from it, fading in and out so none pops, with two spatters beside it.
 */
private fun Modifier.inkDrop(minutes: () -> Float): Modifier = drawWithCache {
    val half = size.minDimension / 2
    val centre = size.center
    val heart = wavering(1.1f, 2.7f, centre, half * 0.7f)
    // Unit blots, scaled each frame to how far each ring has spread.
    val rings = listOf(wavering(0.3f, 1.4f), wavering(2.2f, 0.6f), wavering(4.0f, 3.1f))
    val spatters = listOf(Offset(-0.84f, -0.7f) to 0.07f, Offset(-0.8f, 0.78f) to 0.05f)
    onDrawBehind {
        val bled = minutes() * BLEEDS_PER_HOUR / 60f
        rings.forEachIndexed { index, ring ->
            val spread = (bled + index / rings.size.toFloat()) % 1f
            withTransform({
                translate(centre.x, centre.y)
                scale(half * (0.7f + 0.38f * spread), half * (0.7f + 0.38f * spread), Offset.Zero)
            }) { drawPath(ring, InkWash.copy(alpha = InkWash.alpha * 0.6f * sin(spread * PI.toFloat()))) }
        }
        drawPath(heart, InkHeart)
        spatters.forEach { (at, reach) -> drawCircle(SumiInk, half * reach, centre + at * half) }
    }
}
