package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.EMBLEM_FRACTION
import com.aquigs.launcherplusplus.domain.RING_RADIUS_FRACTION
import com.aquigs.launcherplusplus.domain.ringIconSize
import com.aquigs.launcherplusplus.domain.ringSlotOffset
import kotlin.math.min
import kotlin.math.roundToInt

object HomeRingTags {
    const val EMBLEM = "ring_emblem"

    fun slot(app: AppEntry) = "ring_${app.key}"
}

private val FULL_ICON_SIZE = 64.dp

/**
 * The [ring] of favourite apps round a static emblem. Tap an icon to launch it or long-press it for its [menu]; tap the
 * emblem to choose the favourites on the ring and in the dock.
 * With [showHint] the emblem invites you to add apps instead of showing its mark.
 */
@Composable
fun HomeRing(
    ring: List<AppEntry>,
    showHint: Boolean,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    menu: AppMenu? = null,
) {
    val track = MaterialTheme.colorScheme.outlineVariant

    Layout(
        content = {
            Emblem(showHint = showHint, onClick = onEdit)
            ring.forEach { app -> key(app.key) { AppIcon(app, icon, onLaunch, Modifier.testTag(HomeRingTags.slot(app)), menu) } }
        },
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawCircle(track, radius = size.minDimension * RING_RADIUS_FRACTION, style = Stroke(1.dp.toPx())) },
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val radius = side * RING_RADIUS_FRACTION
        val emblemSize = (side * EMBLEM_FRACTION).roundToInt()
        val iconSize = ringIconSize(FULL_ICON_SIZE.toPx(), side, ring.size).roundToInt()
        val emblem = measurables.first().measure(Constraints.fixed(emblemSize, emblemSize))
        val icons = measurables.drop(1).map { it.measure(Constraints.fixed(iconSize, iconSize)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            fun Placeable.placeCentred(x: Float, y: Float) = place((x - width / 2f).roundToInt(), (y - height / 2f).roundToInt())

            val centreX = constraints.maxWidth / 2f
            val centreY = constraints.maxHeight / 2f
            emblem.placeCentred(centreX, centreY)
            icons.forEachIndexed { index, placeable ->
                val (dx, dy) = ringSlotOffset(index, icons.size)
                placeable.placeCentred(centreX + radius * dx, centreY + radius * dy)
            }
        }
    }
}

@Composable
private fun Emblem(showHint: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClickLabel = "Choose the apps on the home screen", onClick = onClick)
            .drawBehind {
                drawCircle(primary.copy(alpha = 0.12f))
                drawCircle(primary, radius = size.minDimension * 0.46f, style = Stroke(3.dp.toPx()))
                drawCircle(primary, radius = size.minDimension * 0.36f, style = Stroke(1.dp.toPx()))
            }
            .testTag(HomeRingTags.EMBLEM),
    ) {
        if (showHint) {
            Text("Add apps", style = MaterialTheme.typography.labelLarge, color = primary)
        } else {
            Text(
                text = "++",
                style = MaterialTheme.typography.displaySmall,
                color = primary,
                // Screen readers would otherwise say "plus plus".
                modifier = Modifier.clearAndSetSemantics { contentDescription = "Favourites" },
            )
        }
    }
}
