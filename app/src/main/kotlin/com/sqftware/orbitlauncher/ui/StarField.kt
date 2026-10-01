package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private class FieldStar(val x: Float, val y: Float, val radius: Float, val brightness: Float)

private val STAR_SPACING = 60.dp

// Seeded, so the sky is the same every time the drawer opens. Squaring skews the field to many dim specks and few
// bright stars, as a real sky is.
private val FieldStars = Random(1790464006).let { random ->
    List((DRAWER_TILE_WIDTH.value * DRAWER_TILE_HEIGHT.value / (STAR_SPACING.value * STAR_SPACING.value)).toInt()) {
        val weight = random.nextFloat().let { it * it }
        FieldStar(random.nextFloat(), random.nextFloat(), radius = 0.5f + 0.9f * weight, brightness = 0.3f + 0.7f * weight)
    }
}

/** A faint scatter of stars over the whole draw area, in [colour] at its brightest, drifting with the drawer's tiles. */
fun DrawScope.drawStarField(colour: Color, scrolled: Float = 0f) {
    if (colour.alpha <= 0f) return
    forEachDrawerTile(scrolled) { origin, tile ->
        FieldStars.forEach { star ->
            val centre = origin + Offset(star.x * tile.width, star.y * tile.height)
            if (centre.x < size.width && centre.y >= 0f && centre.y < size.height) {
                drawCircle(colour.copy(alpha = colour.alpha * star.brightness), star.radius.dp.toPx(), centre)
            }
        }
    }
}
