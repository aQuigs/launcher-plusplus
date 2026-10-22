package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhereIAmTest {
    @Test
    fun aMapShownWithoutAccessIsRefused() {
        assertEquals(WhereIAm.Refused, WhereIAm.Shown.withGrant(false))
        assertEquals(WhereIAm.Shown, WhereIAm.Shown.withGrant(true))
    }

    @Test
    fun aRefusalUndoneInSettingsShowsTheMap() {
        assertEquals(WhereIAm.Shown, WhereIAm.Refused.withGrant(true))
        assertEquals(WhereIAm.Refused, WhereIAm.Refused.withGrant(false))
    }

    @Test
    fun aMapTurnedOffStaysOffWhateverTheAccess() {
        assertEquals(WhereIAm.Hidden, WhereIAm.Hidden.withGrant(true))
        assertEquals(WhereIAm.Hidden, WhereIAm.Hidden.withGrant(false))
    }

    @Test
    fun anAnswerShowsTheMapOrLeavesItRefused() {
        assertEquals(WhereIAm.Shown, WhereIAm.answered(true))
        assertEquals(WhereIAm.Refused, WhereIAm.answered(false))
    }

    @Test
    fun pickingAThemeAsksUnlessTheUserTurnedTheMapOff() {
        assertTrue(WhereIAm.Shown.asksOnPick)
        assertTrue(WhereIAm.Refused.asksOnPick)
        assertFalse(WhereIAm.Hidden.asksOnPick)
    }
}
