package com.sqftware.orbitlauncher.apps

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.LauncherPage
import com.sqftware.orbitlauncher.domain.WidgetPage
import com.sqftware.orbitlauncher.domain.WidgetPages
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetPageStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = SharedPreferencesWidgetPageStore(context)

    @After
    fun emptyStore() {
        store.save(WidgetPages())
        store.savePick(null)
    }

    @Test
    fun eachPageComesBackWithItsPlaces() {
        val pages = WidgetPages(
            mapOf(
                LauncherPage.Widgets.id to WidgetPage(listOf(HostedWidget(12, row = 0, column = 1, rows = 2, columns = 3))),
                "widgets-2" to WidgetPage(listOf(HostedWidget(3, row = 4, column = 0, rows = 1, columns = 2))),
            ),
        )

        store.save(pages)

        assertEquals(pages, SharedPreferencesWidgetPageStore(context).load())
    }

    @Test
    fun thePickInProgressComesBackUntilItIsCleared() {
        store.savePick(WidgetPick(id = 14, page = "widgets-2", pageRows = 9, columnWidthDp = 90.5f, rowHeightDp = 84.25f))
        assertEquals(WidgetPick(14, "widgets-2", 9, 90.5f, 84.25f), SharedPreferencesWidgetPageStore(context).loadPick())

        store.savePick(null)

        assertNull(SharedPreferencesWidgetPageStore(context).loadPick())
    }
}
