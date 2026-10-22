package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.GridItem
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.ui.theme.Spacing

/** How wide an app in an open grid folder is, its name under it. */
private val FOLDER_APP_WIDTH = 72.dp

object GridTags {
    const val FOLDER_DIALOG = "grid_folder_dialog"

    fun block(id: Int) = "grid_block_$id"

    fun folderApp(app: AppEntry) = "grid_folder_app_${app.key}"
}

/**
 * An app or a folder in a cell of a grid page, its name under it: a tap launches the app or opens the folder ([onOpen]),
 * a long press opens its [menu] or [folderMenu], and one that goes on becomes a [drag]. It shows faint while [lifted]
 * off the cell, and lit while it is [foldTarget], where an app let go now would join it.
 */
@Composable
fun GridBlock(
    block: Int,
    item: GridItem,
    icon: IconLoader,
    onLaunch: (AppEntry) -> Unit,
    onOpen: (GridItem.Folder) -> Unit,
    menu: AppMenu?,
    folderMenu: FolderMenu?,
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
                // Placed by its block, an index no ring slot has, so a look that tells folders apart by place, as the
                // solar system does, tells this one from the ring's.
                val folder = remember(item, block) { RingItem.Folder(HomePlace.Folder(HomePlace.Ring, block), item.apps, name = item.name) }
                FolderIcon(folder, icon, { onOpen(item) }, iconModifier, folderMenu, unread.badge(item.apps), drag)
                AppLabel(folderTitle(item.name), Modifier.fillMaxWidth())
            }
        }
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
    icon: IconLoader,
    onLaunch: (AppEntry) -> Unit,
    menu: AppMenu?,
    unread: UnreadCounts,
    onClearBadge: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    InfoDialog(title = folderTitle(folder.name), onDone = onDismiss, modifier = Modifier.testTag(GridTags.FOLDER_DIALOG)) {
        BoxWithConstraints {
            // As many columns as fit, filled from the start of each row as a launcher's folder is.
            val columns = (maxWidth / FOLDER_APP_WIDTH).toInt().coerceAtLeast(1)
            val width = maxWidth / columns
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(Spacing.Gap),
                maxItemsInEachRow = columns,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                folder.apps.forEach { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(width).testTag(GridTags.folderApp(app))) {
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
        }
    }
}
