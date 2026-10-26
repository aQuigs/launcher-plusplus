package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.MistBank
import com.sqftware.orbitlauncher.ui.theme.SnowEdge
import com.sqftware.orbitlauncher.ui.theme.Snowflake
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** How many flakes fall at once, the near ones the largest, fastest and brightest. */
private const val FLAKES = 90

/** A flake's radius and how far it falls in a second at the nearest depth, and how much of that the farthest keeps. */
private val NEAR_FLAKE = 2.6.dp
private val NEAR_FALL = 70.dp
private const val FAR_SHARE = 0.35f

/** How far the wind carries the snow sideways for each unit it falls, and how far a flake sways either side of its line. */
private const val WIND = 0.25f
private val SWAY = 10.dp

/** How many banks of mist drift across the lower screen, and how long each takes to cross it, in seconds. */
private const val MIST_BANKS = 3
private const val MIST_CROSSING_S = 90f

/**
 * Snow drifting down over the screen on the wind, in three depths, and banks of mist rolling slowly across its lower half,
 * as in the northern game's mountains. They hold still unless [moving], and pick up where they left off.
 */
@Composable
internal fun SnowAndMist(moving: Boolean, modifier: Modifier) {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(moving) {
        if (!moving) return@LaunchedEffect
        val from = seconds.floatValue
        val start = withInfiniteAnimationFrameNanos { it }
        while (true) withInfiniteAnimationFrameNanos { seconds.floatValue = from + (it - start) / 1e9f }
    }
    val flakes = remember { Flakes(Random(FLAKES)) }
    Spacer(
        modifier
            .clearAndSetSemantics {}
            .drawWithCache {
                val nearRadius = NEAR_FLAKE.toPx()
                val nearFall = NEAR_FALL.toPx()
                val sway = SWAY.toPx()
                // Room above and below the screen, so a flake leaves and comes back out of sight.
                val span = size.height + 2 * nearRadius * 2
                val bankRadius = size.width * 0.45f
                val mist = Brush.radialGradient(0f to MistBank, 1f to Color.Transparent, center = Offset.Zero, radius = bankRadius)

                onDrawBehind {
                    val t = seconds.floatValue
                    for (bank in 0 until MIST_BANKS) {
                        val across = (t / MIST_CROSSING_S + bank.toFloat() / MIST_BANKS) % 1f
                        val at = Offset(-bankRadius + across * (size.width + 2 * bankRadius), size.height * (0.55f + 0.14f * bank))
                        // Wide and low, as mist lies.
                        translate(at.x, at.y) {
                            scale(2.6f, 0.7f, pivot = Offset.Zero) {
                                drawCircle(mist, bankRadius, Offset.Zero)
                            }
                        }
                    }
                    for (i in 0 until FLAKES) {
                        val depth = flakes.depth[i]
                        val fall = nearFall * (FAR_SHARE + (1 - FAR_SHARE) * depth)
                        val y = (flakes.y[i] * span + fall * t) % span - nearRadius * 2
                        val x = (flakes.x[i] * size.width + WIND * fall * t + sway * sin(t * flakes.swayRate[i] + flakes.phase[i])).mod(size.width)
                        val radius = nearRadius * (0.4f + 0.6f * depth)
                        val alpha = 0.35f + 0.55f * depth
                        drawCircle(SnowEdge, radius * 1.6f, Offset(x, y), alpha = alpha)
                        drawCircle(Snowflake, radius, Offset(x, y), alpha = alpha)
                    }
                }
            },
    )
}

/** Each flake's start, as shares of the screen across and down, its depth from far (0) to near (1), and its sway. */
private class Flakes(random: Random) {
    val x = FloatArray(FLAKES) { random.nextFloat() }
    val y = FloatArray(FLAKES) { random.nextFloat() }
    val depth = FloatArray(FLAKES) { random.nextFloat() }
    val swayRate = FloatArray(FLAKES) { 0.6f + random.nextFloat() }
    val phase = FloatArray(FLAKES) { random.nextFloat() * 2 * PI.toFloat() }
}
