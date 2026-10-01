package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// A raked garden: sand and a koi's orange on charcoal by night, and the same inked on rice paper by day.
private val Charcoal = Color(0xFF1C1C1A)
private val Sand = Color(0xFFD8CBB0)
private val Rice = Color(0xFFF1ECE1)
private val KoiOrange = Color(0xFFE8875A)

internal val ZenPalette = Palette(
    deep = Charcoal,
    accent = Sand,
    pale = Rice,
    spark = Color(0xFFFFF8EA),
    warm = KoiOrange,
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFE3D8C0),
)

/** A fern garden: soft green on dark earth. */
internal val ZenFern = Palette(
    deep = Color(0xFF161C14),
    accent = Color(0xFFBCD9A0),
    pale = Color(0xFFEAF3E2),
    spark = Color(0xFFF6FFEE),
    warm = Color(0xFFE8875A),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFD3E6C2),
)

/** A slate garden: blue grey on wet stone. */
internal val ZenSlate = Palette(
    deep = Color(0xFF14181E),
    accent = Color(0xFFC6D3DF),
    pale = Color(0xFFE8EEF4),
    spark = Color(0xFFF4F8FC),
    warm = Color(0xFFE8875A),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFD6DEE6),
)

/** A garden in blossom: cherry pink on dark bark. */
internal val ZenSakura = Palette(
    deep = Color(0xFF20161A),
    accent = Color(0xFFF2C4CF),
    pale = Color(0xFFFBECEF),
    spark = Color(0xFFFFF5F7),
    warm = Color(0xFFE8875A),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFF2D6DD),
)

/** The edge of a stone, a pond or a patch of moss, which like a planet's edge has to show on any patch of the wallpaper. */
val GardenEdge = DiscEdge(outer = Charcoal.copy(alpha = 0.6f), inner = Sand.copy(alpha = 0.85f))

/** A stone, the light catching its top, and the pebble beside it. */
val Stone = Color(0xFF6F6A61)
val StoneLight = Color(0xFF8E897E)
val Pebble = Color(0xFF4A4741)

/** A pond's water, the lily pad on it, and its koi. */
val Pond = Color(0xFF1E3A3C)
val LilyPad = Color(0xFF5E7F4A)
val Koi = listOf(KoiOrange, Color(0xFFF2EEE6), Color(0xFFD9553F))

/** Moss, and the lighter and darker tufts on it. */
val Moss = Color(0xFF5B7340)
val MossLight = Color(0xFF7A9255)
val MossDark = Color(0xFF435629)
