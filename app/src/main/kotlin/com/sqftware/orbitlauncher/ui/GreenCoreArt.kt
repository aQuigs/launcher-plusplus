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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.CellWell
import com.sqftware.orbitlauncher.ui.theme.CoreEdge
import com.sqftware.orbitlauncher.ui.theme.CoreGlow
import com.sqftware.orbitlauncher.ui.theme.CoreLight
import com.sqftware.orbitlauncher.ui.theme.CoreWire
import com.sqftware.orbitlauncher.ui.theme.OrbDark
import com.sqftware.orbitlauncher.ui.theme.OrbGleam
import com.sqftware.orbitlauncher.ui.theme.OrbLight
import com.sqftware.orbitlauncher.ui.theme.ReactorBlaze
import com.sqftware.orbitlauncher.ui.theme.ReactorDark
import com.sqftware.orbitlauncher.ui.theme.ReactorDeep
import com.sqftware.orbitlauncher.ui.theme.ReactorMote
import com.sqftware.orbitlauncher.ui.theme.ReactorRing
import com.sqftware.orbitlauncher.ui.theme.RingColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** How many times a wire sphere turns, and an orb pulses, in the hour the folders' sky takes to turn once. */
private const val SPHERE_TURNS_PER_HOUR = 12
private const val ORB_PULSES_PER_HOUR = 240

/** How much of a folder's square the disc of previews takes on each part, which shows round it. */
private const val SPHERE_DISC = 0.72f
private const val ORB_DISC = 0.74f
private const val HEX_DISC = 0.7f

/** The wire sphere's lines of longitude, which turn, and of latitude, in degrees. */
private const val MERIDIANS = 12
private val Parallels = listOf(-60f, -30f, 0f, 30f, 60f)

/** How many rungs the ring's wire tube has, and how many points round each. */
private const val RUNGS = 72
private const val RUNG_POINTS = 12

/**
 * A reactor's green core: the emblem is a wire sphere turning with the ring round a glowing blob that heaves and
 * drifts; the ring a wireframe tube; folders are wire spheres, pulsing orbs or hex cells; the drawer ripples with
 * waves; and the reactor itself, a tunnel of rings into a blazing core, is a scene the launcher's menu offers as the
 * wallpaper.
 */
object GreenCoreArt : ThemeArt {
    override val scene = ThemeScene("reactor") { reactor() }

    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val ground = MaterialTheme.colorScheme.surfaceContainerLowest
        val wire = MaterialTheme.colorScheme.primary
        Spacer(
            modifier.graphicsLayer().drawWithCache {
                val radius = size.emblemRadius * 0.62f
                val sphere = WireSphere(size.center, radius)
                val line = Stroke(1.dp.toPx())
                // Built about the origin and moved to where the blob has drifted, so its glow is made once.
                val blob = Path()
                val plasma = Brush.radialGradient(listOf(wire.copy(alpha = 0.75f), wire.copy(alpha = 0.2f)), Offset.Zero, radius * 0.5f)
                val rim = wire.copy(alpha = 0.8f)
                onDrawBehind {
                    drawCircle(ground, size.emblemRadius)
                    if (marked) {
                        val beat = cycles(fastTurn(), 1) * 2 * PI.toFloat()
                        val heart = size.center + Offset(sin(beat * 3), sin(beat * 2 + 1f)) * (radius * 0.12f)
                        blob.rewind()
                        blob.addBlob(radius * 0.42f, beat)
                        translate(heart.x, heart.y) {
                            drawPath(blob, plasma)
                            drawPath(blob, rim, style = line)
                        }
                        sphere.draw(this, slowTurn(), wire, line)
                    }
                }
            },
        )
    }

    /** A wireframe tube through the slots: two rails and the rungs round it, kept clear of the slots' discs. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val tube = 4.dp.toPx()
        val depth = 1.5.dp.toPx()
        val line = Stroke(0.75.dp.toPx())
        val rails = Path().apply {
            addOval(Rect(centre, radius - tube))
            addOval(Rect(centre, radius + tube))
        }
        val rungs = Path()
        repeat(RUNGS) { rung ->
            val out = direction(rung * 360f / RUNGS)
            val along = Offset(-out.y, out.x)
            repeat(RUNG_POINTS + 1) { point ->
                val turned = point * 2 * PI.toFloat() / RUNG_POINTS
                val at = centre + out * (radius + tube * cos(turned)) + along * (depth * sin(turned))
                if (point == 0) rungs.moveTo(at.x, at.y) else rungs.lineTo(at.x, at.y)
            }
        }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                clipPath(discs, ClipOp.Difference) {
                    drawPath(rails, colours.mark.copy(alpha = (0.35f + 0.4f * glow) * alpha), style = line)
                    drawPath(rungs, colours.mark.copy(alpha = (0.2f + 0.3f * glow) * alpha), style = line)
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
            FolderLook.Orb -> Modifier.orb { style.minutes() } to Modifier.fillMaxSize(ORB_DISC)
            FolderLook.HexCell -> Modifier.hexCell() to Modifier.fillMaxSize(HEX_DISC)
            // Wire spheres, and a look of another theme's met before the theme's own is picked.
            else -> Modifier.wireSphere { style.minutes() } to Modifier.fillMaxSize(SPHERE_DISC)
        }
        Box(modifier.then(worn), contentAlignment = Alignment.Center) { FolderDisc(folder, icon, presses, inner, disc) }
    }

    private val waves = DrawerTileArt { Waves(this, it) }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val faint = colour.copy(alpha = colour.alpha * 0.3f)
        forEachDrawerTile(scrolled) { origin, tile ->
            val built = with(waves) { of(tile) }
            translate(origin.x, origin.y) { drawPath(built.lines, faint, style = built.line) }
        }
    }
}

/**
 * A wire sphere of [radius] round [centre], seen side on: its parallels, built once as straight lines, and the near
 * halves of its meridians, which sweep across as it turns and slip round its rim.
 */
private class WireSphere(val centre: Offset, val radius: Float) {
    private val parallels = Path().apply {
        Parallels.forEach { latitude ->
            val across = radius * cos(latitude * PI.toFloat() / 180)
            val at = centre - Offset(0f, radius * sin(latitude * PI.toFloat() / 180))
            moveTo(at.x - across, at.y)
            lineTo(at.x + across, at.y)
        }
    }

    /** Draws the sphere in [wire], its meridians turned by [turn] degrees. */
    fun draw(scope: DrawScope, turn: Float, wire: Color, line: Stroke) = with(scope) {
        drawCircle(wire, radius, centre, style = line)
        drawPath(parallels, wire, style = line)
        repeat(MERIDIANS) { index ->
            val longitude = (turn + index * 360f / MERIDIANS) * PI.toFloat() / 180
            // Only the meridians on the near side, each half an ellipse on the side it leans to.
            if (cos(longitude) > 0f) {
                val across = radius * sin(longitude)
                drawArc(
                    wire,
                    startAngle = if (across > 0f) -90f else 90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centre.x - abs(across), centre.y - radius),
                    size = Size(abs(across) * 2, radius * 2),
                    style = line,
                )
            }
        }
    }
}

/**
 * A blob of plasma [reach] round the origin, its outline heaving as [beat] goes round: each swell a whole number of
 * times a round, so it never jumps as it starts over. Rounder than [addWavering] would make it, so it heaves.
 */
private fun Path.addBlob(reach: Float, beat: Float) {
    val points = 48
    repeat(points) { index ->
        val angle = index * 2 * PI.toFloat() / points
        val swell = 1f + 0.12f * sin(3 * angle + beat * 5) + 0.08f * sin(5 * angle - beat * 7) + 0.05f * sin(2 * angle + beat * 4)
        val at = Offset(cos(angle), sin(angle)) * (reach * swell)
        if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
    }
    close()
}

/** A wire sphere round the item, turning as the [minutes] go by, its rim edged in two lines. */
private fun Modifier.wireSphere(minutes: () -> Float): Modifier = drawWithCache {
    val radius = size.minDimension / 2 * 1.08f
    val sphere = WireSphere(size.center, radius)
    val line = Stroke(1.dp.toPx())
    val outer = Stroke(3.dp.toPx())
    val glow = Brush.radialGradient(listOf(CoreGlow, Color.Transparent), size.center, radius)
    onDrawBehind {
        drawCircle(glow, radius, size.center)
        drawCircle(CoreEdge.outer, radius, size.center, style = outer)
        sphere.draw(this, minutes() * 6f * SPHERE_TURNS_PER_HOUR, CoreWire, line)
        drawCircle(CoreEdge.inner, radius, size.center, style = line)
    }
}

/** A glass orb round the item, lit from the top left, swelling and easing as the [minutes] go by. */
private fun Modifier.orb(minutes: () -> Float): Modifier = drawWithCache {
    val radius = size.minDimension / 2 * 1.04f
    val centre = size.center
    val glass = Brush.radialGradient(listOf(OrbLight, OrbDark), centre - Offset(radius, radius) * 0.35f, radius * 1.6f)
    val gleam = Rect(centre + Offset(-radius * 0.62f, -radius * 0.8f), Size(radius * 0.6f, radius * 0.28f))
    val line = Stroke(1.dp.toPx())
    val outer = Stroke(3.dp.toPx())
    onDrawBehind {
        val swell = 1f + 0.04f * sin(minutes() * ORB_PULSES_PER_HOUR / 60f * 2 * PI.toFloat())
        val swollen = radius * swell
        drawCircle(glass, swollen, centre)
        drawOval(OrbGleam, gleam.topLeft, gleam.size)
        drawCircle(CoreEdge.outer, swollen, centre, style = outer)
        drawCircle(CoreEdge.inner, swollen, centre, style = line)
    }
}

/**
 * A hexagonal cell round the item, its far rim set back up and to the right, its dark well lit by a glowing inner rim,
 * and its near rim edged in two lines.
 */
private fun Modifier.hexCell(): Modifier = drawWithCache {
    val reach = size.minDimension / 2 * 1.1f
    val centre = size.center
    val back = Offset(3.dp.toPx(), -3.dp.toPx())
    val near = List(6) { centre + direction(it * 60f + 30f) * reach }
    fun outline(corners: List<Offset>) = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        corners.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    val front = outline(near)
    val far = outline(near.map { it + back })
    val walls = Path().apply { near.forEach { moveTo(it.x, it.y); lineTo(it.x + back.x, it.y + back.y) } }
    val innerRim = outline(near.map { centre + (it - centre) * 0.78f })
    val line = Stroke(1.dp.toPx())
    val outer = Stroke(3.dp.toPx())
    onDrawBehind {
        drawPath(far, CoreWire, style = line)
        drawPath(walls, CoreWire, style = line)
        drawPath(front, CellWell)
        drawPath(innerRim, CoreGlow, style = outer)
        drawPath(front, CoreEdge.outer, style = outer)
        drawPath(front, CoreEdge.inner, style = line)
    }
}

/**
 * A drawer tile's waves: a whole number of lines down the tile and of swells across it, so they run on unbroken where
 * tiles meet.
 */
private class Waves(density: Density, size: Size) {
    val line = Stroke(with(density) { 0.75.dp.toPx() })
    val lines = Path().apply {
        val gap = size.height / 16
        val swell = with(density) { 10.dp.toPx() }
        val steps = 48
        var y = gap / 2
        while (y < size.height) {
            repeat(steps + 1) { step ->
                val x = size.width * step / steps
                val at = y + swell * sin(x / size.width * 4 * PI.toFloat() + y / size.height * 2 * PI.toFloat())
                if (step == 0) moveTo(x, at) else lineTo(x, at)
            }
            y += gap
        }
    }
}

/**
 * The reactor: a tunnel of rings running into a blazing core, the nearer rings wider and heavier, ribs along the tunnel's
 * walls, and motes drifting in the glow.
 */
private fun DrawScope.reactor() {
    // About where the home page puts the ring's centre; the blaze is wide enough not to need it exact.
    val core = Offset(size.width / 2, size.height * 0.545f)
    val far = size.maxDimension
    drawRect(ReactorDark)
    drawCircle(Brush.radialGradient(listOf(ReactorBlaze, ReactorDeep, ReactorDark), core, far * 0.42f), far * 0.42f, core)

    val rib = Stroke(1.dp.toPx())
    repeat(16) { index ->
        drawLine(ReactorRing, core + direction(index * 22.5f) * (far * 0.04f), core + direction(index * 22.5f) * far, rib.width)
    }
    var radius = 24.dp.toPx()
    var width = 1.dp.toPx()
    while (radius < far) {
        drawCircle(ReactorRing, radius, core, style = Stroke(width))
        radius *= 1.32f
        width *= 1.25f
    }

    val random = Random(5)
    repeat(70) {
        val at = core + direction(random.nextFloat() * 360f) * (far * 0.5f * random.nextFloat().pow(0.7f))
        drawCircle(ReactorMote, 1.dp.toPx() + random.nextFloat() * 2.dp.toPx(), at)
    }
    drawCircle(Brush.radialGradient(listOf(CoreLight, ReactorBlaze, Color.Transparent), core, 40.dp.toPx()), 40.dp.toPx(), core)
}
