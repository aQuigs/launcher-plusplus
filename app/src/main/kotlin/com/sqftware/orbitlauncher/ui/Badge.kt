package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sqftware.orbitlauncher.domain.AppEntry
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
 * filled when it holds some the user dismissed unread, which a double tap on an app clears, and a ring when all are
 * still in the shade, so a double tap is not tried where it does nothing. A folder's is filled when an app in it is.
 * The ring's fill is opaque, as it overlaps the icon and the digits must not hang on what is beneath.
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
            .cornerBubble(top = true, fill = if (unread.dismissed) colors.error else colors.surfaceContainerHigh, edge = colors.error)
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .padding(horizontal = 5.dp)
            .wrapContentHeight()
            .testTag(BadgeTags.BUBBLE),
    )
}

/**
 * A briefcase on a work app, meant for its icon's bottom-end corner where the system's own work badge sits, or nothing
 * for an app of the user's own. Over the corner, as the round icon would cut the system's badge drawn into it.
 */
@Composable
fun WorkMark(app: AppEntry, modifier: Modifier = Modifier) {
    if (!app.isWork) return

    val colors = MaterialTheme.colorScheme
    Icon(
        imageVector = WorkGlyph,
        contentDescription = "Work",
        tint = colors.primary,
        modifier = modifier
            .cornerBubble(top = false, fill = colors.surfaceContainerHigh, edge = colors.primary)
            .size(18.dp)
            .padding(4.dp),
    )
}

/** A ringed bubble reaching [BADGE_OVERHANG] past the top-end corner, or past the bottom-end one unless [top]. */
private fun Modifier.cornerBubble(top: Boolean, fill: Color, edge: Color) =
    offset(x = BADGE_OVERHANG, y = if (top) -BADGE_OVERHANG else BADGE_OVERHANG)
        .background(fill, CircleShape)
        .border(BADGE_RING, edge, CircleShape)

/** This label for a screen reader, with how many [unread] when there are any. */
internal fun String.withUnread(unread: Int): String = if (unread > 0) "$this, $unread unread" else this
