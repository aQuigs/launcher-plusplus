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
    fun thePageComesBackInOrder() {
        val page = WidgetPage(listOf(HostedWidget(12, 2), HostedWidget(3, 1)))

        store.save(page)

        assertEquals(page, SharedPreferencesWidgetPageStore(context).load())
    }

    @Test
    fun thePickInProgressComesBackUntilItIsCleared() {
        store.savePick(WidgetPick(id = 14, pageRows = 9))
        assertEquals(WidgetPick(14, 9), SharedPreferencesWidgetPageStore(context).loadPick())

        store.savePick(null)

        assertNull(SharedPreferencesWidgetPageStore(context).loadPick())
    }
}
