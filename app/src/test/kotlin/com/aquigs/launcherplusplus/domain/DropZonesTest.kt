package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DropZonesTest {
    // A ring area wider than it is tall, as on a phone, so the disc is bounded by the height.
    private val ring = Bounds(left = 0f, top = 100f, right = 1000f, bottom = 700f)
    private val dock = Bounds(left = 0f, top = 800f, right = 1000f, bottom = 900f)
    private val zones = DropZones(ring, dock)

    @Test
    fun `the ring takes points within the disc inscribed in its bounds`() {
        assertEquals(HomePlace.Ring, zones.placeAt(500f, 400f))
        assertEquals(HomePlace.Ring, zones.placeAt(500f, 100f))
        assertEquals(HomePlace.Ring, zones.placeAt(200f, 400f))
        assertEquals(HomePlace.Ring, zones.placeAt(799f, 400f))
    }

    @Test
    fun `the corners of the ring's bounds are outside the disc`() {
        assertNull(zones.placeAt(1f, 101f))
        assertNull(zones.placeAt(999f, 699f))
        assertNull(zones.placeAt(500f, 99f))
    }

    @Test
    fun `the dock takes its whole row, edges included`() {
        assertEquals(HomePlace.Dock, zones.placeAt(500f, 850f))
        assertEquals(HomePlace.Dock, zones.placeAt(0f, 800f))
        assertEquals(HomePlace.Dock, zones.placeAt(1000f, 900f))
        assertNull(zones.placeAt(500f, 901f))
        assertNull(zones.placeAt(1001f, 850f))
    }

    @Test
    fun `a zone that is not on screen takes nothing`() {
        assertNull(DropZones(ring = ring).placeAt(500f, 850f))
        assertNull(DropZones(dock = dock).placeAt(500f, 400f))
        assertNull(DropZones().placeAt(500f, 400f))
    }
}
