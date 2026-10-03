package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.AppOption
import com.sqftware.orbitlauncher.domain.AppShortcut
import com.sqftware.orbitlauncher.domain.HomePlace
import com.sqftware.orbitlauncher.domain.UnreadCounts
import com.sqftware.orbitlauncher.domain.title

object AppOptionsTags {
    const val MENU = "app_options"
}

/** A long-press menu: [onOpen] asks for it on one item, and [content], drawn inside each item, shows it while it is open there. */
class LongPressMenu<T>(val onOpen: (T) -> Unit, val content: @Composable (T) -> Unit)

typealias AppMenu = LongPressMenu<AppEntry>

/**
 * [onClearBadge] while [app]'s badge holds notifications the user dismissed unread, or none: a double tap delays every tap
 * by the wait for a second one, so it is armed only where it has something to clear.
 */
internal fun UnreadCounts.clearing(app: AppEntry, onClearBadge: ((AppEntry) -> Unit)?) = onClearBadge?.takeIf { hasDismissed(app) }

/**
 * A tap launches [app]; with a [menu], a long press opens it, and with [onClearBadge], a double tap clears its badge instead
 * of launching. Given [presses], the presses are only recorded there, for a ripple drawn on another node, rather than shown
 * on this one.
 */
fun Modifier.launchable(
    app: AppEntry,
    onLaunch: (AppEntry) -> Unit,
    menu: AppMenu?,
    presses: MutableInteractionSource? = null,
    onClearBadge: ((AppEntry) -> Unit)? = null,
): Modifier {
    val onLongClickLabel = menu?.let { "App options" }
    // A hold is never a tap: with no menu to open it does nothing, rather than launching the app on release.
    val onLongClick = menu?.let { m -> { m.onOpen(app) } } ?: {}
    val onClick = { onLaunch(app) }
    val onDoubleClick = onClearBadge?.let { clear -> { clear(app) } }
    val clicks = if (presses == null) {
        combinedClickable(onLongClickLabel = onLongClickLabel, onLongClick = onLongClick, onDoubleClick = onDoubleClick, onClick = onClick)
    } else {
        combinedClickable(
            interactionSource = presses,
            indication = null,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            onClick = onClick,
        )
    }
    // A screen reader's double tap is a click, so clearing needs an action of its own.
    return if (onDoubleClick == null) {
        clicks
    } else {
        clicks.semantics { customActions = listOf(CustomAccessibilityAction("Clear badge") { onDoubleClick(); true }) }
    }
}

/**
 * An app's long-press menu: its [shortcuts] first, then the [options] for where it was pressed. Choosing an item dismisses
 * the menu, then hands on the choice. It keeps what it shows while [expanded] turns false, so it animates away whole.
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
    fun choose(choice: () -> Unit) = chooseFrom(expanded, onDismiss, choice)

    GroundMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(AppOptionsTags.MENU)) {
        shortcuts.forEach { shortcut ->
            DropdownMenuItem(
                text = { Text(shortcut.label) },
                leadingIcon = { ShortcutIcon(shortcut, shortcutIcon) },
                onClick = { choose { onShortcut(shortcut) } },
            )
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider()
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                leadingIcon = { Icon(option.icon, contentDescription = null) },
                onClick = { choose { onOption(option) } },
            )
        }
    }
}

/** Dismisses a menu, then acts. Items stay tappable while the menu animates away, and a second tap must not start the same thing twice. */
internal fun chooseFrom(expanded: Boolean, onDismiss: () -> Unit, choice: () -> Unit) {
    if (!expanded) return
    onDismiss()
    choice()
}

@Composable
private fun ShortcutIcon(shortcut: AppShortcut, icon: suspend (AppShortcut) -> ImageBitmap?) {
    val bitmap by produceState<ImageBitmap?>(null, shortcut) { value = icon(shortcut) }

    Box(Modifier.size(24.dp)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

private val SplitGlyph = materialGlyph(
    "Splitscreen",
    "M18 4v5H6V4h12m0-2H6c-1.1 0-2 .9-2 2v5c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 13v5H6v-5h12m0-2H6" +
        "c-1.1 0-2 .9-2 2v5c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2v-5c0-1.1-.9-2-2-2z",
)

private val AppOption.label
    get() = when (this) {
        is AppOption.Remove -> when (place) {
            HomePlace.Ring -> "Remove from the ring"
            HomePlace.Dock -> "Remove from the dock"
            is HomePlace.Folder -> "Remove from folder"
        }
        AppOption.NewFolder -> "New folder"
        is AppOption.RemoveFromCard -> "Remove from ${kind.title}"
        is AppOption.AddTo -> when (place) {
            HomePlace.Ring -> "Add to the ring"
            HomePlace.Dock -> "Add to the dock"
        }
        AppOption.SplitWith -> "Split with…"
        AppOption.Flip -> "Flip order"
        is AppOption.Badge -> listOfNotNull(if (isOff) "Show" else "Hide", member?.label, "badge").joinToString(" ")
        is AppOption.BuiltInCards -> if (isOff) "Show in New & Most Used" else "Hide from New & Most Used"
        AppOption.PlayStore -> "Open in Play Store"
        AppOption.AppInfo -> "App info"
        AppOption.Uninstall -> "Uninstall"
    }

private val AppOption.icon
    get() = when (this) {
        is AppOption.Remove -> Icons.Default.Close
        AppOption.NewFolder -> FolderGlyph
        is AppOption.RemoveFromCard -> Icons.Default.Close
        is AppOption.AddTo -> Icons.Default.Add
        AppOption.SplitWith -> SplitGlyph
        AppOption.Flip -> SwapGlyph
        is AppOption.Badge -> Icons.Default.Notifications
        is AppOption.BuiltInCards -> Icons.Default.Star
        AppOption.PlayStore -> Icons.Default.ShoppingCart
        AppOption.AppInfo -> Icons.Default.Info
        AppOption.Uninstall -> Icons.Default.Delete
    }

private val SwapGlyph = materialGlyph("SwapVert", "M16 17.01V10h-2v7.01h-3L15 21l4-3.99h-3zM9 3L5 6.99h3V14h2V6.99h3L9 3z")
