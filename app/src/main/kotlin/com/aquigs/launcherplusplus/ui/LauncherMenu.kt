package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.round

object LauncherMenuTags {
    const val MENU = "launcher_menu"
}

/** The launcher's own long-press menu, opened at the spot pressed on a page's empty space and shown there. */
typealias LauncherMenu = LongPressMenu<Offset>

/** One row of the launcher's menu: [label] beside a switch showing [on]. The row is the target; a tap calls [onClick]. */
class LauncherMenuRow(val label: String, val on: Boolean, val onClick: () -> Unit)

/**
 * The empty space of a page. Laid behind the page's content, it only gets the touches nothing on the page claims, since
 * hit testing stops at the first sibling under the finger. A long press opens [menu] where the finger is.
 */
@Composable
fun EmptySpace(menu: LauncherMenu?, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var pressedAt by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier.fillMaxSize().then(
            if (menu == null) {
                Modifier
            } else {
                Modifier.pointerInput(menu) {
                    detectTapGestures(
                        onLongPress = { position ->
                            pressedAt = position
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menu.onOpen(position)
                        },
                    )
                }
            },
        ),
    ) {
        // A point-sized anchor at the spot pressed, so the popup opens under the finger rather than under the page.
        Box(Modifier.offset { pressedAt.round() }) { menu?.content?.invoke(pressedAt) }
    }
}

/** The launcher's menu of [rows]. Choosing a row dismisses the menu, then hands on the choice. */
@Composable
fun LauncherOptionsMenu(expanded: Boolean, rows: List<LauncherMenuRow>, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(LauncherMenuTags.MENU)) {
        rows.forEach { row ->
            DropdownMenuItem(
                text = { Text(row.label) },
                // The switch only shows the state: the row is the control, and says so to screen readers.
                trailingIcon = { Switch(checked = row.on, onCheckedChange = null) },
                onClick = { chooseFrom(expanded, onDismiss, row.onClick) },
                modifier = Modifier.semantics {
                    role = Role.Switch
                    toggleableState = ToggleableState(row.on)
                },
            )
        }
    }
}
