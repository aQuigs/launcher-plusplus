package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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

/** Says the launcher is not the home app yet; its one button asks the user to make it so. */
@Composable
fun HomeAppCard(onBecomeHomeApp: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.testTag(HomeAppCardTags.CARD)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Launcher++ is not your home app yet",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBecomeHomeApp, modifier = Modifier.testTag(HomeAppCardTags.BUTTON)) {
                Text("Set as home")
            }
        }
    }
}
