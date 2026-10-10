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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import com.sqftware.orbitlauncher.domain.goesRound
import com.sqftware.orbitlauncher.domain.restingTurn
import com.sqftware.orbitlauncher.domain.turnBetween
import com.sqftware.orbitlauncher.domain.turnRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

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

/** How briskly a caught ring swings back home once the finger lets go: in well under a second, so the catch reads as a stop. */
private const val SETTLE_STIFFNESS = Spring.StiffnessLow

/**
 * How fast the ring's rim must still move for a touch to catch it. Slower, its last creep home is all but invisible,
 * so a tap there opens what it lands on, as it would on a ring at rest.
 */
private val CATCH_SPEED = 30.dp

/**
 * How long a finger rests on the ring before a drag round it spins it. A swipe moves at once, so one that crosses the
 * ring still turns the page; the rest stays well short of a long press, which opens an app's menu.
 */
private const val HOLD_MILLIS = 200L

/**
 * The ring's fidget spin: a drag round the ring, after a brief press if [pressFirst], turns it, and let go it coasts on and settles on the nearest
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
                glide?.cancel()
                turn = 0f
            }
        }

    /** Whether a drag must start with a rest on the ring to spin it, so a page swipe across the ring still turns the page. */
    internal var pressFirst = true

    private var glide: Job? = null

    /** How fast [glide] turns the ring, in radians a second. */
    private var speed = 0f

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

    /** Lets the ring go at [velocity], in radians a second, to coast as far as a spinner would and settle on a whole turn. */
    private fun letGo(velocity: Float) = glide(restingTurn(turn + velocity / COAST_RATE), velocity, COAST_RATE * COAST_RATE)

    /** Swings a caught ring back to the nearest whole turn. */
    private fun settle() = glide(restingTurn(turn), 0f, SETTLE_STIFFNESS)

    private fun glide(rest: Float, velocity: Float, stiffness: Float) {
        val from = turn
        glide = scope.launch {
            // Critically damped, a spring slows as an exponential coast does, but lands on the rest.
            animate(from, rest, velocity, spring(Spring.DampingRatioNoBouncy, stiffness, REST_THRESHOLD)) { value, now ->
                turn = value
                speed = now
            }
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
            val caught = glide?.isActive == true && abs(speed) * outer >= CATCH_SPEED.toPx()
            if (caught) {
                glide?.cancel()
                down.consume()
            }
            // What the finger lands on takes the press for a tap, which the wait below would read as the touch taken.
            awaitPointerEvent(PointerEventPass.Final)
            // The touch slop crossed going round the ring, after a rest if one is asked for or from a catch, makes the
            // touch a spin; anything else is left to the rest. A caught ring is already in hand, so it needs no rest.
            var crossing = awaitSlop(down.id, down.position)
            val rested = caught || !pressFirst || crossing != null && crossing.uptimeMillis - down.uptimeMillis >= HOLD_MILLIS
            if (crossing == null || !rested || (crossing.position - down.position).let { !goesRound(start.x, start.y, it.x, it.y) }) {
                if (caught) settle()
                return@awaitEachGesture
            }

            // A touch too slow to catch the ring leaves its creep home running, which would fight the finger.
            glide?.cancel()

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
