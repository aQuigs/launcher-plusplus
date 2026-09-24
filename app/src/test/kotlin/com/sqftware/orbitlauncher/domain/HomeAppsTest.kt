package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAppsTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")
    private val music = app("Music")
    private val installed = listOf(clock, mail, maps, music)

    @Test
    fun `toggling one place leaves the other as it is`() {
        val homeApps = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, mail)

        assertEquals(listOf(RingItem.App(clock)), homeApps.ring.resolve(listOf(clock, mail)))
        assertEquals(listOf(mail), homeApps.dock.resolve(listOf(clock, mail)))
    }

    @Test
    fun `an app can be in both places and leave one without the other`() {
        val both = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, clock)
        assertTrue(clock in both[HomePlace.Ring])
        assertTrue(clock in both[HomePlace.Dock])

        val ringOnly = both.toggle(HomePlace.Dock, clock)

        assertTrue(clock in ringOnly.ring.apps)
        assertEquals(Favourites(), ringOnly.dock)
    }

    @Test
    fun `adding to a place is a no-op once the app is there`() {
        val homeApps = HomeApps().add(HomePlace.Ring, clock).add(HomePlace.Dock, clock).add(HomePlace.Dock, mail)

        assertEquals(HomeApps(ring = ringOf(clock), dock = Favourites(listOf(clock.key, mail.key))), homeApps)
        assertEquals(homeApps, homeApps.add(HomePlace.Ring, clock))
        assertEquals(homeApps, homeApps.add(HomePlace.Dock, mail))
        assertEquals(homeApps, homeApps.add(HomePlace.Folder(0), maps))
    }

    @Test
    fun `adding to a folder fills that folder only`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock)))).add(HomePlace.Folder(0), mail)

        assertEquals(HomeApps(ring = Ring(listOf(folder(clock, mail)))), homeApps)
    }

    @Test
    fun `a folder is a place of its own on the ring`() {
        val homeApps = HomeApps(ring = ringOf(clock).newFolder(clock)).toggle(HomePlace.Folder(0), mail)

        assertEquals(Favourites(listOf(clock.key, mail.key)), homeApps[HomePlace.Folder(0)])
        assertTrue(clock !in homeApps[HomePlace.Ring])
        assertEquals(Favourites(), homeApps[HomePlace.Folder(1)])
        assertEquals(homeApps, homeApps.toggle(HomePlace.Folder(1), maps))
    }

    @Test
    fun `taking the last app out of a folder leaves the folder`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock))), dock = Favourites(listOf(mail.key)))

        val emptied = homeApps.toggle(HomePlace.Folder(0), clock)

        assertEquals(HomeApps(ring = Ring(listOf(folder())), dock = Favourites(listOf(mail.key))), emptied)
    }

    @Test
    fun `removing an app takes a folder it empties with it`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock), folder(mail, maps))), dock = Favourites(listOf(clock.key)))

        assertEquals(HomeApps(ring = Ring(listOf(folder(mail, maps))), dock = homeApps.dock), homeApps.remove(HomePlace.Folder(0), clock, installed))
        assertEquals(HomeApps(ring = Ring(listOf(folder(clock), folder(maps))), dock = homeApps.dock), homeApps.remove(HomePlace.Folder(1), mail, installed))
        assertEquals(HomeApps(ring = homeApps.ring), homeApps.remove(HomePlace.Dock, clock, installed))
    }

    @Test
    fun `an app folded onto a ring app makes a folder of the two in that slot and leaves where it came from`() {
        val homeApps = HomeApps(ring = ringOf(clock, mail), dock = Favourites(listOf(maps.key)))
        val (shownClock, shownMail) = homeApps.ring.resolve(listOf(clock, mail))

        assertEquals(
            HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps)))),
            homeApps.move(maps, HomePlace.Dock, Landing.Into(shownMail), installed),
        )
        assertEquals(HomeApps(ring = Ring(listOf(folder(mail, clock))), dock = homeApps.dock), homeApps.move(clock, HomePlace.Ring, Landing.Into(shownMail), installed))
        assertEquals(
            HomeApps(ring = Ring(listOf(folder(clock, music), RingSlot.App(mail.key))), dock = homeApps.dock),
            homeApps.move(music, null, Landing.Into(shownClock), installed),
        )
    }

    @Test
    fun `an app folded into a folder joins it at the end once`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock, mail), RingSlot.App(maps.key))), dock = Favourites(listOf(mail.key)))
        val (work) = homeApps.ring.resolve(listOf(clock, mail, maps))

        assertEquals(Ring(listOf(folder(clock, mail, maps))), homeApps.move(maps, HomePlace.Ring, Landing.Into(work), installed).ring)
        assertEquals(HomeApps(ring = homeApps.ring), homeApps.move(mail, HomePlace.Dock, Landing.Into(work), installed))
        assertEquals(homeApps, homeApps.move(clock, HomePlace.Folder(0), Landing.Into(work), installed))
    }

    // The target folder is named by the slot it had before the emptied one ahead of it went.
    @Test
    fun `an app out of a folder takes the folder it empties with it and lands by the slots as they were`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock), RingSlot.App(mail.key), folder(maps))))
        val (_, _, games) = homeApps.ring.resolve(listOf(clock, mail, maps))

        assertEquals(
            Ring(listOf(RingSlot.App(mail.key), RingSlot.App(clock.key), folder(maps))),
            homeApps.move(clock, HomePlace.Folder(0), Landing.OnRing(games, ReorderMode.Insert), installed).ring,
        )
        assertEquals(Ring(listOf(RingSlot.App(mail.key), folder(maps, clock))), homeApps.move(clock, HomePlace.Folder(0), Landing.Into(games), installed).ring)
    }

    @Test
    fun `a folder goes once the last app it shows leaves, whatever missing apps it holds`() {
        val gone = app("Gone")
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock, gone), folder(mail, maps))))

        assertEquals(Ring(listOf(folder(mail, maps))), homeApps.remove(HomePlace.Folder(0), clock, installed).ring)
        assertEquals(
            HomeApps(ring = Ring(listOf(folder(mail, maps))), dock = Favourites(listOf(clock.key))),
            homeApps.move(clock, HomePlace.Folder(0), Landing.InDock(null, ReorderMode.Insert), installed),
        )
        // Taking a missing app out empties nothing the user can see.
        assertEquals(Ring(listOf(folder(clock), folder(mail, maps))), homeApps.remove(HomePlace.Folder(0), gone, installed).ring)
    }

    @Test
    fun `an app lands at the ring's end without a target, and by the folder it left changes nothing`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock, mail), RingSlot.App(maps.key))), dock = Favourites(listOf(music.key)))
        val (work) = homeApps.ring.resolve(installed)

        assertEquals(
            HomeApps(ring = Ring(listOf(folder(clock, mail), RingSlot.App(maps.key), RingSlot.App(music.key)))),
            homeApps.move(music, HomePlace.Dock, Landing.OnRing(null, ReorderMode.Swap), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(maps)),
            HomeApps(dock = Favourites(listOf(maps.key))).move(maps, HomePlace.Dock, Landing.OnRing(null, ReorderMode.Insert), installed),
        )
        assertEquals(homeApps, homeApps.move(clock, HomePlace.Folder(0), Landing.OnRing(work, ReorderMode.Insert), installed))
        assertEquals(homeApps, homeApps.move(clock, HomePlace.Folder(0), Landing.OnRing(work, ReorderMode.Swap), installed))
    }

    @Test
    fun `the ring and the dock trade apps, inserted or swapped`() {
        val homeApps = HomeApps(ring = ringOf(clock, mail), dock = Favourites(listOf(maps.key, music.key)))
        val (_, shownMail) = homeApps.ring.resolve(listOf(clock, mail))

        assertEquals(
            HomeApps(ring = ringOf(clock, maps, mail), dock = Favourites(listOf(music.key))),
            homeApps.move(maps, HomePlace.Dock, Landing.OnRing(shownMail, ReorderMode.Insert), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(clock, maps), dock = Favourites(listOf(mail.key, music.key))),
            homeApps.move(maps, HomePlace.Dock, Landing.OnRing(shownMail, ReorderMode.Swap), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(mail), dock = Favourites(listOf(maps.key, clock.key, music.key))),
            homeApps.move(clock, HomePlace.Ring, Landing.InDock(music, ReorderMode.Insert), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(mail), dock = Favourites(listOf(maps.key, music.key, clock.key))),
            homeApps.move(clock, HomePlace.Ring, Landing.InDock(null, ReorderMode.Swap), installed),
        )
    }

    @Test
    fun `an app landing in a place that has it moves there rather than doubling`() {
        val homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), folder(mail), RingSlot.App(maps.key))), dock = Favourites(listOf(maps.key, clock.key)))
        val (shownClock, work) = homeApps.ring.resolve(listOf(clock, mail, maps))

        assertEquals(
            HomeApps(ring = Ring(listOf(RingSlot.App(maps.key), RingSlot.App(clock.key), folder(mail))), dock = Favourites(listOf(clock.key))),
            homeApps.move(maps, HomePlace.Dock, Landing.OnRing(shownClock, ReorderMode.Insert), installed),
        )
        // Swapped onto a folder, it goes in before it, as the folder cannot go to the dock.
        assertEquals(
            HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), RingSlot.App(maps.key), folder(mail))), dock = Favourites(listOf(clock.key))),
            homeApps.move(maps, HomePlace.Dock, Landing.OnRing(work, ReorderMode.Swap), installed),
        )
        assertEquals(
            HomeApps(ring = Ring(listOf(folder(mail), RingSlot.App(maps.key))), dock = Favourites(listOf(maps.key, clock.key))),
            homeApps.move(clock, HomePlace.Ring, Landing.InDock(null, ReorderMode.Insert), installed),
        )
    }

    @Test
    fun `a pin stays while its shortcut is anywhere on home, and an activity of the same name keeps none`() {
        val onRing = shortcut("web", "a")
        val inFolder = shortcut("web", "b")
        val docked = shortcut("maps", "c")
        val activity = AppEntry("Main", "chat", "Main")
        assertNotEquals(activity.key, shortcut("chat", "Main").key)
        val homeApps = HomeApps(
            ring = Ring(listOf(RingSlot.App(onRing.key), folder(inFolder), RingSlot.App(activity.key))),
            dock = Favourites(listOf(docked.key)),
        )

        val kept = homeApps.keptPins(mapOf("web" to listOf("a", "b", "gone"), "maps" to listOf("c"), "chat" to listOf("Main")))

        assertEquals(mapOf("web" to listOf("a", "b"), "chat" to emptyList<String>()), kept)
    }

    private fun shortcut(packageName: String, id: String) = AppEntry(id, packageName, "Main", shortcutId = id)
}
