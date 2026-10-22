package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.ui.theme.Ground
import com.sqftware.orbitlauncher.ui.theme.Spacing
import kotlinx.coroutines.flow.first

// Every dialog and menu of the launcher is one of these, so each sits on the drawer's ground (held by the
// panel-ground-only hook), a theme reaches them all, and what is in them is on the pop-up ground. A dialog is one of the
// kinds below, so each kind ends the same way wherever it is used: one that shows or sets things as they change closes
// with Done, one that asks closes with its verb and Cancel, and one that offers choices closes with Cancel.

/** Material's alert dialog in the theme's box on the solid [PanelGround]. */
@Composable
private fun GroundDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    // The dialog is a window of its own, which takes its locals from here.
    OnGround(Ground.Popup) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            modifier = modifier.then(LocalThemeArt.current.dialogBox(panelGround())),
            dismissButton = dismissButton,
            icon = icon,
            title = title,
            text = text,
            containerColor = Color.Transparent,
        )
    }
}

/** What a menu row shows after its label, if anything. */
sealed interface RowEnd {
    /** The value the row's setting is set to; the row opens the choice of it. */
    class Value(val text: String) : RowEnd

    /** A switch showing [on]; the row flips it in place if it [flips], or goes where it is changed. */
    class Toggle(val on: Boolean, val flips: Boolean) : RowEnd

    /** A tick while the row is the one [chosen] of those beside it. */
    class Choice(val chosen: Boolean) : RowEnd
}

/**
 * One row of a menu: its [label], a leading [icon] (a menu of things to do gives every row one; a menu of settings
 * none), and what it shows at its [end]. A [destructive] row is in the error colour. A tap calls [onClick].
 */
class MenuRow(
    val label: String,
    val icon: (@Composable () -> Unit)? = null,
    val end: RowEnd? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** A row that flips the setting it shows [on], handing the new value to [onChange]. */
fun toggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) = MenuRow(label, end = RowEnd.Toggle(on, flips = true)) { onChange(!on) }

/** A menu row's icon drawn from [glyph]. */
fun glyph(glyph: ImageVector): @Composable () -> Unit = { Icon(glyph, contentDescription = null) }

/**
 * Material's dropdown menu in the theme's box on the solid [PanelGround], rimmed so it holds its edge over the launcher's own ground, with
 * its [groups] of rows divided from each other. Choosing a row dismisses the menu, then hands on the choice.
 */
@Composable
fun GroundMenu(expanded: Boolean, onDismissRequest: () -> Unit, groups: List<List<MenuRow>>, modifier: Modifier = Modifier) {
    val art = LocalThemeArt.current
    OnGround(Ground.Popup) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.then(art.menuBox(panelGround())),
            containerColor = Color.Transparent,
            border = art.menuRim,
        ) {
            art.MenuBody {
                groups.filter { it.isNotEmpty() }.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider()
                    group.forEach { MenuItem(it, expanded, onDismissRequest) }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(row: MenuRow, expanded: Boolean, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val end = row.end
    DropdownMenuItem(
        text = { Text(row.label) },
        leadingIcon = row.icon,
        trailingIcon = when (end) {
            null -> null
            is RowEnd.Value -> ({ Text(end.text, color = colors.onSurfaceVariant) })
            // The switch only shows the state: the row is the control.
            is RowEnd.Toggle -> ({ Switch(checked = end.on, onCheckedChange = null) })
            is RowEnd.Choice -> if (end.chosen) ({ PickedMark() }) else null
        },
        colors = if (row.destructive) MenuDefaults.itemColors(textColor = colors.error, leadingIconColor = colors.error) else MenuDefaults.itemColors(),
        onClick = { chooseFrom(expanded, onDismiss, row.onClick) },
        modifier = Modifier.semantics {
            when (end) {
                is RowEnd.Toggle -> if (end.flips) {
                    role = Role.Switch
                    toggleableState = ToggleableState(end.on)
                } else {
                    // A row whose tap goes elsewhere to change the state is a button with a state to screen readers,
                    // not a switch that would not flip.
                    role = Role.Button
                    stateDescription = if (end.on) "On" else "Off"
                }
                is RowEnd.Choice -> selected = end.chosen
                else -> role = Role.Button
            }
        },
    )
}

/** Dismisses a menu, then acts. Items stay tappable while the menu animates away, and a second tap must not start the same thing twice. */
private fun chooseFrom(expanded: Boolean, onDismiss: () -> Unit, choice: () -> Unit) {
    if (!expanded) return
    onDismiss()
    choice()
}

/** A dialog's one further action besides its own: its [label], in the error colour if [destructive], calling [onClick]. */
class DialogAction(val label: String, val destructive: Boolean = false, val modifier: Modifier = Modifier, val onClick: () -> Unit)

/**
 * A dialog that shows [content], or sets things in it as they change, so nothing waits for a confirm: Done, Back and a
 * tap outside call [onDone]. [aside] is one further action, such as removing what the dialog is about, before Done.
 */
@Composable
fun InfoDialog(
    title: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    aside: DialogAction? = null,
    content: @Composable () -> Unit,
) {
    GroundDialog(
        onDismissRequest = onDone,
        confirmButton = { LauncherButton(onClick = onDone) { Text("Done") } },
        dismissButton = aside?.let { action ->
            {
                LauncherButton(
                    onClick = action.onClick,
                    emphasis = if (action.destructive) Emphasis.Destructive else Emphasis.Normal,
                    modifier = action.modifier,
                ) { Text(action.label) }
            }
        },
        title = { Text(title) },
        text = content,
        modifier = modifier,
    )
}

/**
 * Asks before something is done, saying what in [text]: [verb] calls [onConfirm], in the error colour if it is
 * [destructive]; Cancel, Back and a tap outside call [onDismiss]. [icon] shows what it is about, if anything.
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    verb: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            LauncherButton(onClick = onConfirm, emphasis = if (destructive) Emphasis.Destructive else Emphasis.Normal) { Text(verb) }
        },
        dismissButton = { LauncherButton(onClick = onDismiss) { Text("Cancel") } },
        icon = icon,
        title = { Text(title) },
        text = { Text(text) },
        modifier = modifier,
    )
}

/**
 * Asks for a name, starting from [initial], up to [maxLength] long, in a field tagged [fieldTag]: [verb] and the
 * keyboard's Done hand what was typed to [onName], unless it is blank (and not [blankAllowed]) or [refusal] says why a
 * name that is not blank will not do; Cancel, Back and a tap outside call [onDismiss].
 */
@Composable
fun NameDialog(
    title: String,
    verb: String,
    maxLength: Int,
    onName: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    fieldTag: String = "",
    initial: String = "",
    blankAllowed: Boolean = false,
    refusal: (String) -> String? = { null },
) {
    var typed by rememberSaveable { mutableStateOf(initial) }
    val refused = if (typed.isBlank()) null else refusal(typed)
    val accepted = refused == null && (blankAllowed || typed.isNotBlank())
    val name = { if (accepted) onName(typed) }

    GroundDialog(
        onDismissRequest = onDismiss,
        confirmButton = { LauncherButton(onClick = name, enabled = accepted) { Text(verb) } },
        dismissButton = { LauncherButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it.take(maxLength) },
                label = { Text("Name") },
                singleLine = true,
                isError = refused != null,
                supportingText = refused?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { name() }),
                modifier = Modifier.testTag(fieldTag).focusRequester(rememberShownFocus()),
            )
        },
        modifier = modifier,
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
        dismissButton = { LauncherButton(onClick = onDismiss) { Text("Cancel") } },
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
                        if (chosen != null) RadioButton(selected = choice == chosen, onClick = null, Modifier.padding(end = Spacing.Edge))
                        Column(Modifier.padding(vertical = Spacing.Tight)) {
                            Text(label(choice))
                            description(choice)?.let { SupportingText(it) }
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}

/**
 * Focus for a text field in a pop-up, requested once the pop-up's own window has focus: a text field focused before then
 * gets no keyboard.
 */
@Composable
fun rememberShownFocus(): FocusRequester {
    val focus = remember { FocusRequester() }
    val window = LocalWindowInfo.current
    LaunchedEffect(window) {
        snapshotFlow { window.isWindowFocused }.first { it }
        focus.requestFocus()
    }
    return focus
}
