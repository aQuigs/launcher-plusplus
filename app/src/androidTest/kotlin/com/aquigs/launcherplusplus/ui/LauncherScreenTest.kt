package com.aquigs.launcherplusplus.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class LauncherScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val layout = PageLayout()
    private val pager = PagerState(currentPage = layout.homeIndex) { layout.pages.size }
    private val drawer = SheetState(
        skipPartiallyExpanded = false,
        positionalThreshold = { 56f },
        velocityThreshold = { 125f },
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    private val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private fun show() = compose.setContent {
        LauncherScreen(
            layout = layout,
            homeRequests = homeRequests,
            apps = listOf(clock),
            onLaunch = {},
            pagerState = pager,
            drawerState = drawer,
        )
    }

    private fun assertSettledOn(page: LauncherPage) {
        compose.waitForIdle()
        assertEquals(page, layout.pages[pager.settledPage])
        compose.page(page).assertIsDisplayed()
    }

    private fun assertDrawer(value: SheetValue) {
        compose.waitForIdle()
        assertEquals(value, drawer.currentValue)
        if (value == SheetValue.Expanded) compose.onNodeWithText("Clock").assertIsDisplayed() else compose.assertNotShown("Clock")
    }

    @Test
    fun startsOnTheHomePageWithTheDrawerClosed() {
        show()

        assertSettledOn(LauncherPage.Home)
        assertDrawer(SheetValue.PartiallyExpanded)
        compose.drawerHandle().assertIsDisplayed()
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
    fun theHandleTogglesTheDrawer() {
        show()

        compose.drawerHandle().performClick()
        assertDrawer(SheetValue.Expanded)

        compose.drawerHandle().performClick()
        assertDrawer(SheetValue.PartiallyExpanded)
    }

    @Test
    fun draggingTheHandleUpOpensTheDrawer() {
        show()

        val handle = compose.drawerHandle().fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput { swipe(start = handle, end = Offset(handle.x, top + height / 4)) }

        assertDrawer(SheetValue.Expanded)
    }

    @Test
    fun homeRequestClosesTheDrawerAndReturnsToTheHomePage() {
        show()
        compose.swipePager { swipeLeft() }
        compose.drawerHandle().performClick()
        assertSettledOn(LauncherPage.Collections)
        assertDrawer(SheetValue.Expanded)

        compose.runOnIdle { assertTrue(homeRequests.tryEmit(Unit)) }

        assertSettledOn(LauncherPage.Home)
        assertDrawer(SheetValue.PartiallyExpanded)
    }

    @Test
    fun backClosesTheDrawerFirstAndThenReturnsToTheHomePage() {
        show()
        compose.swipePager { swipeRight() }
        compose.drawerHandle().performClick()
        assertSettledOn(LauncherPage.Widgets)
        assertDrawer(SheetValue.Expanded)

        Espresso.pressBack()
        assertDrawer(SheetValue.PartiallyExpanded)
        assertSettledOn(LauncherPage.Widgets)

        Espresso.pressBack()
        assertSettledOn(LauncherPage.Home)
    }
}
