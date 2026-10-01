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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.hourHandDegrees
import com.sqftware.orbitlauncher.domain.minuteHandDegrees
import com.sqftware.orbitlauncher.ui.theme.DialEdge
import com.sqftware.orbitlauncher.ui.theme.GearCog
import com.sqftware.orbitlauncher.ui.theme.RingColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Where the opening onto the watch's wheel sits below the emblem's centre, and how large it is, as shares of its radius. */
private const val WHEEL_AT = 0.42f
private const val WHEEL = 0.17f

/** How many circles, turned round the centre, make an engine-turned rosette. */
private const val ROSETTE_CIRCLES = 24

/** How much of a gear folder's square its disc of previews takes, and how far its teeth reach, past the square as moons do. */
private const val GEAR_DISC = 0.88f
private const val GEAR_ROOT = 0.92f
private const val GEAR_TIP = 1.06f
private const val GEAR_TEETH = 12

/** How many times a gear turns in the hour the folders' sky takes to turn once: a whole number, so it never jumps. */
private const val GEAR_TURNS_PER_HOUR = 2

/** Where each of the drawer's rosettes sits in its tile, as shares of it, and how far it reaches. */
private val DrawerRosettes = listOf(Triple(0.18f, 0.2f, 150.dp), Triple(0.86f, 0.68f, 190.dp))

/**
 * An old pocket watch: the emblem is its dial telling the time, with a wheel of the movement turning in an opening
 * below the hands while the ring turns; the ring is a chapter ring of minute marks; folders are sub-dials or gears; and
 * engine-turned rosettes are cut into the drawer.
 */
object ClockworkArt : ThemeArt {
    override val hintInk @Composable get() = MaterialTheme.colorScheme.onSurface
    override val hintShade @Composable get() = MaterialTheme.colorScheme.inverseOnSurface

    /** The dial follows the scheme: walnut and brass by night, porcelain inked in walnut by day. */
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val scheme = MaterialTheme.colorScheme
        // Each part on a layer of its own, so the ring's glow, the wheel turning and the hands moving each redraw only it.
        Box(modifier) {
            Spacer(Modifier.fillMaxSize().graphicsLayer().drawWithCache { dial(scheme, marked) })
            if (marked) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0.5f + WHEEL_AT * size.emblemRadius / size.height)
                            rotationZ = fastTurn()
                        }
                        .drawWithCache {
                            val radius = size.emblemRadius
                            val hub = size.center + Offset(0f, radius * WHEEL_AT)
                            val rim = radius * WHEEL * 0.8f
                            val line = Stroke(1.5.dp.toPx())
                            val spokes = ticks(hub, 6, -rim, rim) { it < 3 }
                            onDrawBehind {
                                drawCircle(scheme.tertiary, rim, hub, style = line)
                                drawPath(spokes, scheme.tertiary, style = line)
                            }
                        },
                )
                Spacer(Modifier.fillMaxSize().graphicsLayer().drawBehind { drawHands(scheme, minuteOfDay()) })
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
        val minor = Stroke(1.dp.toPx())
        val major = Stroke(2.dp.toPx())
        val short = 2.5.dp.toPx()
        val long = 6.dp.toPx()
        val minutes = ticks(centre, 60, radius - short, radius + short) { it % 5 != 0 }
        val fives = ticks(centre, 60, radius - long, radius + long) { it % 5 == 0 }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                clipPath(discs, ClipOp.Difference) {
                    drawCircle(colours.mark.copy(alpha = (0.3f + 0.45f * glow) * alpha), radius, centre, style = minor)
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
        Box(modifier, contentAlignment = Alignment.Center) {
            if (gear) {
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = style.minutes() * 6f * GEAR_TURNS_PER_HOUR }
                        .drawWithCache {
                            val cog = cogPath(size.center, size.minDimension / 2 * GEAR_TIP, size.minDimension / 2 * GEAR_ROOT)
                            val outline = Stroke(1.dp.toPx())
                            onDrawBehind {
                                drawPath(cog, GearCog.inner)
                                drawPath(cog, GearCog.outer, style = outline)
                            }
                        },
                )
            }
            val face = if (gear) Modifier.fillMaxSize(GEAR_DISC) else Modifier.fillMaxSize().subDialMarks()
            FolderDisc(folder, icon, presses, inner, face.edge(DialEdge))
        }
    }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val faint = colour.copy(alpha = colour.alpha * 0.3f)
        val line = Stroke(0.5.dp.toPx())
        forEachDrawerTile(scrolled) { origin, tile ->
            DrawerRosettes.forEach { (x, y, reach) ->
                rosette(origin + Offset(x * tile.width, y * tile.height), reach.toPx(), faint, line)
            }
        }
    }
}

/** The unit offset [degrees] round from the top, clockwise, in screen axes. */
private fun direction(degrees: Float): Offset {
    val angle = degrees / 180f * PI.toFloat()
    return Offset(sin(angle), -cos(angle))
}

/** Radial marks round [centre] from [from] to [to] out, at those of [count] even steps [kept] by their index. */
private fun ticks(centre: Offset, count: Int, from: Float, to: Float, kept: (Int) -> Boolean): Path = Path().apply {
    for (index in 0 until count) {
        if (!kept(index)) continue
        val along = direction(index * 360f / count)
        (centre + along * from).let { moveTo(it.x, it.y) }
        (centre + along * to).let { lineTo(it.x, it.y) }
    }
}

/** Circles of [reach] turned round [centre], overlapping into the rosette a watchmaker's engine cuts into a dial. */
private fun DrawScope.rosette(centre: Offset, reach: Float, colour: Color, line: Stroke) {
    val circle = reach * 0.7f
    repeat(ROSETTE_CIRCLES) { index ->
        drawCircle(colour, circle, centre + direction(index * 360f / ROSETTE_CIRCLES) * (reach - circle), style = line)
    }
}

/**
 * The watch's dial, in the scheme's roles so it turns over with the wallpaper: a face of the see-through ground that
 * floats below the opaque containers, a rosette and minute marks cut faintly in the accent, accent hour marks, and the
 * opening onto the wheel.
 */
private fun CacheDrawScope.dial(scheme: ColorScheme, marked: Boolean): DrawResult {
    val radius = size.emblemRadius
    val centre = size.center
    val face = scheme.surfaceContainerLowest
    val cut = scheme.primary.copy(alpha = 0.35f)
    val rule = Stroke(0.75.dp.toPx())
    val minutes = ticks(centre, 60, radius * 0.8f, radius * 0.86f) { it % 5 != 0 }
    val hours = ticks(centre, 12, radius * 0.74f, radius * 0.86f) { true }
    val hourMark = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
    val opening = centre + Offset(0f, radius * WHEEL_AT)
    return onDrawBehind {
        drawCircle(face, radius, centre)
        rosette(centre, radius * 0.62f, cut, rule)
        drawCircle(cut, radius * 0.86f, centre, style = rule)
        drawPath(minutes, cut, style = rule)
        if (marked) {
            drawCircle(face, radius * WHEEL, opening)
            drawCircle(cut, radius * WHEEL, opening, style = rule)
        }
        drawPath(hours, scheme.primary, style = hourMark)
    }
}

/** The hour and minute hands at [minuteOfDay], in the content colour on a shadow of the face's, so they read over the rosette. */
private fun DrawScope.drawHands(scheme: ColorScheme, minuteOfDay: Int) {
    val radius = size.emblemRadius
    val centre = size.center
    fun hand(degrees: Float, length: Float, width: Float) = rotate(degrees, centre) {
        val tail = centre + Offset(0f, radius * 0.12f)
        val tip = centre - Offset(0f, radius * length)
        drawLine(scheme.inverseOnSurface, tail, tip, width + 2.dp.toPx(), StrokeCap.Round)
        drawLine(scheme.onSurface, tail, tip, width, StrokeCap.Round)
    }
    hand(hourHandDegrees(minuteOfDay), 0.46f, 3.5.dp.toPx())
    hand(minuteHandDegrees(minuteOfDay), 0.7f, 2.dp.toPx())
    drawCircle(scheme.primary, 3.5.dp.toPx(), centre)
}

/** Twelve marks round the inside of a sub-dial's rim, longer at the quarters, as a watch's smaller dials have. */
private fun Modifier.subDialMarks(): Modifier = drawWithCache {
    val rim = size.minDimension / 2 - 3.5.dp.toPx()
    val mark = Stroke(1.dp.toPx(), cap = StrokeCap.Round)
    val marks = ticks(size.center, 12, rim - 2.dp.toPx(), rim) { it % 3 != 0 }
        .apply { addPath(ticks(size.center, 12, rim - 4.5.dp.toPx(), rim) { it % 3 == 0 }) }
    onDrawWithContent {
        drawContent()
        drawPath(marks, DialEdge.inner, style = mark)
    }
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
            if (tooth == 0 && index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
        }
    }
    close()
}
