package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalTest {
    @Test
    fun discoversAPlaceOnlyTheFirstTime() {
        val quest = LatLon(40.75, -73.98)
        val journal = Journal(quest = quest).discovering("Midtown South")!!

        assertEquals(setOf("Midtown South"), journal.discovered)
        assertEquals(quest, journal.quest)
        assertNull(journal.discovering("Midtown South"))
        assertNull(journal.discovering(null))
        assertEquals(setOf("Midtown South", "Kips Bay"), journal.discovering("Kips Bay")!!.discovered)
    }
}
