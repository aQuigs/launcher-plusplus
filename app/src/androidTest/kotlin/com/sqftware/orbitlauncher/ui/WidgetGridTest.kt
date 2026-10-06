package com.sqftware.orbitlauncher.ui

import android.graphics.Color
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
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
import com.sqftware.orbitlauncher.domain.RING_ID
import com.sqftware.orbitlauncher.domain.WIDGET_COLUMNS
import com.sqftware.orbitlauncher.domain.WIDGET_GAP_DP
import com.sqftware.orbitlauncher.domain.WIDGET_ROW_HEIGHT_DP
import com.sqftware.orbitlauncher.domain.widgetRowsWithin
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

@RunWith(AndroidJUnit4::class)
class WidgetGridTest {
    @get:Rule
    val compose = createComposeRule()

    private val search = HostedWidget(id = 3, row = 0, column = 0, rows = 1, columns = 4)
    private val game = HostedWidget(id = 8, row = 1, column = 0, rows = 2, columns = 4)
    private val inert = HostedWidget(id = 5, row = 0, column = 0, rows = 1, columns = 4)
    private var page by mutableStateOf(WidgetPage())
    private val added = mutableListOf<Triple<Int, Float, Float>>()
    private val removed = mutableListOf<Int>()
    private val shown = mutableListOf<Int>()
    private val tapped = mutableListOf<Int>()
    private val arranged = mutableListOf<WidgetPage>()
    private var editing by mutableStateOf<Int?>(null)
    private var storesAtOnce = true
    private var sizings = emptyMap<Int, WidgetSizing>()

    private var ringTaps = 0

    private fun show(ring: (@Composable (Modifier) -> Unit)? = null) = compose.setContent {
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
                arrange = {
                    arranged += it
                    if (storesAtOnce) page = it
                },
                sizing = { sizings[it] ?: WidgetSizing() },
            ),
            editing = editing,
            onEditingChange = { editing = it },
            ring = ring,
        )
    }

    private fun showRingPage() = show { modifier -> Box(modifier.clickable { ringTaps++ }) }

    private val gap = WIDGET_GAP_DP.dp

    /** The widget [id] as each page stored had it. */
    private fun stored(id: Int) = arranged.map { page -> page.widgets.single { it.id == id } }

    /** The page, which scrolls, add button and all. */
    private fun scroller() = compose.onNode(hasScrollAction())

    // Inside the page's padding, down to the gap above the add button: only while the page fits the screen, since the
    // button follows the rows of a longer one.
    private fun rows() = widgetRowsWithin(
        (compose.addWidgetButton().getUnclippedBoundsInRoot().top - gap - scroller().getUnclippedBoundsInRoot().top - 16.dp).value,
    )

    private fun pageRows() = rows().count

    private fun row() = rows().heightDp.dp

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
        assertNear(screen.bottom - 16.dp, compose.addWidgetButton().assertIsDisplayed().getUnclippedBoundsInRoot().bottom)
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

        val (rows, width, height) = compose.runOnIdle { added.single() }
        assertEquals(pageRows, rows)
        assertNear(columnWidth, width.dp)
        assertNear(row, height.dp)
        assertTrue("$pageRows rows fit the page", pageRows >= 4)
    }

    @Test
    fun aLongPressWhereNoWidgetIsAsksForOneAndEndsEditing() {
        page = WidgetPage(listOf(search))
        show()
        val pageRows = pageRows()
        compose.longPressWidget(search)
        compose.runOnIdle { assertEquals("a long press on a widget edits it", emptyList<Triple<Int, Float, Float>>(), added) }

        scroller().performTouchInput { longClick(center) }

        compose.runOnIdle {
            assertEquals(pageRows, added.single().first)
            assertNull(editing)
        }
    }

    @Test
    fun theRowsStretchDownToTheAddButton() {
        page = WidgetPage(listOf(search))
        show()
        val last = HostedWidget(id = 12, row = pageRows() - 1, column = 0, rows = 1, columns = 1)
        page = WidgetPage(listOf(search, last))

        assertNear(compose.addWidgetButton().getUnclippedBoundsInRoot().top - gap, bounds(last).bottom)
        assertTrue("${bounds(last).height} is shorter than a row", bounds(last).height >= WIDGET_ROW_HEIGHT_DP.dp)
    }

    @Test
    fun aPageLongerThanTheScreenTakesTheAddButtonDownBelowItsLastWidget() {
        page = WidgetPage(listOf(search))
        show()
        val below = HostedWidget(id = 12, row = pageRows(), column = 0, rows = 2, columns = 1)
        page = WidgetPage(listOf(search, below))

        assertNear(bounds(below).bottom + gap, compose.addWidgetButton().getUnclippedBoundsInRoot().top)
        val pageHeight = scroller().fetchSemanticsNode().size.height.toFloat()
        scroller().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, pageHeight) }
        assertAddButtonAtTheBottom()
        assertNear(bounds(below).bottom + gap, compose.addWidgetButton().getUnclippedBoundsInRoot().top)
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
        compose.runOnIdle { assertEquals("nothing is stored while it is held", emptyList<WidgetPage>(), arranged) }
        compose.widget(small).performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(small.copy(row = 3, column = 2)), stored(small.id)) }
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
        compose.runOnIdle { assertEquals("a long press that does not move moves nothing", emptyList<WidgetPage>(), arranged) }

        compose.widget(search).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 100L)
        compose.widget(search).performTouchInput { moveBy(Offset(0f, rowPx())) }
        compose.mainClock.advanceTimeBy(1_000)
        assertTrue("the others make way while it is held", bounds(game).top > gameTop)
        compose.widget(search).performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(search.copy(row = 1)), stored(search.id)) }
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
    fun aWidgetHeldAtTheBottomEdgeScrollsThePageAScreenFurtherAtMost() {
        val tall = HostedWidget(id = 11, row = 0, column = 0, rows = 3, columns = 2)
        page = WidgetPage(listOf(tall))
        show()
        val pageRows = pageRows()
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        compose.longPressWidget(tall)

        // Held by its middle and taken just inside the page's bottom edge: the finger alone would leave it a row short of
        // the last one, and its lower half past the screen.
        compose.widget(tall).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 100L)
        val edge = with(compose.density) { Offset(((bounds(tall).left + bounds(tall).right) / 2).toPx(), (screen.bottom - 20.dp).toPx()) }
        compose.onRoot().performTouchInput { moveTo(edge) }
        compose.mainClock.advanceTimeBy(10_000)
        val landing = compose.onNodeWithTag(WidgetTags.LANDING).getUnclippedBoundsInRoot()
        assertNear(landing.top, bounds(tall).top)
        assertNear(bounds(tall).bottom + gap, compose.addWidgetButton().getUnclippedBoundsInRoot().top)
        compose.onRoot().performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(tall.copy(row = pageRows * 2 - tall.rows, column = 0)), stored(tall.id)) }
        assertNear(bounds(tall).bottom + gap, compose.addWidgetButton().getUnclippedBoundsInRoot().top)
        assertTrue("it shows in full once it lands", bounds(tall).bottom <= screen.bottom)
    }

    @Test
    fun aLongPressAtTheBottomEdgeOfAPageThatScrollsMovesNothing() {
        page = WidgetPage(listOf(search))
        show()
        val low = HostedWidget(id = 11, row = pageRows() - 1, column = 0, rows = 2, columns = 2)
        page = WidgetPage(listOf(search, low))
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        compose.longPressWidget(low)

        val press = with(compose.density) { (screen.bottom - 20.dp - bounds(low).top).toPx() }
        compose.widget(low).performTouchInput { down(Offset(centerX, press)) }
        compose.mainClock.advanceTimeBy(ViewConfiguration.getLongPressTimeout() + 1_000L)
        compose.widget(low).performTouchInput { up() }

        compose.runOnIdle { assertEquals(emptyList<WidgetPage>(), arranged) }
    }

    @Test
    fun theHandleHeldPastTheBottomScrollsThePageAndStretchesTheWidgetBelowIt() {
        page = WidgetPage(listOf(search))
        show()
        val pageRows = pageRows()
        val last = HostedWidget(id = 12, row = pageRows - 1, column = 0, rows = 1, columns = 2)
        page = WidgetPage(listOf(search, last))
        compose.longPressWidget(last)

        compose.widgetResizeHandle().performTouchInput {
            down(center)
            moveBy(Offset(0f, rowPx() * 3))
        }
        compose.mainClock.advanceTimeBy(5_000)
        compose.widgetResizeHandle().performTouchInput { up() }

        compose.runOnIdle { assertEquals(listOf(last.copy(rows = pageRows, columns = 2)), stored(last.id)) }
        assertNear(bounds(last).bottom + gap, compose.addWidgetButton().getUnclippedBoundsInRoot().top)
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
        compose.runOnIdle { assertEquals("nothing is stored while the handle is held", emptyList<WidgetPage>(), arranged) }
        compose.widgetResizeHandle().performTouchInput { up() }
        compose.runOnIdle { assertEquals(listOf(top.copy(rows = 4, columns = 3)), stored(top.id)) }
        assertNear(widgetSpan(4, row()), bounds(top).height)
        assertNear(columnSpan(3), bounds(top).width)
        assertTrue("the widget below makes way", bounds(top).bottom <= bounds(search).top)

        val pageRows = pageRows()
        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx() * 50) }
        compose.runOnIdle { assertEquals("the widget stops at the page's rows", pageRows, stored(top.id).last().rows) }
    }

    @Test
    fun theHandleStopsAtThePagesRightEdge() {
        val small = HostedWidget(id = 9, row = 0, column = 2, rows = 1, columns = 1)
        page = WidgetPage(listOf(small))
        show()
        compose.longPressWidget(small)

        compose.widgetResizeHandle().performTouchInput { swipe(center, center + Offset(columnPx() * 3, 0f)) }

        compose.runOnIdle { assertEquals(listOf(small.copy(rows = 1, columns = 2)), stored(small.id)) }
    }

    @Test
    fun theWidgetHoldsTheDraggedCellsUntilTheyAreStored() {
        page = WidgetPage(listOf(game))
        storesAtOnce = false
        show()
        compose.longPressWidget(game)

        compose.widgetResizeHandle().performTouchInput { swipeDown(centerY, centerY + rowPx()) }
        compose.runOnIdle { assertEquals(listOf(game.copy(rows = 3, columns = 4)), stored(game.id)) }
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
        compose.runOnIdle { assertEquals("only a change is stored", listOf(tall.copy(rows = rows - 1, columns = 4)), stored(tall.id)) }
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

    @Test
    fun aRingPageShowsTheRingAcrossItsMiddleAndNeitherScrollsNorOffersTheButton() {
        showRingPage()

        compose.waitUntil(timeoutMillis = 5_000) { page.ring != null }
        val ring = page.ring!!
        val screen = compose.onRoot().getUnclippedBoundsInRoot()
        val shown = compose.onNodeWithTag(WidgetTags.RING).assertIsDisplayed().getUnclippedBoundsInRoot()
        assertNear(screen.width - 32.dp, shown.width)
        assertTrue("$shown is not about as tall as it is wide", shown.height >= shown.width && ring.row > 0)
        assertTrue("$shown is not in the middle of $screen", abs((shown.top - screen.top - (screen.bottom - shown.bottom)).value) < shown.height.value / ring.rows * 2)
        compose.addWidgetButton().assertDoesNotExist()
        compose.onNode(hasScrollAction()).assertDoesNotExist()
    }

    @Test
    fun theEditedRingTakesNoTapsAndDragsUpWhileTheWidgetsMakeWayBelowIt() {
        showRingPage()
        compose.waitUntil(timeoutMillis = 5_000) { page.ring != null }
        val ring = page.ring!!
        page = page.add(search.id, rows = 1, columns = 4, pageRows = ring.bottom)
        editing = RING_ID
        val pitch = with(compose.density) { (compose.onNodeWithTag(WidgetTags.RING).getUnclippedBoundsInRoot().height + gap).toPx() / ring.rows }

        compose.onNodeWithTag(WidgetTags.EDIT).performClick()
        compose.onNodeWithTag(WidgetTags.EDIT).performTouchInput {
            down(center)
            repeat(ring.row * 4) { moveBy(Offset(0f, -pitch / 4)) }
            moveBy(Offset(0f, -pitch / 4))
            up()
        }

        compose.runOnIdle {
            assertEquals(0, ringTaps)
            assertEquals(0, page.ring?.row)
            assertEquals(search.copy(row = ring.rows), page.widgets.single { it.id == search.id })
            assertEquals("it stays edited", RING_ID, editing)
        }
    }
}
