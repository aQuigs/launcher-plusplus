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
 * The northern game's heads-up display: a gray fog before the local map is painted, its rim a bone hairline around a
 * dark one so it holds on any wallpaper, snow-white words and arrow, bone hairlines on smoke along the compass, and the
 * gold of the quest marker, each edged in smoke so it holds on a pale roof.
 */
val SkyrimHudColors = HudColors(
    fog = Color(0xFF3A3D40),
    rim = DiscEdge(outer = Smoke.copy(alpha = 0.7f), inner = Bone.copy(alpha = 0.85f)),
    ink = Snow,
    arrowEdge = Smoke.copy(alpha = 0.8f),
    smoke = Smoke.copy(alpha = 0.55f),
    line = Bone.copy(alpha = 0.85f),
    shade = Smoke.copy(alpha = 0.8f),
    band = Smoke.copy(alpha = 0.92f),
    quest = Color(0xFFE6BE5A),
    questEdge = Smoke.copy(alpha = 0.85f),
)

/** A shield folder's boards in two shades of old wood, the iron of its rim and boss, and the gleam on its rivets. */
val ShieldBoardDark = Color(0xFF4A3526)
val ShieldBoardLight = Color(0xFF5E4431)
val ShieldIron = Color(0xFF5C5E60)
val ShieldRivet = Color(0xFFB4B6B8)

/**
 * The weather over the launcher: snowflakes of snow, each in a faint dark halo so it shows on a pale wallpaper too, and
 * the white of the mist at its thickest.
 */
val Snowflake = Snow
val SnowEdge = Smoke.copy(alpha = 0.18f)
val MistBank = Snow.copy(alpha = 0.16f)

/** The skills menu's sky round the ring: the blue and the deeper violet of the nebula behind the constellation. */
val SkillNebula = Color(0xFF5D7FC8).copy(alpha = 0.16f)
val SkillNebulaDeep = Color(0xFF7A62B8).copy(alpha = 0.12f)
