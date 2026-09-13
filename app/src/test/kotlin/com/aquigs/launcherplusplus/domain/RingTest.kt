package com.aquigs.launcherplusplus.domain

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
        val ring = ringOf(clock, mail, maps).newFolder(mail, "Work")

        assertEquals(Ring(listOf(RingSlot.App(clock.key), folder("Work", mail), RingSlot.App(maps.key))), ring)
        assertEquals(2, ring.indexOf(maps))
        assertEquals(-1, ring.indexOf(mail))
        assertEquals(ringOf(clock), ringOf(clock).newFolder(mail, "Work"))
    }

    @Test
    fun `toggling into a folder adds at the end and toggling again takes out`() {
        val ring = Ring(listOf(folder("Work", mail))).toggle(0, maps).toggle(0, clock)
        assertEquals(folder("Work", mail, maps, clock), ring.folder(0))

        assertEquals(folder("Work", mail, clock), ring.toggle(0, maps).folder(0))
    }

    @Test
    fun `toggling into a slot that is not a folder changes nothing`() {
        val ring = ringOf(clock)

        assertEquals(ring, ring.toggle(0, mail))
        assertEquals(ring, ring.toggle(3, mail))
        assertEquals(ring, ring.rename(0, "Work"))
    }

    @Test
    fun `an app inside a folder is not on the ring itself`() {
        val ring = Ring(listOf(folder("Work", mail), RingSlot.App(clock.key)))

        assertTrue(clock in ring.apps)
        assertTrue(mail !in ring.apps)
        assertEquals(listOf(folder("Work", mail), RingSlot.App(clock.key), RingSlot.App(mail.key)), ring.toggle(mail).slots)
    }

    @Test
    fun `renaming and removing act on the one slot`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder("Work", mail, maps)))

        assertEquals(folder("Play", mail, maps), ring.rename(1, "Play").folder(1))
        assertEquals(ringOf(clock), ring.remove(1))
        assertEquals(Ring(listOf(folder("Work", mail, maps))), ring.remove(0))
    }

    @Test
    fun `dissolving turns a folder of one into its app and drops an empty one`() {
        val ring = Ring(listOf(folder("Solo", clock), folder("Work", mail, maps), folder("Empty"), RingSlot.App(music.key)))

        assertEquals(listOf(RingSlot.App(clock.key), folder("Work", mail, maps), RingSlot.App(music.key)), ring.dissolved().slots)
    }

    @Test
    fun `a missing app is skipped but kept, in a folder too`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder("Work", mail, maps)))

        assertEquals(listOf(RingItem.App(clock), RingItem.Folder(1, "Work", listOf(maps))), ring.resolve(listOf(clock, maps)))
        assertEquals(listOf(RingItem.App(clock), RingItem.Folder(1, "Work", listOf(mail, maps))), ring.resolve(all))
        assertEquals(ring, ring.dissolved())
    }

    @Test
    fun `a folder none of whose apps is installed is skipped and keeps its slot number`() {
        val ring = Ring(listOf(folder("Work", mail, maps), folder("Play", music)))

        assertEquals(listOf(RingItem.Folder(1, "Play", listOf(music))), ring.resolve(listOf(clock, music)))
    }

    @Test
    fun `a ring of folders only is not empty`() {
        assertTrue(Ring(listOf(folder("Work", mail))).isEmpty.not())
        assertTrue(Ring().isEmpty)
    }
}
