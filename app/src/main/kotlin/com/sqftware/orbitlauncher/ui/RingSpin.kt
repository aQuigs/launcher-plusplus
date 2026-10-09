package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import com.sqftware.orbitlauncher.domain.goesRound
import com.sqftware.orbitlauncher.domain.restingTurn
import com.sqftware.orbitlauncher.domain.turnBetween
import com.sqftware.orbitlauncher.domain.turnRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * How fast a ring let go slows, per second: its speed falls by e every 1 / this seconds, so it coasts a few seconds,
 * as a fidget spinner does.
 */
private const val COAST_RATE = 1.4f

/**
 * How close, in radians, the coast comes to its rest before it ends there: under half a pixel on a phone. The stock
 * threshold of a hundredth is several pixels, which showed as a jump when the ring came to rest.
 */
private const val REST_THRESHOLD = 0.001f

/**
 * The ring's fidget spin: a quick drag round the ring turns it, and let go it coasts on and settles on the nearest
 * whole turn, so it always ends as it shows otherwise. Only how the ring looks turns, never the order of its items.
 */
@Stable
class RingSpin internal constructor(private val scope: CoroutineScope) {
    /** How far the ring is turned, in radians clockwise. */
    var turn by mutableFloatStateOf(0f)
        private set

    /** Whether the ring may spin: off, it is back in place at once, since its slots are hit where they are shown. */
    internal var spins = true
        set(value) {
            field = value
            if (!value) {
                stop()
                turn = 0f
            }
        }

    private var glide: Job? = null

    // Read only by the gesture, so not state: the ring as its layout last placed it, the band round its centre a drag
    // starts a spin from, and the area the drag is made in.
    private var ring: LayoutCoordinates? = null
    private var inner = 0f
    private var outer = 0f
    private var area: LayoutCoordinates? = null

    /** Where the ring is: [coordinates] centred on it, with its band from [inner] to [outer] from the centre. */
    internal fun place(coordinates: LayoutCoordinates, inner: Float, outer: Float) {
        ring = coordinates
        this.inner = inner
        this.outer = outer
    }

    /** [position] in the area the spin is dragged in, as an offset from the ring's centre, if the ring can spin. */
    private fun fromCentre(position: Offset): Offset? {
        val ring = ring?.takeIf { spins && it.isAttached } ?: return null
        val area = area?.takeIf { it.isAttached } ?: return null
        return area.localToRoot(position) - ring.localToRoot(ring.size.center.toOffset())
    }

    /** Stops a spin under the finger, as a spinner caught in the hand does. Whether it was spinning. */
    private fun stop(): Boolean {
        val spinning = glide?.isActive == true
        glide?.cancel()
        return spinning
    }

    /** Lets the ring go at [velocity], in radians a second, to coast as far as a spinner would and settle on a whole turn. */
    private fun letGo(velocity: Float) {
        val from = turn
        val rest = restingTurn(from + velocity / COAST_RATE)
        glide = scope.launch {
            // Critically damped at the coast rate, a spring slows as an exponential coast does, but lands on the rest.
            animate(from, rest, velocity, spring(Spring.DampingRatioNoBouncy, COAST_RATE * COAST_RATE, REST_THRESHOLD)) { value, _ -> turn = value }
            turn = 0f
        }
    }

    /** Where a drag round the ring spins it: a node over the ring and what lies behind it, so the gap between its icons takes a drag too. */
    internal val modifier: Modifier = Modifier.onPlaced { area = it }.pointerInput(this) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val start = fromCentre(down.position)?.takeIf { it.getDistance() in inner..outer } ?: return@awaitEachGesture
            // Caught, it stops under the finger as a spinner does, before what the finger lands on sees the touch, so the
            // catch launches nothing.
            val caught = stop()
            if (caught) down.consume()
            // What the finger lands on takes the press for a tap, which the wait below would read as the touch taken.
            awaitPointerEvent(PointerEventPass.Final)
            // The touch slop crossed going round the ring makes the touch a spin; anything else is left to the rest.
            var crossing = awaitSlop(down.id, down.position)
            if (crossing == null || (crossing.position - down.position).let { !goesRound(start.x, start.y, it.x, it.y) }) {
                if (caught) letGo(0f)
                return@awaitEachGesture
            }

            val velocity = VelocityTracker()
            var at = start
            velocity.addPosition(down.uptimeMillis, down.position)
            while (crossing != null && crossing.pressed) {
                crossing.consume()
                val now = fromCentre(crossing.position) ?: break
                // Over the emblem the finger's angle swings wildly, and at the very centre it has none.
                if (now.getDistance() >= inner) {
                    turn += turnBetween(at.x, at.y, now.x, now.y)
                    at = now
                }
                velocity.addPosition(crossing.uptimeMillis, crossing.position)
                crossing = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            }
            crossing?.consume()
            letGo(velocity.calculateVelocity().let { turnRate(at.x, at.y, it.x, it.y) })
        }
    }
}

@Composable
fun rememberRingSpin(): RingSpin {
    val scope = rememberCoroutineScope()
    return remember(scope) { RingSpin(scope) }
}

fun Modifier.spinsRing(spin: RingSpin): Modifier = then(spin.modifier)
