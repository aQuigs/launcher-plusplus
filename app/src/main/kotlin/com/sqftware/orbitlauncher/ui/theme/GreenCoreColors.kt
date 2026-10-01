package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// A reactor's glowing green core: lime wire on a near-black green by night, and the same wire on a pale green by day.
private val Shielding = Color(0xFF07140A)
private val Plasma = Color(0xFF8CF04A)
private val Coolant = Color(0xFFE2F5D8)

internal val GreenCorePalette = Palette(
    deep = Shielding,
    accent = Plasma,
    pale = Coolant,
    spark = Color(0xFFF2FFE8),
    warm = Color(0xFFE6F25A),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFC9EFB0),
)

/** The edge of a reactor part, which like a planet's edge has to show on any patch of the wallpaper: lime round shielding. */
val CoreEdge = DiscEdge(outer = Shielding.copy(alpha = 0.7f), inner = Plasma)

/** A part's wire, its glow, and the light at its heart. */
val CoreWire = Plasma.copy(alpha = 0.85f)
val CoreGlow = Plasma.copy(alpha = 0.3f)
val CoreLight = Color(0xFFF2FFD8)

/** An orb's glass in its dark and light greens, and the gleam on it. */
val OrbDark = Color(0xFF1F6A12)
val OrbLight = Color(0xFF9CF25E)
val OrbGleam = Color.White.copy(alpha = 0.45f)

/** A hex cell's dark well. */
val CellWell = Shielding.copy(alpha = 0.85f)

/** The reactor scene: the dark round the core, its blaze, the tunnel's rings and the motes drifting in it. */
val ReactorDark = Color(0xFF030A04)
val ReactorDeep = Color(0xFF0E3A0C)
val ReactorBlaze = Color(0xFF8CF04A)
val ReactorRing = Color(0xFF6FD83A).copy(alpha = 0.22f)
val ReactorMote = Color(0xFFC9F7A0).copy(alpha = 0.5f)
