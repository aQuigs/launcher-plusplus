package com.sqftware.orbitlauncher.apps

import android.content.Context
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sqftware.orbitlauncher.domain.AppSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSettingsStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val store = SharedPreferencesAppSettingsStore(context)

    @Before
    @After
    fun forgetTheSettings() = prefs.edit(commit = true) { clear() }

    @Test
    fun nothingSetUntilSavedThenWhatWasSaved() {
        assertEquals(AppSettings(), store.load())

        val settings = AppSettings(badgeOff = setOf("com.example.mail"), offBuiltInCards = setOf("com.example.mail", "com.example.maps"))
        store.save(settings)

        assertEquals(settings, SharedPreferencesAppSettingsStore(context).load())
    }
}
