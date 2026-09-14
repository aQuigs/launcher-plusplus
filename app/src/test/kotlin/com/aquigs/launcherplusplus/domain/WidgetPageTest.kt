package com.aquigs.launcherplusplus.domain

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
}
