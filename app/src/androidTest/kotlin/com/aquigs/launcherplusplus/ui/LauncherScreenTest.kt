package com.aquigs.launcherplusplus.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.domain.PageLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val layout = PageLayout()

    private fun page(page: LauncherPage) = compose.onNodeWithTag(LauncherTags.page(page))

    private fun swipe(swipe: androidx.compose.ui.test.TouchInjectionScope.() -> Unit) =
        compose.onNodeWithTag(LauncherTags.PAGER).performTouchInput(swipe)

    @Test
    fun startsOnTheHomePageWithTheAppList() {
        compose.setContent { LauncherScreen(layout = layout, homeRequests = 0, apps = emptyList(), onLaunch = {}) }

        page(LauncherPage.Home).assertIsDisplayed()
        compose.onNodeWithTag(AppListTags.LIST).assertIsDisplayed()
    }

    @Test
    fun swipesToBothNeighbours() {
        compose.setContent { LauncherScreen(layout = layout, homeRequests = 0, apps = emptyList(), onLaunch = {}) }

        swipe { swipeLeft() }
        page(LauncherPage.Collections).assertIsDisplayed()

        swipe { swipeRight() }
        page(LauncherPage.Home).assertIsDisplayed()

        swipe { swipeRight() }
        page(LauncherPage.Widgets).assertIsDisplayed()
    }

    @Test
    fun homeRequestReturnsToTheHomePage() {
        var homeRequests by mutableIntStateOf(0)
        compose.setContent { LauncherScreen(layout = layout, homeRequests = homeRequests, apps = emptyList(), onLaunch = {}) }
        swipe { swipeLeft() }
        page(LauncherPage.Collections).assertIsDisplayed()

        compose.runOnIdle { homeRequests++ }

        page(LauncherPage.Home).assertIsDisplayed()
    }
}
