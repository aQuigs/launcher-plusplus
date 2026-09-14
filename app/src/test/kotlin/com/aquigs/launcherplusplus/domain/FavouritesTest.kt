package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavouritesTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")

    @Test
    fun `toggling adds apps at the end`() {
        val favourites = Favourites().toggle(mail).toggle(clock)

        assertEquals(listOf(mail, clock), favourites.resolve(listOf(clock, mail, maps)))
    }

    @Test
    fun `toggling an app again takes it off`() {
        val favourites = Favourites().toggle(mail).toggle(clock).toggle(mail)

        assertEquals(listOf(clock), favourites.resolve(listOf(clock, mail)))
        assertFalse(mail in favourites)
    }

    @Test
    fun `adding an app already there changes nothing`() {
        val favourites = Favourites().add(mail).add(clock)

        assertEquals(listOf(mail, clock), favourites.resolve(listOf(clock, mail)))
        assertEquals(favourites, favourites.add(mail))
    }

    @Test
    fun `a missing app is skipped but keeps its place`() {
        val favourites = Favourites().toggle(mail).toggle(clock).toggle(maps)

        assertEquals(listOf(mail, maps), favourites.resolve(listOf(maps, mail)))
        assertEquals(listOf(mail, clock, maps), favourites.resolve(listOf(clock, maps, mail)))
    }

    @Test
    fun `apps are matched by package and activity, not by label`() {
        assertTrue(mail.copy(label = "Inbox") in Favourites().toggle(mail))
    }
}
