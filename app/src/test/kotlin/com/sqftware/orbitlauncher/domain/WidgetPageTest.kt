package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPageTest {
    private val clock = HostedWidget(7, row = 0, column = 0, rows = 1, columns = 2)
    private val notes = HostedWidget(12, row = 2, column = 1, rows = 3, columns = 3)
    private val page = WidgetPage(listOf(clock, notes))

    @Test
    fun `a new widget takes the first free cells from the top and then the left`() {
        assertEquals(clock.copy(id = 20, column = 2), page.add(20, rows = 1, columns = 2).widgets[1])
        assertEquals(HostedWidget(20, row = 1, column = 0, rows = 1, columns = 4), page.add(20, rows = 1, columns = 4).widgets[1])
        assertEquals(HostedWidget(20, row = 5, column = 0, rows = 2, columns = 4), page.add(20, rows = 2, columns = 4).widgets.last())
        assertEquals(HostedWidget(20, row = 0, column = 0, rows = 1, columns = 4), WidgetPage().add(20, rows = 0, columns = 9).widgets.single())
    }

    @Test
    fun `a widget leaves by id and the others keep their places`() {
        assertEquals(WidgetPage(listOf(notes)), page.remove(7))
        assertEquals(page, page.remove(99))
        assertEquals(setOf(7, 12), page.ids)
        assertEquals(5, page.rows)
    }

    @Test
    fun `a move puts a widget in an empty spot and leaves the gap it came from`() {
        val moved = page.move(7, row = 6, column = 2)

        assertEquals(listOf(notes, clock.copy(row = 6, column = 2)), moved.widgets)
        assertEquals(7, moved.rows)
    }

    @Test
    fun `a move stops at the page's edges`() {
        assertEquals(clock.copy(row = 0, column = 2), page.move(7, row = -3, column = 5).widgets.first())
        assertEquals(page, page.move(99, row = 0, column = 0))
    }

    @Test
    fun `widgets a moved one lands on move down out of its way`() {
        val battery = HostedWidget(3, row = 5, column = 1, rows = 1, columns = 1)
        val moved = WidgetPage(listOf(clock, notes, battery)).move(7, row = 3, column = 2)

        assertEquals(
            listOf(clock.copy(row = 3, column = 2), notes.copy(row = 4), battery.copy(row = 7)),
            moved.widgets,
        )
    }

    @Test
    fun `a resize keeps a widget in place, stops at the right edge and pushes down what it covers`() {
        assertEquals(listOf(clock.copy(rows = 3, columns = 4), notes.copy(row = 3)), page.resize(7, rows = 3, columns = 9).widgets)
        assertEquals(notes.copy(rows = 1, columns = 3), page.resize(12, rows = 0, columns = 3).widgets.last())
        assertEquals(page, page.resize(99, rows = 2, columns = 2))
    }

    @Test
    fun `a page comes back as it went`() {
        assertEquals("7\t0\t0\t1\t2\n12\t2\t1\t3\t3", page.encode())
        assertEquals(page, decodeWidgetPage(page.encode()))
    }

    @Test
    fun `an empty page is empty text`() {
        assertEquals("", WidgetPage().encode())
        assertEquals(WidgetPage(), decodeWidgetPage(""))
    }

    @Test
    fun `a page stored before widgets had places stacks them down its width`() {
        assertEquals(
            listOf(HostedWidget(7, 0, 0, 1, WIDGET_COLUMNS), HostedWidget(12, 1, 0, 3, WIDGET_COLUMNS)),
            decodeWidgetPage("7\t1\n12\t3").widgets,
        )
        assertEquals("a line skipped leaves no gap", decodeWidgetPage("7\t1\n12\t3"), decodeWidgetPage("7\t1\n0\t2\n7\t4\n12\t3"))
    }

    @Test
    fun `a malformed line or a second line for the same widget is skipped`() {
        assertEquals(
            page,
            decodeWidgetPage("7\t0\t0\t1\t2\nseven\t0\t0\t1\t1\n8\n9\t0\t0\t0\t1\n10\t0\t3\t1\t2\n-3\t0\t0\t1\t1\n12\t2\t1\t3\t3\n7\t4\t0\t1\t1"),
        )
    }

    @Test
    fun `stored widgets that overlap move down out of each other's way`() {
        assertEquals(listOf(clock.copy(id = 8), clock.copy(row = 1)), decodeWidgetPage("8\t0\t0\t1\t2\n7\t0\t0\t1\t2").widgets)
    }

    @Test
    fun `a widget takes the whole cells its length needs, gaps and all`() {
        assertEquals(1, widgetCells(minDp = 48, cellDp = 80f, most = 9))
        assertEquals(1, widgetCells(minDp = 80, cellDp = 80f, most = 9))
        assertEquals(2, widgetCells(minDp = 81, cellDp = 80f, most = 9))
        assertEquals(2, widgetCells(minDp = 168, cellDp = 80f, most = 9))
        assertEquals(3, widgetCells(minDp = 169, cellDp = 80f, most = 9))
        assertEquals(2, widgetCells(minDp = 180, cellDp = 90.5f, most = 4))
    }

    @Test
    fun `a widget takes at least one cell and at most the most allowed`() {
        assertEquals(1, widgetCells(minDp = 0, cellDp = 80f, most = 9))
        assertEquals(9, widgetCells(minDp = 2000, cellDp = 80f, most = 9))
        assertEquals(1, widgetCells(minDp = 2000, cellDp = 80f, most = 0))
    }

    @Test
    fun `a dragged edge snaps to the nearest whole cell`() {
        assertEquals(3, nearestCells(cells = 3, dragDp = 43f, pitchDp = 88f))
        assertEquals(4, nearestCells(cells = 3, dragDp = 45f, pitchDp = 88f))
        assertEquals(2, nearestCells(cells = 3, dragDp = -45f, pitchDp = 88f))
    }

    @Test
    fun `a widget resizes from one cell to the most allowed when its provider sets no limits`() {
        assertEquals(1..9, WidgetResize().cellRange(cells = 3, cellDp = 80f, most = 9))
    }

    @Test
    fun `a widget resizes within the cells its provider allows`() {
        assertEquals(2..3, WidgetResize(minDp = 81, maxResizeDp = 330).cellRange(cells = 3, cellDp = 80f, most = 9))
        assertEquals(2..4, WidgetResize(minDp = 81, maxResizeDp = 344).cellRange(cells = 3, cellDp = 80f, most = 9))
        assertEquals(2..9, WidgetResize(minDp = 81, maxResizeDp = 2000).cellRange(cells = 3, cellDp = 80f, most = 9))
        assertEquals(3..3, WidgetResize(minDp = 200, maxResizeDp = 100).cellRange(cells = 3, cellDp = 80f, most = 9))
    }

    @Test
    fun `a widget keeps its own cells in reach when they are outside the limits`() {
        assertEquals(1..12, WidgetResize().cellRange(cells = 12, cellDp = 80f, most = 9))
        assertEquals(1..4, WidgetResize(minDp = 200).cellRange(cells = 1, cellDp = 80f, most = 4))
    }

    @Test
    fun `a minimum resize length only lowers the minimum`() {
        assertEquals(1..9, WidgetResize(minDp = 200, minResizeDp = 50).cellRange(cells = 3, cellDp = 80f, most = 9))
        assertEquals(3..9, WidgetResize(minDp = 200, minResizeDp = 300).cellRange(cells = 3, cellDp = 80f, most = 9))
    }

    @Test
    fun `a widget that does not stretch one way takes only the cells its minimum needs, or its own`() {
        assertEquals(2..2, WidgetResize(resizable = false, minDp = 160).cellRange(cells = 2, cellDp = 80f, most = 9))
        assertEquals(2..4, WidgetResize(resizable = false, minDp = 160, minResizeDp = 50).cellRange(cells = 4, cellDp = 80f, most = 9))
    }

    @Test
    fun `a drag past either end of the range is held there`() {
        assertEquals(-176f, heldDrag(cells = 3, dragDp = -1000f, range = 1..9, pitchDp = 88f))
        assertEquals(528f, heldDrag(cells = 3, dragDp = 1000f, range = 1..9, pitchDp = 88f))
        assertEquals(50f, heldDrag(cells = 3, dragDp = 50f, range = 1..9, pitchDp = 88f))
    }
}
