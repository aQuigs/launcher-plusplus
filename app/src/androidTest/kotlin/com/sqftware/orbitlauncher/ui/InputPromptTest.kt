package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.AppEntry
import com.sqftware.orbitlauncher.domain.ChatApp
import com.sqftware.orbitlauncher.domain.ChatValue
import com.sqftware.orbitlauncher.domain.FoundChat
import com.sqftware.orbitlauncher.domain.InputKind
import com.sqftware.orbitlauncher.domain.InputQuestion
import com.sqftware.orbitlauncher.domain.InputValue
import com.sqftware.orbitlauncher.domain.KeptInputs
import com.sqftware.orbitlauncher.domain.PlaceValue
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputPromptTest {
    @get:Rule
    val compose = createComposeRule()

    private val home = PlaceValue("Home", "2 Elm St", 37.0, -122.0)
    private val work = PlaceValue("Work", "1 Main St", 37.5, -122.25)
    private val uber = AppEntry("Uber", "com.ubercab", "com.ubercab.Main")

    private val slack = AppEntry("Slack", "com.Slack", "com.Slack.Main")
    private val general = ChatValue(ChatApp.Slack, "general", "slack://channel?team=T1&id=C1")
    private val random = ChatValue(ChatApp.Slack, "random", "slack://channel?team=T1&id=C2")

    private var chats by mutableStateOf(emptyMap<ChatApp, KeptInputs<ChatValue>>())
    private var places by mutableStateOf(KeptInputs<PlaceValue>())
    private var query by mutableStateOf("")
    private val answers = mutableListOf<InputValue>()
    private var found = CompletableDeferred<List<PlaceValue>>()

    private fun show(question: InputQuestion = InputQuestion(InputKind.Place, listOf(uber))) = compose.setContent {
        InputPrompt(
            question = question,
            query = query,
            onQueryChange = { query = it },
            sources = InputSources(
                places = places,
                onPlacesChange = { places = it },
                findPlaces = { found.await() },
                openMaps = {},
                chats = chats,
                onChatsChange = { app, kept -> chats = chats + (app to kept) },
                findChat = { app, typed -> (app.find(typed) as? FoundChat.Whole)?.chat },
                pasted = { random.link },
            ),
            onAnswer = answers::add,
            onDismiss = {},
        )
    }

    @Test
    fun aRecentPlaceIsStarredIntoTheFavoritesAndBackAndDeleted() {
        places = KeptInputs(recent = listOf(home, work))
        show()
        compose.awaitSettled("Work")
        compose.onNodeWithText("Favorites").assertDoesNotExist()

        compose.onNodeWithContentDescription("Add Work to favorites").performClick()
        compose.onNodeWithText("Favorites").assertIsDisplayed()
        compose.onNodeWithContentDescription("Delete Work from recent").assertDoesNotExist()
        compose.runOnIdle { assertEquals(KeptInputs(favorites = listOf(work), recent = listOf(home)), places) }

        compose.awaitSettled("Work")
        compose.onNodeWithContentDescription("Remove Work from favorites").performClick()
        compose.awaitSettled("Work")
        compose.onNodeWithContentDescription("Delete Work from recent").performClick()
        compose.runOnIdle {
            assertEquals(KeptInputs(recent = listOf(home)), places)
            assertEquals(emptyList<InputValue>(), answers)
        }
    }

    @Test
    fun aPlaceFoundIsStarredWithoutBeingPickedAndOffersNoDelete() {
        places = KeptInputs(recent = listOf(work))
        found.complete(listOf(home, work))
        show()
        compose.onNodeWithTag(OpenWithTags.FIELD).performTextInput("elm")
        compose.awaitSettled("Home")

        compose.onNodeWithContentDescription("Delete Work from recent").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add Home to favorites").performClick()

        compose.onNodeWithContentDescription("Remove Home from favorites").assertIsEnabled()
        compose.runOnIdle {
            assertEquals(KeptInputs(favorites = listOf(home), recent = listOf(work)), places)
            assertEquals(emptyList<InputValue>(), answers)
        }
    }

    @Test
    fun placesThatHaveJustComeBackTakeNoTapAndMoveNoButton() {
        show()
        compose.onNodeWithTag(OpenWithTags.FIELD).performTextInput("elm")
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(hasText("Searching…")).fetchSemanticsNodes().isNotEmpty() }
        val cancel = compose.onNodeWithText("Cancel").getBoundsInRoot()

        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1_000)
        found.complete(listOf(home))
        compose.mainClock.advanceTimeBy(100)

        assertEquals(cancel, compose.onNodeWithText("Cancel").getBoundsInRoot())
        compose.onNodeWithText("Home").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithContentDescription("Add Home to favorites").assertIsNotEnabled()
        compose.mainClock.advanceTimeBy(100)
        assertEquals(emptyList<InputValue>(), answers)

        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Home").assertIsEnabled().performTouchInput { click() }
        compose.mainClock.advanceTimeBy(100)
        assertEquals(listOf<InputValue>(home), answers)
    }

    @Test
    fun placesFoundForWhatWasTypedBeforeTakeNoTapOnceTheTextChanges() {
        found.complete(listOf(home))
        show()
        compose.onNodeWithTag(OpenWithTags.FIELD).performTextInput("elm")
        compose.awaitSettled("Home")

        found = CompletableDeferred()
        compose.runOnIdle { query = "Ferry Building, San Francisco" }

        compose.onNodeWithText("Home").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(emptyList<InputValue>(), answers) }
    }

    @Test
    fun aChatPastedIsNamedThenPickedAndKept() {
        show(InputQuestion(ChatApp.Slack, listOf(slack)))
        compose.onNodeWithText("Pick a Slack channel").assertIsDisplayed()
        compose.onNodeWithText(ChatApp.Slack.howToLink).assertIsDisplayed()

        compose.onNodeWithText("Paste").performClick()
        compose.awaitSettled("New channel")
        compose.onNodeWithText("New channel").performClick()
        compose.onNodeWithText("Name this channel").assertIsDisplayed()
        compose.onNodeWithTag(OpenWithTags.NAME).performTextInput(" random ")
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle {
            assertEquals(listOf<InputValue>(random), answers)
            assertEquals(mapOf(ChatApp.Slack to KeptInputs(recent = listOf(random))), chats)
        }
    }

    @Test
    fun aNameNoSavedChatHasSaysHowToAddOne() {
        show(InputQuestion(ChatApp.Slack, listOf(slack)))

        compose.onNodeWithTag(OpenWithTags.FIELD).performTextInput("orbit")

        compose.awaitSettled("No saved channel found. ${ChatApp.Slack.howToLink}.")
    }

    @Test
    fun aChatKeptIsFoundByNameOrLinkAndPickedWithoutNamingItAgain() {
        chats = mapOf(ChatApp.Slack to KeptInputs(favorites = listOf(general), recent = listOf(random)))
        show(InputQuestion(ChatApp.Slack, listOf(slack)))

        compose.onNodeWithTag(OpenWithTags.FIELD).performTextInput("GENE")
        compose.awaitSettled("general")
        compose.onNodeWithText("random").assertDoesNotExist()
        compose.onNodeWithText("Paste").performClick()
        compose.awaitSettled("random")
        compose.onNodeWithText("random").performClick()

        compose.runOnIdle { assertEquals(listOf<InputValue>(random), answers) }
    }
}
