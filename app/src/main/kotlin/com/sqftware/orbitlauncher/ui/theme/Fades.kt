package com.sqftware.orbitlauncher.ui.theme

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val FADE = 56.dp

// Only its alpha counts: the fade keeps what is under the mask's opaque part.
private val Mask = Color.Black

/**
 * A row that scrolls sideways within its [inset], fading out at an edge with more beyond it, so a cut-off item reads as
 * the start of more rather than as clipped. The fade deepens over the first [FADE] of scroll, so it never snaps on.
 *
 * The fade is drawn in a layer the row's size, which clips what is drawn outside it: room for an item to rise into,
 * such as a lifted card, goes in padding after this modifier.
 */
fun Modifier.sidewaysScroll(state: ScrollState, inset: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val before = state.value
        val after = state.maxValue - state.value
        val (left, right) = if (layoutDirection == LayoutDirection.Rtl) after to before else before to after
        val fade = FADE.toPx()
        fadeEdge(left, fade, from = 0f, to = fade)
        fadeEdge(right, fade, from = size.width, to = size.width - fade)
    }
    .horizontalScroll(state)
    .padding(horizontal = inset)

/** Fades the content from [from], at the edge, to [to], as deep as the [hidden] px beyond it allow. */
private fun DrawScope.fadeEdge(hidden: Int, fade: Float, from: Float, to: Float) {
    if (hidden <= 0) return
    val depth = (hidden / fade).coerceAtMost(1f)
    drawRect(
        Brush.horizontalGradient(0f to Mask.copy(alpha = 1f - depth), 1f to Mask, startX = from, endX = to),
        topLeft = Offset(min(from, to), 0f),
        size = Size(fade, size.height),
        blendMode = BlendMode.DstIn,
    )
}
