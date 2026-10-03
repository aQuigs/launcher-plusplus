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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.FolderLook
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.ui.theme.CitySky
import com.sqftware.orbitlauncher.ui.theme.CityGround
import com.sqftware.orbitlauncher.ui.theme.CityStreet
import com.sqftware.orbitlauncher.ui.theme.CityTrail
import com.sqftware.orbitlauncher.ui.theme.CityTrailGlow
import com.sqftware.orbitlauncher.ui.theme.CrystalEdge
import com.sqftware.orbitlauncher.ui.theme.GeodeHollow
import com.sqftware.orbitlauncher.ui.theme.GeodeRind
import com.sqftware.orbitlauncher.ui.theme.GeodeShard
import com.sqftware.orbitlauncher.ui.theme.GeodeShardLight
import com.sqftware.orbitlauncher.ui.theme.GlassLeft
import com.sqftware.orbitlauncher.ui.theme.GlassLine
import com.sqftware.orbitlauncher.ui.theme.GlassRight
import com.sqftware.orbitlauncher.ui.theme.GlassTop
import com.sqftware.orbitlauncher.ui.theme.PlazaGlow
import com.sqftware.orbitlauncher.ui.theme.RingColors
import com.sqftware.orbitlauncher.ui.theme.TowerEdge
import com.sqftware.orbitlauncher.ui.theme.TowerRoof
import com.sqftware.orbitlauncher.ui.theme.TowerWall
import com.sqftware.orbitlauncher.ui.theme.TowerWallDark
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** How many times an octahedron turns round in the hour the folders' sky takes to turn once. */
private const val OCTAHEDRON_TURNS_PER_HOUR = 4

/** How much of a folder's square the disc of previews takes on each crystal, which shows round it. */
private const val CUBE_DISC = 0.74f
private const val OCTAHEDRON_DISC = 0.6f
private const val GEODE_DISC = 0.66f

/** How many times a minute the emblem's cubes float up and down. */
private const val BOBS_PER_TURN = 6

/** The emblem's cubes: where each sits about the middle, and how far it reaches, both as shares of the emblem's radius. */
private val EmblemCubes = listOf(
    Offset(-0.3f, -0.3f) to 0.1f,
    Offset(-0.24f, 0.12f) to 0.2f,
    Offset(0.14f, -0.06f) to 0.3f,
    Offset(0.02f, 0.36f) to 0.13f,
)

/**
 * A city of glass seen from above: the emblem is a cluster of glass cubes floating, the ring a thread of light with a
 * small cube between each pair of slots, folders are cubes, turning octahedra or geodes, the drawer is scattered with
 * wireframe cubes, and the city itself is a scene the launcher's menu offers as the wallpaper.
 */
object CrystalArt : ThemeArt {
    override val scene = ThemeScene("crystal city") { city() }

    @Composable
    override fun EmblemFace(marked: Boolean, slowTurn: () -> Float, fastTurn: () -> Float, minuteOfDay: () -> Int, modifier: Modifier) {
        val ground = MaterialTheme.colorScheme.surfaceContainerLowest
        // The glass's own light edges would vanish on the day's pale ground, so the emblem's are drawn in the scheme's.
        val edge = MaterialTheme.colorScheme.primary
        Spacer(
            modifier.graphicsLayer().drawWithCache {
                val radius = size.emblemRadius
                val cubes = EmblemCubes.map { (at, reach) -> cube(size.center + at * radius, radius * reach) }
                val line = Stroke(1.dp.toPx())
                onDrawBehind {
                    drawCircle(ground, radius)
                    if (marked) {
                        val beat = cycles(fastTurn(), BOBS_PER_TURN) * 2 * PI.toFloat()
                        cubes.forEachIndexed { index, cube ->
                            // Each out of step with the next, so the cluster never rises and falls as one.
                            translate(top = radius * 0.035f * sin(beat + index * 1.9f)) { cube.draw(this, line, edge) }
                        }
                    }
                }
            },
        )
    }

    /** A thread of light through the slots, with a small glass cube on it halfway between each pair of them. */
    override fun CacheDrawScope.ringMarks(
        centre: Offset,
        slots: List<Offset>,
        radius: Float,
        iconSize: Float,
        colours: RingColors,
    ): DrawScope.(glow: Float, alpha: Float) -> Unit {
        val thread = Stroke(1.dp.toPx())
        val nodeLine = Stroke(0.75.dp.toPx())
        val node = 4.dp.toPx()
        // Each cube halfway round the ring to the next slot, by angle: two slots face each other across the ring, and
        // halfway between their centres is the ring's own.
        val angles = slots.map { (it - centre).let { at -> atan2(at.x, -at.y) * 180f / PI.toFloat() } }
        val nodes = angles.indices.map { index ->
            val gap = (angles[(index + 1) % angles.size] - angles[index]).mod(360f).takeIf { it > 0f } ?: 360f
            cube(centre + direction(angles[index] + gap / 2) * radius, node)
        }
        val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
        return { glow, alpha ->
            if (alpha > 0f) {
                clipPath(discs, ClipOp.Difference) {
                    drawCircle(colours.mark.copy(alpha = (0.3f + 0.4f * glow) * alpha), radius, centre, style = thread)
                    val face = colours.mark.copy(alpha = (0.15f + 0.2f * glow) * alpha)
                    val edge = colours.starLine.copy(alpha = (colours.starLine.alpha + 0.3f * glow) * alpha)
                    nodes.forEach { cube ->
                        drawPath(cube.top, face)
                        drawPath(cube.edges, edge, style = nodeLine)
                    }
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
            FolderLook.Octahedron -> Modifier.octahedron { style.minutes() } to Modifier.fillMaxSize(OCTAHEDRON_DISC)
            FolderLook.Geode -> Modifier.geode() to Modifier.fillMaxSize(GEODE_DISC)
            // Cubes, and a look of another theme's met before the theme's own is picked.
            else -> Modifier.glassCube() to Modifier.fillMaxSize(CUBE_DISC)
        }
        Box(modifier.then(worn), contentAlignment = Alignment.Center) { FolderDisc(folder, icon, presses, inner, disc) }
    }

    private val wireframes = DrawerTileArt { Wireframes(this, it) }

    override fun DrawScope.drawBackdrop(colour: Color, scrolled: Float) {
        if (colour.alpha <= 0f) return
        val faint = colour.copy(alpha = colour.alpha * 0.3f)
        forEachDrawerTile(scrolled) { origin, tile ->
            val built = with(wireframes) { of(tile) }
            translate(origin.x, origin.y) { drawPath(built.edges, faint, style = built.line) }
        }
    }
}

/** A glass cube seen corner on: its three faces, its edges as one path, and its outline. */
private class Cube(val top: Path, val left: Path, val right: Path, val edges: Path, val outline: Path) {
    fun draw(scope: DrawScope, line: Stroke, edge: Color = GlassLine) = with(scope) {
        drawPath(top, GlassTop)
        drawPath(left, GlassLeft)
        drawPath(right, GlassRight)
        drawPath(edges, edge, style = line)
    }
}

/** The six corners of a cube seen corner on round [centre], reaching [reach], clockwise from its top. */
private fun hexagon(centre: Offset, reach: Float) = List(6) { centre + direction(it * 60f) * reach }

private fun cube(centre: Offset, reach: Float): Cube {
    val (top, upperRight, lowerRight, bottom, lowerLeft, upperLeft) = hexagon(centre, reach)
    fun face(corners: List<Offset>) = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        corners.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    val outline = face(listOf(top, upperRight, lowerRight, bottom, lowerLeft, upperLeft))
    val edges = face(listOf(top, upperRight, lowerRight, bottom, lowerLeft, upperLeft)).apply {
        moveTo(upperLeft.x, upperLeft.y)
        lineTo(centre.x, centre.y)
        lineTo(upperRight.x, upperRight.y)
        moveTo(centre.x, centre.y)
        lineTo(bottom.x, bottom.y)
    }
    return Cube(
        top = face(listOf(top, upperRight, centre, upperLeft)),
        left = face(listOf(upperLeft, centre, bottom, lowerLeft)),
        right = face(listOf(centre, upperRight, lowerRight, bottom)),
        edges = edges,
        outline = outline,
    )
}

private operator fun <T> List<T>.component6() = this[5]

/** A glass cube filling the item, corner on, its outline edged in two lines so it shows on any wallpaper. */
private fun Modifier.glassCube(): Modifier = drawWithCache {
    val reach = size.minDimension / 2 * 1.1f
    val glass = cube(size.center, reach)
    val line = Stroke(1.dp.toPx())
    val outer = Stroke(3.dp.toPx())
    onDrawBehind {
        drawPath(glass.outline, CrystalEdge.outer, style = outer)
        glass.draw(this, line)
        drawPath(glass.outline, CrystalEdge.inner, style = line)
    }
}

/**
 * An octahedron filling the item, seen side on and turning about its points as the [minutes] go by: the faces turned to
 * the eye drawn in glass, the more lit the further they face left, and its outline edged in two lines.
 */
private fun Modifier.octahedron(minutes: () -> Float): Modifier = drawWithCache {
    val reach = size.minDimension / 2 * 1.12f
    val centre = size.center
    val apexTop = centre - Offset(0f, reach)
    val apexBottom = centre + Offset(0f, reach)
    val face = Path()
    val outline = Path()
    val line = Stroke(1.dp.toPx())
    val outer = Stroke(3.dp.toPx())
    val waist = reach * 0.8f
    val quarter = PI.toFloat() / 2
    fun DrawScope.drawFace(apex: Offset, from: Float, to: Float, glass: Color) {
        face.rewind()
        face.moveTo(apex.x, apex.y)
        face.lineTo(from, centre.y)
        face.lineTo(to, centre.y)
        face.close()
        drawPath(face, glass)
        drawPath(face, GlassLine, style = line)
    }
    onDrawBehind {
        val turned = minutes() * OCTAHEDRON_TURNS_PER_HOUR / 60f * 4 * quarter
        // Seen side on, the corners of its middle lie on one line, so its outline only reaches as wide as the widest.
        var widest = 0f
        for (corner in 0 until 4) widest = maxOf(widest, abs(cos(turned + corner * quarter)))
        outline.rewind()
        outline.moveTo(apexTop.x, apexTop.y)
        outline.lineTo(centre.x + waist * widest, centre.y)
        outline.lineTo(apexBottom.x, apexBottom.y)
        outline.lineTo(centre.x - waist * widest, centre.y)
        outline.close()
        drawPath(outline, CrystalEdge.outer, style = outer)
        for (corner in 0 until 4) {
            val middle = turned + (corner + 0.5f) * quarter
            // Only the faces turned to the eye, on the near side.
            if (sin(middle) <= 0f) continue
            val from = centre.x + cos(turned + corner * quarter) * waist
            val to = centre.x + cos(turned + (corner + 1) * quarter) * waist
            val light = (1f - cos(middle)) / 2
            drawFace(apexTop, from, to, lerp(GlassRight, GlassTop, light))
            drawFace(apexBottom, from, to, lerp(GlassRight, GlassLeft, light))
        }
        drawPath(outline, CrystalEdge.inner, style = line)
    }
}

/** A geode round the item: a stony rind edged in two lines, its dark hollow lined with crystals pointing in. */
private fun Modifier.geode(): Modifier = drawWithCache {
    val half = size.minDimension / 2
    val centre = size.center
    val rind = wavering(1.4f, 0.6f, centre, half * 1.12f)
    val hollow = wavering(1.4f, 0.6f, centre, half * 0.9f)
    val dark = Path()
    val light = Path()
    val shards = 18
    repeat(shards) { index ->
        val at = index * 360f / shards
        val tip = centre + direction(at + 10f) * (half * if (index % 2 == 0) 0.38f else 0.5f)
        val from = centre + direction(at) * (half * 0.86f)
        val to = centre + direction(at + 360f / shards) * (half * 0.86f)
        (if (index % 2 == 0) dark else light).apply {
            moveTo(from.x, from.y)
            lineTo(tip.x, tip.y)
            lineTo(to.x, to.y)
            close()
        }
    }
    val inner = Stroke(1.dp.toPx())
    val outer = Stroke(3.dp.toPx())
    onDrawBehind {
        drawPath(rind, GeodeRind)
        drawPath(hollow, GeodeHollow)
        clipPath(hollow) {
            drawPath(dark, GeodeShard)
            drawPath(light, GeodeShardLight)
        }
        drawPath(rind, CrystalEdge.outer, style = outer)
        drawPath(rind, CrystalEdge.inner, style = inner)
    }
}

/** A drawer tile scattered with wireframe cubes, the same on every tile and clear of its edges. */
private class Wireframes(density: Density, size: Size) {
    val line = Stroke(with(density) { 0.75.dp.toPx() })
    val edges = Path().apply {
        val random = Random(11)
        val unit = with(density) { 1.dp.toPx() }
        val margin = unit * 32
        repeat(14) {
            val at = Offset(margin + random.nextFloat() * (size.width - margin * 2), margin + random.nextFloat() * (size.height - margin * 2))
            addPath(cube(at, unit * (10 + random.nextFloat() * 14)).edges)
        }
    }
}

/**
 * The crystal city from straight above: streets on a dark ground, glass towers whose roofs lean out from the middle the
 * taller they are, light trails along two streets, an open plaza in the middle where the ring floats, and a square
 * under the clock, so it reads.
 */
private fun DrawScope.city() {
    val block = 64.dp.toPx()
    val inset = 8.dp.toPx()
    val plaza = Offset(size.width / 2, size.height * 0.56f)
    val clear = size.width * 0.44f
    val clock = Offset(size.width / 2, size.height * 0.13f)
    val clockReach = Offset(size.width * 0.5f, size.height * 0.11f)
    fun underClock(at: Offset) = ((at.x - clock.x) / clockReach.x).let { it * it } + ((at.y - clock.y) / clockReach.y).let { it * it } < 1f
    drawRect(Brush.verticalGradient(listOf(CitySky, CityGround)))
    drawCircle(Brush.radialGradient(listOf(PlazaGlow, Color.Transparent), plaza, clear * 1.4f), clear * 1.4f, plaza)

    val street = 1.dp.toPx()
    var x = (size.width / 2) % block
    while (x < size.width) {
        drawLine(CityStreet, Offset(x, 0f), Offset(x, size.height), street)
        x += block
    }
    var y = plaza.y % block
    while (y < size.height) {
        drawLine(CityStreet, Offset(0f, y), Offset(size.width, y), street)
        y += block
    }

    val random = Random(77)
    val towers = mutableListOf<Pair<Rect, Float>>()
    var top = plaza.y % block - block
    while (top < size.height) {
        var left = (size.width / 2) % block - block
        while (left < size.width) {
            val base = Rect(left + inset, top + inset, left + block - inset, top + block - inset)
            val height = random.nextFloat()
            if (height > 0.3f && (base.center - plaza).getDistance() > clear && !underClock(base.center)) towers += base to height
            left += block
        }
        top += block
    }
    val edge = Stroke(1.dp.toPx())
    val wall = Path()
    // The shorter first, so a taller tower's walls lie over its neighbours'.
    towers.sortedBy { it.second }.forEach { (base, height) ->
        val lean = 1f + height * 0.08f
        fun roofed(corner: Offset) = plaza + (corner - plaza) * lean
        val roof = Rect(roofed(base.topLeft), roofed(base.bottomRight))
        // From above, the walls that show are those facing the middle, which the roof leans away from.
        val sideX = if (base.center.x > plaza.x) base.left else base.right
        val sideY = if (base.center.y > plaza.y) base.top else base.bottom
        val roofX = if (base.center.x > plaza.x) roof.left else roof.right
        val roofY = if (base.center.y > plaza.y) roof.top else roof.bottom
        wall.rewind()
        wall.moveTo(sideX, base.top)
        wall.lineTo(sideX, base.bottom)
        wall.lineTo(roofX, roof.bottom)
        wall.lineTo(roofX, roof.top)
        wall.close()
        drawPath(wall, TowerWallDark)
        wall.rewind()
        wall.moveTo(base.left, sideY)
        wall.lineTo(base.right, sideY)
        wall.lineTo(roof.right, roofY)
        wall.lineTo(roof.left, roofY)
        wall.close()
        drawPath(wall, TowerWall)
        drawRect(TowerRoof, roof.topLeft, roof.size)
        drawRect(TowerEdge, roof.topLeft, roof.size, style = edge)
    }

    val trail = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
    val glow = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
    val trails = Path().apply {
        val across = plaza.y + block * 3
        moveTo(0f, across)
        lineTo(size.width, across)
        // On the street nearest a sixth of the way across, whatever the screen's width.
        val first = (size.width / 2) % block
        val down = first + ((size.width / 6 - first) / block).roundToInt().coerceAtLeast(0) * block
        moveTo(down, clock.y + clockReach.y)
        lineTo(down, size.height)
    }
    drawPath(trails, CityTrailGlow, style = glow)
    drawPath(trails, CityTrail, style = trail)
}
