package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.ui.theme.RingSpark

object FolderTags {
    const val MENU = "folder_options"
}

/** A folder's long-press menu, opened and shown like an [AppMenu]. */
typealias FolderMenu = LongPressMenu<RingItem.Folder>

/** What a folder's long-press menu offers, in menu order. */
enum class FolderOption(val label: String, val icon: ImageVector) {
    AddApps("Add apps", Icons.Default.Add),
    Remove("Remove folder", Icons.Default.Close),
}

/** Material's folder glyph, which the core icon set leaves out. */
val FolderGlyph: ImageVector = materialIcon("Folder") {
    materialPath {
        moveTo(10f, 4f)
        horizontalLineTo(4f)
        curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
        lineTo(2f, 18f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(8f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineToRelative(-8f)
        lineToRelative(-2f, -2f)
        close()
    }
}

private const val PREVIEWS = 4
private const val PREVIEWS_PER_ROW = 2

/**
 * The square of the disc the previews fill, and the share of it each preview takes; the rest spaces them out. Four fill
 * the disc as in Arc, their outer corners just inside its edge.
 */
private const val PREVIEWS_FRACTION = 0.86f
private const val PREVIEW_FRACTION = 0.45f

/**
 * A slot on the ring or in the dock holding [folder]: a circle, the size of an app's, filled with previews of up to four of its icons and
 * wearing a badge for the [unread] notifications of all its apps. A tap opens the folder, unless it is empty, a long
 * press opens its [menu], and a long press that goes on becomes a [drag].
 */
@Composable
fun FolderIcon(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onOpen: (RingItem.Folder) -> Unit,
    modifier: Modifier = Modifier,
    menu: FolderMenu? = null,
    unread: Int = 0,
    drag: ItemDrag<RingItem.Folder>? = null,
) {
    val count = folder.apps.size
    val presses = remember { MutableInteractionSource() }
    val name = "Folder, $count ${if (count == 1) "app" else "apps"}"

    Box(
        modifier
            .combinedClickable(
                interactionSource = presses,
                indication = null,
                onLongClickLabel = menu?.let { "Folder options" },
                onLongClick = menu?.let { m -> { m.onOpen(folder) } },
                onClick = { if (count > 0) onOpen(folder) },
            )
            .itemDrag(folder, drag)
            .semantics { contentDescription = name.withUnread(unread) },
    ) {
        IconDisc(presses, Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { FolderPreviews(folder, icon) }
        menu?.content?.invoke(folder)
        UnreadBadge(unread, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * Up to four of [folder]'s icons in rows of two, spaced out in the middle: one alone, two side by side, three as a
 * triangle, four as a grid. Spaced by shares of the square rather than fixed gaps, so a shrunken ring's small discs fit
 * them too.
 */
@Composable
fun FolderPreviews(folder: RingItem.Folder, icon: suspend (AppEntry) -> ImageBitmap?) {
    FlowRow(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.SpaceAround,
        maxItemsInEachRow = PREVIEWS_PER_ROW,
        modifier = Modifier.fillMaxSize(PREVIEWS_FRACTION),
    ) {
        // Clipped round so a square or squircle icon mask keeps its corners inside the disc.
        folder.apps.take(PREVIEWS).forEach { AppImage(it, icon, Modifier.fillMaxSize(PREVIEW_FRACTION).clip(CircleShape)) }
    }
}

/**
 * What a slot on the ring or in the dock shows: [item] as an app, launched by a tap and wearing its [unread] count, or as
 * a folder, opened by a tap and wearing its apps' sum. A long press opens the app's [menu] or the folder's [folderMenu],
 * and one that goes on becomes a [drag].
 */
@Composable
internal fun SlotIcon(
    item: RingItem,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onOpenFolder: (RingItem.Folder) -> Unit,
    modifier: Modifier,
    menu: AppMenu?,
    folderMenu: FolderMenu?,
    unread: UnreadCounts,
    drag: ItemDrag<Any?>?,
) {
    when (item) {
        is RingItem.App -> AppIcon(item.app, icon, onLaunch, modifier, menu, unread[item.app], drag)
        is RingItem.Folder -> FolderIcon(item, icon, onOpenFolder, modifier, folderMenu, unread.sum(item.apps), drag)
    }
}

/** An item lit as the one an app let go now would fold into: a little larger, ringed with the spark's colour. */
internal fun Modifier.foldTarget(lit: Boolean): Modifier = if (!lit) {
    this
} else {
    this
        .semantics { stateDescription = "Drop to put in a folder" }
        .graphicsLayer {
            scaleX = FOLD_TARGET_SCALE
            scaleY = FOLD_TARGET_SCALE
        }
        .drawBehind { drawCircle(RingSpark, radius = size.minDimension / 2 + 3.dp.toPx(), style = Stroke(2.dp.toPx())) }
}

private const val FOLD_TARGET_SCALE = 1.12f

/** A folder's long-press menu. Choosing an item dismisses the menu, then hands on the choice. */
@Composable
fun FolderOptionsMenu(expanded: Boolean, onOption: (FolderOption) -> Unit, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(FolderTags.MENU)) {
        FolderOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                leadingIcon = { Icon(option.icon, contentDescription = null) },
                onClick = { chooseFrom(expanded, onDismiss) { onOption(option) } },
            )
        }
    }
}
