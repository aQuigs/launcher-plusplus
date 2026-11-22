package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// GTA's pause menu: a navy charcoal ground, its pink for what is picked and its teal for the way to go, white type,
// the red of a phone's badge for alarms, and by day the same pink and teal inked on a pale ground.
private val Charcoal = Color(0xFF181926)
private val ViceTeal = Color(0xFF3FE0C5)
private val VicePink = Color(0xFFFF85BC)
private val Ink = Color(0xFF111218)

internal val GtaPalette = Palette(
    deep = Charcoal,
    accent = VicePink,
    pale = Color(0xFFF2F2F6),
    spark = Color.White,
    warm = ViceTeal,
    alarm = Color(0xFFFF8A90),
    ringMark = Color(0xFFCFD1DC),
)

/**
 * GTA's heads-up display: the map's fog before it is painted, its edge a soft dark fading in under a white hairline,
 * white words, arrow and compass letters, the arrow edged in the game's dark, the compass a gray line with gray marks
 * over a faint shade, a discovered place named on the pause menu's charcoal, and the destination the user marked in the
 * game's teal, edged in its dark.
 */
val GtaHudColors = HudColors(
    fog = Color(0xFF2B3A44),
    rim = DiscEdge(outer = Color.Black.copy(alpha = 0.28f), inner = Color.White.copy(alpha = 0.45f)),
    ink = Color.White,
    arrowEdge = Ink.copy(alpha = 0.85f),
    smoke = Charcoal.copy(alpha = 0.8f),
    line = Color(0xFF8A8D9C).copy(alpha = 0.85f),
    shade = Color.Black.copy(alpha = 0.3f),
    band = Charcoal.copy(alpha = 0.94f),
    quest = ViceTeal,
    questEdge = Ink,
)

/** The compass's heading marker: the player's dark disc, its white ring and the white arrow in it. */
val HeadingDisc = Color(0xFF2A2B33)
val HeadingRing = Color.White.copy(alpha = 0.8f)

/** A wanted star lit, and one not, which the game leaves as a faint dark shape. */
val WantedLit = Color.White
val WantedUnlit = Color.Black.copy(alpha = 0.2f)

/** A weapon wheel's tile, the dark glass the game sets each weapon on, and a folder's chip, closed, with its hairline. */
val WheelTile = Color(0xFF26272F).copy(alpha = 0.72f)
val WheelVeil = Color(0xFF080910).copy(alpha = 0.5f)
val WheelChip = Color(0xFF1C1D28)
val WheelChipEdge = Color.White.copy(alpha = 0.35f)

/** The sunset: the sky from its top down to the horizon, the sun's gold and pink, its halo, and the stars coming out. */
val SunsetSky = listOf(Color(0xFF12082E), Color(0xFF4A1462), Color(0xFFD63C7E), Color(0xFFFF9A4D))
val SunGold = Color(0xFFFFE066)
val SunPink = Color(0xFFFF3D8B)
val SunHalo = Color(0xFFFF5C9A).copy(alpha = 0.35f)
val StarGlint = Color(0xFFFFF4FA).copy(alpha = 0.7f)

/** The sea from the horizon in, the sun's glitter on it, and the palms and shore black against it all. */
val SunsetSea = listOf(Color(0xFF3A1258), Color(0xFF080318))
val SunGlitter = Color(0xFFFF7AA8)
val PalmShadow = Color(0xFF0E0620)

/** The city on the horizon and the neon its windows are lit in. */
val Skyline = Color(0xFF1E0B36)
val CityWindows = listOf(VicePink, Color(0xFF3DE7FF), SunGold).map { it.copy(alpha = 0.85f) }
