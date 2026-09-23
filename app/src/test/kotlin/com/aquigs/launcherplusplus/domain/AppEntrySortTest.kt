package com.aquigs.launcherplusplus.domain

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
    fun `a label is shared when apps from two packages go by it, whatever its case`() {
        val apps = listOf(app("Authenticator", "com.google.auth"), app("authenticator", "com.azure.auth"), app("Clock"))

        assertEquals(setOf("authenticator"), apps.sharedLabels())
    }

    @Test
    fun `two activities of one package do not share a label`() {
        val apps = listOf(AppEntry("Tools", "one.pkg", "one.pkg.A"), AppEntry("Tools", "one.pkg", "one.pkg.B"))

        assertEquals(emptySet<String>(), apps.sharedLabels())
    }
}
