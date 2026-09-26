package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.Planet
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.DiscEdge
import com.sqftware.orbitlauncher.ui.theme.FolderEdge
import com.sqftware.orbitlauncher.ui.theme.FolderMoon
import com.sqftware.orbitlauncher.ui.theme.OrbitCoreLit
import com.sqftware.orbitlauncher.ui.theme.OrbitCoreShade
import com.sqftware.orbitlauncher.ui.theme.OrbitTrack
import com.sqftware.orbitlauncher.ui.theme.PlanetPaints
import com.sqftware.orbitlauncher.ui.theme.PlanetRing
import com.sqftware.orbitlauncher.ui.theme.PlanetShadow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * How every folder on the screen draws itself: the [look] the user chose, the [planets] the folders are in the Solar
 * system, and the [minutes] the sky has turned, which move what moves.
 */
class FolderStyle(
    val look: FolderLook,
    val planets: Map<HomePlace.Folder, Planet> = emptyMap(),
    val minutes: () -> Float = { 0f },
)

val LocalFolderStyle = compositionLocalOf { FolderStyle(FolderLook.Rim) }

/**
 * [folder] drawn as a planet in the look of [LocalFolderStyle], filling the item: a disc of its previews the size of an
 * app's, with whatever it wears (moons, a ring, a planet's touch) reaching past it. [inner] fades what shows the folder's
 * apps, as when the planet takes the ring's centre and its apps are round it instead; [presses] ripple its disc.
 */
@Composable
fun PlanetFace(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    modifier: Modifier = Modifier,
    presses: InteractionSource? = null,
    inner: () -> Float = { 1f },
) {
    val style = LocalFolderStyle.current
    if (style.look == FolderLook.Orbit) {
        OrbitFace(folder, icon, style.minutes, inner, modifier)
        return
    }

    val planet = if (style.look == FolderLook.SolarSystem) style.planets[folder.at] else null
    val disc = planet.discFraction()
    val worn = when {
        planet != null -> Modifier.drawWithContent {
            val radius = size.minDimension / 2 * disc
            drawPlanetTouch(planet, radius, style.minutes, behind = true)
            drawContent()
            drawPlanetTouch(planet, radius, style.minutes, behind = false)
        }
        style.look == FolderLook.Ringed -> Modifier.tiltedRing()
        else -> Modifier
    }
    val glass = if (planet != null) Modifier.drawBehind { drawPlanetGlass(planet, size.minDimension / 2, style.minutes) } else Modifier
    val moons = if (style.look == FolderLook.Rim) Modifier.moons(folder.apps.size) else Modifier

    Box(modifier.then(worn), contentAlignment = Alignment.Center) {
        // An app's icon fills its disc, but a folder's disc is mostly glass, which fades into the wallpaper.
        IconDisc(presses, Modifier.fillMaxSize(disc).then(glass).edge(FolderEdge).then(moons), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = inner() }, contentAlignment = Alignment.Center) {
                FolderPreviews(folder, icon)
            }
        }
    }
}

/** Rings the circle inscribed in the item with [edge]'s two lines, the dark one outermost. */
internal fun Modifier.edge(edge: DiscEdge): Modifier = drawWithContent {
    drawContent()
    val line = 1.dp.toPx()
    val radius = size.minDimension / 2
    drawCircle(edge.outer, radius = radius - line / 2, style = Stroke(line))
    drawCircle(edge.inner, radius = radius - line * 1.5f, style = Stroke(line))
}

/**
 * A moon on the disc's edge for each of [count] apps, however many, so a folder reads as a planet its apps orbit. They
 * shrink as they crowd. The first sits at the lower left, clear of the unread badge, and the rest follow clockwise.
 */
private fun Modifier.moons(count: Int): Modifier = drawWithContent {
    drawContent()
    if (count == 0) return@drawWithContent
    val orbit = size.minDimension / 2 - 1.dp.toPx()
    val moon = min(size.minDimension * MOON_FRACTION, (PI * orbit / count).toFloat() * MOON_CROWDING).coerceAtLeast(0.9.dp.toPx())
    val rim = min(0.75.dp.toPx(), moon / 2)
    repeat(count) { index ->
        val angle = MOONS_START + 2 * PI * index / count
        val at = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * orbit
        drawCircle(FolderMoon.outer, radius = moon + rim, center = at)
        drawCircle(FolderMoon.inner, radius = moon, center = at)
    }
}

private const val MOON_FRACTION = 0.06f
private const val MOON_CROWDING = 0.34f
private const val MOONS_START = 3 * PI / 4

/** A gold ring tilted across the item's disc: its far half behind the disc, its near half in front. */
private fun Modifier.tiltedRing(): Modifier = drawWithContent {
    val r = size.minDimension / 2
    val ring = Size(r * 3f, r * 0.84f)
    val topLeft = center - Offset(ring.width / 2, ring.height / 2)
    fun half(from: Float) = rotate(RING_TILT, center) {
        drawArc(PlanetRing.outer, from, 180f, useCenter = false, topLeft = topLeft, size = ring, style = Stroke(r * 0.13f + 2.dp.toPx()))
        drawArc(PlanetRing.inner, from, 180f, useCenter = false, topLeft = topLeft, size = ring, style = Stroke(r * 0.13f))
    }
    half(180f)
    drawContent()
    half(0f)
}

private const val RING_TILT = -22f

/**
 * A small planet with [folder]'s apps going round it as moons, a turn every [ORBIT_MINUTES] of the sky's [minutes]. The
 * track and moons reach past the item; [inner] fades them.
 */
@Composable
private fun OrbitFace(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    minutes: () -> Float,
    inner: () -> Float,
    modifier: Modifier,
) {
    val count = folder.apps.size
    Layout(
        content = {
            folder.apps.forEach { app ->
                AppImage(app, icon, Modifier.clip(CircleShape).graphicsLayer { alpha = inner() })
            }
        },
        modifier = modifier.drawBehind {
            val r = size.minDimension / 2
            val core = r * ORBIT_CORE
            val lit = Brush.radialGradient(listOf(OrbitCoreLit, OrbitCoreShade), center - Offset(core * 0.3f, core * 0.35f), core * 1.3f)
            drawCircle(lit, radius = core)
            drawCircle(FolderEdge.inner, radius = core, style = Stroke(1.dp.toPx()))
            drawCircle(PlanetShadow.copy(alpha = PlanetShadow.alpha * inner()), radius = r * ORBIT_TRACK, style = Stroke(3.dp.toPx()))
            drawCircle(OrbitTrack.copy(alpha = OrbitTrack.alpha * inner()), radius = r * ORBIT_TRACK, style = Stroke(1.dp.toPx()))
        },
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight)
        val track = side / 2f * ORBIT_TRACK
        val moon = min(side * ORBIT_MOON, (PI * track / count.coerceAtLeast(1)).toFloat() * 1.2f).roundToInt().coerceAtLeast(1)
        val placeables = measurables.map { it.measure(Constraints.fixed(moon, moon)) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val turn = 2 * PI * minutes() / ORBIT_MINUTES
            placeables.forEachIndexed { index, placeable ->
                val angle = turn + 2 * PI * index / count
                val x = constraints.maxWidth / 2f + track * sin(angle).toFloat() - moon / 2f
                val y = constraints.maxHeight / 2f - track * cos(angle).toFloat() - moon / 2f
                placeable.place(x.roundToInt(), y.roundToInt())
            }
        }
    }
}

private const val ORBIT_CORE = 0.56f
private const val ORBIT_TRACK = 1.05f
private const val ORBIT_MOON = 0.3f
private const val ORBIT_MINUTES = 1f / 3

/**
 * How far something going round [perMinute] turns a minute, backwards if negative, has gone after [minutes], in radians.
 * The sky is read only here, so a planet with nothing going round is not drawn again as it turns.
 */
private fun orbitAngle(minutes: () -> Float, perMinute: Float) = (2 * PI * minutes() * perMinute).toFloat()

/** Pluto is a dwarf planet, so its folder's disc is a little smaller than the others'. */
internal fun Planet?.discFraction() = if (this == Planet.Pluto) PLUTO_DISC else 1f

private const val PLUTO_DISC = 0.78f

/**
 * Tints a folder's disc of [radius] with [planet]'s colour through its glass. The Earth's land and clouds go round on
 * it, and Jupiter's bands roll, each at its own pace, with the Great Red Spot drifting across, round the far side and
 * back, as Jupiter spins faster than any other planet.
 */
private fun DrawScope.drawPlanetGlass(planet: Planet, radius: Float, minutes: () -> Float) {
    val paint = PlanetPaints.getValue(planet)
    clipPath(Path().apply { addOval(Rect(center, radius)) }) {
        drawCircle(paint.tint, radius, center)
        when (planet) {
            Planet.Earth -> {
                val spin = orbitAngle(minutes, EARTH_SPIN)
                EARTH_FEATURES.forEachIndexed { index, (longitude, latitude, size) ->
                    val angle = longitude + spin
                    val depth = cos(angle)
                    if (depth <= 0f) return@forEachIndexed
                    val at = center + Offset(sin(angle) * radius * 0.82f * cos(latitude), latitude * radius)
                    val oval = Size((size * radius * depth).coerceAtLeast(0.5f) * 2, size * radius * 1.6f)
                    val colour = paint.features[index]
                    drawOval(colour.copy(alpha = colour.alpha * min(1f, depth * 2.5f)), at - Offset(oval.width / 2, oval.height / 2), oval)
                }
            }
            Planet.Jupiter -> {
                val drift = orbitAngle(minutes, JUPITER_SPIN)
                JUPITER_BANDS.forEachIndexed { index, (latitude, height) ->
                    // Wavy edges that roll along as it spins, each band at its own pace, as Jupiter's winds do.
                    fun edge(x: Float, side: Float) =
                        center.y + (latitude + side) * radius + sin(x / radius * 5 + drift * (1 + index * 0.35f) + index) * radius * 0.035f
                    val xs = (0..24).map { -radius + radius * it / 12 }
                    val band = Path().apply {
                        moveTo(center.x + xs.first(), edge(xs.first(), -height / 2))
                        xs.forEach { lineTo(center.x + it, edge(it, -height / 2)) }
                        xs.asReversed().forEach { lineTo(center.x + it, edge(it, height / 2)) }
                        close()
                    }
                    drawPath(band, paint.features[index])
                }
                val spot = drift * 0.6f
                val depth = cos(spot)
                if (depth > 0f) {
                    val at = Offset(center.x + sin(spot) * radius * 0.72f, center.y + radius * 0.34f)
                    val width = (radius * 0.26f * depth).coerceAtLeast(0.6f)
                    val fade = min(1f, depth * 2.5f)
                    fun oval(rx: Float, ry: Float, colour: Color) =
                        drawOval(colour.copy(alpha = colour.alpha * fade), at - Offset(rx, ry), Size(2 * rx, 2 * ry))
                    oval(width * 1.25f, radius * 0.19f, paint.accent)
                    oval(width, radius * 0.14f, paint.accent2)
                }
            }
            else -> Unit
        }
    }
}

/** Longitude and latitude in radians and size against the disc of the Earth's land and clouds, in [PlanetPaint.features]' order. */
private val EARTH_FEATURES = listOf(
    Triple(0.3f, -0.35f, 0.34f), Triple(1.1f, 0.25f, 0.3f), Triple(2.4f, -0.1f, 0.38f), Triple(3.4f, 0.4f, 0.26f),
    Triple(4.6f, -0.2f, 0.32f), Triple(5.5f, 0.3f, 0.22f), Triple(1.8f, -0.5f, 0.24f), Triple(4.0f, 0.55f, 0.22f),
)

/** Where each of Jupiter's bands lies from the disc's centre, and how tall it is, against its radius. */
private val JUPITER_BANDS = listOf(
    -0.78f to 0.1f, -0.52f to 0.13f, -0.24f to 0.12f, 0.06f to 0.1f, 0.32f to 0.14f, 0.6f to 0.1f, 0.82f to 0.08f,
)

/**
 * Draws [planet]'s touch round its folder's disc of [radius], behind the disc when [behind] and in front of it otherwise:
 * Mercury's sunlit edge, Venus's backwards haze, the Earth's Moon, Mars's rim and two moons, Saturn's ring, Uranus's
 * upright one, Neptune's rim and backwards Triton, and Pluto's heart turned to Charon.
 */
private fun DrawScope.drawPlanetTouch(planet: Planet, radius: Float, minutes: () -> Float, behind: Boolean) {
    val paint = PlanetPaints.getValue(planet)
    val line = 1.dp.toPx()

    // Seen edge-on from a little above: behind the folder on the far half of its orbit, in front on the near half.
    fun moon(angle: Float, distance: Float, size: Float, colour: Color) {
        if ((cos(angle) > 0f) == behind) return
        val at = center + Offset(sin(angle) * radius * distance, cos(angle) * radius * distance * 0.35f)
        val r = (radius * size).coerceAtLeast(1.4f * line)
        drawCircle(PlanetShadow, r + line, at)
        drawCircle(colour, r, at)
    }

    fun halfRing(oval: Size, width: Float, colour: Color) = drawArc(
        colour, if (behind) 180f else 0f, 180f, useCenter = false,
        topLeft = center - Offset(oval.width / 2, oval.height / 2), size = oval, style = Stroke(width),
    )

    fun rim() = drawCircle(paint.accent, radius * 0.94f, center, style = Stroke((radius * 0.08f).coerceAtLeast(1.5f * line)))

    fun arcAround(scale: Float) = Size(radius * 2 * scale, radius * 2 * scale).let { it to center - Offset(it.width / 2, it.height / 2) }

    when (planet) {
        // The side nearest the Sun glows hot.
        Planet.Mercury -> if (!behind) {
            val glow = Brush.linearGradient(listOf(paint.accent, paint.accent2), center - Offset(radius, radius), center + Offset(radius * 0.2f, radius * 0.2f))
            val (arc, topLeft) = arcAround(0.96f)
            drawArc(glow, 171f, 117f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke((radius * 0.14f).coerceAtLeast(2 * line), cap = StrokeCap.Round))
        }
        Planet.Venus -> if (behind) {
            val swirl = Math.toDegrees(orbitAngle(minutes, VENUS_SPIN).toDouble()).toFloat()
            val (haze, topLeft) = arcAround(1.13f)
            repeat(3) { index ->
                val stroke = Stroke((radius * 0.13f).coerceAtLeast(2 * line), cap = StrokeCap.Round)
                val shadow = Stroke(stroke.width + 2 * line, cap = StrokeCap.Round)
                drawArc(PlanetShadow, swirl + index * 120f, 69f, useCenter = false, topLeft = topLeft, size = haze, style = shadow)
                drawArc(paint.accent, swirl + index * 120f, 69f, useCenter = false, topLeft = topLeft, size = haze, style = stroke)
            }
        }
        Planet.Earth -> moon(orbitAngle(minutes, MOON_ORBIT), 1.4f, 0.16f, paint.moon)
        // Phobos races round, Deimos dawdles.
        Planet.Mars -> {
            if (!behind) rim()
            moon(orbitAngle(minutes, PHOBOS_ORBIT), 1.22f, 0.08f, paint.moon)
            moon(orbitAngle(minutes, DEIMOS_ORBIT) + 2f, 1.5f, 0.07f, paint.moon)
        }
        Planet.Jupiter -> Unit
        // The C ring, the bright B ring, the dark Cassini Division and the A ring, drawn flat and seen from a little above,
        // so they thin towards the near and far sides as real rings do. A shadow at their edges sets them off any wallpaper.
        Planet.Saturn -> rotate(SATURN_TILT, center) {
            scale(1f, SATURN_TILT_SQUASH, center) {
                fun band(scale: Float) = Size(radius * scale * 2, radius * scale * 2)
                SATURN_EDGES.forEach { halfRing(band(it), 2 * line, PlanetShadow) }
                SATURN_RINGS.forEachIndexed { index, (scale, width) -> halfRing(band(scale), radius * width, paint.features[index]) }
            }
        }
        // Rolls on its side, so its ring stands upright.
        Planet.Uranus -> rotate(URANUS_TILT, center) {
            val oval = Size(radius * 2.9f, radius * 0.6f)
            val width = (radius * 0.07f).coerceAtLeast(1.5f * line)
            halfRing(oval, width + 2 * line, PlanetShadow)
            halfRing(oval, width, paint.accent)
        }
        Planet.Neptune -> {
            if (!behind) rim()
            moon(orbitAngle(minutes, TRITON_ORBIT), 1.38f, 0.12f, paint.moon)
        }
        // Charon goes round backwards, and Pluto's heart always faces it: the two are locked face to face.
        Planet.Pluto -> if (!behind) {
            val angle = orbitAngle(minutes, CHARON_ORBIT)
            val towards = Offset(cos(angle), sin(angle))
            val charon = center + towards * radius * 1.42f
            drawCircle(PlanetShadow, radius * 0.34f + line, charon)
            drawCircle(paint.moon, radius * 0.34f, charon)
            val at = center + towards * radius * 0.82f
            val heart = radius * 0.2f
            val path = Path().apply {
                moveTo(at.x, at.y + heart * 0.9f)
                cubicTo(at.x - heart * 1.4f, at.y - heart * 0.1f, at.x - heart * 0.6f, at.y - heart * 1.1f, at.x, at.y - heart * 0.35f)
                cubicTo(at.x + heart * 0.6f, at.y - heart * 1.1f, at.x + heart * 1.4f, at.y - heart * 0.1f, at.x, at.y + heart * 0.9f)
                close()
            }
            rotate(Math.toDegrees(angle.toDouble()).toFloat() - 90f, at) {
                drawPath(path, paint.accent)
                drawPath(path, PlanetShadow, style = Stroke(line))
            }
        }
    }
}

/** Scale against the disc and width against its radius of Saturn's rings, in [PlanetPaint.features]' order. */
private val SATURN_RINGS = listOf(1.26f to 0.12f, 1.44f to 0.2f, 1.57f to 0.04f, 1.7f to 0.14f)
private val SATURN_EDGES = listOf(1.2f, 1.77f)
private const val SATURN_TILT_SQUASH = 0.3f

// Turns a minute: quick enough to see, and in the order of the real ones.
private const val EARTH_SPIN = 3f
private const val JUPITER_SPIN = 5f
private const val VENUS_SPIN = -2.5f
private const val MOON_ORBIT = 1.2f
private const val PHOBOS_ORBIT = 6f
private const val DEIMOS_ORBIT = 1.5f
private const val TRITON_ORBIT = -1.6f
private const val CHARON_ORBIT = -1f
private const val SATURN_TILT = -23f
private const val URANUS_TILT = 81f
