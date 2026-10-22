package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.round

object LauncherMenuTags {
    const val MENU = "launcher_menu"
    const val RESET_DIALOG = "launcher_reset_dialog"
    const val LOOK_DIALOG = "launcher_folder_look_dialog"
    const val SPIN_STATS_DIALOG = "launcher_spin_stats_dialog"
}

/**
 * The launcher's own long-press menu, opened at a spot in root coordinates (where a page's empty space was pressed) and
 * shown there. The empty space hosts its [content], handing it the host's origin in root coordinates.
 */
class LauncherMenu(val onOpen: (Offset) -> Unit, val content: @Composable (origin: () -> Offset) -> Unit)

/**
 * The empty space of a page. Laid behind the page's content, it only gets the touches nothing on the page claims, since
 * hit testing stops at the first sibling that claims the finger. A tap calls [onTap]; a long press opens [menu], if any, where
 * the finger is. It shows the menu wherever that was opened.
 */
@Composable
fun EmptySpace(menu: LauncherMenu?, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var placed by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val origin = { placed?.positionInRoot() ?: Offset.Zero }
    val currentOnTap by rememberUpdatedState(onTap)

    Box(
        modifier
            .fillMaxSize()
            .onPlaced { placed = it }
            .pointerInput(menu) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onLongPress = menu?.let { menu ->
                        { position ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menu.onOpen(origin() + position)
                        }
                    },
                )
            },
    ) {
        // Read at placement, not here: every page moves on every frame of a page swipe.
        menu?.content(origin)
    }
}

/**
 * The launcher's menu of [groups] of rows, shown [at] an offset in its host. Choosing a row dismisses the menu, then
 * hands on the choice.
 */
@Composable
fun LauncherOptionsMenu(expanded: Boolean, at: () -> Offset, groups: List<List<MenuRow>>, onDismiss: () -> Unit) {
    // A point-sized anchor at the spot, so the popup opens under the finger rather than under the host. Absolute, since
    // the spot is in root coordinates, which right-to-left layouts do not mirror.
    Box(Modifier.absoluteOffset { at().round() }) {
        GroundMenu(expanded = expanded, onDismissRequest = onDismiss, groups = groups, modifier = Modifier.testTag(LauncherMenuTags.MENU))
    }
}

/** Asks before [onReset] erases all the launcher keeps. Cancel, Back and a tap outside call [onDismiss]. */
@Composable
fun ResetDialog(onReset: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "Reset Orbit?",
        text = "This clears the ring, the dock, folders, collections, widgets and your settings, then restarts. Permissions stay.",
        verb = "Reset",
        onConfirm = onReset,
        onDismiss = onDismiss,
        destructive = true,
        modifier = Modifier.testTag(LauncherMenuTags.RESET_DIALOG),
    )
}

/** Asks whether to set [scene] as the home screen's wallpaper: Set calls [onSet]; Cancel, Back and a tap outside call [onDismiss]. */
@Composable
fun SceneDialog(scene: ThemeScene, onSet: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = "Set the ${scene.name} as wallpaper?",
        text = "It replaces the home screen's wallpaper.",
        verb = "Set",
        onConfirm = onSet,
        onDismiss = onDismiss,
    )
}
