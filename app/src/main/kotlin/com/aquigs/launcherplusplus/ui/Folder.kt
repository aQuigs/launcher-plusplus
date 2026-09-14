package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.RingItem

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

private const val PREVIEW_SIDE = 2

/**
 * A ring slot holding [folder]: a circle, the size of an app's, previewing up to four of its icons in a small grid and
 * wearing a badge for the [unread] notifications of all its apps. A tap opens the folder, unless it is empty, and a long
 * press opens its [menu].
 */
@Composable
fun FolderIcon(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onOpen: (RingItem.Folder) -> Unit,
    modifier: Modifier = Modifier,
    menu: FolderMenu? = null,
    unread: Int = 0,
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
            .semantics { contentDescription = name.withUnread(unread) },
    ) {
        IconDisc(presses, Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize(0.62f)) {
                repeat(PREVIEW_SIDE) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        repeat(PREVIEW_SIDE) { column ->
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                folder.apps.getOrNull(row * PREVIEW_SIDE + column)?.let { AppImage(it, icon, Modifier.fillMaxSize()) }
                            }
                        }
                    }
                }
            }
        }
        menu?.content?.invoke(folder)
        UnreadBadge(unread, Modifier.align(Alignment.TopEnd))
    }
}

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
