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
    fun `an app can be in both places and leave one without the other`() {
        val both = HomeApps().toggle(HomePlace.Ring, clock).toggle(HomePlace.Dock, clock)
        assertTrue(clock in both[HomePlace.Ring])
        assertTrue(clock in both[HomePlace.Dock])

        val ringOnly = both.toggle(HomePlace.Dock, clock)

        assertTrue(clock in ringOnly.ring)
        assertEquals(Favourites(), ringOnly.dock)
    }
}
