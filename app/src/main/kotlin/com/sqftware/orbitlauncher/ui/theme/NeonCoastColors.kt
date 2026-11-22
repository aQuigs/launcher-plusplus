package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// A coast city after sundown: hot pink and electric cyan neon on a violet night, swapped round for the lagoon, and by
// day the same neon inked on a pale sky.
private val Dusk = Color(0xFF0E0620)
private val Lagoon = Color(0xFF061826)
private val HotPink = Color(0xFFFF5FB0)
private val ElectricCyan = Color(0xFF3DE7FF)

internal val NeonCoastFlamingo = Palette(
    deep = Dusk,
    accent = HotPink,
    pale = Color(0xFFFFE8F5),
    spark = Color(0xFFFFF2B0),
    warm = ElectricCyan,
    alarm = Color(0xFFFF8A80),
    frost = Color(0xFFFFF0F8),
    ringMark = Color(0xFF6CEEFF),
)

internal val NeonCoastLagoon = Palette(
    deep = Lagoon,
    accent = ElectricCyan,
    pale = Color(0xFFE4FBFF),
    spark = Color(0xFFFFC2E2),
    warm = HotPink,
    alarm = Color(0xFFFF8A80),
    frost = Color(0xFFEFFCFF),
    ringMark = Color(0xFFFF8AC8),
)

/** The sunset: the sky from its top down to the horizon, the sun's gold and pink, its halo, and the stars coming out. */
val SkyTop = Color(0xFF12082E)
val SkyHigh = Color(0xFF4A1462)
val SkyLow = Color(0xFFD63C7E)
val SkyHorizon = Color(0xFFFF9A4D)
val SunGold = Color(0xFFFFE066)
val SunPink = Color(0xFFFF3D8B)
val SunHalo = Color(0xFFFF5C9A).copy(alpha = 0.35f)
val StarGlint = Color(0xFFFFF4FA).copy(alpha = 0.7f)

/** The sea, far by the horizon and near, the sun's glitter on it, and the palms and shore black against it all. */
val SeaFar = Color(0xFF3A1258)
val SeaNear = Color(0xFF080318)
val SunGlitter = Color(0xFFFF7AA8)
val PalmShadow = Color(0xFF0B0416)

/** The city on the horizon and the colours its windows are lit in. */
val Skyline = Color(0xFF1E0B36)
val CityWindows = listOf(Color(0xFFFF6FB8), Color(0xFF6CEEFF), Color(0xFFFFD36B))

/** A neon sign: its dark backing, its pink tube, the cyan tube inside it, and the glow round them. */
val SignBacking = Dusk.copy(alpha = 0.85f)
val NeonPink = HotPink
val NeonCyan = ElectricCyan
val NeonHaze = HotPink.copy(alpha = 0.3f)

/** The edge of a sunset folder, which has to show on any patch of the wallpaper: dusk round a pale pink line. */
val SunsetEdge = DiscEdge(outer = Dusk.copy(alpha = 0.7f), inner = Color(0xFFFFC2DE))
