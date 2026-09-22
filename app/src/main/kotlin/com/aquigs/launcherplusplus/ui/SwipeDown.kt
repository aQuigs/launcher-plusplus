package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Calls [onSwipe] when a finger drags down across this node, wherever on it the finger came down: past the touch slop
 * and at least twice as far down as across. Until then nothing is consumed, so a tap on whatever the finger landed on
 * goes ahead as usual; the move that makes it a swipe is consumed, which cancels that tap and keeps the pager around
 * the node from reading a page swipe. A touch something else has taken first, a long press or the pager, is left to it.
 */
fun Modifier.swipeDown(onSwipe: () -> Unit): Modifier = pointerInput(onSwipe) {
    awaitEachGesture {
        // A pager settling from a fling takes the next touch at once, in the initial pass, which reaches it first.
        val down = awaitFirstDown(pass = PointerEventPass.Initial)
        // What the finger lands on then takes the press for a tap, which the wait below would read as the touch taken.
        awaitPointerEvent(PointerEventPass.Final)
        val swipe = awaitSlop(down.id, down.position) ?: return@awaitEachGesture
        val moved = swipe.position - down.position
        if (moved.y > 2 * abs(moved.x)) {
            swipe.consume()
            onSwipe()
        }
    }
}

/**
 * The move that takes [pointer] past the touch slop from [start], or null once it lifts or another gesture takes it.
 * It decides at that first crossing, where Compose's awaitTouchSlopOrCancellation watches on: once the pager follows a
 * page swipe, the finger barely moves across the page it drags, and the drift down alone would read as a swipe.
 */
private suspend fun AwaitPointerEventScope.awaitSlop(pointer: PointerId, start: Offset): PointerInputChange? {
    while (true) {
        val change = awaitPointerEvent().changes.firstOrNull { it.id == pointer }
        if (change == null || change.isConsumed || !change.pressed) return null
        if ((change.position - start).getDistance() > viewConfiguration.touchSlop) return change
        // The pager reads the touch after this node, so what it takes shows in the final pass.
        if (awaitPointerEvent(PointerEventPass.Final).changes.any { it.id == pointer && it.isConsumed }) return null
    }
}
