package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The colors of a game theme's heads-up display: the local map's [fog] before it is painted and its [rim], whose outer
 * line is also the dark its edge fades into; the [ink] of its words, the player's arrow and the compass's letters, and
 * the [arrowEdge] that keeps the arrow on a pale roof; the [smoke] under the world map's words; the compass's [line]s
 * and the [shade] under its ends and marks; the [band] behind a discovered place's name, dense enough to hide the clock;
 * and the [quest] marker, edged in [questEdge].
 */
class HudColors(
    val fog: Color,
    val rim: DiscEdge,
    val ink: Color,
    val arrowEdge: Color,
    val smoke: Color,
    val line: Color,
    val shade: Color,
    val band: Color,
    val quest: Color,
    val questEdge: Color,
)
