package com.sqftware.orbitlauncher.domain.minimap

/**
 * How a theme that maps where the user is paints its map: the same ground, laid out alike, coloured in the look of a
 * game's map.
 */
enum class MapStyle {
    /** The old game's minimap: flat 4 px tiles of ground, white walls with red doors, its trees and the dots of people. */
    OldSchool,

    /** Skyrim's local map: a soft grey ground seen from above, buildings standing off it in their shadows, and no one on it. */
    Skyrim,
}
