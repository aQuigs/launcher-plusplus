package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageScrollTest {
    @get:Rule
    val compose = createComposeRule()

    private val edge = RecordingEdge()

    private fun flingUp(rows: Int): Velocity {
        compose.setContent {
            CompositionLocalProvider(LocalOverscrollFactory provides edge) {
                Column(Modifier.fillMaxSize().testTag("page").verticalPageScroll()) {
                    repeat(rows) { Box(Modifier.fillMaxWidth().height(100.dp)) }
                }
            }
        }
        compose.onNodeWithTag("page").performTouchInput { swipeUp(durationMillis = 100) }
        compose.waitForIdle()
        // The scroll's own fling, which hands the coast off at once, reports first; the coast reports when it ends.
        assertEquals("the coast never reported to the edge", 2, edge.left.size)
        return edge.left.last()
    }

    @Test
    fun aFlingThatRunsOutMidPageLeavesTheEdgeAlone() {
        assertEquals(Velocity.Zero, flingUp(rows = 500))
    }

    @Test
    fun aFlingThatReachesTheEndStretchesTheEdgeWithWhatItHasLeft() {
        assertTrue(flingUp(rows = 20).y < 0f)
    }
}

/** Records the velocity each fling leaves for the edge, the way the stretch effect reckons it. */
private class RecordingEdge : OverscrollFactory, OverscrollEffect {
    val left = mutableListOf<Velocity>()

    override fun createOverscrollEffect() = this

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset) =
        performScroll(delta)

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        left += velocity - performFling(velocity)
    }

    override val isInProgress = false

    override fun equals(other: Any?) = other === this

    override fun hashCode() = System.identityHashCode(this)
}
