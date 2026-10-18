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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FADE = 32.dp

// Only its alpha counts: the fade keeps what is under the mask's opaque part.
private val Mask = Color.Black

/**
 * A row that scrolls sideways within its [inset], fading out at an edge with more beyond it, so a cut-off item reads as
 * the start of more rather than as clipped.
 */
fun Modifier.sidewaysScroll(state: ScrollState, inset: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = FADE.toPx()
        if (state.value > 0) {
            drawRect(Brush.horizontalGradient(0f to Color.Transparent, 1f to Mask, endX = fade), size = Size(fade, size.height), blendMode = BlendMode.DstIn)
        }
        if (state.value < state.maxValue) {
            val start = size.width - fade
            drawRect(
                Brush.horizontalGradient(0f to Mask, 1f to Color.Transparent, startX = start, endX = size.width),
                topLeft = Offset(start, 0f),
                size = Size(fade, size.height),
                blendMode = BlendMode.DstIn,
            )
        }
    }
    .horizontalScroll(state)
    .padding(horizontal = inset)
