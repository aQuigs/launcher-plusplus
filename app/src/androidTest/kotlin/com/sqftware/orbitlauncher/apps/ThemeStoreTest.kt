package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.Theme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
    private val store = SharedPreferencesThemeStore(context)

    @Before
    @After
    fun forgetTheChoice() = prefs.edit(commit = true) { clear() }

    @Test
    fun spaceUntilAnotherIsChosenAndAnUnknownThemeFallsBackToIt() {
        assertEquals(Theme.Space, store.load())

        store.save(Theme.Clockwork)
        assertEquals(Theme.Clockwork, SharedPreferencesThemeStore(context).load())

        prefs.edit(commit = true) { putString("theme", "Nebula") }
        assertEquals(Theme.Space, store.load())
    }
}
