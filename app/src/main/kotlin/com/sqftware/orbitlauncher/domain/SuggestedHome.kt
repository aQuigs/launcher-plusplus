package com.sqftware.orbitlauncher.domain

/** A job most phones have an app for, which the system opens a default app for. */
enum class AppRole { Phone, Messages, Browser, Camera, Email, Maps, Photos, Calendar, Store, Clock }

/** The [AppEntry.key] of each role's default app, and the packages a guess at favourites [skips]: launchers and settings. */
data class DefaultApps(val byRole: Map<AppRole, String> = emptyMap(), val skips: Set<String> = emptySet())

/** A first home screen and collections page. */
data class Setup(val home: HomeApps, val collections: CollectionsPage)

private val DOCK_ROLES = listOf(AppRole.Phone, AppRole.Messages, AppRole.Browser, AppRole.Camera)
private val RING_ROLES = listOf(AppRole.Email, AppRole.Maps, AppRole.Photos, AppRole.Calendar, AppRole.Store, AppRole.Clock)
private const val SUGGESTED_RING_APPS = 6
private const val SUGGESTED_CARDS = 3
private const val SUGGESTED_CARD_MIN_APPS = 3

/**
 * A guess at the [home] screen and [collections] of someone who has set up neither, as Arc starts with one; null once
 * [home] holds anything, which is the user's.
 */
fun suggestSetup(home: HomeApps, collections: CollectionsPage, apps: List<AppEntry>, defaults: DefaultApps, time: ForegroundTime?): Setup? {
    if (!home.isEmpty) return null

    val candidates = apps.filter { it.shortcutId == null && it.packageName !in defaults.skips }
    return Setup(suggestHome(candidates, defaults, time), suggestCollections(collections, candidates))
}

// Calling and browsing sit at hand in the dock; the ring leads with what the user opens most.
private fun suggestHome(apps: List<AppEntry>, defaults: DefaultApps, time: ForegroundTime?): HomeApps {
    val byKey = apps.associateBy { it.key }
    fun holders(roles: List<AppRole>) = roles.mapNotNull { defaults.byRole[it]?.let(byKey::get) }

    val dock = holders(DOCK_ROLES).distinct()
    val used = time?.let { mostUsed(apps, it, limit = apps.size) }.orEmpty()
    val ring = (used + holders(RING_ROLES)).distinct().filterNot { it in dock }.take(SUGGESTED_RING_APPS)

    return HomeApps(ring = Ring(ring.map { RingSlot.App(it.key) }), dock = Ring(dock.map { RingSlot.App(it.key) }))
}

// A hand-picked card is the user's work, so a page with one is left as it is. A thin category makes a card not worth a
// place.
private fun suggestCollections(page: CollectionsPage, apps: List<AppEntry>): CollectionsPage {
    if (page.cards.any { it.kind is CollectionKind.HandPicked }) return page

    return apps.groupBy { it.suggestedCategory }
        .mapNotNull { (category, members) -> category?.takeIf { members.size >= SUGGESTED_CARD_MIN_APPS }?.let { it to members } }
        .sortedWith(compareByDescending<Pair<AppCategory, List<AppEntry>>> { it.second.size }.thenBy { it.first })
        .take(SUGGESTED_CARDS)
        .fold(page) { suggested, (category, members) -> suggested.add(CollectionKind.Category(category), Favourites(members.map { it.key })) }
}
