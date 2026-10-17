package com.sqftware.orbitlauncher.domain

/** The rows and columns the clock takes on a grid page. */
const val CLOCK_ROWS = 2
const val CLOCK_COLUMNS = WIDGET_COLUMNS

/** A cell of a grid page, by its row and column. */
data class Cell(val row: Int, val column: Int)

/**
 * What the apps and folders on a grid page are, each by the id of the [launcher block][isLauncherBlock] that holds its
 * cell in the page's [WidgetPage], as the ring of a ring page keeps its place there.
 */
data class GridPage(val slots: Map<Int, RingSlot> = emptyMap())

/** What every grid page holds, by page id. A page with nothing stored is empty. */
data class GridPages(val pages: Map<String, GridPage> = emptyMap()) {
    operator fun get(page: String): GridPage = pages[page] ?: GridPage()

    fun with(page: String, grid: Grid): GridPages = GridPages(pages + (page to GridPage(grid.slots)))

    /** The grid page [page] whole, its cells kept among the page's [widgets]. */
    fun grid(page: String, widgets: WidgetPages): Grid = Grid(this[page].slots, widgets[page]).tidy()

    /** These pages with only those in [ids]. */
    fun keepingPages(ids: Set<String>): GridPages = GridPages(pages.filterKeys { it in ids })
}

/** What a block of a grid page shows once its apps are looked up in the installed list. */
sealed interface GridItem {
    data class App(val app: AppEntry) : GridItem

    /** A folder of the [apps] installed of those it holds, named [name] if the user named it. */
    data class Folder(val apps: List<AppEntry>, val name: String) : GridItem
}

/**
 * A grid page whole: what its apps and folders are, by block, and the [cells] where they sit among its widgets and its
 * clock. One of its apps or folders takes one cell, and only one thing takes a cell.
 */
data class Grid(val slots: Map<Int, RingSlot>, val cells: WidgetPage) {
    /** The apps and folders by block, as [GridItem]s of the installed [apps]; a folder shows what of it is installed. */
    fun resolve(apps: List<AppEntry>): Map<Int, GridItem> {
        val byKey = AppsByKey(apps)
        return slots.mapNotNull { (block, slot) ->
            when (slot) {
                is RingSlot.App -> byKey[slot.key]?.let { block to GridItem.App(it) }
                is RingSlot.Folder -> block to GridItem.Folder(slot.keys.mapNotNull(byKey::get), slot.name)
            }
        }.toMap()
    }

    val hasClock: Boolean get() = cells.widgets.any { it.id == CLOCK_ID }

    /** The block over [cell], if any. */
    fun at(cell: Cell): HostedWidget? =
        cells.widgets.find { cell.row in it.row until it.bottom && cell.column in it.column until it.end }

    /**
     * The grid with [slot] let go over [cell]: in a cell of its own if nothing is there, else an app goes into the app
     * there, the two making a folder, or into the folder there at its end. Null where it does not land: a folder on
     * anything, as folders do not nest, an app on itself or on a folder holding it, and anything on a widget or the clock.
     * A [hidden] block, whose app is not installed, makes way for it.
     */
    fun drop(slot: RingSlot, cell: Cell, hidden: Set<Int> = emptySet()): Grid? {
        val target = at(cell) ?: return Grid(slots + (nextBlock to slot), cells.place(nextBlock, cell))
        if (target.id in hidden) return remove(target.id).drop(slot, cell)
        val app = slot as? RingSlot.App ?: return null
        val folded = when (val there = slots[target.id] ?: return null) {
            is RingSlot.App -> RingSlot.Folder(listOf(there.key, app.key)).takeIf { there.key != app.key }
            is RingSlot.Folder -> there.copy(keys = there.keys + app.key).takeIf { app.key !in there.keys }
        } ?: return null
        return copy(slots = slots + (target.id to folded))
    }

    /** The grid with the app or folder [block] let go over [cell], as [drop] has it; null where it does not land. */
    fun move(block: Int, cell: Cell, hidden: Set<Int> = emptySet()): Grid? {
        val slot = slots[block] ?: return null
        val target = at(cell)
        return when {
            target?.id == block -> null
            target == null -> copy(cells = cells.remove(block).place(block, cell))
            else -> remove(block).drop(slot, cell, hidden)
        }
    }

    /** The grid without [block]; a folder goes with its apps. */
    fun remove(block: Int): Grid = Grid(slots - block, cells.remove(block))

    /** The grid with [key] taken out of the folder [block]; a folder left with nothing goes. */
    fun removeFromFolder(block: Int, key: String): Grid {
        val folder = slots[block] as? RingSlot.Folder ?: return this
        val left = folder.keys - key
        return if (left.isEmpty()) remove(block) else copy(slots = slots + (block to folder.copy(keys = left)))
    }

    /** The grid with the folder [block] called [name], cleaned as a ring folder's name is. */
    fun rename(block: Int, name: String): Grid {
        val folder = slots[block] as? RingSlot.Folder ?: return this
        return copy(slots = slots + (block to folder.copy(name = cleanName(name, MAX_FOLDER_NAME))))
    }

    /** The grid with the clock in the first free cells of its [pageRows] rows that hold it; unchanged with none. */
    fun addClock(pageRows: Int): Grid = copy(cells = cells.add(CLOCK_ID, CLOCK_ROWS, CLOCK_COLUMNS, pageRows, lengthens = false))

    /**
     * The grid with only the apps and folders that have both a cell and something stored, so a block that lost either
     * neither takes a cell nor comes back.
     */
    fun tidy(): Grid {
        val placed = cells.widgets.mapTo(HashSet()) { it.id }
        return Grid(slots.filterKeys { it in placed }, WidgetPage(cells.widgets.filter { it.id >= CLOCK_ID || it.id in slots }))
    }

    // Below the clock's and every block's on the page, so it is no other block's.
    private val nextBlock: Int get() = (cells.widgets.map { it.id } + slots.keys + CLOCK_ID).min() - 1
}

/** The page with a block [id] one cell big at [cell]. */
private fun WidgetPage.place(id: Int, cell: Cell) =
    WidgetPage((widgets + HostedWidget(id, cell.row, cell.column, 1, 1)).sortedWith(WidgetPage.PLACE))

/** The grid page as text, one line per app or folder: its block id, a tab, and the slot as a ring stores it. */
fun GridPage.encode(): String = slots.entries.joinToString(LINE) { (block, slot) -> "$block$FIELD${Ring(listOf(slot)).encode()}" }

/** A line that is not a block of the launcher's apps is skipped. */
fun decodeGridPage(text: String): GridPage = GridPage(
    text.nonEmptyLines().mapNotNull { line ->
        val fields = line.split(FIELD, limit = 2)
        val block = fields[0].toIntOrNull()?.takeIf { it < CLOCK_ID } ?: return@mapNotNull null
        decodeRing(fields.getOrNull(1) ?: return@mapNotNull null).slots.singleOrNull()?.let { block to it }
    }.toMap(),
)
