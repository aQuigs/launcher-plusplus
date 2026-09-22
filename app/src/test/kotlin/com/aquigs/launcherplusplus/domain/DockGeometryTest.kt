package com.aquigs.launcherplusplus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DockGeometryTest {
    private val fullSize = 56f
    private val width = 400f

    @Test
    fun `icons keep full size while the row has room`() {
        (1..5).forEach { assertEquals(fullSize, dockRow(fullSize, width, it).iconSize) }
    }

    @Test
    fun `a crowded row shrinks every icon to fit`() {
        val sizes = (6..20).map { dockRow(fullSize, width, it).iconSize }

        assertTrue(sizes.toString(), sizes.zipWithNext().all { (bigger, smaller) -> smaller < bigger })
    }

    @Test
    fun `icons never touch their neighbours or the edges`() {
        (1..30).forEach { count ->
            val row = dockRow(fullSize, width, count)
            val edges = listOf(0f) + row.starts.flatMap { listOf(it, it + row.iconSize) } + width

            assertTrue("$count icons: $edges", edges.zipWithNext().all { (before, after) -> before < after })
        }
    }

    @Test
    fun `a few icons sit together in the middle of the row`() {
        (1..5).forEach { count ->
            val row = dockRow(fullSize, width, count)
            val gaps = row.starts.zipWithNext { a, b -> b - a - row.iconSize }

            assertEquals("$count icons centred", row.starts.first(), width - row.starts.last() - row.iconSize, 0.01f)
            assertTrue("$count icons together: $gaps", gaps.all { it < row.iconSize / 2 })
        }
    }
}
