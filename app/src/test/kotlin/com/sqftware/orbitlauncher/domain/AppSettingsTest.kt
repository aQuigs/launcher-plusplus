package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    private val mail = app("Mail")
    private val compose = AppEntry("Compose", mail.packageName, "${mail.packageName}.Compose")
    private val maps = app("Maps")

    @Test
    fun `a badge turned off hides the counts of every activity of its app, and turned on again shows them`() {
        val unread = UnreadCounts(mapOf(mail.packageName to 3, maps.packageName to 1), setOf(mail.packageName, maps.packageName))
        val off = AppSettings().toggleBadge(compose)

        assertTrue(off.isBadgeOff(mail))
        assertEquals(UnreadCounts(mapOf(maps.packageName to 1), setOf(maps.packageName)), off.badges(unread))
        assertEquals(unread, off.toggleBadge(mail).badges(unread))
    }
}
