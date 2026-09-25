package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A vertical scroll for a page of the pager. A touch while the page still coasts from a fling, or springs back from its
 * edge, goes either way: a move across turns the page, one up or down scrolls. Compose's own scroll takes such a touch
 * on the down, before it has moved, and keeps it to its axis, so the pager never sees the move across.
 */
@Composable
fun Modifier.verticalPageScroll(): Modifier {
    val state = rememberScrollState()
    val scope = rememberCoroutineScope()
    val overscroll = rememberOverscrollEffect()
    val stock = ScrollableDefaults.flingBehavior()
    val fling = remember(state, scope, overscroll, stock) { CoastingFling(state, scope, overscroll, stock) }
    val edge = remember(overscroll) { overscroll?.let(::NeverInProgress) }
    return pointerInput(fling) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            // Only to stop the page, as a touch on a coasting list does everywhere, not to tap what it lands on.
            if (fling.stop()) down.consume()
        }
    }.verticalScroll(state, overscrollEffect = edge, flingBehavior = fling)
}

/**
 * Coasts with the stock fling, only outside the scroll's own mutation, so the scroll is not "in progress" while it
 * runs.
 */
private class CoastingFling(
    private val state: ScrollState,
    private val scope: CoroutineScope,
    private val overscroll: OverscrollEffect?,
    private val stock: FlingBehavior,
) : FlingBehavior {
    private var coast: Job? = null
    private val outside = object : ScrollScope {
        override fun scrollBy(pixels: Float) = state.dispatchRawDelta(pixels)
    }

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        stop()
        coast = scope.launch {
            // The overscroll effect takes the finger's velocity, the opposite of the scroll's, and stretches the edge
            // with what the coast leaves when it gets there.
            if (overscroll == null) {
                glide(initialVelocity)
            } else {
                overscroll.applyToFling(Velocity(0f, -initialVelocity)) { Velocity(0f, -glide(-it.y)) }
            }
        }
        return 0f
    }

    private suspend fun glide(velocity: Float) = with(stock) { outside.performFling(velocity) }

    /** Whether it was coasting. */
    fun stop(): Boolean {
        val coasting = coast?.isActive == true
        coast?.cancel()
        return coasting
    }
}

/** An edge springing back is "in progress" too, which would also have the scroll take a touch on the down. */
private class NeverInProgress(effect: OverscrollEffect) : OverscrollEffect by effect {
    override val isInProgress = false
}
