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

    fun place(place: HomePlace) = "place_${place.name}"
}

/** Heads the drawer while picking: which [place] a tap fills, with a switch to the other places, and what a tap does. */
@Composable
fun PlacePicker(place: HomePlace, onPlaceChange: (HomePlace) -> Unit, modifier: Modifier = Modifier) {
    val places = HomePlace.entries

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp).testTag(PlacePickerTags.PICKER),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            places.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == place,
                    onClick = { onPlaceChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, places.size),
                    label = { Text(option.name) },
                    modifier = Modifier.testTag(PlacePickerTags.place(option)),
                )
            }
        }
        Text(
            text = "Tap apps to add them or take them off",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
