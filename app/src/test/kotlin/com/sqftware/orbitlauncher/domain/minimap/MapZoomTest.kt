package com.sqftware.orbitlauncher.domain.minimap

import org.junit.Assert.assertEquals
import org.junit.Test

class MapZoomTest {
    @Test
    fun stepsAreTheGamesLevelsInWholeScreenPixels() {
        assertEquals(listOf(2.0, 3.0, 4.0, 8.0), ZoomSteps(4).all)
        assertEquals(listOf(2.0, 3.0, 6.0), ZoomSteps(3).all)
        assertEquals(listOf(2.0, 4.0), ZoomSteps(2).all)
    }

    @Test
    fun farStepsHalveTheFarthestAndPaintCoarserAsTheyDo() {
        val steps = ZoomSteps(4, farSteps = 3)
        assertEquals(listOf(0.25, 0.5, 1.0, 2.0, 3.0, 4.0, 8.0), steps.all)
        assertEquals(listOf(8, 4, 2, 1, 1, 1, 1), steps.all.map(steps::spanAt))
        assertEquals(2, steps.spanAt(1.9))
        assertEquals(8, steps.spanAt(0.2))
        assertEquals(0.2, steps.clamp(0.01), 1e-9)
    }

    @Test
    fun aPinchSettlesOnTheStepNearestByRatio() {
        val steps = ZoomSteps(4)
        assertEquals(3.0, steps.nearest(3.4), 0.0)
        assertEquals(4.0, steps.nearest(5.5), 0.0)
        assertEquals(2.0, steps.nearest(1.2), 0.0)
        assertEquals(8.0, steps.nearest(11.0), 0.0)
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
        assertEquals(2.0, steps.paintedAt(2.9), 0.0)
        assertEquals(3.0, steps.paintedAt(3.0), 0.0)
        assertEquals(4.0, steps.paintedAt(7.9), 0.0)
        assertEquals(2.0, steps.paintedAt(1.7), 0.0)
    }
}
