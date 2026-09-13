package com.aquigs.launcherplusplus.domain

/** One slot on the ring: an app, or a named folder of apps in order. Both keep [AppEntry.key]s so a reload of the app list keeps them. */
sealed interface RingSlot {
    data class App(val key: String) : RingSlot

    data class Folder(val name: String, val keys: List<String>) : RingSlot
}

/** What a ring slot shows once its apps are looked up in the installed list. */
sealed interface RingItem {
    data class App(val app: AppEntry) : RingItem

    /** [index] is the folder's slot on the ring, which is how the picker and the folder's menu name it. */
    data class Folder(val index: Int, val name: String, val apps: List<AppEntry>) : RingItem
}

/** The slots round the emblem, in order. */
data class Ring(val slots: List<RingSlot> = emptyList()) {
    val isEmpty: Boolean get() = slots.isEmpty()

    /** The apps in slots of their own. An app inside a folder is not on the ring itself, so it can be added there too. */
    val apps: Favourites get() = Favourites(slots.filterIsInstance<RingSlot.App>().map { it.key })

    fun folder(index: Int): RingSlot.Folder? = slots.getOrNull(index) as? RingSlot.Folder

    fun indexOf(app: AppEntry): Int = slots.indexOf(RingSlot.App(app.key))

    /** Adds [app] in a slot of its own at the end, or takes that slot off if it has one. */
    fun toggle(app: AppEntry): Ring {
        val slot = RingSlot.App(app.key)
        return Ring(if (slot in slots) slots - slot else slots + slot)
    }

    /** Adds [app] to the folder at [index], or takes it out if it is already there. The folder stays, whatever is left in it. */
    fun toggle(index: Int, app: AppEntry): Ring = updateFolder(index) { copy(keys = Favourites(keys).toggle(app).keys) }

    /** Turns [app]'s slot into a folder called [name] holding just [app]. */
    fun newFolder(app: AppEntry, name: String): Ring {
        val index = indexOf(app)
        return if (index < 0) this else replace(index, RingSlot.Folder(name, listOf(app.key)))
    }

    fun rename(index: Int, name: String): Ring = updateFolder(index) { copy(name = name) }

    /** Drops the slot at [index]; a folder goes with its apps. */
    fun remove(index: Int): Ring = Ring(slots.filterIndexed { i, _ -> i != index })

    /**
     * The ring once the user has finished taking apps out of folders: a folder left with one app becomes that app in its
     * slot, unless the ring already holds it, and one left with none goes. Apps missing from the installed list do not
     * count as taken out, so a folder outlives its apps' updates like a favourite does.
     */
    fun dissolved(): Ring {
        val onRing = apps.keys.toMutableSet()
        return Ring(
            slots.mapNotNull { slot ->
                when {
                    slot !is RingSlot.Folder || slot.keys.size > 1 -> slot
                    else -> slot.keys.singleOrNull()?.takeIf(onRing::add)?.let(RingSlot::App)
                }
            },
        )
    }

    /**
     * The slots with their installed apps, in order. An app that is missing is skipped but kept, and so is a folder none
     * of whose apps is installed, so what disappears while it updates comes back in its old place.
     */
    fun resolve(apps: List<AppEntry>): List<RingItem> {
        val byKey = apps.associateBy { it.key }
        return slots.mapIndexedNotNull { index, slot ->
            when (slot) {
                is RingSlot.App -> byKey[slot.key]?.let(RingItem::App)
                is RingSlot.Folder -> slot.keys.mapNotNull(byKey::get)
                    .takeIf { it.isNotEmpty() }
                    ?.let { RingItem.Folder(index, slot.name, it) }
            }
        }
    }

    private fun updateFolder(index: Int, change: RingSlot.Folder.() -> RingSlot.Folder): Ring {
        val folder = folder(index) ?: return this
        return replace(index, folder.change())
    }

    private fun replace(index: Int, slot: RingSlot) = Ring(slots.toMutableList().apply { this[index] = slot })

    companion object {
        private val FORMAT_CHARACTERS = Regex("[\t\r\n]")

        /**
         * [raw] as a folder's name: trimmed, with the tabs and line breaks the stored format is made of turned into
         * spaces, or null when nothing is left.
         */
        fun name(raw: String): String? = raw.replace(FORMAT_CHARACTERS, " ").trim().takeIf(String::isNotEmpty)
    }
}
