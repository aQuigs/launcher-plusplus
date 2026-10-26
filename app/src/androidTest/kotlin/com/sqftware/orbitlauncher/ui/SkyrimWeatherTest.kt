package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlin.math.abs

class SkyrimWeatherTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSnowFallsWithItsClockAndComesBackRoundAfterACycle() {
        val turn = mutableFloatStateOf(90f)
        compose.setContent { SnowAndMist({ turn.floatValue }, Modifier.size(240.dp).background(Color.Black).testTag(TAG)) }
        val start = frame()

        turn.floatValue = 90.01f
        assertFalse("fallen as the clock moves", alike(start, frame()))

        turn.floatValue = 450f
        assertTrue("where it was a whole cycle later", alike(start, frame()))
    }

    private fun frame() = compose.onNodeWithTag(TAG).captureToImage().toPixelMap().buffer

    // A whole cycle later a flake is back within a rounding of where it was, which can shade its edge a level apart.
    private fun alike(a: IntArray, b: IntArray) = a.indices.all { i ->
        (0 until 32 step 8).all { shift -> abs((a[i] shr shift and 0xFF) - (b[i] shr shift and 0xFF)) <= 2 }
    }

    private companion object {
        const val TAG = "weather"
    }
}
