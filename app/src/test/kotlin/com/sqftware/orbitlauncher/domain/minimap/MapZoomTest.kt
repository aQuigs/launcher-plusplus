package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Test

class MapZoomTest {
    @Test
    fun stepsAreTheGamesLevelsInWholeScreenPixels() {
        assertEquals(listOf(2, 3, 4, 8), ZoomSteps(4).all)
        assertEquals(listOf(2, 3, 6), ZoomSteps(3).all)
        assertEquals(listOf(2, 4), ZoomSteps(2).all)
    }

    @Test
    fun aPinchSettlesOnTheStepNearestByRatio() {
        val steps = ZoomSteps(4)
        assertEquals(3, steps.nearest(3.4))
        assertEquals(4, steps.nearest(5.5))
        assertEquals(2, steps.nearest(1.2))
        assertEquals(8, steps.nearest(11.0))
    }

    @Test
    fun aPinchStretchesOnlyALittlePastTheFarthestSteps() {
        val steps = ZoomSteps(4)
        assertEquals(1.6, steps.clamp(1.0), 1e-9)
        assertEquals(10.0, steps.clamp(20.0), 1e-9)
        assertEquals(5.0, steps.clamp(5.0), 1e-9)
    }

    @Test
    fun aViewIsPaintedForTheStepAtOrOutFromIt() {
        val steps = ZoomSteps(4)
        assertEquals(2, steps.paintedAt(2.9))
        assertEquals(3, steps.paintedAt(3.0))
        assertEquals(4, steps.paintedAt(7.9))
        assertEquals(2, steps.paintedAt(1.7))
    }
}
