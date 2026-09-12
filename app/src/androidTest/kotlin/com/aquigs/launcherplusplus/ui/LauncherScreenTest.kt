package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val layout = PageLayout()
    private val pager = PagerState(currentPage = layout.homeIndex) { layout.pages.size }
    private val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val clock = AppEntry("Clock", "com.example.clock", "com.example.clock.Main")

    private fun show() = compose.setContent {
        LauncherScreen(layout = layout, homeRequests = homeRequests, apps = listOf(clock), onLaunch = {}, pagerState = pager)
    }

    private fun swipe(swipe: TouchInjectionScope.() -> Unit) =
        compose.onNodeWithTag(LauncherTags.PAGER).performTouchInput(swipe)

    private fun assertSettledOn(page: LauncherPage) {
        compose.waitForIdle()
        assertEquals(page, layout.pages[pager.settledPage])
        compose.onNodeWithTag(LauncherTags.page(page)).assertIsDisplayed()
    }

    @Test
    fun startsOnTheHomePageWithTheAppList() {
        show()

        assertSettledOn(LauncherPage.Home)
        compose.onNodeWithText("Clock").assertIsDisplayed()
    }

    @Test
    fun swipesToBothNeighbours() {
        show()

        swipe { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)

        swipe { swipeRight() }
        assertSettledOn(LauncherPage.Home)

        swipe { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)
    }

    @Test
    fun homeRequestReturnsToTheHomePage() {
        show()
        swipe { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)

        compose.runOnIdle { assertTrue(homeRequests.tryEmit(Unit)) }

        assertSettledOn(LauncherPage.Home)
    }

    @Test
    fun backOnASidePageReturnsToTheHomePage() {
        show()
        swipe { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)

        Espresso.pressBack()

        assertSettledOn(LauncherPage.Home)
    }
}
