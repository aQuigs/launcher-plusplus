package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// Sumi-e: black ink and a seal's red on rice paper by day, and the paper's tone on ink by night.
private val Sumi = Color(0xFF1E1C1A)
private val Paper = Color(0xFFF3EEE5)
private val Vermilion = Color(0xFFC8473C)

internal val InkPalette = Palette(
    deep = Sumi,
    accent = Color(0xFFCFC7BA),
    pale = Paper,
    spark = Color(0xFFFFF9F0),
    warm = Color(0xFFE0705F),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFDDD5C8),
)

/** The edge of a blot, a seal or an ensō's wash, which like a planet's edge has to show on any patch of the wallpaper. */
val InkEdge = DiscEdge(outer = Sumi.copy(alpha = 0.6f), inner = Paper.copy(alpha = 0.85f))

/** Ink as a brush lays it down, and the grey wash it thins to. */
val SumiInk = Sumi.copy(alpha = 0.88f)
val InkWash = Color(0xFFB9B1A5).copy(alpha = 0.8f)

/** The dark heart of an ink drop, where the ink lies thickest. */
val InkHeart = Color(0xFF5E5850).copy(alpha = 0.85f)

/** A seal's red, and the paper showing through its carved border and glyph. */
val SealRed = Vermilion
val SealPaper = Paper.copy(alpha = 0.9f)
