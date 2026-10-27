package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrailsTest {
    private val park = LatLon(40.7536, -73.9832)
    private val library = LatLon(40.7532, -73.9822)
    private val station = LatLon(40.7527, -73.9772)

    @Test
    fun aTrailGrowsOnlyWhileLoggedAndOneThatWentNowhereIsDropped() {
        val one = Trails().logged(park).started().logged(park).logged(library).stopped().logged(station)

        assertEquals(Trails(listOf(listOf(park, library))), one)
        assertEquals(one, one.started().logged(station).stopped())
    }

    @Test
    fun startingWhileLoggingCarriesOnTheSameTrailApartFromTheFinishedOnes() {
        val trails = Trails(listOf(listOf(park, station))).started().logged(park).started().logged(library)

        assertTrue(trails.logging)
        assertEquals(listOf(listOf(park, station)), trails.finished)
        assertEquals(listOf(park, library), trails.growing)
        assertNull(trails.stopped().growing)
    }

    @Test
    fun readsBackWhatItWroteToAMillionthOfADegreeAndSkipsWhatItCannotRead() {
        val trails = listOf(listOf(park, library), listOf(library, station, park))

        assertEquals(trails, Trails.parse(Trails.encode(trails)))
        assertEquals(listOf(listOf(park, library)), Trails.parse(Trails.encode(listOf(listOf(LatLon(40.75360004, -73.98319996), library)))))
        assertEquals(emptyList<Trail>(), Trails.parse(""))
        assertEquals(listOf(listOf(park, library)), Trails.parse("40.7536,-73.9832 nonsense 40.7532,-73.9822;1,2"))
    }
}
