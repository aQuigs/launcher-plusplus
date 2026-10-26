package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.Pixels
import com.sqftware.orbitlauncher.domain.alpha
import com.sqftware.orbitlauncher.domain.blue
import com.sqftware.orbitlauncher.domain.green
import com.sqftware.orbitlauncher.domain.lerpRgb
import com.sqftware.orbitlauncher.domain.opaque
import com.sqftware.orbitlauncher.domain.red
import com.sqftware.orbitlauncher.domain.rgb
import java.util.EnumSet
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** The minimap's side in pixels: the game's map disc. */
const val MINIMAP_SIZE = 152

/** Ground metres per minimap pixel. */
const val METRES_PER_PIXEL = 1.1

/** A 4 px square: the game's tile, which takes one ground colour. */
const val TILE_PX = 4

private const val WALL = 0xFFEEEEEE.toInt()
private const val DOOR = 0xFFEE0000.toInt()
private const val RAIL = 0xFF3A3024.toInt()
/** The grass's darker shade, as RGB. */
const val GRASS_DARK = 0x698F24
private const val GRASS_LIGHT = 0x718F24
private const val WATER_BASE = 0xFF2E5596.toInt()
private const val WATER_EDGE = 0xFF213E75.toInt()

private val GROUND_COLOUR = IntArray(Ground.entries.size).also { colours ->
    mapOf(
        Ground.Road to 0x85755A, Ground.Walk to 0x8B867A, Ground.Path to 0x85755A, Ground.Plaza to 0x6F6A5E,
        Ground.Parking to 0x85755A, Ground.Sand to 0xB4A26C, Ground.Pier to 0x866A45, Ground.FloorStone to 0x7A7468,
        Ground.FloorWood to 0x866C4C, Ground.Pitch to 0x6F9E2C,
    ).forEach { (ground, rgb) -> colours[ground.ordinal] = opaque(rgb) }
}

/** The ground grass blends into, so it shades toward a path over about a tile rather than meeting it at a hard edge. */
private val BLEND_GROUND = EnumSet.of(Ground.Grass, Ground.Road, Ground.Walk, Ground.Path, Ground.Plaza, Ground.Parking, Ground.Sand, Ground.Pitch)
private val NO_TREES = EnumSet.of(Ground.FloorStone, Ground.FloorWood, Ground.Water, Ground.Road, Ground.Parking)
private val FOOT_WAYS = EnumSet.of(WayKind.Walk, WayKind.Path, WayKind.Parking)

/** Share of the disc under buildings from which a place counts as built up, and from which its open ground is paved. */
private const val BUILT_UP_COVER = 0.15
private const val PAVED_COVER = 0.3

internal const val MAX_DOORS = 2
internal const val DOOR_PX = TILE_PX
private const val DOOR_REACH_PX = 6
internal const val TREE_SPACING_PX = 9.0
internal const val DECOR_SPACING_PX = 12.0
private val DECOR_CELL_PX = sqrt(500.0)
private const val DECOR_CLEAR_PX = 4
private const val DECOR_TRIES = 4
private const val DECOR_MIN_MAPPED = 5
private val WOOD_CELL_PX = sqrt(60.0)
private const val WOOD_FLOOR_SHARE = 0.3
/** How far apart a painting keeps its map icons, in its pixels. */
internal const val ICON_SPACING_PX = 24.0

/**
 * Paints the map of a [MapScene] in a [MapFrame], the minimap's disc or a stretch of the world map, at
 * [METRES_PER_PIXEL], laid out the way the old game builds its minimap and colored in [style]. In the Old School style every 4 px tile gets a flat ground color; walls are 1 px lines around each structure with a few
 * doors; trees and the dots of people go on top. The map icons and the self marker are left to whoever shows the map,
 * so they stay upright as the map turns. Seeded choices are keyed by places on Earth, so the map keeps its trees and grass as
 * it moves.
 */
class MinimapPainter(private val sprites: MinimapSprites, private val style: MapStyle) {
    /**
     * The ground in [frame], opaque, clear outside a round frame's circle, and its icons: apart, for the user's map to
     * keep upright as it turns and for Skyrim's to draw smoothly at a size of their own, or in the ground of the old
     * game's world map.
     */
    fun paint(scene: MapScene, frame: MapFrame): PaintedMap<Pixels> = Painting(scene, sprites, frame, style).run()
}

/**
 * The ground a painting has laid out, for a style to color, pixel by pixel of a [width] by [height] view: what each is
 * made of, whether it is a structure's wall, which structure it is in (0 for none), and whether a rail runs over it;
 * the spots [trees] stand on; and the [shade] of each pixel's grass, a smooth field from 0 to 1 keyed by places on Earth.
 */
internal class GroundLayout(
    val width: Int,
    val height: Int,
    val ground: Array<Ground>,
    val walls: BooleanArray,
    val structure: IntArray,
    val rails: BooleanArray,
    val trees: List<Px>,
    val shade: DoubleArray,
)

/** The world-wide pixel grid seeded features and the world map keep to, in pixels to the mercator metre. */
internal fun gridPixelsPerMetre(lat: Double) = WebMercator.gridScale(lat) / METRES_PER_PIXEL

/** [at] moved to the middle of its pixel of the seeded grid, so maps painted round places near it line up pixel for pixel. */
internal fun onSeedGrid(at: LatLon): LatLon {
    val scale = gridPixelsPerMetre(at.lat)
    val m = WebMercator.toMercator(at)
    return WebMercator.toLatLon(Mercator((floor(m.x * scale) + 0.5) / scale, -(floor(-m.y * scale) + 0.5) / scale))
}

internal class Px(val x: Double, val y: Double)

/**
 * Spots in a [width] by [height] view kept apart, filed by the grid of [cell] px squares they fall in, so a check looks
 * only at its neighbours.
 */
private class Spaced(private val cell: Double, width: Int, height: Int) {
    val all = mutableListOf<Px>()
    private val columns = ceil(width / cell).toInt()
    private val rows = ceil(height / cell).toInt()
    private val cells = arrayOfNulls<MutableList<Px>>(columns * rows)

    /** Whether no spot lies within [spacing] of [p], which must be at most the cell's side. */
    fun clear(p: Px, spacing: Double) = !near(p, spacing) { true }

    /** Whether a spot that passes [test] lies within [spacing] of [p], which must be at most the cell's side. */
    fun near(p: Px, spacing: Double, test: (Px) -> Boolean): Boolean {
        val i = (p.x / cell).toInt()
        val j = (p.y / cell).toInt()
        for (jj in maxOf(0, j - 1)..minOf(rows - 1, j + 1)) for (ii in maxOf(0, i - 1)..minOf(columns - 1, i + 1)) {
            if (cells[jj * columns + ii]?.any { sq(p.x - it.x) + sq(p.y - it.y) < sq(spacing) && test(it) } == true) return true
        }
        return false
    }

    operator fun plusAssign(p: Px) {
        all += p
        val i = (p.x / cell).toInt().coerceIn(0, columns - 1)
        val j = (p.y / cell).toInt().coerceIn(0, rows - 1)
        (cells[j * columns + i] ?: mutableListOf<Px>().also { cells[j * columns + i] = it }) += p
    }
}

/** How many pixels round each pixel of a painting are marked, and what share of them. */
private interface Around {
    fun count(i: Int): Int

    fun share(i: Int): Double
}

/** The same for every pixel: the marks over all [shown], as the minimap judges the place it shows as a whole. */
private class WholeView(marked: BooleanArray, shown: BooleanArray?) : Around {
    private val count = marked.indices.count { marked[it] && shown?.get(it) != false }
    private val share = count.toDouble() / (shown?.count { it } ?: marked.size)

    override fun count(i: Int) = count

    override fun share(i: Int) = share
}

/**
 * The marks in the minimap's span round each pixel of a [width] by [height] view, so the world map judges each place as
 * the minimap would there, alike in every painting.
 */
private class Window(marked: BooleanArray, private val width: Int, private val height: Int) : Around {
    // Summed over the rectangle above and left of each corner, so a window's count takes four reads.
    private val sums = IntArray((width + 1) * (height + 1)).also { sums ->
        for (y in 0 until height) {
            var row = 0
            for (x in 0 until width) {
                if (marked[y * width + x]) row++
                sums[(y + 1) * (width + 1) + x + 1] = sums[y * (width + 1) + x + 1] + row
            }
        }
    }

    override fun count(i: Int) = window(i) { x0, y0, x1, y1 -> sum(x0, y0, x1, y1) }

    override fun share(i: Int) = window(i) { x0, y0, x1, y1 -> sum(x0, y0, x1, y1).toDouble() / ((x1 - x0) * (y1 - y0)) }

    private inline fun <T> window(i: Int, of: (Int, Int, Int, Int) -> T): T {
        val x = i % width
        val y = i / width
        val half = MINIMAP_SIZE / 2
        return of(maxOf(0, x - half), maxOf(0, y - half), minOf(width, x + half), minOf(height, y + half))
    }

    private fun sum(x0: Int, y0: Int, x1: Int, y1: Int) =
        sums[y1 * (width + 1) + x1] - sums[y0 * (width + 1) + x1] - sums[y1 * (width + 1) + x0] + sums[y0 * (width + 1) + x0]
}

private class Painting(
    private val scene: MapScene,
    private val sprites: MinimapSprites,
    private val frame: MapFrame,
    private val style: MapStyle,
) {
    private val w = frame.width
    private val h = frame.height

    // The location sits on the middle of the middle pixel, as the game's minimap centres its player on a pixel.
    private val cx = w / 2 + 0.5
    private val cy = h / 2 + 0.5
    private val radius = minOf(w, h) / 2.0

    /**
     * Which of two places counts first where a rule can take only one: on a map that follows the user, the nearer them,
     * so what they stand by shows; elsewhere the one further north, then west, so every painting of a place agrees.
     */
    private val first: Comparator<Px> =
        if (frame.followsUser) compareBy { sq(it.x - cx) + sq(it.y - cy) } else compareBy({ it.y }, { it.x })

    // Where the view's top-left pixel lies in the world-wide grid seeded trees and grass keep to.
    private val originX: Int
    private val originY: Int

    init {
        val scale = gridPixelsPerMetre(scene.centre.lat)
        val centre = WebMercator.toMercator(scene.centre)
        originX = floor(centre.x * scale).toInt() - w / 2
        originY = floor(-centre.y * scale).toInt() - h / 2
    }

    private val tileX0 = Math.floorDiv(originX, TILE_PX)
    private val tileY0 = Math.floorDiv(originY, TILE_PX)
    private val twX = w / TILE_PX + 2
    private val twY = h / TILE_PX + 2

    private val cls = Array(w * h) { Ground.Grass }
    private val img = IntArray(w * h)

    private fun px(p: Local) = Px(cx + p.east / METRES_PER_PIXEL, cy - p.north / METRES_PER_PIXEL)

    private fun pxRings(polygon: Polygon) = polygon.map { ring -> ring.map(::px) }

    fun run(): PaintedMap<Pixels> {
        // 1. footprints, whose cover says whether the place is built up
        val bid = IntArray(w * h)
        scene.buildings.forEachIndexed { i, b -> scanPolygon(pxRings(b.polygon)) { bid[it] = i + 1 } }
        val cover = around(BooleanArray(w * h) { bid[it] != 0 })
        for (i in 0 until w * h) cls[i] = if (cover.share(i) >= PAVED_COVER) Ground.Plaza else Ground.Grass

        // 2. ground: areas biggest first, then piers, paths and roads, the widest last
        scene.areas.map { it to areaOf(it) }
            .sortedWith(compareBy({ -it.second }, { it.first.order }))
            .forEach { (area, _) ->
                area.polygons.forEach { polygon ->
                    scanPolygon(pxRings(polygon)) { if (!area.builtUpOnly || cover.share(it) >= BUILT_UP_COVER) cls[it] = area.ground }
                }
            }
        val strokes = scene.ways.filter { it.kind == WayKind.Pier } +
            scene.ways.filter { it.kind in FOOT_WAYS } +
            scene.ways.filter { it.kind == WayKind.Road }.sortedBy { it.widthM }
        strokes.forEach { strokeBuffer(it.points.map(::px), it.widthM / METRES_PER_PIXEL, groundOf(it.kind)) }

        // 3. touching footprints merge into one structure with one floor and only its outer wall, as a game building is
        // one walled shape rather than a wall per footprint
        val comp = IntArray(w * h)
        var structures = 0
        val stack = IntArray(w * h)
        val members = IntArray(w * h)
        for (i in 0 until w * h) {
            if (bid[i] == 0 || comp[i] != 0) continue
            val mark = ++structures
            var count = 0
            var top = 0
            comp[i] = mark
            stack[top++] = i
            while (top > 0) {
                val j = stack[--top]
                members[count++] = j
                forNeighbours(j) { k ->
                    if (bid[k] != 0 && comp[k] == 0) {
                        comp[k] = mark
                        stack[top++] = k
                    }
                }
            }
            var seed = Long.MAX_VALUE
            for (m in 0 until count) seed = minOf(seed, scene.buildings[bid[members[m]] - 1].seed)
            val floor = if (stableRandom(seed) < WOOD_FLOOR_SHARE) Ground.FloorWood else Ground.FloorStone
            for (m in 0 until count) cls[members[m]] = floor
        }
        val walls = BooleanArray(w * h) { comp[it] != 0 && anyNeighbour(it) { k -> comp[k] == 0 } }
        val rails = BooleanArray(w * h).also { rails ->
            scene.ways.filter { it.kind == WayKind.Rail }.forEach { rail -> linePixels(rail.points.map(::px)) { rails[it] = true } }
        }
        val noise = grassNoise()
        val layout = GroundLayout(w, h, cls, walls, comp, rails, treeSpots(), DoubleArray(w * h) { noise[tileOf(it % w, it / w)] })

        val icons = when (style) {
            MapStyle.OldSchool -> {
                paintOldSchool(layout)
                placeIcons().also { icons ->
                    paintNpcs(icons)
                    paintFigures()
                }
            }
            MapStyle.Skyrim -> {
                paintSkyrim(layout, img)
                placeIcons()
            }
        }

        frame.disc?.let { disc -> for (i in 0 until w * h) if (!disc[i]) img[i] = 0 }
        // The local map's icons are drawn smoothly over its ground at a size of their own; the old game's world map's go into it.
        if (frame.followsUser || style == MapStyle.Skyrim) return PaintedMap(scene.centre, Pixels(w, h, img), icons, scene.place)
        icons.forEach { blit(it.image, it.x.toInt() - it.image.width / 2, it.y.toInt() - it.image.height / 2) }
        return PaintedMap(scene.centre, Pixels(w, h, img), emptyList())
    }

    private fun paintOldSchool(layout: GroundLayout) {
        // flat tiles: one color per ground, grass shaded only by a smooth field across tiles
        for (i in 0 until w * h) {
            img[i] = when (val g = cls[i]) {
                Ground.Grass -> lerpRgb(GRASS_DARK, GRASS_LIGHT, layout.shade[i])
                Ground.Water -> if (anyNeighbour(i) { cls[it] != Ground.Water }) WATER_EDGE else WATER_BASE
                else -> GROUND_COLOUR[g.ordinal]
            }
        }
        blendGrass()

        // thin lines, then the walls on top
        for (i in 0 until w * h) {
            if (layout.rails[i]) img[i] = RAIL
            if (layout.walls[i]) img[i] = WALL
        }

        paintDoors(layout.walls, layout.structure)
        paintTrees(layout.trees)
    }

    private fun groundOf(kind: WayKind) = when (kind) {
        WayKind.Road -> Ground.Road
        WayKind.Walk -> Ground.Walk
        WayKind.Path -> Ground.Path
        WayKind.Parking -> Ground.Parking
        WayKind.Pier -> Ground.Pier
        WayKind.Rail -> throw IllegalArgumentException("A rail is a line on the ground, not ground")
    }

    private fun areaOf(area: Area) = area.polygons.sumOf { polygon -> polygon.sumOf { abs(shoelace(it)) } }

    private fun shoelace(ring: List<Local>): Double {
        var sum = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            sum += a.east * b.north - b.east * a.north
        }
        return sum / 2
    }

    private inline fun forNeighbours(i: Int, visit: (Int) -> Unit) {
        val x = i % w
        if (x + 1 < w) visit(i + 1)
        if (x > 0) visit(i - 1)
        if (i + w < w * h) visit(i + w)
        if (i >= w) visit(i - w)
    }

    private inline fun anyNeighbour(i: Int, test: (Int) -> Boolean): Boolean {
        forNeighbours(i) { if (test(it)) return true }
        return false
    }

    // --- grass ---------------------------------------------------------------------------------

    private fun tileOf(x: Int, y: Int) = (Math.floorDiv(y + originY, TILE_PX) - tileY0) * twX + Math.floorDiv(x + originX, TILE_PX) - tileX0

    /**
     * A smooth field over the view's tiles, from 0 to 1: noise seeded per world tile, blurred three times. Its stretch
     * is fixed rather than fitted to the view, as the noise must not change with what is in view.
     */
    private fun grassNoise(): DoubleArray {
        val margin = 3
        val spanX = twX + 2 * margin
        val spanY = twY + 2 * margin
        val tx0 = tileX0 - margin
        val ty0 = tileY0 - margin
        var v = DoubleArray(spanX * spanY) { stableRandom(NOISE_SALT, (tx0 + it % spanX).toLong(), (ty0 + it / spanX).toLong()) }
        repeat(3) {
            val n = DoubleArray(spanX * spanY)
            for (y in 0 until spanY) for (x in 0 until spanX) {
                var s = 0.0
                var count = 0
                for (yy in maxOf(0, y - 1)..minOf(spanY - 1, y + 1)) for (xx in maxOf(0, x - 1)..minOf(spanX - 1, x + 1)) {
                    s += v[yy * spanX + xx]
                    count++
                }
                n[y * spanX + x] = s / count
            }
            v = n
        }
        return DoubleArray(twX * twY) { (0.5 + (v[(it / twX + margin) * spanX + it % twX + margin] - 0.5) * NOISE_STRETCH).coerceIn(0.0, 1.0) }
    }

    // Underlay blending: grass takes the mean of its own and its 8 neighbouring tiles' ground.
    private fun blendGrass() {
        val sums = Array(twX * twY) { IntArray(4) }
        for (i in 0 until w * h) if (cls[i] in BLEND_GROUND) {
            val acc = sums[tileOf(i % w, i / w)]
            acc[0] += red(img[i])
            acc[1] += green(img[i])
            acc[2] += blue(img[i])
            acc[3]++
        }
        val blended = IntArray(twX * twY)
        for (ty in 0 until twY) for (tx in 0 until twX) {
            var r = 0.0
            var g = 0.0
            var b = 0.0
            var n = 0
            for (yy in maxOf(0, ty - 1)..minOf(twY - 1, ty + 1)) for (xx in maxOf(0, tx - 1)..minOf(twX - 1, tx + 1)) {
                val acc = sums[yy * twX + xx]
                if (acc[3] == 0) continue
                r += acc[0].toDouble() / acc[3]
                g += acc[1].toDouble() / acc[3]
                b += acc[2].toDouble() / acc[3]
                n++
            }
            if (n > 0) blended[ty * twX + tx] = rgb((r / n).roundToInt(), (g / n).roundToInt(), (b / n).roundToInt())
        }
        for (i in 0 until w * h) if (cls[i] == Ground.Grass) img[i] = blended[tileOf(i % w, i / w)]
    }

    // --- doors ---------------------------------------------------------------------------------

    /** Each entered place turns one tile's length of the nearest wall red, at most [MAX_DOORS] per structure. */
    private fun paintDoors(walls: BooleanArray, comp: IntArray) {
        val doorsPer = mutableMapOf<Int, Int>()
        // In a fixed order, so which places get the doors does not depend on the order they came in.
        for (p in scene.pois.filter { it.door }.map { px(it.at) }.sortedWith(first)) {
            var start = -1
            var bestD = Double.MAX_VALUE
            for (y in maxOf(0, floor(p.y).toInt() - DOOR_REACH_PX)..minOf(h - 1, floor(p.y).toInt() + DOOR_REACH_PX)) {
                for (x in maxOf(0, floor(p.x).toInt() - DOOR_REACH_PX)..minOf(w - 1, floor(p.x).toInt() + DOOR_REACH_PX)) {
                    val i = y * w + x
                    if (!walls[i]) continue
                    val d = abs(x + 0.5 - p.x) + abs(y + 0.5 - p.y)
                    if (d < bestD) {
                        bestD = d
                        start = i
                    }
                }
            }
            if (start < 0 || (doorsPer[comp[start]] ?: 0) >= MAX_DOORS || img[start] == DOOR) continue
            val run = mutableListOf(start)
            val frontier = ArrayDeque<Int>().apply { add(start) }
            while (frontier.isNotEmpty() && run.size < DOOR_PX) {
                val j = frontier.removeFirst()
                for ((dx, dy) in EIGHT_WAYS) {
                    val x = j % w + dx
                    val k = j + dy * w + dx
                    if (x in 0 until w && k in 0 until w * h && walls[k] && k !in run && comp[k] == comp[start] && run.size < DOOR_PX) {
                        run += k
                        frontier += k
                    }
                }
            }
            run.forEach { img[it] = DOOR }
            doorsPer[comp[start]] = (doorsPer[comp[start]] ?: 0) + 1
        }
    }

    // --- trees ---------------------------------------------------------------------------------

    private fun paintTrees(trees: List<Px>) {
        if (sprites.trees.isEmpty()) return
        for (t in trees.sortedBy { it.y }) {
            val pick = stableRandom(TREE_SALT, (round(t.x).toInt() + originX).toLong(), (round(t.y).toInt() + originY).toLong())
            val sprite = sprites.trees[(pick * sprites.trees.size).toInt().coerceAtMost(sprites.trees.size - 1)]
            blit(sprite, t.x.toInt() - sprite.width / 2, t.y.toInt() - (sprite.height - 2))
        }
    }

    /** Where trees stand: in woods, and scattered as decoration where the map gives none. */
    private fun treeSpots(): List<Px> {
        // Woods get a tree in about one world cell of 60 px² each, wherever the cell's seeded spot falls on their grass.
        val wood = BooleanArray(w * h)
        scene.areas.filter { it.woodland }.forEach { area -> area.polygons.forEach { polygon -> scanPolygon(pxRings(polygon)) { wood[it] = true } } }
        val spots = mutableListOf<Px>()
        seededSpots(WOOD_CELL_PX, WOOD_SALT, tries = 1) { x, y ->
            val i = y.toInt() * w + x.toInt()
            if (wood[i] && cls[i] == Ground.Grass) spots += Px(x, y)
            true
        }
        val mapped = around(BooleanArray(w * h).also { marks -> spots.forEach { marks[it.y.toInt() * w + it.x.toInt()] = true } })
        val placed = Spaced(DECOR_SPACING_PX, w, h)
        standing(spots.filter { cls[it.y.toInt() * w + it.x.toInt()] !in NO_TREES }, TREE_SPACING_PX).forEach { placed += it }

        // Decoration where the map gives no trees: a few seeded spots per world cell, the first clear one standing.
        val decor = mutableListOf<Px>()
        seededSpots(DECOR_CELL_PX, DECOR_SALT, DECOR_TRIES) { x, y ->
            val xi = x.toInt()
            val yi = y.toInt()
            if (mapped.count(yi * w + xi) >= DECOR_MIN_MAPPED) return@seededSpots true
            for (yy in maxOf(0, yi - 6 - DECOR_CLEAR_PX) until minOf(h, yi + 2 + DECOR_CLEAR_PX)) {
                for (xx in maxOf(0, xi - 4 - DECOR_CLEAR_PX) until minOf(w, xi + 4 + DECOR_CLEAR_PX)) {
                    if (cls[yy * w + xx].let { it != Ground.Grass && it != Ground.Pitch }) return@seededSpots false
                }
            }
            decor += Px(x, y)
            true
        }
        standing(decor, DECOR_SPACING_PX).filter { placed.clear(it, DECOR_SPACING_PX) }.forEach { placed += it }
        return placed.all
    }

    /**
     * The [spots] that stand: each but those with a spot of higher seeded precedence, keyed by its place on Earth, closer
     * than [spacing]. Whether a spot stands depends on its neighbours alone, so a step of the map that brings a spot in
     * at the view's edge never reshuffles the trees in the middle, as placing them in turn would.
     */
    private fun standing(spots: List<Px>, spacing: Double): List<Px> {
        fun precedence(p: Px) = stableRandom(PRECEDENCE_SALT, (round(p.x).toInt() + originX).toLong(), (round(p.y).toInt() + originY).toLong())
        val all = Spaced(spacing, w, h).also { grid -> spots.forEach { grid += it } }
        return spots.filter { s -> precedence(s).let { mine -> !all.near(s, spacing) { precedence(it) > mine } } }
    }

    /**
     * Up to [tries] seeded spots in each world cell of [cell] px over the view, handed to [take] in view pixels until it
     * takes one. A spot off the view ends its cell's tries.
     */
    private inline fun seededSpots(cell: Double, salt: Long, tries: Int, take: (Double, Double) -> Boolean) {
        for (cj in floor(originY / cell).toInt()..floor((originY + h) / cell).toInt()) {
            for (ci in floor(originX / cell).toInt()..floor((originX + w) / cell).toInt()) {
                for (attempt in 0 until tries) {
                    val x = (ci + stableRandom(salt, 1, ci.toLong(), cj.toLong(), attempt.toLong())) * cell - originX
                    val y = (cj + stableRandom(salt, 2, ci.toLong(), cj.toLong(), attempt.toLong())) * cell - originY
                    if (x < 0 || y < 0 || x >= w || y >= h || take(x, y)) break
                }
            }
        }
    }

    // --- icons and people ----------------------------------------------------------------------

    private class Candidate(val at: Px, val icon: MapIcon, val sprite: Pixels)

    /** Map icons decluttered by priority, then in the order [first] gives. */
    private fun placeIcons(): List<MapMark<Pixels>> {
        val candidates = scene.pois.mapNotNull { poi ->
            val icon = poi.icon ?: return@mapNotNull null
            val sprite = sprites.icons[icon] ?: return@mapNotNull null
            val p = px(poi.at)
            Candidate(p, icon, sprite).takeIf { inside(p, 9.0) && !onUser(p, 8.0) }
        }.sortedWith(compareBy<Candidate> { it.icon.priority }.thenBy(first) { it.at })
        val icons = mutableListOf<Candidate>()
        val spaced = Spaced(ICON_SPACING_PX, w, h)
        val perKind = mutableMapOf<MapIcon, Int>()
        for (cand in candidates) {
            if (icons.size >= frame.maxIcons || (perKind[cand.icon] ?: 0) >= frame.maxPerIcon) continue
            if (!spaced.clear(cand.at, ICON_SPACING_PX)) continue
            icons += cand
            spaced += cand.at
            perKind[cand.icon] = (perKind[cand.icon] ?: 0) + 1
        }
        return icons.map { MapMark(it.sprite, it.at.x.toFloat(), it.at.y.toFloat()) }
    }

    /** Yellow dots where the map has someone to meet; the white self marker stays the only white mark. */
    private fun paintNpcs(icons: List<MapMark<Pixels>>) {
        val sprite = sprites.npc ?: return
        if (frame.maxNpcs == 0) return
        val dots = mutableListOf<Pair<Int, Int>>()
        // In a fixed order, so which people make the cap does not depend on the order they came in.
        for (p in scene.pois.filter { it.npc }.map { px(it.at) }.sortedWith(first)) {
            val x = floor(p.x).toInt()
            val y = floor(p.y).toInt()
            val at = Px(x.toDouble(), y.toDouble())
            if (!inside(at, 4.0) || onUser(at, 5.0)) continue
            if (dots.size >= frame.maxNpcs || dots.any { (a, b) -> abs(x - a) < 5 && abs(y - b) < 5 }) continue
            if (icons.any { hypot(x - it.x.toDouble(), y - it.y.toDouble()) < 9 }) continue
            dots += x to y
        }
        dots.forEach { (x, y) -> blit(sprite, x - sprite.width / 2, y - sprite.height / 2) }
    }

    /**
     * Everyone moving about, players over NPCs as the game stacks its dots. Unlike the mapped people, none are left out
     * to declutter, so a dot passing another never blinks out.
     */
    private fun paintFigures() {
        if (frame.maxNpcs == 0) return
        for (dot in Dot.entries) {
            val sprite = (if (dot == Dot.Npc) sprites.npc else sprites.player) ?: continue
            for (figure in scene.figures.filter { it.dot == dot }) {
                val p = px(figure.at)
                blit(sprite, floor(p.x).toInt() - sprite.width / 2, floor(p.y).toInt() - sprite.height / 2)
            }
        }
    }

    private fun blit(sprite: Pixels, ox: Int, oy: Int) {
        for (y in 0 until sprite.height) for (x in 0 until sprite.width) {
            val argb = sprite[x, y]
            val tx = ox + x
            val ty = oy + y
            if (alpha(argb) < 128 || tx !in 0 until w || ty !in 0 until h) continue
            img[ty * w + tx] = opaque(argb)
        }
    }

    /** Whether [p] is in view at least [margin] px in from its edge. */
    private fun inside(p: Px, margin: Double) =
        if (frame.round) hypot(p.x - cx, p.y - cy) < radius - margin else p.x >= margin && p.y >= margin && p.x < w - margin && p.y < h - margin

    /** Whether [p] is within [reach] px of the user's marker, which nothing else goes under. */
    private fun onUser(p: Px, reach: Double) = frame.followsUser && hypot(p.x - cx, p.y - cy) < reach

    /**
     * Where a rule weighs the [marked] pixels round a place: the whole of the user's map, as the minimap judges the
     * place it shows; otherwise the minimap's span round each pixel.
     */
    private fun around(marked: BooleanArray): Around = if (frame.followsUser) WholeView(marked, frame.disc) else Window(marked, w, h)

    // --- raster primitives ---------------------------------------------------------------------

    /** Even-odd scanline fill of [rings] sampled at pixel centres, so edges stay hard; [set] gets each pixel inside. */
    private inline fun scanPolygon(rings: List<List<Px>>, set: (Int) -> Unit) {
        val edgeCount = rings.sumOf { it.size }
        val edges = DoubleArray(edgeCount * 4)
        var n = 0
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        for (ring in rings) for (k in ring.indices) {
            val a = ring[k]
            val b = ring[(k + 1) % ring.size]
            if (a.y == b.y) continue
            edges[n++] = a.x
            edges[n++] = a.y
            edges[n++] = b.x
            edges[n++] = b.y
            minY = minOf(minY, a.y, b.y)
            maxY = maxOf(maxY, a.y, b.y)
            minX = minOf(minX, a.x, b.x)
            maxX = maxOf(maxX, a.x, b.x)
        }
        if (n == 0 || maxY < 0 || minY >= h || maxX < 0 || minX >= w) return
        val xs = DoubleArray(n / 4)
        for (y in maxOf(0, minY.toInt())..minOf(h - 1, maxY.toInt())) {
            val sy = y + 0.5
            var count = 0
            for (e in 0 until n step 4) {
                val ay = edges[e + 1]
                val by = edges[e + 3]
                if ((ay <= sy) != (by <= sy)) {
                    val ax = edges[e]
                    xs[count++] = ax + (sy - ay) * (edges[e + 2] - ax) / (by - ay)
                }
            }
            java.util.Arrays.sort(xs, 0, count)
            for (k in 0 until count - 1 step 2) {
                for (x in maxOf(0, ceilHalf(xs[k])) until minOf(w, ceilHalf(xs[k + 1]))) set(y * w + x)
            }
        }
    }

    private fun ceilHalf(v: Double) = ceil(v - 0.5).toInt()

    private fun strokeBuffer(pts: List<Px>, widthPx: Double, value: Ground) {
        val r = maxOf(widthPx / 2, 0.5)
        for (k in 0 until pts.size - 1) {
            val a = pts[k]
            val b = pts[k + 1]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val ll = (dx * dx + dy * dy).takeIf { it > 0 } ?: 1e-9
            for (y in maxOf(0, (minOf(a.y, b.y) - r).toInt())..minOf(h - 1, (maxOf(a.y, b.y) + r).toInt())) {
                for (x in maxOf(0, (minOf(a.x, b.x) - r).toInt())..minOf(w - 1, (maxOf(a.x, b.x) + r).toInt())) {
                    val px = x + 0.5
                    val py = y + 0.5
                    val t = (((px - a.x) * dx + (py - a.y) * dy) / ll).coerceIn(0.0, 1.0)
                    if (sq(px - a.x - t * dx) + sq(py - a.y - t * dy) <= r * r) cls[y * w + x] = value
                }
            }
        }
    }

    /** Bresenham along a polyline, the 1 px line the game draws, handing [set] each pixel of it in the view. */
    private inline fun linePixels(pts: List<Px>, set: (Int) -> Unit) {
        for (k in 0 until pts.size - 1) {
            var x0 = floor(pts[k].x).toInt()
            var y0 = floor(pts[k].y).toInt()
            val x1 = floor(pts[k + 1].x).toInt()
            val y1 = floor(pts[k + 1].y).toInt()
            // A segment far outside the view would walk thousands of pixels for nothing.
            if (maxOf(x0, x1) < 0 || minOf(x0, x1) >= w || maxOf(y0, y1) < 0 || minOf(y0, y1) >= h) continue
            val dx = abs(x1 - x0)
            val dy = -abs(y1 - y0)
            val sx = if (x0 < x1) 1 else -1
            val sy = if (y0 < y1) 1 else -1
            var err = dx + dy
            while (true) {
                if (x0 in 0 until w && y0 in 0 until h) set(y0 * w + x0)
                if (x0 == x1 && y0 == y1) break
                val e2 = 2 * err
                if (e2 >= dy) {
                    err += dy
                    x0 += sx
                }
                if (e2 <= dx) {
                    err += dx
                    y0 += sy
                }
            }
        }
    }

    private companion object {
        val EIGHT_WAYS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to -1, 1 to -1, -1 to 1)
        const val NOISE_SALT = 1L
        const val WOOD_SALT = 2L
        const val DECOR_SALT = 3L
        const val TREE_SALT = 4L
        const val PRECEDENCE_SALT = 5L

        // Three 3x3 box blurs leave uniform noise a spread of about ±0.17 over a view; this stretches it back to 0..1.
        const val NOISE_STRETCH = 3.0
    }
}

private fun sq(v: Double) = v * v

/** A number in [0, 1) that depends only on [key]: SplitMix64 over its parts. */
private fun stableRandom(vararg key: Long): Double {
    var h = 0x9E3779B97F4A7C15uL.toLong()
    for (k in key) h = mix(h xor k)
    return (h ushr 11).toDouble() / (1L shl 53).toDouble()
}

private fun mix(seed: Long): Long {
    var z = seed + 0x9E3779B97F4A7C15uL.toLong()
    z = (z xor (z ushr 30)) * 0xBF58476D1CE4E5B9uL.toLong()
    z = (z xor (z ushr 27)) * 0x94D049BB133111EBuL.toLong()
    return z xor (z ushr 31)
}
