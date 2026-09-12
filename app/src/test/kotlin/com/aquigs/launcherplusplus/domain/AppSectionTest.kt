package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSectionTest {
    @Test
    fun `files apps under their upper-cased first letter`() {
        val sections = listOf(app("Calendar"), app("clock"), app("Mail")).sectionsByInitial()

        assertEquals(listOf('C', 'M'), sections.map { it.initial })
        assertEquals(listOf("Calendar", "clock"), sections[0].apps.map { it.label })
    }

    @Test
    fun `digits, symbols and other scripts go first under the hash`() {
        val sections = listOf(app("7zip"), app("Mail"), app("Яндекс"), app("  Zip")).sectionsByInitial()

        assertEquals(listOf(OTHER_INITIAL, 'M', 'Z'), sections.map { it.initial })
        assertEquals(listOf("7zip", "Яндекс"), sections[0].apps.map { it.label })
    }

    @Test
    fun `accented letters file under their base letter`() {
        assertEquals(listOf('E', 'O', 'N'), listOf(app("Éclair"), app("österreich"), app("Ñu")).map { it.initial })
    }

    @Test
    fun `invisible marks before the first letter are skipped`() {
        assertEquals('M', app("‎Mail").initial)
        assertEquals('M', app("﻿Mail").initial)
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
