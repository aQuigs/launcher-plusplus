package com.sqftware.orbitlauncher.domain

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Something an app can be opened straight to, such as a ride to a place, rather than its start screen. The
 * [IntentCatalog] lists every one the launcher knows; an app with none there offers none.
 */
interface AppIntent {
    /** Kept in the keys of the icons made with it, so it never changes. */
    val id: String

    val packageName: String

    /** What it does, as an icon's label names it after its app's: "ride" makes "Uber ride". */
    val name: String

    /** What it does, as the list to choose it from names it, with its [input] as a noun: "Ride to a place". */
    val label: String

    /** What it fills in, if anything. */
    val input: InputKind?

    /** What opens it, filled in with [value] when it takes an [input]. */
    fun request(value: InputValue?): IntentRequest
}

/** An activity of [packageName] to start for [action] on [data], as an Android intent names one. */
data class IntentRequest(val packageName: String, val data: String? = null, val action: String = ACTION_VIEW)

const val ACTION_VIEW = "android.intent.action.VIEW"

/** A kind of value an [AppIntent] fills in. Each kind has a prompt of its own, which asks for one. */
enum class InputKind {
    Place {
        override fun decode(text: String): InputValue? {
            val parts = text.split(FIELD_SEPARATOR).map { URLDecoder.decode(it, "UTF-8") }
            if (parts.size != 4) return null
            return PlaceValue(parts[0], parts[1], parts[2].toDoubleOrNull(), parts[3].toDoubleOrNull())
        }
    };

    /** The value [InputValue.encode] made, or null for text that is not one of this kind. */
    abstract fun decode(text: String): InputValue?
}

/** A value an [AppIntent] is filled in with. */
sealed interface InputValue {
    val kind: InputKind

    /** What a list of them, or the icon it is saved with, names it by. */
    val label: String

    /** It as text [InputKind.decode] reads back, so an icon's key can hold it. */
    fun encode(): String
}

/** A place called [label] at [address], and where it is when that is known. Its fields may hold any text, line breaks too. */
data class PlaceValue(
    override val label: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
) : InputValue {
    override val kind get() = InputKind.Place

    override fun encode() = listOf(label, address, latitude?.toString().orEmpty(), longitude?.toString().orEmpty())
        .joinToString(FIELD_SEPARATOR) { URLEncoder.encode(it, "UTF-8") }
}

private const val FIELD_SEPARATOR = "\n"

/** Where the value an [AppIntent] fills in comes from. */
sealed interface InputSource {
    /** Chosen once, with the icon. */
    data class Saved(val value: InputValue) : InputSource

    /** Asked for each time the icon is opened. */
    data object Ask : InputSource
}

/** One way to have an app's icon open it, as the list to choose from offers them. */
sealed interface OpenChoice {
    /** Its start screen, as an icon of the app's own does. */
    data object Usual : OpenChoice

    /** Straight to [intent], asking for its input each time if it [asks], else with the input chosen now. */
    data class Intent(val intent: AppIntent, val asks: Boolean) : OpenChoice
}

/** How the list names [choice]. */
fun openChoiceLabel(choice: OpenChoice): String = when (choice) {
    OpenChoice.Usual -> "Open as usual"
    is OpenChoice.Intent -> when {
        choice.intent.input == null -> choice.intent.label
        choice.asks -> "${choice.intent.label} you pick each time"
        else -> "${choice.intent.label} you pick now"
    }
}

/**
 * The ways [app] can open, for an icon of a plain app or of one made with an intent: as usual, then each intent the
 * catalog has for it, twice over for one that takes an input, picked now or each time. None for an app with no intents.
 */
fun openChoices(app: AppEntry): List<OpenChoice> {
    val base = app.intentBase ?: return emptyList()
    val intents = IntentCatalog.forPackage(base.packageName)
    if (intents.isEmpty()) return emptyList()
    return listOf(OpenChoice.Usual) + intents.flatMap { intent ->
        if (intent.input == null) listOf(OpenChoice.Intent(intent, asks = false))
        else listOf(OpenChoice.Intent(intent, asks = false), OpenChoice.Intent(intent, asks = true))
    }
}

/** What [app] opens as now, of [openChoices]: null for one that saved an input, as picking it again asks anew. */
fun currentChoice(app: AppEntry): OpenChoice? = when (val kind = app.kind) {
    is EntryKind.Intent -> if (kind.source is InputSource.Saved) null else OpenChoice.Intent(kind.intent, kind.source == InputSource.Ask)
    else -> OpenChoice.Usual
}

/** The plain app an icon opens with an intent, or one that could: itself if it is one. Null for anything else. */
val AppEntry.intentBase: AppEntry?
    get() = when (val kind = kind) {
        is EntryKind.Intent -> kind.app
        else -> takeIf { it.isApp && !it.isWork }
    }

/**
 * The icon that opens [app] straight to [intent], with its input from [source], or null when the intent is not [app]'s, or
 * [source] does not fit what it takes: none for an intent with no input. Named after the app and what the intent does,
 * and the saved input if any.
 */
fun intentEntryOf(app: AppEntry, intent: AppIntent, source: InputSource?): AppEntry? {
    if (!app.isApp || app.isWork || app.packageName != intent.packageName) return null
    val fits = when (source) {
        null -> intent.input == null
        InputSource.Ask -> intent.input != null
        is InputSource.Saved -> source.value.kind == intent.input
    }
    if (!fits) return null
    val named = "${app.label} ${intent.name}"
    return app.copy(
        label = if (source is InputSource.Saved) "$named: ${source.value.label}" else named,
        canUninstall = false,
        kind = EntryKind.Intent(app, intent, source),
    )
}

/** [app] opened as [choice], or null if the choice needs a value it lacks: one picked now, before it is picked. */
fun openedAs(app: AppEntry, choice: OpenChoice, saved: InputValue? = null): AppEntry? {
    val base = app.intentBase ?: return null
    return when (choice) {
        OpenChoice.Usual -> base
        is OpenChoice.Intent -> {
            val source = when {
                choice.intent.input == null -> null
                choice.asks -> InputSource.Ask
                else -> InputSource.Saved(saved ?: return null)
            }
            intentEntryOf(base, choice.intent, source)
        }
    }
}

/** The kinds of input opening [app] asks for, once each, so a pair whose apps both ask for a place asks once. */
val AppEntry.asks: List<InputKind>
    get() = opens.mapNotNull { (it.kind as? EntryKind.Intent)?.takeIf { kind -> kind.source == InputSource.Ask }?.intent?.input }.distinct()

/** What opens [this] intent's app straight to it, with the input asked for from [answers]; null if that is missing. */
fun EntryKind.Intent.request(answers: Map<InputKind, InputValue>): IntentRequest? {
    val value = when (source) {
        null -> null
        InputSource.Ask -> answers[intent.input] ?: return null
        is InputSource.Saved -> source.value
    }
    return intent.request(value)
}

/**
 * The key of the icon opening the app keyed [appKey] straight to the intent [intentId], with its input from [source].
 * Keys are stored, so it must be no other kind of key: it starts with '!', which no package name, pair's or work key
 * starts with. Its parts are URL-encoded, so neither the '!' that ends them nor the '|' that ends a pair's first key, nor
 * a tab or a line break, is in one.
 */
fun intentKey(intentId: String, source: InputSource?, appKey: String): String {
    val sourceText = when (source) {
        null -> NO_INPUT
        InputSource.Ask -> ASK
        is InputSource.Saved -> SAVED + source.value.encode()
    }
    return INTENT + listOf(intentId, sourceText, appKey).joinToString(INTENT) { URLEncoder.encode(it, "UTF-8") }
}

/** The parts of the key [intentKey] made: the intent's id, its encoded source and the app's key; null for another key. */
internal fun intentKeyParts(key: String): Triple<String, String, String>? {
    if (!key.startsWith(INTENT)) return null
    val parts = key.substring(INTENT.length).split(INTENT)
    if (parts.size != 3) return null
    val (id, source, app) = parts.map { URLDecoder.decode(it, "UTF-8") }
    return Triple(id, source, app)
}

/** The icon keyed [key] by [intentKey], given [app] for its app's key, or null if it is not one or no longer makes sense. */
internal fun intentEntryFor(key: String, app: (String) -> AppEntry?): AppEntry? {
    val (id, sourceText, appKey) = intentKeyParts(key) ?: return null
    val intent = IntentCatalog.byId(id) ?: return null
    val source = when {
        sourceText == NO_INPUT -> null
        sourceText == ASK -> InputSource.Ask
        sourceText.startsWith(SAVED) -> InputSource.Saved(intent.input?.decode(sourceText.removePrefix(SAVED)) ?: return null)
        else -> return null
    }
    return intentEntryOf(app(appKey) ?: return null, intent, source)
}

private const val INTENT = "!"
private const val NO_INPUT = "none"
private const val ASK = "ask"
private const val SAVED = "saved:"
