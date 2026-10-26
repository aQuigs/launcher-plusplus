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
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.MistBank
import com.sqftware.orbitlauncher.ui.theme.SnowEdge
import com.sqftware.orbitlauncher.ui.theme.Snowflake
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** How long the weather takes to come round to where it started, in seconds: a turn of the clock that drives it. */
internal const val WEATHER_CYCLE_S = 3_600

/** How many flakes fall at once, the near ones the largest, fastest and brightest. */
private const val FLAKES = 90

/** A flake's radius and how far it falls in a second at the nearest depth, and how much of that the farthest keeps. */
private val NEAR_FLAKE = 2.6.dp
private val NEAR_FALL = 70.dp
private const val FAR_SHARE = 0.35f

/** How far the wind carries the snow sideways for each unit it falls, and how far a flake sways either side of its line. */
private const val WIND = 0.25f
private val SWAY = 10.dp

/** How many banks of mist drift across the lower screen, and how many times each crosses it in a cycle. */
private const val MIST_BANKS = 3
private const val MIST_CROSSINGS = 40

/** How much wider than tall a bank of mist lies. */
private const val MIST_WIDE = 2.6f
private const val MIST_LOW = 0.7f

/** Each flake's start, as shares of the screen across and down, its depth from far (0) to near (1), and its sway. */
private class Flakes(random: Random) {
    val x = DoubleArray(FLAKES) { random.nextDouble() }
    val y = DoubleArray(FLAKES) { random.nextDouble() }
    val depth = FloatArray(FLAKES) { random.nextFloat() }
    val swayRate = FloatArray(FLAKES) { 0.6f + random.nextFloat() }
    val phase = DoubleArray(FLAKES) { random.nextDouble() * 2 * PI }
}

private val FlakeField = Flakes(Random(1_042))

/**
 * Snow drifting down over the screen on the wind, in three depths, and banks of mist rolling slowly across its lower half,
 * as in the northern game's mountains, at the [turn] of their cycle, in degrees. Everything goes round a whole number of
 * times in a cycle, so nothing jumps as it starts over, and lies where it would on the window, so every copy of the
 * weather shows the same flake in the same place wherever it is laid out.
 */
@Composable
internal fun SnowAndMist(turn: () -> Float, modifier: Modifier) {
    val window = LocalWindowInfo.current.containerSize
    var origin by remember { mutableStateOf(Offset.Zero) }
    Spacer(
        modifier
            .onPlaced { origin = it.positionInRoot() }
            .clearAndSetSemantics {}
            .drawWithCache {
                val nearRadius = NEAR_FLAKE.toPx()
                val sway = SWAY.toPx()
                // Room round the window, so a flake leaves and comes back out of sight.
                val margin = 2 * nearRadius
                val spanX = window.width + 2 * margin
                val spanY = window.height + 2 * margin
                val field = FlakeField
                // How many times each flake falls down the window, is blown across it and sways, in a cycle.
                val falls = IntArray(FLAKES)
                val blows = IntArray(FLAKES)
                val sways = IntArray(FLAKES)
                for (i in 0 until FLAKES) {
                    val fall = NEAR_FALL.toPx() * (FAR_SHARE + (1 - FAR_SHARE) * field.depth[i]) * WEATHER_CYCLE_S
                    falls[i] = (fall / spanY).roundToInt().coerceAtLeast(1)
                    blows[i] = (WIND * fall / spanX).roundToInt()
                    sways[i] = (field.swayRate[i] * WEATHER_CYCLE_S / (2 * PI)).roundToInt()
                }
                val bankRadius = window.width * 0.45f
                val mist = Brush.radialGradient(0f to MistBank, 1f to Color.Transparent, center = Offset.Zero, radius = bankRadius)
                val bankReach = MIST_WIDE * bankRadius

                onDrawBehind {
                    // In a double, so a flake that goes round hundreds of times a cycle still lands on its pixel.
                    val cycle = turn() / 360.0
                    translate(-origin.x, -origin.y) {
                        for (bank in 0 until MIST_BANKS) {
                            val across = fraction(cycle * MIST_CROSSINGS + bank.toDouble() / MIST_BANKS).toFloat()
                            // Wholly off the window as it starts over at the other side.
                            val at = Offset(-bankReach + across * (window.width + 2 * bankReach), window.height * (0.55f + 0.14f * bank))
                            translate(at.x, at.y) {
                                scale(MIST_WIDE, MIST_LOW, pivot = Offset.Zero) { drawCircle(mist, bankRadius, Offset.Zero) }
                            }
                        }
                        for (i in 0 until FLAKES) {
                            val depth = field.depth[i]
                            val swaying = sway * sin(2 * PI * fraction(cycle * sways[i]) + field.phase[i])
                            val x = (fraction(field.x[i] + cycle * blows[i] + swaying / spanX) * spanX).toFloat() - margin
                            val y = (fraction(field.y[i] + cycle * falls[i]) * spanY).toFloat() - margin
                            val radius = nearRadius * (0.4f + 0.6f * depth)
                            val alpha = 0.35f + 0.55f * depth
                            drawCircle(SnowEdge, radius * 1.6f, Offset(x, y), alpha = alpha)
                            drawCircle(Snowflake, radius, Offset(x, y), alpha = alpha)
                        }
                    }
                }
            },
    )
}

private fun fraction(x: Double) = x - floor(x)
