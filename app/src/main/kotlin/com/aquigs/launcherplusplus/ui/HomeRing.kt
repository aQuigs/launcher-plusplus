package com.aquigs.launcherplusplus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.EMBLEM_FRACTION
import com.aquigs.launcherplusplus.domain.RING_RADIUS_FRACTION
import com.aquigs.launcherplusplus.domain.RingItem
import com.aquigs.launcherplusplus.domain.UnreadCounts
import com.aquigs.launcherplusplus.domain.ringIconSize
import com.aquigs.launcherplusplus.domain.ringSlotOffset
import kotlin.math.min
import kotlin.math.roundToInt

object HomeRingTags {
    const val EMBLEM = "ring_emblem"

    fun slot(app: AppEntry) = "ring_${app.key}"

    fun folder(index: Int) = "ring_folder_$index"
}

/** The size of a ring icon while the ring has room, and of an icon being dragged onto it. */
internal val RING_ICON_SIZE = 64.dp

/**
 * The [ring] of favourite apps and folders round a static emblem. Tap an app to launch it or long-press it for its
 * [menu]; tap a folder to open it or long-press it for its [folderMenu]; tap the emblem to choose the favourites on the
 * ring and in the dock. With [showHint] the emblem invites you to add apps instead of showing its mark. While
 * [highlighted], the disc the ring fills glows as the place an app being dragged would land. An [openFolder] takes the
 * ring over: its apps sit in the slots, each with the [folderAppMenu], and the emblem gives way to a target that calls
 * [onCloseFolder]. Each app wears its [unread] count, and a folder the sum of its apps'.
 */
@Composable
fun HomeRing(
    ring: List<RingItem>,
    showHint: Boolean,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onOpenFolder: (RingItem.Folder) -> Unit,
    onCloseFolder: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    openFolder: RingItem.Folder? = null,
    menu: AppMenu? = null,
    folderMenu: FolderMenu? = null,
    folderAppMenu: AppMenu? = null,
    unread: UnreadCounts = UnreadCounts(),
) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, label = "ring_glow")

    Layout(
        content = {
            if (openFolder != null) {
                CloseFolderTarget(onClick = onCloseFolder)
                openFolder.apps.forEach { app ->
                    key(app.key) {
                        val tag = Modifier.testTag(HomeRingTags.slot(app))
                        AppIcon(app, icon, onLaunch, tag, folderAppMenu, unread[app])
                    }
                }
            } else {
                Emblem(showHint = showHint, onClick = onEdit)
                ring.forEach { item ->
                    when (item) {
                        is RingItem.App -> key(item.app.key) {
                            val tag = Modifier.testTag(HomeRingTags.slot(item.app))
                            AppIcon(item.app, icon, onLaunch, tag, menu, unread[item.app])
                        }
                        is RingItem.Folder -> key(item.index) {
                            val tag = Modifier.testTag(HomeRingTags.folder(item.index))
                            FolderIcon(item, icon, onOpenFolder, tag, folderMenu, unread.sum(item.apps))
                        }
                    }
                }
            }
        },
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                if (glow > 0f) drawCircle(primary.copy(alpha = 0.1f * glow), radius = size.minDimension / 2)
                drawCircle(lerp(track, primary, glow), radius = size.minDimension * RING_RADIUS_FRACTION, style = Stroke(1.dp.toPx()))
            },
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val radius = side * RING_RADIUS_FRACTION
        val centreSize = (side * EMBLEM_FRACTION).roundToInt()
        val iconSize = ringIconSize(RING_ICON_SIZE.toPx(), side, measurables.size - 1).roundToInt()
        val centre = measurables.first().measure(Constraints.fixed(centreSize, centreSize))
        val icons = measurables.drop(1).map { it.measure(Constraints.fixed(iconSize, iconSize)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            fun Placeable.placeCentred(x: Float, y: Float) = place((x - width / 2f).roundToInt(), (y - height / 2f).roundToInt())

            val centreX = constraints.maxWidth / 2f
            val centreY = constraints.maxHeight / 2f
            centre.placeCentred(centreX, centreY)
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

/** The emblem's place while a folder is open: nothing to see, as in Arc, but a tap there closes the folder. */
@Composable
private fun CloseFolderTarget(onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Close folder" },
    )
}
