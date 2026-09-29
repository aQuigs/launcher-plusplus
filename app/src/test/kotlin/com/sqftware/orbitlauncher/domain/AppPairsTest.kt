package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPairsTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")
    private val clockMail = pairOf(clock, mail)!!

    @Test
    fun `a pair is named after both apps, the first on top`() {
        assertEquals("Clock | Mail", clockMail.label)
        assertEquals("Mail | Clock", pairOf(mail, clock)!!.label)
    }

    @Test
    fun `a pair's key splits back into its apps' keys and passes for no other kind of key`() {
        assertEquals(clock.key to mail.key, pairKeys(clockMail.key))
        assertTrue(isStorable(clockMail.key))
        assertNull(pairKeys(clock.key))
        assertNull(pairKeys(shortcutKey(clock.packageName, "alarm")))
    }

    @Test
    fun `only plain apps of two packages make a pair`() {
        val shortcut = clock.copy(kind = EntryKind.Shortcut("alarm"))
        val stopwatch = AppEntry("Stopwatch", clock.packageName, "${clock.packageName}.Stopwatch")

        assertNull(pairOf(clock, clock))
        assertNull(pairOf(clock, stopwatch))
        assertNull(pairOf(shortcut, mail))
        assertNull(pairOf(clockMail, maps))
    }

    @Test
    fun `a pair resolves wherever a key does, in a slot, a folder or on a card, while both its apps are there`() {
        val ring = Ring(listOf(RingSlot.App(clockMail.key), RingSlot.Folder(listOf(clockMail.key, maps.key))))
        val card = Favourites(listOf(clockMail.key, maps.key))

        assertEquals(
            listOf(RingItem.App(clockMail), RingItem.Folder(HomePlace.Folder(HomePlace.Ring, 1), listOf(clockMail, maps))),
            ring.resolve(listOf(clock, mail, maps), HomePlace.Ring),
        )
        assertEquals(listOf(clockMail, maps), card.resolve(listOf(clock, mail, maps)))
        assertEquals(listOf(maps), card.resolve(listOf(clock, maps)))
    }
}
