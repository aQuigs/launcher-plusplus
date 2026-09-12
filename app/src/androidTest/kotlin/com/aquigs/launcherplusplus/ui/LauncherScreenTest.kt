package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private fun show() = compose.setContent {
        LauncherScreen(layout = layout, homeRequests = homeRequests, apps = listOf(clock), onLaunch = {}, pagerState = pager)
    }

    private fun assertSettledOn(page: LauncherPage) {
        compose.waitForIdle()
        assertEquals(page, layout.pages[pager.settledPage])
        compose.page(page).assertIsDisplayed()
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

        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)

        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Home)

        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)
    }

    @Test
    fun homeRequestReturnsToTheHomePage() {
        show()
        compose.swipePager { swipeLeft() }
        assertSettledOn(LauncherPage.Collections)

        compose.runOnIdle { assertTrue(homeRequests.tryEmit(Unit)) }

        assertSettledOn(LauncherPage.Home)
    }

    @Test
    fun backOnASidePageReturnsToTheHomePage() {
        show()
        compose.swipePager { swipeRight() }
        assertSettledOn(LauncherPage.Widgets)

        Espresso.pressBack()

        assertSettledOn(LauncherPage.Home)
    }
}
