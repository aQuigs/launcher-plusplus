package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import com.sqftware.orbitlauncher.domain.Planet

// The launcher icon's palette (res/drawable/ic_launcher_*.xml), so the space theme reads as the icon writ large.
private val Sky = Color(0xFF0F1630)
private val Star = Color(0xFF9FB2E6)
private val Spark = Color(0xFFF4EFE6)
private val Gold = Color(0xFFFFD27A)
private val Frost = Color.White

internal val SpacePalette = Palette(
    deep = Sky,
    accent = Star,
    pale = Color(0xFFE8ECFF),
    spark = Spark,
    warm = Gold,
    alarm = Color(0xFFFF8A80),
    frost = Frost,
)

// The emblem is a disc of the icon's own sky whatever the wallpaper, so what is drawn inside it keeps the night's colours.
val RingSpark = Spark
val RingInk = Frost.copy(alpha = 0.9f)
val RingShade = Sky

val FolderEdge = DiscEdge(outer = Sky.copy(alpha = 0.5f), inner = Frost.copy(alpha = 0.5f))

/** What rims a planet's moons and rings, so they show on a light wallpaper as the folder's edge does. */
val PlanetShadow = Sky.copy(alpha = 0.6f)

/** A moon on a folder's edge: a spark ringed in shade, so it too shows on any patch of wallpaper. */
val FolderMoon = DiscEdge(outer = PlanetShadow, inner = Spark)

/** The tilted ring across a Ringed planet's disc: gold, shadowed on its outer edge so it shows on a light wallpaper. */
val PlanetRing = DiscEdge(outer = PlanetShadow, inner = Gold.copy(alpha = 0.85f))

/** The small planet at the heart of the Moons in orbit look, lit from the top left, and the track its moons follow. */
val OrbitCoreLit = Color(0xFF4A5AA8)
val OrbitCoreShade = Color(0xFF18214A)
val OrbitTrack = Star.copy(alpha = 0.55f)

/**
 * What one planet of the Solar system look adds to a folder: a [tint] through its glass, the colours of the touch it wears
 * ([accent], and [accent2] where it has two), its [moon]s, and its [features] in the order it draws them (the Earth's
 * land, ice and clouds, Jupiter's bands, Saturn's rings). Each planet uses only the ones it has.
 */
class PlanetPaint(
    val tint: Color,
    val accent: Color = Color.Transparent,
    val accent2: Color = Color.Transparent,
    val moon: Color = Color.Transparent,
    val features: List<Color> = emptyList(),
)

private fun tint(colour: Long, alpha: Float = 0.3f) = Color(colour).copy(alpha = alpha)

private val JupiterBrown = Color(0xFFA8704A)
private val JupiterTan = Color(0xFFD6B284).copy(alpha = 0.6f)

val PlanetPaints = mapOf(
    Planet.Mercury to PlanetPaint(tint(0xFFC9C4BB), accent = Color(0xFFFFD678), accent2 = Color(0x00FF8A3D)),
    Planet.Venus to PlanetPaint(tint(0xFFF3DCA0), accent = Color(0xFFFFF0C8).copy(alpha = 0.9f)),
    Planet.Earth to PlanetPaint(
        // Deeper than the other planets' glass, so the land reads against the sea.
        tint(0xFF2F74D8, alpha = 0.6f),
        moon = Color(0xFFD8D3CB),
        features = listOf(Color(0xFF4CA64F), Color(0xFFF4F8FB), Color.White.copy(alpha = 0.55f)),
    ),
    Planet.Mars to PlanetPaint(tint(0xFFE0794A), accent = Color(0xFFD0643A).copy(alpha = 0.9f), moon = Color(0xFFC9B8A8)),
    Planet.Jupiter to PlanetPaint(
        tint(0xFFE9CFA0),
        accent = Color(0xFFF6E2C4).copy(alpha = 0.8f),
        accent2 = Color(0xFFC4553A).copy(alpha = 0.95f),
        features = listOf(
            JupiterBrown.copy(alpha = 0.6f), JupiterTan, JupiterBrown.copy(alpha = 0.65f), Color(0xFFECD6B0).copy(alpha = 0.5f),
            Color(0xFFBE8458).copy(alpha = 0.65f), JupiterTan, Color(0xFF966240).copy(alpha = 0.6f),
        ),
    ),
    Planet.Saturn to PlanetPaint(
        tint(0xFFF2DFA6),
        features = listOf(
            Color(0xFFB89A66).copy(alpha = 0.55f), Color(0xFFF0DBA6).copy(alpha = 0.95f),
            Color(0xFF281E14).copy(alpha = 0.55f), Color(0xFFD6BC84).copy(alpha = 0.9f),
        ),
    ),
    Planet.Uranus to PlanetPaint(tint(0xFF9FE3E8), accent = Color(0xFFC8F5FA).copy(alpha = 0.85f)),
    Planet.Neptune to PlanetPaint(tint(0xFF3A63D6), accent = Color(0xFF3A63D6).copy(alpha = 0.95f), moon = Color(0xFFE6E0D6)),
    Planet.Pluto to PlanetPaint(tint(0xFFE3CDB0), accent = Color(0xFFFBF1DE), moon = Color(0xFFB3AEA9)),
)
