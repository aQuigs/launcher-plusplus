package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerStyleTest {
    private val calendar = app("Calendar").copy(installedAt = 3)
    private val chrome = app("Chrome").copy(installedAt = 1)
    private val maps = app("Maps").copy(installedAt = 2)
    private val byName = listOf(calendar, chrome, maps)

    @Test
    fun `most used puts the longest in front first, and apps never in front after them by name`() {
        val time = ForegroundTime(mapOf(maps.packageName to 50L, chrome.packageName to 10L))

        assertEquals(listOf(maps, chrome, calendar), byName.inOrder(DrawerOrder.MostUsed, time))
    }

    @Test
    fun `most used without usage access keeps the order by name`() {
        assertEquals(byName, byName.inOrder(DrawerOrder.MostUsed, null))
    }

    @Test
    fun `recently installed puts the newest first`() {
        assertEquals(listOf(calendar, maps, chrome), byName.inOrder(DrawerOrder.Newest, null))
    }
}
