package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageContentsTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")
    private val notes = app("Notes")
    private val other = LauncherPage("ring-2", PageKind.Ring)

    @Test
    fun `a ring page counts the apps in its folders, and home counts the dock too`() {
        val rings = RingPages(rings = mapOf(LauncherPage.Home.id to Ring(listOf(folder(clock, mail))), other.id to ringOf(maps)), dock = ringOf(notes))

        assertEquals(PageContents.Ring(slots = 1, apps = 3, folders = 1, dock = 1), contentsOf(LauncherPage.Home, rings, CollectionPages(), WidgetPages()))
        assertEquals(PageContents.Ring(slots = 1, apps = 1, folders = 0), contentsOf(other, rings, CollectionPages(), WidgetPages()))
    }

    @Test
    fun `a page nothing has been kept for is empty, but for the built-in cards of a collections page`() {
        val fresh = LauncherPage("ring-3", PageKind.Ring)
        val widgets = LauncherPage("widgets-2", PageKind.Widgets)

        assertTrue(contentsOf(fresh, RingPages(), CollectionPages(), WidgetPages()).isEmpty)
        assertTrue(contentsOf(widgets, RingPages(), CollectionPages(), WidgetPages()).isEmpty)
        assertEquals(PageContents.Cards(CollectionsPage().cards.size), contentsOf(LauncherPage.Collections, RingPages(), CollectionPages(), WidgetPages()))
    }
}
