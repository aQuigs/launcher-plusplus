package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RingPagesTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")

    @Test
    fun `each ring page keeps its own ring and shares the dock`() {
        val pages = RingPages(rings = mapOf(HOME to ringOf(clock)), dock = ringOf(mail))

        val changed = pages.with(OTHER, pages.on(OTHER).add(HomePlace.Ring, maps).add(HomePlace.Dock, clock))

        assertEquals(HomeApps(ring = ringOf(clock), dock = ringOf(mail, clock)), changed.on(HOME))
        assertEquals(HomeApps(ring = ringOf(maps), dock = ringOf(mail, clock)), changed.on(OTHER))
    }

    @Test
    fun `a page's folders get planets apart from the home page's, which share theirs with the dock`() {
        val pages = RingPages(rings = mapOf(HOME to Ring(listOf(folder(clock))), OTHER to Ring(listOf(folder(maps)))), dock = Ring(listOf(folder(mail))))

        val kept = pages.withPlanetsKept()

        fun Ring.picks() = slots.map { (it as RingSlot.Folder).pick }
        assertEquals(listOf(PlanetPick.Of(Planet.Mercury)), kept.rings.getValue(HOME).picks())
        assertEquals(listOf(PlanetPick.Of(Planet.Venus)), kept.dock.picks())
        assertEquals(listOf(PlanetPick.Of(Planet.Mercury)), kept.rings.getValue(OTHER).picks())
    }

    @Test
    fun `a pin stays while its shortcut is on any page, and an activity of the same name keeps none`() {
        val onRing = shortcut("web", "a")
        val inFolder = shortcut("web", "b")
        val docked = shortcut("maps", "c")
        val onOtherPage = shortcut("maps", "d")
        val activity = AppEntry("Main", "chat", "Main")
        assertNotEquals(activity.key, shortcut("chat", "Main").key)
        val pages = RingPages(
            rings = mapOf(
                HOME to Ring(listOf(RingSlot.App(onRing.key), folder(inFolder), RingSlot.App(activity.key))),
                OTHER to Ring(listOf(folder(onOtherPage))),
            ),
            dock = Ring(listOf(RingSlot.App(docked.key))),
        )

        val kept = pages.keptPins(mapOf("web" to listOf("a", "b", "gone"), "maps" to listOf("c", "d"), "chat" to listOf("Main")))

        assertEquals(mapOf("web" to listOf("a", "b"), "chat" to emptyList<String>()), kept)
    }

    private fun shortcut(packageName: String, id: String) = AppEntry(id, packageName, "Main", kind = EntryKind.Shortcut(id))

    private companion object {
        val HOME = LauncherPage.Home.id
        const val OTHER = "ring-2"
    }
}
