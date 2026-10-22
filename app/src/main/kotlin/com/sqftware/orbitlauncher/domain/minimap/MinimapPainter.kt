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

private const val N = MINIMAP_SIZE

private const val WALL = 0xFFEEEEEE.toInt()
private const val DOOR = 0xFFEE0000.toInt()
private const val RAIL = 0xFF3A3024.toInt()
/** The grass's darker shade, as RGB: what shows before there is a map. */
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
internal const val MAX_ICONS = 4
internal const val MAX_PER_ICON = 2
private const val ICON_SPACING_PX = 24.0
internal const val MAX_NPCS = 10

// The location sits on the middle of pixel N / 2, as the game's minimap centres its player on a pixel.
private const val C = N / 2 + 0.5
private const val RADIUS = N / 2.0

private val DISC = BooleanArray(N * N) {
    val dx = it % N + 0.5 - RADIUS
    val dy = it / N + 0.5 - RADIUS
    dx * dx + dy * dy <= RADIUS * RADIUS
}
private val DISC_PIXELS = DISC.count { it }

/**
 * Paints the Old School minimap of a [MapScene]: a [MINIMAP_SIZE] px disc at [METRES_PER_PIXEL], built the way the game
 * builds its minimap. Every 4 px tile gets a flat ground colour; walls are 1 px lines round each structure with a few
 * doors; trees and the dots of people go on top. The map icons and the self marker are left to whoever shows the map,
 * so they stay upright as the map turns. Seeded choices are keyed by places on Earth, so the map keeps its trees and grass as
 * it moves.
 */
class MinimapPainter(private val sprites: MinimapSprites) {
    /** The disc, opaque inside the circle and clear outside it, and where its icons go. */
    fun paint(scene: MapScene): Minimap<Pixels> = Painting(scene, sprites).run()
}

private class Px(val x: Double, val y: Double)

private class Painting(private val scene: MapScene, private val sprites: MinimapSprites) {
    // Where the view's top-left pixel lies in a world-wide pixel grid. The grid's scale only changes a whole degree of
    // latitude away, so seeded trees and grass stay put as the view moves.
    private val originX: Int
    private val originY: Int

    init {
        val seedScale = WebMercator.groundScale(round(scene.centre.lat)) / METRES_PER_PIXEL
        val centre = WebMercator.toMercator(scene.centre)
        originX = floor(centre.x * seedScale).toInt() - N / 2
        originY = floor(-centre.y * seedScale).toInt() - N / 2
    }

    private val tileX0 = Math.floorDiv(originX, TILE_PX)
    private val tileY0 = Math.floorDiv(originY, TILE_PX)
    private val tw = N / TILE_PX + 2

    private val cls = Array(N * N) { Ground.Grass }
    private val img = IntArray(N * N)

    private fun px(p: Local) = Px(C + p.east / METRES_PER_PIXEL, C - p.north / METRES_PER_PIXEL)

    private fun pxRings(polygon: Polygon) = polygon.map { ring -> ring.map(::px) }

    fun run(): Minimap<Pixels> {
        // 1. footprints, whose cover says whether the place is built up
        val bid = IntArray(N * N)
        scene.buildings.forEachIndexed { i, b -> scanPolygon(pxRings(b.polygon)) { bid[it] = i + 1 } }
        val cover = (0 until N * N).count { bid[it] != 0 && DISC[it] }.toDouble() / DISC_PIXELS
        val builtUp = cover >= BUILT_UP_COVER
        cls.fill(if (cover >= PAVED_COVER) Ground.Plaza else Ground.Grass)

        // 2. ground: areas biggest first, then piers, paths and roads, the widest last
        scene.areas.filter { builtUp || !it.builtUpOnly }
            .map { it to areaOf(it) }
            .sortedWith(compareBy({ -it.second }, { it.first.order }))
            .forEach { (area, _) -> area.polygons.forEach { polygon -> scanPolygon(pxRings(polygon)) { cls[it] = area.ground } } }
        val strokes = scene.ways.filter { it.kind == WayKind.Pier } +
            scene.ways.filter { it.kind in FOOT_WAYS } +
            scene.ways.filter { it.kind == WayKind.Road }.sortedBy { it.widthM }
        strokes.forEach { strokeBuffer(it.points.map(::px), it.widthM / METRES_PER_PIXEL, groundOf(it.kind)) }

        // 3. touching footprints merge into one structure with one floor and only its outer wall, as a game building is
        // one walled shape rather than a wall per footprint
        val comp = IntArray(N * N)
        var structures = 0
        val stack = IntArray(N * N)
        val members = IntArray(N * N)
        for (i in 0 until N * N) {
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
        val walls = BooleanArray(N * N) { comp[it] != 0 && anyNeighbour(it) { k -> comp[k] == 0 } }

        // 4. flat tiles: one colour per ground, grass shaded only by a smooth field across tiles
        val noise = grassNoise()
        for (i in 0 until N * N) {
            img[i] = when (val g = cls[i]) {
                Ground.Grass -> lerpRgb(GRASS_DARK, GRASS_LIGHT, noise[tileOf(i % N, i / N)])
                Ground.Water -> if (anyNeighbour(i) { cls[it] != Ground.Water }) WATER_EDGE else WATER_BASE
                else -> GROUND_COLOUR[g.ordinal]
            }
        }
        blendGrass()

        // 5. thin lines, then the walls on top
        scene.ways.filter { it.kind == WayKind.Rail }.forEach { rail -> linePixels(rail.points.map(::px)) { img[it] = RAIL } }
        for (i in 0 until N * N) if (walls[i]) img[i] = WALL

        paintDoors(walls, comp)
        paintTrees()
        val icons = placeIcons()
        paintNpcs(icons)

        for (i in 0 until N * N) if (!DISC[i]) img[i] = 0
        return Minimap(Pixels(N, N, img), icons)
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
        val x = i % N
        if (x + 1 < N) visit(i + 1)
        if (x > 0) visit(i - 1)
        if (i + N < N * N) visit(i + N)
        if (i >= N) visit(i - N)
    }

    private inline fun anyNeighbour(i: Int, test: (Int) -> Boolean): Boolean {
        forNeighbours(i) { if (test(it)) return true }
        return false
    }

    // --- grass ---------------------------------------------------------------------------------

    private fun tileOf(x: Int, y: Int) = (Math.floorDiv(y + originY, TILE_PX) - tileY0) * tw + Math.floorDiv(x + originX, TILE_PX) - tileX0

    /**
     * A smooth field over the view's tiles, from 0 to 1: noise seeded per world tile, blurred three times. Its stretch
     * is fixed rather than fitted to the view, as the noise must not change with what is in view.
     */
    private fun grassNoise(): DoubleArray {
        val margin = 3
        val span = tw + 2 * margin
        val tx0 = tileX0 - margin
        val ty0 = tileY0 - margin
        var v = DoubleArray(span * span) { stableRandom(NOISE_SALT, (tx0 + it % span).toLong(), (ty0 + it / span).toLong()) }
        repeat(3) {
            val n = DoubleArray(span * span)
            for (y in 0 until span) for (x in 0 until span) {
                var s = 0.0
                var count = 0
                for (yy in maxOf(0, y - 1)..minOf(span - 1, y + 1)) for (xx in maxOf(0, x - 1)..minOf(span - 1, x + 1)) {
                    s += v[yy * span + xx]
                    count++
                }
                n[y * span + x] = s / count
            }
            v = n
        }
        return DoubleArray(tw * tw) { (0.5 + (v[(it / tw + margin) * span + it % tw + margin] - 0.5) * NOISE_STRETCH).coerceIn(0.0, 1.0) }
    }

    // Underlay blending: grass takes the mean of its own and its 8 neighbouring tiles' ground.
    private fun blendGrass() {
        val sums = Array(tw * tw) { IntArray(4) }
        for (i in 0 until N * N) if (cls[i] in BLEND_GROUND) {
            val acc = sums[tileOf(i % N, i / N)]
            acc[0] += red(img[i])
            acc[1] += green(img[i])
            acc[2] += blue(img[i])
            acc[3]++
        }
        val blended = IntArray(tw * tw)
        for (ty in 0 until tw) for (tx in 0 until tw) {
            var r = 0.0
            var g = 0.0
            var b = 0.0
            var n = 0
            for (yy in maxOf(0, ty - 1)..minOf(tw - 1, ty + 1)) for (xx in maxOf(0, tx - 1)..minOf(tw - 1, tx + 1)) {
                val acc = sums[yy * tw + xx]
                if (acc[3] == 0) continue
                r += acc[0].toDouble() / acc[3]
                g += acc[1].toDouble() / acc[3]
                b += acc[2].toDouble() / acc[3]
                n++
            }
            if (n > 0) blended[ty * tw + tx] = rgb((r / n).roundToInt(), (g / n).roundToInt(), (b / n).roundToInt())
        }
        for (i in 0 until N * N) if (cls[i] == Ground.Grass) img[i] = blended[tileOf(i % N, i / N)]
    }

    // --- doors ---------------------------------------------------------------------------------

    /** Each entered place turns one tile's length of the nearest wall red, at most [MAX_DOORS] per structure. */
    private fun paintDoors(walls: BooleanArray, comp: IntArray) {
        val doorsPer = mutableMapOf<Int, Int>()
        // Nearest first, so which places get the doors does not depend on the order they came in.
        for (poi in scene.pois.filter { it.door }.sortedBy { it.at.distanceSq() }) {
            val p = px(poi.at)
            var start = -1
            var bestD = Double.MAX_VALUE
            for (y in maxOf(0, floor(p.y).toInt() - DOOR_REACH_PX)..minOf(N - 1, floor(p.y).toInt() + DOOR_REACH_PX)) {
                for (x in maxOf(0, floor(p.x).toInt() - DOOR_REACH_PX)..minOf(N - 1, floor(p.x).toInt() + DOOR_REACH_PX)) {
                    val i = y * N + x
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
                    val x = j % N + dx
                    val k = j + dy * N + dx
                    if (x in 0 until N && k in 0 until N * N && walls[k] && k !in run && comp[k] == comp[start] && run.size < DOOR_PX) {
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

    private fun paintTrees() {
        val spots = mutableListOf<Px>()
        // Woods get a tree in about one world cell of 60 px² each, wherever the cell's seeded spot falls on their grass.
        for (area in scene.areas.filter { it.woodland }) {
            val wood = BooleanArray(N * N)
            area.polygons.forEach { polygon -> scanPolygon(pxRings(polygon)) { wood[it] = true } }
            seededSpots(WOOD_CELL_PX, WOOD_SALT, tries = 1) { x, y ->
                val i = y.toInt() * N + x.toInt()
                if (wood[i] && cls[i] == Ground.Grass) spots += Px(x, y)
                true
            }
        }
        val inView = spots.count { hypot(it.x - C, it.y - C) < RADIUS }
        val placed = mutableListOf<Px>()
        for (s in spots) {
            if (cls[s.y.toInt() * N + s.x.toInt()] in NO_TREES) continue
            if (placed.any { sq(s.x - it.x) + sq(s.y - it.y) < sq(TREE_SPACING_PX) }) continue
            placed += s
        }

        // Decoration for views the map gives no trees: a few seeded spots per world cell, the first clear one wins.
        if (inView < DECOR_MIN_MAPPED) {
            seededSpots(DECOR_CELL_PX, DECOR_SALT, DECOR_TRIES) { x, y ->
                val xi = x.toInt()
                val yi = y.toInt()
                val clear = (yi - 6 - DECOR_CLEAR_PX until yi + 2 + DECOR_CLEAR_PX).all { yy ->
                    (xi - 4 - DECOR_CLEAR_PX until xi + 4 + DECOR_CLEAR_PX).all { xx ->
                        xx !in 0 until N || yy !in 0 until N || cls[yy * N + xx].let { it == Ground.Grass || it == Ground.Pitch }
                    }
                }
                (clear && placed.all { sq(x - it.x) + sq(y - it.y) >= sq(DECOR_SPACING_PX) }).also { if (it) placed += Px(x, y) }
            }
        }

        if (sprites.trees.isEmpty()) return
        for (t in placed.sortedBy { it.y }) {
            val pick = stableRandom(TREE_SALT, (round(t.x).toInt() + originX).toLong(), (round(t.y).toInt() + originY).toLong())
            val sprite = sprites.trees[(pick * sprites.trees.size).toInt().coerceAtMost(sprites.trees.size - 1)]
            blit(sprite, t.x.toInt() - sprite.width / 2, t.y.toInt() - (sprite.height - 2))
        }
    }

    /**
     * Up to [tries] seeded spots in each world cell of [cell] px over the view, handed to [take] in view pixels until it
     * takes one. A spot off the view ends its cell's tries.
     */
    private inline fun seededSpots(cell: Double, salt: Long, tries: Int, take: (Double, Double) -> Boolean) {
        for (cj in floor(originY / cell).toInt()..floor((originY + N) / cell).toInt()) {
            for (ci in floor(originX / cell).toInt()..floor((originX + N) / cell).toInt()) {
                for (attempt in 0 until tries) {
                    val x = (ci + stableRandom(salt, 1, ci.toLong(), cj.toLong(), attempt.toLong())) * cell - originX
                    val y = (cj + stableRandom(salt, 2, ci.toLong(), cj.toLong(), attempt.toLong())) * cell - originY
                    if (x < 0 || y < 0 || x >= N || y >= N || take(x, y)) break
                }
            }
        }
    }

    // --- icons and people ----------------------------------------------------------------------

    private class Candidate(val distance: Double, val at: Px, val icon: MapIcon, val sprite: Pixels)

    /** Map icons decluttered by priority, then by nearness to the centre. */
    private fun placeIcons(): List<MapMark<Pixels>> {
        val candidates = scene.pois.mapNotNull { poi ->
            val icon = poi.icon ?: return@mapNotNull null
            val sprite = sprites.icons[icon] ?: return@mapNotNull null
            val p = px(poi.at)
            val d = hypot(p.x - C, p.y - C)
            Candidate(d, p, icon, sprite).takeIf { d < RADIUS - 9 }
        }.sortedWith(compareBy({ it.icon.priority }, { it.distance }))
        val icons = mutableListOf<Candidate>()
        val perKind = mutableMapOf<MapIcon, Int>()
        for (cand in candidates) {
            if (icons.size >= MAX_ICONS || (perKind[cand.icon] ?: 0) >= MAX_PER_ICON) continue
            if (icons.any { hypot(cand.at.x - it.at.x, cand.at.y - it.at.y) < ICON_SPACING_PX } || cand.distance < 8) continue
            icons += cand
            perKind[cand.icon] = (perKind[cand.icon] ?: 0) + 1
        }
        return icons.map {
            MapMark(it.sprite, it.at.x.toInt() - it.sprite.width / 2, it.at.y.toInt() - it.sprite.height / 2, it.sprite.width, it.sprite.height)
        }
    }

    /** Yellow dots where the map has someone to meet; the white self marker stays the only white mark. */
    private fun paintNpcs(icons: List<MapMark<Pixels>>) {
        val sprite = sprites.npc ?: return
        val dots = mutableListOf<Pair<Int, Int>>()
        // Nearest first, so which people make the cap does not depend on the order they came in.
        for (poi in scene.pois.filter { it.npc }.sortedBy { it.at.distanceSq() }) {
            val p = px(poi.at)
            val x = floor(p.x).toInt()
            val y = floor(p.y).toInt()
            val d = hypot(x - C, y - C)
            if (d > RADIUS - 4 || d < 5) continue
            if (dots.size >= MAX_NPCS || dots.any { (a, b) -> abs(x - a) < 5 && abs(y - b) < 5 }) continue
            if (icons.any { hypot(x - it.centreX.toDouble(), y - it.centreY.toDouble()) < 9 }) continue
            dots += x to y
        }
        dots.forEach { (x, y) -> blit(sprite, x - sprite.width / 2, y - sprite.height / 2) }
    }

    private fun blit(sprite: Pixels, ox: Int, oy: Int) {
        for (y in 0 until sprite.height) for (x in 0 until sprite.width) {
            val argb = sprite[x, y]
            val tx = ox + x
            val ty = oy + y
            if (alpha(argb) < 128 || tx !in 0 until N || ty !in 0 until N) continue
            img[ty * N + tx] = opaque(argb)
        }
    }

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
        if (n == 0 || maxY < 0 || minY >= N || maxX < 0 || minX >= N) return
        val xs = DoubleArray(n / 4)
        for (y in maxOf(0, minY.toInt())..minOf(N - 1, maxY.toInt())) {
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
                for (x in maxOf(0, ceilHalf(xs[k])) until minOf(N, ceilHalf(xs[k + 1]))) set(y * N + x)
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
            for (y in maxOf(0, (minOf(a.y, b.y) - r).toInt())..minOf(N - 1, (maxOf(a.y, b.y) + r).toInt())) {
                for (x in maxOf(0, (minOf(a.x, b.x) - r).toInt())..minOf(N - 1, (maxOf(a.x, b.x) + r).toInt())) {
                    val px = x + 0.5
                    val py = y + 0.5
                    val t = (((px - a.x) * dx + (py - a.y) * dy) / ll).coerceIn(0.0, 1.0)
                    if (sq(px - a.x - t * dx) + sq(py - a.y - t * dy) <= r * r) cls[y * N + x] = value
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
            if (maxOf(x0, x1) < 0 || minOf(x0, x1) >= N || maxOf(y0, y1) < 0 || minOf(y0, y1) >= N) continue
            val dx = abs(x1 - x0)
            val dy = -abs(y1 - y0)
            val sx = if (x0 < x1) 1 else -1
            val sy = if (y0 < y1) 1 else -1
            var err = dx + dy
            while (true) {
                if (x0 in 0 until N && y0 in 0 until N) set(y0 * N + x0)
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

        // Three 3x3 box blurs leave uniform noise a spread of about ±0.17 over a view; this stretches it back to 0..1.
        const val NOISE_STRETCH = 3.0
    }
}

private fun sq(v: Double) = v * v

private fun Local.distanceSq() = sq(east) + sq(north)

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
