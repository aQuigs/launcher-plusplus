package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFormatTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")

    @Test
    fun `a ring of apps and folders comes back as it went`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder(mail, maps), folder(clock), folder()))

        assertEquals(ring, decodeRing(ring.encode()))
    }

    @Test
    fun `a ring stored before folders is one app per line`() {
        assertEquals(ringOf(clock, mail), decodeRing("pkg.Clock/pkg.Clock.Main\npkg.Mail/pkg.Mail.Main"))
        assertEquals(Ring(), decodeRing(""))
    }

    @Test
    fun `a folder line is a tab and then its keys, so a folder of one or none is still a folder`() {
        val ring = ringOf(clock, mail).newFolder(mail).toggle(1, maps)

        assertEquals("pkg.Clock/pkg.Clock.Main\n\tpkg.Mail/pkg.Mail.Main\tpkg.Maps/pkg.Maps.Main", ring.encode())
        assertEquals("\tpkg.Mail/pkg.Mail.Main", Ring(listOf(folder(mail))).encode())
        assertEquals("\t", Ring(listOf(folder())).encode())
        assertEquals(Ring(listOf(folder(mail))), decodeRing("\tpkg.Mail/pkg.Mail.Main"))
        assertEquals(Ring(listOf(folder())), decodeRing("\t"))
    }

    @Test
    fun `a folder line stored with a name loses the name and keeps the apps`() {
        val named = "Work\tpkg.Mail/pkg.Mail.Main\tpkg.Maps/pkg.Maps.Main\nEmpty\t"

        assertEquals(Ring(listOf(folder(mail, maps), folder())), decodeRing(named))
    }

    @Test
    fun `favourites are one key per line`() {
        val favourites = Favourites(listOf(clock.key, mail.key))

        assertEquals("pkg.Clock/pkg.Clock.Main\npkg.Mail/pkg.Mail.Main", favourites.encode())
        assertEquals(favourites, decodeFavourites(favourites.encode()))
        assertEquals(Favourites(), decodeFavourites(""))
    }

    @Test
    fun `a shortcut whose id holds a line break or a tab cannot be stored`() {
        assertTrue(isStorable(shortcutKey("web", "da8ed822-1ea0")))
        assertFalse(isStorable(shortcutKey("web", "two\nlines")))
        assertFalse(isStorable(shortcutKey("web", "tab\tbed")))
    }
}
