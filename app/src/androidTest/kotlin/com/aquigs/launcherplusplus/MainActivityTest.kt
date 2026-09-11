package com.aquigs.launcherplusplus

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
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
        val list = compose.onNodeWithTag(AppListTags.LIST)
        list.assertIsDisplayed()

        compose.waitUntil(timeoutMillis = 10_000) {
            list.onChildren().fetchSemanticsNodes().isNotEmpty()
        }
        // Images with many preinstalled apps push Settings below the fold, where LazyColumn has not composed it yet.
        list.performScrollToNode(hasText("Settings"))
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }
}
