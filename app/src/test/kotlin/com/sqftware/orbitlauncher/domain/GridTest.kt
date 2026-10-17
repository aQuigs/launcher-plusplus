package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridTest {
    private val mail = app("Mail")
    private val maps = app("Maps")
    private val notes = app("Notes")
    private val widget = HostedWidget(7, row = 0, column = 0, rows = 2, columns = 4)

    // Mail at the top-left of the third row, under a widget across the first two, and a folder of Maps beside it.
    private val grid = Grid(
        mapOf(-2 to RingSlot.App(mail.key), -3 to folder(maps)),
        WidgetPage(listOf(widget, HostedWidget(-2, 2, 0, 1, 1), HostedWidget(-3, 2, 1, 1, 1))),
    )

    @Test
    fun `an app let go on an empty cell takes it, under a new block id`() {
        val dropped = grid.drop(RingSlot.App(notes.key), Cell(3, 2))!!

        assertEquals(RingSlot.App(notes.key), dropped.slots[-4])
        assertEquals(HostedWidget(-4, 3, 2, 1, 1), dropped.at(Cell(3, 2)))
    }

    @Test
    fun `an app let go on another makes a folder of the two, and on a folder joins it at its end`() {
        assertEquals(folder(mail, notes), grid.drop(RingSlot.App(notes.key), Cell(2, 0))!!.slots[-2])
        assertEquals(folder(maps, notes), grid.drop(RingSlot.App(notes.key), Cell(2, 1))!!.slots[-3])
    }

    @Test
    fun `nothing lands on a widget, a folder on anything, or an app on itself or a folder holding it`() {
        assertNull(grid.drop(RingSlot.App(notes.key), Cell(1, 3)))
        assertNull(grid.drop(folder(notes), Cell(2, 0)))
        assertNull(grid.drop(RingSlot.App(mail.key), Cell(2, 0)))
        assertNull(grid.drop(RingSlot.App(maps.key), Cell(2, 1)))
    }

    @Test
    fun `a move takes an empty cell, or folds into what is there, leaving its own`() {
        val moved = grid.move(-2, Cell(4, 3))!!
        assertEquals(HostedWidget(-2, 4, 3, 1, 1), moved.at(Cell(4, 3)))
        assertNull(moved.at(Cell(2, 0)))

        val folded = grid.move(-2, Cell(2, 1))!!
        assertEquals(mapOf(-3 to folder(maps, mail)), folded.slots)
        assertNull(folded.at(Cell(2, 0)))

        assertNull("onto itself", grid.move(-2, Cell(2, 0)))
    }

    @Test
    fun `an app leaves its folder, and the folder goes with its last app`() {
        val two = grid.drop(RingSlot.App(notes.key), Cell(2, 1))!!

        assertEquals(folder(notes), two.removeFromFolder(-3, maps.key).slots[-3])
        assertEquals(setOf(-2), two.removeFromFolder(-3, maps.key).removeFromFolder(-3, notes.key).slots.keys)
    }

    @Test
    fun `the clock takes the first free rows across the page, once`() {
        val clocked = grid.addClock(pageRows = 6)

        assertTrue(clocked.hasClock)
        assertEquals(HostedWidget(CLOCK_ID, 3, 0, CLOCK_ROWS, CLOCK_COLUMNS), clocked.cells.widgets.find { it.id == CLOCK_ID })
        assertEquals("no room", grid, grid.addClock(pageRows = 4))
    }

    @Test
    fun `a block with no slot, or a slot with no block, is dropped`() {
        val damaged = Grid(grid.slots + (-9 to RingSlot.App(notes.key)), WidgetPage(grid.cells.widgets + HostedWidget(-8, 5, 0, 1, 1)))

        assertEquals(grid, damaged.tidy())
    }

    @Test
    fun `apps and folders show what is installed of them`() {
        val items = grid.drop(RingSlot.App(notes.key), Cell(2, 1))!!.resolve(listOf(mail, notes))

        assertEquals(mapOf(-2 to GridItem.App(mail), -3 to GridItem.Folder(listOf(notes), "")), items)
    }

    @Test
    fun `a grid page comes back as it went, and a line that is not one of its apps is skipped`() {
        val named = GridPage(grid.rename(-3, "Travel").slots)

        assertEquals(named, decodeGridPage(named.encode()))
        assertEquals(GridPage(mapOf(-2 to RingSlot.App(mail.key))), decodeGridPage("-2\t${mail.key}\n7\t${maps.key}\nx\t${notes.key}\n-5"))
    }

    @Test
    fun `the launcher's blocks are not widgets the system hosts, and stay with their page`() {
        val page = grid.addClock(pageRows = 6).cells

        assertEquals(setOf(7), page.ids)
        assertEquals(page, decodeWidgetPage(page.encode()))
        assertEquals(page.widgets - widget, page.keeping(emptySet()).widgets)
    }

    @Test
    fun `a grid page counts the apps in its folders, and its widgets`() {
        val page = LauncherPage("grid-1", PageKind.Grid)
        val contents = contentsOf(page, RingPages(), CollectionPages(), GridPages(mapOf(page.id to GridPage(grid.slots))), WidgetPages(mapOf(page.id to grid.cells)))

        assertEquals(PageContents.Grid(apps = 2, folders = 1, blocks = grid.cells.widgets), contents)
    }
}
