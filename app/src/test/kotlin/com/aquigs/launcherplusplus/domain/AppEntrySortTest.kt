package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppEntrySortTest {
    private fun app(label: String, packageName: String = "pkg.$label") = AppEntry(label, packageName, "$packageName.Main")

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
}
