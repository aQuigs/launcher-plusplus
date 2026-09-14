package com.aquigs.launcherplusplus.domain

/** The categories a collection card can be, in the order the picker offers them. */
enum class AppCategory {
    Business, Communication, Entertainment, Games, Kids, LifeStyle, Media, Music, Personalisation, Photos, Productivity,
    Shopping, Social, Tools, Transport, Video,
}

/**
 * What a collection card shows. The built-in kinds work their apps out from the system; a category's apps are the
 * user's. [name] is how a kind is stored, and how a card is told from the others.
 */
sealed interface CollectionKind {
    val name: String

    data object NewApps : CollectionKind {
        override val name = "NewApps"
    }

    data object MostUsed : CollectionKind {
        override val name = "MostUsed"
    }

    data class Category(val category: AppCategory) : CollectionKind {
        override val name: String get() = category.name
    }

    companion object {
        /** Every kind, in the picker's order: the categories, then the built-in two. */
        val all: List<CollectionKind> = AppCategory.entries.map(::Category) + listOf(NewApps, MostUsed)

        fun named(name: String): CollectionKind? = all.find { it.name == name }
    }
}

/** One card: its [kind], the apps a category keeps (none for a built-in kind), and whether it shows them all with labels. */
data class CollectionCard(val kind: CollectionKind, val apps: Favourites = Favourites(), val expanded: Boolean = false)

/** The cards on the collections page, top to bottom. A page never touched holds the two built-in cards, as Arc's does. */
data class CollectionsPage(
    val cards: List<CollectionCard> = listOf(CollectionCard(CollectionKind.NewApps), CollectionCard(CollectionKind.MostUsed)),
) {
    operator fun contains(kind: CollectionKind): Boolean = cards.any { it.kind == kind }

    fun card(kind: CollectionKind): CollectionCard? = cards.find { it.kind == kind }

    /** Adds a card of [kind] at the bottom, holding [apps], unless the page has one. */
    fun add(kind: CollectionKind, apps: Favourites = Favourites()): CollectionsPage =
        if (kind in this) this else CollectionsPage(cards + CollectionCard(kind, apps))

    /** Drops the card of [kind], and a category's apps with it. */
    fun remove(kind: CollectionKind): CollectionsPage = CollectionsPage(cards.filterNot { it.kind == kind })

    /** Adds [app] at the end of the card of [kind], unless it is already there or there is no such card. */
    fun addApp(kind: CollectionKind, app: AppEntry): CollectionsPage = update(kind) { copy(apps = apps.add(app)) }

    fun removeApp(kind: CollectionKind, app: AppEntry): CollectionsPage = update(kind) { copy(apps = apps.remove(app)) }

    fun toggleExpanded(kind: CollectionKind): CollectionsPage = update(kind) { copy(expanded = !expanded) }

    /** Moves the card at [from] to [to]; a position off the page changes nothing. */
    fun move(from: Int, to: Int): CollectionsPage {
        if (from == to || from !in cards.indices || to !in cards.indices) return this
        return CollectionsPage(cards.toMutableList().apply { add(to, removeAt(from)) })
    }

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
 * The category [AppEntry] belongs in when a category card is made: the one its package declares, else the one a telling
 * word in its package name points to. The name is read from its last part back, so "youtube.music" is music, not video.
 */
val AppEntry.suggestedCategory: AppCategory?
    get() = category ?: packageName.split('.').asReversed().firstNotNullOfOrNull { part ->
        val lower = part.lowercase()
        categoryWords.entries.firstOrNull { (word, _) -> if (word.length < 4) lower == word else word in lower }?.value
    }

/**
 * The installed apps a new card for [category] starts with, in the order given. Arc pre-fills its cards from a list of
 * its own that cannot be read, so this is our guess; from here on the list is the user's.
 */
fun seedCategory(category: AppCategory, apps: List<AppEntry>): Favourites =
    Favourites(apps.filter { it.suggestedCategory == category }.map { it.key })

/** The page as text, one line per card: its kind, 1 or 0 for expanded, then a category's keys, all tab-separated. */
fun CollectionsPage.encode(): String = cards.joinToString(LINE) { card ->
    (listOf(card.kind.name, if (card.expanded) "1" else "0") + card.apps.keys).joinToString(FIELD)
}

/**
 * A line whose kind is unknown or whose expanded flag is not 0 or 1 is skipped, and so is a second line for a kind, so a
 * damaged file loses that card and keeps the rest. Empty text is an empty page: the defaults are for a page never stored.
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
            val keys = if (kind is CollectionKind.Category) fields.drop(2).filter(String::isNotEmpty) else emptyList()
            CollectionCard(kind, Favourites(keys), expanded)
        }
        .distinctBy { it.kind },
)
