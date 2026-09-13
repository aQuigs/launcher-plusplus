package com.aquigs.launcherplusplus.domain

/** One slot on the ring: an app, or a folder of apps in order. Both keep [AppEntry.key]s so a reload of the app list keeps them. */
sealed interface RingSlot {
    data class App(val key: String) : RingSlot

    data class Folder(val keys: List<String>) : RingSlot
}

/** What a ring slot shows once its apps are looked up in the installed list. */
sealed interface RingItem {
    data class App(val app: AppEntry) : RingItem

    /** [index] is the folder's slot on the ring, which is how the picker and the folder's menu name it. */
    data class Folder(val index: Int, val apps: List<AppEntry>) : RingItem
}

/** The slots round the emblem, in order. */
data class Ring(val slots: List<RingSlot> = emptyList()) {
    val isEmpty: Boolean get() = slots.isEmpty()

    /** The apps in slots of their own. An app inside a folder is not on the ring itself, so it can be added there too. */
    val apps: Favourites get() = Favourites(slots.filterIsInstance<RingSlot.App>().map { it.key })

    fun folder(index: Int): RingSlot.Folder? = slots.getOrNull(index) as? RingSlot.Folder

    fun indexOf(app: AppEntry): Int = slots.indexOf(RingSlot.App(app.key))

    /** Adds [app] in a slot of its own at the end, unless it has one. */
    fun add(app: AppEntry): Ring = if (app in apps) this else Ring(slots + RingSlot.App(app.key))

    /** Adds [app] in a slot of its own at the end, or takes that slot off if it has one. */
    fun toggle(app: AppEntry): Ring = if (app in apps) Ring(slots - RingSlot.App(app.key)) else add(app)

    /** Adds [app] to the folder at [index], unless it is already there; a slot that is not a folder is left alone. */
    fun add(index: Int, app: AppEntry): Ring = updateFolder(index) { Favourites(it).add(app).keys }

    /**
     * Adds [app] to the folder at [index], or takes it out if it is already there. The folder stays, whatever is left in
     * it; a slot that is not a folder is left alone.
     */
    fun toggle(index: Int, app: AppEntry): Ring = updateFolder(index) { Favourites(it).toggle(app).keys }

    /** Turns [app]'s slot into a folder holding just [app]; an app without a slot changes nothing. */
    fun newFolder(app: AppEntry): Ring {
        val index = indexOf(app)
        return if (index < 0) this else replace(index, RingSlot.Folder(listOf(app.key)))
    }

    /** Drops the slot at [index]; a folder goes with its apps. */
    fun remove(index: Int): Ring = Ring(slots.filterIndexed { i, _ -> i != index })

    /**
     * The slots with their installed apps, in order. An app that is missing is skipped but kept, on the ring or in a
     * folder, so what disappears while it updates comes back in its old place. A folder shows whatever is left in it, so
     * a folder emptied by the user is an empty badge, not a gap.
     */
    fun resolve(apps: List<AppEntry>): List<RingItem> {
        val byKey = apps.associateBy { it.key }
        return slots.mapIndexedNotNull { index, slot ->
            when (slot) {
                is RingSlot.App -> byKey[slot.key]?.let(RingItem::App)
                is RingSlot.Folder -> RingItem.Folder(index, slot.keys.mapNotNull(byKey::get))
            }
        }
    }

    private fun updateFolder(index: Int, change: (List<String>) -> List<String>): Ring {
        val folder = folder(index) ?: return this
        return replace(index, RingSlot.Folder(change(folder.keys)))
    }

    private fun replace(index: Int, slot: RingSlot) = Ring(slots.toMutableList().apply { this[index] = slot })
}
