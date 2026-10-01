package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

// A city of glass seen from above: ice blue on a night navy, and the same glass by day.
private val Navy = Color(0xFF061233)
private val Ice = Color(0xFF7FB2FF)
private val Frost = Color(0xFFE4EEFF)

internal val CrystalPalette = Palette(
    deep = Navy,
    accent = Ice,
    pale = Frost,
    spark = Color(0xFFF2F7FF),
    warm = Color(0xFFB59CFF),
    alarm = Color(0xFFFF8A80),
    ringMark = Color(0xFFBFD4FF),
)

/** The edge of a crystal, which like a planet's edge has to show on any patch of the wallpaper. */
val CrystalEdge = DiscEdge(outer = Navy.copy(alpha = 0.7f), inner = Color(0xFFCFE0FF).copy(alpha = 0.9f))

/** A glass crystal's faces, lit from above and to the left, and the bright lines of its edges. */
val GlassTop = Color(0xFF9FC3FF).copy(alpha = 0.6f)
val GlassLeft = Color(0xFF5A86E0).copy(alpha = 0.55f)
val GlassRight = Color(0xFF2F55B0).copy(alpha = 0.6f)
val GlassLine = Color(0xFFDCE8FF).copy(alpha = 0.9f)

/** A geode's rind, its dark hollow, and the crystals lining it in two lights. */
val GeodeRind = Color(0xFF5C6E9E)
val GeodeHollow = Color(0xFF0B1640)
val GeodeShard = Color(0xFF8E7BE0)
val GeodeShardLight = Color(0xFFB9A8FF)

/** The city scene: its sky fading to the ground, the streets, the towers' walls and roofs, light trails and the plaza. */
val CitySky = Color(0xFF0A1A4E)
val CityGround = Color(0xFF040B24)
val CityStreet = Color(0xFF2F5BD0).copy(alpha = 0.3f)
val TowerWall = Color(0xFF2A56C8).copy(alpha = 0.35f)
val TowerWallDark = Color(0xFF1A3790).copy(alpha = 0.5f)
val TowerRoof = Color(0xFF6E9BFF).copy(alpha = 0.28f)
val TowerEdge = Color(0xFFAFC9FF).copy(alpha = 0.45f)
val CityTrail = Color(0xFFBFE6FF).copy(alpha = 0.85f)
val CityTrailGlow = Color(0xFF6FB8FF).copy(alpha = 0.25f)
val PlazaGlow = Color(0xFF3D6FE0).copy(alpha = 0.45f)
