package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetPageTest {
    private val page = WidgetPage(listOf(HostedWidget(7, 1), HostedWidget(12, 3)))

    @Test
    fun `widgets join at the bottom and leave by id`() {
        assertEquals(page, WidgetPage().add(HostedWidget(7, 1)).add(HostedWidget(12, 3)))
        assertEquals(WidgetPage(listOf(HostedWidget(12, 3))), page.remove(7))
        assertEquals(page, page.remove(99))
        assertEquals(setOf(7, 12), page.ids)
    }

    @Test
    fun `a resize changes only that widget's rows and keeps its place`() {
        assertEquals(WidgetPage(listOf(HostedWidget(7, 4), HostedWidget(12, 3))), page.resize(7, 4))
        assertEquals(page, page.resize(99, 4))
    }

    @Test
    fun `a move takes a widget out and puts it back at another place`() {
        val three = page.add(HostedWidget(20, 2))
        assertEquals(listOf(12, 20, 7), three.move(7, 2).widgets.map { it.id })
        assertEquals(listOf(20, 7, 12), three.move(20, 0).widgets.map { it.id })
        assertEquals(three, three.move(12, 1))
        assertEquals(three, three.move(99, 0))
    }

    @Test
    fun `a page comes back as it went`() {
        assertEquals("7\t1\n12\t3", page.encode())
        assertEquals(page, decodeWidgetPage(page.encode()))
    }

    @Test
    fun `an empty page is empty text`() {
        assertEquals("", WidgetPage().encode())
        assertEquals(WidgetPage(), decodeWidgetPage(""))
    }

    @Test
    fun `a malformed line is skipped`() {
        assertEquals(page, decodeWidgetPage("7\t1\nseven\t1\n8\n9\t0\n0\t1\n-3\t1\n10\t2\t2\n\n12\t3"))
    }

    @Test
    fun `a second line for the same widget is skipped`() {
        assertEquals(page, decodeWidgetPage("7\t1\n12\t3\n7\t2"))
    }

    @Test
    fun `a widget takes the whole rows its height needs`() {
        assertEquals(1, widgetRows(minHeightDp = 48, pageRows = 9))
        assertEquals(1, widgetRows(minHeightDp = 80, pageRows = 9))
        assertEquals(2, widgetRows(minHeightDp = 81, pageRows = 9))
        assertEquals(2, widgetRows(minHeightDp = 110, pageRows = 9))
        assertEquals(3, widgetRows(minHeightDp = 100, pageRows = 9, rowHeightDp = 40))
    }

    @Test
    fun `a widget takes at least one row and at most the page`() {
        assertEquals(1, widgetRows(minHeightDp = 0, pageRows = 9))
        assertEquals(9, widgetRows(minHeightDp = 2000, pageRows = 9))
        assertEquals(1, widgetRows(minHeightDp = 2000, pageRows = 0))
    }

    @Test
    fun `a dragged edge snaps to the nearest whole row`() {
        assertEquals(3, resizedRows(rows = 3, dragDp = 39f))
        assertEquals(4, resizedRows(rows = 3, dragDp = 41f))
        assertEquals(2, resizedRows(rows = 3, dragDp = -41f))
    }

    @Test
    fun `a widget resizes from one row to the page when its provider sets no limits`() {
        assertEquals(1..9, WidgetSizing().rowRange(rows = 3, pageRows = 9))
    }

    @Test
    fun `a widget resizes within the rows its provider allows`() {
        assertEquals(2..4, WidgetSizing(minHeightDp = 81, maxResizeHeightDp = 330).rowRange(rows = 3, pageRows = 9))
        assertEquals(2..9, WidgetSizing(minHeightDp = 81, maxResizeHeightDp = 2000).rowRange(rows = 3, pageRows = 9))
        assertEquals(3..3, WidgetSizing(minHeightDp = 200, maxResizeHeightDp = 100).rowRange(rows = 3, pageRows = 9))
    }

    @Test
    fun `a widget keeps its own rows in reach when they are outside the limits`() {
        assertEquals(1..12, WidgetSizing().rowRange(rows = 12, pageRows = 9))
        assertEquals(1..4, WidgetSizing(minHeightDp = 200).rowRange(rows = 1, pageRows = 4))
    }

    @Test
    fun `a minimum resize height only lowers the minimum`() {
        assertEquals(1..9, WidgetSizing(minHeightDp = 200, minResizeHeightDp = 50).rowRange(rows = 3, pageRows = 9))
        assertEquals(3..9, WidgetSizing(minHeightDp = 200, minResizeHeightDp = 300).rowRange(rows = 3, pageRows = 9))
    }

    @Test
    fun `a drag past either end of the range is held there`() {
        assertEquals(-160f, heldDrag(rows = 3, dragDp = -1000f, range = 1..9))
        assertEquals(480f, heldDrag(rows = 3, dragDp = 1000f, range = 1..9))
        assertEquals(50f, heldDrag(rows = 3, dragDp = 50f, range = 1..9))
    }
}
