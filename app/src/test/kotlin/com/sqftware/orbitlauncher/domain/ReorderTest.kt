package com.sqftware.orbitlauncher.domain

import com.sqftware.orbitlauncher.domain.ReorderMode.Insert
import com.sqftware.orbitlauncher.domain.ReorderMode.Swap
import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {
    private val list = listOf("a", "b", "c", "d", "e")

    @Test
    fun `insert takes the target's place and shifts the ones between toward the gap`() {
        assertEquals(listOf("b", "c", "d", "a", "e"), list.reordered(0, 3, Insert))
        assertEquals(listOf("a", "e", "b", "c", "d"), list.reordered(4, 1, Insert))
    }

    @Test
    fun `swap trades the two places and leaves the rest`() {
        assertEquals(listOf("d", "b", "c", "a", "e"), list.reordered(0, 3, Swap))
        assertEquals(listOf("a", "e", "c", "d", "b"), list.reordered(4, 1, Swap))
    }

    @Test
    fun `the same place or one off the list changes nothing`() {
        ReorderMode.entries.forEach { mode ->
            assertEquals(list, list.reordered(2, 2, mode))
            assertEquals(list, list.reordered(-1, 2, mode))
            assertEquals(list, list.reordered(2, 5, mode))
        }
    }
}
