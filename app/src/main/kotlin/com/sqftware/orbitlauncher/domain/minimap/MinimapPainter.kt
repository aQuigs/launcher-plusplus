package com.sqftware.orbitlauncher.domain.minimap

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.round
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
private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
private const val GRASS_DARK = 0x698F24
private const val GRASS_LIGHT = 0x718F24
private const val WATER_BASE = 0xFF2E5596.toInt()
private const val WATER_EDGE = 0xFF213E75.toInt()

private val GROUND_COLOUR = mapOf(
    Ground.Road to 0x85755A, Ground.Walk to 0x8B867A, Ground.Path to 0x85755A, Ground.Plaza to 0x6F6A5E,
    Ground.Parking to 0x85755A, Ground.Sand to 0xB4A26C, Ground.Pier to 0x866A45, Ground.FloorStone to 0x7A7468,
    Ground.FloorWood to 0x866C4C, Ground.Pitch to 0x6F9E2C,
).mapValues { (_, rgb) -> rgb or BLACK }

/** The ground grass blends into, so it shades toward a path over about a tile rather than meeting it at a hard edge. */
private val BLEND_GROUND = setOf(Ground.Grass, Ground.Road, Ground.Walk, Ground.Path, Ground.Plaza, Ground.Parking, Ground.Sand, Ground.Pitch)
private val NO_TREES = setOf(Ground.FloorStone, Ground.FloorWood, Ground.Water, Ground.Road, Ground.Parking)

/** Share of the disc under buildings from which a place counts as built up, and from which its open ground is paved. */
internal const val BUILT_UP_COVER = 0.15
internal const val PAVED_COVER = 0.3

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
internal const val ICON_SPACING_PX = 24.0
internal const val MAX_NPCS = 10

/**
 * Paints the Old School minimap of a [MapScene]: a [MINIMAP_SIZE] px disc at [METRES_PER_PIXEL], built the way the game
 * builds its minimap. Every 4 px tile gets a flat ground colour; walls are 1 px lines round each structure with a few
 * doors; trees, map icons and the dots of people go on top, and the self marker sits in the middle. Seeded choices are
 * keyed by places on Earth, so the map keeps its trees and grass as it moves.
 */
class MinimapPainter(private val sprites: MinimapSprites) {
    /** The disc as ARGB pixels, row by row: opaque inside the circle, clear outside it. */
    fun paint(scene: MapScene): IntArray = Painting(scene, sprites).run()
}

private class Px(val x: Double, val y: Double)

private class Painting(private val scene: MapScene, private val sprites: MinimapSprites) {
    private val w = MINIMAP_SIZE
    private val h = MINIMAP_SIZE

    // The 3x3 self marker covers pixels c-1..c+1 of the even disc, so the location sits on its middle pixel.
    private val c = MINIMAP_SIZE / 2 + 0.5
    private val radius = MINIMAP_SIZE / 2.0

    // Where the view's top-left pixel lies in a world-wide pixel grid. The grid's scale only changes a whole degree of
    // latitude away, so seeded trees and grass stay put as the view moves.
    private val originX: Int
    private val originY: Int

    init {
        val seedScale = WebMercator.groundScale(round(scene.centre.lat)) / METRES_PER_PIXEL
        val centre = WebMercator.toMercator(scene.centre)
        originX = floor(centre.x * seedScale).toInt() - MINIMAP_SIZE / 2
        originY = floor(-centre.y * seedScale).toInt() - MINIMAP_SIZE / 2
    }

    private val cls = arrayOfNulls<Ground>(w * h)
    private val img = IntArray(w * h)

    private fun px(p: Local) = Px(c + p.east / METRES_PER_PIXEL, c - p.north / METRES_PER_PIXEL)

    private fun pxRings(polygon: Polygon) = polygon.map { ring -> ring.map(::px) }

    fun run(): IntArray {
        // 1. footprints, whose cover says whether the place is built up
        val bid = IntArray(w * h)
        scene.buildings.forEachIndexed { i, b -> fillPolygon(bid, pxRings(b.polygon), i + 1) }
        val cover = (0 until w * h).count { bid[it] != 0 && inDisc(it % w, it / w) }.toDouble() / discPixels
        val builtUp = cover >= BUILT_UP_COVER
        val base = if (cover >= PAVED_COVER) Ground.Plaza else Ground.Grass
        cls.fill(base)

        // 2. ground classes
        scene.areas.filter { builtUp || !it.builtUpOnly }
            .sortedWith(compareBy({ -areaOf(it) }, { it.order }))
            .forEach { area -> area.polygons.forEach { fillPolygon(cls, pxRings(it), area.ground) } }
        scene.ways.filter { it.kind == WayKind.Pier }.forEach { strokeBuffer(cls, it.points.map(::px), it.widthM / METRES_PER_PIXEL, Ground.Pier) }
        scene.ways.filter { it.kind in setOf(WayKind.Walk, WayKind.Path, WayKind.Parking) }.forEach { way ->
            val ground = when (way.kind) {
                WayKind.Walk -> Ground.Walk
                WayKind.Parking -> Ground.Parking
                else -> Ground.Path
            }
            strokeBuffer(cls, way.points.map(::px), maxOf(1.0, way.widthM / METRES_PER_PIXEL), ground)
        }
        scene.ways.filter { it.kind == WayKind.Road }.sortedBy { it.widthM }.forEach {
            strokeBuffer(cls, it.points.map(::px), it.widthM / METRES_PER_PIXEL, Ground.Road)
        }

        // 3. touching footprints merge into one structure with one floor and only its outer wall, as a game building is
        // one walled shape rather than a wall per footprint
        val comp = IntArray(w * h)
        val floors = mutableListOf<Ground>()
        for (i in 0 until w * h) {
            if (bid[i] == 0 || comp[i] != 0) continue
            val members = mutableListOf<Int>()
            floodFill(i, comp, floors.size + 1, { bid[it] != 0 }) { members += it }
            val seed = members.minOf { scene.buildings[bid[it] - 1].seed }
            val floor = if (stableRandom(seed) < WOOD_FLOOR_SHARE) Ground.FloorWood else Ground.FloorStone
            floors += floor
            members.forEach { cls[it] = floor }
        }
        val walls = (0 until w * h).filter { i ->
            comp[i] != 0 && neighbours4(i).any { comp[it] == 0 }
        }.toSet()

        // 4. flat tiles: one colour per ground, grass shaded only by a smooth field across tiles
        val tw = w / TILE_PX + 2
        val noise = grassNoise(tw)
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            img[i] = when (val g = cls[i]!!) {
                Ground.Grass -> lerpRgb(GRASS_DARK, GRASS_LIGHT, noise[tileOf(x, y, tw)])
                Ground.Water -> if (neighbours4(i).any { cls[it] != Ground.Water }) WATER_EDGE else WATER_BASE
                else -> GROUND_COLOUR.getValue(g)
            }
        }
        blendGrass(tw)

        // 5. thin lines, then the walls on top
        scene.ways.filter { it.kind == WayKind.Rail }.forEach { rail -> linePixels(rail.points.map(::px)).forEach { set(it, RAIL) } }
        walls.forEach { img[it] = WALL }

        paintDoors(walls, comp)
        paintTrees()
        val icons = paintIcons()
        paintNpcs(icons)
        paintSelf()

        for (i in 0 until w * h) if (!inDisc(i % w, i / w)) img[i] = 0
        return img
    }

    private val discPixels = (0 until w * h).count { inDisc(it % w, it / w) }

    private fun inDisc(x: Int, y: Int): Boolean {
        val dx = x + 0.5 - radius
        val dy = y + 0.5 - radius
        return dx * dx + dy * dy <= radius * radius
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

    private fun neighbours4(i: Int): List<Int> {
        val x = i % w
        val y = i / w
        return buildList(4) {
            if (x + 1 < w) add(i + 1)
            if (x > 0) add(i - 1)
            if (y + 1 < h) add(i + w)
            if (y > 0) add(i - w)
        }
    }

    private fun floodFill(start: Int, marks: IntArray, mark: Int, inside: (Int) -> Boolean, visit: (Int) -> Unit) {
        marks[start] = mark
        val stack = ArrayDeque<Int>().apply { add(start) }
        while (stack.isNotEmpty()) {
            val j = stack.removeLast()
            visit(j)
            for (k in neighbours4(j)) if (inside(k) && marks[k] == 0) {
                marks[k] = mark
                stack += k
            }
        }
    }

    private fun set(p: Pair<Int, Int>, colour: Int) {
        val (x, y) = p
        if (x in 0 until w && y in 0 until h) img[y * w + x] = colour
    }

    // --- grass ---------------------------------------------------------------------------------

    private fun tileOf(x: Int, y: Int, tw: Int) =
        (Math.floorDiv(y + originY, TILE_PX) - Math.floorDiv(originY, TILE_PX)) * tw + Math.floorDiv(x + originX, TILE_PX) - Math.floorDiv(originX, TILE_PX)

    /**
     * A smooth field over the view's tiles, from 0 to 1: noise seeded per world tile, blurred three times. Its stretch
     * is fixed rather than fitted to the view, as the noise must not change with what is in view.
     */
    private fun grassNoise(tw: Int): DoubleArray {
        val margin = 3
        val span = tw + 2 * margin
        val tx0 = Math.floorDiv(originX, TILE_PX) - margin
        val ty0 = Math.floorDiv(originY, TILE_PX) - margin
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
    private fun blendGrass(tw: Int) {
        val sums = Array(tw * tw) { IntArray(4) }
        for (i in 0 until w * h) if (cls[i] in BLEND_GROUND) {
            val acc = sums[tileOf(i % w, i / w, tw)]
            acc[0] += img[i] shr 16 and 0xFF
            acc[1] += img[i] shr 8 and 0xFF
            acc[2] += img[i] and 0xFF
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
        for (i in 0 until w * h) if (cls[i] == Ground.Grass) img[i] = blended[tileOf(i % w, i / w, tw)]
    }

    // --- doors ---------------------------------------------------------------------------------

    /** Each entered place turns one tile's length of the nearest wall red, at most [MAX_DOORS] per structure. */
    private fun paintDoors(walls: Set<Int>, comp: IntArray) {
        val doorsPer = mutableMapOf<Int, Int>()
        val doorPx = mutableSetOf<Int>()
        for (poi in scene.pois.filter { it.door }) {
            val p = px(poi.at)
            var best: Int? = null
            var bestD = Double.MAX_VALUE
            for (y in maxOf(0, floor(p.y).toInt() - DOOR_REACH_PX)..minOf(h - 1, floor(p.y).toInt() + DOOR_REACH_PX)) {
                for (x in maxOf(0, floor(p.x).toInt() - DOOR_REACH_PX)..minOf(w - 1, floor(p.x).toInt() + DOOR_REACH_PX)) {
                    val i = y * w + x
                    if (i !in walls) continue
                    val d = abs(x + 0.5 - p.x) + abs(y + 0.5 - p.y)
                    if (d < bestD) {
                        bestD = d
                        best = i
                    }
                }
            }
            val start = best ?: continue
            if ((doorsPer[comp[start]] ?: 0) >= MAX_DOORS || start in doorPx) continue
            val run = mutableListOf(start)
            val frontier = ArrayDeque<Int>().apply { add(start) }
            while (frontier.isNotEmpty() && run.size < DOOR_PX) {
                val j = frontier.removeFirst()
                for ((dx, dy) in EIGHT_WAYS) {
                    val x = j % w + dx
                    val k = j + dy * w + dx
                    if (x in 0 until w && k in walls && k !in run && comp[k] == comp[start] && run.size < DOOR_PX) {
                        run += k
                        frontier += k
                    }
                }
            }
            run.forEach {
                img[it] = DOOR
                doorPx += it
            }
            doorsPer[comp[start]] = (doorsPer[comp[start]] ?: 0) + 1
        }
    }

    // --- trees ---------------------------------------------------------------------------------

    private fun paintTrees() {
        val spots = mutableListOf<Px>()
        // Woods get a tree in about one world cell of 60 px² each, wherever the cell's seeded spot falls on their grass.
        for (area in scene.areas.filter { it.woodland }) {
            val grid = IntArray(w * h)
            area.polygons.forEach { fillPolygon(grid, pxRings(it), 1) }
            seededSpots(WOOD_CELL_PX, WOOD_SALT) { x, y ->
                val i = y.toInt() * w + x.toInt()
                if (grid[i] != 0 && cls[i] == Ground.Grass) spots += Px(x, y)
            }
        }
        val inView = spots.count { hypot(it.x - c, it.y - c) < radius }
        val placed = mutableListOf<Px>()
        for (s in spots) {
            val xi = s.x.toInt()
            val yi = s.y.toInt()
            if (xi !in 0 until w || yi !in 0 until h || cls[yi * w + xi] in NO_TREES) continue
            if (placed.any { sq(s.x - it.x) + sq(s.y - it.y) < sq(TREE_SPACING_PX) }) continue
            placed += s
        }

        // Decoration for views the map gives no trees: a few seeded spots per world cell, the first clear one wins.
        if (inView < DECOR_MIN_MAPPED) {
            val cell = DECOR_CELL_PX
            for (cj in floor(originY / cell).toInt()..floor((originY + h) / cell).toInt()) {
                for (ci in floor(originX / cell).toInt()..floor((originX + w) / cell).toInt()) {
                    for (attempt in 0 until DECOR_TRIES) {
                        val x = (ci + stableRandom(DECOR_SALT, 1, ci.toLong(), cj.toLong(), attempt.toLong())) * cell - originX
                        val y = (cj + stableRandom(DECOR_SALT, 2, ci.toLong(), cj.toLong(), attempt.toLong())) * cell - originY
                        val xi = floor(x).toInt()
                        val yi = floor(y).toInt()
                        if (xi !in 0 until w || yi !in 0 until h) break
                        val clear = (yi - 6 - DECOR_CLEAR_PX until yi + 2 + DECOR_CLEAR_PX).all { yy ->
                            (xi - 4 - DECOR_CLEAR_PX until xi + 4 + DECOR_CLEAR_PX).all { xx ->
                                xx !in 0 until w || yy !in 0 until h || cls[yy * w + xx] == Ground.Grass || cls[yy * w + xx] == Ground.Pitch
                            }
                        }
                        if (clear && placed.all { hypot(x - it.x, y - it.y) >= DECOR_SPACING_PX }) {
                            placed += Px(x, y)
                            break
                        }
                    }
                }
            }
        }

        if (sprites.trees.isEmpty()) return
        for (t in placed.sortedBy { it.y }) {
            val pick = stableRandom(TREE_SALT, (round(t.x).toInt() + originX).toLong(), (round(t.y).toInt() + originY).toLong())
            val sprite = sprites.trees[(pick * sprites.trees.size).toInt().coerceAtMost(sprites.trees.size - 1)]
            blit(sprite, t.x.toInt() - sprite.width / 2, t.y.toInt() - (sprite.height - 2))
        }
    }

    /** One seeded spot in each world cell of [cell] px that falls in the view, handed to [spot] in view pixels. */
    private fun seededSpots(cell: Double, salt: Long, spot: (Double, Double) -> Unit) {
        for (cj in floor(originY / cell).toInt()..floor((originY + h) / cell).toInt()) {
            for (ci in floor(originX / cell).toInt()..floor((originX + w) / cell).toInt()) {
                val x = (ci + stableRandom(salt, 1, ci.toLong(), cj.toLong())) * cell - originX
                val y = (cj + stableRandom(salt, 2, ci.toLong(), cj.toLong())) * cell - originY
                if (x >= 0 && y >= 0 && x < w && y < h) spot(x, y)
            }
        }
    }

    // --- icons and people ----------------------------------------------------------------------

    private class Placed(val x: Double, val y: Double)

    /** Map icons decluttered by priority, then by nearness to the centre. */
    private fun paintIcons(): List<Placed> {
        val candidates = scene.pois.mapNotNull { poi ->
            val icon = poi.icon ?: return@mapNotNull null
            val sprite = sprites.icons[icon] ?: return@mapNotNull null
            val p = px(poi.at)
            val d = hypot(p.x - c, p.y - c)
            Candidate(poi.iconPriority, d, p, icon, sprite).takeIf { d < radius - 9 }
        }.sortedWith(compareBy({ it.priority }, { it.distance }))
        val icons = mutableListOf<Candidate>()
        val perKind = mutableMapOf<MapIcon, Int>()
        for (cand in candidates) {
            if (icons.size >= MAX_ICONS || (perKind[cand.icon] ?: 0) >= MAX_PER_ICON) continue
            if (icons.any { hypot(cand.at.x - it.at.x, cand.at.y - it.at.y) < ICON_SPACING_PX } || cand.distance < 8) continue
            icons += cand
            perKind[cand.icon] = (perKind[cand.icon] ?: 0) + 1
        }
        icons.forEach { blit(it.sprite, it.at.x.toInt() - it.sprite.width / 2, it.at.y.toInt() - it.sprite.height / 2) }
        return icons.map { Placed(it.at.x, it.at.y) }
    }

    private class Candidate(val priority: Int, val distance: Double, val at: Px, val icon: MapIcon, val sprite: Sprite)

    /** Yellow dots where the map has someone to meet; the white self marker stays the only white mark. */
    private fun paintNpcs(icons: List<Placed>) {
        val sprite = sprites.npc ?: return
        val dots = mutableListOf<Pair<Int, Int>>()
        for (poi in scene.pois.filter { it.npc }) {
            val p = px(poi.at)
            val x = floor(p.x).toInt()
            val y = floor(p.y).toInt()
            val d = hypot(x - c, y - c)
            if (d > radius - 4 || d < 5) continue
            if (dots.size >= MAX_NPCS || dots.any { (a, b) -> abs(x - a) < 5 && abs(y - b) < 5 }) continue
            if (icons.any { hypot(x - it.x, y - it.y) < 9 }) continue
            dots += x to y
        }
        dots.forEach { (x, y) -> blit(sprite, x - sprite.width / 2, y - sprite.height / 2) }
    }

    // Drawn last with a 1 px black outline so it never vanishes into a white wall.
    private fun paintSelf() {
        val m = c.toInt()
        for (y in m - 2..m + 2) for (x in m - 2..m + 2) img[y * w + x] = if (abs(x - m) <= 1 && abs(y - m) <= 1) WHITE else BLACK
    }

    private fun blit(sprite: Sprite, ox: Int, oy: Int) {
        for (y in 0 until sprite.height) for (x in 0 until sprite.width) {
            val argb = sprite.argb[y * sprite.width + x]
            val tx = ox + x
            val ty = oy + y
            if (argb ushr 24 < 128 || tx !in 0 until w || ty !in 0 until h) continue
            img[ty * w + tx] = argb or BLACK
        }
    }

    // --- raster primitives ---------------------------------------------------------------------

    /** Even-odd scanline fill sampled at pixel centres, so edges stay hard. */
    private fun fillPolygon(grid: IntArray, rings: List<List<Px>>, value: Int) =
        scanPolygon(rings) { grid[it] = value }

    private fun fillPolygon(grid: Array<Ground?>, rings: List<List<Px>>, value: Ground) =
        scanPolygon(rings) { grid[it] = value }

    private fun scanPolygon(rings: List<List<Px>>, set: (Int) -> Unit) {
        val edges = rings.flatMap { ring -> ring.indices.map { ring[it] to ring[(it + 1) % ring.size] } }.filter { (a, b) -> a.y != b.y }
        if (edges.isEmpty()) return
        val minY = edges.minOf { minOf(it.first.y, it.second.y) }
        val maxY = edges.maxOf { maxOf(it.first.y, it.second.y) }
        val minX = edges.minOf { minOf(it.first.x, it.second.x) }
        val maxX = edges.maxOf { maxOf(it.first.x, it.second.x) }
        if (maxY < 0 || minY >= h || maxX < 0 || minX >= w) return
        for (y in maxOf(0, minY.toInt())..minOf(h - 1, maxY.toInt())) {
            val sy = y + 0.5
            val xs = edges.filter { (a, b) -> (a.y <= sy) != (b.y <= sy) }
                .map { (a, b) -> a.x + (sy - a.y) * (b.x - a.x) / (b.y - a.y) }
                .sorted()
            for (k in 0 until xs.size - 1 step 2) {
                for (x in maxOf(0, ceilHalf(xs[k]))..<minOf(w, ceilHalf(xs[k + 1]))) set(y * w + x)
            }
        }
    }

    private fun ceilHalf(v: Double) = kotlin.math.ceil(v - 0.5).toInt()

    private fun strokeBuffer(grid: Array<Ground?>, pts: List<Px>, widthPx: Double, value: Ground) {
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
                    if (sq(px - a.x - t * dx) + sq(py - a.y - t * dy) <= r * r) grid[y * w + x] = value
                }
            }
        }
    }

    /** Bresenham along a polyline: the 1 px line the game draws. */
    private fun linePixels(pts: List<Px>): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
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
                out += x0 to y0
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
        return out
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

private fun rgb(r: Int, g: Int, b: Int) = BLACK or (r shl 16) or (g shl 8) or b

private fun lerpRgb(from: Int, to: Int, f: Double): Int {
    fun channel(shift: Int): Int {
        val a = from shr shift and 0xFF
        val b = to shr shift and 0xFF
        return (a + (b - a) * f).roundToInt()
    }
    return rgb(channel(16), channel(8), channel(0))
}

/** A number in [0, 1) that depends only on [key]: SplitMix64 over its parts. */
internal fun stableRandom(vararg key: Long): Double {
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
