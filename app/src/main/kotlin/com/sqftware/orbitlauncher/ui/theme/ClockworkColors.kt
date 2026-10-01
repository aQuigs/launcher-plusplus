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

/** The watch in steel on a blued dial. */
internal val ClockworkSteel = Palette(
    deep = Color(0xFF0C121C),
    accent = Color(0xFFC8D4E3),
    pale = Color(0xFFE8EEF6),
    spark = Color(0xFFF4F8FF),
    warm = Color(0xFF8FB3E0),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFD6DEE8),
)

/** The watch in rose gold on a garnet dial. */
internal val ClockworkRoseGold = Palette(
    deep = Color(0xFF221012),
    accent = Color(0xFFEAB09A),
    pale = Color(0xFFF7E7E1),
    spark = Color(0xFFFFF2EC),
    warm = Color(0xFFF08A8A),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFF0CFC4),
)

/** The watch in brass on an emerald dial. */
internal val ClockworkEmerald = Palette(
    deep = Color(0xFF081A14),
    accent = Color(0xFF86D9A8),
    pale = Color(0xFFE3F6EA),
    spark = Color(0xFFF0FFF5),
    warm = Color(0xFFD9B36C),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFC8EBD5),
)

/** The rim of a folder's sub-dial, which, like a planet's edge, has to show on any patch of the wallpaper. */
val DialEdge = DiscEdge(outer = Walnut.copy(alpha = 0.55f), inner = Brass.copy(alpha = 0.8f))

/** A gear folder's cog: brass, outlined in walnut so its teeth show on a light wallpaper. */
val GearCog = DiscEdge(outer = Walnut.copy(alpha = 0.6f), inner = Brass.copy(alpha = 0.9f))
