package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// An old pocket watch: brass and copper on a dark walnut dial by night, and the same inked on cream porcelain by day.
private val Walnut = Color(0xFF1C1712)
private val Brass = Color(0xFFD9B36C)
private val Cream = Color(0xFFF5ECDA)
private val Copper = Color(0xFFE3946A)

internal val ClockworkPalette = Palette(
    deep = Walnut,
    accent = Brass,
    pale = Cream,
    spark = Color(0xFFFFF3D6),
    warm = Copper,
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFEBD3A0),
)

/** The rim of a folder's sub-dial, which, like a planet's edge, has to show on any patch of the wallpaper. */
val DialEdge = DiscEdge(outer = Walnut.copy(alpha = 0.55f), inner = Brass.copy(alpha = 0.8f))

/** A gear folder's cog: brass, outlined in walnut so its teeth show on a light wallpaper. */
val GearCog = DiscEdge(outer = Walnut.copy(alpha = 0.6f), inner = Brass.copy(alpha = 0.9f))
