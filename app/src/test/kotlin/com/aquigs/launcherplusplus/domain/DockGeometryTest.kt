package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockGeometryTest {
    private val fullSize = 56f
    private val width = 400f

    @Test
    fun `icons keep full size while the row has room`() {
        (1..5).forEach { assertEquals(fullSize, dockIconSize(fullSize, width, it)) }
    }

    @Test
    fun `a crowded row shrinks every icon to fit`() {
        val sizes = (6..20).map { dockIconSize(fullSize, width, it) }

        assertTrue(sizes.toString(), sizes.zipWithNext().all { (bigger, smaller) -> smaller < bigger })
    }

    @Test
    fun `icons never touch their neighbours`() {
        (1..30).forEach { count ->
            assertTrue("$count icons touch", dockIconSize(fullSize, width, count) < width / count)
        }
    }
}
