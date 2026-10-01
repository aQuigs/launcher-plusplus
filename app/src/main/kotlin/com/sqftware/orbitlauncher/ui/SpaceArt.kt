package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.R
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.RingInk
import com.sqftware.orbitlauncher.ui.theme.RingShade
import com.sqftware.orbitlauncher.ui.theme.RingSpark

/** The launcher icon's sky, 108 wide, at 2.475 times the emblem's radius, which puts its dust between spark and edge. */
private const val EMBLEM_ART_PER_RADIUS = 2.475f
private const val EMBLEM_SPARK = 0.3f

/** Fewer items make a point, a line or a triangle whose edges cut across the emblem, so they keep a circle. */
private const val MIN_CONSTELLATION = 4

/**
 * The launcher icon writ large: the ring's items are the stars of its constellation round the icon's night sky and spark,
 * folders are planets, and stars are scattered over the drawer.
 */
object SpaceArt : ThemeArt {
    override val hintInk @Composable get() = RingInk
    override val hintShade @Composable get() = RingShade

    /** The icon's night sky, half see-through so it darkens a bright wallpaper without hiding it, with its spark on top. */
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val sky = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_launcher_background))

        // Sky and spark each on a layer of their own, so turning them changes a property of the layer and nothing is drawn
        // again.
        Box(modifier) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = slowTurn() }
                    .drawWithCache {
                        val outer = size.emblemRadius
                        val disc = Path().apply { addOval(Rect(size.center, outer)) }
                        val side = outer * EMBLEM_ART_PER_RADIUS
                        val art = Size(side, side)
                        val inset = (size.minDimension - side) / 2
                        onDrawBehind { clipPath(disc) { translate(inset, inset) { with(sky) { draw(art, alpha = 0.5f) } } } }
                    },
            )
            if (marked) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = fastTurn() }
                        .drawWithCache {
                            val spark = sparkPath(size.center, size.emblemRadius * EMBLEM_SPARK)
                            onDrawBehind { drawPath(spark, RingSpark) }
                        },
                )
            }
        }
    }

    /** The lines joining the slots, stopped at the edges of their discs, like the icon's constellation; a circle for a few. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        if (slots.size < MIN_CONSTELLATION) {
            val track = Stroke(1.dp.toPx())
            return { glow, alpha ->
                if (alpha > 0f) drawCircle(colours.mark.copy(alpha = (0.22f + 0.48f * glow) * alpha), radius, centre, style = track)
            }
        }
        val lineStroke = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round)
        val lines = Path().apply {
            slots.forEachIndexed { index, star -> if (index == 0) moveTo(star.x, star.y) else lineTo(star.x, star.y) }
            close()
        }
        // Glass the wallpaper shows through.
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2)) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                clipPath(discs, ClipOp.Difference) {
                    drawPath(lines, colours.starLine.copy(alpha = (colours.starLine.alpha + 0.3f * glow) * alpha), style = lineStroke)
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
    ) = PlanetFace(folder, icon, modifier, presses, inner)

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) = drawStarField(colour, scrolled)
}

/** The launcher icon's four-point spark, [half] from its [centre] to each tip, its sides bowed in as on the icon. */
private fun sparkPath(centre: Offset, half: Float): Path {
    val bow = half * 0.22f
    val (x, y) = centre
    return Path().apply {
        moveTo(x, y - half)
        quadraticTo(x + bow, y - bow, x + half, y)
        quadraticTo(x + bow, y + bow, x, y + half)
        quadraticTo(x - bow, y + bow, x - half, y)
        quadraticTo(x - bow, y - bow, x, y - half)
        close()
    }
}
