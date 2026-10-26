package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SkyrimWeatherTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSnowHoldsStillUntilItMovesThenFalls() {
        var moving by mutableStateOf(false)
        compose.mainClock.autoAdvance = false
        compose.setContent { SnowAndMist(moving, Modifier.size(240.dp).background(Color.Black).testTag(TAG)) }
        compose.mainClock.advanceTimeByFrame()
        val first = frame()

        compose.mainClock.advanceTimeBy(1_000)
        assertTrue("still while not moving", first.contentEquals(frame()))

        moving = true
        compose.mainClock.advanceTimeBy(1_000)
        assertFalse("fallen once moving", first.contentEquals(frame()))
    }

    private fun frame(): IntArray {
        val pixels = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()
        return pixels.buffer.copyOf()
    }

    private companion object {
        const val TAG = "weather"
    }
}
