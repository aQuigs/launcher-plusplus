package com.sqftware.orbitlauncher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * One segmented button per option, under its label, the [selected] one lit; a tap picks one. Every segmented choice of the
 * launcher is one of these (held by the launcher-surfaces hook), each segment taking its [item] modifier, such as a tag.
 */
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    item: (T) -> Modifier = { Modifier },
) {
    // Lit in the primary container, as a selected Pane is: the default fill is a faint tint that vanishes on a pop-up.
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )

    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = colors,
                // No tick: it would crowd out the label in a narrow segment, and the fill already marks the choice.
                icon = {},
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = item(option),
            )
        }
    }
}
