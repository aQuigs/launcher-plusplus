package com.sqftware.orbitlauncher.ui

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sqftware.orbitlauncher.ui.theme.LocalBoxColors

/**
 * One button per option, under its label, the [selected] one lit; a tap picks one. Each button takes its [item]
 * modifier, such as a tag.
 */
@Composable
fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    item: (T) -> Modifier = { Modifier },
) {
    val choices = options.map { (option, label) -> Choice(label, option == selected, item(option)) { onSelect(option) } }
    LocalThemeArt.current.ChoiceButtons(choices, modifier)
}

/** One of a row of buttons: its [label], whether it is [selected], the [modifier] it takes, and what a tap does. */
class Choice(val label: String, val selected: Boolean, val modifier: Modifier, val onSelect: () -> Unit)

/** [ChoiceRow]'s buttons as most themes draw them: Material's segmented buttons. */
@Composable
internal fun SegmentedChoices(choices: List<Choice>, modifier: Modifier) {
    // Lit as a selected Pane is: the default fill is a faint tint that vanishes on a pop-up.
    val boxes = LocalBoxColors.current
    val colors = SegmentedButtonDefaults.colors(activeContainerColor = boxes.lit, activeContentColor = boxes.onLit)

    SingleChoiceSegmentedButtonRow(modifier) {
        choices.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = choice.selected,
                onClick = choice.onSelect,
                shape = SegmentedButtonDefaults.itemShape(index, choices.size, MaterialTheme.shapes.extraLarge),
                colors = colors,
                // No tick: it would crowd out the label in a narrow segment, and the fill already marks the choice.
                icon = {},
                // One size for every choice, small enough that a word fits a narrow segment in a box.
                label = { Text(choice.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = choice.modifier,
            )
        }
    }
}

/** A chip per one of [choices], under its [label], the [chosen] one lit as a [ChoiceRow]'s is; a tap picks one. */
@Composable
fun <T> ChoiceChips(choices: List<T>, chosen: T, label: (T) -> String, onChoose: (T) -> Unit) {
    val boxes = LocalBoxColors.current
    val colors = FilterChipDefaults.filterChipColors(selectedContainerColor = boxes.lit, selectedLabelColor = boxes.onLit)

    choices.forEach { choice ->
        FilterChip(selected = choice == chosen, onClick = { onChoose(choice) }, label = { Text(label(choice)) }, colors = colors)
    }
}
