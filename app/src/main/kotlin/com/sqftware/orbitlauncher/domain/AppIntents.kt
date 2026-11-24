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

    /** What an icon that opens the app called [appLabel] to it with [saved] is named. */
    fun savedLabel(appLabel: String, saved: InputValue): String = "$appLabel $name: ${saved.label}"
}

/** An activity of [packageName] to start for [action] on [data], as an Android intent names one. */
data class IntentRequest(val packageName: String, val data: String? = null, val action: String = ACTION_VIEW)

const val ACTION_VIEW = "android.intent.action.VIEW"

/** A kind of value an [AppIntent] fills in. Each kind has a prompt of its own, which asks for one. */
sealed interface InputKind {
    /** The value [InputValue.encode] made, or null for text that is not one of this kind. */
    fun decode(text: String): InputValue?

    data object Place : InputKind {
        override fun decode(text: String): PlaceValue? {
            val (label, address, latitude, longitude) = decodeFields(text, 4) ?: return null
            return PlaceValue(label, address, latitude.toDoubleOrNull(), longitude.toDoubleOrNull())
        }
    }
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

    override fun encode() = encodeFields(label, address, latitude?.toString().orEmpty(), longitude?.toString().orEmpty())
}

/** [fields] as one line of text, whatever is in them, that [decodeFields] reads back. */
internal fun encodeFields(vararg fields: String): String = fields.joinToString(FIELD_SEPARATOR) { URLEncoder.encode(it, "UTF-8") }

/** The [count] fields [encodeFields] made [text] of, or null if it is not made of that many. */
internal fun decodeFields(text: String, count: Int): List<String>? =
    text.split(FIELD_SEPARATOR).takeIf { it.size == count }?.map { URLDecoder.decode(it, "UTF-8") }

// URL-encoding escapes it, and line breaks, so an encoded value is one line of its fields.
private const val FIELD_SEPARATOR = ","

/** Where the value an [AppIntent] fills in comes from. */
sealed interface InputSource {
    /** Chosen once, with the icon. */
    data class Saved(val value: InputValue) : InputSource

    /** Asked for each time the icon is opened. */
    data object Ask : InputSource

    /**
     * Asked for each time the icon is opened, by the prompt of the app beside it in a pair when that app asks for the same
     * kind of input with a prompt of its own, else by its own.
     */
    data object Shared : InputSource
}

/** One way to have an app's icon open it, as the list to choose from offers them. */
sealed interface OpenChoice {
    /** Its start screen, as an icon of the app's own does. */
    data object Usual : OpenChoice

    /**
     * Straight to [intent], asking for its input each time if it [asks], else with the input chosen now; asked for with
     * the prompt of the app it is [sharedWith] in a pair, if any.
     */
    data class Intent(val intent: AppIntent, val asks: Boolean, val sharedWith: AppEntry? = null) : OpenChoice
}

/** How the list names [choice]. */
fun openChoiceLabel(choice: OpenChoice): String = when (choice) {
    OpenChoice.Usual -> "Open as usual"
    is OpenChoice.Intent -> when {
        choice.intent.input == null -> choice.intent.label
        choice.sharedWith != null -> "${choice.intent.label} you pick for ${choice.sharedWith.label}"
        choice.asks -> "${choice.intent.label} you pick each time"
        else -> "${choice.intent.label} you pick now"
    }
}

/**
 * The ways [app] can open, for an icon of a plain app or of one made with an intent: as usual, then each intent the
 * catalog has for it, twice over for one that takes an input, picked now or each time, and a third time, picked with
 * its [partner]'s prompt, when [app] is one of a pair whose other app asks for the same kind of input with a prompt of
 * its own. None for an app with no intents.
 */
fun openChoices(app: AppEntry, partner: AppEntry? = null): List<OpenChoice> {
    val base = app.intentBase ?: return emptyList()
    val intents = IntentCatalog.forPackage(base.packageName)
    if (intents.isEmpty()) return emptyList()
    val partnerAsks = partner?.ownPromptFor
    return listOf(OpenChoice.Usual) + intents.flatMap { intent ->
        when (intent.input) {
            null -> listOf(OpenChoice.Intent(intent, asks = false))
            else -> listOfNotNull(
                OpenChoice.Intent(intent, asks = false),
                OpenChoice.Intent(intent, asks = true),
                partner?.intentBase?.takeIf { partnerAsks == intent.input }?.let { OpenChoice.Intent(intent, asks = true, sharedWith = it) },
            )
        }
    }
}

/**
 * What [app], beside [partner] in a pair if it is one of one, opens as now, of [openChoices]: null for one that saved an
 * input, as picking it again asks anew.
 */
fun currentChoice(app: AppEntry, partner: AppEntry? = null): OpenChoice? = when (val kind = app.kind) {
    is EntryKind.Intent -> when (kind.source) {
        is InputSource.Saved -> null
        InputSource.Shared -> OpenChoice.Intent(kind.intent, asks = true, sharedWith = partner?.takeIf { it.ownPromptFor == kind.intent.input }?.intentBase)
        else -> OpenChoice.Intent(kind.intent, asks = kind.source == InputSource.Ask)
    }
    else -> OpenChoice.Usual
}

/** The app of the user's own an icon opens, with an intent or without: what can be opened with one. */
val AppEntry.intentBase: AppEntry? get() = ownApp?.takeUnless { it.isWork }

/**
 * The icon that opens [app] straight to [intent], with its input from [source], or null when the intent is not [app]'s, or
 * [source] does not fit what it takes: none for an intent with no input. Named after the app and what the intent does,
 * and the saved input if any.
 */
fun intentEntryOf(app: AppEntry, intent: AppIntent, source: InputSource?): AppEntry? {
    if (app.intentBase != app || app.packageName != intent.packageName) return null
    val fits = when (source) {
        null -> intent.input == null
        InputSource.Ask, InputSource.Shared -> intent.input != null
        is InputSource.Saved -> source.value.kind == intent.input
    }
    if (!fits) return null
    return app.copy(
        label = if (source is InputSource.Saved) intent.savedLabel(app.label, source.value) else "${app.label} ${intent.name}",
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
                choice.sharedWith != null -> InputSource.Shared
                choice.asks -> InputSource.Ask
                else -> InputSource.Saved(saved ?: return null)
            }
            intentEntryOf(base, choice.intent, source)
        }
    }
}

/** A prompt for an input of [kind] that opening an icon shows, for its [apps]: both of a pair when one shares the other's. */
data class InputQuestion(val kind: InputKind, val apps: List<AppEntry>) {
    /** Who it asks for, by their apps' names: "Uber and Lyft". */
    val forWhom: String get() = apps.mapNotNull { it.ownApp?.label }.joinToString(" and ")
}

/** The prompts opening [this] shows, one per app that asks for an input, except one that shares its partner's. */
val AppEntry.asks: List<InputQuestion>
    get() {
        val asking = opens.filter { it.asksFor != null }
        val joining = asking.associateWith { it.sharedPrompt(asking) }
        return asking.filter { joining[it] == null }.mapNotNull { owner ->
            owner.asksFor?.let { InputQuestion(it, listOf(owner) + asking.filter { app -> joining[app] == owner }) }
        }
    }

/**
 * [this] with each of its apps that [asks] for an input opened with the answer from [answers] instead, so it opens
 * straight away; null if an answer is missing.
 */
fun AppEntry.answered(answers: Map<InputQuestion, InputValue>): AppEntry? {
    fun answer(app: AppEntry): AppEntry? {
        val kind = app.kind as? EntryKind.Intent ?: return app
        if (app.asksFor == null) return app
        val value = answers.entries.firstOrNull { app in it.key.apps }?.value ?: return null
        return intentEntryOf(kind.app, kind.intent, InputSource.Saved(value))
    }
    return when (val kind = kind) {
        is EntryKind.AppPair -> pairOf(answer(kind.first) ?: return null, answer(kind.second) ?: return null)
        else -> answer(this)
    }
}

// The input this asks for as it opens, with a prompt of its own or its partner's.
private val AppEntry.asksFor: InputKind?
    get() = (kind as? EntryKind.Intent)?.takeIf { it.source == InputSource.Ask || it.source == InputSource.Shared }?.intent?.input

// The input this asks for with a prompt of its own, which the other app of its pair may share.
private val AppEntry.ownPromptFor: InputKind?
    get() = (kind as? EntryKind.Intent)?.takeIf { it.source == InputSource.Ask }?.intent?.input

// The app among the [asking] ones of its pair whose prompt this shares, if it shares one and that app asks for the same.
private fun AppEntry.sharedPrompt(asking: List<AppEntry>): AppEntry? =
    if ((kind as? EntryKind.Intent)?.source != InputSource.Shared) null
    else asking.firstOrNull { it != this && it.ownPromptFor == asksFor }

/** What opens [this] intent's app straight to it, or null while it still [asks] for its input. */
fun EntryKind.Intent.request(): IntentRequest? = when (source) {
    null -> intent.request(null)
    InputSource.Ask, InputSource.Shared -> null
    is InputSource.Saved -> intent.request(source.value)
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
        InputSource.Shared -> SHARED
        is InputSource.Saved -> SAVED + source.value.encode()
    }
    return INTENT + listOf(intentId, sourceText, appKey).joinToString(INTENT) { URLEncoder.encode(it, "UTF-8") }
}

/** The icon keyed [key] by [intentKey], given [app] for its app's key, or null if it is not one or no longer makes sense. */
internal fun intentEntryFor(key: String, app: (String) -> AppEntry?): AppEntry? {
    if (!key.startsWith(INTENT)) return null
    val parts = key.substring(INTENT.length).split(INTENT)
    if (parts.size != 3) return null
    val (id, sourceText, appKey) = parts.map { URLDecoder.decode(it, "UTF-8") }
    val intent = IntentCatalog.byId(id) ?: return null
    val source = when {
        sourceText == NO_INPUT -> null
        sourceText == ASK -> InputSource.Ask
        sourceText == SHARED -> InputSource.Shared
        sourceText.startsWith(SAVED) -> InputSource.Saved(intent.input?.decode(sourceText.removePrefix(SAVED)) ?: return null)
        else -> return null
    }
    return intentEntryOf(app(appKey) ?: return null, intent, source)
}

private const val INTENT = "!"
private const val NO_INPUT = "none"
private const val ASK = "ask"
private const val SHARED = "shared"
private const val SAVED = "saved:"
