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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.EnvelopeFold
import com.sqftware.orbitlauncher.ui.theme.EnvelopePaper
import com.sqftware.orbitlauncher.ui.theme.FoldShadow
import com.sqftware.orbitlauncher.ui.theme.PaperDark
import com.sqftware.orbitlauncher.ui.theme.PaperEdge
import com.sqftware.orbitlauncher.ui.theme.PaperLight
import com.sqftware.orbitlauncher.ui.theme.PaperMid
import com.sqftware.orbitlauncher.ui.theme.RingColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** How many times a pinwheel spins round in the hour the folders' sky takes to turn once. */
private const val SPINS_PER_HOUR = 60

/** How much of a folder's square the disc of previews takes on each fold, which shows round it. */
private const val SQUARE_DISC = 0.7f
private const val ENVELOPE_DISC = 0.72f
private const val PINWHEEL_DISC = 0.7f

/** How many lengths of tape go round the ring, laid lighter and heavier by turns. */
private const val WASHI_PIECES = 24

/** How many times a minute the crane on the emblem beats its wings. */
private const val FLAPS_PER_TURN = 15

/** A paper crane facing left, in unit lengths about its middle: each fold's corners, and how much light it catches. */
private val Crane = listOf(
    listOf(Offset(-0.28f, 0.02f), Offset(-0.32f, -0.75f), Offset(0.14f, -0.02f)) to PaperLight,
    listOf(Offset(0.28f, 0.04f), Offset(0.92f, -0.3f), Offset(0.1f, 0.18f)) to PaperLight,
    listOf(Offset(-0.3f, 0.06f), Offset(-0.88f, -0.28f), Offset(-0.1f, 0.16f)) to PaperLight,
    listOf(Offset(-0.88f, -0.28f), Offset(-0.98f, -0.16f), Offset(-0.78f, -0.2f)) to PaperDark,
    listOf(Offset(-0.35f, 0.05f), Offset(0f, -0.05f), Offset(0f, 0.32f)) to PaperMid,
    listOf(Offset(0f, -0.05f), Offset(0.35f, 0.05f), Offset(0f, 0.32f)) to PaperDark,
    listOf(Offset(-0.02f, 0f), Offset(0.4f, -0.78f), Offset(0.3f, 0.04f)) to PaperMid,
)

/** Which of the crane's folds are its wings, the far one and the near one, each with its tip second. */
private val CraneWings = setOf(0, 6)

/**
 * Origami: the emblem is a paper crane beating its wings, the ring a length of washi tape, folders are folded squares,
 * envelopes or pinwheels spinning in a breeze, and the drawer is paper creased on the diagonal.
 */
object PaperArt : ThemeArt {
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val ground = MaterialTheme.colorScheme.surfaceContainerLowest
        Spacer(
            modifier.graphicsLayer().drawWithCache {
                val reach = size.emblemRadius * 0.7f
                // The crane runs higher than it runs low, so it is lowered to sit in the middle.
                val middle = size.center + Offset(0f, reach * 0.23f)
                val crease = Stroke(1.dp.toPx(), join = StrokeJoin.Round)
                val folds = Crane.map { (corners, light) -> polygon(middle, reach, corners) to light }
                onDrawBehind {
                    drawCircle(ground, size.emblemRadius)
                    if (marked) {
                        val beat = cycles(fastTurn(), FLAPS_PER_TURN) * 2 * PI.toFloat()
                        // Its wings from raised to nearly level, the body lifting on each downstroke.
                        val raised = 0.6f + 0.4f * cos(beat)
                        CraneWings.forEach { wing ->
                            folds[wing].first.apply { rewind(); addPolygon(middle, reach, Crane[wing].first, raised) }
                        }
                        translate(top = -reach * 0.06f * sin(beat)) {
                            folds.forEach { (fold, light) ->
                                drawPath(fold, light)
                                drawPath(fold, PaperDark, style = crease)
                            }
                        }
                    }
                }
            },
        )
    }

    /** A length of washi tape through the slots, its pieces laid lighter and heavier by turns, kept clear of the slots' discs. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val band = Stroke(4.dp.toPx())
        val oval = Rect(centre, radius)
        val piece = 360f / WASHI_PIECES
        val heavy = Path()
        val light = Path()
        repeat(WASHI_PIECES) { (if (it % 2 == 0) heavy else light).addArc(oval, it * piece, piece) }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                val laid = (0.3f + 0.3f * glow) * alpha
                clipPath(discs, ClipOp.Difference) {
                    drawPath(heavy, colours.mark.copy(alpha = laid), style = band)
                    drawPath(light, colours.mark.copy(alpha = laid * 0.5f), style = band)
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
            FolderLook.Envelope -> Modifier.envelope() to Modifier.fillMaxSize(ENVELOPE_DISC)
            FolderLook.Pinwheel -> Modifier.pinwheel { style.minutes() } to Modifier.fillMaxSize(PINWHEEL_DISC)
            // Folded squares, and a look of another theme's met before the theme's own is picked.
            else -> Modifier.foldedSquare() to Modifier.fillMaxSize(SQUARE_DISC)
        }
        Box(modifier.then(worn), contentAlignment = Alignment.Center) { FolderDisc(folder, icon, presses, inner, disc) }
    }

    private val creases = DrawerTileArt { Creases(this, it) }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val faint = colour.copy(alpha = colour.alpha * 0.25f)
        forEachDrawerTile(scrolled) { origin, tile ->
            val built = with(creases) { of(tile) }
            translate(origin.x, origin.y) { drawPath(built.lines, faint, style = built.line) }
        }
    }
}

/**
 * A drawer tile's creases: lines on the diagonal, a whole number of gaps across and down the tile so they run on
 * unbroken where tiles meet, each cut at the tile's edges so none is drawn twice.
 */
private class Creases(density: Density, size: Size) {
    val line = Stroke(with(density) { 0.75.dp.toPx() })
    val lines = Path().apply {
        val gap = size.width / 12
        var across = -size.height
        while (across < size.width) {
            val top = maxOf(0f, -across)
            val bottom = minOf(size.height, size.width - across)
            if (bottom > top) {
                moveTo(top + across, top)
                lineTo(bottom + across, bottom)
            }
            across += gap
        }
    }
}

/** A closed path through [corners], given in unit lengths about [centre] and scaled by [reach]. */
private fun polygon(centre: Offset, reach: Float, corners: List<Offset>) = Path().apply { addPolygon(centre, reach, corners) }

/** [corners] round [centre] at [reach], the second of them, a wing's tip, [raised] that share of its height. */
private fun Path.addPolygon(centre: Offset, reach: Float, corners: List<Offset>, raised: Float = 1f) {
    corners.forEachIndexed { index, corner ->
        val at = centre + Offset(corner.x, if (index == 1) corner.y * raised else corner.y) * reach
        if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
    }
    close()
}

/** The two lines a folded paper is edged in, so it shows on any patch of the wallpaper. */
private class Edging(density: Density) {
    val outer = Stroke(with(density) { 3.dp.toPx() })
    val inner = Stroke(with(density) { 1.dp.toPx() })
}

private fun DrawScope.edged(shape: Path, edging: Edging) {
    drawPath(shape, PaperEdge.outer, style = edging.outer)
    drawPath(shape, PaperEdge.inner, style = edging.inner)
}

/** A square on its point, reaching a little past the item, its top point folded down in front. */
private fun Modifier.foldedSquare(): Modifier = drawWithCache {
    val reach = size.minDimension / 2 * 1.2f
    val centre = size.center
    val creaseLeft = Offset(-0.3f, -0.7f)
    val creaseRight = Offset(0.3f, -0.7f)
    val square = polygon(centre, reach, listOf(creaseLeft, creaseRight, Offset(1f, 0f), Offset(0f, 1f), Offset(-1f, 0f)))
    val flap = polygon(centre, reach, listOf(creaseLeft, creaseRight, Offset(0f, -0.4f)))
    val cast = 2.dp.toPx()
    val edging = Edging(this)
    onDrawBehind {
        translate(cast, cast) { drawPath(square, FoldShadow) }
        drawPath(square, PaperLight)
        drawPath(flap, PaperMid)
        edged(square, edging)
        edged(flap, edging)
    }
}

/** An envelope wider than the item, its flap down in coral and its side folds shaded. */
private fun Modifier.envelope(): Modifier = drawWithCache {
    val side = size.minDimension
    val centre = size.center
    val half = Offset(side * 0.6f, side * 0.42f)
    val body = Rect(centre - half, centre + half)
    val outline = Path().apply { addRect(body) }
    val folds = Path().apply {
        moveTo(body.left, body.top)
        lineTo(centre.x, centre.y + half.y * 0.15f)
        lineTo(body.left, body.bottom)
        close()
        moveTo(body.right, body.top)
        lineTo(centre.x, centre.y + half.y * 0.15f)
        lineTo(body.right, body.bottom)
        close()
    }
    val flap = Path().apply {
        moveTo(body.left, body.top)
        lineTo(body.right, body.top)
        lineTo(centre.x, body.top + half.y * 1.1f)
        close()
    }
    val cast = 3.dp.toPx()
    val edging = Edging(this)
    onDrawBehind {
        translate(cast, cast) { drawPath(outline, FoldShadow) }
        drawPath(outline, EnvelopePaper)
        drawPath(folds, EnvelopeFold)
        drawPath(flap, PaperMid)
        edged(outline, edging)
    }
}

/**
 * A pinwheel reaching well past the item, its four blades coral and cream, spinning as the [minutes] go by. Its edges
 * stop at the disc of previews, so they do not turn behind them.
 */
private fun Modifier.pinwheel(minutes: () -> Float): Modifier = drawWithCache {
    // Just short of the dock's spacing, so two pinwheels side by side never cross.
    val reach = size.minDimension / 2 * 1.24f
    val centre = size.center
    val coral = Path()
    val cream = Path()
    val blades = Path()
    repeat(4) { index ->
        val tip = direction(index * 90f)
        val heel = direction(index * 90f + 80f) * 0.62f
        val blade = polygon(centre, reach, listOf(Offset.Zero, tip, heel))
        (if (index % 2 == 0) coral else cream).addPath(blade)
        blades.addPath(blade)
    }
    val disc = Path().apply { addOval(Rect(centre, size.minDimension / 2 * PINWHEEL_DISC)) }
    val edging = Edging(this)
    onDrawBehind {
        rotate(minutes() * 6f * SPINS_PER_HOUR) {
            drawPath(coral, PaperMid)
            drawPath(cream, EnvelopePaper)
            clipPath(disc, ClipOp.Difference) { edged(blades, edging) }
        }
    }
}
