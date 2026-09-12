package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppOption
import com.aquigs.launcherplusplus.domain.AppShortcut
import com.aquigs.launcherplusplus.domain.HomePlace

object AppOptionsTags {
    const val MENU = "app_options"
}

/**
 * An app's long-press menu: its [shortcuts] first, then the [options] for where it was pressed. It keeps what it shows
 * while [expanded] turns false, so it animates away whole.
 */
@Composable
fun AppOptionsMenu(
    expanded: Boolean,
    shortcuts: List<AppShortcut>,
    options: List<AppOption>,
    shortcutIcon: suspend (AppShortcut) -> ImageBitmap?,
    onShortcut: (AppShortcut) -> Unit,
    onOption: (AppOption) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(AppOptionsTags.MENU)) {
        shortcuts.forEach { shortcut ->
            DropdownMenuItem(
                text = { Text(shortcut.label) },
                leadingIcon = { ShortcutIcon(shortcut, shortcutIcon) },
                onClick = { onShortcut(shortcut) },
            )
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider()
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                leadingIcon = { Icon(option.icon, contentDescription = null) },
                onClick = { onOption(option) },
            )
        }
    }
}

@Composable
private fun ShortcutIcon(shortcut: AppShortcut, icon: suspend (AppShortcut) -> ImageBitmap?) {
    val bitmap by produceState<ImageBitmap?>(null, shortcut) { value = icon(shortcut) }

    Box(Modifier.size(24.dp)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

private val AppOption.label
    get() = when (this) {
        is AppOption.Remove -> when (place) {
            HomePlace.Ring -> "Remove from the ring"
            HomePlace.Dock -> "Remove from the dock"
        }
        AppOption.AppInfo -> "App info"
        AppOption.Uninstall -> "Uninstall"
    }

private val AppOption.icon
    get() = when (this) {
        is AppOption.Remove -> Icons.Default.Close
        AppOption.AppInfo -> Icons.Default.Info
        AppOption.Uninstall -> Icons.Default.Delete
    }
