package com.sqftware.orbitlauncher.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.ChatApp
import com.sqftware.orbitlauncher.domain.ChatValue
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.InputQuestion
import com.sqftware.orbitlauncher.domain.InputValue
import com.sqftware.orbitlauncher.domain.KeptInputs
import com.sqftware.orbitlauncher.domain.MAX_CHAT_NAME
import com.sqftware.orbitlauncher.domain.OpenChoice
import com.sqftware.orbitlauncher.domain.PlaceValue
import com.sqftware.orbitlauncher.domain.matching
import com.sqftware.orbitlauncher.domain.currentChoice
import com.sqftware.orbitlauncher.domain.intentBase
import com.sqftware.orbitlauncher.domain.openChoiceLabel
import com.sqftware.orbitlauncher.domain.openChoices
import com.sqftware.orbitlauncher.domain.searchable

object OpenWithTags {
    const val CHOICES = "open_with_choices"
    const val PROMPT = "input_prompt"
    const val FIELD = "input_prompt_field"
    const val NAME = "input_prompt_name"
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
 * What the prompts for an intent's input offer, search and keep: the [places] ([onPlacesChange]), [findPlaces] and
 * [openMaps]; each app's [chats] ([onChatsChange]), [findChat] and what was [pasted].
 */
class InputSources(
    val places: KeptInputs<PlaceValue>,
    val onPlacesChange: (KeptInputs<PlaceValue>) -> Unit,
    val findPlaces: suspend (String) -> List<PlaceValue>,
    val openMaps: () -> Unit,
    val chats: Map<ChatApp, KeptInputs<ChatValue>>,
    val onChatsChange: (ChatApp, KeptInputs<ChatValue>) -> Unit,
    val findChat: suspend (ChatApp, String) -> ChatValue?,
    val pasted: () -> String?,
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
    when (val kind = question.kind) {
        InputKind.Place -> PlacePrompt(question.forWhom, query, onQueryChange, sources, onAnswer, onDismiss)
        is ChatApp -> ChatPrompt(kind, question.forWhom, query, onQueryChange, sources, onAnswer, onDismiss)
    }
}

/**
 * The favorite and recent places while nothing is typed, else the places the search finds and, last, what was typed as it
 * is, for an app to look up itself, picked [forWhom]. A place found in the maps app comes back shared, into the field.
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
        idle = keptSections(sources.places),
        idleNote = "Places you pick show up here",
        find = { typed -> sources.findPlaces(typed) + typed.trim().let { PlaceValue(label = it, address = it) } },
        label = PlaceValue::label,
        description = { place -> if (place.latitude == null) "As typed" else place.address.takeIf { it != place.label } },
        actions = { place -> keptActions(place, sources.places, forgets = query.isBlank(), sources.onPlacesChange) },
        onChoose = { place ->
            sources.onPlacesChange(sources.places.picked(place))
            onAnswer(place)
        },
        onDismiss = onDismiss,
        extra = { LauncherButton(onClick = sources.openMaps) { Text("Find in Maps") } },
        fieldTag = OpenWithTags.FIELD,
        modifier = Modifier.testTag(OpenWithTags.PROMPT),
    )
}

/**
 * The favorite and recent chats of [app] while nothing is typed, else those named like what is, and the one a typed link
 * or phone number tells, picked [forWhom]. A new one is named before it is picked, as hardly any link names its chat.
 */
@Composable
private fun ChatPrompt(
    app: ChatApp,
    forWhom: String,
    query: String,
    onQueryChange: (String) -> Unit,
    sources: InputSources,
    onAnswer: (ChatValue) -> Unit,
    onDismiss: () -> Unit,
) {
    val kept = sources.chats[app] ?: KeptInputs()
    val chats = kept.favorites + kept.recent
    var naming by remember { mutableStateOf<ChatValue?>(null) }

    fun pick(chat: ChatValue) {
        // The prompt after this one can be in its place, asking for the same app's other build.
        naming = null
        sources.onChatsChange(app, kept.picked(chat))
        onAnswer(chat)
    }

    naming?.let { chat ->
        NameDialog(
            title = "Name this ${app.noun}",
            verb = "Save",
            maxLength = MAX_CHAT_NAME,
            onName = { name -> pick(chat.named(name)) },
            onDismiss = { naming = null },
            fieldTag = OpenWithTags.NAME,
            initial = chat.label,
        )
        return
    }
    SearchDialog(
        title = "Pick a $forWhom ${app.noun}",
        fieldLabel = "Paste a link, or search",
        query = query,
        onQueryChange = onQueryChange,
        idle = keptSections(kept),
        idleNote = app.howToLink,
        find = { typed ->
            sources.findChat(app, typed)?.let { found -> listOf(chats.find { it.link == found.link } ?: found) } ?: chats.matching(typed) { it.label.searchable() }
        },
        label = { chat -> chat.label.ifEmpty { "New ${app.noun}" } },
        description = { chat -> chat.link.takeIf { chat !in chats } },
        actions = { chat -> if (chat !in chats) emptyList() else keptActions(chat, kept, forgets = query.isBlank()) { sources.onChatsChange(app, it) } },
        onChoose = { chat -> if (chat !in chats) naming = chat else pick(chat) },
        onDismiss = onDismiss,
        extra = { LauncherButton(onClick = { sources.pasted()?.let { onQueryChange(it.replace('\n', ' ')) } }) { Text("Paste") } },
        fieldTag = OpenWithTags.FIELD,
        modifier = Modifier.testTag(OpenWithTags.PROMPT),
    )
}

/** The sections a prompt shows of the values [kept], which the user knows by name, so one line of each description will do. */
private fun <T : InputValue> keptSections(kept: KeptInputs<T>) =
    listOf(ChoiceSection("Favorites", kept.favorites, descriptionLines = 1), ChoiceSection("Recent", kept.recent, descriptionLines = 1))

/** What a prompt's row for [value] offers: to make it a favorite or not and, if it [forgets], to delete it from the recent ones. */
private fun <T : InputValue> keptActions(
    value: T,
    kept: KeptInputs<T>,
    forgets: Boolean,
    onChange: (KeptInputs<T>) -> Unit,
): List<ChoiceAction> = buildList {
    val favorite = kept.isFavorite(value)
    add(
        ChoiceAction(
            glyph = if (favorite) Icons.Default.Star else StarOutlineGlyph,
            label = if (favorite) "Remove ${value.label} from favorites" else "Add ${value.label} to favorites",
        ) { onChange(kept.toggledFavorite(value)) },
    )
    if (forgets && value in kept.recent) {
        add(ChoiceAction(Icons.Default.Close, "Delete ${value.label} from recent") { onChange(kept.forgotten(value)) })
    }
}
