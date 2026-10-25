package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// The northern game's interface: bone-white type and hairlines on a smoke black by night, the blood red of its health
// bar for what warns, and by day the same lines inked on frosted bone.
private val Smoke = Color(0xFF0C0C0B)
private val Bone = Color(0xFFE4DCC8)
private val Snow = Color(0xFFF0EEE8)
private val Blood = Color(0xFFD25440)

internal val SkyrimPalette = Palette(
    deep = Smoke,
    accent = Bone,
    pale = Snow,
    spark = Color.White,
    warm = Blood,
    alarm = Color(0xFFE57368),
    frost = Color(0xFFE8E4DA),
    ringMark = Color(0xFFCFC8B6),
)

/**
 * The local map's disc: the fog it shows before a map is painted, the dark its edge fades into, and its rim, a bone
 * hairline around a dark one so it holds on any wallpaper.
 */
val LocalMapFog = Color(0xFF3A3D40)
val LocalMapVignette = Smoke.copy(alpha = 0.7f)
val LocalMapRim = DiscEdge(outer = Smoke.copy(alpha = 0.7f), inner = Bone.copy(alpha = 0.85f))

/** The player's arrow at the local map's middle, edged so it shows on a pale roof. */
val PlayerArrow = Snow
val PlayerArrowEdge = Smoke.copy(alpha = 0.8f)

/** What the local map's credit is written in, over its gray. */
val LocalMapInk = Snow

/**
 * The compass along the top of the home page: the smoke its bar fades out of at both ends, its bone hairlines and
 * letters, and the shade under its marks so a mark reads over any wallpaper.
 */
val CompassSmoke = Smoke.copy(alpha = 0.55f)
val CompassLine = Bone.copy(alpha = 0.85f)
val CompassLetter = Snow
val CompassShade = Smoke.copy(alpha = 0.8f)

/** A shield folder's boards in two shades of old wood, the iron of its rim and boss, and the gleam on its rivets. */
val ShieldBoardDark = Color(0xFF4A3526)
val ShieldBoardLight = Color(0xFF5E4431)
val ShieldIron = Color(0xFF5C5E60)
val ShieldRivet = Color(0xFFB4B6B8)

/** A menu's smoke, nearly opaque so the page under it does not show, and the bone hairlines above and below it. */
val SkyrimMenuSmoke = Smoke.copy(alpha = 0.92f)
val SkyrimMenuLine = Bone.copy(alpha = 0.7f)
