package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.RingColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The unit offset [degrees] round from the top, clockwise, in screen axes. */
internal fun direction(degrees: Float): Offset {
    val angle = degrees / 180f * PI.toFloat()
    return Offset(sin(angle), -cos(angle))
}

/** Radial marks round [centre] from [from] to [to] out, at those of [count] even steps [kept] by their index. */
internal fun ticks(centre: Offset, count: Int, from: Float, to: Float, kept: (Int) -> Boolean): Path = Path().apply {
    for (index in 0 until count) {
        if (!kept(index)) continue
        val along = direction(index * 360f / count)
        (centre + along * from).let { moveTo(it.x, it.y) }
        (centre + along * to).let { lineTo(it.x, it.y) }
    }
}

/**
 * A ring's marks for a theme whose ring is a dial's scale: a track through the [slots] on a circle of [radius] round
 * [centre], crossed by [minor] and [major] marks, all kept clear of the slots' discs [iconSize] across. They brighten as
 * the ring glows, as [ThemeArt.ringMarks] asks.
 */
internal fun CacheDrawScope.tickedTrack(
    centre: Offset,
    slots: List<Offset>,
    radius: Float,
    iconSize: Float,
    colours: RingColors,
    minor: Path,
    major: Path,
    majorWidth: Dp,
): DrawScope.(glow: Float, alpha: Float) -> Unit {
    val thin = Stroke(1.dp.toPx())
    val thick = Stroke(majorWidth.toPx())
    val discs = Path().apply { slots.forEach { addOval(Rect(it, iconSize / 2 + 3.dp.toPx())) } }
    return { glow, alpha ->
        if (alpha > 0f) {
            clipPath(discs, ClipOp.Difference) {
                drawCircle(colours.mark.copy(alpha = (0.3f + 0.45f * glow) * alpha), radius, centre, style = thin)
                val tick = colours.starLine.copy(alpha = (colours.starLine.alpha + 0.3f * glow) * alpha)
                drawPath(minor, tick.copy(alpha = tick.alpha * 0.6f), style = thin)
                drawPath(major, tick, style = thick)
            }
        }
    }
}

/**
 * A closed wavering line round [centre], like a coast or a stone: a circle rippled [lobes] and two more times round, the
 * ripples set at [first] and [second]. It is drawn at [reach] times each of [steps], so one path holds a coast or all of
 * a hill's contours.
 */
internal fun wavering(
    first: Float,
    second: Float,
    centre: Offset = Offset.Zero,
    reach: Float = 1f,
    steps: List<Float> = listOf(1f),
    lobes: Int = 3,
) = Path().apply {
    val points = 64
    steps.forEach { step ->
        repeat(points) { index ->
            val angle = index * 2 * PI.toFloat() / points
            val wave = 0.9f + 0.07f * sin(lobes * angle + first) + 0.04f * sin((lobes + 2) * angle + second)
            val at = centre + Offset(sin(angle), -cos(angle)) * (reach * step * wave)
            if (index == 0) moveTo(at.x, at.y) else lineTo(at.x, at.y)
        }
        close()
    }
}
