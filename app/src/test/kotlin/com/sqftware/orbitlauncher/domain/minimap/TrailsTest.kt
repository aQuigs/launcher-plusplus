package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailsTest {
    private val park = LatLon(40.7536, -73.9832)
    private val library = LatLon(40.7532, -73.9822)
    private val station = LatLon(40.7527, -73.9772)

    @Test
    fun aTrailGrowsOnlyWhileLoggedAndOneThatWentNowhereIsDropped() {
        val one = Trails().logged(park).started().logged(park).logged(library).stopped().logged(station)

        assertEquals(Trails(listOf(Trail(listOf(park, library)))), one)
        assertEquals(one, one.started().logged(station).stopped())
    }

    @Test
    fun startingWhileLoggingCarriesOnTheSameTrail() {
        val trails = Trails().started().logged(park).started().logged(library)

        assertTrue(trails.logging)
        assertEquals(listOf(Trail(listOf(park, library))), trails.trails)
    }

    @Test
    fun readsBackWhatItWroteAsStoppedAndSkipsWhatItCannotRead() {
        val trails = Trails(listOf(Trail(listOf(park, library)), Trail(listOf(library, station, park))))
        val logging = trails.started().logged(station).logged(park)

        assertEquals(trails, Trails.parse(trails.encode()))
        assertFalse(Trails.parse(logging.encode()).logging)
        assertEquals(3, Trails.parse(logging.encode()).trails.size)
        assertEquals(Trails(), Trails.parse(""))
        assertEquals(Trails(listOf(Trail(listOf(park, library)))), Trails.parse("40.7536,-73.9832 nonsense 40.7532,-73.9822;1,2"))
    }
}
