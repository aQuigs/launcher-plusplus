package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.dockRow
import kotlin.math.roundToInt

object DockTags {
    const val DOCK = "dock"

    fun slot(app: AppEntry) = "dock_${app.key}"
}

private val FULL_DOCK_ICON_SIZE = 56.dp

/**
 * The user's dock [apps] in one row of equal slots, together in the middle of the width. The row is as tall as a
 * full-size icon, so a crowded dock shrinks its icons but not the row, and an empty dock still holds its place. While
 * [highlighted], the row glows as the place an app being dragged would land. Each app wears its [unread] count. With
 * [rearrange], a long press that moves on picks an app up to move it along the row; while one is on the move, the row
 * shows where everything would be if it were dropped.
 */
@Composable
fun Dock(
    apps: List<AppEntry>,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    menu: AppMenu? = null,
    unread: UnreadCounts = UnreadCounts(),
    rearrange: Rearrange? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, label = "dock_glow")

    Layout(
        content = {
            val moving = rearrange?.moving
            moving.shown(apps).forEachIndexed { index, app ->
                key(app.key) {
                    val slot = Modifier.testTag(DockTags.slot(app)).reorderSlot(rearrange, index, moving?.at == index)
                    AppIcon(app, icon, onLaunch, slot, menu, unread[app], rearrange?.drag(index))
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { if (glow > 0f) drawRoundRect(primary.copy(alpha = 0.12f * glow), cornerRadius = CornerRadius(size.height / 2)) }
            .padding(vertical = 12.dp)
            .testTag(DockTags.DOCK),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = FULL_DOCK_ICON_SIZE.roundToPx()
        if (measurables.isEmpty()) return@Layout layout(width, height) {}

        val row = dockRow(FULL_DOCK_ICON_SIZE.toPx(), width.toFloat(), measurables.size)
        val size = row.iconSize.roundToInt()
        val icons = measurables.map { it.measure(Constraints.fixed(size, size)) }

        layout(width, height) {
            icons.forEachIndexed { index, placeable -> placeable.placeRelative(row.starts[index].roundToInt(), (height - size) / 2) }
        }
    }
}
