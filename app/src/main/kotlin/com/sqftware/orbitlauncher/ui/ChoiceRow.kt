package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** One button per option, under its label and tagged by [tag], the [selected] one checked; a tap picks one. */
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    tag: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val choices = options.map { (option, label) -> Choice(label, tag(option), option == selected) { onSelect(option) } }
    LocalThemeArt.current.ChoiceButtons(choices, modifier)
}

/** One of a row of buttons: its [label], the [tag] tests find it by, whether it is [selected], and what a tap does. */
class Choice(val label: String, val tag: String, val selected: Boolean, val onSelect: () -> Unit)

/** [ChoiceRow]'s buttons as most themes draw them: Material's segmented buttons, across the width. */
@Composable
internal fun SegmentedChoices(choices: List<Choice>, modifier: Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        choices.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = choice.selected,
                onClick = choice.onSelect,
                shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                label = { Text(choice.label) },
                modifier = Modifier.testTag(choice.tag),
            )
        }
    }
}
