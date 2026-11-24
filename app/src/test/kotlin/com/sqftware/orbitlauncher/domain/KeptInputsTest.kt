package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class KeptInputsTest {
    private val home = PlaceValue("Home", "2 Elm St")
    private val work = PlaceValue("Work", "1 Main St")

    @Test
    fun `a value picked goes to the top of the recent ones, once, and the oldest drops off`() {
        val recent = List(RECENT_INPUTS) { PlaceValue("$it", "$it Oak St") }

        assertEquals(listOf(home) + recent.dropLast(1), KeptInputs(recent = recent).picked(home).recent)
        assertEquals(listOf(home, work), KeptInputs(recent = listOf(work, home)).picked(home).recent)
    }

    @Test
    fun `a favorite is not recent too, however it is picked`() {
        val kept = KeptInputs(favorites = listOf(work), recent = listOf(home)).toggledFavorite(home)

        assertEquals(KeptInputs(favorites = listOf(work, home)), kept)
        assertEquals(kept, kept.picked(home))
    }

    @Test
    fun `a favorite no more tops the recent ones`() {
        assertEquals(KeptInputs(recent = listOf(work, home)), KeptInputs(listOf(work), listOf(home)).toggledFavorite(work))
    }

    @Test
    fun `places that share an address, as parks with no street do, are kept apart`() {
        val field = PlaceValue("Crissy Field", "San Francisco, California", 37.804, -122.465)
        val fort = PlaceValue("Fort Mason", "San Francisco, California", 37.806, -122.430)

        assertEquals(KeptInputs(favorites = listOf(field, fort)), KeptInputs(favorites = listOf(field)).toggledFavorite(fort))
        assertEquals(KeptInputs(recent = listOf(fort, field)), KeptInputs(recent = listOf(field)).picked(fort))
    }

    @Test
    fun `a value forgotten leaves the recent ones`() {
        assertEquals(KeptInputs(recent = listOf(work)), KeptInputs(recent = listOf(home, work)).forgotten(home))
    }
}
