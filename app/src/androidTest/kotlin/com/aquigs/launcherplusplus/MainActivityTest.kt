package com.aquigs.launcherplusplus

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.swipeLeft
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.ui.appList
import com.aquigs.launcherplusplus.ui.drawerHandle
import com.aquigs.launcherplusplus.ui.page
import com.aquigs.launcherplusplus.ui.swipePager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val scenario get() = compose.activityRule.scenario

    /**
     * Stands in for the system: a HOME press reaches the running singleTask home activity as this intent, and the
     * framework has the activity in [state] when it arrives: STARTED (paused around onNewIntent) when the launcher was
     * in front, CREATED (stopped) when another app was.
     */
    private fun deliverHomeIntent(state: Lifecycle.State) {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        scenario.moveToState(state)
        scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, home) }
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun theDrawerListsInstalledAppsIncludingSettings() {
        compose.drawerHandle().performClick()
        val list = compose.appList()
        list.assertIsDisplayed()

        compose.waitUntil(timeoutMillis = 10_000) {
            list.onChildren().fetchSemanticsNodes().isNotEmpty()
        }
        // Images with many preinstalled apps push Settings below the fold, where LazyColumn has not composed it yet.
        list.performScrollToNode(hasText("Settings"))
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun homeKeyWhileInFrontReturnsToTheHomePage() {
        compose.swipePager { swipeLeft() }
        compose.page(LauncherPage.Collections).assertIsDisplayed()

        deliverHomeIntent(Lifecycle.State.STARTED)

        compose.page(LauncherPage.Home).assertIsDisplayed()
    }

    @Test
    fun homeKeyWhileInFrontClosesTheDrawer() {
        compose.drawerHandle().performClick()
        compose.appList().assertIsDisplayed()

        deliverHomeIntent(Lifecycle.State.STARTED)

        compose.appList().assertIsNotDisplayed()
    }

    @Test
    fun homeKeyFromAnotherAppKeepsThePage() {
        compose.swipePager { swipeLeft() }
        compose.page(LauncherPage.Collections).assertIsDisplayed()

        deliverHomeIntent(Lifecycle.State.CREATED)

        compose.page(LauncherPage.Collections).assertIsDisplayed()
    }
}
