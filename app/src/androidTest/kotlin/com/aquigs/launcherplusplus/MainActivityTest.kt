package com.aquigs.launcherplusplus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.ui.AppListTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun listsInstalledAppsIncludingSettings() {
        compose.onNodeWithTag(AppListTags.LIST).assertIsDisplayed()

        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }
}
