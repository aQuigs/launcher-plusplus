package com.sqftware.orbitlauncher.domain.minimap

import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.isActive

/** The farthest, in ground meters, a quest lies for its route to be found: the tiles past it would crowd out the minimap's. */
const val MAX_ROUTE_M = 4_000.0

/** The ground the search reaches round both ends, for a road that swings out past them. */
private const val ROUTE_MARGIN_M = 300.0

/** The finest grid the roads are laid on, and the most cells a side, which coarsens it for a far quest. */
private const val FINEST_CELL_M = 3.0
private const val MAX_CELLS = 700

/** The farthest an end lies from a road to be taken onto it, as a GPS takes a car parked off the street. */
private const val SNAP_M = 150.0

/** How many cells the search settles between looks at whether its caller still wants the route. */
private const val CHECK_EVERY = 4096

private val RAISED = setOf("bridge", "tunnel")

private const val GROUND = 1
private const val LIFTED = 2
private const val PORTAL = 4

/**
 * Finds the way by road between two places, as a game's GPS lays its route: along the roads' middles, never along a
 * footpath, and onto a bridge or into a tunnel only from its ends, so a road crossing under one is not a turn off it.
 * One caller at a time; it keeps the tiles it has decoded, since the user moves on a little at a time.
 */
class RoadRouter(tiles: MapTiles) {
    private val decoded = DecodedTiles(tiles) { VectorTile.decode(it, setOf(LAYER), KEYS) }

    /**
     * The route by road from [from] to [to], starting at [from] and ending at [to], or null when the two are more than
     * [MAX_ROUTE_M] apart, a tile it needs cannot be had, or no road joins them. Every bridge and tunnel is one level
     * apart from the ground, so ramps stacked over one another may meet, and one-way streets are taken either way.
     */
    suspend fun route(from: LatLon, to: LatLon): List<LatLon>? {
        if (WebMercator.groundDistance(from, to) > MAX_ROUTE_M) return null
        val grid = RoadGrid.around(from, to)
        val tiles = decoded.get(grid.tiles()) ?: return null
        grid.lay(tiles.mapNotNull { (id, tile) -> tile[LAYER]?.let { id to it } }.toMap())
        val context = coroutineContext
        val cells = grid.path(grid.cellOf(from), grid.cellOf(to)) { context.isActive } ?: return null
        return listOf(from) + simplified(cells.map(grid::centreOf), grid.cellMercator).map(WebMercator::toLatLon) + to
    }

    private companion object {
        const val LAYER = "transportation"
        val KEYS = setOf("class", "service", "ramp", "brunnel")
    }
}

/**
 * The roads round two places laid on a grid of [width] by [height] square cells, [cellMercator] mercator meters a side,
 * its top left at [origin]: a cell holds a road at ground level, one raised off it (a bridge, or a tunnel under it), or
 * both where they meet.
 */
internal class RoadGrid private constructor(
    private val origin: Mercator,
    val cellMercator: Double,
    private val cellM: Double,
    private val width: Int,
    private val height: Int,
) {
    private val cells = ByteArray(width * height)

    // Grid coordinates count cells right and down from the top left, as mercator's y counts up.
    private fun gridX(at: Mercator) = (at.x - origin.x) / cellMercator
    private fun gridY(at: Mercator) = (origin.y - at.y) / cellMercator

    /** The tiles the grid covers. */
    fun tiles(): List<TileId> {
        val topLeft = WebMercator.tileAt(origin, OpenMapTiles.ZOOM)
        val bottomRight = WebMercator.tileAt(Mercator(origin.x + width * cellMercator, origin.y - height * cellMercator), OpenMapTiles.ZOOM)
        return (topLeft.y..bottomRight.y).flatMap { y -> (topLeft.x..bottomRight.x).map { x -> TileId(OpenMapTiles.ZOOM, x, y) } }
    }

    fun cellOf(at: LatLon): Int {
        val m = WebMercator.toMercator(at)
        return floor(gridY(m)).toInt().coerceIn(0, height - 1) * width + floor(gridX(m)).toInt().coerceIn(0, width - 1)
    }

    fun centreOf(cell: Int) = Mercator(origin.x + (cell % width + 0.5) * cellMercator, origin.y - (cell / width + 0.5) * cellMercator)

    /**
     * Lays the roads of [layers], each tile's transportation layer. A route goes onto a bridge or into a tunnel only where
     * it ends on the end of a road on the ground, as one runs on from it, and never where a tile cuts it or where it
     * merely carries on as another way, which would let a route drop off it onto the street below.
     */
    fun lay(layers: Map<TileId, VectorLayer>) {
        val groundEnds = HashSet<Int>()
        val raisedEnds = mutableListOf<Mercator>()
        for ((id, layer) in layers) {
            for (feature in layer.features) {
                if (feature.type != GeometryType.Line || !OpenMapTiles.isRoad(feature)) continue
                val raised = feature.string("brunnel") in RAISED
                for (part in feature.parts.filter { it.size >= 2 }) {
                    val line = part.map { WebMercator.fromTile(id, layer.extent, it) }
                    for (i in 1 until line.size) draw(line[i - 1], line[i], if (raised) LIFTED else GROUND)
                    for (end in listOf(0, part.lastIndex)) {
                        val at = part[end]
                        if (at.x !in 0..layer.extent || at.y !in 0..layer.extent) continue
                        if (raised) raisedEnds += line[end] else cellAt(line[end])?.let { groundEnds += it }
                    }
                }
            }
        }
        for (end in raisedEnds) {
            val cell = cellAt(end) ?: continue
            if (around(cell).any { it in groundEnds }) around(cell).forEach { cells[it] = (cells[it].toInt() or PORTAL).toByte() }
        }
    }

    private fun cellAt(at: Mercator): Int? {
        val x = floor(gridX(at)).toInt()
        val y = floor(gridY(at)).toInt()
        return if (x in 0 until width && y in 0 until height) y * width + x else null
    }

    // The cell and its eight neighbors that lie on the grid.
    private fun around(cell: Int): List<Int> {
        val x = cell % width
        val y = cell / width
        return (-1..1).flatMap { dy -> (-1..1).map { dx -> x + dx to y + dy } }
            .filter { (nx, ny) -> nx in 0 until width && ny in 0 until height }
            .map { (nx, ny) -> ny * width + nx }
    }

    // Each step only across a side, never a corner, so two roads that cross always share a cell.
    private fun draw(a: Mercator, b: Mercator, level: Int) {
        val ax = gridX(a)
        val ay = gridY(a)
        val bx = gridX(b)
        val by = gridY(b)
        if (max(ax, bx) < 0 || max(ay, by) < 0 || minOf(ax, bx) >= width || minOf(ay, by) >= height) return
        val steps = max(1, ceil(hypot(bx - ax, by - ay) * 2).toInt())
        var lastX = floor(ax).toInt()
        var lastY = floor(ay).toInt()
        mark(lastX, lastY, level)
        for (s in 1..steps) {
            val t = s.toDouble() / steps
            val x = floor(ax + (bx - ax) * t).toInt()
            val y = floor(ay + (by - ay) * t).toInt()
            if (x != lastX && y != lastY) mark(x, lastY, level)
            mark(x, y, level)
            lastX = x
            lastY = y
        }
    }

    private fun mark(x: Int, y: Int, bits: Int) {
        if (x in 0 until width && y in 0 until height) cells[y * width + x] = (cells[y * width + x].toInt() or bits).toByte()
    }

    private fun has(cell: Int, level: Int) = cells[cell].toInt() and (if (level == 0) GROUND else LIFTED) != 0

    /** The road cell nearest [cell] within the snapping distance, with the level it is on, as a search state. */
    private fun nearestRoad(cell: Int): Int? {
        val cx = cell % width
        val cy = cell / width
        val reach = (SNAP_M / cellM).roundToInt()
        var best: Int? = null
        var bestDistance = reach * reach + 1
        for (y in (cy - reach).coerceAtLeast(0)..(cy + reach).coerceAtMost(height - 1)) {
            for (x in (cx - reach).coerceAtLeast(0)..(cx + reach).coerceAtMost(width - 1)) {
                val c = y * width + x
                val level = if (has(c, 0)) 0 else if (has(c, 1)) 1 else continue
                val d = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                if (d < bestDistance) {
                    bestDistance = d
                    best = c * 2 + level
                }
            }
        }
        return best
    }

    /**
     * The cells of the shortest way by road from the road nearest [from] to the road nearest [to], or null if none joins
     * them or the caller stops [wanting] it.
     */
    fun path(from: Int, to: Int, wanting: () -> Boolean = { true }): List<Int>? {
        val start = nearestRoad(from) ?: return null
        val goal = nearestRoad(to)?.div(2) ?: return null
        val gx = goal % width
        val gy = goal / width
        fun estimate(cell: Int): Float {
            val dx = (cell % width - gx).toFloat()
            val dy = (cell / width - gy).toFloat()
            return sqrt(dx * dx + dy * dy)
        }

        val cost = FloatArray(cells.size * 2) { Float.POSITIVE_INFINITY }
        val before = IntArray(cells.size * 2) { -1 }
        val open = Heap()
        cost[start] = 0f
        open.push(start, estimate(start / 2))
        var settled = 0
        while (open.isNotEmpty()) {
            if (++settled % CHECK_EVERY == 0 && !wanting()) return null
            val f = open.topKey()
            val state = open.pop()
            val cell = state / 2
            val level = state % 2
            val g = cost[state]
            // A state pushed again at a lower cost leaves its older, dearer entry behind.
            if (f > g + estimate(cell) + 1e-3f) continue
            if (cell == goal) return generateSequence(state) { before[it].takeIf { b -> b >= 0 } }.map { it / 2 }.toList().reversed()

            fun reach(next: Int, step: Float) {
                if (g + step < cost[next]) {
                    cost[next] = g + step
                    before[next] = state
                    open.push(next, g + step + estimate(next / 2))
                }
            }
            if (cells[cell].toInt() and PORTAL != 0 && has(cell, 1 - level)) reach(cell * 2 + 1 - level, 0f)
            val x = cell % width
            val y = cell / width
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx !in 0 until width || ny !in 0 until height) continue
                val next = ny * width + nx
                if (has(next, level)) reach(next * 2 + level, if (dx != 0 && dy != 0) DIAGONAL else 1f)
            }
        }
        return null
    }

    companion object {
        private const val DIAGONAL = 1.4142135f

        /** The grid that holds [a], [b] and the margin round them, as fine as its size allows. */
        fun around(a: LatLon, b: LatLon): RoadGrid {
            val scale = WebMercator.groundScale((a.lat + b.lat) / 2)
            val ma = WebMercator.toMercator(a)
            val mb = WebMercator.toMercator(b)
            val spanX = abs(ma.x - mb.x) * scale + 2 * ROUTE_MARGIN_M
            val spanY = abs(ma.y - mb.y) * scale + 2 * ROUTE_MARGIN_M
            val cellM = max(FINEST_CELL_M, max(spanX, spanY) / MAX_CELLS)
            val margin = ROUTE_MARGIN_M / scale
            return RoadGrid(
                Mercator(minOf(ma.x, mb.x) - margin, maxOf(ma.y, mb.y) + margin),
                cellM / scale,
                cellM,
                ceil(spanX / cellM).toInt(),
                ceil(spanY / cellM).toInt(),
            )
        }
    }
}

/** A binary min-heap of search states by their estimated cost, in flat arrays, as a search pushes up to millions. */
private class Heap {
    private var states = IntArray(1024)
    private var keys = FloatArray(1024)
    private var size = 0

    fun isNotEmpty() = size > 0

    fun topKey() = keys[0]

    fun push(state: Int, key: Float) {
        if (size == states.size) {
            states = states.copyOf(size * 2)
            keys = keys.copyOf(size * 2)
        }
        var i = size++
        while (i > 0) {
            val parent = (i - 1) / 2
            if (keys[parent] <= key) break
            states[i] = states[parent]
            keys[i] = keys[parent]
            i = parent
        }
        states[i] = state
        keys[i] = key
    }

    /** Takes the state of least cost off the heap. */
    fun pop(): Int {
        val top = states[0]
        val state = states[--size]
        val key = keys[size]
        var i = 0
        while (true) {
            var child = 2 * i + 1
            if (child >= size) break
            if (child + 1 < size && keys[child + 1] < keys[child]) child++
            if (keys[child] >= key) break
            states[i] = states[child]
            keys[i] = keys[child]
            i = child
        }
        states[i] = state
        keys[i] = key
        return top
    }
}

/** [points] with every point dropped that lies within [tolerance] of the line its neighbors kept would run along. */
internal fun simplified(points: List<Mercator>, tolerance: Double): List<Mercator> {
    if (points.size < 3) return points
    val keep = BooleanArray(points.size)
    keep[0] = true
    keep[points.lastIndex] = true
    val spans = ArrayDeque(listOf(0 to points.lastIndex))
    while (spans.isNotEmpty()) {
        val (first, last) = spans.removeLast()
        val a = points[first]
        val b = points[last]
        val length = hypot(b.x - a.x, b.y - a.y)
        var farthest = -1
        var distance = tolerance
        for (i in first + 1 until last) {
            val p = points[i]
            val d = if (length == 0.0) hypot(p.x - a.x, p.y - a.y) else abs((b.x - a.x) * (a.y - p.y) - (a.x - p.x) * (b.y - a.y)) / length
            if (d > distance) {
                farthest = i
                distance = d
            }
        }
        if (farthest >= 0) {
            keep[farthest] = true
            spans += first to farthest
            spans += farthest to last
        }
    }
    return points.filterIndexed { i, _ -> keep[i] }
}
