package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.Unread
import com.sqftware.orbitlauncher.domain.badgeText

object BadgeTags {
    const val BUBBLE = "badge"
}

/** How far a badge reaches past its icon's top-end corner, both across and up. */
internal val BADGE_OVERHANG = 4.dp

private val BADGE_RING = 2.dp

/**
 * A bubble saying how many notifications are [unread], meant for an icon's top-end corner, or nothing for none. It is
 * filled when it holds some the user dismissed unread, which a double tap clears, and a ring when all are still in the
 * shade, so a double tap is not tried where it does nothing.
 */
@Composable
fun UnreadBadge(unread: Unread, modifier: Modifier = Modifier) {
    if (unread.count <= 0) return

    val colors = MaterialTheme.colorScheme
    Text(
        text = badgeText(unread.count),
        style = MaterialTheme.typography.labelSmall,
        color = if (unread.dismissed) colors.onError else colors.error,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = modifier
            // Over the corner rather than inside it: a round icon has no room there.
            .offset(x = BADGE_OVERHANG, y = -BADGE_OVERHANG)
            .then(
                if (unread.dismissed) {
                    Modifier.background(colors.error, CircleShape)
                } else {
                    Modifier.background(colors.surface, CircleShape).border(BADGE_RING, colors.error, CircleShape)
                },
            )
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .padding(horizontal = 5.dp)
            .wrapContentHeight()
            .testTag(BadgeTags.BUBBLE),
    )
}

/** This label for a screen reader, with how many [unread] when there are any. */
internal fun String.withUnread(unread: Int): String = if (unread > 0) "$this, $unread unread" else this
