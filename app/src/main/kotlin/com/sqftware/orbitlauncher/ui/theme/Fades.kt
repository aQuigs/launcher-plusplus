package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.foundation.ScrollState
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
 * A row that scrolls sideways within its [inset], fading out at an edge with more beyond it, so a cut-off item reads as
 * the start of more rather than as clipped. The fade deepens over the first [FADE] of scroll, so it never snaps on.
 *
 * The fade is drawn in a layer, which clips: [bleed] is how far an item may rise or sink out of the row, such as a
 * lifted card, and the layer reaches that far past the row without taking room from what is around it.
 */
fun Modifier.sidewaysScroll(state: ScrollState, inset: Dp = Spacing.Edge, bleed: Dp = 0.dp): Modifier = this
    .layout { measurable, constraints ->
        val extra = bleed.roundToPx()
        val placeable = measurable.measure(constraints.offset(vertical = 2 * extra))
        layout(placeable.width, placeable.height - 2 * extra) { placeable.place(0, -extra) }
    }
    // Offscreen only while there is more to scroll to, the only time anything fades.
    .graphicsLayer { compositingStrategy = if (state.maxValue > 0) CompositingStrategy.Offscreen else CompositingStrategy.Auto }
    .drawWithCache {
        val fade = FADE.toPx()
        val edge = Size(fade, size.height)
        val leftFade = Brush.horizontalGradient(0f to Mask, 1f to Color.Transparent, endX = fade)
        val rightStart = size.width - fade
        val rightFade = Brush.horizontalGradient(0f to Color.Transparent, 1f to Mask, startX = rightStart, endX = size.width)
        onDrawWithContent {
            drawContent()
            val before = state.value
            val after = state.maxValue - state.value
            val (left, right) = if (layoutDirection == LayoutDirection.Rtl) after to before else before to after
            if (left > 0) drawRect(leftFade, size = edge, alpha = (left / fade).coerceAtMost(1f), blendMode = BlendMode.DstOut)
            if (right > 0) {
                drawRect(rightFade, Offset(rightStart, 0f), edge, alpha = (right / fade).coerceAtMost(1f), blendMode = BlendMode.DstOut)
            }
        }
    }
    .horizontalScroll(state)
    .padding(horizontal = inset, vertical = bleed)
