package com.sqftware.orbitlauncher.domain

import java.io.Serializable

/** A place on the home screen where the user keeps apps. Serializable so the screen can save which one it is picking for. */
sealed interface HomePlace : Serializable {
    data object Ring : HomePlace

    data object Dock : HomePlace

    /** The folder in the ring's slot [index]. */
    data class Folder(val index: Int) : HomePlace
}

/** Where on the home screen an app dragged in from another place, or from the drawer, lands. */
sealed interface Landing {
    /**
     * [target]'s place on the ring, as [mode] says: inserted, the app goes in before it; swapped, the two trade places.
     * A folder never leaves the ring, so an app swapped onto one goes in before it instead. Without one, the ring's end.
     */
    data class OnRing(val target: RingItem?, val mode: ReorderMode) : Landing

    /** Into [target]: an app there and the one dropped on it become a folder, and a folder takes the app in at its end. */
    data class Into(val target: RingItem) : Landing

    /** [target]'s place in the dock as [mode] says, or the end of the dock without one. */
    data class InDock(val target: AppEntry?, val mode: ReorderMode) : Landing
}

/** The apps the user keeps on the home screen: on the ring round the emblem, and in the dock at the foot of the page. */
data class HomeApps(val ring: Ring = Ring(), val dock: Favourites = Favourites()) {
    /** The apps at [place]: those in slots of their own for the ring, and none for a folder that is not there. */
    operator fun get(place: HomePlace): Favourites = when (place) {
        HomePlace.Ring -> ring.apps
        HomePlace.Dock -> dock
        is HomePlace.Folder -> Favourites(ring.folder(place.index)?.keys.orEmpty())
    }

    /** Adds [app] to [place], unless it is already there. The other places are left as they are. */
    fun add(place: HomePlace, app: AppEntry): HomeApps = when (place) {
        HomePlace.Ring -> copy(ring = ring.add(app))
        HomePlace.Dock -> copy(dock = dock.add(app))
        is HomePlace.Folder -> copy(ring = ring.add(place.index, app))
    }

    /**
     * Adds [app] to [place], or takes it off if it is already there. The other places are left as they are, and so is a
     * folder left with one app or none, as a pick filling it may empty it on the way.
     */
    fun toggle(place: HomePlace, app: AppEntry): HomeApps = when (place) {
        HomePlace.Ring -> copy(ring = ring.toggle(app))
        HomePlace.Dock -> copy(dock = dock.toggle(app))
        is HomePlace.Folder -> copy(ring = ring.toggle(place.index, app))
    }

    /** Moves [app] to [target]'s place at [place] as [mode] says; on the ring, only apps in slots of their own move. */
    fun move(place: HomePlace, app: AppEntry, target: AppEntry, mode: ReorderMode): HomeApps = when (place) {
        HomePlace.Ring -> copy(ring = ring.move(RingItem.App(app), RingItem.App(target), mode))
        HomePlace.Dock -> copy(dock = dock.move(app, target, mode))
        is HomePlace.Folder -> copy(ring = ring.move(place.index, app, target, mode))
    }

    /** Takes [app] off [place]; a folder it leaves showing none of the [shown] apps goes too. */
    fun remove(place: HomePlace, app: AppEntry, shown: Set<String>): HomeApps =
        Draft(this).apply { put(place, app.key, GAP) }.build(shown)

    /**
     * Whether [app] from [from], another home place or the drawer without one, can land [to]: not in the place it is in,
     * nor into itself or a copy of itself, nor into or by the folder it left.
     */
    fun lands(app: AppEntry, from: HomePlace?, to: Landing): Boolean {
        val index = indexOf(to)
        return when (to) {
            is Landing.Into -> {
                val slot = ring.slots.getOrNull(index)
                slot != null && slot != RingSlot.App(app.key) && from != HomePlace.Folder(index)
            }
            is Landing.OnRing -> index >= 0 && from != HomePlace.Ring && from != HomePlace.Folder(index)
            is Landing.InDock -> index >= 0 && from != HomePlace.Dock
        }
    }

    /**
     * Moves [app] from [from] as [to] says, if it [lands] there. It is in the place it lands in once: a copy already there
     * gives way to it, and folding it into a folder that holds it only takes it off [from]. A folder it leaves showing none
     * of the [shown] apps goes.
     */
    fun move(app: AppEntry, from: HomePlace?, to: Landing, shown: Set<String>): HomeApps {
        if (!lands(app, from, to)) return this
        val index = indexOf(to)
        val draft = Draft(this)
        when (to) {
            is Landing.Into -> {
                draft.put(from, app.key, GAP)
                draft.slots[index] = when (val slot = ring.slots[index]) {
                    is RingSlot.App -> RingSlot.Folder(listOf(slot.key, app.key))
                    is RingSlot.Folder -> RingSlot.Folder(Favourites(slot.keys).add(app).keys)
                }
            }
            is Landing.OnRing -> draft.land(app.key, from, HomePlace.Ring, index, to.mode)
            is Landing.InDock -> draft.land(app.key, from, HomePlace.Dock, index, to.mode)
        }
        return draft.build(shown)
    }

    /** The ring slot or dock position [to] names, or -1 for a target that is not there. */
    private fun indexOf(to: Landing): Int = when (to) {
        is Landing.Into -> ring.slotOf(to.target)
        is Landing.OnRing -> to.target?.let(ring::slotOf) ?: ring.slots.size
        is Landing.InDock -> to.target?.let { dock.keys.indexOf(it.key) } ?: dock.keys.size
    }

    /**
     * The pins to set so that no shortcut stays pinned once it is nowhere on the home screen. [pinned] is the ids pinned
     * now, by package; the result holds, for each package with a pin that has gone, the ids of its pins still here.
     */
    fun keptPins(pinned: Map<String, List<String>>): Map<String, List<String>> {
        val here = ring.slots.flatMap { slot ->
            when (slot) {
                is RingSlot.App -> listOf(slot.key)
                is RingSlot.Folder -> slot.keys
            }
        }.toSet() + dock.keys
        return pinned.mapValues { (packageName, ids) -> ids.filter { shortcutKey(packageName, it) in here } }
            .filter { (packageName, kept) -> kept.size < pinned.getValue(packageName).size }
    }
}

/** A key no app has. It holds the place of an app that has left, so no slot or dock position shifts until an edit is done. */
private const val GAP = ""

/** [home] being edited in place. Every insertion comes last, so the positions found at the start still hold. */
private class Draft(home: HomeApps) {
    val slots = home.ring.slots.toMutableList()
    val dock = home.dock.keys.toMutableList()

    /** Puts [key] in [app]'s stead wherever [place] holds it. */
    fun put(place: HomePlace?, app: String, key: String) {
        when (place) {
            HomePlace.Ring -> slots.replaceAll { if (it == RingSlot.App(app)) RingSlot.App(key) else it }
            HomePlace.Dock -> dock.replaceAll { if (it == app) key else it }
            is HomePlace.Folder -> (slots.getOrNull(place.index) as? RingSlot.Folder)?.let { folder ->
                slots[place.index] = RingSlot.Folder(folder.keys.map { if (it == app) key else it })
            }
            null -> Unit
        }
    }

    /** Takes [app] off [place], leaving the app it [swapped] places with in its stead, once, or a gap. */
    fun leave(place: HomePlace?, app: String, swapped: String?) {
        if (swapped != null) put(place, swapped, GAP)
        put(place, app, swapped ?: GAP)
    }

    /**
     * Puts [app], leaving [from], at [index] of [place], the ring or the dock: in the stead of the app there if [mode]
     * swaps, else in before whatever is there, a folder included.
     */
    fun land(app: String, from: HomePlace?, place: HomePlace, index: Int, mode: ReorderMode) {
        val onRing = place == HomePlace.Ring
        val there = if (onRing) (slots.getOrNull(index) as? RingSlot.App)?.key else dock.getOrNull(index)
        val swapped = there?.takeIf { mode == ReorderMode.Swap }
        leave(from, app, swapped)
        put(place, app, GAP)
        when {
            onRing && swapped != null -> slots[index] = RingSlot.App(app)
            onRing -> slots.add(index, RingSlot.App(app))
            swapped != null -> dock[index] = app
            else -> dock.add(index, app)
        }
    }

    /**
     * The home apps with the gaps closed. A folder an app left that [showsNone][RingSlot.Folder.showsNone] of the [shown]
     * apps goes with them, the missing apps it holds too: its last app the user could see has left.
     */
    fun build(shown: Set<String>) = HomeApps(
        ring = Ring(
            slots.mapNotNull { slot ->
                when (slot) {
                    is RingSlot.App -> slot.takeIf { it.key != GAP }
                    is RingSlot.Folder -> RingSlot.Folder(slot.keys.filter { it != GAP }).takeUnless {
                        GAP in slot.keys && slot.showsNone(shown)
                    }
                }
            },
        ),
        dock = Favourites(dock.filter { it != GAP }),
    )
}
