package com.sqftware.orbitlauncher.ui

import android.graphics.Color
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.WIDGET_COLUMNS
import com.sqftware.orbitlauncher.domain.WIDGET_GAP_DP
import com.sqftware.orbitlauncher.domain.WIDGET_ROW_HEIGHT_DP
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetResize
import com.sqftware.orbitlauncher.domain.WidgetSizing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class WidgetGridTest {
    @get:Rule
    val compose = createComposeRule()

    private val search = HostedWidget(id = 3, row = 0, column = 0, rows = 1, columns = 4)
    private val game = HostedWidget(id = 8, row = 1, column = 0, rows = 2, columns = 4)
    private val inert = HostedWidget(id = 5, row = 0, column = 0, rows = 1, columns = 4)
    private var page by mutableStateOf(WidgetPage())
    private val added = mutableListOf<Triple<Int, Int, Int>>()
    private val removed = mutableListOf<Int>()
    private val shown = mutableListOf<Int>()
    private val tapped = mutableListOf<Int>()
    private val resized = mutableListOf<Triple<Int, Int, Int>>()
    private val moved = mutableListOf<Triple<Int, Int, Int>>()
    private var editing by mutableStateOf<Int?>(null)
    private var storesAtOnce = true
    private var sizings = emptyMap<Int, WidgetSizing>()

    private fun show() = compose.setContent {
        WidgetGrid(
            page = page,
            actions = WidgetActions(
                view = { context, id ->
                    shown += id
                    View(context).apply {
                        setBackgroundColor(Color.RED)
                        if (id != inert.id) setOnClickListener { tapped += id }
                    }
                },
                add = { rows, columnWidth, rowHeight -> added += Triple(rows, columnWidth, rowHeight) },
                remove = removed::add,
                resize = { id, rows, columns ->
                    resized += Triple(id, rows, columns)
                    if (storesAtOnce) page = page.resize(id, rows, columns)
                },
                move = { id, row, column ->
                    moved += Triple(id, row, column)
                    page = page.move(id, row, column)
                },
                sizing = { sizings[it] ?: WidgetSizing() },
            ),
            editing = editing,
            onEditingChange = { editing = it },
        )
    }

    private val gap = WIDGET_GAP_DP.dp

    /** The part of the page that scrolls, above the add button. */
    private fun scroller() = compose.onNode(hasScrollAction())

    // The add button's touch target, in which it draws a shorter pill.
    private val buttonTarget = 48.dp
    private val pillInset = (buttonTarget - ButtonDefaults.MinHeight) / 2

    // Inside the page's padding, less the add button's touch target and the gap above it.
    private fun rowsHeight() = scroller().getUnclippedBoundsInRoot().height - 32.dp - buttonTarget - gap

    private fun pageRows() = ((rowsHeight() + gap) / (WIDGET_ROW_HEIGHT_DP.dp + gap)).toInt()

    // The rows share the height between them.
    private fun row() = (rowsHeight() + gap) / pageRows() - gap

    private fun columnWidth() = (scroller().getUnclippedBoundsInRoot().width - 32.dp - gap * (WIDGET_COLUMNS - 1)) / WIDGET_COLUMNS

    private fun columnSpan(cells: Int) = widgetSpan(cells, columnWidth())

    private fun rowPx() = with(compose.density) { (row() + gap).toPx() }

    private fun columnPx() = with(compose.density) { (columnWidth() + gap).toPx() }

    private fun bounds(widget: HostedWidget) = compose.widget(widget).getUnclippedBoundsInRoot()

    // Within a pixel, as the cells are placed on whole pixels.
    private fun assertNear(expected: Dp, actual: Dp) = assertTrue("$actual is not $expected", abs((expected - actual).value) <= 1f)

    // Inside the page's padding.
    private fun assertAddButtonAtTheBottom() {
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        assertNear(screen.bottom - 16.dp - pillInset, compose.addWidgetButton().assertIsDisplayed().getUnclippedBoundsInRoot().bottom)
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
        page = WidgetPage(listOf(search))
        show()
        val pageRows = pageRows()
        val columnWidth = columnWidth()
        val row = row()

        compose.addWidgetButton().performClick()

        compose.runOnIdle { assertEquals(listOf(Triple(pageRows, columnWidth.value.roundToInt(), row.value.roundToInt())), added) }
        assertTrue("$pageRows rows fit the page", pageRows >= 4)
    }

    @Test
    fun theRowsStretchDownToTheAddButton() {
        page = WidgetPage(listOf(search))
        show()
        val last = HostedWidget(id = 12, row = pageRows() - 1, column = 0, rows = 1, columns = 1)
        page = WidgetPage(listOf(search, last))

        assertNear(compose.addWidgetButton().getUnclippedBoundsInRoot().top - pillInset - gap, bounds(last).bottom)
        assertTrue("${row()} rows are no shorter than a row", row() >= WIDGET_ROW_HEIGHT_DP.dp)
    }

    @Test
    fun widgetsSitInTheirCellsSideBySideAndLeaveEmptyCellsEmpty() {
        val clock = HostedWidget(id = 9, row = 0, column = 0, rows = 1, columns = 2)
        val battery = HostedWidget(id = 10, row = 0, column = 3, rows = 2, columns = 1)
        val notes = HostedWidget(id = 11, row = 3, column = 1, rows = 1, columns = 3)
        page = WidgetPage(listOf(clock, battery, notes))
        show()

        val origin = bounds(clock)
        val batteryAt = bounds(battery)
        val notesAt = bounds(notes)
        assertNear(columnSpan(2), origin.width)
        assertNear(row(), origin.height)
        assertNear(origin.left + (columnWidth() + gap) * 3, batteryAt.left)
        assertNear(origin.top, batteryAt.top)
        assertNear(widgetSpan(2, row()), batteryAt.height)
        assertNear(origin.left + columnWidth() + gap, notesAt.left)
        assertNear(origin.top + (row() + gap) * 3, notesAt.top)
        assertNear(columnSpan(3), notesAt.width)
        assertAddButtonAtTheBottom()
        compose.onNodeWithText("No widgets yet").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(clock.id, battery.id, notes.id), shown) }
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
    fun aLongPressThatDragsOnMovesTheWidgetToEmptyCellsAndItStaysWhereItIsLetGo() {
        val small = game.copy(columns = 2)
        page = WidgetPage(listOf(search, small))
        show()
        val searchAt = bounds(search)
        val smallAt = bounds(small)

        compose.widget(small).performTouchInput { down(center) }
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodesWithTag(WidgetTags.EDIT).fetchSemanticsNodes().isNotEmpty() }
        compose.widget(small).performTouchInput {
            moveBy(Offset(columnPx(), rowPx()))
            moveBy(Offset(columnPx(), rowPx()))
        }
        val landing = compose.onNodeWithTag(WidgetTags.LANDING).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertNear(smallAt.left + (columnWidth() + gap) * 2, landing.left)
        assertNear(smallAt.top + (row() + gap) * 2, landing.top)
        compose.runOnIdle { assertEquals("nothing is stored while it is held", emptyList<Triple<Int, Int, Int>>(), moved) }
        compose.widget(small).performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(Triple(small.id, 3, 2)), moved) }
        compose.onNodeWithTag(WidgetTags.LANDING).assertDoesNotExist()
        assertNear(landing.left, bounds(small).left)
        assertNear(landing.top, bounds(small).top)
        assertEquals("the widget above keeps its place", searchAt, bounds(search))
        compose.runOnIdle { assertEquals("it stays edited", small.id, editing) }
    }

    @Test
    fun aPressHeldOnTheEditedWidgetMovesItAndTheWidgetsItCoversMakeWayBelow() {
        page = WidgetPage(listOf(search, game))
        show()
        val gameTop = bounds(game).top
        compose.longPressWidget(search)
        compose.runOnIdle { assertEquals("a long press that does not move moves nothing", emptyList<Triple<Int, Int, Int>>(), moved) }

        compose.widget(search).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 100L)
        compose.widget(search).performTouchInput { moveBy(Offset(0f, rowPx())) }
        compose.mainClock.advanceTimeBy(1_000)
        assertTrue("the others make way while it is held", bounds(game).top > gameTop)
        compose.widget(search).performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(Triple(search.id, 1, 0)), moved) }
        assertNear(gameTop, bounds(search).top)
        assertTrue(bounds(search).bottom <= bounds(game).top)
    }

    @Test
    fun aLongPressThatDoesNotMoveLeavesNoLandingBehind() {
        page = WidgetPage(listOf(search, game))
        show()

        compose.longPressWidget(game)

        compose.widgetEditFrame().assertIsDisplayed()
        compose.onNodeWithTag(WidgetTags.LANDING).assertDoesNotExist()
    }

    @Test
    fun aWidgetDraggedPastTheButtonAndThePagesEdgeStopsInTheCornerAboveTheButton() {
        val small = game.copy(columns = 2)
        page = WidgetPage(listOf(small))
        show()
        val pageRows = pageRows()
        val button = compose.addWidgetButton().getUnclippedBoundsInRoot()
        compose.longPressWidget(small)

        compose.widget(small).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 100L)
        compose.widget(small).performTouchInput { repeat(4) { moveBy(Offset(columnPx(), rowPx() * 5)) } }
        compose.mainClock.advanceTimeBy(1_000)
        val landing = compose.onNodeWithTag(WidgetTags.LANDING).getUnclippedBoundsInRoot()
        assertNear(landing.bottom, bounds(small).bottom)
        assertNear(landing.right, bounds(small).right)
        assertTrue("it is held above the button", bounds(small).bottom <= button.top)
        compose.widget(small).performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(Triple(small.id, pageRows - small.rows, WIDGET_COLUMNS - small.columns)), moved) }
        assertTrue("it lands above the button", bounds(small).bottom <= button.top)
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
    fun theHandleResizesTheWidgetInWholeCellsBothWaysAndStoresThemOnRelease() {
        // First, so that at the page's full height its handle is still on screen.
        page = WidgetPage(listOf(game.copy(row = 0), search.copy(row = 2)))
        show()
        val top = game.copy(row = 0)
        compose.longPressWidget(top)

        compose.widgetResizeHandle().performTouchInput {
            down(center)
            moveBy(Offset(-columnPx() * 1.3f, rowPx() * 1.7f))
        }
        assertNear(widgetSpan(4, row()), bounds(top).height)
        assertNear(columnSpan(3), compose.widgetEditFrame().getUnclippedBoundsInRoot().width)
        compose.runOnIdle { assertEquals("nothing is stored while the handle is held", emptyList<Triple<Int, Int, Int>>(), resized) }
        compose.widgetResizeHandle().performTouchInput { up() }
        compose.runOnIdle { assertEquals(listOf(Triple(top.id, 4, 3)), resized) }
        assertNear(widgetSpan(4, row()), bounds(top).height)
        assertNear(columnSpan(3), bounds(top).width)
        assertTrue("the widget below makes way", bounds(top).bottom <= bounds(search).top)

        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx() * 50) }
        val tallest = compose.runOnIdle { resized.last().second }
        assertTrue("$tallest rows", tallest > 4)
        assertTrue("the widget stops above the button", bounds(top).bottom <= compose.addWidgetButton().getUnclippedBoundsInRoot().top)
    }

    @Test
    fun theHandleStopsAtThePagesRightEdge() {
        val small = HostedWidget(id = 9, row = 0, column = 2, rows = 1, columns = 1)
        page = WidgetPage(listOf(small))
        show()
        compose.longPressWidget(small)

        compose.widgetResizeHandle().performTouchInput { swipe(center, center + Offset(columnPx() * 3, 0f)) }

        compose.runOnIdle { assertEquals(listOf(Triple(small.id, 1, 2)), resized) }
    }

    @Test
    fun theWidgetHoldsTheDraggedCellsUntilTheyAreStored() {
        page = WidgetPage(listOf(game))
        storesAtOnce = false
        show()
        compose.longPressWidget(game)

        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx()) }
        compose.runOnIdle { assertEquals(listOf(Triple(game.id, 3, 4)), resized) }
        assertNear(widgetSpan(3, row()), compose.widgetHeight(game))

        page = page.resize(game.id, 3, 4)
        assertNear(widgetSpan(3, row()), compose.widgetHeight(game))
    }

    @Test
    fun aWidgetTallerThanThePageKeepsItsRowsUntilItIsDraggedAndResizesFromThere() {
        page = WidgetPage(listOf(search))
        show()
        val rows = pageRows() + 2
        val tall = HostedWidget(id = 11, row = 0, column = 0, rows = rows, columns = 4)
        page = WidgetPage(listOf(tall))
        compose.longPressWidget(tall)
        // The last widget's handle reaches into the page's bottom padding, which performScrollTo never counts as in view,
        // so it would scroll forever; a page's height takes the widget's two extra rows to the end.
        val pageHeight = scroller().fetchSemanticsNode().size.height.toFloat()
        scroller().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, pageHeight) }
        compose.widgetResizeHandle().assertIsDisplayed()

        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx() * 0.3f) }
        assertNear(widgetSpan(rows, row()), compose.widgetHeight(tall))
        compose.widgetResizeHandle().performTouchInput { swipeUp(centerY, centerY - rowPx()) }
        compose.runOnIdle { assertEquals("only a change is stored", listOf(Triple(tall.id, rows - 1, 4)), resized) }
    }

    @Test
    fun aWidgetThatCannotTakeAnotherSizeHasNoHandle() {
        page = WidgetPage(listOf(search, game))
        // Minimums that take the widgets' own cells.
        val fullWidth = WidgetResize(resizable = false, minDp = 2000)
        sizings = mapOf(
            game.id to WidgetSizing(vertical = WidgetResize(resizable = false, minDp = 160), horizontal = fullWidth),
            search.id to WidgetSizing(vertical = WidgetResize(maxResizeDp = 80), horizontal = fullWidth),
        )
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
        page = WidgetPage(listOf(search))
        show()
        val tall = HostedWidget(id = 11, row = 1, column = 0, rows = pageRows(), columns = 4)
        page = WidgetPage(listOf(search, tall))
        compose.longPressWidget(search)
        val top = bounds(search).top

        compose.widget(tall).performTouchInput { swipeUp(centerY, centerY - rowPx() * 2) }

        assertTrue("the page scrolled", bounds(search).top < top)
        compose.runOnIdle { assertEquals(search.id, editing) }
    }
}
