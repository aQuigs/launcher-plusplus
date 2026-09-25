package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sqftware.orbitlauncher.domain.AppEntry

/**
 * One app as a round icon, named by its label for screen readers, with a badge for its [unread] notifications. A tap
 * launches it, a long press opens its [menu], and a long press that goes on becomes a [drag]; its parent decides the
 * size.
 */
@Composable
fun AppIcon(
    app: AppEntry,
    icon: suspend (AppEntry) -> ImageBitmap?,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    menu: AppMenu? = null,
    unread: Int = 0,
    drag: AppDrag? = null,
) {
    val presses = remember { MutableInteractionSource() }

    Box(
        modifier
            .launchable(app, onLaunch, menu, presses)
            .itemDrag(app, drag)
            .semantics { contentDescription = app.label.withUnread(unread) },
    ) {
        IconDisc(presses, Modifier.fillMaxSize()) { AppImage(app, icon, Modifier.fillMaxSize()) }
        menu?.content?.invoke(app)
        UnreadBadge(unread, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * The round face of an icon: [content] on a tinted disc, clipped to it, rippling for the presses in [presses] when it
 * has any. The press handling, the badge and the name stay on the icon's own node outside, so the badge can overhang the
 * disc while a screen reader still meets one icon, and the ripple keeps to the disc all the same. The whole square is
 * the target, badge included, as on other launchers.
 */
@Composable
fun IconDisc(
    presses: InteractionSource? = null,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (presses != null) Modifier.indication(presses, ripple()) else Modifier),
        contentAlignment = contentAlignment,
        content = content,
    )
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
