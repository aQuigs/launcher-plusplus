package com.sqftware.orbitlauncher.ui

import android.graphics.Color
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.WIDGET_ROW_HEIGHT_DP
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetSizing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetColumnTest {
    @get:Rule
    val compose = createComposeRule()

    private val search = HostedWidget(id = 3, rows = 1)
    private val game = HostedWidget(id = 8, rows = 2)
    private val inert = HostedWidget(id = 5, rows = 1)
    private var page by mutableStateOf(WidgetPage())
    private val added = mutableListOf<Int>()
    private val removed = mutableListOf<Int>()
    private val shown = mutableListOf<Int>()
    private val tapped = mutableListOf<Int>()
    private val resized = mutableListOf<Pair<Int, Int>>()
    private var editing by mutableStateOf<Int?>(null)
    private var storesAtOnce = true
    private var sizings = emptyMap<Int, WidgetSizing>()

    private fun show() = compose.setContent {
        WidgetColumn(
            page = page,
            actions = WidgetActions(
                view = { context, id ->
                    shown += id
                    View(context).apply {
                        setBackgroundColor(Color.RED)
                        if (id != inert.id) setOnClickListener { tapped += id }
                    }
                },
                add = added::add,
                remove = removed::add,
                resize = { id, rows ->
                    resized += id to rows
                    if (storesAtOnce) page = page.resize(id, rows)
                },
                sizing = { sizings[it] ?: WidgetSizing() },
            ),
            editing = editing,
            onEditingChange = { editing = it },
        )
    }

    private val row = WIDGET_ROW

    /** The part of the page that scrolls, above the add button. */
    private fun scroller() = compose.onNode(hasScrollAction())

    private fun pageRows() = ((scroller().getUnclippedBoundsInRoot().height - 32.dp) / row).toInt()

    private fun rowPx() = with(compose.density) { row.toPx() }

    // Within its bottom padding and the button's own touch margin.
    private fun assertAddButtonAtTheBottom() {
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val button = compose.addWidgetButton().assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("$button is not at the bottom of $screen", screen.bottom - button.bottom < 24.dp)
    }

    @Test
    fun anEmptyPageSaysSoInTheMiddleWithTheAddButtonAtTheBottom() {
        show()

        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val hint = compose.onNodeWithText("No widgets yet").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("$hint is not in the middle of $screen", hint.top > screen.height / 3 && hint.bottom < screen.height * 2 / 3)
        assertAddButtonAtTheBottom()
    }

    @Test
    fun theAddButtonAsksForAWidgetSizedForThePage() {
        show()
        val pageRows = pageRows()

        compose.addWidgetButton().performClick()

        compose.runOnIdle { assertEquals(listOf(pageRows), added) }
        assertTrue("$pageRows rows fit the page", pageRows >= 4)
    }

    @Test
    fun widgetsStackInOrderAtTheirHeightsWithTheButtonAtTheBottom() {
        page = WidgetPage(listOf(search, game))
        show()

        val first = compose.widget(search).assertIsDisplayed().getUnclippedBoundsInRoot()
        val second = compose.widget(game).assertIsDisplayed().getUnclippedBoundsInRoot()
        val button = compose.addWidgetButton().getUnclippedBoundsInRoot()
        assertEquals(WIDGET_ROW_HEIGHT_DP.dp, first.height)
        assertEquals((2 * WIDGET_ROW_HEIGHT_DP).dp, second.height)
        assertTrue("$first, $second, $button", first.bottom <= second.top && second.bottom <= button.top)
        assertAddButtonAtTheBottom()
        compose.onNodeWithText("No widgets yet").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(search.id, game.id), shown) }
    }

    @Test
    fun aLongPressEditsTheWidgetAndWhileItDoesNoWidgetTakesATap() {
        page = WidgetPage(listOf(search, game))
        show()

        compose.widget(game).performClick()
        compose.runOnIdle { assertEquals(listOf(game.id), tapped) }

        compose.longPressWidget(game)
        compose.widgetEditFrame().assertIsDisplayed()
        compose.widgetRemoveButton().assertIsDisplayed()
        compose.widgetResizeHandle().assertIsDisplayed()
        compose.runOnIdle { assertEquals(game.id, editing) }

        compose.widget(game).performClick()
        compose.widgetResizeHandle().performClick()
        compose.runOnIdle { assertEquals("a tap on the edited widget or its handle keeps it edited", game.id, editing) }
        compose.widget(search).performClick()
        compose.widgetEditFrame().assertDoesNotExist()

        compose.longPressWidget(game)
        scroller().performTouchInput { click(bottomCenter - Offset(0f, 10f)) }
        compose.widgetEditFrame().assertDoesNotExist()

        compose.longPressWidget(game)
        compose.addWidgetButton().performClick()
        compose.widgetEditFrame().assertDoesNotExist()
        compose.runOnIdle { assertEquals("no tap reached a widget while one was edited", listOf(game.id), tapped) }
    }

    @Test
    fun aWidgetThatIgnoresTouchesIsEditedOnALongPressButNotOnATap() {
        page = WidgetPage(listOf(inert))
        show()

        compose.widget(inert).performClick()
        compose.waitOutWidgetLongPress()
        compose.widgetEditFrame().assertDoesNotExist()

        compose.longPressWidget(inert)
        compose.widgetEditFrame().assertIsDisplayed()
    }

    @Test
    fun theBinAndTheHandleOfAWidgetOneRowTallDoNotOverlap() {
        page = WidgetPage(listOf(search, game))
        show()
        compose.longPressWidget(search)

        // The handle's outer corner, past the widget's own bounds, then the bin's lower edge, nearest the handle.
        compose.widgetResizeHandle().performTouchInput { click(bottomRight - Offset(4f, 4f)) }
        compose.runOnIdle { assertEquals(search.id, editing) }
        compose.widgetRemoveButton().performTouchInput { click(bottomCenter - Offset(0f, 4f)) }

        compose.runOnIdle {
            assertEquals(listOf(search.id), removed)
            assertNull(editing)
        }
    }

    @Test
    fun theHandleResizesTheWidgetInWholeRowsAsItIsDraggedAndStoresTheRowsOnRelease() {
        // First, so that at the page's full height its handle is still on screen.
        page = WidgetPage(listOf(game, search))
        show()
        compose.longPressWidget(game)

        compose.widgetResizeHandle().performTouchInput {
            down(center)
            moveBy(Offset(0f, rowPx() * 1.7f))
        }
        assertEquals(row * 4, compose.widgetHeight(game))
        compose.runOnIdle { assertEquals("nothing is stored while the handle is held", emptyList<Pair<Int, Int>>(), resized) }
        compose.widgetResizeHandle().performTouchInput { up() }
        compose.runOnIdle { assertEquals(listOf(game.id to 4), resized) }
        assertEquals(row * 4, compose.widgetHeight(game))

        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx() * 50) }
        val tallest = compose.runOnIdle { resized.last().second }
        assertTrue("$tallest rows", tallest > 4)
        val shown = scroller().getUnclippedBoundsInRoot()
        assertTrue("the widget stops above the button", compose.widget(game).getUnclippedBoundsInRoot().bottom <= shown.bottom - 16.dp)
    }

    @Test
    fun theWidgetHoldsTheDraggedRowsUntilTheyAreStored() {
        page = WidgetPage(listOf(game))
        storesAtOnce = false
        show()
        compose.longPressWidget(game)

        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx()) }
        compose.runOnIdle { assertEquals(listOf(game.id to 3), resized) }
        assertEquals(row * 3, compose.widgetHeight(game))

        page = page.resize(game.id, 3)
        assertEquals(row * 3, compose.widgetHeight(game))
    }

    @Test
    fun aWidgetTallerThanThePageKeepsItsRowsUntilItIsDraggedAndResizesFromThere() {
        show()
        val rows = pageRows() + 2
        val tall = HostedWidget(id = 11, rows = rows)
        page = WidgetPage(listOf(tall))
        compose.longPressWidget(tall)
        // The last widget's handle reaches into the page's bottom padding, which performScrollTo never counts as in view,
        // so it would scroll forever; a page's height takes the widget's two extra rows to the end.
        val pageHeight = scroller().fetchSemanticsNode().size.height.toFloat()
        scroller().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, pageHeight) }
        compose.widgetResizeHandle().assertIsDisplayed()

        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx() * 0.3f) }
        assertEquals(row * rows, compose.widgetHeight(tall))
        compose.widgetResizeHandle().performTouchInput { swipeUp(centerY, centerY - rowPx()) }
        compose.runOnIdle { assertEquals("only a change is stored", listOf(tall.id to rows - 1), resized) }
    }

    @Test
    fun aWidgetThatCannotTakeAnotherHeightHasNoHandle() {
        page = WidgetPage(listOf(search, game))
        sizings = mapOf(game.id to WidgetSizing(vertical = false), search.id to WidgetSizing(maxResizeHeightDp = 80))
        show()

        compose.longPressWidget(game)
        compose.widgetRemoveButton().assertIsDisplayed()
        compose.widgetResizeHandle().assertDoesNotExist()

        compose.widget(search).performClick()
        compose.longPressWidget(search)
        compose.widgetRemoveButton().assertIsDisplayed()
        compose.widgetResizeHandle().assertDoesNotExist()
    }

    @Test
    fun aScrollStartingOnAWidgetStillScrollsThePageWhileOneIsEdited() {
        show()
        val tall = HostedWidget(id = 11, rows = pageRows())
        page = WidgetPage(listOf(search, tall))
        compose.longPressWidget(search)
        val top = compose.widget(search).getUnclippedBoundsInRoot().top

        compose.widget(tall).performTouchInput { swipeUp(centerY, centerY - rowPx() * 2) }

        assertTrue("the page scrolled", compose.widget(search).getUnclippedBoundsInRoot().top < top)
        compose.runOnIdle { assertEquals(search.id, editing) }
    }
}
