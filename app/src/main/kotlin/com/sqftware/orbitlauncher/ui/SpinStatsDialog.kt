package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.SpinStats

/** Where the two directions meet on the dial, in degrees, so each reads as its own arc. */
private const val DIAL_GAP = 8f

/** Shows [stats], the tally of every spin of the ring and its folders. Done, Back and a tap outside call [onDismiss]. */
@Composable
fun SpinStatsDialog(stats: SpinStats, onDismiss: () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    fun decimal(n: Double, places: Int) = String.format(locale, "%,.${places}f", n)

    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = { LauncherButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Spin stats") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SpinDial(stats, count = String.format(locale, "%,d", stats.spins))
                if (stats.spins == 0) {
                    Text(
                        "Drag up or down round the ring's side and let go to start counting.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // As tall as each other, though one label wraps.
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DirectionTile(
                            MaterialTheme.colorScheme.primary,
                            decimal(stats.clockwise, 1),
                            "turns clockwise",
                            Modifier.weight(1f),
                        )
                        DirectionTile(
                            MaterialTheme.colorScheme.tertiary,
                            decimal(stats.anticlockwise, 1),
                            "turns counterclockwise",
                            Modifier.weight(1f),
                            mirrored = true,
                        )
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Records", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RecordRow(LoopGlyph, "Most in one spin", "${decimal(stats.mostTurns, 1)} turns")
                        RecordRow(SpeedGlyph, "Top speed", "${decimal(stats.topRpm, 0)} rpm")
                        RecordRow(TimerGlyph, "Longest coast", "${decimal(stats.longestCoast, 1)} s")
                    }
                }
            }
        },
        modifier = Modifier.testTag(LauncherMenuTags.SPIN_STATS_DIALOG),
    )
}

/** A ring split by how much of all the turning went each way, round the [count] of spins. */
@Composable
private fun SpinDial(stats: SpinStats, count: String) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val clockwise = MaterialTheme.colorScheme.primary
    val anticlockwise = MaterialTheme.colorScheme.tertiary
    val total = stats.clockwise + stats.anticlockwise

    Box(Modifier.size(136.dp).semantics(mergeDescendants = true) {}, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = 12.dp.toPx()
            fun arc(colour: Color, from: Float, sweep: Float) = drawArc(
                colour,
                from,
                sweep,
                useCenter = false,
                topLeft = Offset(width / 2, width / 2),
                size = Size(size.width - width, size.height - width),
                style = Stroke(width),
            )

            if (total <= 0) {
                arc(track, 0f, 360f)
                return@Canvas
            }
            val share = (stats.clockwise / total).toFloat()
            val gap = if (share > 0f && share < 1f) DIAL_GAP else 0f
            // Both start from the top: the clockwise turns go round to the right, the anticlockwise ones to the left.
            arc(clockwise, -90f + gap / 2, (360f * share - gap).coerceAtLeast(0f))
            arc(anticlockwise, -90f - gap / 2, -(360f * (1 - share) - gap).coerceAtLeast(0f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, style = MaterialTheme.typography.headlineLarge)
            Text(
                if (stats.spins == 1) "spin" else "spins",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DirectionTile(tint: Color, value: String, label: String, modifier: Modifier, mirrored: Boolean = false) {
    Pane(modifier.fillMaxHeight().semantics(mergeDescendants = true) {}) {
        Column(Modifier.padding(12.dp)) {
            Icon(
                RotateRightGlyph,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp).graphicsLayer { if (mirrored) scaleX = -1f },
            )
            Spacer(Modifier.size(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = tint)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecordRow(glyph: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Icon(glyph, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private val RotateRightGlyph = materialGlyph(
    "RotateRight",
    "M15.55 5.55L11 1v3.07C7.06 4.56 4 7.92 4 12s3.05 7.44 7 7.93v-2.02c-2.84-.48-5-2.94-5-5.91s2.16-5.43 5-5.91V10l4.55-4.45z" +
        "M19.93 11c-.17-1.39-.72-2.73-1.62-3.89l-1.42 1.42c.54.75.88 1.6 1.02 2.47h2.02zM13 17.9v2.02c1.39-.17 2.74-.71 3.9-1.61" +
        "l-1.44-1.44c-.75.54-1.59.89-2.46 1.03zm3.89-2.42l1.42 1.41c.9-1.16 1.45-2.5 1.62-3.89h-2.02c-.14.87-.48 1.72-1.02 2.48z",
)
private val LoopGlyph = materialGlyph(
    "Autorenew",
    "M12 6v3l4-4-4-4v3c-4.42 0-8 3.58-8 8 0 1.57.46 3.03 1.24 4.26L6.7 14.8c-.45-.83-.7-1.79-.7-2.8 0-3.31 2.69-6 6-6z" +
        "m6.76 1.74L17.3 9.2c.44.84.7 1.79.7 2.8 0 3.31-2.69 6-6 6v-3l-4 4 4 4v-3c4.42 0 8-3.58 8-8 0-1.57-.46-3.03-1.24-4.26z",
)
private val SpeedGlyph = materialGlyph(
    "Speed",
    "M20.38 8.57l-1.23 1.85a8 8 0 0 1-.22 7.58H5.07A8 8 0 0 1 15.58 6.85l1.85-1.23A10 10 0 0 0 3.35 19a2 2 0 0 0 1.72 1h13.85" +
        "a2 2 0 0 0 1.74-1 10 10 0 0 0-.27-10.44zm-9.79 6.84a2 2 0 0 0 2.83 0l5.66-8.49-8.49 5.66a2 2 0 0 0 0 2.83z",
)
private val TimerGlyph = materialGlyph(
    "Timer",
    "M15 1H9v2h6V1zm-4 13h2V8h-2v6zm8.03-6.61l1.42-1.42c-.43-.51-.9-.99-1.41-1.41l-1.42 1.42C16.07 4.74 14.12 4 12 4" +
        "c-4.97 0-9 4.03-9 9s4.02 9 9 9 9-4.03 9-9c0-2.12-.74-4.07-1.97-5.61zM12 20c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z",
)
