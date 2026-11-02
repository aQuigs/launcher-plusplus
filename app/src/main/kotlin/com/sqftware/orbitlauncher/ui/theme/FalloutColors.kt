package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// The wrist computer's screen: phosphor green glowing on the tube's green-black by night, its pale glow for text, the
// amber of the wasteland's warning signs as the second accent, and by day the same green inked on a pale screen.
private val Tube = Color(0xFF06140C)
private val Phosphor = Color(0xFF3CF08C)
private val Glow = Color(0xFFB6FFD4)
private val Amber = Color(0xFFFFB53D)

internal val FalloutPalette = Palette(
    deep = Tube,
    accent = Phosphor,
    pale = Glow,
    spark = Glow,
    warm = Amber,
    alarm = Color(0xFFFF6E5A),
    frost = Color(0xFFDCF5E6),
    ringMark = Color(0xFF8CF0B4),
)

/**
 * The wrist computer's heads-up display: the tube's dark before the map is painted, its rim a phosphor line inside a
 * dark one so it holds on any wallpaper, the screen's brightest glow for words and the arrow, phosphor ticks over a
 * shade of the tube's dark along the compass, and the quest marker in the wasteland's amber, apart from the green of the
 * map's own icons, each edged in the tube's dark.
 */
val PipBoyHudColors = HudColors(
    fog = Color(0xFF0C2416),
    rim = DiscEdge(outer = Tube.copy(alpha = 0.75f), inner = Phosphor.copy(alpha = 0.9f)),
    ink = Glow,
    arrowEdge = Tube.copy(alpha = 0.85f),
    smoke = Tube.copy(alpha = 0.8f),
    line = Phosphor.copy(alpha = 0.9f),
    shade = Tube.copy(alpha = 0.6f),
    band = Tube.copy(alpha = 0.94f),
    quest = Amber,
    questEdge = Tube.copy(alpha = 0.9f),
)

/** The screen's scanlines over the map, a faint dark line every few pixels, and the tile they are cut from, tinted as drawn. */
val PipBoyScanline = Tube.copy(alpha = 0.28f)
val ScanlineTile = Tube

/** A vault door: its rolled steel, the shade between its cogs and its ring, and the paint of its number band. */
val VaultSteel = Color(0xFF8A9096)
val VaultSteelDark = Color(0xFF50565C)
val VaultBand = Color(0xFFD9A93A)

/** A bottle cap: its face, its crimped skirt catching the light and in shade, and the red ring printed on its face. */
val CapFace = Color(0xFFC9CDD0)
val CapCrimpLit = Color(0xFFE8EAEC)
val CapCrimpShade = Color(0xFF7D8287)
val CapPrint = Color(0xFFC0392B)

/** The wasteland's dust blowing over the launcher: tan grains, each in a faint dark halo, and the haze they raise. */
val WastelandDust = Color(0xFFE2D3A8)
val WastelandDustEdge = Tube.copy(alpha = 0.2f)
val WastelandHaze = Color(0xFFD8C79A).copy(alpha = 0.12f)
