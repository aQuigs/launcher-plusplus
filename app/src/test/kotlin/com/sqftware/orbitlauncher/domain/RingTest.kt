package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")
    private val music = app("Music")
    private val all = listOf(clock, mail, maps, music)

    @Test
    fun `a new folder takes the app's slot and keeps the order round it`() {
        val ring = ringOf(clock, mail, maps).newFolder(mail)

        assertEquals(Ring(listOf(RingSlot.App(clock.key), folder(mail), RingSlot.App(maps.key))), ring)
        assertEquals(2, ring.indexOf(maps))
        assertEquals(-1, ring.indexOf(mail))
        assertEquals(ringOf(clock), ringOf(clock).newFolder(mail))
    }

    // The ring shows three items, but the moves are among the four stored slots.
    @Test
    fun `a move lands among the stored slots, past the one kept for a missing app`() {
        val gone = app("Gone")
        val ring = Ring(listOf(RingSlot.App(clock.key), RingSlot.App(gone.key), folder(mail, maps), RingSlot.App(music.key)))
        val (shownClock, shownFolder, shownMusic) = ring.resolve(all, HomePlace.Ring)

        assertEquals(
            Ring(listOf(RingSlot.App(gone.key), folder(mail, maps), RingSlot.App(clock.key), RingSlot.App(music.key))),
            ring.move(shownClock, shownFolder, ReorderMode.Insert),
        )
        assertEquals(
            Ring(listOf(RingSlot.App(clock.key), RingSlot.App(gone.key), RingSlot.App(music.key), folder(mail, maps))),
            ring.move(shownMusic, shownFolder, ReorderMode.Swap),
        )
    }

    @Test
    fun `adding puts an app in a slot of its own at the end, once`() {
        val ring = ringOf(clock).add(mail)

        assertEquals(ringOf(clock, mail), ring)
        assertEquals(ring, ring.add(mail))
        assertEquals(ring, ring.add(clock))
    }

    @Test
    fun `adding into a folder is a no-op for an app already in it`() {
        val ring = Ring(listOf(folder(mail))).add(0, maps)

        assertEquals(folder(mail, maps), ring.folder(0))
        assertEquals(ring, ring.add(0, mail))
        assertEquals(ring, ring.add(1, clock))
    }

    @Test
    fun `toggling into a folder adds at the end and toggling again takes out`() {
        val ring = Ring(listOf(folder(mail))).toggle(0, maps).toggle(0, clock)
        assertEquals(folder(mail, maps, clock), ring.folder(0))

        assertEquals(folder(mail, clock), ring.toggle(0, maps).folder(0))
    }

    @Test
    fun `a folder stays with one app or none until it is removed`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps)))

        val one = ring.toggle(1, maps)
        assertEquals(Ring(listOf(RingSlot.App(clock.key), folder(mail))), one)
        val empty = one.toggle(1, mail)
        assertEquals(Ring(listOf(RingSlot.App(clock.key), folder())), empty)
        assertEquals(ringOf(clock), empty.remove(1))
    }

    @Test
    fun `a folder is removed if it shows no app, and nothing else is`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder(), folder(mail), folder(maps)))
        val installed = setOf(clock.key, mail.key)

        assertEquals(Ring(listOf(RingSlot.App(clock.key), folder(mail), folder(maps))), ring.removeIfEmpty(1, installed))
        assertEquals(Ring(listOf(RingSlot.App(clock.key), folder(), folder(mail))), ring.removeIfEmpty(3, installed))
        assertEquals(ring, ring.removeIfEmpty(0, installed))
        assertEquals(ring, ring.removeIfEmpty(2, installed))
    }

    @Test
    fun `toggling into a slot that is not a folder changes nothing`() {
        val ring = ringOf(clock)

        assertEquals(ring, ring.toggle(0, mail))
        assertEquals(ring, ring.toggle(3, mail))
    }

    @Test
    fun `an app inside a folder is not on the ring itself`() {
        val ring = Ring(listOf(folder(mail), RingSlot.App(clock.key)))

        assertTrue(clock in ring.apps)
        assertTrue(mail !in ring.apps)
        assertEquals(listOf(folder(mail), RingSlot.App(clock.key), RingSlot.App(mail.key)), ring.toggle(mail).slots)
    }

    @Test
    fun `removing acts on the one slot`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps)))

        assertEquals(ringOf(clock), ring.remove(1))
        assertEquals(Ring(listOf(folder(mail, maps))), ring.remove(0))
    }

    @Test
    fun `a missing app is skipped but kept, in a folder too`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps)))

        assertEquals(listOf(RingItem.App(clock), RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(maps))), ring.resolve(listOf(clock, maps), HomePlace.Ring))
        assertEquals(listOf(RingItem.App(clock), RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(mail, maps))), ring.resolve(all, HomePlace.Ring))
    }

    @Test
    fun `a folder with nothing to show still holds its slot`() {
        val ring = Ring(listOf(folder(mail, maps), folder(), folder(music)))

        assertEquals(
            listOf(RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 0), emptyList()), RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), emptyList()), RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 2), listOf(music))),
            ring.resolve(listOf(clock, music), HomePlace.Ring),
        )
    }

    @Test
    fun `a ring of folders only is not empty`() {
        assertTrue(Ring(listOf(folder(mail))).isEmpty.not())
        assertTrue(Ring(listOf(folder())).isEmpty.not())
        assertTrue(Ring().isEmpty)
    }
}
