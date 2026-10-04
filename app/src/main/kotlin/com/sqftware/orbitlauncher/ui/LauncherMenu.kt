package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round

object LauncherMenuTags {
    const val MENU = "launcher_menu"
    const val RESET_DIALOG = "launcher_reset_dialog"
    const val LOOK_DIALOG = "launcher_folder_look_dialog"
}

/**
 * The launcher's own long-press menu, opened at a spot in root coordinates (where a page's empty space was pressed, or the
 * ring's emblem) and shown there. The empty space hosts its [content], handing it the host's origin in root coordinates.
 */
class LauncherMenu(val onOpen: (Offset) -> Unit, val content: @Composable (origin: () -> Offset) -> Unit)

/**
 * One row of the launcher's menu: [label] beside a switch showing [on], or beside the [value] it is set to, or alone for an
 * action. The row is the target; a tap calls [onClick], which flips [on] in place if [flips].
 */
class LauncherMenuRow(
    val label: String,
    val on: Boolean? = null,
    val flips: Boolean = false,
    val value: String? = null,
    val onClick: () -> Unit,
)

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

/** The launcher's menu of [rows], shown [at] an offset in its host. Choosing a row dismisses the menu, then hands on the choice. */
@Composable
fun LauncherOptionsMenu(expanded: Boolean, at: () -> Offset, rows: List<LauncherMenuRow>, onDismiss: () -> Unit) {
    // A point-sized anchor at the spot, so the popup opens under the finger rather than under the host. Absolute, since
    // the spot is in root coordinates, which right-to-left layouts do not mirror.
    Box(Modifier.absoluteOffset { at().round() }) {
        GroundMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(LauncherMenuTags.MENU)) {
            rows.forEach { row -> LauncherMenuItem(row, expanded, onDismiss) }
        }
    }
}

/** A [row] of a menu shown while [expanded]. Choosing it dismisses the menu ([onDismiss]), then hands on the choice. */
@Composable
fun LauncherMenuItem(row: LauncherMenuRow, expanded: Boolean, onDismiss: () -> Unit) {
    DropdownMenuItem(
        text = { Text(row.label) },
        // The switch only shows the state: the row is the control. A row whose tap goes elsewhere to change the
        // state is a button with a state to screen readers, not a switch that would not flip.
        trailingIcon = row.on?.let { on -> { Switch(checked = on, onCheckedChange = null) } }
            ?: row.value?.let { value -> { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
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

/** Asks before [onReset] erases all the launcher keeps. Cancel, Back and a tap outside call [onDismiss]. */
@Composable
fun ResetDialog(onReset: () -> Unit, onDismiss: () -> Unit) {
    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = { PopupButton(onClick = onReset, destructive = true) { Text("Reset") } },
        dismissButton = { PopupButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Reset Orbit?") },
        text = {
            Text("This clears the ring, the dock, folders, collections, widgets and your settings, then restarts. Permissions stay.")
        },
        modifier = Modifier.testTag(LauncherMenuTags.RESET_DIALOG),
    )
}

/** Asks whether to set [scene] as the home screen's wallpaper: Set calls [onSet]; Cancel, Back and a tap outside call [onDismiss]. */
@Composable
fun SceneDialog(scene: ThemeScene, onSet: () -> Unit, onDismiss: () -> Unit) {
    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = { PopupButton(onClick = onSet) { Text("Set") } },
        dismissButton = { PopupButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Set the ${scene.name} as wallpaper?") },
        text = { Text("It replaces the home screen's wallpaper.") },
    )
}

/**
 * Asks which of [choices] to have, each named by its [label] over its [description], if any, with [chosen] marked, if any.
 * A tap on one hands it to [onChoose]; Cancel, Back and a tap outside call [onDismiss].
 */
@Composable
fun <T> ChoiceDialog(
    title: String,
    choices: List<T>,
    chosen: T?,
    label: (T) -> String,
    onChoose: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    description: (T) -> String? = { null },
) {
    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { PopupButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                choices.forEach { choice ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = choice == chosen, role = Role.RadioButton) { onChoose(choice) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (chosen != null) RadioButton(selected = choice == chosen, onClick = null, Modifier.padding(end = 16.dp))
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(label(choice))
                            description(choice)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}
