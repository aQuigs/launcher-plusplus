package com.sqftware.orbitlauncher.domain

/**
 * The apps on every ring page, by page id, and in the dock, which shows on the home page alone. A page with nothing
 * stored has an empty ring.
 */
data class RingPages(val rings: Map<String, Ring> = emptyMap(), val dock: Ring = Ring()) {
    /** What the ring page [page] works on: its own ring, and the dock every ring page shares. */
    fun on(page: String): HomeApps = HomeApps(rings[page] ?: Ring(), dock)

    /** These pages with [page]'s ring and the dock as [home] has them. */
    fun with(page: String, home: HomeApps): RingPages = RingPages(rings + (page to home.ring), home.dock)

    /**
     * Moves [app] off [place] on ring page [from] to the end of [to] on ring page [onto], where it is once. A folder it
     * leaves showing none of the [shown] apps goes.
     */
    fun move(app: AppEntry, from: String, place: HomePlace, onto: String, to: HomePlace, shown: Set<String>): RingPages {
        val left = with(from, on(from).remove(place, app, shown))
        return left.with(onto, left.on(onto).add(to, app))
    }

    /** These pages with only the rings of the pages in [ids]. */
    fun keepingPages(ids: Set<String>): RingPages = copy(rings = rings.filterKeys { it in ids })

    /** The key of every app kept on any ring page or in the dock. */
    val keys: Set<String> get() = HomeApps(Ring(rings.values.flatMap { it.slots }), dock).keys

    /**
     * Each page's folders given planets of their own, as [HomeApps.withPlanetsKept] does: the home page's among its
     * ring and the dock, and another page's among its ring alone, which is all it shows.
     */
    fun withPlanetsKept(): RingPages {
        val home = on(LauncherPage.Home.id).withPlanetsKept()
        val kept = rings.mapValues { (page, ring) -> if (page == LauncherPage.Home.id) home.ring else HomeApps(ring).withPlanetsKept().ring }
        return RingPages(kept, home.dock)
    }

    /**
     * The pins to set so that no shortcut stays pinned once it is on no page. [pinned] is the ids pinned now, by package;
     * the result holds, for each package with a pin that has gone, the ids of its pins still here.
     */
    fun keptPins(pinned: Map<String, List<String>>): Map<String, List<String>> {
        val here = keys
        return pinned.mapValues { (packageName, ids) -> ids.filter { shortcutKey(packageName, it) in here } }
            .filter { (packageName, kept) -> kept.size < pinned.getValue(packageName).size }
    }
}
