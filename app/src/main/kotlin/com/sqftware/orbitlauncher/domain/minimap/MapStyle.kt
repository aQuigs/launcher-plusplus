package com.sqftware.orbitlauncher.domain.minimap

/**
 * How a theme that maps where the user is paints its map: the same ground, laid out alike, colored in the look of a
 * game's map. A style with a [relief] tint paints the ground in relief in that tint and is drawn scaled smoothly, its
 * icons kept apart from the ground at a size of their own; the old game's is painted in flat tiles, in crisp pixels with
 * the icons in the ground of its world map.
 */
enum class MapStyle(val relief: LightTint?) {
    /** The old game's minimap: flat 4 px tiles of ground, white walls with red doors, its trees and the dots of people. */
    OldSchool(relief = null),

    /** Skyrim's local map: a soft gray ground seen from above, buildings standing off it in their shadows, and no one on it. */
    Skyrim(relief = LightTint(::cold)),

    /** Fallout's Pip-Boy map: the same ground in relief, glowing green on the screen's dark, and no one on it. */
    Fallout(relief = LightTint(::phosphor)),
}

/** The color, opaque, of a light from 0 to 255 on a map in relief. */
fun interface LightTint {
    fun tint(light: Double): Int
}
