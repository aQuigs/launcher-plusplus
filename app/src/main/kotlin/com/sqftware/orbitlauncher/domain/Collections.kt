package com.sqftware.orbitlauncher.domain

import java.io.Serializable

/** The categories a collection card can be, in the order the picker offers them. */
enum class AppCategory {
    Business, Communication, Entertainment, Games, Kids, LifeStyle, Media, Music, Personalisation, Photos, Productivity,
    Shopping, Social, Tools, Transport, Video,
}

/**
 * What a collection card shows. The built-in kinds work their apps out from the system; the apps of a category or a
 * custom collection are the user's. [name] is how a kind is stored, and how a card is told from the others. Serializable
 * so the screen can save a pick for a card.
 */
sealed interface CollectionKind : Serializable {
    val name: String

    data object NewApps : CollectionKind {
        override val name = "NewApps"
    }

    data object MostUsed : CollectionKind {
        override val name = "MostUsed"
    }

    /** A kind whose card keeps apps the user picked, rather than ones the system works out. */
    sealed interface HandPicked : CollectionKind

    data class Category(val category: AppCategory) : HandPicked {
        override val name: String get() = category.name
    }

    /** A collection the user made and named, Arc's [CREATE_YOUR_OWN]. [CollectionsPage.custom] vets the [label]. */
    data class Custom(val label: String) : HandPicked {
        // Prefixed, so a label cannot pass for a built-in kind's stored name.
        override val name: String = CUSTOM_PREFIX + label
    }

    companion object {
        /** Every built-in kind, in the picker's order: the categories, then New Apps and Most Used. */
        val all: List<CollectionKind> = AppCategory.entries.map(::Category) + listOf(NewApps, MostUsed)

        /** The kind stored as [name]; a custom one only if its label is still one [CollectionsPage.custom] would take. */
        fun named(name: String): CollectionKind? = all.find { it.name == name }
            ?: if (name.startsWith(CUSTOM_PREFIX)) customNamed(name.removePrefix(CUSTOM_PREFIX)) else null
    }
}

private const val CUSTOM_PREFIX = "Custom:"

/** What the picker's tile for making a custom collection says; no collection may take it as a name. */
const val CREATE_YOUR_OWN = "Create Your Own"

/** The longest name a custom collection keeps. */
const val MAX_COLLECTION_NAME = 30

/**
 * [raw] as a custom collection's name: every run of whitespace (a tab or line break, which would split the stored text,
 * or a no-break space) made one space, invisible formatting and control characters dropped, trimmed, and cut to
 * [MAX_COLLECTION_NAME].
 */
private fun cleanName(raw: String): String {
    val spaced = buildString {
        raw.forEach { c ->
            when {
                c.isWhitespace() -> append(' ')
                c.category != CharCategory.FORMAT && c.category != CharCategory.CONTROL -> append(c)
            }
        }
    }
    val cut = spaced.split(' ').filter(String::isNotEmpty).joinToString(" ").take(MAX_COLLECTION_NAME)
    return (if (cut.lastOrNull()?.isHighSurrogate() == true) cut.dropLast(1) else cut).trimEnd()
}

// Case and spaces aside, so "lifestyle" and "Most Used" are taken as well as "Life Style" and "Most Used Apps".
private fun nameKey(name: String) = name.lowercase().filterNot { it == ' ' }

private val reservedNames: Set<String> =
    (CollectionKind.all.flatMap { listOf(it.name, it.title) } + CREATE_YOUR_OWN).map(::nameKey).toSet()

private fun customNamed(raw: String): CollectionKind.Custom? =
    cleanName(raw).takeIf { it.isNotEmpty() && nameKey(it) !in reservedNames }?.let(CollectionKind::Custom)

val AppCategory.label: String
    get() = if (this == AppCategory.LifeStyle) "Life Style" else name

val CollectionKind.title: String
    get() = when (this) {
        CollectionKind.NewApps -> "New Apps"
        CollectionKind.MostUsed -> "Most Used Apps"
        is CollectionKind.Category -> category.label
        is CollectionKind.Custom -> label
    }

/** How many apps a card shows in a row. */
const val CARD_ROW_APPS = 5

/** When a card names its apps underneath. */
enum class AppNames { Never, Expanded, Always }

/** How a card lays out its apps: [rows] of them while compact, at most [limit] if it works them out, named as [names] says. */
data class CardLook(val rows: Int = 1, val limit: Int = BUILT_IN_CARD_APPS, val names: AppNames = AppNames.Expanded) {
    /** How many apps it shows while compact. */
    val compactApps: Int get() = rows * CARD_ROW_APPS

    fun namesShown(expanded: Boolean): Boolean = names == AppNames.Always || names == AppNames.Expanded && expanded
}

/** One part of a [CardLook] a card can hold its own value of: the values it takes, and how it is read and written. */
sealed class CardSetting<T : Any>(val choices: List<T>, private val get: (CardLook) -> T, private val put: (CardLook, T) -> CardLook) {
    fun of(look: CardLook): T = get(look)

    /** Whether a card of [kind] has any use for it. */
    open fun appliesTo(kind: CollectionKind) = true

    /** The choice [text] names, as [toString] writes it, if it names one. */
    internal fun parse(text: String): T? = choices.find { it.toString() == text }

    // Only ever handed a value this setting gave, as a card's own settings hold each under its setting.
    @Suppress("UNCHECKED_CAST")
    internal fun with(look: CardLook, value: Any): CardLook = put(look, value as T)

    data object Rows : CardSetting<Int>((1..4).toList(), CardLook::rows, { look, it -> look.copy(rows = it) })

    /** Only for a card that works its apps out: one the user fills holds what they put there. */
    data object Limit : CardSetting<Int>((5..30 step 5).toList(), CardLook::limit, { look, it -> look.copy(limit = it) }) {
        override fun appliesTo(kind: CollectionKind) = kind !is CollectionKind.HandPicked
    }

    data object Names : CardSetting<AppNames>(AppNames.entries, CardLook::names, { look, it -> look.copy(names = it) })
}

/** Every [CardSetting], in the order they are stored and offered. */
val CARD_SETTINGS: List<CardSetting<*>> = listOf(CardSetting.Rows, CardSetting.Limit, CardSetting.Names)

/**
 * One card: its [kind], the apps it keeps if [CollectionKind.HandPicked], whether it shows them all, and the settings it
 * holds its [own] value of rather than taking the page's default, each value under its setting.
 */
data class CollectionCard(
    val kind: CollectionKind,
    val apps: Favourites = Favourites(),
    val expanded: Boolean = false,
    val own: Map<CardSetting<*>, Any> = emptyMap(),
)

/**
 * The cards on the collections page, top to bottom, and the [defaults] every card's look starts from. A page never
 * touched holds the two built-in cards, as Arc's does.
 */
data class CollectionsPage(
    val cards: List<CollectionCard> = listOf(CollectionCard(CollectionKind.NewApps), CollectionCard(CollectionKind.MostUsed)),
    val defaults: CardLook = CardLook(),
) {
    operator fun contains(kind: CollectionKind): Boolean = cards.any { it.kind == kind }

    fun card(kind: CollectionKind): CollectionCard? = cards.find { it.kind == kind }

    fun look(card: CollectionCard): CardLook = card.own.entries.fold(defaults) { look, (setting, value) -> setting.with(look, value) }

    /**
     * The custom collection [label] names once cleaned up, or null when it names none: blank, or taken, whatever the case
     * or spacing, by a built-in kind, by [CREATE_YOUR_OWN], or by a custom card on the page.
     */
    fun custom(label: String): CollectionKind.Custom? =
        customNamed(label)?.takeIf { new -> cards.none { it.kind is CollectionKind.Custom && nameKey(it.kind.label) == nameKey(new.label) } }

    /**
     * The custom cards a visit to the picker lists, given those it [listed] so far: each of those as the page now has it,
     * or as it last was if it has been taken off, then any the page has that were not listed. A card taken off keeps its
     * place and its apps, so the tiles do not move under the finger and a second tap puts it back whole.
     */
    fun customTiles(listed: List<CollectionCard>): List<CollectionCard> =
        listed.map { card(it.kind) ?: it } + cards.filter { it.kind is CollectionKind.Custom && listed.none { l -> l.kind == it.kind } }

    /** Adds a card of [kind] at the bottom, holding [apps], unless the page has one. */
    fun add(kind: CollectionKind, apps: Favourites = Favourites()): CollectionsPage = add(CollectionCard(kind, apps))

    /** Adds [card] at the bottom as it is, unless the page has one of its kind. */
    fun add(card: CollectionCard): CollectionsPage = if (card.kind in this) this else copy(cards = cards + card)

    /** Drops the card of [kind], and the apps it kept with it. */
    fun remove(kind: CollectionKind): CollectionsPage = copy(cards = cards.filterNot { it.kind == kind })

    /** Adds [app] at the end of the card of [kind], unless it is already there or there is no such card. */
    fun addApp(kind: CollectionKind, app: AppEntry): CollectionsPage = update(kind) { copy(apps = apps.add(app)) }

    fun removeApp(kind: CollectionKind, app: AppEntry): CollectionsPage = update(kind) { copy(apps = apps.remove(app)) }

    /** Adds [app] at the end of the card of [kind], or takes it off if it is already there. */
    fun toggleApp(kind: CollectionKind, app: AppEntry): CollectionsPage = update(kind) { copy(apps = apps.toggle(app)) }

    /** Moves [app] to [target]'s place on the card of [kind] as [mode] says. */
    fun moveApp(kind: CollectionKind, app: AppEntry, target: AppEntry, mode: ReorderMode): CollectionsPage =
        update(kind) { copy(apps = apps.move(app, target, mode)) }

    fun toggleExpanded(kind: CollectionKind): CollectionsPage = update(kind) { copy(expanded = !expanded) }

    /**
     * Gives the card of [kind] its own [value] of [setting], unless the value is not one of its choices or the setting
     * does not apply to the card. One the same as the default's is kept too, so the card holds it when the default changes.
     */
    fun <T : Any> set(kind: CollectionKind, setting: CardSetting<T>, value: T): CollectionsPage =
        if (value !in setting.choices || !setting.appliesTo(kind)) this else update(kind) { copy(own = own + (setting to value)) }

    /** Has the card of [kind] take [setting] from the defaults again. */
    fun useDefault(kind: CollectionKind, setting: CardSetting<*>): CollectionsPage = update(kind) { copy(own = own - setting) }

    /** Makes [value] the default of [setting], which every card without its own then follows, unless it is not a choice. */
    fun <T : Any> setDefault(setting: CardSetting<T>, value: T): CollectionsPage =
        if (value !in setting.choices) this else copy(defaults = setting.with(defaults, value))

    /** Moves the card at [from] to [to]; a position off the page changes nothing. */
    fun move(from: Int, to: Int): CollectionsPage = copy(cards = cards.reordered(from, to, ReorderMode.Insert))

    private fun update(kind: CollectionKind, change: CollectionCard.() -> CollectionCard) =
        copy(cards = cards.map { if (it.kind == kind) it.change() else it })
}

/** How many apps a built-in card lists at least. */
const val BUILT_IN_CARD_APPS = 10

/** The most recently installed apps, newest first, one per package. */
fun newApps(apps: List<AppEntry>, limit: Int = BUILT_IN_CARD_APPS): List<AppEntry> =
    apps.sortedByDescending { it.installedAt }.distinctBy { it.packageName }.take(limit)

/** How long each package has been in the foreground lately, in milliseconds. A package never in front is absent. */
data class ForegroundTime(val byPackage: Map<String, Long> = emptyMap()) {
    /** How long [app]'s package has been in front, none if never. */
    fun of(app: AppEntry): Long = byPackage[app.packageName] ?: 0L
}

/** The apps in front the longest by [time], the longest first, one per package; an app never in front is left out. */
fun mostUsed(apps: List<AppEntry>, time: ForegroundTime, limit: Int = BUILT_IN_CARD_APPS): List<AppEntry> =
    apps.distinctBy { it.packageName }
        .filter { time.of(it) > 0L }
        .sortedByDescending(time::of)
        .take(limit)

// Popular apps whose names give nothing away, or point the wrong way, filed where the Play Store files them. They come
// before an app's declared category, as that knows only a few coarse kinds (WhatsApp and LinkedIn call themselves social)
// and none for business.
private val categoryPackages: Map<String, AppCategory> = mapOf(
    "com.Slack" to AppCategory.Business,
    "com.microsoft.teams" to AppCategory.Business,
    "us.zoom.videomeetings" to AppCategory.Business,
    "com.cisco.wx2.android" to AppCategory.Business,
    "com.linkedin.android" to AppCategory.Business,
    "com.indeed.android.jobsearch" to AppCategory.Business,
    "com.whatsapp" to AppCategory.Communication,
    "com.whatsapp.w4b" to AppCategory.Communication,
    "org.thoughtcrime.securesms" to AppCategory.Communication,
    "org.telegram.messenger" to AppCategory.Communication,
    "com.facebook.orca" to AppCategory.Communication,
    "com.discord" to AppCategory.Communication,
    "com.viber.voip" to AppCategory.Communication,
    "com.skype.raider" to AppCategory.Communication,
    "jp.naver.line.android" to AppCategory.Communication,
    "com.tencent.mm" to AppCategory.Communication,
    "com.google.android.apps.tachyon" to AppCategory.Communication,
)

// Words that give a package's category away, matched inside each part of its name: "deskclock" holds "clock". Only the
// obvious ones, since a wrong guess puts an app in a card the user did not expect; for the same reason a word of two or
// three letters must be a whole part, as "gm" is Gmail's last part but also sits inside "sigma".
private val categoryWords: Map<String, AppCategory> = mapOf(
    "clock" to AppCategory.Tools,
    "calculator" to AppCategory.Tools,
    "settings" to AppCategory.Tools,
    "files" to AppCategory.Tools,
    "chrome" to AppCategory.Tools,
    "camera" to AppCategory.Photos,
    "photos" to AppCategory.Photos,
    "gallery" to AppCategory.Photos,
    "messages" to AppCategory.Communication,
    "messaging" to AppCategory.Communication,
    "dialer" to AppCategory.Communication,
    "contacts" to AppCategory.Communication,
    "gm" to AppCategory.Communication,
    "meet" to AppCategory.Communication,
    "mail" to AppCategory.Communication,
    "maps" to AppCategory.Transport,
    "calendar" to AppCategory.Productivity,
    "docs" to AppCategory.Productivity,
    "drive" to AppCategory.Productivity,
    "keep" to AppCategory.Productivity,
    "sheets" to AppCategory.Productivity,
    "slides" to AppCategory.Productivity,
    "youtube" to AppCategory.Video,
    "music" to AppCategory.Music,
)

/**
 * The category [AppEntry] belongs in when a category card is made: a well-known app's, else the one its package declares,
 * else the one a telling word in its package name points to. The name is read from its last part back, so
 * "youtube.music" is music, not video.
 */
val AppEntry.suggestedCategory: AppCategory?
    get() = categoryPackages[packageName] ?: category ?: packageName.split('.').asReversed().firstNotNullOfOrNull { part ->
        val lower = part.lowercase()
        categoryWords.entries.firstOrNull { (word, _) -> if (word.length < 4) lower == word else word in lower }?.value
    }

/**
 * The installed apps a new card for [category] starts with, in the order given. Arc pre-fills its cards from a list of
 * its own that cannot be read, so this is our guess; from here on the list is the user's.
 */
fun seedCategory(category: AppCategory, apps: List<AppEntry>): Favourites =
    Favourites(apps.filter { it.suggestedCategory == category }.map { it.key })

/**
 * The page's cards as text, one line per card: its kind, then 1 or 0 for expanded followed by its own settings, comma
 * separated in [CARD_SETTINGS]'s order with nothing for a default, then the keys it keeps, all tab-separated. Defaults
 * at the end are left off, so a card with its own rows alone is written as before the other settings came.
 */
fun CollectionsPage.encode(): String = cards.joinToString(LINE) { card ->
    val own = CARD_SETTINGS.map { card.own[it]?.toString().orEmpty() }
    val look = (listOf(if (card.expanded) "1" else "0") + own).joinToString("$LOOK").trimEnd(LOOK)
    (listOf(card.kind.name, look) + card.apps.keys).joinToString(FIELD)
}

private const val LOOK = ','

/**
 * A line whose kind is unknown (a custom name included that cleans to nothing or to a built-in's) or whose expanded flag
 * is not 0 or 1 is skipped, and so is a second line for a kind, so a damaged file loses that card and keeps the rest.
 * A setting missing or not one of its choices is the default, and any after the known ones are ignored, so a card
 * written by a later version keeps its apps. Empty text is an empty page: the default cards are for a page never stored.
 */
fun decodeCollectionsPage(text: String): CollectionsPage = CollectionsPage(
    text.nonEmptyLines()
        .mapNotNull { line ->
            val fields = line.split(FIELD)
            val kind = CollectionKind.named(fields[0]) ?: return@mapNotNull null
            val look = fields.getOrNull(1)?.split(LOOK) ?: return@mapNotNull null
            val expanded = when (look[0]) {
                "1" -> true
                "0" -> false
                else -> return@mapNotNull null
            }
            val own = CARD_SETTINGS.zip(look.drop(1)).mapNotNull { (setting, text) -> setting.parse(text)?.let { setting to it } }.toMap()
            val keys = if (kind is CollectionKind.HandPicked) fields.drop(2).filter(String::isNotEmpty) else emptyList()
            CollectionCard(kind, Favourites(keys), expanded, own)
        }
        .distinctBy { it.kind },
)

/** The defaults as text: each setting in [CARD_SETTINGS]'s order, comma separated. */
fun CardLook.encode(): String = CARD_SETTINGS.joinToString("$LOOK") { it.of(this).toString() }

/** A setting missing or not one of its choices keeps its first default. */
fun decodeCardLook(text: String): CardLook =
    CARD_SETTINGS.zip(text.split(LOOK)).fold(CardLook()) { look, (setting, text) -> setting.parse(text)?.let { setting.with(look, it) } ?: look }
