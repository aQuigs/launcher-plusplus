package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAppsTest {
    private val clock = app("Clock")
    private val mail = app("Mail")
    private val maps = app("Maps")

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
}
