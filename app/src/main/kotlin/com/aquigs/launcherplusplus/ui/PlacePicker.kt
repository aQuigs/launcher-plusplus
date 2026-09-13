package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aquigs.launcherplusplus.domain.HomePlace

object PlacePickerTags {
    const val PICKER = "place_picker"

    fun place(place: HomePlace) = "place_$place"
}

private val switchablePlaces = listOf(HomePlace.Ring to "Ring", HomePlace.Dock to "Dock")

/** Heads the drawer while picking for the ring or the dock: which [place] a tap fills, with a switch to the other, and what a tap does. */
@Composable
fun PlacePicker(place: HomePlace, onPlaceChange: (HomePlace) -> Unit, modifier: Modifier = Modifier) {
    PickingHeader(modifier) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            switchablePlaces.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = option == place,
                    onClick = { onPlaceChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, switchablePlaces.size),
                    label = { Text(label) },
                    modifier = Modifier.testTag(PlacePickerTags.place(option)),
                )
            }
        }
    }
}

/** Heads the drawer while picking for a folder: which folder a tap fills, and what a tap does. */
@Composable
fun FolderPicker(name: String, modifier: Modifier = Modifier) {
    PickingHeader(modifier) {
        Text(text = "Adding apps to “$name”", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PickingHeader(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag(PlacePickerTags.PICKER),
    ) {
        content()
        Text(
            text = "Tap apps to add them or take them off",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
