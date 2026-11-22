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
 * which a zoom about that point and then the move keeps the ground under the fingers. [onEnd] comes as the last lifts,
 * after [onStill] if one finger lifted without passing the slop, and nothing inside took the touch: where it lifted,
 * and whether it was held past a long press. A finger often rests a moment before it pans, so a hold is known only once
 * it lifts. A button over the node, or a cancel, consumes the touch.
 */
suspend fun PointerInputScope.detectPinchPan(
    onDown: () -> Unit,
    onMove: (zoom: Float, pan: Offset, anchor: Offset) -> Unit,
    onEnd: () -> Unit,
    onStill: (at: Offset, held: Boolean) -> Unit = { _, _ -> },
) {
    val slop = viewConfiguration.touchSlop
    val holdMillis = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDown()
        var moving = false
        var fingers = 1
        var last = down
        var zoomed = 1f
        var panned = Offset.Zero
        var anchor = Offset.Zero
        val middle = Offset(size.width / 2f, size.height / 2f)
        do {
            val event = awaitPointerEvent()
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
        } while (event.changes.any { it.pressed })
        if (!moving && fingers == 1 && !down.isConsumed && !last.isConsumed) onStill(last.position, last.uptimeMillis - down.uptimeMillis >= holdMillis)
        onEnd()
    }
}
