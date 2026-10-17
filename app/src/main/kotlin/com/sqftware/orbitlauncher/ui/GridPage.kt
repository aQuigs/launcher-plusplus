package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.GridItem
import com.sqftware.orbitlauncher.domain.UnreadCounts

object GridTags {
    const val FOLDER_DIALOG = "grid_folder_dialog"

    fun block(id: Int) = "grid_block_$id"

    fun folderApp(app: AppEntry) = "grid_folder_app_${app.key}"
}

/** A folder's long-press menu on a grid page, opened and shown like an [AppMenu]. */
typealias GridFolderMenu = LongPressMenu<GridItem.Folder>

/**
 * An app or a folder in a cell of a grid page, its name under it: a tap launches the app or opens the folder ([onOpen]),
 * a long press opens its [menu] or [folderMenu], and one that goes on becomes a [drag]. It shows faint while [lifted]
 * off the cell, and lit while it is [foldTarget], where an app let go now would join it.
 */
@Composable
fun GridBlock(
    block: Int,
    item: GridItem,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onOpen: (GridItem.Folder) -> Unit,
    menu: AppMenu?,
    folderMenu: GridFolderMenu?,
    unread: UnreadCounts,
    onClearBadge: (AppEntry) -> Unit,
    drag: ItemDrag<Any?>?,
    lifted: Boolean,
    foldTarget: Boolean,
    modifier: Modifier = Modifier,
) {
    val lit = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.alpha(if (lifted) LANDING_ALPHA else 1f).testTag(GridTags.block(block)),
    ) {
        val iconModifier = Modifier.size(GRID_ICON_SIZE).foldTarget(foldTarget, lit)
        when (item) {
            is GridItem.App -> {
                AppIcon(item.app, icon, onLaunch, iconModifier, menu, unread.badge(item.app), drag, onClearBadge)
                AppLabel(item.app.label, Modifier.fillMaxWidth())
            }
            is GridItem.Folder -> {
                GridFolderIcon(item, icon, onOpen, iconModifier, folderMenu, unread, drag)
                AppLabel(folderTitle(item.name), Modifier.fillMaxWidth())
            }
        }
    }
}

/** A folder as a disc of up to four of its icons, wearing its apps' unread sum. */
@Composable
private fun GridFolderIcon(
    folder: GridItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onOpen: (GridItem.Folder) -> Unit,
    modifier: Modifier,
    menu: GridFolderMenu?,
    unread: UnreadCounts,
    drag: ItemDrag<Any?>?,
) {
    val presses = remember { MutableInteractionSource() }
    val badge = unread.badge(folder.apps)
    val name = folderDescription(folder.name, folder.apps.size)
    Box(
        modifier
            .combinedClickable(
                interactionSource = presses,
                indication = null,
                onLongClickLabel = menu?.let { "Folder options" },
                onLongClick = menu?.let { m -> { m.onOpen(folder) } } ?: {},
                onClick = { onOpen(folder) },
            )
            .itemDrag(folder, drag)
            .semantics { contentDescription = name.withUnread(badge.count) },
    ) {
        IconDisc(presses, Modifier.size(GRID_ICON_SIZE), contentAlignment = Alignment.Center) { FolderPreviews(folder.apps, icon) }
        menu?.content?.invoke(folder)
        UnreadBadge(badge, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * The open [folder] of a grid page, its apps in rows under its name, each launched by a tap, which closes the folder, and
 * opening the [menu] for it at a long press. Back and a tap outside call [onDismiss].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GridFolderDialog(
    folder: GridItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    menu: AppMenu?,
    unread: UnreadCounts,
    onClearBadge: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(folderTitle(folder.name)) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                folder.apps.forEach { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).testTag(GridTags.folderApp(app))) {
                        AppIcon(
                            app,
                            icon,
                            onLaunch = {
                                onDismiss()
                                onLaunch(it)
                            },
                            modifier = Modifier.size(GRID_ICON_SIZE),
                            menu = menu,
                            unread = unread.badge(app),
                            onClearBadge = onClearBadge,
                        )
                        AppLabel(app.label, Modifier.fillMaxWidth())
                    }
                }
            }
        },
        modifier = Modifier.testTag(GridTags.FOLDER_DIALOG),
    )
}
