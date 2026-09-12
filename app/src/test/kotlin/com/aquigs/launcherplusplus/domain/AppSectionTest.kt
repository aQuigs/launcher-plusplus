package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSectionTest {
    private fun app(label: String) = AppEntry(label, "pkg.$label", "pkg.$label.Main")

    @Test
    fun `files apps under their upper-cased first letter`() {
        val sections = listOf(app("Calendar"), app("clock"), app("Mail")).sectionsByInitial()

        assertEquals(listOf('C', 'M'), sections.map { it.initial })
        assertEquals(listOf("Calendar", "clock"), sections[0].apps.map { it.label })
    }

    @Test
    fun `digits, symbols and other scripts go first under the hash`() {
        val sections = listOf(app("7zip"), app("Mail"), app("Éclair"), app("  Zip")).sectionsByInitial()

        assertEquals(listOf(OTHER_INITIAL, 'M', 'Z'), sections.map { it.initial })
        assertEquals(listOf("7zip", "Éclair"), sections[0].apps.map { it.label })
    }

    @Test
    fun `sections come out in rail order whatever the input order`() {
        val sections = listOf(app("Zip"), app("~tilde"), app("apple")).sectionsByInitial()

        assertEquals(listOf(OTHER_INITIAL, 'A', 'Z'), sections.map { it.initial })
    }

    @Test
    fun `blank and empty labels are filed under the hash`() {
        assertEquals(OTHER_INITIAL, app("").initial)
        assertEquals(OTHER_INITIAL, app("   ").initial)
    }

    @Test
    fun `no apps means no sections`() {
        assertEquals(emptyList<AppSection>(), emptyList<AppEntry>().sectionsByInitial())
    }
}
