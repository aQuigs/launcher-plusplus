package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.Ring
import com.aquigs.launcherplusplus.domain.RingItem

object FolderTags {
    const val POPUP = "folder_popup"
    const val MENU = "folder_options"
    const val RENAME = "folder_rename"
    const val NAME = "folder_name"
}

/** A folder's long-press menu, opened and shown like an [AppMenu]. */
typealias FolderMenu = LongPressMenu<RingItem.Folder>

/** What a folder's long-press menu offers, in menu order. */
enum class FolderOption(val label: String, val icon: ImageVector) {
    Rename("Rename", Icons.Default.Edit),
    AddApps("Add apps", Icons.Default.Add),
    Remove("Remove folder", Icons.Default.Close),
}

/** Material's folder glyph, which the core icon set leaves out. */
val FolderGlyph: ImageVector = materialIcon("Folder") {
    materialPath {
        moveTo(10f, 4f)
        horizontalLineTo(4f)
        curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
        lineTo(2f, 18f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(8f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineToRelative(-8f)
        lineToRelative(-2f, -2f)
        close()
    }
}

private const val FOLDER_COLUMNS = 4
private const val PREVIEW_SIDE = 2

/**
 * A ring slot holding [folder]: a circle, the size of an app's, previewing up to four of its icons in a small grid. A tap
 * opens the folder and a long press opens its [menu].
 */
@Composable
fun FolderIcon(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onOpen: (RingItem.Folder) -> Unit,
    modifier: Modifier = Modifier,
    menu: FolderMenu? = null,
) {
    val count = folder.apps.size

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onLongClickLabel = menu?.let { "Folder options" },
                onLongClick = menu?.let { m -> { m.onOpen(folder) } },
                onClick = { onOpen(folder) },
            )
            .semantics { contentDescription = "Folder ${folder.name}, $count ${if (count == 1) "app" else "apps"}" },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxSize(0.62f)) {
            repeat(PREVIEW_SIDE) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    repeat(PREVIEW_SIDE) { column ->
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            folder.apps.getOrNull(row * PREVIEW_SIDE + column)?.let { AppImage(it, icon, Modifier.fillMaxSize()) }
                        }
                    }
                }
            }
        }
        menu?.content?.invoke(folder)
    }
}

/**
 * An open [folder]: its name over its apps, four to a row, and a button to add more. A tap launches the app and a long
 * press opens its [menu]. It is a window of its own, so Back closes it before anything on the screen behind.
 */
@Composable
fun FolderPopup(
    folder: RingItem.Folder,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    onAddApps: () -> Unit,
    onDismiss: () -> Unit,
    menu: AppMenu? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp, modifier = Modifier.testTag(FolderTags.POPUP)) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                ) {
                    folder.apps.chunked(FOLDER_COLUMNS).forEach { row ->
                        Row {
                            row.forEach { app -> FolderApp(app, icon, onLaunch, menu, Modifier.weight(1f)) }
                            repeat(FOLDER_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onAddApps) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Add apps")
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderApp(
    app: AppEntry,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    menu: AppMenu?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .launchable(app, onLaunch, menu)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        AppImage(app, icon, Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        menu?.content?.invoke(app)
    }
}

/** A folder's long-press menu. Choosing an item dismisses the menu, then hands on the choice. */
@Composable
fun FolderOptionsMenu(expanded: Boolean, onOption: (FolderOption) -> Unit, onDismiss: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.testTag(FolderTags.MENU)) {
        FolderOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.label) },
                leadingIcon = { Icon(option.icon, contentDescription = null) },
                onClick = { chooseFrom(expanded, onDismiss) { onOption(option) } },
            )
        }
    }
}

/** Asks for a folder's new name, starting from [name]. A name that [Ring.name] rejects cannot be confirmed. */
@Composable
fun RenameFolderDialog(name: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(name) { mutableStateOf(name) }
    val focusRequester = remember { FocusRequester() }
    val newName = Ring.name(text)

    fun confirm() {
        newName?.let(onRename)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename folder") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier.focusRequester(focusRequester).testTag(FolderTags.NAME),
            )
        },
        confirmButton = { TextButton(onClick = ::confirm, enabled = newName != null) { Text("Rename") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        modifier = Modifier.testTag(FolderTags.RENAME),
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
