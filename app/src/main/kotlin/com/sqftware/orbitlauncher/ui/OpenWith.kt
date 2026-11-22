package com.sqftware.orbitlauncher.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.InputQuestion
import com.sqftware.orbitlauncher.domain.InputValue
import com.sqftware.orbitlauncher.domain.OpenChoice
import com.sqftware.orbitlauncher.domain.PlaceValue
import com.sqftware.orbitlauncher.domain.currentChoice
import com.sqftware.orbitlauncher.domain.intentBase
import com.sqftware.orbitlauncher.domain.openChoiceLabel
import com.sqftware.orbitlauncher.domain.openChoices

object OpenWithTags {
    const val CHOICES = "open_with_choices"
    const val PROMPT = "input_prompt"
    const val FIELD = "input_prompt_field"
}

/**
 * Asks how [app], beside [partner] if it is one of a pair, opens: as usual, or straight to one of its intents, with what
 * it is opened as now marked.
 */
@Composable
fun OpenWithDialog(app: AppEntry, partner: AppEntry?, onChoose: (OpenChoice) -> Unit, onDismiss: () -> Unit) {
    val base = app.intentBase ?: return
    ChoiceDialog(
        title = "How ${base.label} opens",
        choices = openChoices(app, partner),
        chosen = currentChoice(app, partner),
        label = ::openChoiceLabel,
        onChoose = onChoose,
        onDismiss = onDismiss,
        modifier = Modifier.testTag(OpenWithTags.CHOICES),
    )
}

/**
 * What the prompts for an intent's input offer, search and keep: the [recentPlaces], to which [onPlacePicked] adds, and
 * [findPlaces] and [openMaps].
 */
class InputSources(
    val recentPlaces: List<PlaceValue>,
    val onPlacePicked: (PlaceValue) -> Unit,
    val findPlaces: suspend (String) -> List<PlaceValue>,
    val openMaps: () -> Unit,
)

/**
 * Asks [question] from [sources], searching for the [query] typed, and hands the input picked to [onAnswer]. Each kind
 * of input has a prompt of its own, so a new one does not compile until it has one.
 */
@Composable
fun InputPrompt(
    question: InputQuestion,
    query: String,
    onQueryChange: (String) -> Unit,
    sources: InputSources,
    onAnswer: (InputValue) -> Unit,
    onDismiss: () -> Unit,
) {
    when (question.kind) {
        InputKind.Place -> PlacePrompt(question.forWhom, query, onQueryChange, sources, onAnswer, onDismiss)
    }
}

/**
 * The recent places while nothing is typed, else the places the search finds and, last, what was typed as it is, for an
 * app to look up itself, picked [forWhom]. A place found in the maps app comes back shared, into the field.
 */
@Composable
private fun PlacePrompt(
    forWhom: String,
    query: String,
    onQueryChange: (String) -> Unit,
    sources: InputSources,
    onAnswer: (PlaceValue) -> Unit,
    onDismiss: () -> Unit,
) {
    SearchDialog(
        title = "Pick a place for $forWhom",
        fieldLabel = "Address or place",
        query = query,
        onQueryChange = onQueryChange,
        idle = sources.recentPlaces,
        find = { typed -> sources.findPlaces(typed) + typed.trim().let { PlaceValue(label = it, address = it) } },
        label = PlaceValue::label,
        description = { place -> if (place.latitude == null) "As typed" else place.address.takeIf { it != place.label } },
        onChoose = { place ->
            sources.onPlacePicked(place)
            onAnswer(place)
        },
        onDismiss = onDismiss,
        extra = { LauncherButton(onClick = sources.openMaps) { Text("Find in Maps") } },
        fieldTag = OpenWithTags.FIELD,
        modifier = Modifier.testTag(OpenWithTags.PROMPT),
    )
}
