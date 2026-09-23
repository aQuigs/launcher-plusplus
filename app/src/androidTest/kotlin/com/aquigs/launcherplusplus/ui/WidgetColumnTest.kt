package com.aquigs.launcherplusplus.ui

import android.graphics.Color
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.launcherplusplus.domain.HostedWidget
import com.aquigs.launcherplusplus.domain.WIDGET_ROW_HEIGHT_DP
import com.aquigs.launcherplusplus.domain.WidgetPage
import org.junit.Assert.assertEquals
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
    private var openMenu by mutableStateOf<Int?>(null)

    private fun show() = compose.setContent {
        WidgetColumn(
            page = page,
            view = { context, id ->
                shown += id
                View(context).apply {
                    setBackgroundColor(Color.RED)
                    if (id != inert.id) setOnClickListener { tapped += id }
                }
            },
            onAdd = added::add,
            menu = WidgetMenu(
                onOpen = { openMenu = it.id },
                content = { widget ->
                    if (openMenu == widget.id) {
                        WidgetOptionsMenu(expanded = true, onRemove = { removed += widget.id }, onDismiss = { openMenu = null })
                    }
                },
            ),
        )
    }

    @Test
    fun anEmptyPageSaysSoInTheMiddleOverTheAddButton() {
        show()

        val page = compose.onRoot().getUnclippedBoundsInRoot()
        val hint = compose.onNodeWithText("No widgets yet").assertIsDisplayed().getUnclippedBoundsInRoot()
        val button = compose.addWidgetButton().assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue("the hint sits over the button", hint.bottom <= button.top)
        assertTrue("$hint is not in the middle of $page", hint.top > page.height / 3 && button.bottom < page.height * 2 / 3)
    }

    @Test
    fun theAddButtonAsksForAWidgetSizedForThePage() {
        show()
        val pageRows = ((compose.onRoot().getUnclippedBoundsInRoot().height - 32.dp) / WIDGET_ROW_HEIGHT_DP.dp).toInt()

        compose.addWidgetButton().performClick()

        compose.runOnIdle { assertEquals(listOf(pageRows), added) }
        assertTrue("$pageRows rows fit the page", pageRows >= 4)
    }

    @Test
    fun widgetsStackInOrderAtTheirHeightsWithTheButtonBelow() {
        page = WidgetPage(listOf(search, game))
        show()

        val first = compose.widget(search).assertIsDisplayed().getUnclippedBoundsInRoot()
        val second = compose.widget(game).assertIsDisplayed().getUnclippedBoundsInRoot()
        val button = compose.addWidgetButton().getUnclippedBoundsInRoot()
        assertEquals(WIDGET_ROW_HEIGHT_DP.dp, first.height)
        assertEquals((2 * WIDGET_ROW_HEIGHT_DP).dp, second.height)
        assertTrue("$first, $second, $button", first.bottom <= second.top && second.bottom <= button.top)
        compose.onNodeWithText("No widgets yet").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(search.id, game.id), shown) }
    }

    @Test
    fun aTapReachesTheWidgetButALongPressOpensTheMenuInstead() {
        page = WidgetPage(listOf(search, game))
        show()

        compose.widget(game).performClick()
        compose.runOnIdle { assertEquals(listOf(game.id), tapped) }

        compose.longPressWidget(game)
        compose.widgetOptionsMenu().assertIsDisplayed()
        compose.onNodeWithText("Remove").performClick()

        compose.runOnIdle {
            assertEquals(listOf(game.id), removed)
            assertEquals("the long press was not also a tap", listOf(game.id), tapped)
        }
    }

    @Test
    fun aWidgetThatIgnoresTouchesOpensItsMenuOnALongPressButNotOnATap() {
        page = WidgetPage(listOf(inert))
        show()

        compose.widget(inert).performClick()
        compose.waitOutWidgetLongPress()
        compose.widgetOptionsMenu().assertDoesNotExist()

        compose.longPressWidget(inert)
        compose.widgetOptionsMenu().assertIsDisplayed()
    }
}
