package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAppsTest {
    private val clock = app("Clock")
    private val mail = app("Mail")

    @Test
    fun `toggling one place leaves the other as it is`() {
        val homeApps = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, mail)

        assertEquals(listOf(clock), homeApps.ring.resolve(listOf(clock, mail)))
        assertEquals(listOf(mail), homeApps.dock.resolve(listOf(clock, mail)))
    }

    @Test
    fun `an app can be on the ring and in the dock at once`() {
        val homeApps = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, clock)

        assertTrue(clock in homeApps[HomePlace.Ring])
        assertTrue(clock in homeApps[HomePlace.Dock])
    }

    @Test
    fun `taking an app out of the dock keeps it on the ring`() {
        val homeApps = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, clock).toggle(HomePlace.Dock, clock)

        assertTrue(clock in homeApps.ring)
        assertEquals(Favourites(), homeApps.dock)
    }
}
