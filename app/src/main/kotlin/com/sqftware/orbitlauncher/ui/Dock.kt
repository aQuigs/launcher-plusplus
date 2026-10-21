package com.sqftware.orbitlauncher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.DockRow
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.dockRow
import com.sqftware.orbitlauncher.ui.theme.LocalRingColors
import kotlin.math.roundToInt

object DockTags {
    const val DOCK = "dock"

    fun slot(app: AppEntry) = "dock_${app.key}"

    fun folder(index: Int) = "dock_folder_$index"
}

/**
 * The user's dock [items], apps and folders, in one row of equal slots, together in the middle of the width. The row is
 * as tall as a full-size icon, so a crowded dock shrinks its icons but not the row, and an empty dock still holds its
 * place. While [highlighted], the row glows as the place an app being dragged would land. Tap an app to launch it or
 * long-press it for its [menu]; tap a folder to open it or long-press it for its [folderMenu]. Each app wears its
 * [unread] count, and a folder the sum of its apps'. With [rearrange], a long press that moves on picks an item up to
 * move it along the row; while one is on the move, the row shows where everything would be if it were dropped. The item
 * at [foldTarget] is lit as the one an app let go now would fold into, and one arriving from another place shows where
 * it would land among the others.
 */
@Composable
fun Dock(
    items: List<RingItem>,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onOpenFolder: (RingItem.Folder) -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    menu: AppMenu? = null,
    folderMenu: FolderMenu? = null,
    unread: UnreadCounts = UnreadCounts(),
    onClearBadge: ((AppEntry) -> Unit)? = null,
    rearrange: Rearrange? = null,
    foldTarget: Int? = null,
) {
    val glow by animateFloatAsState(if (highlighted) 1f else 0f, label = "dock_glow")
    val marks = LocalRingColors.current
    val art = LocalThemeArt.current
    val fullSize = art.dockIconSize
    val ground = art.dockGround(MaterialTheme.colorScheme.primary)
    // Written by measure and read by draw, so a new layout of the row redraws its ground.
    var laidOut by remember { mutableStateOf<DockRow?>(null) }

    Layout(
        content = {
            val landsAt = rearrange?.arriving?.to ?: rearrange?.moving?.at
            (rearrange?.shown(items) ?: items).forEachIndexed { index, item ->
                val tag = when (item) {
                    is RingItem.App -> DockTags.slot(item.app)
                    is RingItem.Folder -> DockTags.folder(item.at.index)
                }
                key(tag) {
                    val slot = Modifier.testTag(tag).reorderSlot(rearrange, index, landsAt == index).foldTarget(index == foldTarget, marks.lit)
                    SlotIcon(item, icon, onLaunch, onOpenFolder, slot, menu, folderMenu, unread, rearrange?.drag(index), onClearBadge)
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .drawBehind { laidOut?.let { ground(DockSlots(it.iconSize, it.starts, glow)) } }
            .padding(vertical = 12.dp)
            .testTag(DockTags.DOCK),
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = fullSize.roundToPx()
        if (measurables.isEmpty()) {
            laidOut = DockRow(fullSize.toPx(), emptyList())
            return@Layout layout(width, height) {}
        }

        val row = dockRow(fullSize.toPx(), width.toFloat(), measurables.size, art.dockIconFill)
        laidOut = row
        val size = row.iconSize.roundToInt()
        val icons = measurables.map { it.measure(Constraints.fixed(size, size)) }

        layout(width, height) {
            icons.forEachIndexed { index, placeable -> placeable.placeRelative(row.starts[index].roundToInt(), (height - size) / 2) }
        }
    }
}
