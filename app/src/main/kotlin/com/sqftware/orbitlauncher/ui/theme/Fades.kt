package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

private val FADE = 56.dp

// Only its alpha counts: the fade takes away what is under the mask's opaque part.
private val Mask = Color.Black

/**
 * Fades what scrolls along [orientation] out at an edge with more beyond it, so a cut-off item reads as the start of
 * more rather than as clipped. [hidden] gives the px past the start and the end, the fade deepening over the first
 * [FADE] of it, so it never snaps on. The fade is drawn in a layer, offscreen only while something is hidden.
 */
fun Modifier.fadingEdges(orientation: Orientation, hidden: () -> Pair<Float, Float>): Modifier = this
    .graphicsLayer {
        val (before, after) = hidden()
        compositingStrategy = if (before > 0f || after > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
    .drawWithCache {
        val fade = FADE.toPx()
        val across = orientation == Orientation.Horizontal
        val edge = if (across) Size(fade, size.height) else Size(size.width, fade)
        val length = if (across) size.width else size.height
        val startFade = if (across) {
            Brush.horizontalGradient(0f to Mask, 1f to Color.Transparent, endX = fade)
        } else {
            Brush.verticalGradient(0f to Mask, 1f to Color.Transparent, endY = fade)
        }
        val endFade = if (across) {
            Brush.horizontalGradient(0f to Color.Transparent, 1f to Mask, startX = length - fade, endX = length)
        } else {
            Brush.verticalGradient(0f to Color.Transparent, 1f to Mask, startY = length - fade, endY = length)
        }
        val endAt = if (across) Offset(length - fade, 0f) else Offset(0f, length - fade)
        onDrawWithContent {
            drawContent()
            val (before, after) = hidden()
            // A row runs from the right in a right-to-left layout, so what is before it lies on the right.
            val mirrored = across && layoutDirection == LayoutDirection.Rtl
            val (start, end) = if (mirrored) after to before else before to after
            if (start > 0f) drawRect(startFade, size = edge, alpha = (start / fade).coerceAtMost(1f), blendMode = BlendMode.DstOut)
            if (end > 0f) drawRect(endFade, endAt, edge, alpha = (end / fade).coerceAtMost(1f), blendMode = BlendMode.DstOut)
        }
    }

/** [fadingEdges] for a lazy list or grid, which knows only whether there is more, so it fades in full while there is. */
fun Modifier.fadingEdges(state: ScrollableState, orientation: Orientation = Orientation.Vertical): Modifier =
    fadingEdges(orientation) {
        (if (state.canScrollBackward) Float.MAX_VALUE else 0f) to (if (state.canScrollForward) Float.MAX_VALUE else 0f)
    }

/**
 * A row that scrolls sideways within its [inset], with [fadingEdges] where there is more. The fade's layer clips:
 * [bleed] is how far an item may rise or sink out of the row, such as a lifted card, and the layer reaches that far past
 * the row without taking room from what is around it.
 */
fun Modifier.sidewaysScroll(state: ScrollState, inset: Dp = Spacing.Edge, bleed: Dp = 0.dp): Modifier = this
    .layout { measurable, constraints ->
        val extra = bleed.roundToPx()
        val placeable = measurable.measure(constraints.offset(vertical = 2 * extra))
        layout(placeable.width, placeable.height - 2 * extra) { placeable.place(0, -extra) }
    }
    .fadingEdges(Orientation.Horizontal) { state.value.toFloat() to (state.maxValue - state.value).toFloat() }
    .horizontalScroll(state)
    .padding(horizontal = inset, vertical = bleed)
