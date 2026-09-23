package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.AppEntry

object PinDialogTags {
    const val DIALOG = "pin_dialog"
}

/**
 * An app asking to pin [shortcut] to the home screen; [appLabel] names the app. [icon] draws the shortcut, and [accept]
 * pins it and says whether it did.
 */
class PinRequest(val shortcut: AppEntry, val appLabel: String, val icon: suspend () -> ImageBitmap?, val accept: () -> Boolean)

/**
 * Asks whether to add [request]'s shortcut to the ring, showing it on the disc it will wear there. Cancel, Back and a tap
 * outside call [onDismiss].
 */
@Composable
fun PinDialog(request: PinRequest, onAdd: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onAdd) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        icon = { IconDisc(modifier = Modifier.size(48.dp)) { AppImage(request.shortcut, { request.icon() }, Modifier.fillMaxSize()) } },
        title = { Text(request.shortcut.label) },
        text = { Text("Shortcut from ${request.appLabel}") },
        modifier = Modifier.testTag(PinDialogTags.DIALOG),
    )
}
