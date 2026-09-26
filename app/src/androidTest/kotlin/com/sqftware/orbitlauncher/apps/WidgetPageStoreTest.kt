package com.sqftware.orbitlauncher.apps

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.HostedWidget
import com.sqftware.orbitlauncher.domain.WidgetPage
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
        store.save(WidgetPage())
        store.savePick(null)
    }

    @Test
    fun thePageComesBackWithItsPlaces() {
        val page = WidgetPage(listOf(HostedWidget(12, row = 0, column = 1, rows = 2, columns = 3), HostedWidget(3, row = 4, column = 0, rows = 1, columns = 2)))

        store.save(page)

        assertEquals(page, SharedPreferencesWidgetPageStore(context).load())
    }

    @Test
    fun thePickInProgressComesBackUntilItIsCleared() {
        store.savePick(WidgetPick(id = 14, pageRows = 9, columnWidthDp = 90))
        assertEquals(WidgetPick(14, 9, 90), SharedPreferencesWidgetPageStore(context).loadPick())

        store.savePick(null)

        assertNull(SharedPreferencesWidgetPageStore(context).loadPick())
    }
}
