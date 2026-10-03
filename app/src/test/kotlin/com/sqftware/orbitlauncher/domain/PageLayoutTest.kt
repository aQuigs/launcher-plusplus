package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageLayoutTest {
    @Test
    fun `default layout puts home in the middle`() {
        val layout = PageLayout()

        assertEquals(3, layout.pages.size)
        assertEquals(layout.pages.size / 2, layout.homeIndex)
    }

    @Test
    fun `home index follows the page order`() {
        val layout = PageLayout(listOf(LauncherPage.Home, LauncherPage.Collections))

        assertEquals(0, layout.homeIndex)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a layout without home is rejected`() {
        PageLayout(listOf(LauncherPage.Widgets, LauncherPage.Collections))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a repeated page is rejected`() {
        PageLayout(listOf(LauncherPage.Home, LauncherPage.Home))
    }

    @Test
    fun `a page is added at either end with an id of its own`() {
        val layout = PageLayout().add(PageKind.Ring, atStart = true).add(PageKind.Ring, atStart = false)

        assertEquals(PageKind.Ring, layout.pages.first().kind)
        assertEquals(PageKind.Ring, layout.pages.last().kind)
        assertNotEquals(layout.pages.first().id, layout.pages.last().id)
    }

    @Test
    fun `nothing is added to a full layout`() {
        val full = generateSequence(PageLayout()) { it.add(PageKind.Widgets, atStart = false) }.first { it.isFull }

        assertTrue(full.isFull)
        assertEquals(full, full.add(PageKind.Ring, atStart = true))
    }

    @Test
    fun `home is never removed, other pages are`() {
        val layout = PageLayout()

        assertEquals(layout, layout.remove(LauncherPage.Home))
        assertEquals(listOf(LauncherPage.Home, LauncherPage.Collections), layout.remove(LauncherPage.Widgets).pages)
    }

    @Test
    fun `a page moves to another place and home's index follows it`() {
        val moved = PageLayout().move(from = 0, to = 2)

        assertEquals(listOf(LauncherPage.Home, LauncherPage.Collections, LauncherPage.Widgets), moved.pages)
    }

    @Test
    fun `dots show once a page is two swipes from home`() {
        assertFalse(PageLayout().showsDots)
        assertTrue(PageLayout().add(PageKind.Ring, atStart = false).showsDots)
    }

    @Test
    fun `pages are named by kind, numbered from the second of a kind, home being the first ring`() {
        val layout = PageLayout().add(PageKind.Ring, atStart = false, id = "r2").add(PageKind.Widgets, atStart = true, id = "w2")

        assertEquals(listOf("Widgets", "Widgets 2", "Home", "Collections", "Ring 2"), layout.pages.map(layout::label))
        val rings = PageLayout(listOf(LauncherPage("a", PageKind.Ring), LauncherPage.Home, LauncherPage("b", PageKind.Ring)))
        assertEquals(listOf("Ring 2", "Home", "Ring 3"), rings.pages.map(rings::label))
    }

    @Test
    fun `what a page holds is kept only while the layout has the page, and only the first collections page starts with the built-in cards`() {
        val ring = LauncherPage("r2", PageKind.Ring)
        val cards = LauncherPage("c2", PageKind.Collections)
        val ids = PageLayout(listOf(LauncherPage.Home, cards)).ids

        val rings = RingPages(rings = mapOf(LauncherPage.Home.id to ringOf(app("Clock")), ring.id to ringOf(app("Mail"))))
        assertEquals(setOf(LauncherPage.Home.id), rings.keepingPages(ids).rings.keys)
        val collections = CollectionPages(mapOf(LauncherPage.Collections.id to CollectionsPage(emptyList())))
        assertEquals(emptyMap<String, CollectionsPage>(), collections.keepingPages(ids).pages)
        assertEquals(CollectionsPage().cards, CollectionPages().on(LauncherPage.Collections.id).cards)
        assertEquals(emptyList<CollectionCard>(), CollectionPages().on(cards.id).cards)
        val widgets = WidgetPages(mapOf(LauncherPage.Widgets.id to WidgetPage(listOf(HostedWidget(1, 0, 0, 1, 1)))))
        assertEquals(WidgetPages(), widgets.keepingPages(ids))
    }

    @Test
    fun `a layout survives encoding, and one without home or with stray lines decodes sensibly`() {
        val layout = PageLayout().add(PageKind.Collections, atStart = true, id = "c2")

        assertEquals(layout, decodePageLayout(layout.encode()))
        assertEquals(PageLayout(), decodePageLayout("widgets\tWidgets"))
        assertEquals(PageLayout(listOf(LauncherPage.Home)), decodePageLayout("home\tRing\nx\tNope\nhome\tRing"))
    }
}
