package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntrySortTest {
    @Test
    fun `sorts labels case-insensitively`() {
        val sorted = listOf(app("zebra"), app("Apple"), app("mango")).sortedByLabel()

        assertEquals(listOf("Apple", "mango", "zebra"), sorted.map { it.label })
    }

    @Test
    fun `equal labels fall back to package name`() {
        val sorted = listOf(app("Clock", "z.clock"), app("Clock", "a.clock")).sortedByLabel()

        assertEquals(listOf("a.clock", "z.clock"), sorted.map { it.packageName })
    }

    @Test
    fun `apps from two packages share a label whatever its case and surrounding spaces`() {
        val google = app("Authenticator", "com.google.auth")
        val microsoft = app("authenticator ", "com.azure.auth")

        assertEquals(setOf(google.key, microsoft.key), listOf(google, microsoft, app("Clock")).keysWithSharedLabels())
    }

    @Test
    fun `two activities of one package do not share a label`() {
        val apps = listOf(AppEntry("Tools", "one.pkg", "one.pkg.A"), AppEntry("Tools", "one.pkg", "one.pkg.B"))

        assertEquals(emptySet<String>(), apps.keysWithSharedLabels())
    }
}
