package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.DialEdge
import com.sqftware.orbitlauncher.ui.theme.GearCog
import com.sqftware.orbitlauncher.ui.theme.RingColors
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/** Where the small seconds dial sits below the emblem's centre, and how large it is, as shares of the emblem's radius. */
private const val SMALL_SECONDS_AT = 0.42f
private const val SMALL_SECONDS = 0.17f

/** How many circles, turned round the centre, make an engine-turned rosette. */
private const val ROSETTE_CIRCLES = 24

/** How much of a gear folder's square its disc of previews takes, the teeth the rest. */
private const val GEAR_DISC = 0.8f
private const val GEAR_TEETH = 12

/** How many times a gear turns in the hour the folders' sky takes to turn once: a whole number, so it never jumps. */
private const val GEAR_TURNS_PER_HOUR = 2

// The drawer's rosettes repeat in a tile a phone's screen fits in, as the space theme's stars do, at a fraction of the pace.
private val TILE_WIDTH = 480.dp
private val TILE_HEIGHT = 960.dp
private const val PARALLAX = 0.25f

/** Where each of the drawer's rosettes sits in its tile, as shares of it, and how far it reaches. */
private val DrawerRosettes = listOf(Triple(0.18f, 0.2f, 150.dp), Triple(0.86f, 0.68f, 190.dp))

/**
 * An old pocket watch: the emblem is its dial telling the time, with a small seconds hand that sweeps while the ring
 * turns; the ring is a chapter ring of minute marks; folders are sub-dials or gears; and engine-turned rosettes are cut
 * into the drawer.
 */
object ClockworkArt : ThemeArt {
    override val hintInk @Composable get() = MaterialTheme.colorScheme.onSurface
    override val hintShade @Composable get() = MaterialTheme.colorScheme.inverseOnSurface

    /** The dial follows the scheme: walnut and brass by night, porcelain inked in walnut by day. */
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: Int, modifier: Modifier) {
        val dial = DialColors(MaterialTheme.colorScheme)
        Box(modifier) {
            Spacer(Modifier.fillMaxSize().drawWithCache { dial(dial, marked) })
            if (marked) {
                Spacer(Modifier.fillMaxSize().drawBehind { drawHands(dial, minuteOfDay) })
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0.5f + SMALL_SECONDS_AT * size.emblemRadius / size.height)
                            rotationZ = fastTurn()
                        }
                        .drawWithCache {
                            val radius = size.emblemRadius
                            val hub = size.center + Offset(0f, radius * SMALL_SECONDS_AT)
                            val hand = 1.5.dp.toPx()
                            onDrawBehind {
                                drawLine(dial.seconds, hub, hub - Offset(0f, radius * SMALL_SECONDS * 0.9f), hand, StrokeCap.Round)
                                drawCircle(dial.seconds, hand, hub)
                            }
                        },
                )
            }
        }
    }

    /** A chapter ring: a track through the slots crossed by sixty minute marks, longer every fifth, clear of the discs. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val track = Stroke(1.dp.toPx())
        val minor = Stroke(1.dp.toPx())
        val major = Stroke(2.dp.toPx())
        val minutes = Path()
        val fives = Path()
        repeat(60) { minute ->
            val reach = (if (minute % 5 == 0) 6.dp else 2.5.dp).toPx()
            val along = direction(minute * 6f)
            (if (minute % 5 == 0) fives else minutes).apply {
                moveTo(centre + along * (radius - reach))
                lineTo(centre + along * (radius + reach))
            }
        }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                clipPath(discs, ClipOp.Difference) {
                    drawCircle(colours.mark.copy(alpha = (0.3f + 0.45f * glow) * alpha), radius, centre, style = track)
                    val tick = colours.starLine.copy(alpha = (colours.starLine.alpha + 0.3f * glow) * alpha)
                    drawPath(minutes, tick.copy(alpha = tick.alpha * 0.6f), style = minor)
                    drawPath(fives, tick, style = major)
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
        val gear = style.look == FolderLook.Gear
        // A layer of its own, so a turning gear redraws only itself, not the page round it.
        Box(modifier.graphicsLayer(), contentAlignment = Alignment.Center) {
            if (gear) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = style.minutes() * 6f * GEAR_TURNS_PER_HOUR }
                        .drawWithCache {
                            val cog = cogPath(size.center, size.minDimension / 2 - 1.dp.toPx(), size.minDimension / 2 * GEAR_DISC)
                            val outline = Stroke(1.dp.toPx())
                            onDrawBehind {
                                drawPath(cog, GearCog.inner)
                                drawPath(cog, GearCog.outer, style = outline)
                            }
                        },
                )
            }
            val face = if (gear) Modifier.fillMaxSize(GEAR_DISC * 0.94f) else Modifier.fillMaxSize().subDialMarks()
            IconDisc(presses, face.edge(DialEdge), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = inner() }, contentAlignment = Alignment.Center) {
                    FolderPreviews(folder, icon)
                }
            }
        }
    }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val tileWidth = TILE_WIDTH.toPx()
        val tileHeight = TILE_HEIGHT.toPx()
        val top = -(scrolled * PARALLAX).mod(tileHeight)
        val line = Stroke(0.5.dp.toPx())
        repeat(ceil(size.width / tileWidth).toInt()) { column ->
            repeat(ceil((size.height - top) / tileHeight).toInt()) { row ->
                translate(column * tileWidth, top + row * tileHeight) {
                    DrawerRosettes.forEach { (x, y, reach) ->
                        rosette(Offset(x * tileWidth, y * tileHeight), reach.toPx(), colour.copy(alpha = colour.alpha * 0.3f), line)
                    }
                }
            }
        }
    }
}

private fun direction(degrees: Float): Offset {
    val angle = degrees / 180f * PI.toFloat()
    return Offset(sin(angle), -cos(angle))
}

private fun Path.moveTo(at: Offset) = moveTo(at.x, at.y)

private fun Path.lineTo(at: Offset) = lineTo(at.x, at.y)

/** Circles of [reach] turned round [centre], overlapping into the rosette a watchmaker's engine cuts into a dial. */
private fun DrawScope.rosette(centre: Offset, reach: Float, colour: Color, line: Stroke) {
    val circle = reach * 0.7f
    repeat(ROSETTE_CIRCLES) { index ->
        drawCircle(colour, circle, centre + direction(index * 360f / ROSETTE_CIRCLES) * (reach - circle), style = line)
    }
}

/**
 * What the dial is drawn in, from the scheme so it turns over with the wallpaper: its [face] the see-through ground that
 * floats below the opaque containers, [rule]s cut faintly in the accent, [brass] hour marks, [hand]s in the content
 * colour on a [shade] of the face's own, and the [seconds] hand in the second accent.
 */
private class DialColors(scheme: ColorScheme) {
    val face = scheme.surfaceContainerLowest
    val rule = scheme.primary.copy(alpha = 0.35f)
    val brass = scheme.primary
    val hand = scheme.onSurface
    val shade = scheme.inverseOnSurface
    val seconds = scheme.tertiary
}

/** The watch's dial: its face, a rosette cut in it, its minute marks and brass hour marks, and the small seconds dial. */
private fun CacheDrawScope.dial(colours: DialColors, marked: Boolean): DrawResult {
    val radius = size.emblemRadius
    val centre = size.center
    val rule = Stroke(0.75.dp.toPx())
    val minutes = Path()
    val hours = Path()
    repeat(60) { minute ->
        val along = direction(minute * 6f)
        (if (minute % 5 == 0) hours else minutes).apply {
            moveTo(centre + along * radius * (if (minute % 5 == 0) 0.74f else 0.8f))
            lineTo(centre + along * radius * 0.86f)
        }
    }
    val hourMark = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
    val smallSeconds = centre + Offset(0f, radius * SMALL_SECONDS_AT)
    return onDrawBehind {
        drawCircle(colours.face, radius, centre)
        rosette(centre, radius * 0.62f, colours.rule, rule)
        drawCircle(colours.rule, radius * 0.86f, centre, style = rule)
        drawPath(minutes, colours.rule, style = rule)
        if (marked) {
            drawCircle(colours.face, radius * SMALL_SECONDS, smallSeconds)
            drawCircle(colours.rule, radius * SMALL_SECONDS, smallSeconds, style = rule)
        }
        drawPath(hours, colours.brass, style = hourMark)
    }
}

/** The hour and minute hands at [minuteOfDay], on a shadow of the face's colour so they read over the rosette. */
private fun DrawScope.drawHands(colours: DialColors, minuteOfDay: Int) {
    val radius = size.emblemRadius
    val centre = size.center
    fun hand(degrees: Float, length: Float, width: Float) = rotate(degrees, centre) {
        val tail = centre + Offset(0f, radius * 0.12f)
        val tip = centre - Offset(0f, radius * length)
        drawLine(colours.shade, tail, tip, width + 2.dp.toPx(), StrokeCap.Round)
        drawLine(colours.hand, tail, tip, width, StrokeCap.Round)
    }
    hand(minuteOfDay % 720 / 2f, 0.46f, 3.5.dp.toPx())
    hand(minuteOfDay % 60 * 6f, 0.7f, 2.dp.toPx())
    drawCircle(colours.brass, 3.5.dp.toPx(), centre)
}

/** Twelve marks round the inside of a sub-dial's rim, as a watch's smaller dials have. */
private fun Modifier.subDialMarks(): Modifier = drawWithContent {
    drawContent()
    val radius = size.minDimension / 2
    val mark = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
    val marks = Path()
    repeat(12) { hour ->
        val along = direction(hour * 30f)
        marks.moveTo(center + along * (radius - 3.5.dp.toPx()))
        marks.lineTo(center + along * (radius - (if (hour % 3 == 0) 8.dp else 5.5.dp).toPx()))
    }
    drawPath(marks, DialEdge.inner, style = mark)
}

/** A cog's outline round [centre]: [GEAR_TEETH] flat-topped teeth out to [outer], from a rim at [root]. */
private fun cogPath(centre: Offset, outer: Float, root: Float): Path = Path().apply {
    val step = 360f / GEAR_TEETH
    repeat(GEAR_TEETH) { tooth ->
        val start = tooth * step
        listOf(
            start to root,
            start + step * 0.12f to outer,
            start + step * 0.42f to outer,
            start + step * 0.54f to root,
        ).forEachIndexed { index, (degrees, reach) ->
            val at = centre + direction(degrees) * reach
            if (tooth == 0 && index == 0) moveTo(at) else lineTo(at)
        }
    }
    close()
}
