package com.sqftware.orbitlauncher.domain

/**
 * Something the home screen can hold and start, as its [kind] says: a launchable activity, a shortcut an app pinned to the
 * home screen, which belongs to its app's [activityName], or a pair of apps. [canUninstall] is false for an app built into
 * the system. [installedAt] is when its package was first installed, in epoch milliseconds, and [category] the kind of app
 * its package says it is, if it says. A pair belongs to no one package, so its package and activity names are empty; make
 * one with [pairOf].
 */
data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
    val canUninstall: Boolean = true,
    val installedAt: Long = 0L,
    val category: AppCategory? = null,
    val kind: EntryKind = EntryKind.App,
) {
    /** Identifies one activity, shortcut or pair whatever its label, so it survives relabelling and reloads of the app list. */
    val key: String = when (kind) {
        EntryKind.App -> activityKey(packageName, activityName)
        is EntryKind.Shortcut -> shortcutKey(packageName, kind.id)
        is EntryKind.AppPair -> pairKey(kind.first.key, kind.second.key)
    }

    /** Whether it is a plain app, one launchable activity, rather than a shortcut or a pair. */
    val isApp: Boolean get() = kind == EntryKind.App

    /** What starting it opens: a pair's two apps, else itself. */
    val opens: List<AppEntry> get() = if (kind is EntryKind.AppPair) listOf(kind.first, kind.second) else listOf(this)

    /**
     * The packages whose unread notifications its badge counts: its own, a pair's two, and none for a shortcut, whose own
     * are not told apart from its app's. Made once, as every icon reads it.
     */
    val badgePackages: List<String> = when (kind) {
        EntryKind.App -> listOf(packageName)
        is EntryKind.Shortcut -> emptyList()
        is EntryKind.AppPair -> listOf(kind.first.packageName, kind.second.packageName)
    }

    /** The label as a search sees it: lower case, without accents. Made once here rather than on every keystroke. */
    val searchableLabel: String = label.unaccented().lowercase()
}

/** What an [AppEntry] starts. */
sealed interface EntryKind {
    /** Its own activity. */
    data object App : EntryKind

    /** The shortcut [id] its app pinned to the home screen. */
    data class Shortcut(val id: String) : EntryKind

    /** Two plain apps side by side in split screen, [first] on top. */
    data class AppPair(val first: AppEntry, val second: AppEntry) : EntryKind
}

fun activityKey(packageName: String, activityName: String) = "$packageName/$activityName"

/**
 * The key of [packageName]'s shortcut [id]. Keys are stored, so it must never be an activity's: a package name holds
 * neither '/' nor '#', so the first of them in a key ends the package name, and says which kind of key it is.
 */
fun shortcutKey(packageName: String, id: String) = "$packageName#$id"

/**
 * The key of the pair of the apps keyed [first] and [second]. Keys are stored, so it must be neither an activity's nor a
 * shortcut's, and must split back into the two ([pairKeys]). It starts with '|', where those start with a package name,
 * which never holds one. A class name holds none either (a Java identifier, and dex allows no '|' in one), so the next
 * '|' ends [first]. Neither key holds a tab or a line break, so the pair's is storable too.
 */
fun pairKey(first: String, second: String) = "$PAIR$first$PAIR$second"

/** The keys of the two apps of the pair keyed [key], or null for a key that is not a pair's. */
fun pairKeys(key: String): Pair<String, String>? {
    if (!key.startsWith(PAIR)) return null
    val parts = key.substring(PAIR.length).split(PAIR, limit = 2)
    return parts.takeIf { it.size == 2 && it.all(String::isNotEmpty) }?.let { it[0] to it[1] }
}

private const val PAIR = "|"

/** [apps] by key, and a pair by its key while both its apps are among them, so a pair resolves wherever a key does. */
class AppsByKey(apps: List<AppEntry>) {
    private val byKey = apps.associateBy { it.key }

    operator fun get(key: String): AppEntry? = byKey[key] ?: pairKeys(key)?.let { (first, second) ->
        pairOf(byKey[first] ?: return null, byKey[second] ?: return null)
    }
}

/**
 * The keys of the apps whose label, as a search sees it, an app from another package also goes by, like Google's and
 * Microsoft's Authenticator, so a list can tell them apart.
 */
fun List<AppEntry>.keysWithSharedLabels(): Set<String> =
    groupBy { it.searchableLabel.trim() }.values
        .filter { entries -> entries.distinctBy(AppEntry::packageName).size > 1 }
        .flatten()
        .mapTo(HashSet(), AppEntry::key)

fun List<AppEntry>.sortedByLabel(): List<AppEntry> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, AppEntry::label).thenBy(AppEntry::packageName))
