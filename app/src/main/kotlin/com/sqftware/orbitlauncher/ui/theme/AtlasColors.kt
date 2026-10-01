package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// An explorer's chart: parchment and a compass's red on sepia ink by night, and the same inked on the paper by day.
private val Sepia = Color(0xFF1F1B16)
private val Parchment = Color(0xFFE2CC9A)
private val Vellum = Color(0xFFF4ECDB)
private val CompassRed = Color(0xFFE0765C)

internal val AtlasPalette = Palette(
    deep = Sepia,
    accent = Parchment,
    pale = Vellum,
    spark = Color(0xFFFFF5DE),
    warm = CompassRed,
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFE8D7AE),
)

/** A sea chart: pale blue on deep navy. */
internal val AtlasNautical = Palette(
    deep = Color(0xFF0C1826),
    accent = Color(0xFFA9D4FF),
    pale = Color(0xFFE6F2FF),
    spark = Color(0xFFF2F9FF),
    warm = Color(0xFFE0765C),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFC9E2FA),
)

/** A desert map: sandstone on dark earth. */
internal val AtlasDesert = Palette(
    deep = Color(0xFF24160E),
    accent = Color(0xFFF2B27A),
    pale = Color(0xFFFBEEE2),
    spark = Color(0xFFFFF6EC),
    warm = Color(0xFFE0765C),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFF2D6BC),
)

/** A forest survey: leaf green on dark pine. */
internal val AtlasForest = Palette(
    deep = Color(0xFF101C12),
    accent = Color(0xFFC3E6A8),
    pale = Color(0xFFECF6E4),
    spark = Color(0xFFF6FFF0),
    warm = Color(0xFFE0A35C),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFD4EBC4),
)

/**
 * An island's coast, a globe's rim and a folded map's edge, which like a planet's edge have to show on any patch of the
 * wallpaper: parchment, outlined in sepia.
 */
val ChartEdge = DiscEdge(outer = Sepia.copy(alpha = 0.6f), inner = Parchment.copy(alpha = 0.9f))

/** The land of an island or a globe, and the contour lines over it. */
val Land = Color(0xFF5F7A52)
val Contour = Color(0xFFCFC69A).copy(alpha = 0.7f)

/** A globe's seas and the lines of latitude and longitude over them. */
val Sea = Color(0xFF24435A)
val Graticule = Parchment.copy(alpha = 0.55f)

/** A folded map's paper, the shade of its middle panel, and the route drawn on it. */
val MapPaper = Color(0xFFEAD9B0)
val MapFold = Color(0xFFCDB98C)
val Route = CompassRed
