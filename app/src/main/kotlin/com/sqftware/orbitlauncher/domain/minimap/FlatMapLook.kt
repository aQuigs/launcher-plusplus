package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.blue
import com.sqftware.orbitlauncher.domain.green
import com.sqftware.orbitlauncher.domain.lerpRgb
import com.sqftware.orbitlauncher.domain.opaque
import com.sqftware.orbitlauncher.domain.red
import com.sqftware.orbitlauncher.domain.rgb
import kotlin.math.roundToInt

/**
 * The colors, as RGB, of a map painted flat: the [land] and the [water], the [park]s and [sand], the [road]s and the
 * [block]s that buildings stand in. Paved open ground stays close to the land, as the game leaves the space between
 * its blocks green, and footpaths lie between the land and the roads.
 */
internal class FlatGround(val land: Int, val water: Int, val park: Int, val sand: Int, val road: Int, val block: Int) {
    private val paved = lerpRgb(land, block, 0.25)
    private val walk = lerpRgb(land, road, 0.45)

    /** Each ground's color, opaque, by its ordinal. */
    val colours = IntArray(Ground.entries.size) { i ->
        opaque(
            when (Ground.entries[i]) {
                Ground.Grass -> land
                Ground.Water -> water
                Ground.Pitch -> park
                Ground.Sand -> sand
                Ground.Road, Ground.Pier -> road
                Ground.Walk, Ground.Path -> walk
                Ground.Plaza, Ground.Parking -> paved
                Ground.FloorStone, Ground.FloorWood -> block
            },
        )
    }
}

/** GTA's map by day: mint land, the sea's pale cyan, the greener park, the beach, near-white roads and blue-gray blocks. */
internal val GtaDayGround = FlatGround(
    land = 0x9FD8B8, water = 0x4CC3E2, park = 0x7CC79C, sand = 0xF0E4B8, road = 0xF6F8FA, block = 0xBCC8D4,
)

/** GTA's map after dark: deep teal land, violet water, a darker park, a dusky beach, lavender roads and navy blocks. */
internal val GtaNightGround = FlatGround(
    land = 0x1E6B5C, water = 0x4B45A0, park = 0x1A5E50, sand = 0x5B6A7E, road = 0xB4B0DE, block = 0x2C3270,
)

/** A paint for a map in flat [ground] colors, as [paintFlat] colors it. */
internal fun flat(ground: FlatGround) = GroundPaint { layout, into -> paintFlat(layout, into, ground) }

/**
 * Colors [layout] flat, as GTA draws its map: each ground in its one color, buildings sunk into their blocks with no
 * walls or shadows, rails as roads and no trees, softened where one ground meets the next. Writes the pixels, opaque,
 * to [into].
 */
internal fun paintFlat(layout: GroundLayout, into: IntArray, ground: FlatGround) {
    val w = layout.width
    val h = layout.height
    val flat = IntArray(w * h) { i -> if (layout.rails[i]) opaque(ground.road) else ground.colours[layout.ground[i].ordinal] }

    // Softened, so an edge drawn scaled is not stepped.
    val r = softened(w, h, DoubleArray(w * h) { red(flat[it]).toDouble() })
    val g = softened(w, h, DoubleArray(w * h) { green(flat[it]).toDouble() })
    val b = softened(w, h, DoubleArray(w * h) { blue(flat[it]).toDouble() })
    for (i in 0 until w * h) into[i] = rgb(r[i].roundToInt(), g[i].roundToInt(), b[i].roundToInt())
}
