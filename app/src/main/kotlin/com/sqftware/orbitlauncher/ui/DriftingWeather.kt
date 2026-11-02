package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.MistBank
import com.sqftware.orbitlauncher.ui.theme.SnowEdge
import com.sqftware.orbitlauncher.ui.theme.Snowflake
import com.sqftware.orbitlauncher.ui.theme.WastelandDust
import com.sqftware.orbitlauncher.ui.theme.WastelandDustEdge
import com.sqftware.orbitlauncher.ui.theme.WastelandHaze
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** How long the weather takes to come round to where it started, in seconds: a turn of the clock that drives it. */
internal const val WEATHER_CYCLE_S = 3_600

/** How many banks of mist drift across the lower screen. */
private const val MIST_BANKS = 3

/** How much wider than tall a bank of mist lies. */
private const val MIST_WIDE = 2.6f
private const val MIST_LOW = 0.7f

/**
 * Weather that drifts over the screen: [count] motes seeded by [seed] falling on the wind in depths from far to near,
 * and banks of haze rolling across the lower screen, each crossing it [crossings] times a cycle. A mote at the nearest
 * depth is [nearRadius] across and falls [nearFall] a second, of which the farthest keeps [farShare]; the wind carries it
 * [wind] sideways for each unit it falls, and it sways [sway] either side of its line. Each mote is [mote] in a faint
 * halo of [edge], so it shows on a pale wallpaper too, and a bank is [bank] at its thickest.
 */
internal class Drift(
    val count: Int,
    seed: Int,
    val nearRadius: Dp,
    val nearFall: Dp,
    val farShare: Float,
    val wind: Float,
    val sway: Dp,
    val mote: Color,
    val edge: Color,
    val bank: Color,
    val crossings: Int,
) {
    // Each mote's start, as shares of the screen across and down, its depth from far (0) to near (1), and its sway.
    private val random = Random(seed)
    val x = DoubleArray(count) { random.nextDouble() }
    val y = DoubleArray(count) { random.nextDouble() }
    val depth = FloatArray(count) { random.nextFloat() }
    val swayRate = FloatArray(count) { 0.6f + random.nextFloat() }
    val phase = DoubleArray(count) { random.nextDouble() * 2 * PI }
}

/** Snow in the northern game's mountains: flakes falling gently on a light wind, and mist rolling low. */
internal val Snowfall = Drift(
    count = 90, seed = 1_042, nearRadius = 2.6.dp, nearFall = 70.dp, farShare = 0.35f, wind = 0.25f, sway = 10.dp,
    mote = Snowflake, edge = SnowEdge, bank = MistBank, crossings = 40,
)

/** The wasteland's dust: fine grains carried mostly sideways on a steady wind, under rolling haze. */
internal val Dust = Drift(
    count = 70, seed = 2_077, nearRadius = 1.6.dp, nearFall = 12.dp, farShare = 0.4f, wind = 5f, sway = 14.dp,
    mote = WastelandDust, edge = WastelandDustEdge, bank = WastelandHaze, crossings = 60,
)

/**
 * The [drift] over the screen at the [turn] of its cycle, in degrees. Everything goes round a whole number of times in a
 * cycle, so nothing jumps as it starts over, and lies where it would on the window, so every copy of the weather shows
 * the same mote in the same place wherever it is laid out.
 */
@Composable
internal fun DriftingWeather(drift: Drift, turn: () -> Float, modifier: Modifier) {
    val window = LocalWindowInfo.current.containerSize
    var origin by remember { mutableStateOf(Offset.Zero) }
    Spacer(
        modifier
            .onPlaced { origin = it.positionInRoot() }
            .clearAndSetSemantics {}
            .drawWithCache {
                val count = drift.count
                val nearRadius = drift.nearRadius.toPx()
                val sway = drift.sway.toPx()
                // Room round the window, so a mote leaves and comes back out of sight.
                val margin = 2 * nearRadius
                val spanX = window.width + 2 * margin
                val spanY = window.height + 2 * margin
                // How many times each mote falls down the window, is blown across it and sways, in a cycle.
                val falls = IntArray(count)
                val blows = IntArray(count)
                val sways = IntArray(count)
                for (i in 0 until count) {
                    val fall = drift.nearFall.toPx() * (drift.farShare + (1 - drift.farShare) * drift.depth[i]) * WEATHER_CYCLE_S
                    falls[i] = (fall / spanY).roundToInt().coerceAtLeast(1)
                    blows[i] = (drift.wind * fall / spanX).roundToInt()
                    sways[i] = (drift.swayRate[i] * WEATHER_CYCLE_S / (2 * PI)).roundToInt()
                }
                val bankRadius = window.width * 0.45f
                val mist = Brush.radialGradient(0f to drift.bank, 1f to Color.Transparent, center = Offset.Zero, radius = bankRadius)
                val bankReach = MIST_WIDE * bankRadius

                onDrawBehind {
                    // In a double, so a mote that goes round hundreds of times a cycle still lands on its pixel.
                    val cycle = turn() / 360.0
                    translate(-origin.x, -origin.y) {
                        for (bank in 0 until MIST_BANKS) {
                            val across = fraction(cycle * drift.crossings + bank.toDouble() / MIST_BANKS).toFloat()
                            // Wholly off the window as it starts over at the other side.
                            val at = Offset(-bankReach + across * (window.width + 2 * bankReach), window.height * (0.55f + 0.14f * bank))
                            translate(at.x, at.y) {
                                scale(MIST_WIDE, MIST_LOW, pivot = Offset.Zero) { drawCircle(mist, bankRadius, Offset.Zero) }
                            }
                        }
                        for (i in 0 until count) {
                            val depth = drift.depth[i]
                            val swaying = sway * sin(2 * PI * fraction(cycle * sways[i]) + drift.phase[i])
                            val x = (fraction(drift.x[i] + cycle * blows[i] + swaying / spanX) * spanX).toFloat() - margin
                            val y = (fraction(drift.y[i] + cycle * falls[i]) * spanY).toFloat() - margin
                            val radius = nearRadius * (0.4f + 0.6f * depth)
                            val alpha = 0.35f + 0.55f * depth
                            drawCircle(drift.edge, radius * 1.6f, Offset(x, y), alpha = alpha)
                            drawCircle(drift.mote, radius, Offset(x, y), alpha = alpha)
                        }
                    }
                }
            },
    )
}

private fun fraction(x: Double) = x - floor(x)
