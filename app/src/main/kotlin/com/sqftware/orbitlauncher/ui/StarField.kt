package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private class FieldStar(val x: Float, val y: Float, val radius: Float, val brightness: Float)

// One star to a square of this side, so a bigger screen gets more stars rather than sparser ones.
private val STAR_SPACING = 60.dp

// Seeded, so the sky is the same every time the drawer opens. Squaring skews the field to many dim specks and few
// bright stars, as a real sky is.
private val FieldStars = Random(1790464006).let { random ->
    List(400) {
        val weight = random.nextFloat().let { it * it }
        FieldStar(random.nextFloat(), random.nextFloat(), radius = 0.5f + 0.9f * weight, brightness = 0.3f + 0.7f * weight)
    }
}

/** A faint scatter of stars over the whole draw area, in [colour] at its brightest and scaled by [alpha]. */
fun DrawScope.drawStarField(colour: Color, alpha: Float = 1f) {
    if (alpha <= 0f) return
    val spacing = STAR_SPACING.toPx()
    val count = (size.width * size.height / (spacing * spacing)).toInt().coerceAtMost(FieldStars.size)
    FieldStars.take(count).forEach { star ->
        drawCircle(
            colour.copy(alpha = colour.alpha * star.brightness * alpha),
            radius = star.radius.dp.toPx(),
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }
}
