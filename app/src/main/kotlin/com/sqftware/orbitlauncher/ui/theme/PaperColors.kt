package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// Origami: coral and cream washi on warm charcoal by night, and folded from cream paper by day.
private val Charcoal = Color(0xFF221C1A)
private val Coral = Color(0xFFE39A8A)
private val Cream = Color(0xFFF6EFE6)

internal val PaperPalette = Palette(
    deep = Charcoal,
    accent = Coral,
    pale = Cream,
    spark = Color(0xFFFFF8F2),
    warm = Color(0xFFD97A66),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFEFD9CF),
)

/** The edge of a folded paper, which like a planet's edge has to show on any patch of the wallpaper. */
val PaperEdge = DiscEdge(outer = Charcoal.copy(alpha = 0.55f), inner = Cream.copy(alpha = 0.9f))

/** Coral paper in three lights, as a fold turns it towards or away from the light, and the crane's creases in the darkest. */
val PaperLight = Color(0xFFF6D3C9)
val PaperMid = Color(0xFFE59A88)
val PaperDark = Color(0xFFC9705E)

/** Cream paper, an envelope's and a pinwheel's, the shade along an envelope's folds, and the shadow a folded paper casts. */
val EnvelopePaper = Cream
val EnvelopeFold = Color(0xFFE2D6C6)
val FoldShadow = Charcoal.copy(alpha = 0.3f)
