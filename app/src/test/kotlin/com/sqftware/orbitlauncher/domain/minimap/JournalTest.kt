package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalTest {
    private val midtown = Place("Midtown", LatLon(40.7549, -73.9840))

    @Test
    fun discoversAPlaceOnlyTheFirstTime() {
        val quest = LatLon(40.75, -73.98)
        val journal = Journal(quest = quest).discovering(midtown)!!

        assertEquals(quest, journal.quest)
        assertNull(journal.discovering(midtown))
        assertNull(journal.discovering(Place("Midtown", LatLon(40.75491, -73.98402))))
        assertNull(journal.discovering(null))
        assertEquals(2, journal.discovering(Place("Kips Bay", LatLon(40.7423, -73.9801)))!!.discovered.size)
    }

    @Test
    fun clearsTheQuestOnlyOnceTheUserIsThere() {
        val quest = LatLon(40.7562, -73.9838)
        val journal = Journal(quest = quest, discovered = setOf("Midtown"))

        assertNull(journal.arriving(LatLon(40.7536, -73.9832)))
        assertEquals(Journal(discovered = setOf("Midtown")), journal.arriving(LatLon(40.7559, -73.9838)))
        assertNull(Journal().arriving(quest))
    }

    @Test
    fun aPlaceOfTheSameNameInAnotherTownIsNews() {
        val journal = Journal().discovering(Place("Chinatown", LatLon(40.7158, -73.9970)))!!

        assertEquals(2, journal.discovering(Place("Chinatown", LatLon(37.7941, -122.4078)))!!.discovered.size)
    }
}
