package com.sqftware.orbitlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class RingSpinTest {
    @Test
    fun `a move along the ring goes round it, one toward or away from the centre does not`() {
        // At the top of the ring, across is round it and up or down is in or out; at its side, the other way about.
        assertTrue(goesRound(0f, -100f, 10f, 2f))
        assertFalse(goesRound(0f, -100f, 2f, 10f))
        assertTrue(goesRound(100f, 0f, -2f, -10f))
        assertFalse(goesRound(100f, 0f, -10f, 2f))
    }

    @Test
    fun `the turn between two points is clockwise on screen and the short way round`() {
        assertEquals(PI.toFloat() / 2, turnBetween(0f, -1f, 1f, 0f), 1e-6f)
        assertEquals(-PI.toFloat() / 2, turnBetween(1f, 0f, 0f, -1f), 1e-6f)
        assertEquals(PI.toFloat() / 2, turnBetween(-1f, 0f, 0f, -5f), 1e-6f)
        assertEquals(0.2f, turnBetween(-1f, 0.1f, -1f, -0.1f), 0.01f)
    }

    @Test
    fun `only the finger's speed round the centre turns the ring`() {
        assertEquals(2f, turnRate(0f, -100f, 200f, 0f), 1e-6f)
        assertEquals(-2f, turnRate(0f, 100f, 200f, 0f), 1e-6f)
        assertEquals(0f, turnRate(0f, -100f, 0f, 500f), 1e-6f)
    }

    @Test
    fun `a spun ring rests on the whole turn nearest where it coasts to`() {
        assertEquals(0f, restingTurn(0.4f), 0f)
        assertEquals(FULL_TURN, restingTurn(0.6f * FULL_TURN), 1e-5f)
        assertEquals(-2 * FULL_TURN, restingTurn(-2.3f * FULL_TURN), 1e-5f)
    }
}
