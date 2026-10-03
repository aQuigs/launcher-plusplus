package com.sqftware.orbitlauncher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.PageContents
import com.sqftware.orbitlauncher.domain.PageKind
import com.sqftware.orbitlauncher.domain.PageLayout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageEditorTest {
    @get:Rule
    val compose = createComposeRule()

    private var layout by mutableStateOf(PageLayout())
    private val opened = mutableListOf<LauncherPage>()
    private val empty = LauncherPage("ring-2", PageKind.Ring)

    private fun show() = compose.setContent {
        PageEditor(
            layout = layout,
            contentsOf = { page ->
                when (page.kind) {
                    PageKind.Ring -> if (page == LauncherPage.Home) PageContents.Ring(slots = 4, apps = 4, folders = 0) else PageContents.Ring(0, 0, 0)
                    PageKind.Collections -> PageContents.Cards(3)
                    PageKind.Widgets -> PageContents.Widgets(emptyList())
                }
            },
            onLayoutChange = { layout = it },
            onOpen = opened::add,
        )
    }

    @Test
    fun aPlusAtEitherEndAddsThePickedKindThere() {
        show()

        compose.onNodeWithTag(PageEditorTags.add(atStart = true)).performClick()
        compose.addPageOf(PageKind.Collections)
        compose.onNodeWithTag(PageEditorTags.add(atStart = false)).performScrollTo().performClick()
        compose.addPageOf(PageKind.Ring)

        compose.runOnIdle {
            assertEquals(PageKind.Collections, layout.pages.first().kind)
            assertEquals(PageKind.Ring, layout.pages.last().kind)
            assertEquals(5, layout.pages.size)
        }
    }

    @Test
    fun homeCannotBeDeletedAnEmptyPageGoesAtOnceAndOneWithSomethingOnItAsksFirst() {
        layout = PageLayout(PageLayout().pages + empty)
        show()
        compose.onNodeWithTag(PageEditorTags.delete(LauncherPage.Home)).assertDoesNotExist()

        compose.onNodeWithTag(PageEditorTags.delete(empty)).performScrollTo().performClick()
        compose.onNodeWithTag(PageEditorTags.DELETE_DIALOG).assertDoesNotExist()
        compose.runOnIdle { assertEquals(PageLayout(), layout) }

        compose.onNodeWithTag(PageEditorTags.delete(LauncherPage.Collections)).performScrollTo().performClick()
        compose.onNodeWithText("This deletes 3 collections. The apps stay installed.").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(PageLayout(), layout) }

        compose.onNodeWithTag(PageEditorTags.delete(LauncherPage.Collections)).performScrollTo().performClick()
        compose.onNodeWithText("Delete").performClick()
        compose.runOnIdle { assertEquals(listOf(LauncherPage.Widgets, LauncherPage.Home), layout.pages) }
    }

    @Test
    fun aFullLayoutHasNoPlus() {
        layout = generateSequence(PageLayout()) { it.add(PageKind.Widgets, atStart = false) }.first { it.isFull }
        show()

        compose.onNodeWithTag(PageEditorTags.add(atStart = true)).assertDoesNotExist()
        compose.onNodeWithTag(PageEditorTags.add(atStart = false)).assertDoesNotExist()
    }

    @Test
    fun aTapOpensAPageAndAHeldOneDragsToAnotherPlace() {
        show()

        compose.onNodeWithTag(PageEditorTags.page(LauncherPage.Collections)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(LauncherPage.Collections), opened) }

        compose.onNodeWithTag(PageEditorTags.page(LauncherPage.Widgets)).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(width * 1.2f, 0f))
            moveBy(Offset(width * 1.2f, 0f))
            up()
        }
        compose.runOnIdle { assertEquals(listOf(LauncherPage.Home, LauncherPage.Collections, LauncherPage.Widgets), layout.pages) }
    }
}
