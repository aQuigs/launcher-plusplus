package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    private val apps = listOf(
        app("7zip"),
        app("Amazon Photos"),
        app("Éclair"),
        app("Gmail"),
        app("Google Maps"),
        app("Mail"),
        app("Photos"),
        app("Zip It"),
    )

    private fun labels(query: String) = apps.matching(query).map { it.label }

    @Test
    fun `words starting with the query come before labels merely containing it`() {
        assertEquals(listOf("Mail", "Google Maps", "Amazon Photos", "Gmail"), labels("ma"))
    }

    @Test
    fun `a label starting with the query beats one whose later word does`() {
        assertEquals(listOf("Photos", "Amazon Photos"), labels("photos"))
    }

    @Test
    fun `a digit before the query is not a word start`() {
        assertEquals(listOf("Zip It", "7zip"), labels("zip"))
    }

    @Test
    fun `case and accents do not matter`() {
        assertEquals(listOf("Éclair"), labels("ECLAIR"))
        assertEquals(listOf("Gmail"), labels("GMAIL"))
    }

    @Test
    fun `a blank query matches every app in order and spaces round it are ignored`() {
        assertEquals(apps, apps.matching("   "))
        assertEquals(listOf("Mail", "Gmail"), labels(" mail "))
    }

    @Test
    fun `nothing matches nothing`() {
        assertEquals(emptyList<String>(), labels("xyz"))
    }
}
