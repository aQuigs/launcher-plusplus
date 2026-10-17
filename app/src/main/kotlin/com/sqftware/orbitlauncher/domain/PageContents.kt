package com.sqftware.orbitlauncher.domain

/** What a page holds: what deleting it would take away, and what a sketch of it shows. */
sealed interface PageContents {
    val isEmpty: Boolean

    /**
     * A ring page's [slots], which hold its [apps], those in its [folders] included, and the [widgets] round its ring. The
     * home page's count the dock too, which has [dock] slots of its own.
     */
    data class Ring(val slots: Int, val apps: Int, val folders: Int, val dock: Int = 0, val widgets: Int = 0) : PageContents {
        override val isEmpty: Boolean get() = slots == 0 && dock == 0 && widgets == 0
    }

    data class Cards(val cards: Int) : PageContents {
        override val isEmpty: Boolean get() = cards == 0
    }

    data class Widgets(val widgets: List<HostedWidget>) : PageContents {
        override val isEmpty: Boolean get() = widgets.isEmpty()
    }

    /** A grid page's [apps], those in its [folders] included, and its [blocks] of every kind where they sit. */
    data class Grid(val apps: Int, val folders: Int, val blocks: List<HostedWidget>) : PageContents {
        override val isEmpty: Boolean get() = blocks.isEmpty()

        val widgets: Int get() = blocks.count { !isLauncherBlock(it.id) }
    }
}

/** What [page] holds of [rings], [collections], [grids] or [widgets], as its kind says; the dock is the home page's. */
fun contentsOf(page: LauncherPage, rings: RingPages, collections: CollectionPages, grids: GridPages, widgets: WidgetPages): PageContents = when (page.kind) {
    PageKind.Ring -> {
        val held = rings.on(page.id).let { if (page == LauncherPage.Home) it else HomeApps(it.ring) }
        PageContents.Ring(
            slots = held.ring.slots.size,
            apps = held.keys.size,
            folders = (held.ring.slots + held.dock.slots).count { it is RingSlot.Folder },
            dock = held.dock.slots.size,
            widgets = widgets[page.id].widgets.count { it.id != RING_ID },
        )
    }
    PageKind.Collections -> PageContents.Cards(collections.on(page.id).cards.size)
    PageKind.Widgets -> PageContents.Widgets(widgets[page.id].widgets)
    PageKind.Grid -> {
        val grid = grids.grid(page.id, widgets)
        val slots = grid.slots.values
        PageContents.Grid(
            apps = slots.sumOf { if (it is RingSlot.Folder) it.keys.size else 1 },
            folders = slots.count { it is RingSlot.Folder },
            blocks = grid.cells.widgets,
        )
    }
}
