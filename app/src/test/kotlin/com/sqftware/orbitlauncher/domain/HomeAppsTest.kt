package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAppsTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")
    private val music = app("Music")
    private val installed = setOf(clock.key, mail.key, maps.key, music.key)

    @Test
    fun `toggling one place leaves the other as it is`() {
        val homeApps = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, mail)

        assertEquals(listOf(RingItem.App(clock)), homeApps.ring.resolve(listOf(clock, mail), HomePlace.Ring))
        assertEquals(listOf(RingItem.App(mail)), homeApps.dock.resolve(listOf(clock, mail), HomePlace.Dock))
    }

    @Test
    fun `an app can be in both places and leave one without the other`() {
        val both = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, clock)
        assertTrue(clock in both[HomePlace.Ring])
        assertTrue(clock in both[HomePlace.Dock])

        val ringOnly = both.toggle(HomePlace.Dock, clock)

        assertTrue(clock in ringOnly.ring.apps)
        assertEquals(Ring(), ringOnly.dock)
    }

    @Test
    fun `adding to a place is a no-op once the app is there`() {
        val homeApps = HomeApps().add(HomePlace.Ring, clock).add(HomePlace.Dock, clock).add(HomePlace.Dock, mail)

        assertEquals(HomeApps(ring = ringOf(clock), dock = ringOf(clock, mail)), homeApps)
        assertEquals(homeApps, homeApps.add(HomePlace.Ring, clock))
        assertEquals(homeApps, homeApps.add(HomePlace.Dock, mail))
        assertEquals(homeApps, homeApps.add(HomePlace.Folder(HomePlace.Ring, 0), maps))
    }

    @Test
    fun `adding to a folder fills that folder only`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock)))).add(HomePlace.Folder(HomePlace.Ring, 0), mail)

        assertEquals(HomeApps(ring = Ring(listOf(folder(clock, mail)))), homeApps)
    }

    @Test
    fun `a folder is a place of its own on the ring`() {
        val homeApps = HomeApps(ring = ringOf(clock).newFolder(clock)).toggle(HomePlace.Folder(HomePlace.Ring, 0), mail)

        assertEquals(Favourites(listOf(clock.key, mail.key)), homeApps[HomePlace.Folder(HomePlace.Ring, 0)])
        assertTrue(clock !in homeApps[HomePlace.Ring])
        assertEquals(Favourites(), homeApps[HomePlace.Folder(HomePlace.Ring, 1)])
        assertEquals(homeApps, homeApps.toggle(HomePlace.Folder(HomePlace.Ring, 1), maps))
    }

    @Test
    fun `taking the last app out of a folder leaves the folder`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock))), dock = ringOf(mail))

        val emptied = homeApps.toggle(HomePlace.Folder(HomePlace.Ring, 0), clock)

        assertEquals(HomeApps(ring = Ring(listOf(folder())), dock = ringOf(mail)), emptied)
    }

    @Test
    fun `an app folded onto a ring app makes a folder of the two in that slot and leaves where it came from`() {
        val homeApps = HomeApps(ring = ringOf(clock, mail), dock = ringOf(maps))
        val (shownClock, shownMail) = homeApps.ring.resolve(listOf(clock, mail), HomePlace.Ring)

        assertEquals(
            HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps)))),
            homeApps.move(maps, HomePlace.Dock, Landing.Into(HomePlace.Ring, shownMail), installed),
        )
        assertEquals(HomeApps(ring = Ring(listOf(folder(mail, clock))), dock = homeApps.dock), homeApps.move(clock, HomePlace.Ring, Landing.Into(HomePlace.Ring, shownMail), installed))
        assertEquals(
            HomeApps(ring = Ring(listOf(folder(clock, music), RingSlot.App(mail.key))), dock = homeApps.dock),
            homeApps.move(music, null, Landing.Into(HomePlace.Ring, shownClock), installed),
        )
    }

    @Test
    fun `an app folded into a folder joins it at the end once`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock, mail), RingSlot.App(maps.key))), dock = ringOf(mail))
        val (work) = homeApps.ring.resolve(listOf(clock, mail, maps), HomePlace.Ring)

        assertEquals(Ring(listOf(folder(clock, mail, maps))), homeApps.move(maps, HomePlace.Ring, Landing.Into(HomePlace.Ring, work), installed).ring)
        assertEquals(HomeApps(ring = homeApps.ring), homeApps.move(mail, HomePlace.Dock, Landing.Into(HomePlace.Ring, work), installed))
        assertEquals(homeApps, homeApps.move(clock, HomePlace.Folder(HomePlace.Ring, 0), Landing.Into(HomePlace.Ring, work), installed))
    }

    // The target folder is named by the slot it had before the emptied one ahead of it went.
    @Test
    fun `an app out of a folder takes the folder it empties with it and lands by the slots as they were`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock), RingSlot.App(mail.key), folder(maps))))
        val (_, _, games) = homeApps.ring.resolve(listOf(clock, mail, maps), HomePlace.Ring)

        assertEquals(
            Ring(listOf(RingSlot.App(mail.key), RingSlot.App(clock.key), folder(maps))),
            homeApps.move(clock, HomePlace.Folder(HomePlace.Ring, 0), Landing.At(HomePlace.Ring, games, ReorderMode.Insert), installed).ring,
        )
        assertEquals(Ring(listOf(RingSlot.App(mail.key), folder(maps, clock))), homeApps.move(clock, HomePlace.Folder(HomePlace.Ring, 0), Landing.Into(HomePlace.Ring, games), installed).ring)
    }

    @Test
    fun `a folder goes once the last app it shows leaves, whatever missing apps it holds`() {
        val gone = app("Gone")
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock, gone), folder(mail, maps))), dock = ringOf(clock))
        val left = HomeApps(ring = Ring(listOf(folder(mail, maps))), dock = homeApps.dock)

        assertEquals(left, homeApps.remove(HomePlace.Folder(HomePlace.Ring, 0), clock, installed))
        assertEquals(left, homeApps.move(clock, HomePlace.Folder(HomePlace.Ring, 0), Landing.At(HomePlace.Dock, null, ReorderMode.Insert), installed))
        // Taking out a missing app, or one of two shown, empties nothing the user can see, and the dock's copy is apart.
        assertEquals(Ring(listOf(folder(clock), folder(mail, maps))), homeApps.remove(HomePlace.Folder(HomePlace.Ring, 0), gone, installed).ring)
        assertEquals(Ring(listOf(folder(clock, gone), folder(maps))), homeApps.remove(HomePlace.Folder(HomePlace.Ring, 1), mail, installed).ring)
        assertEquals(HomeApps(ring = homeApps.ring), homeApps.remove(HomePlace.Dock, clock, installed))
    }

    @Test
    fun `an app lands at the ring's end without a target, and before the folder it left, which it empties`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock, mail), RingSlot.App(maps.key))), dock = ringOf(music))
        val (work) = homeApps.ring.resolve(listOf(clock, mail, maps), HomePlace.Ring)

        assertEquals(
            HomeApps(ring = Ring(listOf(folder(clock, mail), RingSlot.App(maps.key), RingSlot.App(music.key)))),
            homeApps.move(music, HomePlace.Dock, Landing.At(HomePlace.Ring, null, ReorderMode.Swap), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(maps)),
            HomeApps(dock = ringOf(maps)).move(maps, HomePlace.Dock, Landing.At(HomePlace.Ring, null, ReorderMode.Insert), installed),
        )
        assertEquals(
            HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), folder(mail), RingSlot.App(maps.key))), dock = homeApps.dock),
            homeApps.move(clock, HomePlace.Folder(HomePlace.Ring, 0), Landing.At(HomePlace.Ring, work, ReorderMode.Swap), installed),
        )
    }

    @Test
    fun `an app lands neither in the place it is in, nor into itself or a copy, nor into the folder it left`() {
        val homeApps = HomeApps(ring = Ring(listOf(folder(clock, mail), RingSlot.App(maps.key))), dock = ringOf(maps))
        val (work, shownMaps) = homeApps.ring.resolve(listOf(clock, mail, maps), HomePlace.Ring)

        assertFalse(homeApps.lands(maps, HomePlace.Ring, Landing.At(HomePlace.Ring, null, ReorderMode.Insert)))
        assertFalse(homeApps.lands(maps, HomePlace.Dock, Landing.At(HomePlace.Dock, null, ReorderMode.Insert)))
        assertFalse(homeApps.lands(maps, HomePlace.Dock, Landing.Into(HomePlace.Ring, shownMaps)))
        assertFalse(homeApps.lands(clock, HomePlace.Folder(HomePlace.Ring, 0), Landing.Into(HomePlace.Ring, work)))
        assertTrue(homeApps.lands(maps, HomePlace.Dock, Landing.Into(HomePlace.Ring, work)))
        assertTrue(homeApps.lands(clock, HomePlace.Folder(HomePlace.Ring, 0), Landing.At(HomePlace.Dock, RingItem.App(maps), ReorderMode.Insert)))
    }

    @Test
    fun `the ring and the dock trade apps, inserted or swapped`() {
        val homeApps = HomeApps(ring = ringOf(clock, mail), dock = ringOf(maps, music))
        val (_, shownMail) = homeApps.ring.resolve(listOf(clock, mail), HomePlace.Ring)

        assertEquals(
            HomeApps(ring = ringOf(clock, maps, mail), dock = ringOf(music)),
            homeApps.move(maps, HomePlace.Dock, Landing.At(HomePlace.Ring, shownMail, ReorderMode.Insert), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(clock, maps), dock = ringOf(mail, music)),
            homeApps.move(maps, HomePlace.Dock, Landing.At(HomePlace.Ring, shownMail, ReorderMode.Swap), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(mail), dock = ringOf(maps, clock, music)),
            homeApps.move(clock, HomePlace.Ring, Landing.At(HomePlace.Dock, RingItem.App(music), ReorderMode.Insert), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(mail), dock = ringOf(maps, music, clock)),
            homeApps.move(clock, HomePlace.Ring, Landing.At(HomePlace.Dock, null, ReorderMode.Swap), installed),
        )
    }

    @Test
    fun `an app landing in a place that has it moves there rather than doubling`() {
        val homeApps = HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), folder(mail), RingSlot.App(maps.key))), dock = ringOf(maps, clock))
        val (shownClock, work) = homeApps.ring.resolve(listOf(clock, mail, maps), HomePlace.Ring)

        assertEquals(
            HomeApps(ring = Ring(listOf(RingSlot.App(maps.key), RingSlot.App(clock.key), folder(mail))), dock = ringOf(clock)),
            homeApps.move(maps, HomePlace.Dock, Landing.At(HomePlace.Ring, shownClock, ReorderMode.Insert), installed),
        )
        // Swapped onto a folder, it goes in before it, as the folder cannot go to the dock.
        assertEquals(
            HomeApps(ring = Ring(listOf(RingSlot.App(clock.key), RingSlot.App(maps.key), folder(mail))), dock = ringOf(clock)),
            homeApps.move(maps, HomePlace.Dock, Landing.At(HomePlace.Ring, work, ReorderMode.Swap), installed),
        )
        assertEquals(
            HomeApps(ring = Ring(listOf(folder(mail), RingSlot.App(maps.key))), dock = ringOf(maps, clock)),
            homeApps.move(clock, HomePlace.Ring, Landing.At(HomePlace.Dock, null, ReorderMode.Insert), installed),
        )
    }

    @Test
    fun `an app folded onto a dock app makes a folder of the two in that dock slot`() {
        val homeApps = HomeApps(ring = ringOf(maps), dock = ringOf(clock, mail))
        val (shownClock, shownMail) = homeApps.dock.resolve(listOf(clock, mail), HomePlace.Dock)

        assertEquals(
            HomeApps(dock = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps)))),
            homeApps.move(maps, HomePlace.Ring, Landing.Into(HomePlace.Dock, shownMail), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(maps), dock = Ring(listOf(folder(mail, clock)))),
            homeApps.move(clock, HomePlace.Dock, Landing.Into(HomePlace.Dock, shownMail), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(maps), dock = Ring(listOf(folder(clock, music), RingSlot.App(mail.key)))),
            homeApps.move(music, null, Landing.Into(HomePlace.Dock, shownClock), installed),
        )
    }

    @Test
    fun `a dock folder is a place of its own that takes apps in and goes once emptied by a move`() {
        val homeApps = HomeApps(ring = ringOf(maps), dock = Ring(listOf(RingSlot.App(clock.key), folder(mail))))
        val (_, work) = homeApps.dock.resolve(listOf(clock, mail), HomePlace.Dock)
        val inDock = HomePlace.Folder(HomePlace.Dock, 1)

        assertEquals(Favourites(listOf(mail.key)), homeApps[inDock])
        assertEquals(Favourites(), homeApps[HomePlace.Folder(HomePlace.Ring, 1)])
        assertEquals(Favourites(listOf(mail.key, music.key)), homeApps.toggle(inDock, music)[inDock])
        assertEquals(
            HomeApps(dock = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps)))),
            homeApps.move(maps, HomePlace.Ring, Landing.Into(HomePlace.Dock, work), installed),
        )
        assertEquals(
            HomeApps(ring = ringOf(maps, mail), dock = ringOf(clock)),
            homeApps.move(mail, inDock, Landing.At(HomePlace.Ring, null, ReorderMode.Insert), installed),
        )
        assertFalse(homeApps.lands(mail, inDock, Landing.Into(HomePlace.Dock, work)))
        // A folder is named by its place too: the dock's folder is no target on the ring.
        assertFalse(homeApps.lands(maps, HomePlace.Ring, Landing.Into(HomePlace.Ring, work)))
    }

    @Test
    fun `a folder is kept while only its own apps change, and not once another slot of its place comes, goes or moves`() {
        val homeApps = HomeApps(ring = ringOf(maps), dock = Ring(listOf(RingSlot.App(clock.key), folder(mail), folder(music))))
        val open = HomePlace.Folder(HomePlace.Dock, 1)

        assertTrue(homeApps.keeps(open, homeApps.toggle(open, maps)))
        assertTrue(homeApps.keeps(open, homeApps.toggle(HomePlace.Ring, clock)))
        assertFalse(homeApps.keeps(open, homeApps.remove(HomePlace.Dock, clock, installed)))
        assertFalse(homeApps.keeps(open, homeApps.add(HomePlace.Dock, maps)))
        assertFalse(homeApps.keeps(open, homeApps.copy(dock = Ring(listOf(RingSlot.App(clock.key), RingSlot.App(mail.key), folder(music))))))
    }

    @Test
    fun `a pin stays while its shortcut is anywhere on home, and an activity of the same name keeps none`() {
        val onRing = shortcut("web", "a")
        val inFolder = shortcut("web", "b")
        val docked = shortcut("maps", "c")
        val inDockFolder = shortcut("maps", "d")
        val activity = AppEntry("Main", "chat", "Main")
        assertNotEquals(activity.key, shortcut("chat", "Main").key)
        val homeApps = HomeApps(
            ring = Ring(listOf(RingSlot.App(onRing.key), folder(inFolder), RingSlot.App(activity.key))),
            dock = Ring(listOf(RingSlot.App(docked.key), folder(inDockFolder))),
        )

        val kept = homeApps.keptPins(mapOf("web" to listOf("a", "b", "gone"), "maps" to listOf("c", "d"), "chat" to listOf("Main")))

        assertEquals(mapOf("web" to listOf("a", "b"), "chat" to emptyList<String>()), kept)
    }

    private fun shortcut(packageName: String, id: String) = AppEntry(id, packageName, "Main", shortcutId = id)
}
