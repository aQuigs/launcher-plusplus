package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .launchable(app, onLaunch, menu)
            .semantics { contentDescription = app.label },
    ) {
        AppImage(app, icon, Modifier.fillMaxSize())
        menu?.content?.invoke(app)
    }
}

/** An app's icon alone, with no name and nothing to tap; blank until it has loaded. */
@Composable
fun AppImage(app: AppEntry, icon: suspend (AppEntry) -> ImageBitmap?, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, app.key) {
        // Handed another app, as a folder's preview is when an app leaves it, the cell must not keep showing the old one.
        value = null
        value = icon(app)
    }

    Box(modifier) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
