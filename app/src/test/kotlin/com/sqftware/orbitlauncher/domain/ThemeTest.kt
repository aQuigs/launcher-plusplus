package com.sqftware.orbitlauncher.domain

import com.sqftware.orbitlauncher.domain.minimap.MapStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test
    fun `every folder look belongs to exactly one theme`() {
        assertEquals(FolderLook.entries.sorted(), Theme.entries.flatMap(Theme::folderLooks).sorted())
    }

    @Test
    fun `every colourway belongs to exactly one theme`() {
        assertEquals(Colourway.entries.sorted(), Theme.entries.flatMap(Theme::colourways).sorted())
    }

    @Test
    fun `each theme keeps its own pick and shows its first until one is picked`() {
        val looks = FolderLooks().with(Theme.Space, FolderLook.SolarSystem)

        assertEquals(FolderLook.SolarSystem, looks.of(Theme.Space))
        assertEquals(FolderLook.SubDial, looks.of(Theme.Clockwork))
        assertEquals(FolderLook.SolarSystem, looks.with(Theme.Clockwork, FolderLook.Gear).of(Theme.Space))
    }

    @Test
    fun `folders move only in a look that turns, and in the Solar system only if a planet shown moves`() {
        assertTrue(FolderLook.Gear.moves(folders = true, planets = emptyList()))
        assertTrue(!FolderLook.Gear.moves(folders = false, planets = emptyList()))
        assertTrue(!FolderLook.SubDial.moves(folders = true, planets = listOf(Planet.Earth)))
        assertTrue(!FolderLook.SolarSystem.moves(folders = true, planets = listOf(Planet.Mercury)))
        assertTrue(FolderLook.SolarSystem.moves(folders = true, planets = listOf(Planet.Mercury, Planet.Earth)))
    }

    @Test
    fun `night falls at seven in the evening and ends at six in the morning`() {
        assertTrue(!isNight(6 * 60))
        assertTrue(!isNight(19 * 60 - 1))
        assertTrue(isNight(19 * 60))
        assertTrue(isNight(0))
        assertTrue(isNight(6 * 60 - 1))
    }

    @Test
    fun `a map that darkens with the sky takes its night style after dark, and the rest keep theirs`() {
        assertEquals(MapStyle.Gta, Theme.Gta.mapStyleAt(12 * 60))
        assertEquals(MapStyle.GtaNight, Theme.Gta.mapStyleAt(23 * 60))
        assertEquals(MapStyle.Skyrim, Theme.Skyrim.mapStyleAt(23 * 60))
        assertEquals(null, Theme.Space.mapStyleAt(23 * 60))
    }

    @Test
    fun `a pick the theme does not offer shows its first instead`() {
        assertTrue(FolderLook.Gear !in Theme.Space.folderLooks)

        assertEquals(FolderLook.Rim, FolderLooks().with(Theme.Space, FolderLook.Gear).of(Theme.Space))
        assertEquals(null, FolderLooks().with(Theme.Space, FolderLook.Gear).picked(Theme.Space))
    }
}
