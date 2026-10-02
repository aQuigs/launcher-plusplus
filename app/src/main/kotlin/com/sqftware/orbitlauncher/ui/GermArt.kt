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
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.Cytoplasm
import com.sqftware.orbitlauncher.ui.theme.DishCoccus
import com.sqftware.orbitlauncher.ui.theme.GermBody
import com.sqftware.orbitlauncher.ui.theme.Membrane
import com.sqftware.orbitlauncher.ui.theme.Nucleus
import com.sqftware.orbitlauncher.ui.theme.Organelle
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.Virus
import com.sqftware.orbitlauncher.ui.theme.VirusLight
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.random.Random
import kotlin.math.sin

// How many times a bacillus's flagella wave, a virus turns and a cell wobbles in the hour the folders' sky takes to turn
// once: whole, so none jumps as the hour starts over.
private const val WAVES_PER_HOUR = 60
private const val VIRUS_TURNS_PER_HOUR = 4
private const val WOBBLES_PER_HOUR = 20

/**
 * How many times the emblem's bacillus divides in a turn, how far round each daughter leaves from the last, and how many
 * are seen swimming off. The daughters go round a whole number of times a turn, so none jumps as it starts over.
 */
private const val GENERATIONS_PER_TURN = 10
private const val SPLIT_TURN = 108f
private const val DAUGHTERS = 2

/** How much of a folder's square the disc of previews takes on each germ, which shows round it. */
private const val GERM_DISC = 0.78f
private const val VIRUS_DISC = 0.7f

/** How far a bacillus lies askew, so its flagella trail up and away to one side. */
private const val BACILLUS_TILT = -25f

/** Where a bacillus's two flagella leave its tail, as shares of its thickness either side of its middle. */
private val FLAGELLA = floatArrayOf(-0.16f, 0.16f)

/** Where the specks in the petri dish lie: at which angle from the top, and how far out as a share of the ring's radius. */
private val DishRods = listOf(20f to 0.76f, 160f to 0.72f, 250f to 0.78f)
private val DishCocci = listOf(75f to 0.74f, 205f to 0.7f, 300f to 0.76f)

/**
 * Under the microscope: the emblem is a bacillus dividing without end, the ring a petri dish with specks of culture in
 * it, folders are bacilli waving their flagella, viruses or wobbling cells, and the drawer is a slide.
 */
object GermArt : ThemeArt {
    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val ground = MaterialTheme.colorScheme.surfaceContainerLowest
        Box(modifier) {
            Spacer(Modifier.fillMaxSize().drawWithCache { onDrawBehind { drawCircle(ground, size.emblemRadius) } })
            // A layer of its own, so the colony dividing redraws only it.
            if (marked) Spacer(Modifier.fillMaxSize().graphicsLayer().drawWithCache { colony(fastTurn) })
        }
    }

    /** A petri dish's rim round the slots, its agar faintly tinting the ring, with a few rods and cocci between them. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val dish = minOf(radius + iconSize / 2 + 10.dp.toPx(), size.minDimension / 2 - 2.dp.toPx())
        val rim = Stroke(1.5.dp.toPx())
        val shine = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
        val shineAt = dish - 5.dp.toPx()
        val rodLength = 5.dp.toPx()
        val rods = Path().apply {
            DishRods.forEach { (angle, out) ->
                val at = centre + direction(angle) * (radius * out)
                val along = direction(angle + 70f) * rodLength
                moveTo(at.x - along.x, at.y - along.y)
                lineTo(at.x + along.x, at.y + along.y)
            }
        }
        val rodWidth = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
        val coccus = 2.5.dp.toPx()
        val cocci = Path().apply {
            DishCocci.forEach { (angle, out) ->
                val at = centre + direction(angle) * (radius * out)
                listOf(Offset(0f, 0f), Offset(1.7f, 0.4f), Offset(0.6f, 1.6f)).forEach { addOval(Rect(at + it * coccus, coccus)) }
            }
        }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                drawCircle(colours.mark.copy(alpha = (0.05f + 0.04f * glow) * alpha), dish, centre)
                drawCircle(colours.mark.copy(alpha = (0.35f + 0.35f * glow) * alpha), dish, centre, style = rim)
                drawArc(
                    colours.starLine.copy(alpha = (colours.starLine.alpha * 0.5f + 0.3f * glow) * alpha),
                    195f,
                    55f,
                    false,
                    centre - Offset(shineAt, shineAt),
                    Size(shineAt * 2, shineAt * 2),
                    style = shine,
                )
                clipPath(discs, ClipOp.Difference) {
                    drawPath(rods, colours.starLine.copy(alpha = (colours.starLine.alpha + 0.2f * glow) * alpha), style = rodWidth)
                    drawPath(cocci, DishCoccus.copy(alpha = DishCoccus.alpha * alpha))
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
            FolderLook.Virus -> Modifier.drawWithCache {
                virus(size.minDimension / 2 * 0.82f) { style.minutes() * 6f * VIRUS_TURNS_PER_HOUR }
            } to Modifier.fillMaxSize(VIRUS_DISC)
            FolderLook.Cell -> Modifier.cell { style.minutes() } to Modifier.fillMaxSize(GERM_DISC)
            // Bacilli, and a look of another theme's met before the theme's own is picked.
            else -> Modifier.bacillus { style.minutes() } to Modifier.fillMaxSize(GERM_DISC)
        }
        Box(modifier.then(worn), contentAlignment = Alignment.Center) { FolderDisc(folder, icon, presses, inner, disc) }
    }

    private val slide = DrawerTileArt { SlideTile(this, it) }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val faint = colour.copy(alpha = colour.alpha * 0.3f)
        forEachDrawerTile(scrolled) { origin, tile ->
            val built = with(slide) { of(tile) }
            translate(origin.x, origin.y) {
                drawPath(built.rods, faint, style = built.rodWidth)
                drawPath(built.tails, faint, style = built.tailWidth)
                drawPath(built.cocci, faint)
            }
        }
    }
}

/**
 * A drawer tile of the slide: rods, some trailing a flagellum, and clusters of cocci, scattered the same way on every
 * tile and kept clear of its edges so none is cut where tiles meet.
 */
private class SlideTile(density: Density, size: Size) {
    val rodWidth = Stroke(with(density) { 6.dp.toPx() }, cap = StrokeCap.Round)
    val tailWidth = Stroke(with(density) { 1.dp.toPx() })
    val rods = Path()
    val tails = Path()
    val cocci = Path()

    init {
        val random = Random(7)
        val margin = with(density) { 40.dp.toPx() }
        val unit = with(density) { 1.dp.toPx() }
        fun somewhere() = Offset(margin + random.nextFloat() * (size.width - margin * 2), margin + random.nextFloat() * (size.height - margin * 2))
        repeat(16) { index ->
            val at = somewhere()
            val along = direction(random.nextFloat() * 360f) * (unit * 7)
            rods.moveTo(at.x - along.x, at.y - along.y)
            rods.lineTo(at.x + along.x, at.y + along.y)
            if (index % 3 == 0) {
                val from = at + along * 1.5f
                val across = Offset(-along.y, along.x) / 7f
                tails.moveTo(from.x, from.y)
                for (step in 1..12) {
                    val point = from + along * (step / 4f) + across * (2f * sin(step * 0.9f))
                    tails.lineTo(point.x, point.y)
                }
            }
        }
        repeat(10) {
            val at = somewhere()
            listOf(Offset(0f, 0f), Offset(1.7f, 0.4f), Offset(0.6f, 1.6f)).forEach { cocci.addOval(Rect(at + it * (unit * 3.5f), unit * 3.5f)) }
        }
    }
}

/**
 * A bacillus in the middle of the dish dividing without end, [GENERATIONS_PER_TURN] times a turn of [turn]: it grows out
 * along its length, pinches in two, and the daughter swims off out of the dish, tumbling, while the half left behind
 * turns on and grows again. Each daughter leaves [SPLIT_TURN] degrees round from the last.
 */
private fun CacheDrawScope.colony(turn: () -> Float): DrawResult {
    val radius = size.emblemRadius
    val length = radius * 0.36f
    val thick = radius * 0.15f
    val dish = Path().apply { addOval(Rect(size.center, radius)) }
    val outer = Stroke(2.5.dp.toPx())
    val inner = Stroke(1.2.dp.toPx())
    fun Path.addCapsule(along: Float) =
        addRoundRect(RoundRect(along - length / 2, -thick / 2, along + length / 2, thick / 2, CornerRadius(thick / 2)))
    val cell = Path().apply { addCapsule(0f) }
    val growth = Path()
    val dividing = Path()
    fun DrawScope.germAt(shape: Path, at: Offset, degrees: Float) = withTransform({
        translate(at.x, at.y)
        rotate(degrees - 90f, Offset.Zero)
    }) { germ(shape, outer, inner) }
    return onDrawBehind {
        val generations = cycles(turn(), GENERATIONS_PER_TURN)
        val born = floor(generations)
        clipPath(dish) {
            // Those already off, from the eldest, which is the furthest out.
            for (ago in DAUGHTERS downTo 1) {
                val left = born - ago + 1
                val swum = generations - left
                // Turning on as the cell it left does, and slowing, so it curves away rather than swinging off straight.
                val heading = (left + 1f - exp(-swum)) * SPLIT_TURN
                germAt(cell, size.center + direction(heading) * (length * (1f + swum + 0.2f * swum * swum)), heading)
            }
            val grown = length * (generations - born)
            dividing.rewind()
            if (grown <= length - thick) {
                // Its halves still share their sides, so it is one longer rod.
                dividing.addRoundRect(RoundRect(-length / 2, -thick / 2, grown + length / 2, thick / 2, CornerRadius(thick / 2)))
            } else {
                // Pinching in two: the union shows the waist, or failing that both halves, which look the same but for it.
                growth.rewind()
                growth.addCapsule(grown)
                if (!dividing.op(cell, growth, PathOperation.Union)) {
                    dividing.addPath(cell)
                    dividing.addPath(growth)
                }
            }
            germAt(dividing, size.center, generations * SPLIT_TURN)
        }
    }
}

/** A germ's body in [shape], edged in its membrane's two lines so it shows on any wallpaper. */
private fun DrawScope.germ(shape: Path, outer: Stroke, inner: Stroke) {
    drawPath(shape, GermBody)
    drawPath(shape, Membrane.outer, style = outer)
    drawPath(shape, Membrane.inner, style = inner)
}

/**
 * A virus [radius] round the middle: a pink sphere lit at its top left, with twelve spikes, each ending in a knob,
 * edged in two lines so it shows on any wallpaper, and turned by [turned] degrees while its light stays put.
 */
private fun CacheDrawScope.virus(radius: Float, turned: () -> Float): DrawResult {
    val centre = size.center
    val spikes = Path()
    val knobs = Path()
    repeat(12) { index ->
        val along = direction(index * 30f)
        (centre + along * radius).let { spikes.moveTo(it.x, it.y) }
        (centre + along * (radius * 1.26f)).let {
            spikes.lineTo(it.x, it.y)
            knobs.addOval(Rect(it, radius * 0.09f))
        }
    }
    val spike = Stroke(radius * 0.08f)
    val outer = Stroke(3.dp.toPx())
    val inner = Stroke(1.dp.toPx())
    val light = centre + Offset(-0.32f, -0.34f) * radius
    return onDrawBehind {
        rotate(turned()) {
            drawPath(spikes, Virus, style = spike)
            drawPath(knobs, Virus)
        }
        drawCircle(Virus, radius, centre)
        drawCircle(Membrane.outer, radius, centre, style = outer)
        drawCircle(Membrane.inner, radius, centre, style = inner)
        drawCircle(VirusLight, radius * 0.24f, light)
    }
}

/**
 * A bacillus round the item, lying askew and reaching past it: a rod with a lime membrane, and two flagella trailing
 * from its tail that wave as the [minutes] go by.
 */
private fun Modifier.bacillus(minutes: () -> Float): Modifier = drawWithCache {
    val side = size.minDimension
    val centre = size.center
    val length = side * 1.08f
    val thick = side * 0.8f
    val bounds = Rect(centre.x - length / 2, centre.y - thick / 2, centre.x + length / 2, centre.y + thick / 2)
    val capsule = Path().apply { addRoundRect(RoundRect(bounds, CornerRadius(thick / 2))) }
    val tails = Path()
    val tailOuter = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round)
    val tailInner = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
    val outer = Stroke(4.dp.toPx())
    val inner = Stroke(2.dp.toPx())
    onDrawBehind {
        val phase = minutes() * WAVES_PER_HOUR / 60f * 2 * PI.toFloat()
        tails.rewind()
        for (across in FLAGELLA) {
            val from = Offset(bounds.right - thick * 0.08f, centre.y + thick * across)
            tails.moveTo(from.x, from.y)
            for (step in 1..16) {
                val along = step / 16f
                tails.lineTo(from.x + along * side * 0.3f, from.y + side * 0.07f * along * sin(along * 3 * PI.toFloat() - phase + across * 6))
            }
        }
        rotate(BACILLUS_TILT) {
            drawPath(tails, Membrane.outer, style = tailOuter)
            drawPath(tails, Membrane.inner, style = tailInner)
            germ(capsule, outer, inner)
        }
    }
}

/**
 * A cell round the item, its membrane wobbling as the [minutes] go by, with specks in its cytoplasm round the disc of
 * previews, which they never cross the membrane out of, and its nucleus showing through the previews.
 */
private fun Modifier.cell(minutes: () -> Float): Modifier = drawWithCache {
    val half = size.minDimension / 2
    val centre = size.center
    val membrane = Path()
    val outer = Stroke(4.dp.toPx())
    val inner = Stroke(2.dp.toPx())
    val nucleus = centre + Offset(-0.4f, -0.36f) * half
    // At its narrowest the membrane is 0.79 of its reach from the middle, so specks at 0.82 of the half stay inside it.
    val specks = listOf(direction(60f), direction(150f), direction(240f), direction(320f)).map { centre + it * (half * 0.82f) }
    onDrawBehind {
        val wobble = minutes() * WOBBLES_PER_HOUR / 60f * 2 * PI.toFloat()
        membrane.rewind()
        membrane.addWavering(wobble, wobble * 1.6f, centre, half * 1.16f)
        drawPath(membrane, Cytoplasm)
        specks.forEach { drawCircle(Organelle, half * 0.05f, it) }
        drawCircle(Nucleus, half * 0.17f, nucleus)
        drawPath(membrane, Membrane.outer, style = outer)
        drawPath(membrane, Membrane.inner, style = inner)
    }
}
