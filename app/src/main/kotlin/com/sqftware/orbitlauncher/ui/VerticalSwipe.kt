package com.sqftware.orbitlauncher.ui

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
 * Calls [onDown] or [onUp] when a finger drags down or up across this node, wherever on it the finger came down: past
 * the touch slop and at least twice as far along as across. Until then nothing is consumed, so a tap on whatever the
 * finger landed on goes ahead as usual; from the move that makes it a swipe until the last finger lifts, every change is
 * consumed, which cancels that tap and keeps the pager around the node from reading a page swipe. A touch something else
 * has taken first, a long press or the pager, is left to it.
 */
fun Modifier.verticalSwipe(onDown: () -> Unit, onUp: () -> Unit): Modifier = pointerInput(onDown, onUp) {
    awaitEachGesture {
        // A pager settling from a fling takes the next touch at once, in the initial pass, which reaches it first.
        val down = awaitFirstDown(pass = PointerEventPass.Initial)
        // What the finger lands on then takes the press for a tap, which the wait below would read as the touch taken.
        awaitPointerEvent(PointerEventPass.Final)
        val swipe = awaitSlop(down.id, down.position) ?: return@awaitEachGesture
        val moved = swipe.position - down.position
        if (abs(moved.y) > 2 * abs(moved.x)) {
            swipe.consume()
            if (moved.y > 0) onDown() else onUp()
            // The finger often carries on as the shade or the drawer opens over it, and the pager takes a touch back the
            // moment its moves stop being consumed, so a drift across would page behind it.
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * The move that takes [pointer] past the touch slop from [start], or null once it lifts or another gesture takes it.
 * It decides at that first crossing, where Compose's awaitTouchSlopOrCancellation watches on: once the pager follows a
 * page swipe, the finger barely moves across the page it drags, and its drift up or down alone would read as a swipe.
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
