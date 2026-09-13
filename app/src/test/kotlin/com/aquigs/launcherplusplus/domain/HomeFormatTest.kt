package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeFormatTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")

    @Test
    fun `a ring of apps and folders comes back as it went`() {
        val ring = Ring(listOf(RingSlot.App(clock.key), folder("Work", mail, maps), folder("Solo", clock), folder("Empty")))

        assertEquals(ring, decodeRing(ring.encode()))
    }

    @Test
    fun `a ring stored before folders is one app per line`() {
        assertEquals(ringOf(clock, mail), decodeRing("pkg.Clock/pkg.Clock.Main\npkg.Mail/pkg.Mail.Main"))
        assertEquals(Ring(), decodeRing(""))
    }

    @Test
    fun `a folder line is its name and its keys separated by tabs`() {
        val ring = ringOf(clock, mail).newFolder(mail, "Work").toggle(1, maps)

        assertEquals("pkg.Clock/pkg.Clock.Main\nWork\tpkg.Mail/pkg.Mail.Main\tpkg.Maps/pkg.Maps.Main", ring.encode())
        assertEquals(Ring(listOf(folder("Work", mail))), decodeRing("Work\tpkg.Mail/pkg.Mail.Main"))
    }

    @Test
    fun `a name cannot break the line it is on`() {
        val ring = Ring(listOf(folder("Work\tand\nplay", mail), RingSlot.App(clock.key)))

        assertEquals(Ring(listOf(folder("Work and play", mail), RingSlot.App(clock.key))), decodeRing(ring.encode()))
    }

    @Test
    fun `favourites are one key per line`() {
        val favourites = Favourites(listOf(clock.key, mail.key))

        assertEquals("pkg.Clock/pkg.Clock.Main\npkg.Mail/pkg.Mail.Main", favourites.encode())
        assertEquals(favourites, decodeFavourites(favourites.encode()))
        assertEquals(Favourites(), decodeFavourites(""))
    }
}
