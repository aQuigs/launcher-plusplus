package com.sqftware.orbitlauncher.domain

/** What a page holds: what deleting it would take away, and what a sketch of it shows. */
sealed interface PageContents {
    val isEmpty: Boolean

    /**
     * A ring page's [slots], which hold its [apps], those in its [folders] included. The home page's count the dock too,
     * which has [dock] slots of its own.
     */
    data class Ring(val slots: Int, val apps: Int, val folders: Int, val dock: Int = 0) : PageContents {
        override val isEmpty: Boolean get() = slots == 0 && dock == 0
    }

    data class Cards(val cards: Int) : PageContents {
        override val isEmpty: Boolean get() = cards == 0
    }

    data class Widgets(val widgets: List<HostedWidget>) : PageContents {
        override val isEmpty: Boolean get() = widgets.isEmpty()
    }
}

/** What [page] holds of [rings], [collections] or [widgets], as its kind says; the dock is the home page's. */
fun contentsOf(page: LauncherPage, rings: RingPages, collections: CollectionPages, widgets: WidgetPages): PageContents = when (page.kind) {
    PageKind.Ring -> {
        val held = rings.on(page.id).let { if (page == LauncherPage.Home) it else HomeApps(it.ring) }
        PageContents.Ring(
            slots = held.ring.slots.size,
            apps = held.keys.size,
            folders = (held.ring.slots + held.dock.slots).count { it is RingSlot.Folder },
            dock = held.dock.slots.size,
        )
    }
    PageKind.Collections -> PageContents.Cards(collections.on(page.id).cards.size)
    PageKind.Widgets -> PageContents.Widgets(widgets[page.id].widgets)
}
