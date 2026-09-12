package com.aquigs.launcherplusplus.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppListTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsEveryAppLabel() {
        compose.setContent { AppList(apps = listOf(clock, mail), onLaunch = {}) }

        compose.onNodeWithTag(AppListTags.LIST).assertIsDisplayed()
        compose.onNodeWithText("Clock").assertIsDisplayed()
        compose.onNodeWithText("Mail").assertIsDisplayed()
    }

    @Test
    fun tappingAnAppLaunchesIt() {
        val launched = mutableListOf<AppEntry>()
        compose.setContent { AppList(apps = listOf(clock, mail), onLaunch = launched::add) }

        compose.onNodeWithText("Mail").performClick()

        assertEquals(listOf(mail), launched)
    }
}
