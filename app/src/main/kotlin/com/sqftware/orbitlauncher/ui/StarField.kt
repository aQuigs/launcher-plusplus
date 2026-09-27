package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.random.Random

private class FieldStar(val x: Float, val y: Float, val radius: Float, val brightness: Float)

// The field is a tile a phone's screen fits in, repeated beyond it. A star sits a fixed distance from the top left, not
// at a share of the area drawn, so when the keyboard shrinks the drawer for a search it covers stars rather than
// squeezing them up.
private val TILE_WIDTH = 480.dp
private val TILE_HEIGHT = 960.dp
private val STAR_SPACING = 60.dp

// Seeded, so the sky is the same every time the drawer opens. Squaring skews the field to many dim specks and few
// bright stars, as a real sky is.
private val FieldStars = Random(1790464006).let { random ->
    List((TILE_WIDTH.value * TILE_HEIGHT.value / (STAR_SPACING.value * STAR_SPACING.value)).toInt()) {
        val weight = random.nextFloat().let { it * it }
        FieldStar(random.nextFloat(), random.nextFloat(), radius = 0.5f + 0.9f * weight, brightness = 0.3f + 0.7f * weight)
    }
}

// A far sky drifts behind what scrolls over it at a fraction of its pace.
private const val PARALLAX = 0.25f

/**
 * A faint scatter of stars over the whole draw area, in [colour] at its brightest, drifting up by a share of how far
 * the content over it has [scrolled].
 */
fun DrawScope.drawStarField(colour: Color, scrolled: Float = 0f) {
    if (colour.alpha <= 0f) return
    val tileWidth = TILE_WIDTH.toPx()
    val tileHeight = TILE_HEIGHT.toPx()
    val top = -(scrolled * PARALLAX).mod(tileHeight)
    repeat(ceil(size.width / tileWidth).toInt()) { column ->
        repeat(ceil((size.height - top) / tileHeight).toInt()) { row ->
            FieldStars.forEach { star ->
                val centre = Offset((column + star.x) * tileWidth, top + (row + star.y) * tileHeight)
                if (centre.x < size.width && centre.y >= 0f && centre.y < size.height) {
                    drawCircle(colour.copy(alpha = colour.alpha * star.brightness), star.radius.dp.toPx(), centre)
                }
            }
        }
    }
}
