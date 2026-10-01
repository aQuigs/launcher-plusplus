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
