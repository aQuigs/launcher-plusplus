package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderLookTest {
    private val clock = app("Clock")

    private fun folder(holder: HomePlace.Slots, index: Int) = RingItem.Folder(HomePlace.Folder(holder, index), listOf(clock))

    @Test
    fun `folders take the planets round the ring then along the dock, skipping apps`() {
        val ring = listOf(folder(HomePlace.Ring, 0), RingItem.App(clock), folder(HomePlace.Ring, 2))
        val dock = listOf(RingItem.App(clock), folder(HomePlace.Dock, 1))

        assertEquals(
            mapOf(
                HomePlace.Folder(HomePlace.Ring, 0) to Planet.Mercury,
                HomePlace.Folder(HomePlace.Ring, 2) to Planet.Venus,
                HomePlace.Folder(HomePlace.Dock, 1) to Planet.Earth,
            ),
            planetsOf(ring, dock),
        )
    }

    @Test
    fun `a picked planet is no other folder's, and folders past the planets left are plain`() {
        val ring = List(11) { folder(HomePlace.Ring, it) }.toMutableList()
        ring[0] = ring[0].copy(pick = PlanetPick.Plain)
        ring[10] = ring[10].copy(pick = PlanetPick.Of(Planet.Earth))

        val planets = planetsOf(ring, emptyList())

        assertEquals(null, planets[HomePlace.Folder(HomePlace.Ring, 0)])
        assertEquals(Planet.Pluto, planets[HomePlace.Folder(HomePlace.Ring, 8)])
        assertEquals(null, planets[HomePlace.Folder(HomePlace.Ring, 9)])
        assertEquals(Planet.Earth, planets[HomePlace.Folder(HomePlace.Ring, 10)])
    }

    @Test
    fun `kept planets stay with their folders as folders move, go and come`() {
        val mail = app("Mail")
        val a = RingSlot.Folder(listOf(clock.key))
        val b = RingSlot.Folder(listOf(mail.key))
        val kept = HomeApps(ring = Ring(listOf(a, b))).withPlanetsKept()
        val (keptA, keptB) = kept.ring.slots

        assertEquals(listOf(PlanetPick.Of(Planet.Mercury), PlanetPick.Of(Planet.Venus)), kept.ring.slots.map { (it as RingSlot.Folder).pick })
        assertEquals(HomeApps(ring = Ring(listOf(keptB, keptA))), HomeApps(ring = Ring(listOf(keptB, keptA))).withPlanetsKept())
        assertEquals(
            HomeApps(ring = Ring(listOf(keptB, a.copy(pick = PlanetPick.Of(Planet.Mercury))))),
            HomeApps(ring = Ring(listOf(keptB, a))).withPlanetsKept(),
        )
    }
}
