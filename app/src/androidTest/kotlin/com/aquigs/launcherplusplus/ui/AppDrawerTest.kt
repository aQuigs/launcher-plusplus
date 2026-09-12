package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.OTHER_INITIAL
import com.aquigs.launcherplusplus.domain.sectionsByInitial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDrawerTest {
    @get:Rule
    val compose = createComposeRule()

    private val listState = LazyListState()

    private fun show(apps: List<AppEntry>, onLaunch: (AppEntry) -> Unit = {}) = compose.setContent {
        AppDrawer(sections = apps.sectionsByInitial(), onLaunch = onLaunch, listState = listState)
    }

    @Test
    fun filesEveryAppUnderItsInitialWithTheRailToMatch() {
        val zip7 = AppEntry("7zip", "com.example.zip", "Main")
        show(listOf(zip7, clock, mail))

        compose.appList().assertIsDisplayed()
        listOf("7zip", "Clock", "Mail").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        listOf(OTHER_INITIAL, 'C', 'M').forEach {
            compose.sectionHeader(it).assertIsDisplayed()
            compose.railLetter(it).assertIsDisplayed()
        }
    }

    @Test
    fun tappingAnAppLaunchesIt() {
        val launched = mutableListOf<AppEntry>()
        show(listOf(clock, mail), onLaunch = launched::add)

        compose.onNodeWithText("Mail").performClick()

        assertEquals(listOf(mail), launched)
    }

    @Test
    fun tappingARailLetterJumpsToItsSection() {
        show(alphabet)
        compose.assertNotShown("T1")

        compose.railLetter('T').performTouchInput { click() }

        compose.onNodeWithText("T1").assertIsDisplayed()
        compose.assertNotShown("A1")
        compose.runOnIdle { assertEquals(alphabet.sectionsByInitial().indexOfFirst { it.initial == 'T' } * 4, listState.firstVisibleItemIndex) }
    }

    @Test
    fun slidingAlongTheRailKeepsJumping() {
        show(alphabet)

        // One finger from A down to M without lifting: the list should follow it, not just the first touch.
        val a = compose.railLetter('A').fetchSemanticsNode().boundsInRoot.center
        val m = compose.railLetter('M').fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput { swipe(start = a, end = m) }

        compose.runOnIdle { assertTrue("scrolled to item ${listState.firstVisibleItemIndex}", listState.firstVisibleItemIndex >= 4 * 10) }
        compose.assertNotShown("A1")
    }
}
