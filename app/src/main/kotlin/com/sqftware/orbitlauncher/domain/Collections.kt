package com.sqftware.orbitlauncher.domain

/** The categories a collection card can be, in the order the picker offers them. */
enum class AppCategory {
    Business, Communication, Entertainment, Games, Kids, LifeStyle, Media, Music, Personalisation, Photos, Productivity,
    Shopping, Social, Tools, Transport, Video,
}

/**
 * What a collection card shows. The built-in kinds work their apps out from the system; the apps of a category or a
 * custom collection are the user's. [name] is how a kind is stored, and how a card is told from the others.
 */
sealed interface CollectionKind {
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

/** One card: its [kind], the apps it keeps if [CollectionKind.HandPicked], and whether it shows them all with labels. */
data class CollectionCard(val kind: CollectionKind, val apps: Favourites = Favourites(), val expanded: Boolean = false)

/** The cards on the collections page, top to bottom. A page never touched holds the two built-in cards, as Arc's does. */
data class CollectionsPage(
    val cards: List<CollectionCard> = listOf(CollectionCard(CollectionKind.NewApps), CollectionCard(CollectionKind.MostUsed)),
) {
    operator fun contains(kind: CollectionKind): Boolean = cards.any { it.kind == kind }

    fun card(kind: CollectionKind): CollectionCard? = cards.find { it.kind == kind }

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
    fun add(kind: CollectionKind, apps: Favourites = Favourites()): CollectionsPage =
        if (kind in this) this else CollectionsPage(cards + CollectionCard(kind, apps))

    /** Drops the card of [kind], and the apps it kept with it. */
    fun remove(kind: CollectionKind): CollectionsPage = CollectionsPage(cards.filterNot { it.kind == kind })

    /** Adds [app] at the end of the card of [kind], unless it is already there or there is no such card. */
    fun addApp(kind: CollectionKind, app: AppEntry): CollectionsPage = update(kind) { copy(apps = apps.add(app)) }

    fun removeApp(kind: CollectionKind, app: AppEntry): CollectionsPage = update(kind) { copy(apps = apps.remove(app)) }

    /** Moves [app] to [target]'s place on the card of [kind] as [mode] says. */
    fun moveApp(kind: CollectionKind, app: AppEntry, target: AppEntry, mode: ReorderMode): CollectionsPage =
        update(kind) { copy(apps = apps.move(app, target, mode)) }

    fun toggleExpanded(kind: CollectionKind): CollectionsPage = update(kind) { copy(expanded = !expanded) }

    /** Moves the card at [from] to [to]; a position off the page changes nothing. */
    fun move(from: Int, to: Int): CollectionsPage = CollectionsPage(cards.reordered(from, to, ReorderMode.Insert))

    private fun update(kind: CollectionKind, change: CollectionCard.() -> CollectionCard) =
        CollectionsPage(cards.map { if (it.kind == kind) it.change() else it })
}

/** How many apps a built-in card lists. */
const val BUILT_IN_CARD_APPS = 10

/** The most recently installed apps, newest first, one per package. */
fun newApps(apps: List<AppEntry>, limit: Int = BUILT_IN_CARD_APPS): List<AppEntry> =
    apps.sortedByDescending { it.installedAt }.distinctBy { it.packageName }.take(limit)

/** How long each package has been in the foreground lately, in milliseconds. A package never in front is absent. */
data class ForegroundTime(val byPackage: Map<String, Long> = emptyMap())

/** The apps in front the longest by [time], the longest first, one per package; an app never in front is left out. */
fun mostUsed(apps: List<AppEntry>, time: ForegroundTime, limit: Int = BUILT_IN_CARD_APPS): List<AppEntry> =
    apps.distinctBy { it.packageName }
        .filter { (time.byPackage[it.packageName] ?: 0L) > 0L }
        .sortedByDescending { time.byPackage[it.packageName] }
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

/** The page as text, one line per card: its kind, 1 or 0 for expanded, then the keys it keeps, all tab-separated. */
fun CollectionsPage.encode(): String = cards.joinToString(LINE) { card ->
    (listOf(card.kind.name, if (card.expanded) "1" else "0") + card.apps.keys).joinToString(FIELD)
}

/**
 * A line whose kind is unknown (a custom name included that cleans to nothing or to a built-in's) or whose expanded flag
 * is not 0 or 1 is skipped, and so is a second line for a kind, so a damaged file loses that card and keeps the rest.
 * Empty text is an empty page: the defaults are for a page never stored.
 */
fun decodeCollectionsPage(text: String): CollectionsPage = CollectionsPage(
    text.nonEmptyLines()
        .mapNotNull { line ->
            val fields = line.split(FIELD)
            val kind = CollectionKind.named(fields[0]) ?: return@mapNotNull null
            val expanded = when (fields.getOrNull(1)) {
                "1" -> true
                "0" -> false
                else -> return@mapNotNull null
            }
            val keys = if (kind is CollectionKind.HandPicked) fields.drop(2).filter(String::isNotEmpty) else emptyList()
            CollectionCard(kind, Favourites(keys), expanded)
        }
        .distinctBy { it.kind },
)
