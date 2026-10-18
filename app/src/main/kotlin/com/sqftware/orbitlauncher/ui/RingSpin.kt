package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.Spin
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
 * The ring's fidget spin: a drag up or down round the ring's sides turns it, and let go it coasts on and settles on the
 * nearest whole turn, so it always ends as it shows otherwise. Only how the ring looks turns, never the order of its items.
 * Each spin is handed to [onSpin] once it rests or is caught.
 */
@Stable
class RingSpin internal constructor(private val scope: CoroutineScope, private val onSpin: (Spin) -> Unit) {
    /** How far the ring is turned, in radians clockwise. */
    var turn by mutableFloatStateOf(0f)
        private set

    /**
     * What may spin, the ring or an open folder, or null for nothing. Whenever it changes, what spun is back in place at
     * once, since slots are hit where they are shown, and a spin never carries over to what spins next.
     */
    internal var spinning: Any? = HomePlace.Ring
        set(value) {
            if (value != field) {
                glide?.cancel()
                turn = 0f
            }
            field = value
        }

    private var glide: Job? = null

    /** How fast [glide] turns the ring, in radians a second. */
    private var speed = 0f

    /** [CATCH_SPEED] in radians a second, at the ring's rim as the last gesture found it. */
    private var catchSpeed = 0f

    private fun catchable() = abs(speed) >= catchSpeed

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
        val ring = ring?.takeIf { spinning != null && it.isAttached } ?: return null
        val area = area?.takeIf { it.isAttached } ?: return null
        return area.localToRoot(position) - ring.localToRoot(ring.size.center.toOffset())
    }

    /**
     * Lets the ring go at [velocity], in radians a second, to coast as far as a spinner would and settle on a whole turn,
     * which ends the spin [tally] counts.
     */
    private fun letGo(velocity: Float, tally: Tally) =
        glide(restingTurn(turn + velocity / COAST_RATE), velocity, COAST_RATE * COAST_RATE, tally)

    /** Swings a caught ring back to the nearest whole turn. */
    private fun settle() = glide(restingTurn(turn), 0f, SETTLE_STIFFNESS, tally = null)

    private fun glide(rest: Float, velocity: Float, stiffness: Float, tally: Tally?) {
        val motion = AnimationState(turn, velocity)
        glide = scope.launch {
            // Timed by the animation's frames, and only while it could be caught, so neither the spring's invisible last creep to
            // rest nor the time the launcher spends out of sight mid-coast counts.
            var coastNanos = 0L
            try {
                // Critically damped, a spring slows as an exponential coast does, but lands on the rest.
                motion.animateTo(rest, spring(Spring.DampingRatioNoBouncy, stiffness, REST_THRESHOLD)) {
                    tally?.add(value - turn)
                    turn = value
                    speed = this.velocity
                    if (catchable()) coastNanos = lastFrameTimeNanos - startTimeNanos
                }
                turn = 0f
            } finally {
                // However the coast ends: at rest, caught, or cut short as what spins changes.
                tally?.let { onSpin(it.spin(velocity, coast = coastNanos / 1e9)) }
            }
        }
    }

    /** Where a drag round the ring spins it: a node over the ring and what lies behind it, so the gap between its icons takes a drag too. */
    internal val modifier: Modifier = Modifier.onPlaced { area = it }.pointerInput(this) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val start = fromCentre(down.position)?.takeIf { it.getDistance() in inner..outer } ?: return@awaitEachGesture
            // Caught, it stops under the finger as a spinner does, before what the finger lands on sees the touch, so the
            // catch launches nothing.
            catchSpeed = CATCH_SPEED.toPx() / outer
            val caught = glide?.isActive == true && catchable()
            if (caught) {
                glide?.cancel()
                down.consume()
            }
            // What the finger lands on takes the press for a tap, which the wait below would read as the touch taken.
            awaitPointerEvent(PointerEventPass.Final)
            // The touch slop crossed going round the ring makes the touch a spin, but only up or down its sides, so a swipe
            // across its top or bottom still turns the page; a caught ring is in hand, and any drag round it spins it.
            // Anything else is left to the rest.
            var crossing = awaitSlop(down.id, down.position)
            val moved = crossing?.let { it.position - down.position }
            val spun = moved != null && goesRound(start.x, start.y, moved.x, moved.y) && (caught || abs(moved.y) >= abs(moved.x))
            if (!spun) {
                if (caught) settle()
                return@awaitEachGesture
            }

            // A touch too slow to catch the ring leaves its creep home running, which would fight the finger.
            glide?.cancel()

            val velocity = VelocityTracker()
            val tally = Tally()
            var at = start
            velocity.addPosition(down.uptimeMillis, down.position)
            while (crossing != null && crossing.pressed) {
                crossing.consume()
                val now = fromCentre(crossing.position) ?: break
                // Over the emblem the finger's angle swings wildly, and at the very centre it has none.
                if (now.getDistance() >= inner) {
                    val by = turnBetween(at.x, at.y, now.x, now.y)
                    turn += by
                    tally.add(by)
                    at = now
                }
                velocity.addPosition(crossing.uptimeMillis, crossing.position)
                crossing = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            }
            crossing?.consume()
            letGo(velocity.calculateVelocity().let { turnRate(at.x, at.y, it.x, it.y) }, tally)
        }
    }

    /** How far a spin under way has turned each way, in radians. */
    private class Tally {
        private var clockwise = 0.0
        private var anticlockwise = 0.0

        fun add(by: Float) {
            if (by > 0) clockwise += by else anticlockwise -= by
        }

        fun spin(letGo: Float, coast: Double) = Spin(clockwise, anticlockwise, letGo.toDouble(), coast)
    }
}

/** The fidget spin of a ring or folder, handing each spin to [onSpin] once it is over. */
@Composable
fun rememberRingSpin(onSpin: (Spin) -> Unit = {}): RingSpin {
    val scope = rememberCoroutineScope()
    val latestOnSpin by rememberUpdatedState(onSpin)
    return remember(scope) { RingSpin(scope) { latestOnSpin(it) } }
}

fun Modifier.spinsRing(spin: RingSpin): Modifier = then(spin.modifier)
