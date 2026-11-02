package com.sqftware.orbitlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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

class DriftingWeatherTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun eachWeatherDriftsWithItsClockAndComesBackRoundAfterACycle() {
        val drift = mutableStateOf(Snowfall)
        val turn = mutableFloatStateOf(90f)
        compose.setContent { DriftingWeather(drift.value, { turn.floatValue }, Modifier.size(240.dp).background(Color.Black).testTag(TAG)) }

        listOf(Snowfall, Dust).forEach { weather ->
            drift.value = weather
            turn.floatValue = 90f
            val start = frame()

            turn.floatValue = 90.01f
            assertFalse("moved as the clock moves", alike(start, frame()))

            turn.floatValue = 450f
            assertTrue("where it was a whole cycle later", alike(start, frame()))
        }
    }

    private fun frame() = compose.onNodeWithTag(TAG).captureToImage().toPixelMap().buffer

    // A whole cycle later a mote is back within a rounding of where it was, which can shade its edge a level apart.
    private fun alike(a: IntArray, b: IntArray) = a.indices.all { i ->
        (0 until 32 step 8).all { shift -> abs((a[i] shr shift and 0xFF) - (b[i] shr shift and 0xFF)) <= 2 }
    }

    private companion object {
        const val TAG = "weather"
    }
}
