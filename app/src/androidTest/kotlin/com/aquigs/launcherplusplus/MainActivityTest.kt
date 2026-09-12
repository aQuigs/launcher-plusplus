package com.aquigs.launcherplusplus

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aquigs.launcherplusplus.domain.LauncherPage
import com.aquigs.launcherplusplus.ui.AppListTags
import com.aquigs.launcherplusplus.ui.LauncherTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun page(page: LauncherPage) = compose.onNodeWithTag(LauncherTags.page(page))

    // Stands in for the system: a HOME press reaches the running singleTask home activity as this intent.
    private fun deliverHomeIntent() {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        compose.activityRule.scenario.onActivity {
            InstrumentationRegistry.getInstrumentation().callActivityOnNewIntent(it, home)
        }
    }

    @Test
    fun listsInstalledAppsIncludingSettings() {
        val list = compose.onNodeWithTag(AppListTags.LIST)
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
        compose.onNodeWithTag(LauncherTags.PAGER).performTouchInput { swipeLeft() }
        page(LauncherPage.Collections).assertIsDisplayed()

        deliverHomeIntent()

        page(LauncherPage.Home).assertIsDisplayed()
    }

    @Test
    fun homeKeyFromAnotherAppKeepsThePage() {
        compose.onNodeWithTag(LauncherTags.PAGER).performTouchInput { swipeLeft() }
        page(LauncherPage.Collections).assertIsDisplayed()

        // Another activity in front stops the launcher; the HOME intent arrives before it resumes.
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        deliverHomeIntent()
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        page(LauncherPage.Collections).assertIsDisplayed()
    }
}
