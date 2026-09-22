package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.round

object LauncherMenuTags {
    const val MENU = "launcher_menu"
    const val RESET_DIALOG = "launcher_reset_dialog"
}

/** The launcher's own long-press menu, opened at the spot pressed on a page's empty space and shown there. */
typealias LauncherMenu = LongPressMenu<Offset>

/**
 * One row of the launcher's menu: [label] beside a switch showing [on], or alone for an action, when [on] is null. The row
 * is the target; a tap calls [onClick], which flips [on] in place if [flips].
 */
class LauncherMenuRow(val label: String, val on: Boolean? = null, val flips: Boolean = false, val onClick: () -> Unit)

/**
 * The empty space of a page. Laid behind the page's content, it only gets the touches nothing on the page claims, since
 * hit testing stops at the first sibling that claims the finger. A long press opens [menu] where the finger is.
 */
@Composable
fun EmptySpace(menu: LauncherMenu, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    var pressedAt by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier.fillMaxSize().pointerInput(menu) {
            detectTapGestures(
                onLongPress = { position ->
                    pressedAt = position
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu.onOpen(position)
                },
            )
        },
    ) {
        // A point-sized anchor at the spot pressed, so the popup opens under the finger rather than under the page.
        Box(Modifier.offset { pressedAt.round() }) { menu.content(pressedAt) }
    }
}

/** The launcher's menu of [rows]. Choosing a row dismisses the menu, then hands on the choice. */
@Composable
fun LauncherOptionsMenu(expanded: Boolean, rows: List<LauncherMenuRow>, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(LauncherMenuTags.MENU)) {
        rows.forEach { row ->
            DropdownMenuItem(
                text = { Text(row.label) },
                // The switch only shows the state: the row is the control. A row whose tap goes elsewhere to change the
                // state is a button with a state to screen readers, not a switch that would not flip.
                trailingIcon = row.on?.let { on -> { Switch(checked = on, onCheckedChange = null) } },
                onClick = { chooseFrom(expanded, onDismiss, row.onClick) },
                modifier = Modifier.semantics {
                    val on = row.on
                    when {
                        on == null -> role = Role.Button
                        row.flips -> {
                            role = Role.Switch
                            toggleableState = ToggleableState(on)
                        }
                        else -> {
                            role = Role.Button
                            stateDescription = if (on) "On" else "Off"
                        }
                    }
                },
            )
        }
    }
}

/** Asks before [onReset] erases all the launcher keeps. Cancel, Back and a tap outside call [onDismiss]. */
@Composable
fun ResetDialog(onReset: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onReset) { Text("Reset") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Reset Launcher++?") },
        text = {
            Text("This clears the ring, the dock, folders, collections, widgets and the clock choice, then restarts. Permissions stay.")
        },
        modifier = Modifier.testTag(LauncherMenuTags.RESET_DIALOG),
    )
}
