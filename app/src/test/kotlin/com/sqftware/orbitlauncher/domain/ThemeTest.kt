package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    @Test
    fun `every folder look belongs to exactly one theme`() {
        assertEquals(FolderLook.entries.sorted(), Theme.entries.flatMap(Theme::folderLooks).sorted())
    }

    @Test
    fun `each theme keeps its own pick and shows its first until one is picked`() {
        val looks = FolderLooks().with(Theme.Space, FolderLook.SolarSystem)

        assertEquals(FolderLook.SolarSystem, looks.of(Theme.Space))
        assertEquals(FolderLook.SubDial, looks.of(Theme.Clockwork))
        assertEquals(FolderLook.SolarSystem, looks.with(Theme.Clockwork, FolderLook.Gear).of(Theme.Space))
    }

    @Test
    fun `a pick the theme does not offer shows its first instead`() {
        assertTrue(FolderLook.Gear !in Theme.Space.folderLooks)

        assertEquals(FolderLook.Rim, FolderLooks().with(Theme.Space, FolderLook.Gear).of(Theme.Space))
    }
}
