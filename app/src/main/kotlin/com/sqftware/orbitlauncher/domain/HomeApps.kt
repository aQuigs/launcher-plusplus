package com.sqftware.orbitlauncher.domain

import java.io.Serializable

/** A place on the home screen where the user keeps apps. Serializable so the screen can save which one it is picking for. */
sealed interface HomePlace : Serializable {
    /** A place of slots, each an app or a folder. */
    sealed interface Slots : HomePlace

    data object Ring : Slots

    data object Dock : Slots

    /** The folder in slot [index] of [holder]. */
    data class Folder(val holder: Slots, val index: Int) : HomePlace
}

/** Where on the home screen an app dragged in from another place, or from the drawer, lands. */
sealed interface Landing {
    /** The ring or the dock, whose slots it lands among. */
    val holder: HomePlace.Slots

    /**
     * [target]'s place in [holder], as [mode] says: inserted, the app goes in before it; swapped, the two trade places. A
     * folder never leaves its place, so an app swapped onto one goes in before it instead. Without one, the end.
     */
    data class At(override val holder: HomePlace.Slots, val target: RingItem?, val mode: ReorderMode) : Landing

    /** Into [target]: an app there and the one dropped on it become a folder, and a folder takes the app in at its end. */
    data class Into(override val holder: HomePlace.Slots, val target: RingItem) : Landing
}

/** The apps the user keeps on the home screen: on the ring round the emblem, and in the dock at the foot of the page. */
data class HomeApps(val ring: Ring = Ring(), val dock: Ring = Ring()) {
    fun slots(place: HomePlace.Slots): Ring = when (place) {
        HomePlace.Ring -> ring
        HomePlace.Dock -> dock
    }

    /** These home apps with [place]'s slots as [change] leaves them. */
    fun change(place: HomePlace.Slots, change: Ring.() -> Ring): HomeApps = when (place) {
        HomePlace.Ring -> copy(ring = ring.change())
        HomePlace.Dock -> copy(dock = dock.change())
    }

    /** The apps at [place]: those in slots of their own for the ring or the dock, and none for a folder that is not there. */
    operator fun get(place: HomePlace): Favourites = when (place) {
        is HomePlace.Slots -> slots(place).apps
        is HomePlace.Folder -> Favourites(slots(place.holder).folder(place.index)?.keys.orEmpty())
    }

    /** Adds [app] to [place], unless it is already there. The other places are left as they are. */
    fun add(place: HomePlace, app: AppEntry): HomeApps = when (place) {
        is HomePlace.Slots -> change(place) { add(app) }
        is HomePlace.Folder -> change(place.holder) { add(place.index, app) }
    }

    /**
     * Adds [app] to [place], or takes it off if it is already there. The other places are left as they are, and so is a
     * folder left with one app or none, as a pick filling it may empty it on the way.
     */
    fun toggle(place: HomePlace, app: AppEntry): HomeApps = when (place) {
        is HomePlace.Slots -> change(place) { toggle(app) }
        is HomePlace.Folder -> change(place.holder) { toggle(place.index, app) }
    }

    /**
     * Whether [folder] is still the same folder in [changed]: a folder in the same slot, with every other slot of its place
     * as it was. A folder is known only by its slot, so once any other slot there comes, goes or moves, it may be another.
     */
    fun keeps(folder: HomePlace.Folder, changed: HomeApps): Boolean {
        val before = slots(folder.holder).slots
        val after = changed.slots(folder.holder).slots
        return before.size == after.size && after.getOrNull(folder.index) is RingSlot.Folder &&
            before.indices.all { it == folder.index || before[it] == after[it] }
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
        val folderLeft = from == HomePlace.Folder(to.holder, index)
        return when (to) {
            is Landing.Into -> {
                val slot = slots(to.holder).slots.getOrNull(index)
                slot != null && slot != RingSlot.App(app.key) && !folderLeft
            }
            is Landing.At -> index >= 0 && from != to.holder && !folderLeft
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
                draft.slots(to.holder)[index] = when (val slot = slots(to.holder).slots[index]) {
                    is RingSlot.App -> RingSlot.Folder(listOf(slot.key, app.key))
                    is RingSlot.Folder -> RingSlot.Folder(Favourites(slot.keys).add(app).keys)
                }
            }
            is Landing.At -> draft.land(app.key, from, to.holder, index, to.mode)
        }
        return draft.build(shown)
    }

    /** The slot [to] names, or -1 for a target that is not there. */
    private fun indexOf(to: Landing): Int {
        val slots = slots(to.holder)
        val target = when (to) {
            is Landing.Into -> to.target
            is Landing.At -> to.target ?: return slots.slots.size
        }
        return if (target is RingItem.Folder && target.at.holder != to.holder) -1 else slots.slotOf(target)
    }

    /**
     * The pins to set so that no shortcut stays pinned once it is nowhere on the home screen. [pinned] is the ids pinned
     * now, by package; the result holds, for each package with a pin that has gone, the ids of its pins still here.
     */
    fun keptPins(pinned: Map<String, List<String>>): Map<String, List<String>> {
        val here = (ring.slots + dock.slots).flatMap { slot ->
            when (slot) {
                is RingSlot.App -> listOf(slot.key)
                is RingSlot.Folder -> slot.keys
            }
        }.toSet()
        return pinned.mapValues { (packageName, ids) -> ids.filter { shortcutKey(packageName, it) in here } }
            .filter { (packageName, kept) -> kept.size < pinned.getValue(packageName).size }
    }
}

/** A key no app has. It holds the place of an app that has left, so no slot shifts until an edit is done. */
private const val GAP = ""

/** [home] being edited in place. Every insertion comes last, so the positions found at the start still hold. */
private class Draft(home: HomeApps) {
    private val slots = listOf(HomePlace.Ring, HomePlace.Dock).associateWith { home.slots(it).slots.toMutableList() }

    fun slots(place: HomePlace.Slots): MutableList<RingSlot> = slots.getValue(place)

    /** Puts [key] in [app]'s stead wherever [place] holds it. */
    fun put(place: HomePlace?, app: String, key: String) {
        when (place) {
            is HomePlace.Slots -> slots(place).replaceAll { if (it == RingSlot.App(app)) RingSlot.App(key) else it }
            is HomePlace.Folder -> {
                val slots = slots(place.holder)
                (slots.getOrNull(place.index) as? RingSlot.Folder)?.let { folder ->
                    slots[place.index] = RingSlot.Folder(folder.keys.map { if (it == app) key else it })
                }
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
     * Puts [app], leaving [from], at [index] of [place]: in the stead of the app there if [mode] swaps, else in before
     * whatever is there, a folder included.
     */
    fun land(app: String, from: HomePlace?, place: HomePlace.Slots, index: Int, mode: ReorderMode) {
        val slots = slots(place)
        val swapped = (slots.getOrNull(index) as? RingSlot.App)?.key?.takeIf { mode == ReorderMode.Swap }
        leave(from, app, swapped)
        put(place, app, GAP)
        if (swapped != null) slots[index] = RingSlot.App(app) else slots.add(index, RingSlot.App(app))
    }

    /**
     * The home apps with the gaps closed. A folder an app left that [showsNone][RingSlot.Folder.showsNone] of the [shown]
     * apps goes with them, the missing apps it holds too: its last app the user could see has left.
     */
    fun build(shown: Set<String>) = HomeApps(ring = closed(slots(HomePlace.Ring), shown), dock = closed(slots(HomePlace.Dock), shown))

    private fun closed(slots: List<RingSlot>, shown: Set<String>) = Ring(
        slots.mapNotNull { slot ->
            when (slot) {
                is RingSlot.App -> slot.takeIf { it.key != GAP }
                is RingSlot.Folder -> RingSlot.Folder(slot.keys.filter { it != GAP }).takeUnless {
                    GAP in slot.keys && slot.showsNone(shown)
                }
            }
        },
    )
}
