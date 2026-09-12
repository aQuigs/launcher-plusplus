package com.aquigs.launcherplusplus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DockTest {
    @get:Rule
    val compose = createComposeRule()

    private var docked by mutableStateOf(emptyList<AppEntry>())

    private fun show(apps: List<AppEntry>, onLaunch: (AppEntry) -> Unit = {}) {
        docked = apps
        compose.setContent { Dock(apps = docked, icon = { null }, onLaunch = onLaunch) }
    }

    @Test
    fun appsSitInOneRowInTheOrderAdded() {
        val four = alphabet.take(4)
        show(four)

        val bounds = four.map { compose.dockSlot(it).getUnclippedBoundsInRoot() }
        assertTrue(bounds.toString(), bounds.zipWithNext().all { (a, b) -> a.right < b.left && a.top == b.top })
    }

    @Test
    fun tappingADockAppLaunchesIt() {
        val launched = mutableListOf<AppEntry>()
        show(listOf(clock, mail), onLaunch = launched::add)

        compose.onNodeWithContentDescription("Mail").performClick()

        assertEquals(listOf(mail), launched)
    }

    @Test
    fun anEmptyDockTakesNoSpace() {
        show(emptyList())

        compose.dock().assertDoesNotExist()
    }

    @Test
    fun aCrowdedDockShrinksItsIcons() {
        show(alphabet.take(4))
        val roomy = compose.dockSlot(alphabet[0]).getUnclippedBoundsInRoot().width

        docked = alphabet.take(20)
        compose.waitForIdle()

        assertTrue("20 icons are smaller than 4", compose.dockSlot(alphabet[0]).getUnclippedBoundsInRoot().width < roomy)
    }
}
