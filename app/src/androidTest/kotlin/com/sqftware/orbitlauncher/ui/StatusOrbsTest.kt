package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.RingItem
import com.sqftware.orbitlauncher.domain.Steps
import com.sqftware.orbitlauncher.domain.Theme
import com.sqftware.orbitlauncher.domain.Vital
import com.sqftware.orbitlauncher.domain.Vitals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusOrbsTest {
    @get:Rule
    val compose = createComposeRule()

    private var ring by mutableStateOf(emptyList<RingItem>())
    private var vitals by mutableStateOf(Vitals(battery = 87, storageFree = 64, steps = Steps.Today(4_200)))
    private var spins by mutableStateOf(true)
    private val opened = mutableListOf<Any>()

    private fun show() = compose.setContent {
        Themed(Theme.OldSchool, Theme.OldSchool.colourways.first(), lightWallpaper = false) {
            val shown = VitalsShown({ vitals }, { opened += it }, { opened += "spin stats" }.takeIf { spins })
            CompositionLocalProvider(LocalVitals provides shown) {
                HomeRing(
                    ring = ring,
                    hint = null,
                    icon = { _, _ -> null },
                    onLaunch = {},
                    onOpenFolder = {},
                    onCloseFolder = {},
                    onEdit = {},
                    onSearch = {},
                )
            }
        }
    }

    private val orbs = listOf("Battery 87%", "Storage 64% free", "4200 steps today", "Spin stats")

    @Test
    fun eachOrbShowsItsReadingAndOpensWhatItStandsFor() {
        show()

        orbs.forEach { compose.onNodeWithContentDescription(it).assertIsDisplayed().performClick() }

        assertEquals(listOf(Vital.Battery, Vital.Storage, Vital.Steps, "spin stats"), opened)
    }

    @Test
    fun theRunOrbWaitsForStepsAndGoesWithoutACounterAndTheSpecialAttackOnlyWhileTheRingSpins() {
        vitals = vitals.copy(steps = Steps.NotAllowed)
        show()
        compose.onNodeWithContentDescription("Count steps").performClick()
        assertEquals(listOf<Any>(Vital.Steps), opened)

        vitals = vitals.copy(steps = Steps.Unavailable)
        spins = false
        compose.waitForIdle()
        listOf("Count steps", "Spin stats").forEach { compose.onNodeWithContentDescription(it).assertDoesNotExist() }
        compose.onNodeWithContentDescription("Battery 87%").assertIsDisplayed()
    }

    @Test
    fun ringsOfAnySizeKeepTheirAppsOffTheOrbs() {
        show()
        (1..14).forEach { count ->
            ring = alphabet.take(count).asRingItems()
            compose.waitForIdle()

            val orbBounds = orbs.map { compose.onNodeWithContentDescription(it).getUnclippedBoundsInRoot() }
            alphabet.take(count).forEach { app ->
                val slot = compose.ringSlot(app).getUnclippedBoundsInRoot()
                orbBounds.forEachIndexed { i, orb -> assertFalse("${app.label} of $count on ${orbs[i]}", slot.overlaps(orb)) }
            }
        }
    }

    private fun DpRect.overlaps(other: DpRect) = left < other.right && other.left < right && top < other.bottom && other.top < bottom
}
