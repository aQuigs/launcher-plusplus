package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.ReorderMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReorderModeStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("reorder", Context.MODE_PRIVATE)
    private val store = SharedPreferencesReorderModeStore(context)

    @Before
    @After
    fun forgetTheChoice() = prefs.edit(commit = true) { clear() }

    @Test
    fun insertUntilSwapIsChosenAndAnUnknownModeFallsBackToInsert() {
        assertEquals(ReorderMode.Insert, store.load())

        store.save(ReorderMode.Swap)
        assertEquals(ReorderMode.Swap, SharedPreferencesReorderModeStore(context).load())

        prefs.edit(commit = true) { putString("mode", "Shuffle") }
        assertEquals(ReorderMode.Insert, store.load())
    }
}
