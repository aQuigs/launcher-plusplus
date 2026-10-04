package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

// Every dialog and menu of the launcher is one of these, so each sits on the drawer's ground (held by the
// panel-ground-only hook) and a theme reaches them all.

/** Material's alert dialog on the solid [PanelGround]. */
@Composable
fun GroundDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.solidGround(panelGround(), AlertDialogDefaults.shape),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        containerColor = Color.Transparent,
    )
}

/** Material's dropdown menu on the solid [PanelGround], rimmed so it holds its edge over the launcher's own ground. */
@Composable
fun GroundMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.solidGround(panelGround(), MenuDefaults.shape),
        containerColor = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

/**
 * A text button for a pop-up's own solid ground; its bare text would vanish on the wallpaper, where [TonalButton] goes.
 * A [destructive] one, which erases what the user keeps, is in the error colour.
 */
@Composable
fun PopupButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = if (destructive) {
        ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
    } else {
        ButtonDefaults.textButtonColors()
    }
    TextButton(onClick, modifier, enabled, colors = colors, content = content)
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
