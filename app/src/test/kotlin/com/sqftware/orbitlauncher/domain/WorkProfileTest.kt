package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkProfileTest {
    private val chrome = app("Chrome")
    private val workChrome = chrome.copy(workProfile = 10)

    @Test
    fun `a work app's key is its own, storable, and no pair's`() {
        assertNotEquals(chrome.key, workChrome.key)
        assertNotEquals(workChrome.key, chrome.copy(workProfile = 11).key)
        assertTrue(isStorable(workChrome.key))
        assertNull(pairKeys(workChrome.key))
        assertEquals(workChrome, AppsByKey(listOf(chrome, workChrome))[workChrome.key])
    }

    @Test
    fun `a work app makes no pair, so its menu offers none`() {
        assertNull(pairOf(workChrome, app("Mail")))
        assertNull(pairOf(app("Mail"), workChrome))
        assertFalse(AppOption.SplitWith in appOptions(workChrome, AppSpot.Drawer, HomeApps(), AppSettings(), badgesEnabled = true, hasStorePage = false))
    }

    @Test
    fun `the two copies of an app keep their own badges, settings and usage`() {
        assertNotEquals(chrome.badgePackages, workChrome.badgePackages)
        assertFalse(AppSettings().toggleBadge(workChrome).isBadgeOff(chrome))
        assertEquals(0L, ForegroundTime(mapOf(chrome.packageName to 10L)).of(workChrome))
    }

    @Test
    fun `the user's own copy and the work one of an app do not count as sharing a label`() {
        assertEquals(emptySet<String>(), listOf(chrome, workChrome).keysWithSharedLabels())
    }
}
