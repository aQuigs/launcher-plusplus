package com.aquigs.launcherplusplus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.EMBLEM_FRACTION
import com.aquigs.launcherplusplus.domain.RingItem
import com.aquigs.launcherplusplus.domain.UnreadCounts
import com.aquigs.launcherplusplus.domain.ringLayout
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

/** How far ring icons keep inside the ring's box: room for the unread badge's overhang, and a little air besides. */
internal val RING_EDGE_MARGIN = BADGE_OVERHANG + 4.dp

/** The ring's lines and the emblem's mark: neutral, so they sit on any wallpaper without a hue of their own. */
private val Mark = Color.White

/** The emblem's "++" or its hint: the mark at its most present. */
private val Ink = Mark.copy(alpha = 0.9f)

private const val EMBLEM_TICKS = 60
private const val EMBLEM_MAJOR_TICK_EVERY = 5

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
    val slots = openFolder?.apps?.size ?: ring.size
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, label = "ring_glow")
    // The one layout both the drawn track and the icons follow.
    fun Density.layoutOn(side: Float) = ringLayout(RING_ICON_SIZE.toPx(), side, slots, RING_EDGE_MARGIN.toPx())

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
            // Cached, so the glow animating does not lay the ring out again every frame.
            .drawWithCache {
                val radius = layoutOn(size.minDimension).radius
                val track = Stroke(1.dp.toPx())
                onDrawBehind {
                    if (glow > 0f) drawCircle(Mark.copy(alpha = 0.08f * glow), radius = size.minDimension / 2)
                    drawCircle(Mark.copy(alpha = 0.22f + 0.48f * glow), radius = radius, style = track)
                }
            },
    ) { measurables, constraints ->
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val (radius, iconSize) = layoutOn(side)
        val centreSize = (side * EMBLEM_FRACTION).roundToInt()
        val centre = measurables.first().measure(Constraints.fixed(centreSize, centreSize))
        val iconPx = iconSize.roundToInt()
        val icons = measurables.drop(1).map { it.measure(Constraints.fixed(iconPx, iconPx)) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            fun Placeable.placeCentred(x: Float, y: Float) = place((x - width / 2f).roundToInt(), (y - height / 2f).roundToInt())

            val centreX = constraints.maxWidth / 2f
            val centreY = constraints.maxHeight / 2f
            centre.placeCentred(centreX, centreY)
            icons.forEachIndexed { index, placeable ->
                val (dx, dy) = ringSlotOffset(index, slots)
                placeable.placeCentred(centreX + radius * dx, centreY + radius * dy)
            }
        }
    }
}

/**
 * The ring's centre: a translucent dark disc, so it holds on bright wallpapers, edged by a hairline and a bezel of fine
 * ticks round a crisp "++", or the hint to add apps in its place. Quiet, so the icons stay the eye's first stop.
 */
@Composable
private fun Emblem(showHint: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClickLabel = "Choose the apps on the home screen", onClick = onClick)
            .drawBehind {
                val hairline = 1.dp.toPx()
                val outer = size.minDimension / 2 * 0.96f
                drawCircle(Color.Black.copy(alpha = 0.4f), radius = outer)
                drawCircle(Mark.copy(alpha = 0.35f), radius = outer, style = Stroke(hairline))
                repeat(EMBLEM_TICKS) { tick ->
                    val major = tick % EMBLEM_MAJOR_TICK_EVERY == 0
                    rotate(360f * tick / EMBLEM_TICKS) {
                        drawLine(
                            color = Mark.copy(alpha = if (major) 0.55f else 0.22f),
                            start = center.copy(y = center.y - outer * if (major) 0.8f else 0.84f),
                            end = center.copy(y = center.y - outer * 0.89f),
                            strokeWidth = hairline,
                        )
                    }
                }
                drawCircle(Mark.copy(alpha = 0.14f), radius = outer * 0.72f, style = Stroke(hairline))
                if (!showHint) drawPlusPlus(outer)
            }
            .testTag(HomeRingTags.EMBLEM)
            .semantics { if (!showHint) contentDescription = "Favourites" },
    ) {
        if (showHint) {
            Text(
                text = "Add apps",
                // Shadowed so it still reads where a light wallpaper shows through the disc.
                style = MaterialTheme.typography.labelLarge.copy(shadow = Shadow(Color.Black, blurRadius = 6f)),
                color = Ink,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                // Inside the inner hairline, clear of the ticks, however large the font.
                modifier = Modifier.fillMaxWidth(0.55f),
            )
        }
    }
}

/** The "++" drawn as strokes rather than text, so it stays sharp and truly centred whatever the font. */
private fun DrawScope.drawPlusPlus(outer: Float) {
    val arm = outer * 0.11f
    val stroke = outer * 0.035f
    listOf(-1, 1).forEach { side ->
        val middle = center.copy(x = center.x + side * outer * 0.15f)
        drawLine(Ink, middle.copy(x = middle.x - arm), middle.copy(x = middle.x + arm), stroke)
        drawLine(Ink, middle.copy(y = middle.y - arm), middle.copy(y = middle.y + arm), stroke)
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
