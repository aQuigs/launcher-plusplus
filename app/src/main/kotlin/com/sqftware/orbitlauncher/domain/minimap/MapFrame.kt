package com.sqftware.orbitlauncher.domain.minimap

/** The world map's side in pixels: the stretch painted at once, which the user pans across before the next is painted. */
const val WORLD_MAP_SIZE = 1024

internal const val MAX_ICONS = 4
internal const val MAX_PER_ICON = 2
internal const val MAX_NPCS = 10

/**
 * The ground a painting covers, [width] by [height] pixels, cut to a disc if [round], and how much it may mark: at most
 * [maxIcons] map icons, [maxPerIcon] of any one, and [maxNpcs] dots of people. A map that [followsUser] is centred on
 * them and turns with them, so its icons come apart to stay upright; it keeps its centre clear for their marker and
 * judges the place as a whole, nearest them first. Any other is painted north up to a world-wide grid, icons and all,
 * and judges each place by its own surroundings, so paintings of one place agree.
 */
class MapFrame(
    val width: Int,
    val height: Int,
    val round: Boolean,
    val followsUser: Boolean,
    val maxIcons: Int,
    val maxPerIcon: Int,
    val maxNpcs: Int,
) {
    /**
     * Ground metres to the mercator metre the map is drawn at round [lat]: the true scale round the user; otherwise the
     * world-wide grid's, so every painting's pixels fall on one grid and paintings of one place meet pixel for pixel.
     */
    fun groundScale(lat: Double) = if (followsUser) WebMercator.groundScale(lat) else WebMercator.gridScale(lat)

    /** Where a map asked for round [at] is centred: on the user, or on the middle of [at]'s pixel of the world-wide grid. */
    fun centreFor(at: LatLon) = if (followsUser) at else onSeedGrid(at)

    /** Which pixels a round frame shows, row by row, or null when it shows them all. */
    internal val disc: BooleanArray? by lazy {
        if (!round) return@lazy null
        val radius = minOf(width, height) / 2.0
        BooleanArray(width * height) {
            val dx = it % width + 0.5 - width / 2.0
            val dy = it / width + 0.5 - height / 2.0
            dx * dx + dy * dy <= radius * radius
        }
    }

    /** How far round the centre the map gathers features: to the frame's edge, plus room for what reaches into it. */
    val reachM: Double
        get() = maxOf(width, height) / 2 * METRES_PER_PIXEL + 40

    companion object {
        /** The game's minimap: a disc, sparsely marked so the player's surroundings read at a glance. */
        val MINIMAP = MapFrame(MINIMAP_SIZE, MINIMAP_SIZE, round = true, followsUser = true, MAX_ICONS, MAX_PER_ICON, MAX_NPCS)

        /** The game's world map: a square stretch with every icon that has room and, as in the game, nobody on it. */
        val WORLD_MAP = MapFrame(WORLD_MAP_SIZE, WORLD_MAP_SIZE, round = false, followsUser = false, Int.MAX_VALUE, Int.MAX_VALUE, maxNpcs = 0)
    }
}
