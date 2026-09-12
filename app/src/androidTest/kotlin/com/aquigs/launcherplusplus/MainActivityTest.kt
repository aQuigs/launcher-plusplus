package com.aquigs.launcherplusplus

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.swipeLeft
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.apps.SharedPreferencesFavouritesStore
import com.aquigs.launcherplusplus.domain.Favourites
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.ui.HomeRingTags
import com.aquigs.launcherplusplus.ui.appList
import com.aquigs.launcherplusplus.ui.drawerHandle
import com.aquigs.launcherplusplus.ui.page
import com.aquigs.launcherplusplus.ui.swipePager
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /**
     * Delivers a HOME intent as the system does: with the launcher in front it arrives while the activity is paused; from
     * another app it arrives after the launcher was stopped, flagged as bringing its task to the front.
     */
    private fun deliverHomeIntent(fromAnotherApp: Boolean) {
        val scenario = compose.activityRule.scenario
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        if (fromAnotherApp) home.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        scenario.moveToState(if (fromAnotherApp) Lifecycle.State.CREATED else Lifecycle.State.STARTED)
        scenario.onActivity { InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, home) }
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

    private fun openDrawerOnCollections() {
        compose.swipePager { swipeLeft() }
        compose.page(LauncherPage.Collections).assertIsDisplayed()
        compose.drawerHandle().performClick()
        compose.appList().assertIsDisplayed()
    }

    // The activity reads the ring before any @Before could run, so each test clears what it stored afterwards instead.
    @After
    fun emptyTheRing() {
        SharedPreferencesFavouritesStore(InstrumentationRegistry.getInstrumentation().targetContext).save(Favourites())
    }

    @Test
    fun aFavouriteSurvivesRecreatingTheActivity() {
        compose.onNodeWithTag(HomeRingTags.EMBLEM).performClick()
        val list = compose.appList()
        compose.waitUntil(timeoutMillis = 10_000) { list.onChildren().fetchSemanticsNodes().isNotEmpty() }
        list.performScrollToNode(hasText("Settings"))
        compose.onNodeWithText("Settings").performClick()
        Espresso.pressBack()
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()

        compose.activityRule.scenario.recreate()

        // The app list loads again after recreation, and the ring only shows installed apps.
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
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
    fun homeKeyWhileInFrontClosesTheDrawerAndReturnsToTheHomePage() {
        openDrawerOnCollections()

        deliverHomeIntent(fromAnotherApp = false)

        compose.page(LauncherPage.Home).assertIsDisplayed()
        compose.appList().assertIsNotDisplayed()
    }

    @Test
    fun homeKeyFromAnotherAppClosesTheDrawerAndKeepsThePage() {
        openDrawerOnCollections()

        deliverHomeIntent(fromAnotherApp = true)

        compose.page(LauncherPage.Collections).assertIsDisplayed()
        compose.appList().assertIsNotDisplayed()
    }
}
