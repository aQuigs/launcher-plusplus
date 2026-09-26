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

    @Test
    fun `each position of a reordered list traces back to where its item was`() {
        ReorderMode.entries.forEach { mode ->
            (-1..5).forEach { from ->
                (-1..5).forEach { to ->
                    val reordered = list.reordered(from, to, mode)
                    list.indices.forEach { assertEquals(reordered[it], list[list.reorderedFrom(it, from, to, mode)]) }
                }
            }
        }
    }

    @Test
    fun `an item from elsewhere goes in before the target, takes its stead, or joins the end`() {
        assertEquals(listOf("a", "x", "b", "c", "d", "e"), list.arrived("x", 1, Insert))
        assertEquals(listOf("a", "x", "c", "d", "e"), list.arrived("x", 1, Swap))
        ReorderMode.entries.forEach { mode ->
            assertEquals(list + "x", list.arrived("x", 5, mode))
            assertEquals(list, list.arrived("x", 6, mode))
        }
    }

    @Test
    fun `each position after an arrival traces back to where its item was, or to none for the one that arrived`() {
        ReorderMode.entries.forEach { mode ->
            (-1..6).forEach { to ->
                val arrived = list.arrived("x", to, mode)
                arrived.indices.forEach { assertEquals(arrived[it], list.arrivedFrom(it, to, mode)?.let(list::get) ?: "x") }
            }
        }
    }
}
