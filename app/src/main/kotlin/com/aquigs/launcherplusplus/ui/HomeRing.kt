package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.key
import com.aquigs.launcherplusplus.domain.ringIconScale
import com.aquigs.launcherplusplus.domain.ringSlotAngle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object HomeRingTags {
    const val RING = "home_ring"
    const val EMBLEM = "ring_emblem"

    fun slot(app: AppEntry) = "ring_${app.key}"
}

private val FULL_ICON_SIZE = 64.dp
private const val RING_RADIUS_FRACTION = 0.36f
private const val EMBLEM_FRACTION = 0.42f

/**
 * The installed [favourites] on a ring round a static emblem. Tap an icon to launch it; tap the emblem to choose the
 * favourites. The empty-ring hint follows what is stored, not what is installed, so it does not flash while [apps] load.
 */
@Composable
fun HomeRing(
    favourites: Favourites,
    apps: List<AppEntry>,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val ring = remember(favourites, apps) { favourites.resolve(apps) }

    Layout(
        content = {
            Emblem(empty = favourites.keys.isEmpty(), onClick = onEdit)
            ring.forEach { app -> key(app.key) { RingIcon(app, icon, onLaunch) } }
        },
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawCircle(track, radius = size.minDimension * RING_RADIUS_FRACTION, style = Stroke(1.dp.toPx())) }
            .testTag(HomeRingTags.RING),
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight)
        val radius = side * RING_RADIUS_FRACTION
        val emblemSize = (side * EMBLEM_FRACTION).roundToInt()
        val iconSize = (FULL_ICON_SIZE.toPx() * ringIconScale(ring.size)).roundToInt()
        val emblem = measurables.first().measure(Constraints.fixed(emblemSize, emblemSize))
        val icons = measurables.drop(1).map { it.measure(Constraints.fixed(iconSize, iconSize)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            val centreX = constraints.maxWidth / 2f
            val centreY = constraints.maxHeight / 2f
            emblem.place((centreX - emblemSize / 2f).roundToInt(), (centreY - emblemSize / 2f).roundToInt())
            icons.forEachIndexed { index, placeable ->
                val angle = ringSlotAngle(index, icons.size)
                placeable.place(
                    (centreX + radius * sin(angle) - iconSize / 2f).roundToInt(),
                    (centreY - radius * cos(angle) - iconSize / 2f).roundToInt(),
                )
            }
        }
    }
}

@Composable
private fun Emblem(empty: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClickLabel = "Choose the apps on the ring", onClick = onClick)
            .drawBehind {
                drawCircle(primary.copy(alpha = 0.12f))
                drawCircle(primary, radius = size.minDimension * 0.46f, style = Stroke(3.dp.toPx()))
                drawCircle(primary, radius = size.minDimension * 0.36f, style = Stroke(1.dp.toPx()))
            }
            .testTag(HomeRingTags.EMBLEM),
    ) {
        if (empty) {
            Text("Add apps", style = MaterialTheme.typography.labelLarge, color = primary)
        } else {
            Text("++", style = MaterialTheme.typography.displaySmall, color = primary)
        }
    }
}

@Composable
private fun RingIcon(app: AppEntry, icon: suspend (AppEntry) -> ImageBitmap?, onLaunch: (AppEntry) -> Unit) {
    val bitmap by produceState<ImageBitmap?>(null, app.key) { value = icon(app) }

    Box(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onLaunch(app) }
            .semantics { contentDescription = app.label }
            .testTag(HomeRingTags.slot(app)),
    ) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
