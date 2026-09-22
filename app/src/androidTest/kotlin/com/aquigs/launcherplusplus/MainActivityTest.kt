package com.aquigs.launcherplusplus

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.swipeLeft
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.apps.SystemRelauncher
import com.aquigs.launcherplusplus.apps.SystemWallClock
import com.aquigs.launcherplusplus.domain.HomePlace
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.ui.DockTags
import com.aquigs.launcherplusplus.ui.HomeClockTags
import com.aquigs.launcherplusplus.ui.LauncherTags
import com.aquigs.launcherplusplus.ui.appList
import com.aquigs.launcherplusplus.ui.drawerHandle
import com.aquigs.launcherplusplus.ui.emblem
import com.aquigs.launcherplusplus.ui.homeAppCard
import com.aquigs.launcherplusplus.ui.longPressEmptyHomeSpace
import com.aquigs.launcherplusplus.ui.page
import com.aquigs.launcherplusplus.ui.placeOption
import com.aquigs.launcherplusplus.ui.swipePager
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // On a dark system the default bar styles draw light icons too, so the tests run on a light system, where the default
    // went wrong. The user's setting comes back afterwards.
    @get:Rule(order = 0)
    val lightSystemTheme = object : ExternalResource() {
        private lateinit var previous: String

        override fun before() {
            previous = shell("cmd uimode night").substringAfter(": ").trim()
            shell("cmd uimode night no")
        }

        override fun after() {
            shell("cmd uimode night $previous")
        }
    }

    @get:Rule(order = 0)
    val restoreHourStyle = RestoreHourStyle()

    // The activity reads what the launcher stores in onCreate, so it is all erased before the compose rule starts the
    // activity, whatever an earlier run or by-hand use left there, and erased again afterwards.
    @get:Rule(order = 1)
    val emptyStores = object : ExternalResource() {
        override fun before() = SystemRelauncher(context).erase()

        override fun after() = SystemRelauncher(context).erase()
    }

    @get:Rule(order = 2)
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun restoreTheLauncherAsHome() = makeLauncherHome()

    /** Stops and restarts the activity, as leaving for Settings and coming back does. */
    private fun leaveAndComeBack() {
        val scenario = compose.activityRule.scenario
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
    }

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

    private fun scrollDrawerTo(label: String) {
        val list = compose.appList()
        compose.waitUntil(timeoutMillis = 10_000) { list.onChildren().fetchSemanticsNodes().isNotEmpty() }
        // Images with many preinstalled apps push the app below the fold, where LazyColumn has not composed it yet.
        list.performScrollToNode(hasText(label))
    }

    private fun ringIcon(label: String) =
        hasContentDescription(label) and hasAnyAncestor(hasTestTag(LauncherTags.page(LauncherPage.Home)))

    private fun dockIcon(label: String) = hasContentDescription(label) and hasAnyAncestor(hasTestTag(DockTags.DOCK))

    // Against the face the clock would draw in that style, read afresh in case the minute turns meanwhile.
    private fun assertClockIn(twentyFourHour: Boolean) {
        val wallClock = SystemWallClock(context)
        compose.waitUntil(timeoutMillis = 5_000) {
            val clockIn = hasTestTag(HomeClockTags.TIME) and hasText(wallClock.face(twentyFourHour).time)
            compose.onAllNodes(clockIn).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertSettingsOnTheRingAndInTheDock() {
        compose.onNode(ringIcon("Settings")).assertIsDisplayed()
        compose.onNode(dockIcon("Settings")).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun homeScreenAppsSurviveRecreatingTheActivity() {
        compose.emblem().performClick()
        scrollDrawerTo("Settings")
        compose.onNodeWithText("Settings").performClick()
        compose.placeOption(HomePlace.Dock).performClick()
        compose.onNodeWithText("Settings").performClick()
        Espresso.pressBack()
        assertSettingsOnTheRingAndInTheDock()

        compose.activityRule.scenario.recreate()

        // The app list loads again after recreation, and the ring and the dock only show installed apps.
        compose.waitUntilAtLeastOneExists(dockIcon("Settings"), timeoutMillis = 10_000)
        assertSettingsOnTheRingAndInTheDock()
    }

    // The system is set to 24 hours, where a clock that fell to 12 without a stored choice would show it.
    @Test
    fun theClockFollowsTheSystemUntilTheMenuFlipsItAndKeepsTheChoice() {
        setSystemHourStyle(twentyFourHour = true)
        assertClockIn(twentyFourHour = true)

        compose.longPressEmptyHomeSpace()
        compose.onNodeWithText("24-hour clock").performClick()

        assertClockIn(twentyFourHour = false)
        compose.activityRule.scenario.recreate()
        assertClockIn(twentyFourHour = false)
    }

    @Test
    fun theDrawerListsInstalledAppsIncludingSettings() {
        compose.drawerHandle().performClick()
        compose.appList().assertIsDisplayed()

        scrollDrawerTo("Settings")
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun theSystemBarsDrawLightIconsOverTheWallpaper() {
        compose.activityRule.scenario.onActivity { activity ->
            val bars = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertFalse("status bar icons are dark", bars.isAppearanceLightStatusBars)
            assertFalse("navigation bar icons are dark", bars.isAppearanceLightNavigationBars)
            assertTrue("three-button navigation lost its backing", activity.window.isNavigationBarContrastEnforced)
        }
    }

    @Test
    fun theHomePageAsksToBeTheHomeAppUntilTheRoleIsHeld() {
        makeHomeApp(otherHomeApp())
        leaveAndComeBack()
        compose.homeAppCard().assertIsDisplayed()

        makeLauncherHome()
        leaveAndComeBack()

        compose.homeAppCard().assertDoesNotExist()
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
