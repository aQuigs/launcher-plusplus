package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object HomeAppCardTags {
    const val CARD = "home_app_card"
    const val BUTTON = "home_app_button"
}

/** A slim strip that says the launcher is not the home app yet; its one button asks the user to make it so. */
@Composable
fun HomeAppCard(onBecomeHomeApp: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        // Not a circle shape: its corners would grow with a line that wraps at a large font and clip the text.
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.testTag(HomeAppCardTags.CARD),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp)) {
            Text(
                text = "Orbit is not your home app yet",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f, fill = false),
            )
            TextButton(onClick = onBecomeHomeApp, modifier = Modifier.testTag(HomeAppCardTags.BUTTON)) {
                Text("Set as home")
            }
        }
    }
}
