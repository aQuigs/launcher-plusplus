package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    private val mail = app("Mail")
    private val compose = AppEntry("Compose", mail.packageName, "${mail.packageName}.Compose")
    private val maps = app("Maps")

    @Test
    fun `a badge turned off hides the counts of every activity of its app, and turned on again shows them`() {
        val unread = UnreadCounts(mapOf(mail.packageName to 3, maps.packageName to 1))
        val off = AppSettings().toggleBadge(compose)

        assertEquals(0, off.badges(unread)[mail])
        assertEquals(1, off.badges(unread)[maps])
        assertEquals(unread, off.toggleBadge(mail).badges(unread))
    }

    @Test
    fun `an app left off the built-in cards goes with all its activities, and comes back when put back`() {
        val off = AppSettings().toggleBuiltInCards(mail)

        assertEquals(listOf(maps), off.forBuiltInCards(listOf(mail, compose, maps)))
        assertEquals(listOf(mail, maps), off.toggleBuiltInCards(mail).forBuiltInCards(listOf(mail, maps)))
    }
}
