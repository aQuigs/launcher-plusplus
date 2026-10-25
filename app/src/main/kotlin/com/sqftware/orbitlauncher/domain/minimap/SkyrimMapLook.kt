package com.sqftware.orbitlauncher.domain.minimap

import com.sqftware.orbitlauncher.domain.rgb
import kotlin.math.roundToInt

/** How bright each ground is on Skyrim's local map, from 0 to 255: a gray world, its roads and roofs the palest. */
private val GROUND_LIGHT = IntArray(Ground.entries.size).also { light ->
    mapOf(
        Ground.Grass to 0, Ground.Road to 150, Ground.Walk to 134, Ground.Path to 124, Ground.Plaza to 118,
        Ground.Parking to 126, Ground.Sand to 150, Ground.Water to 44, Ground.Pier to 112, Ground.FloorStone to 178,
        Ground.FloorWood to 166, Ground.Pitch to 104,
    ).forEach { (ground, value) -> light[ground.ordinal] = value }
}

/** Grass from its darkest to its palest shade. */
private const val GRASS_FROM = 82
private const val GRASS_TO = 104

/** A roof's edges: lit where it faces the light, from the upper left, and in shade where it faces away. */
private const val ROOF_LIT = 208
private const val ROOF_SHADED = 136

private const val RAIL_LIGHT = 64

/** How much of the light a structure's shadow leaves, which falls two pixels down and to the right. */
private const val SHADOW_LEFT = 0.6

/** A tree's crown: its radius in pixels, how much of the ground's light it leaves, and its lit edge's. */
private const val CROWN_PX = 2.6
private const val CROWN_LEFT = 0.5
private const val CROWN_LIT = 0.8

/**
 * Colors [layout] as Skyrim's local map: a gray ground seen from above under a light from the upper left, a touch
 * cold, roofs standing off it in their shadows, trees as dark crowns, and all of it softened as the game's view from
 * high above is. Writes the pixels, opaque, to [into].
 */
internal fun paintSkyrim(layout: GroundLayout, into: IntArray) {
    val w = layout.width
    val h = layout.height
    val structure = layout.structure
    fun inStructure(x: Int, y: Int) = x in 0 until w && y in 0 until h && structure[y * w + x] != 0

    val light = DoubleArray(w * h) { i ->
        val ground = layout.ground[i]
        if (ground == Ground.Grass) GRASS_FROM + (GRASS_TO - GRASS_FROM) * layout.shade[i] else GROUND_LIGHT[ground.ordinal].toDouble()
    }
    for (i in 0 until w * h) {
        val x = i % w
        val y = i / w
        if (layout.rails[i]) light[i] = RAIL_LIGHT.toDouble()
        when {
            layout.walls[i] -> light[i] = if (!inStructure(x - 1, y) || !inStructure(x, y - 1)) ROOF_LIT.toDouble() else ROOF_SHADED.toDouble()
            structure[i] == 0 && (inStructure(x - 1, y - 1) || inStructure(x - 2, y - 2)) -> light[i] *= SHADOW_LEFT
        }
    }
    for (tree in layout.trees) {
        for (y in (tree.y - CROWN_PX).toInt()..(tree.y + CROWN_PX).toInt()) for (x in (tree.x - CROWN_PX).toInt()..(tree.x + CROWN_PX).toInt()) {
            if (x !in 0 until w || y !in 0 until h) continue
            val dx = x + 0.5 - tree.x
            val dy = y + 0.5 - tree.y
            if (dx * dx + dy * dy > CROWN_PX * CROWN_PX) continue
            val lit = dx + dy < -CROWN_PX
            light[y * w + x] *= if (lit) CROWN_LIT else CROWN_LEFT
        }
    }

    // Softened by a 3 by 3 binomial blur, its weights summing to 16, edges held.
    val weights = intArrayOf(1, 2, 1)
    for (i in 0 until w * h) {
        val x = i % w
        val y = i / w
        var sum = 0.0
        for (dy in -1..1) for (dx in -1..1) {
            sum += light[(y + dy).coerceIn(0, h - 1) * w + (x + dx).coerceIn(0, w - 1)] * weights[dx + 1] * weights[dy + 1]
        }
        into[i] = cold(sum / 16)
    }
}

/** A gray of [light], tinted a touch toward blue as the game's northern light is. */
internal fun cold(light: Double): Int {
    val v = light.roundToInt().coerceIn(0, 255)
    return rgb((v - 3).coerceAtLeast(0), v, (v + 5).coerceAtMost(255))
}
