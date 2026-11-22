package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

/**
 * Reads pinches and drags on this node. [onDown] comes as a finger lands. Once the fingers pass the touch slop, [onMove]
 * comes with each move: by what factor they spread, how far they went, and where they were, from the node's middle,
 * which a zoom about that point and then the move keeps the ground under the fingers. [onEnd] comes as the last lifts.
 * A lone finger that rests a long press without passing the slop gets [onHold] there and then, still down, so the user
 * sees it take, even if it pans after; one that lifts sooner gets [onTap] where it lifted. Neither comes if a button
 * over the node consumes the touch, or a cancel comes first.
 */
suspend fun PointerInputScope.detectPinchPan(
    onDown: () -> Unit,
    onMove: (zoom: Float, pan: Offset, anchor: Offset) -> Unit,
    onEnd: () -> Unit,
    onTap: (at: Offset) -> Unit,
    onHold: () -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    val holdMillis = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDown()
        var moving = false
        var held = false
        var fingers = 1
        var last = down
        var zoomed = 1f
        var panned = Offset.Zero
        var anchor = Offset.Zero
        val middle = Offset(size.width / 2f, size.height / 2f)
        fun still() = !held && !moving && fingers == 1 && !down.isConsumed && !last.isConsumed
        while (true) {
            val event = if (still()) {
                withTimeoutOrNull(down.uptimeMillis + holdMillis - last.uptimeMillis) { awaitPointerEvent() }
            } else {
                awaitPointerEvent()
            }
            if (event == null) {
                held = true
                onHold()
                continue
            }
            fingers = maxOf(fingers, event.changes.count { it.pressed })
            last = event.changes.first()
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            if (!moving) {
                zoomed *= zoom
                panned += pan
                moving = abs(1 - zoomed) * event.calculateCentroidSize(useCurrent = false) > slop || panned.getDistance() > slop
            }
            if (moving) {
                event.calculateCentroid(useCurrent = false).takeIf { it.isSpecified }?.let { anchor = it - middle }
                onMove(zoom, pan, anchor)
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
            if (event.changes.none { it.pressed }) break
        }
        if (still()) onTap(last.position)
        onEnd()
    }
}
