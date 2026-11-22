package com.sqftware.orbitlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerRoomTest {
    private val cell = Size(300f, 300f)
    private val half = 15f

    @Test
    fun `a quest past a wide window is marked on the room's edge whichever way the map turns`() {
        val room = MapWindow(aspect = 1.8f, corner = 0.16f).markerRoom(cell, inset = 0f, half)
        for (degrees in 0 until 360 step 15) {
            val way = direction(degrees.toFloat())
            val marked = room.towards(cell.center, cell.center + way * 1_000f)
            assertTrue("turned $degrees°", room.contains(marked - way * 0.5f))
            assertFalse("turned $degrees°", room.contains(marked + way * 0.5f))
        }
    }

    @Test
    fun `a round window's room is the disc short of its rim, whole round the middle`() {
        val window = MapWindow.Disc
        val reach = window.reach(cell)
        val room = window.markerRoom(cell, inset = 0.38f * reach, half)
        val radius = 0.62f * reach
        assertEquals(cell.center.x - radius, room.left, 0.01f)
        assertEquals(cell.center.y - radius, room.top, 0.01f)
        assertEquals(cell.center.y + radius, room.bottom, 0.01f)
        assertEquals(radius, room.topLeftCornerRadius.x, 0.01f)
        val marked = room.towards(cell.center, cell.center + Offset(300f, 300f))
        assertEquals(radius, (marked - cell.center).getDistance(), 0.05f)
    }
}
