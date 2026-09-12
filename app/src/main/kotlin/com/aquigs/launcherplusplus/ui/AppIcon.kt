package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aquigs.launcherplusplus.domain.AppEntry

/** A long-press menu for apps: [onOpen] asks for it, and [content], drawn inside each app, shows it while it is open there. */
class AppMenu(val onOpen: (AppEntry) -> Unit, val content: @Composable (AppEntry) -> Unit)

/** A tap launches [app]; with a [menu], a long press opens it. */
fun Modifier.launchable(app: AppEntry, onLaunch: (AppEntry) -> Unit, menu: AppMenu?): Modifier =
    combinedClickable(
        onLongClickLabel = menu?.let { "App options" },
        onLongClick = menu?.let { m -> { m.onOpen(app) } },
        onClick = { onLaunch(app) },
    )

/**
 * One app as a round icon, named by its label for screen readers. A tap launches it and a long press opens its [menu];
 * its parent decides the size.
 */
@Composable
fun AppIcon(
    app: AppEntry,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    menu: AppMenu? = null,
) {
    val bitmap by produceState<ImageBitmap?>(null, app.key) { value = icon(app) }

    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .launchable(app, onLaunch, menu)
            .semantics { contentDescription = app.label },
    ) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
        menu?.content?.invoke(app)
    }
}
