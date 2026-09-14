package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.background
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
import com.aquigs.launcherplusplus.domain.badgeText

object BadgeTags {
    const val BUBBLE = "badge"
}

/** A bubble saying how many [unread] notifications an icon has, meant for its top-end corner, or nothing for none. */
@Composable
fun UnreadBadge(unread: Int, modifier: Modifier = Modifier) {
    if (unread <= 0) return

    Text(
        text = badgeText(unread),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onError,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = modifier
            // Over the corner rather than inside it: a round icon has no room there.
            .offset(x = 4.dp, y = (-4).dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .padding(horizontal = 5.dp)
            .wrapContentHeight()
            .testTag(BadgeTags.BUBBLE),
    )
}

/** This label for a screen reader, with how many [unread] when there are any. */
internal fun String.withUnread(unread: Int): String = if (unread > 0) "$this, $unread unread" else this
